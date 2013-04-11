package data_Ccsds.Packets;

public enum ChecksumType {

		/// <summary>Checksum type used for coding the checksum (ISO or CRC).</summary>
		/// <summary>
		/// ISO standard checksum
		/// <remarks>NOT USED IN THIS PROJECT</remarks>
		/// </summary>
		Iso,
		/// <summary>
		/// Cyclic Redundancy Check (CRC)
		/// </summary>
		Crc
	}

	