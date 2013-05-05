package data;

import data_Ccsds.Function.ArgumentNullException;
import data_Ccsds.Packets.ArgumentException;

/// <summary>This helper class provides methods to convert data to/from host/network byte order.</summary>
	/// <remarks>Network order is big-endian.</remarks>
	public class ByteOrderConverter
	{
		/// <summary>Converts a value from host byte order to network byte order.</summary>
		/// <param name="value">The value to convert, in host byte order.</param>
		/// <returns>The value after conversion, in network byte order.</returns>
		public static short HostToNetworkOrder(short value)
		{
		    return value;
		}

		/// <summary>Converts a value from host byte order to network byte order.</summary>
		/// <param name="value">The value to convert, in host byte order.</param>
		/// <returns>The value after conversion, in network byte order.</returns>
		public static int HostToNetworkOrder(int value)
		{
		    return value;
		}

		/// <summary>Converts a value from host byte order to network byte order.</summary>
		/// <param name="value">The value to convert, in host byte order.</param>
		/// <returns>The value after conversion, in network byte order.</returns>
		public static long HostToNetworkOrder(long value)
		{
		    return value;
		}

		/// <summary>Converts a value from network byte order to host byte order.</summary>
		/// <param name="value">The value to convert, in network byte order.</param>
		/// <returns>The value after conversion, in host byte order.</returns>
		public static short NetworkToHostOrder(short value)
		{
		    return value;
		}

		/// <summary>Converts a value from network byte order to host byte order.</summary>
		/// <param name="value">The value to convert, in network byte order.</param>
		/// <returns>The value after conversion, in host byte order.</returns>
		public static int NetworkToHostOrder(int value)
		{
		    return value;
		}

		/// <summary>Converts a value from network byte order to host byte order.</summary>
		/// <param name="value">The value to convert, in network byte order.</param>
		/// <returns>The value after conversion, in host byte order.</returns>
		public static long NetworkToHostOrder(long value)
		{
		    return value;
		}

/*		#region Add primitive value to a network order (big-endian) byte array*/
		private static void ensureValidArray(byte[] dstArray, int offset, int dataSize) throws ArgumentNullException, ArgumentException
		{
			if(dstArray == null)
				throw new ArgumentNullException("dstArray");
			if((offset + dataSize) > dstArray.length)
				throw new ArgumentException("The array is too small to contain the data at specified offset.");
		}

		/// <summary>Copy a value in host byte order to an byte array in network byte order.</summary>
		/// <param name="dstArray">Destination array, in network byte order.</param>
		/// <param name="offset">Offset inside the destination array at which to copy the value.</param>
		/// <param name="value">The value to copy, in host byte order.</param>
		/// <remarks>The byte order has not impact on this method, but the method is present
		/// to have the full range of integer primitives for consistency.</remarks>
		public static void CopyValueNetworkOrder(byte[] dstArray, int offset, byte value) throws ArgumentNullException, ArgumentException
		{
			ensureValidArray(dstArray, offset, 1);
			dstArray[offset] = value;
		}

		/// <summary>Copy a value in host byte order to an byte array in network byte order.</summary>
		/// <param name="dstArray">Destination array, in network byte order.</param>
		/// <param name="offset">Offset inside the destination array at which to copy the value.</param>
		/// <param name="value">The value to copy, in host byte order.</param>
		public static void CopyValueNetworkOrder(byte[] dstArray, int offset, short value) throws ArgumentNullException, ArgumentException
		{
			ensureValidArray(dstArray, offset, 2);
			dstArray[offset + 0] = ((byte)(value >> 8));
			dstArray[offset + 1] = ((byte)value);
			//CopyValueNetworkOrder(dstArray, offset, BitConverter.GetBytes(HostToNetworkOrder(value)));
		}

		/// <summary>Copy a value in host byte order to an byte array in network byte order.</summary>
		/// <param name="dstArray">Destination array, in network byte order.</param>
		/// <param name="offset">Offset inside the destination array at which to copy the value.</param>
		/// <param name="value">The value to copy, in host byte order.</param>
		public static void CopyValueNetworkOrder(byte[] dstArray, int offset, int value) throws ArgumentNullException, ArgumentException
		{
			ensureValidArray(dstArray, offset, 4);
			dstArray[offset + 0] = ((byte)(value >> 24));
			dstArray[offset + 1] = ((byte)(value >> 16));
			dstArray[offset + 2] = ((byte)(value >> 8));
			dstArray[offset + 3] = ((byte)value);
			//CopyValueNetworkOrder(dstArray, offset, BitConverter.GetBytes(HostToNetworkOrder(value)));
		}

		/// <summary>Copy a value in host byte order to an byte array in network byte order.</summary>
		/// <param name="dstArray">Destination array, in network byte order.</param>
		/// <param name="offset">Offset inside the destination array at which to copy the value.</param>
		/// <param name="value">The value to copy, in host byte order.</param>
		public static void CopyValueNetworkOrder(byte[] dstArray, int offset, long value) throws ArgumentNullException, ArgumentException
		{
			ensureValidArray(dstArray, offset, 8);
			dstArray[offset + 0] = ((byte)(value >> 56));
			dstArray[offset + 1] = ((byte)(value >> 48));
			dstArray[offset + 2] = ((byte)(value >> 40));
			dstArray[offset + 3] = ((byte)(value >> 32));
			dstArray[offset + 4] = ((byte)(value >> 24));
			dstArray[offset + 5] = ((byte)(value >> 16));
			dstArray[offset + 6] = ((byte)(value >> 8));
			dstArray[offset + 7] = ((byte)value);
			//CopyValueNetworkOrder(dstArray, offset, BitConverter.GetBytes(HostToNetworkOrder(value)));
		}

		/// <summary>Copy a value in host byte order to an byte array in network byte order.</summary>
		/// <param name="dstArray">Destination array, in network byte order.</param>
		/// <param name="offset">Offset inside the destination array at which to copy the value.</param>
		/// <param name="value">The value to copy, in host byte order.</param>
		/// <exception cref="ArgumentNullException">dstArray is a null reference.</exception>
		/// <exception cref="IndexOutOfRangeException">offset is negative or too big for the value to fit in the array.</exception>
		public static void CopyValueNetworkOrder(byte[] dstArray, int offset, byte[] value) throws ArgumentNullException, IndexOutOfRangeException
		{
			if(dstArray == null)
				throw new ArgumentNullException("dstArray");
			if(offset > (dstArray.length - value.length))
				throw new IndexOutOfRangeException();

			for (int i = 0; i < value.length; i++) {
			    dstArray [offset + i] = value [i];
			}
		}

/*		#region Network order (big-endian) byte array to primitive values*/
		/// <summary>Get a value in host byte order from a byte array in network byte order.</summary>
		/// <param name="srcArray">Source array, in network byte order.</param>
		/// <param name="offset">Offset in bytes inside the source array at which to get the value.</param>
		/// <returns>The value, in host byte order.</returns>
		/// <remarks>The byte order has not impact on this method, but the method is present
		/// to have the full range of integer primitives for consistency.</remarks>
		public static byte GetByte(byte[] srcArray, int offset)
		{
			return srcArray[offset];
		}

		/// <summary>Get a value in host byte order from a byte array in network byte order.</summary>
		/// <param name="srcArray">Source array, in network byte order.</param>
		/// <param name="offset">Offset in bytes inside the source array at which to get the value.</param>
		/// <returns>The value, in host byte order.</returns>
		public static short GetInt16(byte[] srcArray, int offset)
		{
			return NetworkToHostOrder(BitConverter.ToInt16(srcArray, offset));
		}

		/// <summary>Get a value in host byte order from a byte array in network byte order.</summary>
		/// <param name="srcArray">Source array, in network byte order.</param>
		/// <param name="offset">Offset in bytes inside the source array at which to get the value.</param>
		/// <returns>The value, in host byte order.</returns>
		public static int GetInt32(byte[] srcArray, int offset)
		{
			return NetworkToHostOrder(BitConverter.ToInt32(srcArray, offset));
		}

		/// <summary>Get a value in host byte order from a byte array in network byte order.</summary>
		/// <param name="srcArray">Source array, in network byte order.</param>
		/// <param name="offset">Offset in bytes inside the source array at which to get the value.</param>
		/// <returns>The value, in host byte order.</returns>
		public static long GetInt64(byte[] srcArray, int offset)
		{
			return NetworkToHostOrder(BitConverter.ToInt64(srcArray, offset));
		}

		/// <summary>Get a value in host byte order from a byte array in network byte order.</summary>
		/// <param name="srcArray">Source array, in network byte order.</param>
		/// <returns>The value, in host byte order.</returns>
		/// <remarks>The byte order has not impact on this method, but the method is present
		/// to have the full range of integer primitives for consistency.</remarks>
		public static byte GetByte(byte[] srcArray)
		{
			return srcArray[0];
		}

		/// <summary>Get a value in host byte order from a byte array in network byte order.</summary>
		/// <param name="srcArray">Source array, in network byte order.</param>
		/// <returns>The value, in host byte order.</returns>
		public static short GetInt16(byte[] srcArray)
		{
			return GetInt16(srcArray, 0);
		}
		
		/// <summary>Get a value in host byte order from a byte array in network byte order.</summary>
		/// <param name="srcArray">Source array, in network byte order.</param>
		/// <returns>The value, in host byte order.</returns>
		public static int GetInt32(byte[] srcArray)
		{
			return GetInt32(srcArray, 0);
		}

		/// <summary>Get a value in host byte order from a byte array in network byte order.</summary>
		/// <param name="srcArray">Source array, in network byte order.</param>
		/// <returns>The value, in host byte order.</returns>
		public static long GetInt64(byte[] srcArray)
		{
			return GetInt64(srcArray, 0);
		}
	}
