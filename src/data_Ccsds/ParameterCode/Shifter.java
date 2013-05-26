package data_Ccsds.ParameterCode;

public class Shifter
{

    /**
     * (byte) (1001 0000 >>> 4) gives us 1111 1001 <br/>
     * Shifter.right ((byte)0b10010000, 4) gives us 0000 1001
     * @param value the byte on which right-shift is to be performed.
     * @param shamt number of bits for rightshift.
     * @return a byte which is value logically right-shifted by shamt bits.
     */
    public static byte right (byte value, int shamt)
    {
        byte answer = (byte)(value >>> shamt);
        if (value >= 0) return answer;
        else {
            return (byte)(answer & 0xFFFFFFFF >>> (32 - (8 - shamt)));
        }
    }
    
    public static byte left_mask (int mask_size)
    {
        return (byte) (0xFF << (8 - mask_size));
    }    
    
    public static byte right_mask (int mask_size)
    {
        return (byte) (0xFFFFFFFF >>> (32 - mask_size));
    }
   
    // For testing
    public static void main (String [] args)
    {
        System.out.println ((byte)((byte)0b11010000 >>> 4)); // 1111 1101 -> -3 
        System.out.println (right ((byte)0b11010000, 4));    // 0000 1101 -> 13
    }
}
