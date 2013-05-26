package data_Ccsds.ParameterCode;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import data.BitConverter;
import data.ByteOrderConverter;
import data_Ccsds.Function.ArgumentNullException;
import data_Ccsds.Packets.ArgumentException;
import data_Ccsds.Packets.ArgumentOutOfRangeException;
import data_Ccsds.Packets.NotSupportedException;
import data_Ccsds.Packets.TimeSpan;

/// <summary>Provides methods to encode/decode parameter code values to/from .NET values.</summary>
public class ParameterConverter
{
	/**
	 * Encodes the given object into byte arrays (so that they can be sent over
	 * the network etc.)
	 * @param value the object to be encoded.
	 * @param parameterCode the parameter code specifying how to encode the value.
	 * @return a byte array representing the encoded object.
	 * @throws NotSupportedException 
	 * @throws UnsupportedEncodingException 
	 * @throws ArgumentOutOfRangeException 
	 * @throws ArgumentNullException 
	 */
	public static byte[] EncodeValue(Object value, ParameterCode parameterCode) throws NotSupportedException, UnsupportedEncodingException, ArgumentNullException, ArgumentOutOfRangeException
	{
		byte[] data = null;
		switch(parameterCode.Ptc)
		{
		case Boolean:
			data = new byte[1];
			if ((Boolean)value) {
			    data [0] = (byte)(1 << 7);
			}
			else {
			    data [0] = (byte)0;
			}
			break;
		case Enumerated:
			data = BitConverter.GetBytes(ByteOrderConverter.HostToNetworkOrder((int)(((int)value) << (32 - GetBitsSize(parameterCode)))));
			break;
		case UnsignedInteger:
			data = BitConverter.GetBytes(ByteOrderConverter.HostToNetworkOrder((long)(((long)value) << (64 - GetBitsSize(parameterCode)))));
			break;
		case SignedInteger:
			data = BitConverter.GetBytes(ByteOrderConverter.HostToNetworkOrder(((long)value) << (64 - GetBitsSize(parameterCode))));
			break;
		case Real:
			switch(parameterCode.Pfc)
			{
			case 1:
				data = BitConverter.GetBytes ((float)value);
				break;
			case 2:
				data = BitConverter.GetBytes ((double)value);
				break;
			default:
				throw new NotSupportedException(null);
			}
			break;
		case BitString:
			boolean [] bits = (boolean [])value;
			if(parameterCode.Pfc == 0)
			{
				int length = Math.min(bits.length, 0xFF);
				data = new byte [GetByteSize(length) + 1];
				data[0] = (byte)length;
				copyBitArrayToByteArray(bits, data, 1, length);
			}
			else
			{
				data = new byte[GetByteSize(parameterCode.Pfc)];
				copyBitArrayToByteArray(bits, data, 0, parameterCode.Pfc);
			}
			break;
		case OctetString:
			data = getByteArrayDependingOnParameterCode ((byte[])value, parameterCode);
			break;
		case CharacterString:
			data = getByteArrayDependingOnParameterCode (((String)value).getBytes("US-ASCII"), parameterCode);
			break;
		case AbsoluteTime:
			TimeSpan absoluteTime = (TimeSpan)value;
			if(parameterCode.Pfc == 0)
			    data = encodeTimeSpanWithPField(absoluteTime, (byte)0x2F, true, false); // 0b00101111 CUC-Level2-full  (PFC=18)
			else if(parameterCode.Pfc == 1)
				data = encodeTimeSpanWithPField(absoluteTime, (byte)0x48, false, false); // 0b01001000 CDS-Level2-16bit-ms
			else if(parameterCode.Pfc == 2)
				data = encodeTimeSpanWithPField(absoluteTime, (byte)0x49, false, false); // 0b01001001 CDS-Level2-16bit-us
			else
				data = encodeTimeSpanWithPField(absoluteTime, (byte)(0x20 + parameterCode.Pfc - 3), false, false);
			break;
		case RelativeTime:
			TimeSpan relativeTime = (TimeSpan)value;
			if(parameterCode.Pfc == 0)
				data = encodeTimeSpanWithPField(relativeTime, (byte)0x2F, true, true); // 0b00101111 CUC-Level2-full  (PFC=18)
			else
				data = encodeTimeSpanWithPField(relativeTime, (byte)(0x20 + parameterCode.Pfc - 1), false, true);
			break;
		case Deduced:
			data = (byte[])value;
			break;
		default:
			throw new NotSupportedException(null);
		}

		// Do we know the real size
		int size = GetBitsSize(parameterCode, data, 0);
		if(size > 0)
		{
			int byteSize = GetByteSize (size);
			// Using too much bytes
			if(data.length > byteSize)
			{
				byte[] shortData = new byte[byteSize];
				System.arraycopy (data, 0, shortData, 0, byteSize);
				data = shortData;
			}
		}

		return data;
	}
	
	/**
	 * Returns a byte array which might directly go into a CCSDS packet.
	 * Depending on the parameter code, the returned array might or might not have
	 * the first byte indicating its length.
	 * @param array the array to be copied.
	 * @param parameterCode based on this, the resultant array might have a size header.
	 * @return a newly allocated array, values copied from array, with the possibility of an header.
	 */
	private static byte [] getByteArrayDependingOnParameterCode (byte [] array,
	                                                             ParameterCode parameterCode)
    {
        byte[] data;
        // PFC zero means the first byte should talk about the length.
        if(parameterCode.Pfc == 0)
        {
            data = new byte [Math.min(array.length, 0xFF) + 1];
            data[0] = (byte)(data.length - 1);
            System.arraycopy (array, 0, data, 1, data[0]);
        }
        else
        {
            data = new byte[parameterCode.Pfc];
            System.arraycopy (array, 0, data, 0, Math.min(parameterCode.Pfc, array.length));
        }
        return data;
    }

    /**
	 * Copies a boolean array to a byte array, each bit representing a boolean.
	 * @param bits the boolean array to be copied.
	 * @param data a pre-allocated byte array to which the booleans will be copied as one or zero.
	 * @param index index in BYTES from which the bit array starts in the byte array.
	 * @param length number of bits in the bit array.
	 */
	private static void copyBitArrayToByteArray (boolean [] bits,
                                                 byte [] data,
                                                 int index,
                                                 int length)
    {
	    for (int i = 0; i < bits.length && i < length; i++) {
	        if (bits [i]) {
	              data [i / 8 + index] = (byte)(data [i / 8 + index] | (byte)(1 << (7 - (i % 8))));
	        }
	        else {
	              data [i / 8 + index] = (byte)(data [i / 8 + index] & (byte)(~(1 << (7 - (i % 8)))));
	        }
	    }
        
    }

    /// <summary>Get the number of bytes needed to store the specified number of bits.</summary>
	/// <param name="bitsSize">Size in bits.</param>
	/// <returns>Number of bytes.</returns>
	public static int GetByteSize (int bitsSize)
	{
		return (int)(bitsSize / 8) + ((bitsSize % 8 == 0) ? 0 : 1);
	}

	/// <summary>Get the number of bytes needed to store the specified parameter code.</summary>
	/// <param name="parameterCode"><see cref="ParameterCode"/> to get the storage size of.</param>
	/// <returns>Number of bytes.</returns>
	public static int GetByteSize (ParameterCode parameterCode) throws NotSupportedException
	{
		return GetByteSize (GetBitsSize(parameterCode));
	}

	/// <summary>Gets the size in bits of the values encoded with the specified <see cref="ParameterCode"/>.</summary>
	/// <param name="parameterCode"><see cref="ParameterCode"/> to get the size of.</param>
	/// <returns>A size in bits if the size is fixed, -1 otherwise.</returns>
	/// <remarks>
	/// It is important to check the value returned by this method, it must not be
	/// used directly without proper verification that the size is fixed and not variable.
	/// The other overload can use the data to get the effective size of a variable length parameter code.
	/// </remarks>
	public static int GetBitsSize(ParameterCode parameterCode) throws NotSupportedException
	{
		switch(parameterCode.Ptc)
		{
		case NoData:
			return 0;
		case Boolean:
			return 1;
		case Enumerated:
			return parameterCode.Pfc; // Bits size is PFC with Enumerated
		case UnsignedInteger:
			if((parameterCode.Pfc >= 0) && (parameterCode.Pfc <= 12))
				return parameterCode.Pfc + 4;
			else if(parameterCode.Pfc == 13)
				return 3 * 8;
			else if(parameterCode.Pfc == 14)
				return 4 * 8;
			else if(parameterCode.Pfc == 15)
				return 6 * 8;
			else if(parameterCode.Pfc == 16)
				return 8 * 8;
			break;
		case SignedInteger:
			if((parameterCode.Pfc >= 0) && (parameterCode.Pfc <= 12))
				return parameterCode.Pfc + 4;
			else if(parameterCode.Pfc == 13)
				return 3 * 8;
			else if(parameterCode.Pfc == 14)
				return 4 * 8;
			else if(parameterCode.Pfc == 15)
				return 6 * 8;
			else if(parameterCode.Pfc == 16)
				return 8 * 8;
			break;
		case Real:
			if(parameterCode.Pfc == 1)
				return 4 * 8;
			else if(parameterCode.Pfc == 2)
				return 8 * 8;
			else if(parameterCode.Pfc == 3)
				return 4 * 8;
			else if(parameterCode.Pfc == 4)
				return 6 * 8;
			break;
		case BitString:
			if(parameterCode.Pfc == 0)
				return -1; // Variable length
			else
				return parameterCode.Pfc;
		case OctetString:
			if(parameterCode.Pfc == 0)
				return -1; // Variable length
			else
				return parameterCode.Pfc * 8;
		case CharacterString:
			if(parameterCode.Pfc == 0)
				return -1; // Variable length
			else
				return parameterCode.Pfc * 8;
		case AbsoluteTime:
			if(parameterCode.Pfc == 0)
				return 8 * 8; // Full Format: 1 P-Field + 4 Coarce + 3 Fine
			else if(parameterCode.Pfc == 1)
				return 6 * 8;
			else if(parameterCode.Pfc == 2)
				return 8 * 8;
			else if((parameterCode.Pfc >= 3) && (parameterCode.Pfc <= 18))
				return (((parameterCode.Pfc + 1) / 4) + ((parameterCode.Pfc + 1) % 4)) * 8;
			break;
		case RelativeTime:
			if(parameterCode.Pfc == 0)
				return 8 * 8; // Full Format: 1 P-Field + 4 Coarce + 3 Fine
			else if((parameterCode.Pfc >= 1) && (parameterCode.Pfc <= 16))
				return (((parameterCode.Pfc + 3) / 4) + ((parameterCode.Pfc + 3) % 4)) * 8;
			break;
		case Deduced:
			if(parameterCode.Pfc == 0)
				return -1; // Variable length
			break;
            default:
                break;
		}

		throw new NotSupportedException(null);
	}

	/// <summary>Get the size in bits of the specified parameter code using the data for additional information.</summary>
	/// <param name="parameterCode"><see cref="ParameterCode"/> to get the size of.</param>
	/// <param name="data">Byte array containing the actual value used to get a variable length parameter code.</param>
	/// <param name="index">Index in bits at which the value start.</param>
	/// <returns>A size in bits.</returns>
	/// <exception cref="ArgumentNullException"><c>data</c> is null.</exception>
	/// <exception cref="ArgumentOutOfRangeException"><c>index</c> does not point to a bit index inside the data array.</exception>
	public static int GetBitsSize(ParameterCode parameterCode, byte[] data, int index) throws ArgumentNullException, ArgumentOutOfRangeException, NotSupportedException
	{
		if(data == null)
			throw new ArgumentNullException("data");
		if(index < 0 || index > (data.length * 8 - 1))
			throw new ArgumentOutOfRangeException("Index does not point to a bit inside the data array.");

		// For strings, PFC zero means that they are of variable length.
		// For variable length strings, the first byte of the string
		// makes up the integer which states the length of the string.
		if((parameterCode.Ptc == PtcType.OctetString
		    ||
		    parameterCode.Ptc == PtcType.CharacterString
		   )
		   &&
		   (parameterCode.Pfc == 0)
		  )
			return (int)UnalignedData.ExtractValue(data, index, 1)[0] * 8 + 8;
		else if(parameterCode.Ptc == PtcType.BitString && parameterCode.Pfc == 0)
			return (int)UnalignedData.ExtractValue(data, index, 1)[0] + 8;
		// NOTE : The +8 is because 8 bits are used up to store the size itself.
		else
			return GetBitsSize(parameterCode);
	}

    public static int InsertValue (byte [] buffer,
                                   int index,
                                   Object value,
                                   ParameterCode parameterCode) throws NotSupportedException, UnsupportedEncodingException, ArgumentNullException, ArgumentOutOfRangeException
    {
        int bitsSize = ParameterConverter.GetBitsSize(parameterCode);
        byte[] encodedData = ParameterConverter.EncodeValue(value, parameterCode);
        // HACK:
        if(bitsSize == -1)
            bitsSize = encodedData.length * 8;
        UnalignedData.InsertValue(buffer, index, encodedData, bitsSize);
        return bitsSize;
    }
    
	/**
	 * Extract a value from a byte array.
	 * @param data Byte array containing the value to extract.
	 * @param parameterCode Parameter code of the value to extract.
	 * @param startBit index in bits at which the value starts.
	 * @return the extracted value.
	 */
	public static Object ExtractValue(byte[] data,
	                                  ParameterCode parameterCode,
	                                  int startBit) throws ArgumentNullException, ArgumentException
    {
        if(data == null)
            throw new ArgumentNullException("data");
        if(startBit < 0 || startBit > (data.length * 8 - 1))
            throw new IndexOutOfBoundsException ("startBit does not point to a bit index inside the data array.");

        try
        {
            int readLength = ParameterConverter.GetBitsSize(parameterCode, data, startBit);
            byte[] extractedData = UnalignedData.ExtractValue(data, startBit, readLength);
            Object value = null;

            int start;
            switch(parameterCode.Ptc)
            {
                case Boolean:
                    value = (extractedData[0] >> 7) > 0;
                    break;
                case Enumerated:
                case UnsignedInteger:
                case SignedInteger:
                    value = BitConverter.ToInt64 (extractedData, 0);
                    break;
                case Real:
                    switch(parameterCode.Pfc)
                    {
                        case 1:
                            value = ByteBuffer.wrap (extractedData).getFloat ();
                            break;
                        case 2:
                            value = ByteBuffer.wrap (extractedData).getDouble ();
                            break;
                        default:
                            throw new NotSupportedException(null);
                    }
                    break;
                case BitString:
                    start = parameterCode.Pfc == 0 ? 1 : 0;
                    int length = start > 0 ? extractedData[0] : parameterCode.Pfc;
                    boolean [] bitArray = new boolean [length];
                    copyByteArrayToBitArray (extractedData,
                                             start,
                                             bitArray,
                                             length);
                    value = bitArray;
                    break;
                case OctetString:
                    start = parameterCode.Pfc == 0 ? 1 : 0;
                    byte[] byteArray = new byte[extractedData.length - start];
                    System.arraycopy(extractedData, start, byteArray, 0, byteArray.length);
                    value = byteArray;
                    break;
                case CharacterString:
                    start = parameterCode.Pfc == 0 ? 1 : 0;
                    value = new String (extractedData, start, extractedData.length - start);
                    break;
                case AbsoluteTime:
                    value = extractAbsoluteTime(extractedData, parameterCode.Pfc);
                    break;
                case RelativeTime:
                    value = extractRelativeTime(extractedData, parameterCode.Pfc);
                    break;
                case Deduced:
                    value = extractedData;
                    break;
                default:
                    value = extractedData;
                    break;
            }

            return value;
        }
        catch(Exception ex)
        {
            throw new ArgumentException("Invalid data.");
        }
    }

    /**
     * Copies bits of the byte array as booleans into the boolean array.
     * @param bytes the byte array.
     * @param start the start index in bytes in byte array. (either 0 or 1)
     * @param bits the boolean array.
     * @param num_bools number of booleans to copy into the boolean array.
     */
    private static void copyByteArrayToBitArray (byte [] bytes,
                                                 int start,
                                                 boolean [] bits,
                                                 int num_bools)
    {
        int [] masks = {0b10000000,
                        0b01000000,
                        0b00100000,
                        0b00010000,
                        0b00001000,
                        0b00000100,
                        0b00000010,
                        0b00000001};
        for (int i = 0; i < num_bools; i++) {
            if ((bytes [start + i/8] & masks [i%8]) == 0) bits [i] = false;
            else bits [i] = true;
        }
    }
    
    private static byte[] encodeTimeSpanWithPField(TimeSpan time,
                                                   byte pField,
                                                   boolean isExplicit,
                                                   boolean isSigned) throws ArgumentOutOfRangeException
    {
        int offset = isExplicit ? 1 : 0;
        byte[] data;

        boolean isNegative = time.Ticks < 0;

        // Bit 1 = Time Format
        if((pField & 0x40) == 0)
        {
            // CUC
            int sec = (int)(time.Ticks / (10 * 1000 * 1000));
            int fsec = (int)((time.Ticks % (10 * 1000 * 1000)) * 429.4967296d);   // 2^32 / 10'000'000 = 429.4967296

            int coarceLength = ((pField >> 2) & 0x3) + 1;
            int fineLength = (pField & 0x3);

            if(isSigned && sec > (0xFFFFFFFF >>> (33 - (coarceLength * 8))))
                throw new ArgumentOutOfRangeException("time");
            else if(!isSigned && sec > (0xFFFFFFFF >>> (32 - (coarceLength * 8))))
                throw new ArgumentOutOfRangeException("time");

            data = new byte[coarceLength + fineLength + offset];

            for(int i = 0; i < coarceLength; i++)
                data[i + offset] = (byte)(sec >> ((coarceLength - i - 1) * 8));
            for(int i = 0; i < fineLength; i++)
                data[i + coarceLength + offset] = (byte)(fsec >> ((3 - i) * 8));

            if(isNegative)
            {
                long value = 0;
                for(int i = offset; i < data.length; i++)
                    value |= (long)data[i] << ((data.length - i - 1) * 8);
                value = (0xFFFFFFFFFFFFFFFFl - value) + 1;
                for(int i = offset; i < data.length; i++)
                    data[i] = (byte)(value >>> ((data.length - i - 1) * 8));
            }
        }
        else
        {
            // CDS
            boolean is24BitDay = (pField >> 2) == 1;
            boolean hasUSec = (pField & 0x3) == 1;

            data = new byte[(is24BitDay ? 3 : 2) + 4 + (hasUSec ? 2 : 0) + offset];

            int days = (int)time.Days;
            int msec = (int)((time.Ticks / TimeSpan.TicksPerMillisecond) % (TimeSpan.TicksPerDay / TimeSpan.TicksPerMillisecond));

            int i = offset;
            if(is24BitDay)
                data[i++] = (byte)(days >>> 16);
            data[i++] = (byte)(days >>> 8);
            data[i++] = (byte)(days);

            data[i++] = (byte)(msec >>> 24);
            data[i++] = (byte)(msec >>> 16);
            data[i++] = (byte)(msec >>> 8);
            data[i++] = (byte)(msec);

            if(hasUSec)
            {
                short usec = (short)((time.Ticks / 10) % 1000);
                data[i++] = (byte)(usec >> 8);
                data[i++] = (byte)(usec);
            }
        }

        if(isExplicit)
            data[0] = pField;
        return data;
    }
    
    private static TimeSpan extractAbsoluteTime(byte[] data, int pfc)
    {
        if(pfc == 0)
            return extractTime(data, data[0], true, false);
        else if(pfc == 1)
            return extractTime(data, (byte)0x48, false, false); // 0b01001000 CDS-Level2-16bit-ms
        else if(pfc == 2)
            return extractTime(data, (byte)0x49, false, false); // 0b01001001 CDS-Level2-16bit-us
        else
            return extractTime(data, (byte)(0x20 + pfc - 3), false, false);
    }

    private static TimeSpan extractRelativeTime(byte[] data, int pfc)
    {
        if(pfc == 0)
            return extractTime(data, data[0], true, data[1] > 0x7F);
        else
            return extractTime(data, (byte)(0x20 + pfc - 1), false, data[0] > 0x7F);
    }

    private static TimeSpan extractTime(byte[] data,
                                        byte pField,
                                        boolean isExplicit,
                                        boolean isSigned)
    {
        int offset = isExplicit ? 1 : 0;

        // Bit 1 = Time Format
        if((pField & 0x40) == 0)
        {
            // CUC
            int sign = 1;
            if(isSigned && data[offset] > 0x7F) // Signed and negative
            {
                long value = 0;
                for(int i = offset; i < data.length; i++)
                    value |= (long)data[i] << ((data.length - i - 1) * 8);
                value = (0xFFFFFFFFFFFFFFFFl - value) + 1;  // 2's complement
                for(int i = offset; i < data.length; i++)
                    data[i] = (byte)(value >> ((data.length - i - 1) * 8));
                sign = -1;
            }

            int coarceLength = ((pField >> 2) & 0x3) + 1;
            int fineLength = (pField & 0x3);

            int sec = 0;
            int fsec = 0;
            for(int i = 0; i < coarceLength; i++)
                sec |= ((int)data[i + offset] << ((coarceLength - i - 1) * 8));
            for(int i = 0; i < fineLength; i++)
                fsec |= ((int)data[i + coarceLength + offset] << ((3 - i) * 8));

            return new TimeSpan((((long)sec * (10 * 1000 * 1000)) + (long)(fsec / 429.4967296d)) * sign); // 2^32 / 10'000'000 = 429.4967296
        }
        else
        {
            // CDS
            boolean is24BitDay = (pField >> 2) == 1;
            boolean hasUSec = (pField & 0x3) == 1;

            int days = 0;
            int msec = 0;
            short usec = 0;

            int i = offset;
            if(is24BitDay)
                days |= (int)(data[i++] << 16);
            days |= (int)(data[i++] << 8);
            days |= data[i++];

            msec |= (int)(data[i++] << 24);
            msec |= (int)(data[i++] << 16);
            msec |= (int)(data[i++] << 8);
            msec |= data[i++];

            if(hasUSec)
            {
                usec |= (short)(data[i++] << 8);
                usec |= data[i++];
            }

            return new TimeSpan((days * TimeSpan.TicksPerDay) + (msec * TimeSpan.TicksPerMillisecond) + (usec * 10));
        }
    }
}
