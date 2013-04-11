package data_Ccsds.Packets;

import java.util.HashMap;
import java.util.Map;

import data_Ccsds.ParameterCode.ParameterCode;


/// <summary>Contains the definition of the optional fields of a telecommand.</summary>
public class TelecommandSettings
{
	/// <summary>Telecommand Header: Packet Data Field - Field: Source Id</summary>
	public ParameterCode SourceIdPc;

	/// <summary>Telecommand Header: Data Field Header - Field: Spare - Alignment of DHF in bytes.</summary>
	public byte DataFieldHeaderPadding; 
	public byte getDataFieldHeaderPadding() {
		return DataFieldHeaderPadding;
	}

	public void setDataFieldHeaderPadding(byte dataFieldHeaderPadding) {
		DataFieldHeaderPadding = dataFieldHeaderPadding;
	}
	/// <summary>Telecommand Header: Packet Data Field - Field: Spare - Alignment of PDF in bytes.</summary>
	public byte DataFieldPadding; 
	public byte getDataFieldPadding() {
		return DataFieldPadding;
	}

	public void setDataFieldPadding(byte dataFieldPadding) {
		DataFieldPadding = dataFieldPadding;
	}

	/// <summary>Telecommand Application Data: Function ID (default)</summary>
	public ParameterCode DefaultFunctionIdPc; 
	public ParameterCode getDefaultFunctionIdPc() {
		return DefaultFunctionIdPc;
	}

	public void setDefaultFunctionIdPc(ParameterCode defaultFunctionIdPc) {
		DefaultFunctionIdPc = defaultFunctionIdPc;
	}

	/// <summary>Telecommand Application Data: Function ID</summary>
	public Map<Integer, ParameterCode> FunctionIdPcPerApid;

	/// <summary>ChecksumType (CRC or ISO)</summary>
	public ChecksumType ChecksumType; 
	public ChecksumType getChecksumType() {
		return ChecksumType;
	}

	public void setChecksumType(ChecksumType checksumType) {
		ChecksumType = checksumType;
	}
	/// <summary>Initializes a new instance of the <see cref="TelecommandSettings"/> class.</summary>
	public TelecommandSettings()
	{
		FunctionIdPcPerApid = new HashMap <Integer, ParameterCode>();
	}
}
