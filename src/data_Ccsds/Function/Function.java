package data_Ccsds.Function;

import data_Ccsds.Packets.Telecommand;
import data_Ccsds.ParameterCode.ParameterCode;

   /// <summary>Function class</summary>
	public class Function extends data.IDataBlock
	{
		/// <summary>The function's Application Process ID.</summary>
		public int Apid;
		public int getApid() {
			return Apid;
		}
		private void setApid(int apid) {
			Apid = apid;
		}

		/// <summary>The function number.</summary>
		public long Number;
		public long getNumber() {
			return Number;
		}
		private void setNumber(long number) {
			Number = number;
		}

		/// <summary>The function's parameters.</summary>
		public byte[] Parameters; 
		public byte[] getParameters() {
			return Parameters;
		}
		private void setParameters(byte[] parameters) {
			Parameters = parameters;
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
		public int getLength() {
			int length = 0;

			// Function ID field
			ParameterCode functionIdPc;
			if(!Telecommand.EffectiveSettings.FunctionIdPcPerApid.TryGetValue(Apid, out functionIdPc))
				functionIdPc = Telecommand.EffectiveSettings.DefaultFunctionIdPc;
			length += ParameterConverter.GetStorageSize(functionIdPc);

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
		public int ToBuffer(byte[] buffer, int start)
		{
			int index = start;

			// Get Function ID PC (padded)
			ParameterCode functionIdPc;
			if(!Telecommand.EffectiveSettings.FunctionIdPcPerApid.TryGetValue(Apid, out functionIdPc))
				functionIdPc = Telecommand.EffectiveSettings.DefaultFunctionIdPc;
			// Insert Function ID field
			int numberBitsInserted = ParameterConverter.InsertValue(buffer, index * 8, Number, functionIdPc); // Need bits index, have bytes
			index += ParameterConverter.GetStorageSize(functionIdPc);

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
		public static Function FromBuffer(byte[] buffer, int start, int length, int apid)
		{
			// Get Function ID PC (padded)
			ParameterCode functionIdPc;
			if(!Telecommand.EffectiveSettings.FunctionIdPcPerApid.TryGetValue(apid, out functionIdPc))
				functionIdPc = Telecommand.EffectiveSettings.DefaultFunctionIdPc;
			int functionIdLength = ParameterConverter.GetStorageSize(functionIdPc);

			// Extract number
			long number = ParameterConverter.ExtractValue<uint>(buffer, functionIdPc, start * 8); // Need bits index, have bytes

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
