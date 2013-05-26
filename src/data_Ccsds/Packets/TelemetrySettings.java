package data_Ccsds.Packets;

import java.util.HashMap;
import java.util.Map;

import data_Ccsds.ParameterCode.ParameterCode;

/// <summary>Telemetry optional fields definition class</summary>
public class TelemetrySettings
{
	/// <summary>Telemetry Header: Packet Data Field - Field: Packet Error Control (PEC)</summary>
	/// <value>0: Field not present, 1: Field present, the field checksumType</value>
	public boolean HasPacketErrorControl; 
	public boolean isHasPacketErrorControl() {
		return HasPacketErrorControl;
	}
	public void setHasPacketErrorControl(boolean hasPacketErrorControl) {
		HasPacketErrorControl = hasPacketErrorControl;
	}

	/// <summary>ChecksumType (CRC or ISO)</summary>
	public ChecksumType ChecksumType; 
	public ChecksumType getChecksumType() {
		return ChecksumType;
	}
	public void setChecksumType(ChecksumType checksumType) {
		ChecksumType = checksumType;
	}

	/// <summary>Telemetry Header: Data Field Header - Field: Packet Subcounter</summary>
	/// <value>0: Field not present, 1: Field present</value>
	public boolean HasPacketSubcounter; 
	public boolean isHasPacketSubcounter() {
		return HasPacketSubcounter;
	}
	public void setHasPacketSubcounter(boolean hasPacketSubcounter) {
		HasPacketSubcounter = hasPacketSubcounter;
	}

	/// <summary>Telemetry Header: Data Field Header - Field: Destination ID</summary>
	/// <value>An Enumerated <see cref="ParameterCode"/>; or <c>null</c> if the field is not present.</value>
	public ParameterCode DestinationIdPc;

	/// <summary>Telemetry Header: Data Field Header - Field: Time (default)</summary>
	/// <value>An Absolute Time <see cref="ParameterCode"/>; or <c>null</c> if the field is not present.</value>
	public ParameterCode DefaultTimePc;

	/// <summary>Telemetry Header: Data Field Header - Field: Time</summary>
	/// <value>Key : Apid, Value : Null: Field not present, else The parameter code</value>
	public Map<Integer, ParameterCode> TimePcPerApid;

	/// <summary>Telemetry Header: Data Field Header - Field: Spare - Alignment of DHF in bytes.</summary>
	public byte DataFieldHeaderPadding; 
	public byte getDataFieldHeaderPadding() {
		return DataFieldHeaderPadding;
	}
	public void setDataFieldHeaderPadding(byte dataFieldHeaderPadding) {
		DataFieldHeaderPadding = dataFieldHeaderPadding;
	}

	/// <summary>Telemetry Header: Packet Data Field - Field: Spare - Alignment of PDF in bytes.</summary>
	public byte DataFieldPadding; 
	public byte getDataFieldPadding() {
		return DataFieldPadding;
	}
	public void setDataFieldPadding(byte dataFieldPadding) {
		DataFieldPadding = dataFieldPadding;
	}

	/// <summary>Initializes a new instance of the <see cref="TelemetrySettings"/> class.</summary>
	public TelemetrySettings()
	{
		TimePcPerApid = new HashMap<Integer,ParameterCode>();

		// Default values
		HasPacketErrorControl = true;
		ChecksumType = data_Ccsds.Packets.ChecksumType.Crc;
		HasPacketSubcounter = false;
		DestinationIdPc = null;
		DefaultTimePc = null;
		DataFieldHeaderPadding = 1;
		DataFieldPadding = 1;
	}
}
