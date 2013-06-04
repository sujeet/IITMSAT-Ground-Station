package AX25;

/// AX.25 Telecommand Transfer Frame
public class AX25Telecommand extends AX25Frame
{
     public AX25Telecommand() throws AX25Exception
          {
               super ();
          }

     /// <param name="frame">AX.25 Frame</param>
     public AX25Telecommand(byte[] frame) throws AX25Exception
          {
               super (frame);
          }

     /// <param name="frame">AX.25 Frame</param>
     /// <param name="offset">Offset into the data byte array</param>
     public AX25Telecommand(byte[] frame, int offset) throws AX25Exception
          {
               super (frame, offset);
          }

     /// <param name="dstAddress">Destination Address</param>
     /// <param name="srcAddress">Source Address</param>
     /// <param name="informationField">Information Field</param>
     public AX25Telecommand(AX25AddressField dstAddress, AX25AddressField srcAddress, byte[] informationField) throws AX25Exception
          {
               super (dstAddress, srcAddress, informationField);
          }
}
