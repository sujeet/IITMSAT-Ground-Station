package data_Ccsds.Packets;

import data.IDataBlock;
import data.CrcCcittChecksum;
import data.IsoChecksum;


/// <summary>CCSDS Packet (ECSS-E-70-41A) base class for telecommand and telemetry packets.</summary>
public abstract class CcsdsPacket extends IDataBlock
{
	/// <summary>The length of the header of a CCSDS packet.</summary>
	protected final static int HeaderLength = 6; // Packet header = 6 bytes

	public static int getHeaderLength() {
		return HeaderLength;
	}

	//-----------------------------------------------------------------------------------Packet Id
	/// <summary>The version of the packet structure.</summary>
	/// <remarks>Always return 0.</remarks>
	private final int VersionNumber = 0; 

	/// <summary>Distinguish between telecommand (=1/<c>true</c>) and telemetry (=0/<c>false</c>) packets.</summary>
	private Boolean Type;
	public void setType(Boolean value)
	{
		Type = value;
	}

	/// <summary>This indicate the presence of a data field header.</summary>
	/// <remarks>Always return <c>true</c>.</remarks>
	private final Boolean DataFieldHeaderFlag = true;

	/// <summary>
	/// The APID (Application Process ID) corresponds uniquely to an on-board
	/// application process which is the destination for this packet.
	/// </summary>
	private int ApplicationProcessId;
	public int getApplicationProcessId() { return ApplicationProcessId; }
	public void	setApplicationProcessId(int value) throws ArgumentOutOfRangeException
	{
		if ((value & 0x07FF) != value)  ///-----------------check
			throw new ArgumentOutOfRangeException("Application Process Id Value");
		else
			ApplicationProcessId = value;
	}

	/// <summary>
	/// Packet ID (16 bits) is a part of the header CCSDS "Packet Header (48 Bits)". 
	/// </summary>
	private int PacketId;
	public void setPacketId()
	{
		PacketId = 0;

		//Version No
		PacketId |= (int)(VersionNumber << 13);

		// Type
		PacketId |= (int)(((Type) ? 1 : 0) << 12);

		// Data Field Header Flag
		PacketId |= (int)(((DataFieldHeaderFlag)?1:0) << 11);

		// APID
		PacketId |= ApplicationProcessId;
	}

	//---------------------------------------------------------------------------------Packet Sequence Control

	/// <summary>
	/// The sequence flags are intended for use if a series of the packets
	/// are sent in a particular sequence. only the "stand-alone" packet is used (11).
	/// </summary>
	private SequenceFlagsType SequenceFlags;
	public int getSequenceFlags() { return SequenceFlags.getCode(); }
	public void setSequenceFlags(SequenceFlagsType value) throws ArgumentOutOfRangeException
	{
		if ((value.getCode() & 3) != value.getCode())
			throw new ArgumentOutOfRangeException("SequenceFlags");
		else
			SequenceFlags.setCode(value.getCode());
	}

	/// <summary>
	/// This fields is provided to identify a particular telecommand/telemetry packet
	/// so that it can be traced within the end-to-end telecommand/telemetry system.
	/// </summary>
	protected int SequenceCount;
	public int getSequenceCount() { return SequenceCount;}
	protected void setSequenceCount(int value) throws ArgumentOutOfRangeException 
	{
		if ((value & 0x3FFF) != value)
			throw new ArgumentOutOfRangeException("SequenceCount");
		else
			SequenceCount = value;
	}
		
	/// <summary>
	/// Packet sequence control (16 bits) is a part of the header CCSDS "Packet Header (48 Bits)". 
	/// </summary>
	private int PacketSequenceControl;
	public void setPacketSequenceControl()
	{
		PacketSequenceControl = 0;
			
		// Sequence Flags
		PacketSequenceControl |= ((SequenceFlags.getCode()) << 14);

		// Sequence Count
		PacketSequenceControl |= SequenceCount;
	}

	//---------------------------------------------------------------------------------------Packet Length
	
	/// <summary>
	/// The packet length field specifies the number of octets contained within the packet data field.
	/// C = (Number of octets in packet data field) - 1.
	/// </summary>
	// Where reading from buffer the value is read from it, but when construction the packet the value must be lazy-computed.
	// Therefore private setter, and lazy getter if not set.
	// But on top of it come the serialization, which doesn't call the object constructor.
	private int PacketLength;
	public int getPacketLength() 
	{
		if(PacketLength == 0)
			PacketLength = ComputePacketLengthField();
		return PacketLength;
	}
	private void setPacketLength(int packetLength) {
		PacketLength = packetLength;
	}
	//--------------------------------------------------------------------------Packet Data Field (Variable)
	
	/// <summary>The telecommand/telemetry Application/Source Data field.</summary>
	private byte[] Data;
	public byte[] getData() {return Data;}
	public void setData(byte[] data) {Data = data;}

	/// <summary>The length of the Application/Source Data field.</summary>
	/// <value>The length of the Application/Source Data data.</value>
	public int DataLength;
	public int getDataLength() {
		if (Data != null && DataLength==0)
			DataLength = Data.length;
		else if(Data == null)
			DataLength = 0;
		return DataLength;
	}
	
	//-----------------------------------------------------------------------------Packet Error Control
	
	/// <summary>The Packet Error Control field.</summary>
	/// <value>The Packet Error Control checksum.</value>
	private int PacketErrorControl;
	public int getPacketErrorControl() {return PacketErrorControl;}
	public void setPacketErrorControl(int packetErrorControl) {PacketErrorControl = packetErrorControl;}

	//-----------------------------------------------------------------------------ChecksumType
	
	/// <summary>The type of checksum (ISO or CRC).</summary>
	/// <remarks>Only the CRC type is supported.</remarks>
	protected ChecksumType checksumType;
	public ChecksumType getChecksumType() {
		return checksumType;
	}
	public void setChecksumType(ChecksumType value) {
		checksumType = value;
	}

	//-----------------------------------------------------------------------------Functions
	/// <summary>Gets the length of the complete packet in bytes.</summary>
	/// <value>The length of the complete packet in bytes.</value>
	public int CompletePacketLength; 
	public int getCompletePacketLength() {return CompletePacketLength;}
		
	//-----------------------------------------------------------------------------Constructor
	/// <summary>Initializes a new instance of the <see cref="CcsdsPacket"/> class.</summary>
	protected CcsdsPacket()
	{
		// Default to CRC
		checksumType = ChecksumType.Crc;

		// Stand-alone packet by default
		SequenceFlags = SequenceFlagsType.StandAlone;
		
		//Length of Application data in the packet
		DataLength = 0;
	}
	
	//-----------------------------------------------------------------------------Abstract Methods
	/// <summary>Gets a value indicating whether this instance has a Packet Error Control field.</summary>
	/// <value><c>true</c> if this instance has a Packet Error Control field; otherwise, <c>false</c>.</value>
	public abstract boolean HasPacketErrorControlField(); //{ get; }

	/// <summary>Computes the length of the Data Field Header.</summary>
	/// <returns>The length of the Data Field Header.</returns>
	protected abstract int ComputeDataFieldHeaderLength();

	/// <summary>Convert the Data Field Header field of the current <see cref="CcsdsPacket"/> instance to bytes into the specified buffer.</summary>
	/// <param name="buffer">The destination buffer of the bytes.</param>
	/// <param name="start">The offset at which the Data Field Header starts in the buffer.</param>
	/// <returns>The number of bytes written into the buffer.</returns>
	/// <exception cref="System.ArgumentOutOfRangeException">The buffer is too small to put the data at the specified offset.</exception>
	protected abstract int WriteDataFieldHeaderToBuffer(byte[] buffer, int start);

	/// <summary>Gets the alignment of the Packet Data Field in bytes.</summary>
	/// <value>he alignment of the Packet Data Field in bytes.</value>
	public abstract int PacketDataFieldAlignment();// { get; }
	

	//------------------------------------------------------------------------------Methods
	/// <summary>Compute the length the CCSDS packet for the "Packet Length" field.</summary>
	/// <returns>(Number of octets in packet data field) - 1</returns>
	protected int ComputePacketLengthField()
	{
		// C = (Number of octets in packet data field) - 1
		return (int)((HasPacketErrorControlField() ? 2 : 0) + ComputeDataFieldHeaderLength() + getDataLength() - 1);
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
			System.arraycopy(data, 0, buffer, index, data.length);
			index += data.length;
		}

		// PDF Spare (alignment)
		int pdfAlignment = PacketDataFieldAlignment(); // alignment in bytes
		if(pdfAlignment != 0)
		{
			int pdfLength = index - (start + HeaderLength); // Compute current length of PDF
			index += (pdfAlignment - (pdfLength % pdfAlignment)) % pdfAlignment; // Add missing byte count to align
		}

		// Checksum
		if(HasPacketErrorControlField())
		{
			int checksum = ComputeChecksum(buffer, start, index - start, checksumType);
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
		boolean typeIsTc = ((buffer[start] >> 4) & 0x01) == 1;
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
		if((start + HeaderLength + (HasPacketErrorControlField() ? 2 : 0)) > buffer.length)
			throw new ArgumentException("The buffer is too small to contain a packet at specified index.");

		// Packet Length first, needed to compute checksum
		PacketLength = (int)(ByteOrderConverter.GetUInt16(buffer, start + 4) + 1);

		// Buffer big enough to contain full packet?
		if((start + HeaderLength + PacketLength) > buffer.length)
			throw new ArgumentException(String.Format("The buffer is too small to contain the packet at specified index (missing {0} bytes).", (start + HeaderLength + PacketLength) - buffer.Length), "buffer");

		//--------------------------------------------------------------------------------------------Packet Error Control
		if(this.HasPacketErrorControlField())
		{
			// Extract PEC from field
			int pecIndex = start + HeaderLength + getPacketLength() - 2;
			int pecFieldValue = ByteOrderConverter.GetUInt16(buffer, pecIndex);

			// Compute PEC for telemetry in buffer
			int pecComputed = ComputeChecksum(buffer, start, pecIndex - start, checksumType);

			// If checksum doesn't match, fire an exception
			if(pecFieldValue != pecComputed)
				throw new InvalidChecksumException(pecFieldValue, pecComputed);
			else
				this.PacketErrorControl = pecFieldValue;
		}
		//#endregion

		// Check that Version Number =0
		int versionNumber = (buffer[start] >> 5) & 0x07;
		if(versionNumber != 0)
			throw new NotSupportedException("The CCSDS packet contained in the buffer refers to an unsupported CCSDS version"+versionNumber+", only 0 is supported.");

		// Check that type field correspond to the expected value
		boolean type = ((buffer[start] >> 4) & 0x01) == 1;
		if(type != Type)
			throw new ArgumentException(String.format("The buffer contains a {0} packet but a {1} packet was expected.", type ? "telecommand" : "telemetry", Type ? "telecommand" : "telemetry"));

		// Check that Data Field Header Flag =1
		int dataFieldHeaderFlag = (buffer[start] >> 3) & 0x01;
		if(dataFieldHeaderFlag != 1)
			throw new NotSupportedException("The Data Field Header Flag of the CCSDS packet contained in the buffer is cleared.");

		// Application Process ID
		ApplicationProcessId = (int)(ByteOrderConverter.GetUInt16(buffer, start) & 0x07FF);

		// Sequence Flags
		SequenceFlags.setCode((int)((buffer[start + 2] & 0xC0) >> 6));

		// Sequence Count
		SequenceCount = (int)(ByteOrderConverter.GetUInt16(buffer, start + 2) & 0x3FFF);
	}

	/// <summary>Fill the Data and Packet Error Control fields of the current instance with the values contained in the buffer.</summary>
	/// <param name="buffer">The buffer containing the CCSDS packet.</param>
	/// <param name="start">The index at which the packet starts.</param>
	/// <param name="index">The index in bytes of the start of the Data field in the buffer.</param>
	protected void FillDataFromBuffer(byte[] buffer, int start, int index)
	{
		int pdfOffset = index - HeaderLength - start;
		int dataLength = this.PacketLength - pdfOffset - (this.HasPacketErrorControlField() ? 2 : 0);
		if(dataLength > 0)
		{
			byte[] data = new byte[dataLength];
			System.arraycopy(buffer, index, data, 0, dataLength);
			this.Data = data;
		}

		// PDF Spare (alignment)
		// Ignore PDF Spare at decoding, part of the data (can't know with only the full packet size)
		
	}

	/// <summary>Computes the checksum of a data buffer.</summary>
	/// <param name="buffer">The buffer containing the data.</param>
	/// <param name="start">The starting index in bytes of the data in the buffer.</param>
	/// <param name="length">The length in bytes of the data.</param>
	/// <param name="checksumType">The type of checksum to compute.</param>
	/// <returns>The computed checksum.</returns>
	public static int ComputeChecksum(byte[] buffer, int start, int length, ChecksumType checksumType) throws ArgumentException
	{
		if(checksumType == ChecksumType.Crc)
			return CrcCcittChecksum.ComputeChecksum(buffer, start, length);
		else if(checksumType == ChecksumType.Iso)
			return IsoChecksum.ComputeChecksum(buffer, start, length);
		else
			throw new ArgumentException("checksumType");
	}
	/*
	/// <summary>Gets the length in bytes.</summary>
	/// <value>The length.</value>
	int IDataBlock.Length { get { return ComputeEntirePacketLength(); } }*/
}