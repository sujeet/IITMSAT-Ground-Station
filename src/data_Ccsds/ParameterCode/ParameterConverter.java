package data_Ccsds.ParameterCode;

/// <summary>Provides methods to encode/decode parameter code values to/from .NET values.</summary>
public static class ParameterConverter
{
	// Temp buffer for simple types conversion, per thread to avoid locking
	[ThreadStatic]
			private static byte[] tempBuffer = null;
	private static byte[] TempBuffer
	{
		get
		{
			if(tempBuffer == null)
				tempBuffer = new byte[8];
			return tempBuffer;
		}
	}

	// Encoding objet for string conversions, per thread to avoid locking
	[ThreadStatic]
			private static Encoding encoding = null;
	private static Encoding Encoding
	{
		get
		{
			if(encoding == null)
				#if SILVERLIGHT
				encoding = Encoding.UTF8; // ASCII not supported on Silverlight
			#else
				encoding = Encoding.ASCII;
			#endif
			return encoding;
		}
	}


	/// <summary>Encodes an object using the specified parameter code. A return value indicates whether the conversion succeeded.</summary>
	/// <param name="value">Object to encode.</param>
	/// <param name="parameterCode">Parameter code to encode the value to.</param>
	/// <param name="result">When this method returns, contains the value encoded as a byte array, if the conversion succeeded, or <c>null</c> if the conversion failed.</param>
	/// <returns><c>true</c> if s was converted successfully; otherwise, <c>false</c>.</returns>
	public static bool TryEncodeValue(object value, ParameterCode parameterCode, out byte[] result)
	{
		try
		{
			result = EncodeValue(value, parameterCode);
			return true;
		}
		catch
		{
			result = null;
			return false;
		}
	}

	/// <summary>Encodes an object using the specified parameter code.</summary>
	/// <param name="value">Object to encode.</param>
	/// <param name="parameterCode">Parameter code to encode the value to.</param>
	/// <returns>The value encoded as a byte array.</returns>
	// TODO: Overflow checking
	public static byte[] EncodeValue(object value, ParameterCode parameterCode)
	{
		object o;
		Type type = GetType(parameterCode);
		try
		{
			o = Convert.ChangeType(value, type, Thread.CurrentThread.CurrentCulture);
		}
		catch(InvalidCastException ex)
		{
			throw new ArgumentException(value.GetType().FullName + " is not compatible with specified parameter code (" + parameterCode + "). Need " + type.FullName + " or compatible.", ex);
		}

		byte[] data;
		switch(parameterCode.Ptc)
		{
		case PtcType.Boolean:
			data = new byte[1];
			data[0] = Convert.ToBoolean(o) ? (byte)(1 << 7) : (byte)0;
			break;
		case PtcType.Enumerated:
			data = BitConverter.GetBytes(ByteOrderConverter.HostToNetworkOrder((int)(((uint)o) << (32 - GetBitsSize(parameterCode)))));
			break;
		case PtcType.UnsignedInteger:
			data = BitConverter.GetBytes(ByteOrderConverter.HostToNetworkOrder((long)(((ulong)o) << (64 - GetBitsSize(parameterCode)))));
			break;
		case PtcType.SignedInteger:
			data = BitConverter.GetBytes(ByteOrderConverter.HostToNetworkOrder(((long)o) << (64 - GetBitsSize(parameterCode))));
			break;
		case PtcType.Real:
			switch(parameterCode.Pfc)
			{
			case 1:
				data = new byte[4];
				ByteOrderConverter.CopyValueNetworkOrder(data, 0, BitReinterpreterSingle.Convert((float)o));
				break;
			case 2:
				data = new byte[8];
				ByteOrderConverter.CopyValueNetworkOrder(data, 0, BitReinterpreterDouble.Convert((double)o));
				break;
			default:
				throw new NotSupportedException();
			}
			break;
		case PtcType.BitString:
			BitArray bits = (BitArray)o;
			if(parameterCode.Pfc == 0)
			{
				int length = Math.Min(bits.Length, byte.MaxValue);
				data = new byte[GetStorageSize(length) + 1];
				data[0] = (byte)length;
				copyBitArrayToByteArray(bits, data, 1, length);
			}
			else
			{
				data = new byte[GetStorageSize(parameterCode.Pfc)];
				copyBitArrayToByteArray(bits, data, 0, parameterCode.Pfc);
			}
			break;
		case PtcType.OctetString:
			data = createVariableLengthData((byte[])o, parameterCode);
			break;
		case PtcType.CharacterString:
			data = createVariableLengthData(Encoding.GetBytes((string)o), parameterCode);
			break;
		case PtcType.AbsoluteTime:
			TimeSpan absoluteTime = (TimeSpan)o;
			if(parameterCode.Pfc == 0)
				data = encodeTimeSpanWithPField(absoluteTime, 0x2F, true, false); // 0b00101111 CUC-Level2-full  (PFC=18)
				else if(parameterCode.Pfc == 1)
					data = encodeTimeSpanWithPField(absoluteTime, 0x48, false, false); // 0b01001000 CDS-Level2-16bit-ms
				else if(parameterCode.Pfc == 2)
					data = encodeTimeSpanWithPField(absoluteTime, 0x49, false, false); // 0b01001001 CDS-Level2-16bit-us
				else
					data = encodeTimeSpanWithPField(absoluteTime, (byte)(0x20 + parameterCode.Pfc - 3), false, false);
			break;
		case PtcType.RelativeTime:
			TimeSpan relativeTime = (TimeSpan)o;
			if(parameterCode.Pfc == 0)
				data = encodeTimeSpanWithPField(relativeTime, 0x2F, true, true); // 0b00101111 CUC-Level2-full  (PFC=18)
			else
				data = encodeTimeSpanWithPField(relativeTime, (byte)(0x20 + parameterCode.Pfc - 1), false, true);
			break;
		case PtcType.Deduced:
			data = (byte[])o;
			break;
		default:
			throw new NotSupportedException();
		}

		// Do we know the real size
		int size = GetBitsSize(parameterCode, data, 0);
		if(size > 0)
		{
			int byteSize = GetStorageSize(size);
			// Using too much bytes
			if(data.Length > byteSize)
			{
				byte[] shortData = new byte[byteSize];
				Buffer.BlockCopy(data, 0, shortData, 0, byteSize);
				data = shortData;
			}
		}

		return data;
	}

	private static byte[] encodeTimeSpanWithPField(TimeSpan time, byte pField, bool isExplicit, bool isSigned)
	{
		int offset = isExplicit ? 1 : 0;
		byte[] data;

		bool isNegative = time.Ticks < 0;
		time = time.Duration();

		// Bit 1 = Time Format
		if((pField & 0x40) == 0)
		{
			// CUC
			uint sec = (uint)(time.Ticks / (10 * 1000 * 1000));
			uint fsec = (uint)((time.Ticks % (10 * 1000 * 1000)) * 429.4967296d);   // 2^32 / 10'000'000 = 429.4967296

			int coarceLength = ((pField >> 2) & 0x3) + 1;
			int fineLength = (pField & 0x3);

			if(isSigned && sec > (uint.MaxValue >> (33 - (coarceLength * 8))))
				throw new ArgumentOutOfRangeException("time");
			else if(!isSigned && sec > (uint.MaxValue >> (32 - (coarceLength * 8))))
				throw new ArgumentOutOfRangeException("time");

			data = new byte[coarceLength + fineLength + offset];

			for(int i = 0; i < coarceLength; i++)
				data[i + offset] = (byte)(sec >> ((coarceLength - i - 1) * 8));
			for(int i = 0; i < fineLength; i++)
				data[i + coarceLength + offset] = (byte)(fsec >> ((3 - i) * 8));

			if(isNegative)
			{
				ulong value = 0;
				for(int i = offset; i < data.Length; i++)
					value |= (ulong)data[i] << ((data.Length - i - 1) * 8);
				value = (ulong.MaxValue - value) + 1;
				for(int i = offset; i < data.Length; i++)
					data[i] = (byte)(value >> ((data.Length - i - 1) * 8));
			}
		}
		else
		{
			// CDS
			bool is24BitDay = (pField >> 2) == 1;
			bool hasUSec = (pField & 0x3) == 1;

			data = new byte[(is24BitDay ? 3 : 2) + 4 + (hasUSec ? 2 : 0) + offset];

			uint days = (uint)time.Days;
			uint msec = (uint)((time.Ticks / TimeSpan.TicksPerMillisecond) % (TimeSpan.TicksPerDay / TimeSpan.TicksPerMillisecond));

			int i = offset;
			if(is24BitDay)
				data[i++] = (byte)(days >> 16);
			data[i++] = (byte)(days >> 8);
			data[i++] = (byte)(days);

			data[i++] = (byte)(msec >> 24);
			data[i++] = (byte)(msec >> 16);
			data[i++] = (byte)(msec >> 8);
			data[i++] = (byte)(msec);

			if(hasUSec)
			{
				ushort usec = (ushort)((time.Ticks / 10) % 1000);
				data[i++] = (byte)(usec >> 8);
				data[i++] = (byte)(usec);
			}
		}

		if(isExplicit)
			data[0] = pField;
		return data;
	}

	/// <summary>Insert a value into a byte array.</summary>
	/// <param name="data">Byte array to insert the value into.</param>
	/// <param name="index">Index in bits at which the value must be inserted.</param>
	/// <param name="value">Value object to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted</returns>
	// TODO: Rewrite object insertion
	public static int InsertValue(byte[] data, int index, object value, ParameterCode parameterCode)
	{
		int bitsSize = ParameterConverter.GetBitsSize(parameterCode);
		byte[] encodedData = ParameterConverter.EncodeValue(value, parameterCode);
		// HACK:
		if(bitsSize == -1)
			bitsSize = encodedData.Length * 8;
		UnalignedData.InsertValue(data, index, encodedData, bitsSize);
		return bitsSize;
	}

	#region Boolean
	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, bool value, ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		// Don't encode
		case PtcType.NoData:
			return 0;
			// Boolean number
		case PtcType.Boolean:
			byte[] temp = TempBuffer;
			temp[0] = value ? (byte)(1 << 7) : (byte)0;
			UnalignedData.InsertValue(buffer, index, temp, 1);
			return 1;
			// Integral number
		case PtcType.Enumerated:
		case PtcType.UnsignedInteger:
		case PtcType.SignedInteger:
			return InsertValue(buffer, index, 1, parameterCode);
			// Floating-point number
		case PtcType.Real:
			return InsertValue(buffer, index, 1f, parameterCode);
		default:
			throw new NotSupportedException(string.Format("Cannot encode value of type {0} in {1} Parameter Type Code.", value.GetType(), parameterCode.Ptc));
		}
	}
	#endregion

	#region Integral types (-> unsigned * -> unsigned long)
	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, sbyte value, ParameterCode parameterCode)
	{
		return InsertValue(buffer, index, (long)value, parameterCode);
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, byte value, ParameterCode parameterCode)
	{
		return InsertValue(buffer, index, (ulong)value, parameterCode);
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, short value, ParameterCode parameterCode)
	{
		return InsertValue(buffer, index, (long)value, parameterCode);
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, char value, ParameterCode parameterCode)
	{
		return InsertValue(buffer, index, (ulong)value, parameterCode);
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, ushort value, ParameterCode parameterCode)
	{
		return InsertValue(buffer, index, (ulong)value, parameterCode);
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, int value, ParameterCode parameterCode)
	{
		return InsertValue(buffer, index, (long)value, parameterCode);
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, uint value, ParameterCode parameterCode)
	{
		return InsertValue(buffer, index, (ulong)value, parameterCode);
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, long value, ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		// Don't encode
		case PtcType.NoData:
			return 0;
			// Boolean number
		case PtcType.Boolean:
			if(value != 0 && value != 1)
				throw new OverflowException("Boolean values must be either 0 or 1.");
			return InsertValue(buffer, index, value == 1, parameterCode);
			// Integral number
		case PtcType.Enumerated:
		case PtcType.UnsignedInteger:
			if(value < 0)
				throw new OverflowException(string.Format("Negative values {0} cannot be encoded as UnsignedInteger.", value));
			else
				return InsertValue(buffer, index, (ulong)value, parameterCode);
		case PtcType.SignedInteger:
			int bitLength = GetBitsSize(parameterCode);
			int signedBitLength = bitLength - 1; // 1 bit is for sign

			// Check overflow if smaller than 63-bit
			if((bitLength < 63) && (value >= (1L << signedBitLength) || value < (-1L << signedBitLength)))
				throw new OverflowException(string.Format("Value {0} is too big to be encoded with PTC={1} and PFC={2}.", value, parameterCode.Ptc, parameterCode.Pfc));

			// If value doesn't overflow, just truncate it keeps its value  (1111001@8bits == 1001@4bits)
			byte[] temp = TempBuffer;
			ByteOrderConverter.CopyValueNetworkOrder(temp, 0, value << (64 - bitLength));
			UnalignedData.InsertValue(buffer, index, temp, bitLength);

			return bitLength;
			// Floating-point number
		case PtcType.Real:
			return InsertValue(buffer, index, (double)value, parameterCode);
			// Anything else is not supported
		default:
			throw new NotSupportedException(string.Format("Cannot encode value of type {0} in {1} Parameter Type Code.", value.GetType(), parameterCode.Ptc));
		}
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, ulong value, ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		// Don't encode
		case PtcType.NoData:
			return 0;
			// Boolean number
		case PtcType.Boolean:
			if(value != 0 && value != 1)
				throw new OverflowException("Boolean values must be either 0 or 1.");
			return InsertValue(buffer, index, value == 1, parameterCode);
			// Integral number
		case PtcType.Enumerated:
		case PtcType.UnsignedInteger:
			int bitLength = GetBitsSize(parameterCode);
			if((bitLength < 64) && value >= (1UL << bitLength)) // Check overflow if smaller than 64-bit
				throw new OverflowException(string.Format("Value {0} is too big to be encoded with PTC={1} and PFC={2}.", value, parameterCode.Ptc, parameterCode.Pfc));
			byte[] temp = TempBuffer;
			ByteOrderConverter.CopyValueNetworkOrder(temp, 0, value << (64 - bitLength));
			UnalignedData.InsertValue(buffer, index, temp, bitLength);
			return bitLength;
		case PtcType.SignedInteger:
			if(value >= (1UL << 63))
				throw new OverflowException(string.Format("Value {0} is too big to be encoded with PTC={1} and PFC={2}.", value, parameterCode.Ptc, parameterCode.Pfc));
			else
				return InsertValue(buffer, index, (long)value, parameterCode);
			// Floating-point number
		case PtcType.Real:
			return InsertValue(buffer, index, (double)value, parameterCode);
			// Anything else is not supported
		default:
			throw new NotSupportedException(string.Format("Cannot encode value of type {0} in {1} Parameter Type Code.", value.GetType(), parameterCode.Ptc));
		}
	}
	#endregion

	#region Floating-point types
	[System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Explicit)]
			private struct BitReinterpreterSingle
			{
		public static int Convert(float f)
		{
			BitReinterpreterSingle br = new BitReinterpreterSingle(f);
			return br.i;
		}

		public static float Convert(int i)
		{
			BitReinterpreterSingle br = new BitReinterpreterSingle(i);
			return br.f;
		}

		[System.Runtime.InteropServices.FieldOffset(0)]
				float f;
		[System.Runtime.InteropServices.FieldOffset(0)]
				int i;

		private BitReinterpreterSingle(float f)
		{ this.i = 0; this.f = f; }

		private BitReinterpreterSingle(int i)
		{ this.f = 0; this.i = i; }
			}

	[System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Explicit)]
			private struct BitReinterpreterDouble
			{
		public static long Convert(double d)
		{
			BitReinterpreterDouble br = new BitReinterpreterDouble(d);
			return br.l;
		}

		public static double Convert(long l)
		{
			BitReinterpreterDouble br = new BitReinterpreterDouble(l);
			return br.d;
		}

		[System.Runtime.InteropServices.FieldOffset(0)]
				double d;
		[System.Runtime.InteropServices.FieldOffset(0)]
				long l;

		private BitReinterpreterDouble(double d)
		{ this.l = 0; this.d = d; }

		private BitReinterpreterDouble(long l)
		{ this.d = 0; this.l = l; }
			}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, float value, ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		// Don't encode
		case PtcType.NoData:
			return 0;
			// Floating-point number
		case PtcType.Real:
			if(parameterCode.Pfc == 1) // Float
			{
				byte[] temp = TempBuffer;
				ByteOrderConverter.CopyValueNetworkOrder(temp, 0, BitReinterpreterSingle.Convert(value));
				UnalignedData.InsertValue(buffer, index, temp, 64);
				return 64;
			}
			else if(parameterCode.Pfc == 2) // Double
				return InsertValue(buffer, index, (double)value, parameterCode);
			else
				throw new NotSupportedException("Only IEEE standard precision formats are supported for Real Parameter Type Code.");
			// Anything else is not supported
		default:
			throw new NotSupportedException(string.Format("Cannot encode value of type {0} in {1} Parameter Format Code.", value.GetType(), parameterCode.Ptc));
		}
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, double value, ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		// Don't encode
		case PtcType.NoData:
			return 0;
			// Floating-point number
		case PtcType.Real:
			if(parameterCode.Pfc == 1) // Float
				return InsertValue(buffer, index, (float)value, parameterCode);
			else if(parameterCode.Pfc == 2) // Double
			{
				byte[] temp = TempBuffer;
				ByteOrderConverter.CopyValueNetworkOrder(temp, 0, BitReinterpreterDouble.Convert(value));
				UnalignedData.InsertValue(buffer, index, temp, 64);
				return 64;
			}
			else
				throw new NotSupportedException("Only IEEE standard precision formats are supported for Real Parameter Type Code.");
			// Anything else is not supported
		default:
			throw new NotSupportedException(string.Format("Cannot encode value of type {0} in {1} Parameter Format Code.", value.GetType(), parameterCode.Ptc));
		}
	}
	#endregion

	#region Time types
	#endregion

	#region String
	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, string value, ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		case PtcType.NoData:
			return 0;
		case PtcType.Boolean:
			return InsertValue(buffer, index, bool.Parse(value), parameterCode);
		case PtcType.Enumerated:
		case PtcType.UnsignedInteger:
			return InsertValue(buffer, index, ulong.Parse(value), parameterCode);
		case PtcType.SignedInteger:
			return InsertValue(buffer, index, long.Parse(value), parameterCode);
		case PtcType.Real:
			if(parameterCode.Pfc == 1) // Float
			return InsertValue(buffer, index, float.Parse(value), parameterCode);
			else
				return InsertValue(buffer, index, double.Parse(value), parameterCode);
		case PtcType.BitString:
			throw new NotImplementedException();
		case PtcType.OctetString:
			throw new NotImplementedException();
		case PtcType.CharacterString:
			return insertCharacterString(buffer, index, value, parameterCode.Pfc);
		default:
			throw new NotSupportedException(string.Format("Cannot encode value of type {0} in {1} Parameter Format Code.", value.GetType(), parameterCode.Ptc));
		}
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, char[] value, ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		case PtcType.NoData:
			return 0;
		case PtcType.CharacterString:
			return insertCharacterString(buffer, index, value, parameterCode.Pfc);
		default:
			throw new NotSupportedException(string.Format("Cannot encode value of type {0} in {1} Parameter Format Code.", value.GetType(), parameterCode.Ptc));
		}
	}

	private static int insertVariableSizeLength(byte[] data, int index, int length)
	{
		byte[] temp = TempBuffer;
		// TODO: Get size PC from MIB
		// TODO: Check that data length is not longer (throw)
		ByteOrderConverter.CopyValueNetworkOrder(temp, 0, (byte)length);
		UnalignedData.InsertValue(data, index, temp, 8);
		return 8;
	}

	// TODO: Refractor, code duplicated for char[]
	private static int insertCharacterString(byte[] buffer, int index, string value, ushort pfc)
	{
		int bitLength = 0;
		int availableSpace;

		int byteCount = Encoding.GetByteCount(value);

		if(pfc == 0) // Insert variable size 
		{
			bitLength += insertVariableSizeLength(buffer, index, byteCount);
			availableSpace = byteCount;
		}
		else
		{
			availableSpace = pfc;
			if(byteCount > pfc)
				throw new ArgumentException(string.Format("Character string is too long to be encoded with PFC={0} ({1} characters).", pfc, byteCount), "value");
		}

		index += bitLength;
		if((index % 8) == 0) // Aligned insertion
		{
			int encodedBytes = encoding.GetBytes(value, 0, value.Length, buffer, index / 8);
			Debug.Assert(encodedBytes == byteCount);
			if(encodedBytes < availableSpace) // Set remaining bytes to 0
				Array.Clear(buffer, (index / 8) + encodedBytes, availableSpace - encodedBytes);
		}
		else // Unaligned
		{
			byte[] charData = new byte[availableSpace]; // Use intermediate buffer (with padding already)
			int encodedBytes = encoding.GetBytes(value, 0, value.Length, charData, 0);
			Debug.Assert(encodedBytes == byteCount);
			UnalignedData.InsertValue(buffer, index, charData, charData.Length * 8);
		}
		bitLength += (availableSpace * 8);
		return bitLength;
	}

	private static int insertCharacterString(byte[] buffer, int index, char[] value, ushort pfc)
	{
		int bitLength = 0;
		int availableSpace;

		int byteCount = Encoding.GetByteCount(value);

		if(pfc == 0) // Insert variable size 
		{
			bitLength += insertVariableSizeLength(buffer, index, byteCount);
			availableSpace = byteCount;
		}
		else
		{
			availableSpace = pfc;
			if(byteCount > pfc)
				throw new ArgumentException(string.Format("Character string is too long to be encoded with PFC={0} ({1} characters).", pfc, byteCount), "value");
		}

		index += bitLength;
		if((index % 8) == 0) // Aligned insertion
		{
			int encodedBytes = encoding.GetBytes(value, 0, value.Length, buffer, index / 8);
			Debug.Assert(encodedBytes == byteCount);
			if(encodedBytes < availableSpace) // Set remaining bytes to 0
				Array.Clear(buffer, (index / 8) + encodedBytes, availableSpace - encodedBytes);
		}
		else // Unaligned
		{
			byte[] charData = new byte[availableSpace]; // Use intermediate buffer (with padding already)
			int encodedBytes = encoding.GetBytes(value, 0, value.Length, charData, 0);
			Debug.Assert(encodedBytes == byteCount);
			UnalignedData.InsertValue(buffer, index, charData, charData.Length * 8);
		}
		bitLength += (availableSpace * 8);
		return bitLength;
	}
	#endregion

	#region Bit/byte array
	private static int insertOctetString(byte[] buffer, int index, byte[] value, ushort pfc)
	{
		int bitLength = 0;
		int availableSpace;

		if(pfc == 0) // Insert variable size 
		{
			bitLength += insertVariableSizeLength(buffer, index, value.Length);
			availableSpace = value.Length;
		}
		else
		{
			if(value.Length > pfc)
				throw new ArgumentException(string.Format("Octet string is too long to be encoded with PFC={0} ({1} octets).", pfc, value.Length), "value");
			availableSpace = pfc;
		}

		index += bitLength;
		UnalignedData.InsertValue(buffer, index, value, value.Length * 8);
		if(value.Length < availableSpace)
		{
			// Pad unused space with 0s
			// TODO: Without new allocation
			byte[] padding = new byte[availableSpace - value.Length];
			UnalignedData.InsertValue(buffer, index + (value.Length * 8), padding, padding.Length * 8);
		}

		bitLength += (availableSpace * 8);
		return bitLength;
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, byte[] value, ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		case PtcType.NoData:
			return 0;
		case PtcType.BitString:
			throw new NotImplementedException();
		case PtcType.OctetString:
		case PtcType.CharacterString:
			return insertOctetString(buffer, index, value, parameterCode.Pfc);
		case PtcType.Deduced:
		case PtcType.ScoeCommand:
		case PtcType.Function:
		case PtcType.Telecommand:
			UnalignedData.InsertValue(buffer, index, value, value.Length * 8);
			return value.Length * 8;
		default:
			int bitSize = GetBitsSize(parameterCode);
			if(bitSize > 0)
			{
				UnalignedData.InsertValue(buffer, index, value, bitSize);
				return bitSize;
			}
			else
				throw new NotSupportedException(string.Format("Cannot insert value of type {0} as {1} Parameter Type Code.", value.GetType(), parameterCode.Ptc));
		}
	}

	/// <summary>Insert a value into a buffer with no alignment requirement.</summary>
	/// <param name="buffer">Buffer in which to insert the value.</param>
	/// <param name="index">Bit index at which the value must be inserted.</param>
	/// <param name="value">Value to insert.</param>
	/// <param name="parameterCode">Parameter code of the value to insert.</param>
	/// <returns>Number of bits inserted.</returns>
	public static int InsertValue(byte[] buffer, int index, BitArray value, ParameterCode parameterCode)
	{
		throw new NotImplementedException();
	}
	#endregion

	/*
		public static int InsertValue<T>(byte[] data, int index, T value, ParameterCode parameterCode)
		{
			byte[] temp = TempBuffer;
			switch(parameterCode.Ptc)
			{
				case PtcType.NoData:
					return 0;
				case PtcType.Boolean:
					temp[0] = Convert.ToBoolean(value) ? (byte)(1 << 7) : (byte)0;
					break;
				case PtcType.Enumerated:
					break;
				case PtcType.UnsignedInteger:
					break;
				case PtcType.SignedInteger:
					break;
				case PtcType.Real:
					break;
				case PtcType.BitString:
					break;
				case PtcType.OctetString:
					break;
				case PtcType.CharacterString:
					break;
				case PtcType.AbsoluteTime:
					break;
				case PtcType.RelativeTime:
					break;
				case PtcType.Deduced:
					break;
				case PtcType.ObtCounter:
					break;
				case PtcType.ScoeCommand:
					break;
				case PtcType.Function:
					break;
				case PtcType.Telecommand:
					break;
				default:
					break;
			}

			throw new NotImplementedException();
			return 0;
		}*/

	private static void copyBitArrayToByteArray(BitArray bitArray, byte[] data, int index, int bitCount)
	{
		for(int i = 0; i < bitCount && i < bitArray.Count; i++)
		{
			if(bitArray[i])//8 * (i / 8) + (7 - (i % 8))])
				data[i / 8 + index] |= (byte)(1 << (7 - (i % 8)));
		}
	}

	private static byte[] createVariableLengthData(byte[] bytes, ParameterCode parameterCode)
	{
		byte[] data;
		if(parameterCode.Pfc == 0)
		{
			data = new byte[Math.Min(bytes.Length, byte.MaxValue) + 1];
			data[0] = (byte)(data.Length - 1);
			Buffer.BlockCopy(bytes, 0, data, 1, data[0]);
		}
		else
		{
			data = new byte[parameterCode.Pfc];
			Buffer.BlockCopy(bytes, 0, data, 0, Math.Min(parameterCode.Pfc, bytes.Length));
		}
		return data;
	}

	/// <summary>Get the .NET type for the specified parameter code.</summary>
	/// <param name="parameterCode">The parameter code.</param>
	/// <returns>A <see cref="System.Type"/> instance.</returns>
	public static Type GetType(ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		case PtcType.NoData:
			return typeof(void);
		case PtcType.Boolean:
			return typeof(bool);
		case PtcType.Enumerated:
			return typeof(UInt32);
		case PtcType.UnsignedInteger:
			return typeof(UInt64);
		case PtcType.SignedInteger:
			return typeof(Int64);
		case PtcType.Real:
			switch(parameterCode.Pfc)
			{
			case 1:
				return typeof(float);
			case 2:
				return typeof(double);
			default:
				throw new NotSupportedException("Parameter code not supported (" + parameterCode + ").");
			}
		case PtcType.BitString:
			return typeof(BitArray);
		case PtcType.OctetString:
			return typeof(byte[]);
		case PtcType.CharacterString:
			return typeof(string);
		case PtcType.AbsoluteTime:
			return typeof(TimeSpan);
		case PtcType.RelativeTime:
			return typeof(TimeSpan);
		case PtcType.Deduced:
			return typeof(byte[]);
		default:
			throw new NotSupportedException("Parameter code not supported (" + parameterCode + ").");
		}
	}

	/// <summary>Extract a value from a byte array.</summary>
	/// <typeparam name="T">Type of the value to return.</typeparam>
	/// <param name="data">Byte array containing the value to extract.</param>
	/// <param name="parameterCode">Parameter code of the value to extract.</param>
	/// <returns>The extracted value.</returns>
	public static T ExtractValue<T>(byte[] data, ParameterCode parameterCode)
	{
		return ExtractValue<T>(data, parameterCode, 0);
	}

	/// <summary>Extract a value from a byte array.</summary>
	/// <typeparam name="T">Type of the value to return.</typeparam>
	/// <param name="data">Byte array containing the value to extract.</param>
	/// <param name="parameterCode">Parameter code of the value to extract.</param>
	/// <param name="index">Index in bits at which the value start.</param>
	/// <returns>The extracted value.</returns>
	/// <exception cref="InvalidCastException">Cannot convert data from the Parameter Code to the specified type.</exception>
	public static T ExtractValue<T>(byte[] data, ParameterCode parameterCode, int index)
	{
		int readLength;
		return ExtractValue<T>(data, parameterCode, index, out readLength);
	}

	/// <summary>Extract a value from a byte array.</summary>
	/// <typeparam name="T">Type of the value to return.</typeparam>
	/// <param name="data">Byte array containing the value to extract.</param>
	/// <param name="parameterCode">Parameter code of the value to extract.</param>
	/// <param name="index">Index in bits at which the value start.</param>
	/// <param name="readLength">The length in bits of the read value.</param>
	/// <returns>The extracted value.</returns>
	/// <exception cref="InvalidCastException">Cannot convert data from the Parameter Code to the specified type.</exception>
	public static T ExtractValue<T>(byte[] data, ParameterCode parameterCode, int index, out int readLength)
	{
		object value = ExtractValue(data, parameterCode, index, out readLength);

		try
		{
			return (T)Convert.ChangeType(value, typeof(T), Thread.CurrentThread.CurrentCulture);
		}
		catch(Exception ex)
		{
			throw new InvalidCastException(string.Format("Cannot convert data from Parameter Code (PTC={0},PFC={1}) to the type {2}.", parameterCode.Ptc, parameterCode.Pfc, typeof(T).FullName), ex);
		}
	}

	/// <summary>Extract a value from a byte array.</summary>
	/// <param name="data">Byte array containing the value to extract.</param>
	/// <param name="parameterCode">Parameter code of the value to extract.</param>
	/// <returns>The extracted value.</returns>
	/// <remarks>The type of the returned object is the same as the method <see cref="ParameterConverter.GetType" /> would return.</remarks>
	/// <exception cref="InvalidParameterCodeException">The parameter code is not valid.</exception>
	/// <exception cref="ArgumentNullException"><c>data</c> is null.</exception>
	/// <exception cref="ArgumentException">The data is invalid.</exception>
	public static object ExtractValue(byte[] data, ParameterCode parameterCode)
	{
		return ExtractValue(data, parameterCode, 0);
	}

	/// <summary>Extract a value from a byte array.</summary>
	/// <param name="data">Byte array containing the value to extract.</param>
	/// <param name="parameterCode">Parameter code of the value to extract.</param>
	/// <param name="startBit">Index in bits at which the value start.</param>
	/// <returns>The extracted value.</returns>
	/// <remarks>The type of the returned object is the same as the method <see cref="ParameterConverter.GetType" /> would return.</remarks>
	/// <exception cref="ArgumentNullException"><c>data</c> is null.</exception>
	/// <exception cref="ArgumentException">The data is invalid.</exception>
	/// <exception cref="IndexOutOfRangeException"><c>startBit</c> does not point to a bit index inside the data array.</exception>
	public static object ExtractValue(byte[] data, ParameterCode parameterCode, int startBit)
	{
		int readLength;
		return ExtractValue(data, parameterCode, startBit, out readLength);
	}

	/// <summary>Extract a value from a byte array.</summary>
	/// <param name="data">Byte array containing the value to extract.</param>
	/// <param name="parameterCode">Parameter code of the value to extract.</param>
	/// <param name="startBit">Index in bits at which the value start.</param>
	/// <param name="readLength">The length in bits of the read value.</param>
	/// <returns>The extracted value.</returns>
	/// <remarks>The type of the returned object is the same as the method <see cref="ParameterConverter.GetType" /> would return.</remarks>
	/// <exception cref="ArgumentNullException"><c>data</c> is null.</exception>
	/// <exception cref="ArgumentException">The data is invalid.</exception>
	/// <exception cref="IndexOutOfRangeException"><c>startBit</c> does not point to a bit index inside the data array.</exception>
	public static object ExtractValue(byte[] data, ParameterCode parameterCode, int startBit, out int readLength)
	{
		if(data == null)
			throw new ArgumentNullException("data");
		if(startBit < 0 || startBit > (data.Length * 8 - 1))
			throw new IndexOutOfRangeException("startBit does not point to a bit index inside the data array.");

		try
		{
			readLength = ParameterConverter.GetBitsSize(parameterCode, data, startBit);
			byte[] extractedData = UnalignedData.ExtractValue(data, startBit, readLength);
			object value;

			int start;
			switch(parameterCode.Ptc)
			{
			case PtcType.Boolean:
				value = (extractedData[0] >> 7) > 0;
				break;
			case PtcType.Enumerated:
				value = (uint)convertToUInt64(extractedData, GetBitsSize(parameterCode));
				break;
			case PtcType.UnsignedInteger:
				value = convertToUInt64(extractedData, GetBitsSize(parameterCode));
				break;
			case PtcType.SignedInteger:
				value = convertToInt64(extractedData, GetBitsSize(parameterCode));
				break;
			case PtcType.Real:
				switch(parameterCode.Pfc)
				{
				case 1:
					value = convertToSingle(extractedData);
					break;
				case 2:
					value = convertToDouble(extractedData);
					break;
				default:
					throw new NotSupportedException();
				}
				break;
			case PtcType.BitString:
				start = parameterCode.Pfc == 0 ? 1 : 0;
				int length = start > 0 ? extractedData[0] : parameterCode.Pfc;
				BitArray dataBitArray = new BitArray(extractedData);
				BitArray bitArray = new BitArray(length);
				for(int i = 0; i < length; i++)
					bitArray[i] = dataBitArray[invertBitOrder(i + start * 8)];
				value = bitArray;
				break;
			case PtcType.OctetString:
				start = parameterCode.Pfc == 0 ? 1 : 0;
				byte[] byteArray = new byte[extractedData.Length - start];
				Buffer.BlockCopy(extractedData, start, byteArray, 0, byteArray.Length);
				value = byteArray;
				break;
			case PtcType.CharacterString:
				start = parameterCode.Pfc == 0 ? 1 : 0;
				value = Encoding.GetString(extractedData, start, extractedData.Length - start);
				break;
			case PtcType.AbsoluteTime:
				value = extractAbsoluteTime(extractedData, parameterCode.Pfc);
				break;
			case PtcType.RelativeTime:
				value = extractRelativeTime(extractedData, parameterCode.Pfc);
				break;
			case PtcType.Deduced:
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
			throw new ArgumentException("Invalid data.", ex);
		}
	}

	private static int invertBitOrder(int value)
	{
		return 8 * (value / 8) + (7 - (value % 8));
	}

	private static TimeSpan extractAbsoluteTime(byte[] data, int pfc)
	{
		if(pfc == 0)
			return extractTime(data, data[0], true, false);
		else if(pfc == 1)
			return extractTime(data, 0x48, false, false); // 0b01001000 CDS-Level2-16bit-ms
		else if(pfc == 2)
			return extractTime(data, 0x49, false, false); // 0b01001001 CDS-Level2-16bit-us
		else
			return extractTime(data, (byte)(0x20 + pfc - 3), false, false);
	}

	private static TimeSpan extractRelativeTime(byte[] data, int pfc)
	{
		if(pfc == 0)
			return extractTime(data, data[0], true, data[1] > sbyte.MaxValue);
		else
			return extractTime(data, (byte)(0x20 + pfc - 1), false, data[0] > sbyte.MaxValue);
	}

	private static TimeSpan extractTime(byte[] data, byte pField, bool isExplicit, bool isSigned)
	{
		int offset = isExplicit ? 1 : 0;

		// Bit 1 = Time Format
		if((pField & 0x40) == 0)
		{
			// CUC
			int sign = 1;
			if(isSigned && data[offset] > sbyte.MaxValue) // Signed and negative
			{
				ulong value = 0;
				for(int i = offset; i < data.Length; i++)
					value |= (ulong)data[i] << ((data.Length - i - 1) * 8);
				value = (ulong.MaxValue - value) + 1;  // 2's complement
				for(int i = offset; i < data.Length; i++)
					data[i] = (byte)(value >> ((data.Length - i - 1) * 8));
				sign = -1;
			}

			int coarceLength = ((pField >> 2) & 0x3) + 1;
			int fineLength = (pField & 0x3);

			uint sec = 0;
			uint fsec = 0;
			for(int i = 0; i < coarceLength; i++)
				sec |= ((uint)data[i + offset] << ((coarceLength - i - 1) * 8));
			for(int i = 0; i < fineLength; i++)
				fsec |= ((uint)data[i + coarceLength + offset] << ((3 - i) * 8));

			return new TimeSpan((((long)sec * (10 * 1000 * 1000)) + (long)(fsec / 429.4967296d)) * sign); // 2^32 / 10'000'000 = 429.4967296
		}
		else
		{
			// CDS
			bool is24BitDay = (pField >> 2) == 1;
			bool hasUSec = (pField & 0x3) == 1;

			uint days = 0;
			uint msec = 0;
			ushort usec = 0;

			int i = offset;
			if(is24BitDay)
				days |= (uint)(data[i++] << 16);
			days |= (uint)(data[i++] << 8);
			days |= data[i++];

			msec |= (uint)(data[i++] << 24);
			msec |= (uint)(data[i++] << 16);
			msec |= (uint)(data[i++] << 8);
			msec |= data[i++];

			if(hasUSec)
			{
				usec |= (ushort)(data[i++] << 8);
				usec |= data[i++];
			}

			return new TimeSpan((days * TimeSpan.TicksPerDay) + (msec * TimeSpan.TicksPerMillisecond) + (usec * 10));
		}
	}

	private static ulong convertToUInt64(byte[] data, int bitSize)
	{
		int byteSize = GetStorageSize(bitSize);
		int bitOffset = (8 - (bitSize % 8)) % 8;

		ulong value = 0;
		for(int i = 0; i < byteSize; i++)
		{
			int offset = 8 * (byteSize - i - 1) - bitOffset;
			if(offset > 0)
				value |= (ulong)data[i] << offset;
			else
				value |= (ulong)data[i] >> (-offset);
		}
		return value;
	}

	private static long convertToInt64(byte[] data, int bitSize)
	{
		//int signIndex = bitSize % 8;
		bool signed = (data[0] >> 7) != 0;

		ulong uValue = convertToUInt64(data, bitSize);

		long value;
		if(signed)
		{
			uValue = (ulong.MaxValue - uValue) + 1; // Two Complement
			uValue &= ulong.MaxValue >> (64 - bitSize); // Keep data bits
			value = -(long)uValue;
		}
		else
			value = (long)uValue;
		return value;
	}

	private static float convertToSingle(byte[] data)
	{
		if(data.Length < 4)
			throw new ArgumentException("Data should be at least 4 bytes long.", "data");

		return BitReinterpreterSingle.Convert(ByteOrderConverter.GetInt32(data, 0));
	}

	private static double convertToDouble(byte[] data)
	{
		if(data.Length < 8)
			throw new ArgumentException("Data should be at least 8 bytes long.", "data");

		return BitReinterpreterDouble.Convert(ByteOrderConverter.GetInt64(data, 0));
	}

	/// <summary>Get the number of bytes needed to store the specified number of bits.</summary>
	/// <param name="bitsSize">Size in bits.</param>
	/// <returns>Number of bytes.</returns>
	public static int GetStorageSize(int bitsSize)
	{
		return (int)(bitsSize / 8) + ((bitsSize % 8 == 0) ? 0 : 1);
	}

	/// <summary>Get the number of bytes needed to store the specified parameter code.</summary>
	/// <param name="parameterCode"><see cref="ParameterCode"/> to get the storage size of.</param>
	/// <returns>Number of bytes.</returns>
	public static int GetStorageSize(ParameterCode parameterCode)
	{
		return GetStorageSize(GetBitsSize(parameterCode));
	}

	/// <summary>Gets the size in bits of the values encoded with the specified <see cref="ParameterCode"/>.</summary>
	/// <param name="parameterCode"><see cref="ParameterCode"/> to get the size of.</param>
	/// <returns>A size in bits if the size is fixed, -1 otherwise.</returns>
	/// <remarks>
	/// It is important to check the value returned by this method, it must not be
	/// used directly without proper verification that the size is fixed and not variable.
	/// The other overload can use the data to get the effective size of a variable length parameter code.
	/// </remarks>
	public static int GetBitsSize(ParameterCode parameterCode)
	{
		switch(parameterCode.Ptc)
		{
		case PtcType.NoData:
			return 0;
		case PtcType.Boolean:
			return 1;
		case PtcType.Enumerated:
			return parameterCode.Pfc; // Bits size is PFC with Enumerated
		case PtcType.UnsignedInteger:
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
		case PtcType.SignedInteger:
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
		case PtcType.Real:
			if(parameterCode.Pfc == 1)
				return 4 * 8;
			else if(parameterCode.Pfc == 2)
				return 8 * 8;
			else if(parameterCode.Pfc == 3)
				return 4 * 8;
			else if(parameterCode.Pfc == 4)
				return 6 * 8;
			break;
		case PtcType.BitString:
			if(parameterCode.Pfc == 0)
				return -1; // Variable length
			else
				return parameterCode.Pfc;
		case PtcType.OctetString:
			if(parameterCode.Pfc == 0)
				return -1; // Variable length
			else
				return parameterCode.Pfc * 8;
		case PtcType.CharacterString:
			if(parameterCode.Pfc == 0)
				return -1; // Variable length
			else
				return parameterCode.Pfc * 8;
		case PtcType.AbsoluteTime:
			if(parameterCode.Pfc == 0)
				return 8 * 8; // Full Format: 1 P-Field + 4 Coarce + 3 Fine
			else if(parameterCode.Pfc == 1)
				return 6 * 8;
			else if(parameterCode.Pfc == 2)
				return 8 * 8;
			else if((parameterCode.Pfc >= 3) && (parameterCode.Pfc <= 18))
				return (((parameterCode.Pfc + 1) / 4) + ((parameterCode.Pfc + 1) % 4)) * 8;
			break;
		case PtcType.RelativeTime:
			if(parameterCode.Pfc == 0)
				return 8 * 8; // Full Format: 1 P-Field + 4 Coarce + 3 Fine
			else if((parameterCode.Pfc >= 1) && (parameterCode.Pfc <= 16))
				return (((parameterCode.Pfc + 3) / 4) + ((parameterCode.Pfc + 3) % 4)) * 8;
			break;
		case PtcType.Deduced:
			if(parameterCode.Pfc == 0)
				return -1; // Variable length
			break;
		}

		throw new NotSupportedException();
	}

	/// <summary>Get the size in bits of the specified parameter code using the data for additional information.</summary>
	/// <param name="parameterCode"><see cref="ParameterCode"/> to get the size of.</param>
	/// <param name="data">Byte array containing the actual value used to get a variable length parameter code.</param>
	/// <param name="index">Index in bits at which the value start.</param>
	/// <returns>A size in bits.</returns>
	/// <exception cref="ArgumentNullException"><c>data</c> is null.</exception>
	/// <exception cref="ArgumentOutOfRangeException"><c>index</c> does not point to a bit index inside the data array.</exception>
	public static int GetBitsSize(ParameterCode parameterCode, byte[] data, int index)
	{
		if(data == null)
			throw new ArgumentNullException("data");
		if(index < 0 || index > (data.Length * 8 - 1))
			throw new ArgumentOutOfRangeException("Index does not point to a bit inside the data array.");

		if((parameterCode.Ptc == PtcType.OctetString || parameterCode.Ptc == PtcType.CharacterString) && (parameterCode.Pfc == 0))
			return (int)UnalignedData.ExtractValue(data, index, 8)[0] * 8 + 8;
		else if(parameterCode.Ptc == PtcType.BitString && parameterCode.Pfc == 0)
			return (int)UnalignedData.ExtractValue(data, index, 8)[0] + 8;
		else
			return GetBitsSize(parameterCode);
	}
}
