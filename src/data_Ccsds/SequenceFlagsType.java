package data_Ccsds;

public enum SequenceFlagsType {
	/// <summary>The Sequence Flag field possible values.</summary>
	/// <summary>Packet Continuation</summary>
	ContinuationPacket(0),
	/// <summary>Packet First</summary>
	FirstPacket(1),
	/// <summary>Packet Last</summary>
	LastPacket(2),
	/// <summary>Packet StandAlone</summary>
	StandAlone(3);
	private int code;
	/// <summary>Constructor</summary>
	private SequenceFlagsType(int c)
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
