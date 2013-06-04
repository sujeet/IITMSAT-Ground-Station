package AX25;

import data.ByteOrderConverter;
import data_Ccsds.Function.ArgumentNullException;
import data_Ccsds.Packets.ArgumentException;

/// AX.25 Frame Identification
class AX25FrameIdentification
{
     /// Version Number
     private static byte VersionNumber = 0x00;
     /// Spare
     private static byte Spare = 0x00;

     /// Virtual Channel ID
     public byte VirtualChannelId;
     
     public AX25FrameIdentification()
          {
               this.VirtualChannelId = 0;
          }

     /// <param name="framePart">AX.25 Frame Part</param>
     public AX25FrameIdentification(byte[] framePart) throws AX25Exception
     {  
             this(framePart, 0);
     }

     /// <param name="framePart">AX.25 Frame Part</param>
     /// <param name="offset">Offset into the data byte array</param>
     public AX25FrameIdentification(byte[] framePart, int offset) throws AX25Exception
          {
               // Version Number
               if (((byte)(framePart[offset] >> 6)) != AX25FrameIdentification.VersionNumber)
               {
                    throw new AX25Exception(String.format("Version number must be {0:X} !", AX25FrameIdentification.VersionNumber));
               }

               // Virtual Channel ID
               this.VirtualChannelId = (byte)((framePart[offset] >> 3) & 0x07);

               // Spare
               if (((byte)(framePart[offset] & 0x07)) != AX25FrameIdentification.Spare)
               {
                    throw new AX25Exception(String.format("Frame Identification Spare must be {0:X} !", AX25FrameIdentification.Spare));
               }
          }

     /// AX.25 Frame Identification constructor
     /// <param name="vcId">Virtual Channel ID</param>
     public AX25FrameIdentification(byte vcId)
          {
               this.VirtualChannelId = vcId;
          }

     /// Convert the Frame Identification to a byte array
     /// <returns>The byte array</returns>
     public byte[] ToByteArray()
          {
               byte[] tmp = new byte[1];

               // Version Number
               tmp[0] = (byte)(AX25FrameIdentification.VersionNumber << 6);

               // Virtual Channel ID
               tmp[0] |= (byte)(this.VirtualChannelId << 3);

               // Version Number
               tmp[0] |= AX25FrameIdentification.Spare;

               return tmp;
          }
}

/// AX.25 Frame Status
class AX25FrameStatus
{
     /// Spare
     private static byte Spare = 0x00;

     /// Time Flag
     public byte TimeFlag;

     /// Time Length
     public int getTimeLength()
          {
               return (int)(this.TimeFlag >= 8 ? ((this.TimeFlag & 0x7) + 1) : 0);
          }

     public byte TCCounter;

     public AX25FrameStatus()
          {
               this.TimeFlag = 0xB; // Default SwissCube Time Flag value (0xB)
               this.TCCounter = 0;
          }

     /// <param name="framePart">AX.25 Frame Part</param>
     public AX25FrameStatus(byte[] framePart) throws AX25Exception
     {
         this (framePart, 0);
     }

     /// <param name="framePart">AX.25 Frame Part</param>
     /// <param name="offset">Offset into the data byte array</param>
     public AX25FrameStatus(byte[] framePart, int offset) throws AX25Exception
          {
               // Time Flag
               this.TimeFlag = (byte)(framePart[offset] >> 4);

               // Spare
               if (((byte)((framePart[offset] >> 2) & 0x03)) != AX25FrameStatus.Spare)
               {
                    throw new AX25Exception(String.format("Frame Status Spare must be {0:X} !", AX25FrameStatus.Spare));
               }

               // TC Counter
               this.TCCounter = (byte)(framePart[offset] & 0x03);
          }

     /// AX.25 Frame Status constructor
     /// <param name="timeFlag">Time Flag</param>
     /// <param name="tcCounter">TC Counter</param>
     public AX25FrameStatus(byte timeFlag, byte tcCounter)
          {
               this.TimeFlag = timeFlag;
               this.TCCounter = tcCounter;
          }

     /// Convert the Frame Status to a byte array
     /// <returns>The byte array</returns>
     public byte[] ToByteArray()
          {
               byte[] tmp = new byte[1];

               // Time Flag
               tmp[0] = (byte)(this.TimeFlag << 4);

               // Spare
               tmp[0] |= (byte)(AX25FrameStatus.Spare << 2);

               // TC Counter
               tmp[0] |= this.TCCounter;

               return tmp;
          }
}
    
/// AX.25 Telemetry Transfer Frame
public class AX25Telemetry extends AX25Frame
{
     /// Header Length
     public static int SecondaryHeaderLength = 4;

     /// Trailer Length
     public int getSecondaryTrailerLength ()
     {
          return (int)(1 + this.FrameStatus.getTimeLength());
     }

     /// Total Frame Length
     public int getLength ()
          {
               return (int)(AX25Frame.HeaderLength + AX25Telemetry.SecondaryHeaderLength + this.Data.length + this.getSecondaryTrailerLength());
     }

     /// AX.25 Frame Identification
     public AX25FrameIdentification FrameIdentification;

     /// Master Frame Count
     public byte MasterFrameCount;

     /// Virtual Channel Frame Count
     public byte VirtualChannelFrameCount;

     /// First Header Pointer
     public byte FirstHeaderPointer;

     /// Data
     public byte[] Data = new byte [0];

     /// AX.25 Frame Status
     public AX25FrameStatus FrameStatus;

     /// Time
     public long Time;

     public AX25Telemetry() throws AX25Exception
     {
         super ();
     }

     /// <param name="frame">AX.25 Frame</param>
     public AX25Telemetry(byte[] frame) throws AX25Exception
     {
         super (frame);
     }

     /// <param name="frame">AX.25 Frame</param>
     /// <param name="offset">Offset into the data byte array</param>
     public AX25Telemetry(byte[] frame, int offset) throws AX25Exception
     {
         super (frame, offset);
     }

     /// <param name="dstAddress">Destination Address</param>
     /// <param name="srcAddress">Source Address</param>
     /// <param name="frameIdentification">Frame Identification</param>
     /// <param name="masterFrameCount">Master Frame Count</param>
     /// <param name="virtualChannelFrameCount">Virtual Channel Frame Count</param>
     /// <param name="firstHeaderPointer">First Header Pointer</param>
     /// <param name="data">Data</param>
     /// <param name="frameStatus">Frame Status</param>
     /// <param name="time">Time</param>
     public AX25Telemetry(AX25AddressField dstAddress, AX25AddressField srcAddress, AX25FrameIdentification frameIdentification, byte masterFrameCount, byte virtualChannelFrameCount, byte firstHeaderPointer, byte[] data, AX25FrameStatus frameStatus, long time) throws AX25Exception
     {
          super (dstAddress, srcAddress, new byte [0]);
          this.FrameIdentification = frameIdentification;
          this.MasterFrameCount = masterFrameCount;
          this.VirtualChannelFrameCount = virtualChannelFrameCount;
          this.FirstHeaderPointer = firstHeaderPointer;
          this.Data = data;
          this.FrameStatus = frameStatus;
          this.Time = time;
     }

     /// Convert the Information Field to a byte array
     /// <returns>The byte array</returns>
     protected byte[] GetInformationField() throws ArgumentNullException, ArgumentException, AX25Exception
     {
          int length = (int)(AX25Telemetry.SecondaryHeaderLength 
                  +this.getSecondaryTrailerLength() + this.Data.length);

          byte[] informationField = new byte[length];

          // Frame Identification
          System.arraycopy(this.FrameIdentification.ToByteArray(), 0, informationField, 0, 1);

          // Master Frame Count
          informationField[1] = this.MasterFrameCount;

          // Virtual Channel Frame Count
          informationField[2] = this.VirtualChannelFrameCount;

          // First Header Pointer
          informationField[3] = this.FirstHeaderPointer;

          // Data
          System.arraycopy(this.Data, 0, informationField, 4, this.Data.length);

          // Frame Status
          System.arraycopy(this.FrameStatus.ToByteArray(), 0, informationField, this.Data.length + 4, 1);

          // Time
          if (this.FrameStatus.getTimeLength() == 8)
          {
               ByteOrderConverter.CopyValueNetworkOrder(informationField, this.Data.length + 5, this.Time);
          }
          else if (this.FrameStatus.getTimeLength() == 4)
          {
               ByteOrderConverter.CopyValueNetworkOrder(informationField, this.Data.length + 5, (int)this.Time);
          }
          else if (this.FrameStatus.getTimeLength() == 2)
          {
               ByteOrderConverter.CopyValueNetworkOrder(informationField, this.Data.length + 5, (short)this.Time);
          }
          else if (this.FrameStatus.getTimeLength() == 1)
          {
               ByteOrderConverter.CopyValueNetworkOrder(informationField, this.Data.length + 5, (byte)this.Time);
          }
          else
          {
               throw new AX25Exception(String.format("Time Flag {0} is not supported!", this.FrameStatus.TimeFlag));
          }
            
          return informationField;
     }

     /// Set a byte array to the Information Field
     /// <param name="informationField">The byte array</param>
     protected void SetInformationField(byte[] informationField) throws AX25Exception
     {
          if (informationField.length == 0)
          {
               this.FrameIdentification = new AX25FrameIdentification();
               this.MasterFrameCount = 0;
               this.VirtualChannelFrameCount = 0;
               this.FirstHeaderPointer = 0;
               this.Data = new byte[0];
               this.FrameStatus = new AX25FrameStatus();
               this.Time = 0;
          }
          else
          {
               // Frame Identification
               this.FrameIdentification = new AX25FrameIdentification(new byte[] { informationField[0] });

               // Master Frame Count
               this.MasterFrameCount = informationField[1];

               // Virtual Channel Frame Count
               this.VirtualChannelFrameCount = informationField[2];

               // First Header Pointer
               this.FirstHeaderPointer = informationField[3];

               // Data
               if (informationField.length - 9 > 0)
               {
                    this.Data = new byte[informationField.length - 9];
                    System.arraycopy(informationField, 4, this.Data, 0, informationField.length - 9);
               }
               else
               {
                    this.Data = new byte[0];
               }

               // Frame Status
               this.FrameStatus = new AX25FrameStatus(new byte[] { informationField[informationField.length - 5] });

               // Time
               if (this.FrameStatus.getTimeLength() == 8)
               {
                    this.Time = ByteOrderConverter.GetInt64(informationField, informationField.length - 8);
               }
               else if (this.FrameStatus.getTimeLength() == 4)
               {
                    this.Time = ByteOrderConverter.GetInt32(informationField, informationField.length - 4);
               }
               else if (this.FrameStatus.getTimeLength() == 2)
               {
                    this.Time = ByteOrderConverter.GetInt16(informationField, informationField.length - 2);
               }
               else if (this.FrameStatus.getTimeLength() == 1)
               {
                    this.Time = ByteOrderConverter.GetByte(informationField, informationField.length - 1);
               }
               else
               {
                    throw new AX25Exception(String.format("Time Flag {0} is not supported!", this.FrameStatus.TimeFlag));
               }
          }
     }
}
