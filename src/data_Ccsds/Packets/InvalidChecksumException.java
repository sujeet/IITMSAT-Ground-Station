package data_Ccsds.Packets;

	public class InvalidChecksumException extends Exception
	{
        private static final long serialVersionUID = 1L;

        /// <summary>Gets the expected checksum.</summary>
		/// <value>The expected checksum.</value>
		public int ExpectedChecksum;// { get; private set; }

		/// <summary>Gets the computed checksum.</summary>
		/// <value>The computed checksum.</value>
		public int ComputedChecksum;// { get; private set; }

		/// <summary>Initializes a new instance of the <see cref="InvalidChecksumException"/> class.</summary>
		public InvalidChecksumException(int expectedChecksum, int computedChecksum)
		{	
			super("The checksum of the CCSDS packet is invalid.");
			ExpectedChecksum = expectedChecksum;
			ComputedChecksum = computedChecksum;
		}
	}
