package webdata;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;

public class TokensDictionary extends Dictionary {
    private int[] totalFrequencies; // total frequency for each token
    private int reviews;
    private int collection;


    /***
     * Contains all the tokens in the reviews.
     * We compressed the data using k-1 in k front coding method using k = 6. We wrote into one file
     * string that is an alphanumeric concatenation of all the products id’s appears in all the reviews.
     * For each k-successive product we removed a mutual prefix that we calculated. We have a frequencies
     * file that contains the total frequency of each token, and a file that contains positions to the
     * posting list of each token in the posting lists file.
     * @param dir - The directory to read the inverted index from
     */
    public TokensDictionary(String dir) {
        K = 24;
        concatenation = "";
        gamma = new Gamma();
        try {
            DataInputStream infoBlocksFile = new DataInputStream( new FileInputStream( dir +
                    "\\infoBlocks.bin" ) );
            DataInputStream sizesFile = new DataInputStream( new FileInputStream(
                    dir + "\\sizes.bin" ) );
            DataInputStream positionsFile = new DataInputStream( new FileInputStream( dir +
                    "\\positions.bin" ) );
            File totalFrequenciesFile = new File( dir + "\\frequencies.bin" );
            BufferedReader longStringFile = new BufferedReader( new FileReader( dir +
                    "\\longString.txt" ) );
            tokensSize = infoBlocksFile.readInt();
            blocks = (int) Math.ceil( (double) tokensSize / K );
            reviews = infoBlocksFile.readInt();
            collection = infoBlocksFile.readInt();
            tokenPointer = new int[blocks];
            readInfoBlockFile( infoBlocksFile );
            sizeToken = new int[tokensSize];
            prefixSize = new int[tokensSize];
            readSizesFile( sizesFile );
            postingLists = new int[tokensSize];
            readPositionsFile( positionsFile );
            totalFrequencies = new int[tokensSize];
            readFrequenciesFile( totalFrequenciesFile );
            readLongString( longStringFile );
            infoBlocksFile.close();
            sizesFile.close();
            positionsFile.close();
            longStringFile.close();

        } catch (Exception e) {
            System.out.println( "Error - Constructor" );
        }
    }

    /***
     * Reads the total frequencies of each token into an array field.
     * @param totalFrequenciesFile - The file in which the frequencies are written.
     */
    private void readFrequenciesFile(File totalFrequenciesFile) {
        try {
            String frequencies = getAllBytes( totalFrequenciesFile );
            String[] totalFrequenciesCodes = getAllCodes( frequencies );
            int i = 0;
            for (String code : totalFrequenciesCodes) {
                int freq = gamma.decode( code );
                totalFrequencies[i] = freq;
                i++;
            }
        } catch (Exception e) {
            System.out.println( "Error - frequencies" );
        }

    }

    /***
     * This function read all bytes of the given file
     * @param file - The file to read bytes from
     * @return - concatenation string of all bytes read from the given file
     */
    private String getAllBytes(File file) {
        try {
            byte[] bytes = Files.readAllBytes( file.toPath() );
            return new BigInteger( bytes ).toString( 2 );
        } catch (Exception e) {
            System.out.println( "Error - getAllBytes!!" );
        }
        return "";
    }

    /***
     * Finds the index of the token in the dictionary
     * @param token - The token to search for
     * @return The index of the given token in the dictionary
     */
    public int searchToken(String token) {
        return searchTokenRecursive( 0, blocks - 1, token.toLowerCase() );
    }

    /***
     * @return The number of reviews
     */
    public int getNumOfReviews() {
        return reviews;
    }

    /***
     * @return The number of number of tokens
     */
    public int getNumOfCollection() {
        return collection;
    }

    /***
     * @param index - Index of desired token
     * @return The frequency of the given token's index
     */
    public int getFrequencyOfToken(int index) {
        return totalFrequencies[index];
    }

}
