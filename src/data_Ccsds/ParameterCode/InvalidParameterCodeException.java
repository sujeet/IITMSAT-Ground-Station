package data_Ccsds.ParameterCode;

import data_Ccsds.Packets.ArgumentException;


	/// <summary>The exception that is throw when a parameter code argument is invalid.</summary>
	public class InvalidParameterCodeException extends ArgumentException
	{
        private static final long serialVersionUID = 1L;
        /// <summary>The Parameter Type Code (PTC) of the invalid Parameter Code.</summary>
		public PtcType Ptc; 
		public PtcType getPtc() {
			return Ptc;
		}
		/// <summary>The Parameter Format Code (PFC) of the invalid Parameter Code.</summary>
		public int Pfc; 
		public int getPfc() {
			return Pfc;
		}

		/// <summary>Initializes a new instance of the <see cref="InvalidParameterCodeException"/> class.</summary>
		/// <param name="ptc">The Parameter Type Code (PTC) of the invalid Parameter Code.</param>
		/// <param name="pfc">The Format Type Code (PFC) of the invalid Parameter Code.</param>
		public InvalidParameterCodeException(PtcType ptc, int pfc)
		{
			super("The Parameter Code with PTC=" + ptc + " and PFC=" + pfc + " is not a valid Parameter Code.");
			Ptc = ptc;
			Pfc = pfc;
		}
	}
