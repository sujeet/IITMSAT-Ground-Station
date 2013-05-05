package data;

public class BitConverter
{
    /*
    Example:
    
    initial byte array
    ------------------
    0F-00-00-80-10-27-F0-D8-F1-FF-7F

    index   returned short value     returned int value     returned long
    -----   --------------------     ------------------     -------------
        1            00-00              00-00-80-10         00-...-F1
        0            0F-00              0F-00-00-80         0F-...-D8
        2            00-80              00-80-10-27         00-...-FF
    */

    /**
     * @param num_bytes maximum can be 8 (else, overflow will occur)
     */
    private static long ConvertBytesToLong (byte [] srcArray, int offset, int num_bytes)
    {
        long answer = 0;
        for (int i =0; i < num_bytes; i++) {
            answer = answer << 8;
            answer += srcArray [offset + i];
        }
        return answer;
    }
    
    /**
     * @param srcArray Byte array from which the short at given array is to be retrieved.
     * @param offset Short at this offset should be returned.
     * @return The two bytes at offset as a short.
     */
    public static short ToInt16 (byte [] srcArray, int offset)
    {
        return (short) ConvertBytesToLong (srcArray, offset, 2);
    }

    /**
     * @param srcArray Byte array from which the int at given array is to be retrieved.
     * @param offset int at this offset should be returned.
     * @return The four bytes at offset as a int.
     */
    public static int ToInt32 (byte [] srcArray, int offset)
    {
        // TODO Auto-generated method stub
        return (int) ConvertBytesToLong (srcArray, offset, 4);
    }

    /**
     * @param srcArray Byte array from which the long at given array is to be retrieved.
     * @param offset long at this offset should be returned.
     * @return The eight bytes at offset as a long.
     */
    public static long ToInt64 (byte [] srcArray, int offset)
    {
        return (long) ConvertBytesToLong (srcArray, offset, 8);
    }

}
