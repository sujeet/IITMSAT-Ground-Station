package data_Ccsds.ParameterCode;

import data.BitConverter;

/**
 * A class to deal with byte arrays, in which bits need not necessarily be aligned.
 * Example: real data is one byte, all 1s : 11111111
 *          but the byte array is two bytes 00011111 11100011
 *          Here, the data we are interested in starts at index 3 and is one byte long.
 *
 */
public class UnalignedData
{

    /**
     * @param data is the unaligned byte array (with initial useless bits)
     * @param index number of useless bits.
     * @param num_bytes is the number of bytes to extract from the data array.
     * @return a byte array of length num_bytes extracted from data.
     */
    public static byte [] ExtractValue (byte [] data,
                                        int index,
                                        int num_bytes)
    {
        byte [] result = new byte [num_bytes];
        for (int i = 0; i < num_bytes; i++) {
            result [i] = (byte)((byte)(data [i] << index) | Shifter.right (data [i+1], 8-index));
        }
            
        return result;
    }
    
    /**
     * @param buffer byte array to insert the value into.
     * @param index bit index into buffer from where to start insertion.
     * @param value byte array to be inserted.
     * @param bitsSize number of bits from value to be inserted.
     */
    public static void InsertValue (byte [] buffer,
                                    int index,
                                    byte [] value,
                                    int bitsSize)
    {
        int shift_amt = index % 8;
        if (shift_amt == 0) {
            // Byte copy should work.
            for (int i = 0; i < (bitsSize + 7) / 8; i++) {
                buffer [index / 8 + i] = value [i];
            }
        }
        else {
            byte appendage = buffer [index / 8];
            
            byte [] new_value = AppendBitsAtStart (value, appendage, shift_amt, bitsSize);
            // The last byte to be overwritten in buffer should not be completely overwritten.
            byte last_byte = buffer [index / 8 + new_value.length - 1];
            
            int mask_size = (bitsSize + shift_amt) % 8;
            byte left_mask = Shifter.left_mask (mask_size);
            byte right_mask = Shifter.right_mask (8 - mask_size);
            
            for (int i = 0; i < new_value.length; i++) {
                buffer [index / 8 + i] = new_value [i];
            }
                buffer [index / 8 + new_value.length - 1] 
                        = (byte)((buffer [index / 8 + new_value.length - 1] & left_mask)
                                 |
                                 (last_byte & right_mask));
        }
    }
    
    /**
     * @param array array at the start of which bits are to be appended.
     * @param appendage the byte from which num_bits number of MSB are to be appended.
     * @param num_bits number of bits to append.
     * @param num_bits_to_copy number of bits to be copied from array.
     * @return new byte array with appendage at beginning and then the rest of array.
     */
    private static byte [] AppendBitsAtStart (byte [] array,
                                              byte appendage,
                                              int num_bits,
                                              int num_bits_to_copy)
    {
        byte [] return_array = new byte [(num_bits + num_bits_to_copy + 7) / 8];
        byte left_bits = Shifter.right (appendage, 8 - num_bits);
        left_bits = (byte)(left_bits << (8 - num_bits));
        byte right_bits = Shifter.right (array [0], num_bits);
        int i;
        for (i = 0; i < (num_bits_to_copy + 7)/8; i++) {
            return_array [i] = (byte)(left_bits | right_bits);
            left_bits = (byte)(array [i] << (8 - num_bits));
            if (array.length > i+1) {
                right_bits = Shifter.right (array [i+1], num_bits);
            }
        }
        if (return_array.length > i) {
            return_array [i] = (byte)(left_bits | right_bits);
        }
        return return_array;
    }
    
    // For testing purposes
    private static String Binarify( byte ByteToCheck ) {
        String binaryCode = "";
        byte[] reference = new byte[]{ (byte) 0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01 };
         
        for ( byte z = 0; z < 8; z++ ) {
            //if bit z of byte a is set, append a 1 to binaryCode. Otherwise, append a 0 to binaryCode
            if ( ( reference[z] & ByteToCheck ) != 0 ) {
                binaryCode += "1";
            }
            else {
                binaryCode += "0";
            }
        }
         
        return binaryCode;
    }
    
    private static void PrintBinArray (byte [] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print (Binarify (array [i]) + " ");
        }
        System.out.println ();
    }
    
    public static void main (String [] args)
    {
        // Testing for ExtractValue
        byte [] array = {0b00110011, 0b01110010, 0b01001011};
        PrintBinArray (array);
        byte [] extracted = ExtractValue (array, 3, 2);
        PrintBinArray (extracted);
        if (extracted.length == 2) System.out.println ("OK");
        else System.out.println (":(");
        if (extracted [0] == (byte)0b10011011) System.out.println ("OK");
        else System.out.println (":(");
        if (extracted [1] == (byte)0b10010010) System.out.println ("OK");
        else System.out.println (":(");
        
        // Testing for InsertValue
        byte [] buffer = {0b00000000, 0b00000000, 0b01010101, 0b01010101};
        byte [] value  = {(byte)0b11111111, (byte)0b11111111};
        InsertValue (buffer, 11, value, 9);
        if (buffer [0] == (byte)0b00000000) System.out.println ("OK");
        else System.out.println (":(");
        if (buffer [1] == (byte)0b00011111) System.out.println ("OK");
        else System.out.println (":(");
        if (buffer [2] == (byte)0b11110101) System.out.println ("OK");
        else System.out.println (":(");
        if (buffer [3] == (byte)0b01010101) System.out.println ("OK");
        else System.out.println (":(");
        PrintBinArray (buffer);
        PrintBinArray (BitConverter.GetBytes (Float.floatToRawIntBits ((float) 0.15)));
    }

}
