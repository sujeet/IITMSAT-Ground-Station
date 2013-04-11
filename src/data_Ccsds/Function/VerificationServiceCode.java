package data_Ccsds.Function;

/// <summary>The code indicating the reason for the failure of the telecommand at a verification stage in PUS Service 1.</summary>
public enum VerificationServiceCode 
{
	/// <summary>Illegal APID (PAC error)</summary>
	IllegalApid(0),
	/// <summary>Incomplete or invalid length packet</summary>
	IncompleteOrInvalidLengthPacket(1),
	/// <summary>Incorrect checksum</summary>
	IncorrectChecksum(2),
	/// <summary>Illegal packet type</summary>
	IllegalPacketType(3),
	/// <summary>Illegal packet subtype</summary>
	IllegalPacketSubtype(4),
	/// <summary>Illegal or inconsistent application data</summary>
	IllegalOrInconsistentApplicationData(5),	

	/// <summary>None</summary>
	/// <remarks>Specific to SwissCube Ground Segment</remarks>
	None(Byte.MAX_VALUE - 1),
	/// <summary>Not Error</summary>
	/// <remarks>Specific to SwissCube Ground Segment</remarks>
	Correct(Byte.MAX_VALUE);

	private int code;
	/// <summary>Constructor</summary>
	private VerificationServiceCode(int c)
	{
		code=c;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int value) {
		code=value;
	}
}
