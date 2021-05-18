package webdata;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SlowIndexWriter {
    private SortedMap<String, LinkedHashMap<Integer, Integer>> dictionary; // the dictionary of the tokens
    private int collection;
    private int reviews;
    private final int K_words = 24;
    private final int K_products = 6;

    private DataOutputStream frequenciesW; // frequencies file
    private DataOutputStream postingW; // posting lists file

    private FileWriter longStringW; // long string file
    private DataOutputStream positionsW; // hold the position of posting lists and frequencies
    private DataOutputStream sizesW;
    private DataOutputStream infoBlocksW;

    private DataOutputStream productPostingW; // posting lists file

    private TreeMap<String, ArrayList<Integer>> productDict; // the dictionary of the productIds
    private FileWriter productLongStringW;
    private DataOutputStream productPositionsW; // hold the position of posting lists and frequencies
    private DataOutputStream productSizesW;
    private DataOutputStream productInfoBlocksW;
    private DataOutputStream locationsLongStringW; // long string file
    private Gamma gamma; // an encoder object


    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    public void slowWrite(String inputFile, String dir) {
        gamma = new Gamma();
        reviews = 0;
        collection = 0;
        dictionary = new TreeMap<>();
        productDict = new TreeMap<>();
        try {
            boolean created;
            int reviewId = 0;
            File pathAsFile = new File( dir );
            if (!Files.exists( Paths.get( dir ) )) {
                created = pathAsFile.mkdir();
            }
            // creates the index files
            File products = new File( dir + "\\products.txt" );
            created = products.createNewFile();
            File scores = new File( dir + "\\scores.bin" );
            created = scores.createNewFile();
            File helpfulness = new File( dir + "\\helpfulness.bin" );
            created = helpfulness.createNewFile();
            File reviewLengths = new File( dir + "\\reviewLengths.bin" );
            created = reviewLengths.createNewFile();
            FileWriter productsW = new FileWriter( dir + "\\products.txt" );
            DataOutputStream scoresW = new DataOutputStream( new FileOutputStream( dir +
                    "\\scores.bin" ) );
            DataOutputStream helpfulnessW = new DataOutputStream( new FileOutputStream( dir +
                    "\\helpfulness.bin" ) );
            DataOutputStream reviewLengthsW = new DataOutputStream( new FileOutputStream( dir +
                    "\\reviewLengths.bin" ) );
            // Read the input file
            BufferedReader reader = new BufferedReader( new FileReader( inputFile ) );
            String line = reader.readLine();
            while (line != null) {
                if (line.contains( "product/productId:" )) {
                    reviews++;
                    reviewId++;
                    String goodProduct = line.split( "\\s+" )[1];
                    if (productDict.containsKey( goodProduct )) {
                        productDict.get( goodProduct ).add( reviewId );
                    } else {
                        ArrayList<Integer> reviewsOfOneProduct = new ArrayList<>();
                        reviewsOfOneProduct.add( reviewId );
                        productDict.put( goodProduct, reviewsOfOneProduct );
                    }
                } else if (line.contains( "review/helpfulness:" )) {
                    String numeric = line.split( "\\s+" )[1];
                    String[] numerator_denominator = numeric.split( "/" );
                    byte[] numerator = intToByteArray( Integer.parseInt( numerator_denominator[0] ) );
                    helpfulnessW.write( numerator );
                    byte[] denominator = intToByteArray( Integer.parseInt( numerator_denominator[1] ) );
                    helpfulnessW.write( denominator );
                } else if (line.contains( "review/score:" )) {
                    String score = line.split( "\\s+" )[1];
                    scoresW.writeByte( score.charAt( 0 ) - '0' );
                } else if (line.contains( "review/text:" )) {
                    int size = this.writeTokens( line, reviewId );
                    reviewLengthsW.writeInt( size );
                }
                line = reader.readLine();
            }
            reader.close();
            scoresW.close();
            helpfulnessW.close();
            reviewLengthsW.close();
            openArraysFiles( dir );
            constructorDic( dir );
            buildDicFile();
            frequenciesW.close();
            postingW.close();
            positionsW.close();
            sizesW.close();
            infoBlocksW.close();
            openArraysProductFiles( dir );
            buildDicProductFile();
            productPostingW.close();
            productPositionsW.close();
            productSizesW.close();
        } catch (Exception e) {
            System.out.println( "Error in slowWrite!!!" );
        }
    }

    /***
     * Opens all the product dictionary files
     * @param dir - The directory in which to open file in
     */
    private void openArraysProductFiles(String dir) {
        try {
            File productLongString = new File( dir +
                    "\\longStringProduct.txt" );
            boolean created = productLongString.createNewFile();
            productLongStringW = new FileWriter( dir +
                    "\\longStringProduct.txt" );

            File productPositions = new File( dir +
                    "\\positionsProduct.bin" );
            created = productPositions.createNewFile();
            productPositionsW = new DataOutputStream( new FileOutputStream( dir +
                    "\\positionsProduct.bin" ) );

            File productSizes = new File( dir +
                    "\\sizesProduct.bin" );
            created = productSizes.createNewFile();
            productSizesW = new DataOutputStream( new FileOutputStream( dir +
                    "\\sizesProduct.bin" ) );

            File productInfoBlocks = new File( dir + "\\infoBlocksProduct.bin" );
            created = productInfoBlocks.createNewFile();
            productInfoBlocksW = new DataOutputStream( new FileOutputStream( dir +
                    "\\infoBlocksProduct.bin" ) );

            File locationsLongString = new File( dir + "\\locationsLongString.bin" );
            created = locationsLongString.createNewFile();
            locationsLongStringW = new DataOutputStream( new FileOutputStream( dir +
                    "\\locationsLongString.bin" ) );

        } catch (Exception e) {
            System.out.println( "Error creating file in ProductIdDictionary!!!" );
        }
    }

    /***
     * Opens all the tokens dictionary files
     * @param dir - The directory in which to open file in
     */
    private void constructorDic(String dir) {
        try {
            File frequenciesFile = new File( dir + "\\frequencies.bin" );
            boolean created = frequenciesFile.createNewFile();
            frequenciesW = new DataOutputStream( new FileOutputStream( dir +
                    "\\frequencies.bin" ) );

            File postingFile = new File( dir + "\\postingLists.bin" );
            created = postingFile.createNewFile();
            postingW = new DataOutputStream( new FileOutputStream( dir +
                    "\\postingLists.bin" ) );

            File productPostingFile = new File( dir + "\\productPosting.bin" );
            created = productPostingFile.createNewFile();
            productPostingW = new DataOutputStream( new FileOutputStream( dir +
                    "\\productPosting.bin" ) );
        } catch (Exception e) {
            System.out.println( "Error creating file in dictionary!!!" );
        }
    }

    /***
     * Opens all the tokens dictionary files
     * @param dir - The directory in which to open file in
     */
    private void openArraysFiles(String dir) {
        try {
            File longStringFile = new File( dir + "\\longString.txt" );
            boolean created = longStringFile.createNewFile();
            longStringW = new FileWriter( dir + "\\longString.txt" );

            File positionsFile = new File( dir + "\\positions.bin" );
            created = positionsFile.createNewFile();
            positionsW = new DataOutputStream( new FileOutputStream( dir +
                    "\\positions.bin" ) );
            File sizesFile = new File( dir + "\\sizes.bin" );
            created = sizesFile.createNewFile();
            sizesW = new DataOutputStream( new FileOutputStream( dir +
                    "\\sizes.bin" ) );
            File infoBlocksFile = new File( dir + "\\infoBlocks.bin" );
            created = infoBlocksFile.createNewFile();
            infoBlocksW = new DataOutputStream( new FileOutputStream( dir +
                    "\\infoBlocks.bin" ) );
            infoBlocksW.writeInt( dictionary.size() );
            infoBlocksW.writeInt( reviews );
            infoBlocksW.writeInt( collection );
        } catch (Exception e) {
            System.out.println( "Error creating file in dictionary!!!" );
        }
    }

    /***
     * Convert number into it's bytes array representation
     * @param i - The number to convert to bytes
     * @return - bytes array representing the given number
     */
    public byte[] intToByteArray(final int i) {
        BigInteger number = BigInteger.valueOf( i );
        byte[] b = number.toByteArray(); // len of array == 2
        byte[] addElem = new byte[2];
        if (b.length == 1) {
            addElem[1] = b[0];
            return addElem;
        }
        return b;
    }

    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {
        try {
            File folder = new File( dir );
            String[] entries = folder.list();
            for (String s : entries) {
                File currentFile = new File( folder.getPath(), s );
                currentFile.delete();
            }
            Files.delete( Paths.get( dir ) );
        } catch (Exception e) {
            System.out.println( "Error Deleting!!!" );
        }
    }

    /***
     * Writes into the dictionary files data about a given review text
     * @param line - The current review text
     * @param reviewId - The number of The current review
     * @return - The length of the current review text
     */
    private int writeTokens(String line, int reviewId) {
        line = line.toLowerCase();
        int counterWords = 0;
        String[] tokens = line.split( "\\s+" );

        for (String token : tokens) {
            if (token.equals( "review/text:" )) {
                continue;
            }
            String[] onlyWords = token.split( "[^a-z0-9]+" );
            for (String word : onlyWords) {
                if (word.equals( "" )) {
                    continue;
                }
                if (!dictionary.containsKey( word )) {
                    LinkedHashMap<Integer, Integer> fields = new LinkedHashMap<>();
                    fields.put( reviewId, 0 );
                    dictionary.put( word, fields );
                } else if (!dictionary.get( word ).containsKey( reviewId )) {
                    dictionary.get( word ).put( reviewId, 0 );
                }
                int freq = dictionary.get( word ).get( reviewId ) + 1;
                dictionary.get( word ).put( reviewId, freq );
                collection++;
                counterWords++;
            }
        }
        return counterWords;

    }

    /***
     * Calculates the mutual prefix of two given strings
     * @param prevToken - First string
     * @param token - Second String
     * @return The mutual prefix of two given strings
     */
    private int findPrefixSize(String prevToken, String token) {
        int size = prevToken.length() < token.length() ? prevToken.length() : token.length();
        int sizePrefix = 0;
        for (int i = 0; i < size; i++) {
            if (prevToken.charAt( i ) == token.charAt( i )) {
                sizePrefix++;
            } else break;
        }
        return sizePrefix;
    }

    /***
     * Writes all the products id and their data into the corresponding files.
     */
    private void buildDicProductFile() {
        String concatenation = "";
        int posAccordingToBlocks = 0; // index to productID
        int positionId = 0;
        String prevProduct = "";
        try {
            int positions = 0;
            productInfoBlocksW.writeInt( productDict.size() );
            for (String product : productDict.keySet()) { // for each product
                productSizesW.writeInt( product.length() );
                // not new block
                if (posAccordingToBlocks % K_products != 0) {
                    int curPrefixSize = findPrefixSize( prevProduct, product );
                    productSizesW.writeInt( curPrefixSize );
                    concatenation = concatenation.concat( product.substring( curPrefixSize ) );
                    locationsLongStringW.writeInt( positions );
                    positions += product.substring( curPrefixSize ).length();
                } else {
                    productSizesW.writeInt( 0 );
                    productInfoBlocksW.writeInt( concatenation.length() );
                    concatenation = concatenation.concat( product );
                    locationsLongStringW.writeInt( positions );
                    positions += product.length();
                }
                int prevReviewId = 0;
                productPositionsW.writeInt( positionId );

                for (int reviewId : productDict.get( product )) {
                    int gapReviewId = reviewId - prevReviewId;
                    byte[] reviewEncoded = gamma.encode( gapReviewId );
                    positionId += reviewEncoded.length;
                    productPostingW.write( reviewEncoded );
                    prevReviewId = reviewId;
                }
                posAccordingToBlocks++;
                prevProduct = product;
            }
            productLongStringW.write( concatenation );
            productLongStringW.close();
            productInfoBlocksW.close();
            locationsLongStringW.close();
        } catch (Exception e) {
            System.out.println( "Error in writing product!" );
        }
    }

    /***
     * Writes all the tokens and their data into the corresponding files.
     */
    private void buildDicFile() {
        String concatenation = "";
        int posAccordingToBlocks = 0; // index to token number
        int positionId = 0;
        String prevToken = "";
        try {
            for (String token : dictionary.keySet()) {
                sizesW.writeInt( token.length() );
                int curPrefixSize = findPrefixSize( prevToken, token );
                sizesW.writeInt( curPrefixSize );
                // not new block
                if (posAccordingToBlocks % K_words != 0) {
                    concatenation = concatenation.concat( token.substring( curPrefixSize ) );
                } else {
                    infoBlocksW.writeInt( concatenation.length() );
                    concatenation = concatenation.concat( token );
                }
                int allFrequencyInReviews = 0;
                int prevReviewId = 0;
                positionsW.writeInt( positionId );

                for (int reviewId : dictionary.get( token ).keySet()) {
                    int gapReviewId = reviewId - prevReviewId;
                    byte[] reviewEncoded = gamma.encode( gapReviewId );
                    positionId += reviewEncoded.length;
                    int freqOneReview = dictionary.get( token ).get( reviewId );
                    byte[] fregEncoded = gamma.encode( freqOneReview );
                    positionId += fregEncoded.length;
                    postingW.write( reviewEncoded );
                    postingW.write( fregEncoded );
                    allFrequencyInReviews += freqOneReview;
                    prevReviewId = reviewId;
                }
                byte[] allfreqsEncode = gamma.encode( allFrequencyInReviews );
                frequenciesW.write( allfreqsEncode );
                posAccordingToBlocks++;
                prevToken = token;
            }
            longStringW.write( concatenation );
            longStringW.close();
        } catch (Exception e) {
            System.out.println( "Error in writing!" );
        }
    }
}



