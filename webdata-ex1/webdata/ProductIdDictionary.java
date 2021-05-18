package webdata;

import java.io.*;

public class ProductIdDictionary extends Dictionary {
    protected int[] locationsReviews;
    protected String dir_index;

    /***
     * Contains all the products of the reviews.
     * We compressed the data using k-1 in k front coding method using k = 6. We wrote into one file
     * string that is an alphanumeric concatenation of all the products id’s appears in all the reviews.
     * For each k-successive product we removed a mutual prefix that we calculated. We have a frequencies
     * file that contains the total frequency of each productId, and a file that contains positions to the
     * posting list of each productId in the posting lists file.
     * @param dir - The directory to read the inverted index from
     */
    public ProductIdDictionary(String dir) {
        K = 6;
        dir_index = dir;
        concatenation = "";
        gamma = new Gamma();
        try {
            DataInputStream infoBlocksFile = new DataInputStream( new FileInputStream( dir +
                    "\\infoBlocksProduct.bin" ) );
            DataInputStream sizesFile = new DataInputStream(
                    new FileInputStream( dir + "\\sizesProduct.bin" ) );
            DataInputStream positionsFile = new DataInputStream( new FileInputStream( dir +
                    "\\positionsProduct.bin" ) );
            DataInputStream locationsFile = new DataInputStream( new FileInputStream( dir +
                    "\\locationsLongString.bin" ) );
            BufferedReader longStringFile = new BufferedReader( new FileReader( dir +
                    "\\longStringProduct.txt" ) );
            tokensSize = infoBlocksFile.readInt();
            blocks = (int) Math.ceil( (double) tokensSize / K );
            tokenPointer = new int[blocks];
            readInfoBlockFile( infoBlocksFile );
            sizeToken = new int[tokensSize]; // size of each product
            prefixSize = new int[tokensSize]; // size of each product prefix
            readSizesFile( sizesFile );
            postingLists = new int[tokensSize];
            locationsReviews = new int[tokensSize];
            readPositionsFile( positionsFile );
            readLongString( longStringFile );
            readLocationsFile( locationsFile );
            infoBlocksFile.close();
            sizesFile.close();
            positionsFile.close();
            locationsFile.close();
            longStringFile.close();
        } catch (Exception e) {
            System.out.println( "Error - Constructor Product" );
        }
    }


    /***
     * Reads the pointers in block of each product into an array field.
     * @param locationsFile - The file in which the locations are written.
     */
    protected void readLocationsFile(DataInputStream locationsFile) {
        try {
            int i = 0;
            while (i < tokensSize) {
                locationsReviews[i] = locationsFile.readInt();
                i++;
            }
        } catch (Exception e) {
            System.out.println( "Error - locations of products" );
        }
    }

    /***
     * Finds the index of the productId in the dictionary
     * @param token - The productId to search for
     * @return The index of the given productId in the dictionary
     */
    public int searchToken(String token) {
        return searchTokenRecursive( 0, blocks - 1, token );
    }

    /***
     * @return The number of productsIds
     */
    public int getNumOfProducts() {
        return tokensSize;
    }


    /***
     * Finds the product id connected to the given reviewId
     * @param reviewID - The reviewId which to find it's product id
     * @return The product id connected to the given reviewId
     */
    public String getProductId(int reviewID) {
        String productOfReview = "";
        int product = getProductIndex( reviewID + 1 );
        int locationProduct = locationsReviews[product];
        int currProductPrefix = prefixSize[product];
        productOfReview = productOfReview.concat( concatenation.substring( locationProduct,
                locationProduct + (10 - currProductPrefix) ) );
        while ((productOfReview.length() < 10) && (currProductPrefix != 0)) {
            product--;
            if (currProductPrefix > prefixSize[product]) {
                int diff = currProductPrefix - prefixSize[product];
                locationProduct = locationsReviews[product];
                productOfReview = concatenation.substring( locationProduct,
                        locationProduct + diff ).concat( productOfReview );
                currProductPrefix = prefixSize[product];
            }
        }
        return productOfReview;
    }

    /***
     * Finds the index of the given review id's product
     * @param reviewID - The reviewId which to find it's product id
     * @return The index in the dictionary of the product that connected to the given reviewId
     */
    protected int getProductIndex(int reviewID) {
        for (int i = 0; i < tokensSize - 1; i++) {
            int[] allReviews = readInfoTokenByPos( dir_index, "\\productPosting.bin",
                    postingLists[i], postingLists[i + 1] );
            int prevRev = 0;
            for (int k = 0; k < allReviews.length; k++) {
                allReviews[k] += prevRev;
                prevRev = allReviews[k];
            }
            for (int j = 0; j < allReviews.length; j++) {
                if (allReviews[j] == reviewID) {
                    return i;
                }
            }
        }
        int[] allReviews = readInfoTokenByPos( dir_index, "\\productPosting.bin",
                postingLists[tokensSize - 1], -1 );
        int prevRev = 0;
        for (int k = 0; k < allReviews.length; k++) {
            allReviews[k] += prevRev;
            prevRev = allReviews[k];
        }
        for (int j = 0; j < allReviews.length; j++) {
            if (allReviews[j] == reviewID) {
                return tokensSize - 1;
            }
        }
        return -1;
    }

}
