package data;

/// <summary>Computes CRC-CCITT checksums.</summary>
public class CrcCcittChecksum
{
	// CRC-CCITT look-up table
	private static long[] __lookupTable = createCrcLookupTable();

	/// <summary>Creates the CRC-CCITT look-up table.</summary>
	/// <returns>The CRC-CCITT look-up table.</returns>
	private static long[] createCrcLookupTable()
	{
		long tmp;
		long[] lookupTable = new long[256];

		for(int i = 0 ; i < 256 ; i++)
		{
			tmp = 0;

			if((i & 1) != 0) tmp = tmp ^ 0x1021;
			if((i & 2) != 0) tmp = tmp ^ 0x2042;
			if((i & 4) != 0) tmp = tmp ^ 0x4084;
			if((i & 8) != 0) tmp = tmp ^ 0x8108;
			if((i & 16) != 0) tmp = tmp ^ 0x1231;
			if((i & 32) != 0) tmp = tmp ^ 0x2462;
			if((i & 64) != 0) tmp = tmp ^ 0x48C4;
			if((i & 128) != 0) tmp = tmp ^ 0x9188;

			lookupTable[i] = tmp;
		}

		return lookupTable;
	}

	/// <summary>Computes the CRC-CCITT checksum of a data buffer.</summary>
	/// <param name="buffer">The buffer containing the data.</param>
	/// <param name="start">The starting index of the data in the buffer.</param>
	/// <param name="length">The length of the data.</param>
	/// <returns>The computed CRC-CCITT checksum.</returns>
	public static int ComputeChecksum(byte[] buffer, int start, int length)
	{
		// Init CRC syndrome
		int syndrome = 0xFFFF;

		int end = start + length;
		for(int i = start ; i < end ; i++)
			syndrome = calculateCrc(buffer[i], syndrome);

		// Convertion to byte
		return (int)syndrome;
	}

	private static int calculateCrc(byte data, int syndrome)
	{
		return (int)(((syndrome << 8) & 0xFF00) ^ __lookupTable[(((syndrome >> 8) ^ data) & 0x00FF)]);
	}
}
