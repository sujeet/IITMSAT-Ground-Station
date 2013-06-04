package AX25;

import data_Ccsds.Function.ArgumentNullException;
import data_Ccsds.Packets.ArgumentException;

/// AX.25 Transfer Frame
public class AX25Frame
{
     /// AX.25 Address Field
     public class AX25AddressField
     {
          /// SSID Mask
          private static final byte SSIDMask = 0x60;
        
          public String CallSign;
          
          /// Secondary Station Identifier
          public byte Ssid;
        
          /// AX.25 Address Field constructor
          public AX25AddressField()
               {
                    this.CallSign = "000000";
                    this.Ssid = 0;
               }
        
          /// AX.25 Address Field constructor
          /// <param name="framePart">AX.25 Frame Part</param>
          public AX25AddressField(byte[] framePart)
               {
                    this(framePart, 0);
               }

          /// AX.25 Address Field constructor
          /// <param name="framePart">AX.25 Frame Part</param>
          /// <param name="offset">Offset into the data byte array</param>
          public AX25AddressField(byte[] framePart, int offset)
               {
                    // Call Sign
                    char[] tmp = new char[6];
                    tmp[0] = (char)(framePart[offset] >> 1);
                    tmp[1] = (char)(framePart[offset + 1] >> 1);
                    tmp[2] = (char)(framePart[offset + 2] >> 1);
                    tmp[3] = (char)(framePart[offset + 3] >> 1);
                    tmp[4] = (char)(framePart[offset + 4] >> 1);
                    tmp[5] = (char)(framePart[offset + 5] >> 1);
                    this.CallSign = new String(tmp);

                    // SSID
                    this.Ssid = (byte)((framePart[offset + 6] >> 1) & 0xF);
               }

          /// AX.25 Address Field constructor
          /// <param name="callSign">Call Sign</param>
          /// <param name="ssid">Secondary Station Identifier</param>
          public AX25AddressField(String callSign, byte ssid)
               {
                    this.CallSign = callSign;
                    this.Ssid = ssid;
               }
        
          /// Convert the Frame Identification to a byte array
          /// <param name="sourceAddress">Source Address</param>
          /// <returns>The byte array</returns>
          public byte[] ToByteArray(boolean sourceAddress)
               {
                    // Call Sign
                    byte[] tmp = new byte[7];
                    tmp[0] = (byte)(this.CallSign.charAt(0) << 1);
                    tmp[1] = (byte)(this.CallSign.charAt(1) << 1);
                    tmp[2] = (byte)(this.CallSign.charAt(2) << 1);
                    tmp[3] = (byte)(this.CallSign.charAt(3) << 1);
                    tmp[4] = (byte)(this.CallSign.charAt(4) << 1);
                    tmp[5] = (byte)(this.CallSign.charAt(5) << 1);

                    // SSID
                    tmp[6] = (byte)((this.Ssid << 1) | AX25AddressField.SSIDMask);
                    if (sourceAddress == true)
                    {
                         tmp[6] |= 0x01;
                    }

                    return tmp;
               }

          /// 
          /// <param name="o"></param>
          /// <returns></returns>
          public boolean equals (Object other)
               {
                    if (other == null) return false;
                    if (other == this) return true;
                    if (!(other instanceof AX25AddressField))return false;
                    AX25AddressField otherField = (AX25AddressField)other;
                  
                    if (this.CallSign == otherField.CallSign && this.Ssid == otherField.Ssid)
                    {
                         return true;
                    }

                    return false;
               }

          public int hashCode ()
               {
                    return super.hashCode ();
               }

     }
     
     /// Control Bits (Unnumbered frame)
     private static byte ControlBits = 0x03;
     /// Protocol Identifier (No layer 3 protocol implemented)
     private static byte ProtocolIdentifier = (byte)0xF0;
        
     /// Header Length
     public static int HeaderLength = 16;

     /// Total Frame Length
     public int getLength () throws ArgumentNullException, ArgumentException, AX25Exception
          {
               return (int)(AX25Frame.HeaderLength + this.GetInformationField().length);
          }
        
     /// Destination Address
     public AX25AddressField DstAddress;
        
     /// Source Address
     public AX25AddressField SrcAddress;
        
     private byte[] _informationField;
     /// Information Field
        
     /// AX.25 Frame constructor
     public AX25Frame() throws AX25Exception
          {
               this.DstAddress = new AX25AddressField();
               this.SrcAddress = new AX25AddressField();
               this.SetInformationField (new byte[0]);
          }

     /// AX.25 Frame constructor
     /// <param name="frame">AX.25 Frame</param>
     public AX25Frame(byte[] frame) throws AX25Exception
          {this (frame, 0); }

     /// AX.25 Frame constructor
     /// <param name="frame">AX.25 Frame</param>
     /// <param name="offset">Offset into the data byte array</param>
     public AX25Frame(byte[] frame, int offset) throws AX25Exception
          {
               this ();
               // **************************************
               // Destination Address Field
               // **************************************
               this.DstAddress = new AX25AddressField(frame, offset);

               // **************************************
               // Source Address Field
               // **************************************
               this.SrcAddress = new AX25AddressField(frame, offset + 7);

               // **************************************
               // Control Bits
               // **************************************
               if (frame[offset + 14] != AX25Frame.ControlBits)
               {
                    throw new AX25Exception("Wrong Control Bits in AX.25 Frame !");
               }

               // **************************************
               // Protocol Identifier
               // **************************************
               if (frame[offset + 15] != AX25Frame.ProtocolIdentifier)
               {
                    throw new AX25Exception("Wrong Protocol Identifier in AX.25 Frame !");
               }

               // **************************************
               // Information Field
               // **************************************
               if (frame.length - offset - AX25Frame.HeaderLength > 0)
               {
                    byte[] informationField = new byte[frame.length - offset - AX25Frame.HeaderLength];
                    System.arraycopy (frame, (int)(offset + AX25Frame.HeaderLength), informationField, 0, (int)(frame.length - offset - AX25Frame.HeaderLength));
                    this.SetInformationField (informationField);
               }
               else
               {
                    this.SetInformationField (new byte[0]);
               }
          }

     /// AX.25 Frame constructor
     /// <param name="dstAddress">Destination Address</param>
     /// <param name="srcAddress">Source Address</param>
     /// <param name="informationField">Information Field</param>
     public AX25Frame(AX25AddressField dstAddress, AX25AddressField srcAddress, byte[] informationField) throws AX25Exception
          {
               this ();
               this.DstAddress = dstAddress;
               this.SrcAddress = srcAddress;
               this.SetInformationField (informationField);
          }
        
     /// Convert the Frame to a byte array
     /// <returns>The byte array</returns>
     public byte[] ToByteArray() throws ArgumentNullException, ArgumentException, AX25Exception
          {
               byte[] frame = new byte[this.getLength ()];

               // **************************************
               // Destination Address Field
               // **************************************
               System.arraycopy(this.DstAddress.ToByteArray(false), 0, frame, 0, 7);

               // **************************************
               // Source Address Field
               // **************************************
               System.arraycopy(this.SrcAddress.ToByteArray(true), 0, frame, 7, 7);

               // **************************************
               // Control Bits
               // **************************************
               frame[14] = AX25Frame.ControlBits;

               // **************************************
               // Protocol Identifier
               // **************************************
               frame[15] = AX25Frame.ProtocolIdentifier;

               // **************************************
               // Information Field
               // **************************************
               byte[] informationField = this.GetInformationField ();
               System.arraycopy(informationField, 0, frame, 16, informationField.length);

               return frame;
          }

     /// Convert the Information Field to a byte array
     /// <returns>The byte array</returns>
     protected byte[] GetInformationField() throws ArgumentNullException, ArgumentException, AX25Exception
          {
               if (this._informationField == null)
               {
                    return new byte[0];
               }
               else
               {
                    return this._informationField;
               }
          }

     /// Set a byte array to the Information Field
     /// <param name="informationField">The byte array</param>
     protected void SetInformationField(byte[] informationField) throws AX25Exception
          {
               this._informationField = informationField;
          }
}
