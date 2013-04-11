package data_Ccsds.Function;

public class CommandParameter {

}
/*
using System;
using System.Runtime.Serialization;

namespace Epfl.SwissCube.GroundSegment.Data.Ccsds
{
	
    /// <summary>The command parameter abstract class</summary>
    [DataContract(Namespace = "http://swisscube.epfl.ch/GroundSegment/Data/CCSDS/")]
    [KnownType(typeof(Telecommand))]
	[KnownType(typeof(Function))]
    public abstract class CommandParameter : IDataBlock
    {
        #region Fields
        [DataMember(Name = "ParameterCode", IsRequired = true)]
        private ParameterCode _parameterCode;
        /// <summary>Gets or sets the parameter code.</summary>
        /// <value>The parameter code.</value>
        public ParameterCode ParameterCode
        {
            get { return _parameterCode; }
            protected set { _parameterCode = value; }
        }

        [DataMember(Name = "Value", IsRequired = true)]
        private object _value;
        /// <summary>Gets or sets the value.</summary>
        /// <value>The value.</value>
        public object Value
        {
            get { return _value; }
            set { _value = value; }
        }
        #endregion

        #region Constructors
        /// <summary>Initializes a new instance of the <see cref="CommandParameter"/> class.</summary>
        public CommandParameter()
        {

        }

        /// <summary>Initializes a new instance of the <see cref="CommandParameter"/> class.</summary>
        /// <param name="parameterCode">The parameter code.</param>
        /// <param name="value">The value.</param>
        public CommandParameter(ParameterCode parameterCode, object value)
            : this()
        {
            ParameterCode = parameterCode;
            Value = value;
        }
        #endregion

		/// <summary>Gets the length in bytes.</summary>
		/// <value>The length.</value>
		int IDataBlock.Length { get { return GetBitsSize() / 8; } }

        #region Methods
		/// <summary>Convert the instance to bytes into the specified buffer.</summary>
		/// <param name="buffer">The buffer in which to write the bytes.</param>
		/// <param name="offset">The offset in the buffer at which to put the bytes.</param>
		/// <returns>The number of bytes written into the buffer.</returns>
		/// <exception cref="System.ArgumentOutOfRangeException">The buffer is too small to put the data at the specified offset.</exception>
		public virtual int ToBuffer(byte[] buffer, int offset)
        {
            switch (ParameterCode.Ptc)
            {
                case PtcType.Function:
                    throw new NotImplementedException();
                case PtcType.Telecommand:
                    Telecommand tc = (Telecommand)Value;
                    return tc.ToBuffer(buffer, offset/8);
                default:
					// HACK: Null values
					if(Value != null)
						ParameterConverter.InsertValue(buffer, offset, Value, ParameterCode);
					return ParameterConverter.GetBitsSize(ParameterCode);
            }
        }

        /// <summary>Gets the size in bits.</summary>
        /// <returns>The size in bits</returns>
        public int GetBitsSize()
        {
            switch (ParameterCode.Ptc)
            {
				case PtcType.ObtCounter:
					return ParameterConverter.GetBitsSize(new ParameterCode(PtcType.UnsignedInteger, ParameterCode.Pfc));
                case PtcType.ScoeCommand:
                    throw new NotImplementedException();
                case PtcType.Function:
                    throw new NotImplementedException();
                case PtcType.Telecommand:
                    Telecommand tc = (Telecommand)Value;
                    return tc.GetBitsSize();
                default:
                    return ParameterConverter.GetBitsSize(ParameterCode); // TODO: Marche pas avec PC de taille variable... dont octet string que tu mets dedans par default...
            }
        }
        #endregion
    }
	
}*/