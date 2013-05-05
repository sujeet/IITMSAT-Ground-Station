package data_Ccsds.Function;

import data_Ccsds.Packets.ArgumentOutOfRangeException;
import data_Ccsds.ParameterCode.ParameterCode;

/// <summary>Reads PUS-encoded parameters from a buffer.</summary>
public class ParameterReader
{
	private byte[] _buffer;

	/// <summary>The current bit position of the reader in the buffer.</summary>
	private int Position;
	public int getPosition() {
		return Position;
	}

	/// <summary>Initializes a new instance of the <see cref="ParameterReader"/> class.</summary>
	/// <param name="buffer">The buffer to be read.</param>
	public ParameterReader(byte[] buffer) throws ArgumentNullException, ArgumentOutOfRangeException
	{
		int startPosition=0;
		// Check arguments
		if(buffer == null)
			throw new ArgumentNullException("buffer");
		if(startPosition < 0 || startPosition >= (buffer.length * 8))
			throw new ArgumentOutOfRangeException("Index does not point to a location inside the buffer."+startPosition);

		_buffer = buffer;
		Position = startPosition;

	}

	/// <summary>Initializes a new instance of the <see cref="ParameterReader"/> class.</summary>
	/// <param name="buffer">The buffer to be read.</param>
	/// <param name="startPosition">The bit index of the starting position of the reader in the buffer.</param>
	public ParameterReader(byte[] buffer, int startPosition) throws ArgumentNullException, ArgumentOutOfRangeException
	{
		// Check arguments
		if(buffer == null)
			throw new ArgumentNullException("buffer");
		if(startPosition < 0 || startPosition >= (buffer.length * 8))
			throw new ArgumentOutOfRangeException("Index does not point to a location inside the buffer."+startPosition);

		_buffer = buffer;
		Position = startPosition;
	}
	
	/// <summary>Reads a parameter of the specified parameter code.</summary>
	/// <typeparam name="T">The type of the parameter to read, must be compatible with the Parameter Code.</typeparam>
	/// <param name="parameterCode">The Parameter Code of the parameter to read.</param>
	/// <param name="value">The read parameter's value.</param>
	/// <returns>The number of bits read.</returns>
	public <T> int Read(ParameterCode parameterCode, out T value)
	{
		int readLength;
		value = ParameterConverter.ExtractValue<T>(_buffer, parameterCode, Position, out readLength);
		Position += readLength;
		return readLength;
	}
}
