package data_Ccsds;

/// <summary>CCSDS Packet (ECSS-E-70-41A) base class for telecommand and telemetry packets.</summary>
public abstract class CcsdsPacket_Old extends IDataBlock
{
	//#region Constants
	/// <summary>The length of the header of a CCSDS packet.</summary>
	public final int HeaderLength = 6; // Packet header = 6 bytes
	//#endregion

	#region Fields
	#region Packet Header (48 Bits)
	#region Packet ID
	/// <summary>
	/// Packet ID (16 bits) is a part of the header CCSDS "Packet Header (48 Bits)". 
	/// </summary>
	[IgnoreDataMember]
			public ushort PacketId
			{
		get
		{
			ushort packetId = 0;

			// Type
			packetId |= (ushort)((Type ? 1 : 0) << 12);

			// Data Field Header Flag
			packetId |= (ushort)(1 << 11);

			// APID
			packetId |= ApplicationProcessId;

			return packetId;
		}
			}

	/// <summary>The version of the packet stucture.</summary>
	/// <remarks>Always return 0.</remarks>
	[IgnoreDataMember]
			public byte VersionNumber { get { return 0; } }

	/// <summary>Distinguish between telecommand (=1/<c>true</c>) and telemetry (=0/<c>false</c>) packets.</summary>
	[IgnoreDataMember]
			public abstract bool Type { get; }

	/// <summary>This indicate the presence of a data field header.</summary>
	/// <remarks>Always return <c>true</c>.</remarks>
	[IgnoreDataMember]
			public bool DataFieldHeaderFlag { get { return true; } }

	[DataMember(Name = "ApplicationProcessId", IsRequired = true)]
			private ushort _applicationProcessId;
	/// <summary>
	/// The APID (Application Process ID) corresponds uniquely to an on-board
	/// application process which is the destination for this packet.
	/// </summary>
	[IgnoreDataMember]
			public ushort ApplicationProcessId
			{
		get { return _applicationProcessId; }
		set
		{
			if ((value & 0x07FF) != value)
				throw new ArgumentOutOfRangeException("value");
			else
				_applicationProcessId = value;
		}
			}
	#endregion

	#region Packet Sequence Control
	/// <summary>
	/// Packet sequence control (16 bits) is a part of the header CCSDS "Packet Header (48 Bits)". 
	/// </summary>
	[IgnoreDataMember]
			public ushort PacketSequenceControl
			{
		get
		{
			// Sequence Flags
			ushort packetSequenceControl = (ushort)((byte)(SequenceFlags) << 14);

			// Sequence Count
			packetSequenceControl |= SequenceCount;

			return packetSequenceControl;
		}
			}

	[DataMember(Name = "SequenceFlags", IsRequired = true)]
			private SequenceFlagsType _sequenceFlags;
	/// <summary>
	/// The sequence flags are intended for use if a series of the packets
	/// are sent in a particular sequence. only the "stand-alone" packet is used (11).
	/// </summary>
	[IgnoreDataMember]
			public SequenceFlagsType SequenceFlags
			{
		get { return _sequenceFlags; }
		set
		{
			if ((((byte)(value)) & 3) != ((byte)value))
				throw new ArgumentOutOfRangeException("SequenceFlags");
			else
				_sequenceFlags = value;
		}
			}

	[DataMember(Name = "SequenceCount", IsRequired = true)]
			private ushort _sequenceCount;
	/// <summary>
	/// This fields is provided to identify a particular telecommand/telemetry packet
	/// so that it can be traced within the end-to-end telecommand/telemetry system.
	/// </summary>
	[IgnoreDataMember]
			public ushort SequenceCount
			{
		get { return _sequenceCount; }
		set
		{
			if ((value & 0x3FFF) != value)
				throw new ArgumentOutOfRangeException("SequenceCount");
			else
				_sequenceCount = value;
		}
			}
	#endregion

	#region Packet Length
	[IgnoreDataMember]
			private ushort _packetLength;
	/// <summary>
	/// The packet length field specifies the number of octets contained within the packet data field.
	/// C = (Number of octets in packet data field) - 1.
	/// </summary>
	// Where reading from buffer the value is read from it, but when construction the packet the value must be lazy-computed.
	// Therefore private setter, and lazy getter if not set.
	// But on top of it come the serialization, which doesn't call the object constructor.
	[IgnoreDataMember]
			public ushort PacketLength
			{
		get
		{
			if(_packetLength == 0)
				return ComputePacketLengthField();
			else
				return _packetLength;
		}
		private set { _packetLength = value; }
			}
	#endregion
	#endregion

	#region Packet Data Field (Variable)
	[DataMember(Name = "Data")]
			private byte[] _data;
	/// <summary>The telecommand/telemetry Application/Source Data field.</summary>
	[IgnoreDataMember]
			public byte[] Data
			{
		get { return _data; }
		set { _data = value; }
			}

	/// <summary>The length of the Application/Source Data field.</summary>
	/// <value>The length of the Application/Source Data data.</value>
	[IgnoreDataMember]
			public ushort DataLength
			{
		get
		{
			if (Data != null)
				return (ushort)Data.Length;
			else
				return 0;
		}
			}
	#endregion

	#region Packet Error Control
	[DataMember(Name = "PacketErrorControl")]
			private ushort _packetErrorControl;
	/// <summary>The Packet Error Control field.</summary>
	/// <value>The Packet Error Control checksum.</value>
	public ushort PacketErrorControl
	{
		get { return _packetErrorControl; }
		set { _packetErrorControl = value; }
	}
	#endregion

	#region Checksum Type
	[DataMember(Name = "ChecksumType")]
			private ChecksumType _checksumType;
	/// <summary>The type of checksum (ISO or CRC).</summary>
	/// <remarks>Only the CRC type is supported.</remarks>
	public ChecksumType ChecksumType
	{
		get { return _checksumType; }
		set { _checksumType = value; }
	}
	#endregion

	/// <summary>Gets the length of the complete packet in bytes.</summary>
	/// <value>The length of the complete packet in bytes.</value>
	[IgnoreDataMember]
			public int CompletePacketLength { get { return ComputeEntirePacketLength(); } }
	#endregion

	#region Constructors
	/// <summary>Initializes a new instance of the <see cref="CcsdsPacket"/> class.</summary>
	protected CcsdsPacket_Old()
	{
		// Default to CRC
		ChecksumType = ChecksumType.Crc;

		// Stand-alone packet by default
		SequenceFlags = SequenceFlagsType.StandAlone;
	}
	#endregion

	#region Abstract Methods
	/// <summary>Gets a value indicating whether this instance has a Packet Error Control field.</summary>
	/// <value><c>true</c> if this instance has a Packet Error Control field; otherwise, <c>false</c>.</value>
	public abstract bool HasPacketErrorControlField { get; }

	/// <summary>Computes the length of the Data Field Header.</summary>
	/// <returns>The length of the Data Field Header.</returns>
	protected abstract ushort ComputeDataFieldHeaderLength();

	/// <summary>Convert the Data Field Header field of the current <see cref="CcsdsPacket"/> instance to bytes into the specified buffer.</summary>
	/// <param name="buffer">The destination buffer of the bytes.</param>
	/// <param name="start">The offset at which the Data Field Header starts in the buffer.</param>
	/// <returns>The number of bytes written into the buffer.</returns>
	/// <exception cref="System.ArgumentOutOfRangeException">The buffer is too small to put the data at the specified offset.</exception>
	protected abstract int WriteDataFieldHeaderToBuffer(byte[] buffer, int start);

	/// <summary>Gets the alignment of the Packet Data Field in bytes.</summary>
	/// <value>he alignment of the Packet Data Field in bytes.</value>
	public abstract int PacketDataFieldAlignment { get; }
	#endregion

	#region Methods
	/// <summary>Compute the length the CCSDS packet for the "Packet Length" field.</summary>
	/// <returns>(Number of octets in packet data field) - 1</returns>
	protected ushort ComputePacketLengthField()
	{
		// C = (Number of octets in packet data field) - 1
		return (ushort)((HasPacketErrorControlField ? 2 : 0) + ComputeDataFieldHeaderLength() + DataLength - 1);
	}

	/// <summary>Calculate the length of the entire CCSDS packet in bytes.</summary>
	/// <returns>The length of the entire CCSDS packet in bytes.</returns>
	protected int ComputeEntirePacketLength()
	{
		return ComputePacketLengthField() + 1 + HeaderLength;
	}

	/// <summary>Convert the current <see cref="CcsdsPacket"/> instance to bytes into the specified buffer.</summary>
	/// <param name="buffer">The buffer in which to write the bytes.</param>
	/// <param name="start">The index at which the packet must start in the buffer.</param>
	/// <returns>The number of bytes written into the buffer.</returns>
	/// <exception cref="System.ArgumentOutOfRangeException">The buffer is too small to put the data at the specified offset.</exception>
	public int ToBuffer(byte[] buffer, int start)
	{
		int index = start;

		//Packet ID
		ByteOrderConverter.CopyValueNetworkOrder(buffer, index, PacketId);
		index += 2;

		//Packet Sequence Control
		ByteOrderConverter.CopyValueNetworkOrder(buffer, start + 2, PacketSequenceControl);
		index += 2;

		//Packet Length
		ByteOrderConverter.CopyValueNetworkOrder(buffer, start + 4, ComputePacketLengthField());
		index += 2;

		// Data Field Header (done by actual packet implementation)
		index += WriteDataFieldHeaderToBuffer(buffer, index);

		// Data
		byte[] data = Data;
		if(data != null)
		{
			Buffer.BlockCopy(data, 0, buffer, index, data.Length);
			index += data.Length;
		}

		// PDF Spare (alignment)
		int pdfAlignment = PacketDataFieldAlignment; // alignment in bytes
		if(pdfAlignment != 0)
		{
			int pdfLength = index - (start + HeaderLength); // Compute current length of PDF
			index += (pdfAlignment - (pdfLength % pdfAlignment)) % pdfAlignment; // Add missing byte count to align
		}

		// Checksum
		if(HasPacketErrorControlField)
		{
			ushort checksum = ComputeChecksum(buffer, start, index - start, ChecksumType);
			ByteOrderConverter.CopyValueNetworkOrder(buffer, index, checksum);
			index += 2;
		}

		return index - start;
	}

	/// <summary>Reads a <see cref="CcsdsPacket"/> packet from a buffer.</summary>
	/// <param name="buffer">The buffer containing the <see cref="CcsdsPacket"/> packet starting at byte 0.</param>
	/// <returns>The read <see cref="CcsdsPacket"/> packet.</returns>
	public static CcsdsPacket FromBuffer(byte[] buffer)
	{
		return FromBuffer(buffer, 0);
	}

	/// <summary>Reads a <see cref="CcsdsPacket"/> packet from a buffer.</summary>
	/// <param name="buffer">The buffer containing the <see cref="CcsdsPacket"/> packet.</param>
	/// <param name="start">The index in bytes of the start of the <see cref="CcsdsPacket"/> packet in the buffer.</param>
	/// <returns>The read <see cref="CcsdsPacket"/> packet.</returns>
	public static CcsdsPacket FromBuffer(byte[] buffer, int start)
	{
		bool typeIsTc = ((buffer[start] >> 4) & 0x01) == 1;
		if(typeIsTc)
			return Telecommand.FromBuffer(buffer, start);
		else
			return Telemetry.FromBuffer(buffer, start);
	}

	/// <summary>Fill the headers of the current instance with the values contained in the buffer.</summary>
	/// <param name="buffer">The buffer containing the CCSDS packet.</param>
	/// <param name="start">The offset in bytes of the start of the CCSDS packet in the buffer.</param>
	protected void FillHeadersAndPecFromBuffer(byte[] buffer, int start)
	{
		// Buffer big enough to at least have CCSDS Header and a PEC?
		if((start + HeaderLength + (HasPacketErrorControlField ? 2 : 0)) > buffer.Length)
			throw new ArgumentException("The buffer is too small to contain a packet at specified index.", "buffer");

		// Packet Length first, needed to compute checksum
		PacketLength = (ushort)(ByteOrderConverter.GetUInt16(buffer, start + 4) + 1);

		// Buffer big enough to contain full packet?
		if((start + HeaderLength + PacketLength) > buffer.Length)
			throw new ArgumentException(string.Format("The buffer is too small to contain the packet at specified index (missing {0} bytes).", (start + HeaderLength + PacketLength) - buffer.Length), "buffer");

		#region Packet Error Control
		if(this.HasPacketErrorControlField)
		{
			// Extract PEC from field
			int pecIndex = start + CcsdsPacket.HeaderLength + this.PacketLength - 2;
			ushort pecFieldValue = ByteOrderConverter.GetUInt16(buffer, pecIndex);

			// Compute PEC for telemetry in buffer
			ushort pecComputed = ComputeChecksum(buffer, start, pecIndex - start, ChecksumType);

			// If checksum doesn't match, fire an exception
			if(pecFieldValue != pecComputed)
				throw new InvalidChecksumException(pecFieldValue, pecComputed);
			else
				this.PacketErrorControl = pecFieldValue;
		}
		#endregion

		// Check that Version Number =0
		int versionNumber = (buffer[start] >> 5) & 0x07;
		if(versionNumber != 0)
			throw new NotSupportedException(string.Format("The CCSDS packet contained in the buffer refers to an unsupported CCSDS version {0}, only 0 is supported.", versionNumber));

		// Check that type field correspond to the expected value
		bool type = ((buffer[start] >> 4) & 0x01) == 1;
		if(type != Type)
			throw new ArgumentException(string.Format("The buffer contains a {0} packet but a {1} packet was expected.", type ? "telecommand" : "telemetry", Type ? "telecommand" : "telemetry"));

		// Check that Data Field Header Flag =1
		int dataFieldHeaderFlag = (buffer[start] >> 3) & 0x01;
		if(dataFieldHeaderFlag != 1)
			throw new NotSupportedException("The Data Field Header Flag of the CCSDS packet contained in the buffer is cleared.");

		// Application Process ID
		ApplicationProcessId = (ushort)(ByteOrderConverter.GetUInt16(buffer, start) & 0x07FF);

		// Sequence Flags
		SequenceFlags = (SequenceFlagsType)(byte)((buffer[start + 2] & 0xC0) >> 6);

		// Sequence Count
		SequenceCount = (ushort)(ByteOrderConverter.GetUInt16(buffer, start + 2) & 0x3FFF);
	}

	/// <summary>Fill the Data and Packet Error Control fields of the current instance with the values contained in the buffer.</summary>
	/// <param name="buffer">The buffer containing the CCSDS packet.</param>
	/// <param name="start">The index at which the packet starts.</param>
	/// <param name="index">The index in bytes of the start of the Data field in the buffer.</param>
	protected void FillDataFromBuffer(byte[] buffer, int start, int index)
	{
		#region Data
		int pdfOffset = index - HeaderLength - start;
		int dataLength = this.PacketLength - pdfOffset - (this.HasPacketErrorControlField ? 2 : 0);
		if(dataLength > 0)
		{
			byte[] data = new byte[dataLength];
			Buffer.BlockCopy(buffer, index, data, 0, dataLength);
			this.Data = data;
		}

		// PDF Spare (alignment)
		// Ignore PDF Spare at decoding, part of the data (can't know with only the full packet size)
		#endregion
	}

	/// <summary>Computes the checksum of a data buffer.</summary>
	/// <param name="buffer">The buffer containing the data.</param>
	/// <param name="start">The starting index in bytes of the data in the buffer.</param>
	/// <param name="length">The length in bytes of the data.</param>
	/// <param name="checksumType">The type of checksum to compute.</param>
	/// <returns>The computed checksum.</returns>
	public static ushort ComputeChecksum(byte[] buffer, int start, int length, ChecksumType checksumType)
	{
		if(checksumType == ChecksumType.Crc)
			return CrcCcittChecksum.ComputeChecksum(buffer, start, length);
		else if(checksumType == ChecksumType.Iso)
			return IsoChecksum.ComputeChecksum(buffer, start, length);
		else
			throw new ArgumentException("checksumType");
	}
	#endregion

	/// <summary>Gets the length in bytes.</summary>
	/// <value>The length.</value>
	int IDataBlock.Length { get { return ComputeEntirePacketLength(); } }
		}