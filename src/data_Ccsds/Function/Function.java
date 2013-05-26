package data_Ccsds.Function;

import java.io.UnsupportedEncodingException;

import data_Ccsds.Packets.ArgumentException;
import data_Ccsds.Packets.ArgumentOutOfRangeException;
import data_Ccsds.Packets.NotSupportedException;
import data_Ccsds.Packets.Telecommand;
import data_Ccsds.ParameterCode.ParameterCode;
import data_Ccsds.ParameterCode.ParameterConverter;

   /// <summary>Function class</summary>
	public class Function extends data.IDataBlock
	{
		/// <summary>The function's Application Process ID.</summary>
		public int Apid;
		public int getApid() {
			return Apid;
		}

		/// <summary>The function number.</summary>
		public long Number;
		public long getNumber() {
			return Number;
		}

		/// <summary>The function's parameters.</summary>
		public byte[] Parameters; 
		public byte[] getParameters() {
			return Parameters;
		}

		/// <summary>Initializes a new instance of the <see cref="Function"/> class.</summary>
		/// <param name="apid">The function's Application Process ID.</param>
		/// <param name="number">The function number.</param>
		/// <param name="parameters">The function's parameters</param>
		public Function(int apid, long number, byte[] parameters)
		{
			Apid = apid;
			Number = number;
			Parameters = parameters;
		}

		/// <summary>Gets the length in bytes of the data block.</summary>
		/// <value>The length in bytes.</value>
		public int getLength() throws NotSupportedException {
			int length = 0;

			// Function ID field
			ParameterCode functionIdPc;
			if (Telecommand.EffectiveSettings.FunctionIdPcPerApid.containsKey (Apid)) {
				functionIdPc = Telecommand.EffectiveSettings.FunctionIdPcPerApid.get (Apid);
			}
			else {
				functionIdPc = Telecommand.EffectiveSettings.DefaultFunctionIdPc;
			}
			length += ParameterConverter.GetByteSize (functionIdPc);

			// Parameters field
			if(Parameters != null)
				length += Parameters.length;

			return length;

		}
		
		/// <summary>Convert the instance to bytes into the specified buffer.</summary>
		/// <param name="buffer">The buffer in which to write the bytes.</param>
		/// <param name="start">The index in bytes at which the data must start in the buffer.</param>
		/// <returns>The number of bytes written in the buffer.</returns>
		/// <exception cref="System.ArgumentOutOfRangeException">The buffer is too small to put the data at the specified offset.</exception>
		public int ToBuffer(byte[] buffer, int start) throws NotSupportedException, UnsupportedEncodingException, ArgumentNullException, ArgumentOutOfRangeException
		{
			int index = start;

			// Get Function ID PC (padded)
			ParameterCode functionIdPc;
			if(Telecommand.EffectiveSettings.FunctionIdPcPerApid.containsKey (Apid)) {
				functionIdPc = Telecommand.EffectiveSettings.FunctionIdPcPerApid.get (Apid);
			}
			else {
				functionIdPc = Telecommand.EffectiveSettings.DefaultFunctionIdPc;
			}
			// Insert Function ID field
			ParameterConverter.InsertValue(buffer, index * 8, Number, functionIdPc); // Need bits index, have bytes
			index += ParameterConverter.GetByteSize (functionIdPc);

			// Insert Parameters field
			if(Parameters != null)
			{
				System.arraycopy(Parameters, 0, buffer, index, Parameters.length);
				index += Parameters.length;
			}

			return index - start;
		}

		/// <summary>Reads a <see cref="Function"/> from a buffer.</summary>
		/// <param name="buffer">The buffer containing the <see cref="Function"/>.</param>
		/// <param name="start">The index in bytes of the start of the <see cref="Function"/> in the buffer.</param>
		/// <param name="length">The length in bytes of the data belonging to the function.</param>
		/// <param name="apid">The function's Application Process ID.</param>
		/// <returns>The read <see cref="Function"/>.</returns>
		public static Function FromBuffer(byte[] buffer, int start, int length, int apid) throws ArgumentNullException, ArgumentException, NotSupportedException
		{
			// Get Function ID PC (padded)
			ParameterCode functionIdPc;
			if(Telecommand.EffectiveSettings.FunctionIdPcPerApid.containsKey(apid)) {
				functionIdPc = Telecommand.EffectiveSettings.FunctionIdPcPerApid.get (apid);
			}
			else {
				functionIdPc = Telecommand.EffectiveSettings.DefaultFunctionIdPc;
			}
			int functionIdLength = ParameterConverter.GetByteSize (functionIdPc);

			// Extract number
			long number = (long) ParameterConverter.ExtractValue (buffer, functionIdPc, start * 8); // Need bits index, have bytes

			// Extract parameters
			int parametersLength = length - functionIdLength;
			byte[] parameters = null;
			if(parametersLength > 0)
			{
				parameters = new byte[length - functionIdLength];
				System.arraycopy(buffer, start + functionIdLength, parameters, 0, parameters.length);
			}

			return new Function(apid, number, parameters);
		}
	}
