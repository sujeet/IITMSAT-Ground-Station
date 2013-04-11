package data_Ccsds.ParameterCode;

/// <summary>Parameter Type Code (ECSS-E-70-41A - Chapter 23.3 Encoding formats of parameter types)</summary>
public enum PtcType 
{
	/// <summary>No data (PTC = 0)</summary>
	NoData(0),
	/// <summary>Boolean parameter (PTC = 1)</summary>
	Boolean(1),
	/// <summary>Enumerated parameter (PTC = 2)</summary>
	Enumerated(2),
	/// <summary>Unsigned integer parameter (PTC = 3)</summary>
	UnsignedInteger(3),
	/// <summary>Signed integer parameter (PTC = 4)</summary>
	SignedInteger(4),
	/// <summary>Real parameter (PTC = 5)</summary>
	Real(5),
	/// <summary>Bit-string parameter (PTC = 6)</summary>
	BitString(6),
	/// <summary>Octet-string parameter (PTC = 7)</summary>
	OctetString(7),
	/// <summary>Character-string parameter (PTC = 8)</summary>
	CharacterString(8),
	/// <summary>Absolute time parameter (PTC = 9)</summary>
	AbsoluteTime(9),
	/// <summary>Relative time parameter (PTC = 10)</summary>
	RelativeTime(10),
/// <summary>Deduced parameter (PTC = 11)</summary>
	Deduced(11),
	/// <summary>OBT counter (PTC = 252)</summary>
	/// <remarks>MCS Specific. Same PFCs and encoded as Unsigned Integer, but shall be correlated from DateTime before encoding.</remarks>
	ObtCounter(252),
	/// <summary>Scoe Command (PTC = 253)</summary>
	/// <remarks>MCS Specific</remarks>
	ScoeCommand(253),
	/// <summary>Function (PTC = 254)</summary>
	/// <remarks>MCS Specific</remarks>
	Function(254),
	/// <summary>Telecommand (PTC = 255)</summary>
	/// <remarks>MCS Specific</remarks>
	Telecommand(255);
	
	private int code;
	public int getCode() {
		return code;
	}
	/// <summary>Constructor</summary>
	private PtcType(int c)
	{
		code=c;
	}
}

