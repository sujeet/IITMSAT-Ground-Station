package data_Ccsds.ParameterCode;
/// <summary>Represents a Parameter Code as defined in chapter 23.3 of the ECSS-E-70-41A standard.</summary>
public class ParameterCode
{
	/// <summary>The Parameter Type Code (PTC).</summary>
	public PtcType Ptc;
	public PtcType getPtc() {
		return Ptc;
	}

	/// <summary>The Parameter Format Code (PFC).</summary>
	int Pfc;
	public int getPfc() {
		return Pfc;
	}
	/// <summary>Initializes a new instance of the <see cref="ParameterCode"/> class.</summary>
	/// <param name="ptc">The Parameter Type Code (PTC).</param>
	/// <param name="pfc">The Format Type Code (PFC).</param>
	public ParameterCode(PtcType ptc, int pfc) throws InvalidParameterCodeException
	{
		if(!validate(ptc, pfc))
			throw new InvalidParameterCodeException(ptc, pfc);

		Ptc = ptc;
		Pfc = pfc;
	}

	/// <summary>Returns a <see cref="System.String"/> that represents the current <see cref="ParameterCode"/>.</summary>
	/// <returns>A <see cref="System.String"/> that represents the current <see cref="ParameterCode"/>.</returns>
	public String ToString()
	{
		return "PTC=" + Ptc + ", PFC=" + Pfc;
	}

	/// <summary>Returns the hash code for this instance.</summary>
	/// <returns>A 32-bit signed integer that is the hash code for this instance.</returns>
	public int GetHashCode()
	{
		return (int)Ptc.getCode() << 16 | Pfc;
	}

	/// <summary>Get whether this parameter code contains a valid value according to the ECSS-E-70-41A standard.</summary>
	/// <param name="ptc">The Parameter Type Code (PTC).</param>
	/// <param name="pfc">The Format Type Code (PFC).</param>
	/// <returns><c>true</c> if the parameter code is valid; otherwise <c>false</c>.</returns>
	private boolean validate(PtcType ptc, int pfc)
	{
		switch(Ptc)
		{
		case NoData:
			return pfc == 0;
		case Boolean:
			return pfc == 0;
		case Enumerated:
			return (pfc >= 1 && (pfc <= 16 || pfc == 24 || pfc == 32));
		case ObtCounter:
		case UnsignedInteger:
			return pfc <= 16;
		case SignedInteger:
			return pfc <= 16;
		case Real:
			return (pfc >= 1 && pfc <= 4);
		case BitString:
		case OctetString:
		case CharacterString:
			return true;
		case AbsoluteTime:
			return pfc <= 18;
		case RelativeTime:
			return pfc <= 16;
		case Deduced:
			return pfc == 0;
		case ScoeCommand:
		case Function:
		case Telecommand:
			return pfc == 0;
		default:
			return false;
		}
	}
}
