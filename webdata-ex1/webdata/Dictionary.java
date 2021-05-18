package webdata;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;

public abstract class Dictionary {
    protected int K;
    protected int[] sizeToken; // sizes for each token
    protected int[] prefixSize; // mutual prefix size of token and prev token
    protected int[] postingLists; // positions to postingLists
    protected int[] tokenPointer; // pointer to the beginning of each block
    protected int tokensSize;
    protected int blocks;
    protected String concatenation;
    protected Gamma gamma; // an encoder object


    /***
     * Reads the concatenation string into a string field.
     * @param longStringFile - The file in which the string is written.
     */
    protected void readLongString(BufferedReader longStringFile) {
        try {
            concatenation = longStringFile.readLine();
        } catch (Exception e) {
            System.out.println( "Error - longString" );
        }
    }

    /***
     * Reads the positions of the posting lists of each token into an array field.
     * @param positionsFile - The file in which the positions are written.
     */
    protected void readPositionsFile(DataInputStream positionsFile) {
        try {
            int i = 0;
            while (i < tokensSize) {
                postingLists[i] = positionsFile.readInt();
                i++;
            }
        } catch (Exception e) {
            System.out.println( "Error positions!!" );
        }
    }

    /***
     * Reads the pointers in block of each token into an array field.
     * @param infoBlocksFile - The file in which the pointers are written.
     */
    protected void readInfoBlockFile(DataInputStream infoBlocksFile) {
        try {
            int i = 0;
            while (i < blocks) {
                tokenPointer[i] = infoBlocksFile.readInt();
                i++;
            }
        } catch (Exception e) {
            System.out.println( "Error - info" );
        }
    }

    /***
     * Reads the sizes of each token and each token's prefix into an array field.
     * @param sizeFile - The file in which the sizes are written.
     */
    protected void readSizesFile(DataInputStream sizeFile) {
        try {
            int i = 0;
            while (i < tokensSize) {
                sizeToken[i] = sizeFile.readInt();
                prefixSize[i] = sizeFile.readInt();
                i++;
            }
        } catch (Exception e) {
            System.out.println( "Error - sizes" );
        }
    }

    /***
     * The function gets a long string of gamma code, removes the overhead and parses the string into the
     * gamma codes
     * @param gammaCode - The long string to parse
     * @return - String array of all codes parsed from the string
     */
    protected String[] getAllCodes(String gammaCode) {
        ArrayList<String> numbers = new ArrayList<>();
        while (0 < gammaCode.length()) {
            int overhead = findOverhead( gammaCode.substring( 0, 8 ) );
            gammaCode = gammaCode.substring( 8 + overhead );
            if (gammaCode.length() == 1) {
                numbers.add( gammaCode );
                break;
            }
            String lenNum = gammaCode.substring( 0, gammaCode.indexOf( "0" ) + 1 );
            String code = gammaCode.substring( 0, 2 * lenNum.length() - 1 );
            numbers.add( code );
            if (code.length() == gammaCode.length()) {
                break;
            }
            gammaCode = gammaCode.substring( code.length() + 8 );
        }
        String[] finalNumbers = new String[numbers.size()];
        for (int i = 0; i < finalNumbers.length; i++) {
            finalNumbers[i] = numbers.get( i );
        }
        return finalNumbers;
    }

    /***
     * This function get a series of gamma codes and decodes them into their real numbers
     * @param codes - codes to decode
     * @return - An int array contains all numbers decoded from the given gamma codes
     */
    private int[] decodeGammaCodes(String[] codes) {
        int[] numbers = new int[codes.length];
        for (int i = 0; i < codes.length; i++) {
            numbers[i] = gamma.decode( codes[i] );
        }
        return numbers;
    }

    /***
     * Finds the overhead of a given gamma code
     * @param substring - The gamma code to calculate it's overhead
     * @return - The overhead of the given gamma code
     */
    private int findOverhead(String substring) {
        int counter = 0;
        for (int i = 0; i < 8; i++) {
            if (substring.charAt( i ) == '1') {
                counter++;
            }
        }
        return counter;
    }

    /***
     * Finds the index of the token in the dictionary
     * @param token - The token to search for
     * @return The index of the given token in the dictionary
     */
    public abstract int searchToken(String token);

    /***
     * A recursive function that performs a binary search to find in which block the token is
     * @param left - Left boundary to search from
     * @param right - Right boundary to search from
     * @param token - The token to search for
     * @return The index of the given token in the dictionary
     */
    protected int searchTokenRecursive(int left, int right, String token) {
        if (left < right) {
            int middle = left + (right - left) / 2;
            if (token.equals( concatenation.substring( tokenPointer[middle], tokenPointer[middle] +
                    (byte) sizeToken[middle * K] ) )) {
                return middle * K;
            }
            return checkWhichBlock( left, right, token, middle );
        }
        if (right == left) {
            if (!(token.equals( concatenation.substring( tokenPointer[left],
                    tokenPointer[left] + (byte) sizeToken[left * K] ) ))) {
                return searchInBlock( left, token );
            }
            return left * K;
        }
        return -1;
    }

    /***
     * A recursive function that performs a binary search to find in which block the token is
     * @param left - Left boundary to search from
     * @param right - Right boundary to search from
     * @param token - The token to search for
     * @return The index of the given token in the dictionary
     */
    private int checkWhichBlock(int left, int right, String token, int middle) {
        if (token.compareTo( concatenation.substring( tokenPointer[middle], tokenPointer[middle] +
                (byte) sizeToken[middle * K] ) ) < 0) {
            return searchTokenRecursive( left, middle - 1, token );
        }
        if (token.compareTo( concatenation.substring( tokenPointer[middle + 1],
                tokenPointer[middle + 1] + (byte) sizeToken[(middle + 1) * K] ) ) < 0) {
            return searchTokenRecursive( middle, middle, token );
        }
        return searchTokenRecursive( middle + 1, right, token );
    }

    /***
     * Search for the token in a given block (linear search)
     * @param left - Left boundary to search from
     * @param token - The token to search for
     * @return The index of the given token in the dictionary
     */
    private int searchInBlock(int left, String token) {
        int startBlock = tokenPointer[left];
        int posAccordingToBlocks = left * K; // index to token number
        String prevToken = concatenation.substring( startBlock, startBlock +
                (byte) sizeToken[posAccordingToBlocks] );
        startBlock += (byte) sizeToken[posAccordingToBlocks];

        int endBlock = tokensSize;
        if ((tokensSize - posAccordingToBlocks >= K) || (left != (blocks - 1))) {
            endBlock = posAccordingToBlocks + K;
        }

        posAccordingToBlocks++;
        String currToken;
        while (posAccordingToBlocks < endBlock) {
            currToken = concatenation.substring( startBlock,
                    startBlock + (byte) sizeToken[posAccordingToBlocks] -
                            (byte) prefixSize[posAccordingToBlocks] );
            String prefixCurrToken = prevToken.substring( 0, (byte) prefixSize[posAccordingToBlocks] );
            currToken = prefixCurrToken.concat( currToken );
            if (currToken.equals( token )) return posAccordingToBlocks;

            prevToken = currToken;
            startBlock += (byte) sizeToken[posAccordingToBlocks] - (byte) prefixSize[posAccordingToBlocks];
            posAccordingToBlocks++;
        }
        return -1;
    }

    /***
     * Read bytes from a given file by the given positions
     * @param dir - The directory in which the desired file is in
     * @param fileName - The file to read bytes from
     * @param start - Start position to read from
     * @param end - End position to read until
     * @return An int array that contains all numbers written in the given file from position start to
     * position end
     */
    public int[] readInfoTokenByPos(String dir, String fileName, int start, int end) {
        try {
            RandomAccessFile file = new RandomAccessFile( dir + fileName, "r" );
            file.skipBytes( start );
            // if there is no end position - end of file
            if (end == -1) {
                ArrayList<Byte> bytes = new ArrayList<>();
                while (true) {
                    try {
                        byte curByte = file.readByte();
                        bytes.add( curByte );
                    } catch (EOFException e) {
                        break;
                    }
                }
                file.seek( 0 );
                file.close();
                byte[] bytesArray = new byte[bytes.size()];
                for (int i = 0; i < bytes.size(); i++) {
                    bytesArray[i] = bytes.get( i );
                }
                String numbers = new BigInteger( bytesArray ).toString( 2 );
                String[] codes = getAllCodes( numbers );
                return decodeGammaCodes( codes );
            }
            file.seek( 0 );
            file.close();
        } catch (Exception e) {
            System.out.println( "Error - reading file by position" );
        }
        byte[] bytesArray = new byte[(end - start)];
        try {
            RandomAccessFile file = new RandomAccessFile( dir + fileName, "r" );
            file.skipBytes( start );
            for (int i = 0; i < (end - start); i++) {
                bytesArray[i] = file.readByte();
            }
            file.seek( 0 );
            file.close();
        } catch (Exception e) {
            System.out.println( "Error - reading file by position" );
        }
        String numbers = new BigInteger( bytesArray ).toString( 2 );
        String[] codes = getAllCodes( numbers );
        return decodeGammaCodes( codes );
    }

    /***
     * @return The number of tokens in the dictionary
     */
    public int getNumOfTokens() {
        return tokensSize;
    }

    /***
     * @param index - Index of desired token
     * @return The position of the given token's positing list
     */
    public int getPostingListPosOfToken(int index) {
        return postingLists[index];
    }

}
