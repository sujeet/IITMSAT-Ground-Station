package data;

	/// <summary>Implemented by objects that can be treated as block of data of
	/// known size such as Packets or Messages and copied into a buffer.</summary>
	public abstract class IDataBlock
	{
		/// <summary>Gets the length in bytes of the data block.</summary>
		/// <value>The length in bytes.</value>
		int Length; 
		
		
		public int getLength() {
			return Length;
		}

		/*public void setLength(int length) {
			Length = length;
		}*/

		/// <summary>Convert the instance to bytes into the specified buffer.</summary>
		/// <param name="buffer">The buffer in which to write the bytes.</param>
		/// <param name="start">The index at which the data must start in the buffer.</param>
		/// <returns>The number of bytes written in the buffer.</returns>
		/// <exception cref="System.ArgumentOutOfRangeException">The buffer is too small to put the data at the specified offset.</exception>
		abstract public int ToBuffer(byte[] buffer, int start);
	}
