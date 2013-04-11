package data_Ccsds.Function;

import data_Ccsds.Packets.ArgumentOutOfRangeException;
import data_Ccsds.ParameterCode.ParameterCode;
import data_Ccsds.ParameterCode.ParameterConverter;

/// <summary>Writes PUS-encoded parameters to a buffer.</summary>
public class ParameterWriter
{
	private byte[] _buffer;

	/// <summary>The current bit position of the reader in the buffer.</summary>
	private int Position;
	public int getPosition() {
		return Position;
	}

	/// <summary>Initializes a new instance of the <see cref="ParameterWriter"/> class.</summary>
	/// <param name="buffer">The buffer to be read.</param>
	public ParameterWriter(byte[] buffer) 
	{
		int startPosition = 0;
		// Check arguments
		if(buffer == null) try {
            throw new ArgumentNullException("buffer");
        }
        catch (ArgumentNullException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		if(startPosition < 0 || startPosition >= (buffer.length * 8)) {
		    try {
            throw new ArgumentOutOfRangeException("Index does not point to a location inside the buffer."+startPosition);
        }
        catch (ArgumentOutOfRangeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		}

		_buffer = buffer;
		Position = startPosition;
	}

	/// <summary>Initializes a new instance of the <see cref="ParameterWriter"/> class.</summary>
	/// <param name="buffer">The buffer in which to write the parameters.</param>
	/// <param name="startPosition">The bit index of the starting position of the writer in the buffer.</param>
	public ParameterWriter(byte[] buffer, int startPosition)
	{
		// Check arguments
		if(buffer == null) {
		    try {
            throw new ArgumentNullException("buffer");
        }
        catch (ArgumentNullException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		}
		if(startPosition < 0 || startPosition >= (buffer.length * 8)) {
		    try {
            throw new ArgumentOutOfRangeException("Index does not point to a location inside the buffer."+startPosition);
        }
        catch (ArgumentOutOfRangeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		}

		_buffer = buffer;
		Position = startPosition;
	}

	/// <summary>Writes a parameter of the specified parameter code.</summary>
	/// <typeparam name="T">The type of the parameter to write, must be compatible with the Parameter Code.</typeparam>
	/// <param name="parameterCode">The parameter code of the parameter to read.</param>
	/// <param name="value">The parameter's value to write.</param>
	/// <returns>The number of bits written.</returns>
	public <T> int Write(ParameterCode parameterCode, T value)
	{
		int written = ParameterConverter.InsertValue(_buffer, Position, value, parameterCode);
		Position += written;
		return written;
	}

	/// <summary>Writes the specified raw value directly.</summary>
	/// <param name="value">The raw value to write.</param>
	/// <param name="bitLength">Length in bits of the value to write.</param>
	/// <returns>The number of bits written.</returns>
	public int Write(byte[] value, int bitLength)
	{
		UnalignedData.InsertValue(_buffer, Position, value, bitLength);
		Position += bitLength;
		return bitLength;
	}
}
