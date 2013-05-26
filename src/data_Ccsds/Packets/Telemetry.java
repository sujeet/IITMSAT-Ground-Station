package data_Ccsds.Packets;

import java.io.UnsupportedEncodingException;

import data.NotImplementedException;
import data_Ccsds.Function.ArgumentNullException;
import data_Ccsds.ParameterCode.ParameterCode;
import data_Ccsds.ParameterCode.ParameterConverter;

/// <summary>PUS telemetry packet.</summary>
public class Telemetry extends CcsdsPacket
{
    //#region Data Field Header (variable length)
    /// <summary>This indicate the service to which the packet relate.</summary>
    public byte ServiceType; 
    public byte getServiceType() {
        return ServiceType;
    }

    public void setServiceType(byte serviceType) {
        ServiceType = serviceType;
    }

    /// <summary>
    /// Together with the service type, the subtype uniquely identifies
    /// the nature of the service request constitued by this telecommand packet.
    /// </summary>
    public byte ServiceSubtype; 
    public byte getServiceSubtype() {
        return ServiceSubtype;
    }

    public void setServiceSubtype(byte serviceSubtype) {
        ServiceSubtype = serviceSubtype;
    }

    /// <summary>The Packet Subcounter field's value.</summary>
    public byte PacketSubcounter; 
    public byte getPacketSubcounter() {
        return PacketSubcounter;
    }

    public void setPacketSubcounter(byte packetSubcounter) {
        PacketSubcounter = packetSubcounter;
    }

    /// <summary>The Destination ID field's value.</summary>
    public long DestinationId; 
    public long getDestinationId() {
        return DestinationId;
    }

    public void setDestinationId(long destinationId) {
        DestinationId = destinationId;
    }

    /// <summary>The Time field's value.</summary>
    public TimeSpan Time;


    /// <summary>Gets a value indicating whether this instance has a Packet Error Control field.</summary>
    /// <value><c>true</c> if this instance has a Packet Error Control field; otherwise, <c>false</c>.</value>
    public boolean HasPacketErrorControlField()
    {
        return EffectiveSettings.HasPacketErrorControl;
    }

    /// <summary>Gets the alignment of the Packet Data Field in bytes.</summary>
    /// <value>he alignment of the Packet Data Field in bytes.</value>
    public int PacketDataFieldAlignment;
    public int getPacketDataFieldAlignment() {
        PacketDataFieldAlignment=EffectiveSettings.DataFieldPadding;	
        return PacketDataFieldAlignment;
    }

    /// <summary>Gets or sets the global telemetry settings.</summary>
    /// <value>The global telemetry settings.</value>
    /// <remarks>
    /// These settings apply to all threads if not overriden by per-thread settings using the <see cref="Settings"/> property.
    /// 
    /// If you want to get the effective settings, use <see cref="EffectiveSettings"/> instead.
    /// </remarks>
    public static TelemetrySettings GlobalSettings = null; 
    public static TelemetrySettings getGlobalSettings() {
        return GlobalSettings;
    }

    public static void setGlobalSettings(TelemetrySettings globalSettings) {
        GlobalSettings = globalSettings;
    }

    /// <summary>Gets or sets the per-thread telemetry settings.</summary>
    /// <value>The telemetry settings.</value>
    /// <remarks>
    /// These settings are per-thread. If not set, <see cref="GlobalSettings"/> will be used.
    /// 
    /// If you just want to get the effective settings, use <see cref="EffectiveSettings"/> instead.
    /// </remarks>
    private static TelemetrySettings Settings = null;
    public static TelemetrySettings getSettings() {
        return Settings;
    }

    public static void setSettings(TelemetrySettings settings) {
        Settings = settings;
    }

    /// <summary>Gets the effective telemetry settings.</summary>
    public static TelemetrySettings EffectiveSettings;
    public static TelemetrySettings getEffectiveSettings() throws InvalidOperationException {
        if(Settings != null)
            return Settings;
        else if(GlobalSettings != null)
            return GlobalSettings;
        else
            throw new InvalidOperationException("No telemetry settings have been set using either Telemetry.GlobalSettings or Telemetry.Settings.");

    }

    /// <summary>Distinguish between telecommand (=1/<c>true</c>) and telemetry (=0/<c>false</c>) packets.</summary>
    /// <remarks><see cref="Telemetry"/> implementation always returns <c>false</c>.</remarks>
    public boolean Type; 
    public boolean isType() {
        Type=false;
        return Type;
    }

    //#endregion

    //#region Constructors
    /// <summary>Initializes a new instance of the <see cref="Telemetry"/> class.</summary>
    public Telemetry() {super(); }

    /// <summary>Initializes a new instance of the <see cref="Telemetry"/> class.</summary>
    /// <param name="applicationProcessId">The Application Process ID (APID).</param>
    public Telemetry(int applicationProcessId) throws ArgumentOutOfRangeException
    {
        super();
        setApplicationProcessId(applicationProcessId);
    }

    /// <summary>Initializes a new instance of the <see cref="Telemetry"/> class.</summary>
    /// <param name="applicationProcessId">The Application Process ID (APID).</param>
    /// <param name="serviceType">The Service Type.</param>
    /// <param name="serviceSubtype">The Service Subtype.</param>
    public Telemetry(int applicationProcessId, byte serviceType, byte serviceSubtype) throws ArgumentOutOfRangeException
    {
        setApplicationProcessId(applicationProcessId);
        ServiceType = serviceType;
        ServiceSubtype = serviceSubtype;
    }

    /// <summary>Initializes a new instance of the <see cref="Telemetry"/> class.</summary>
    /// <param name="applicationProcessId">The Application Process ID (APID).</param>
    /// <param name="serviceType">The Service Type.</param>
    /// <param name="serviceSubtype">The Service Subtype.</param>
    /// <param name="sequenceCount">The Sequence Count.</param>
    public Telemetry(int applicationProcessId, byte serviceType, byte serviceSubtype, int sequenceCount) throws ArgumentOutOfRangeException
    {
        setApplicationProcessId(applicationProcessId);
        ServiceType = serviceType;
        ServiceSubtype = serviceSubtype;
        SequenceCount = sequenceCount;
    }
    //#endregion

    //#region Methods
    /// <summary>Computes the length of the Data Field Header.</summary>
    /// <returns>The length of the Data Field Header.</returns>
    protected int ComputeDataFieldHeaderLength() throws NotSupportedException
    {
        return (int)(3 + computeDfhOptionalFieldsLength());
    }

    /// <summary>Calculates the length of the Data Field Header optional fields.</summary>
    /// <returns>The length of Data Field Header optional fields.</returns>
    private int computeDfhOptionalFieldsLength() throws NotSupportedException
    {
        TelemetrySettings settings = EffectiveSettings;

        int optionalFieldsLength = 0;

        // Packet Subcounter
        if(settings.HasPacketSubcounter)
            optionalFieldsLength += 1;

        // Destination ID (only Enumerated PFCs with an integral number of bytes supported or padded _after_)
        ParameterCode destinationIdPc = settings.DestinationIdPc;
        if(destinationIdPc != null)
            optionalFieldsLength += ParameterConverter.GetByteSize(destinationIdPc);

        // Time
        ParameterCode timePc;
        if (settings.TimePcPerApid.containsKey (ApplicationProcessId)) {
            timePc = settings.TimePcPerApid.get (ApplicationProcessId);
        }
        else {
            timePc = settings.DefaultTimePc;
        }
        if(timePc != null)
            optionalFieldsLength += ParameterConverter.GetBitsSize (timePc);

        // Spare / DFH alignment
        int dfhAlignment = settings.DataFieldHeaderPadding; // alignment in bytes
        if(dfhAlignment != 0)
        {
            int dfhLength = optionalFieldsLength + 3; // Compute current length of DHF
            optionalFieldsLength += (dfhAlignment - (dfhLength % dfhAlignment)) % dfhAlignment; // Add missing byte count to align
        }

        return ((int)optionalFieldsLength);
    }

    /// <summary>Convert the Data Field Header field of the current <see cref="CcsdsPacket"/> instance to bytes into the specified buffer.</summary>
    /// <param name="buffer">The destination buffer of the bytes.</param>
    /// <param name="start">The index in bytes at which the Data Field Header starts in the buffer.</param>
    /// <returns>The number of bytes written into the buffer.</returns>
    /// <exception cref="System.ArgumentOutOfRangeException">The buffer is too small to put the data at the specified offset.</exception>
    protected int WriteDataFieldHeaderToBuffer(byte[] buffer, int start) throws UnsupportedEncodingException, NotSupportedException, ArgumentNullException, ArgumentOutOfRangeException
    {
        TelemetrySettings settings = EffectiveSettings;

        int index = start;

        // Telemetry Packet PUS Version Number = 1 (1 byte)
        buffer[index++] = 0x10;

        // Service Type (1 byte)
        buffer[index++] = ServiceType;

        // Service Subtype (1 byte)
        buffer[index++] = ServiceSubtype;

        // Packet Subcounter (0/1 byte)
        if(settings.HasPacketSubcounter)
            buffer[index++] = PacketSubcounter;

        // Destination ID (only Enumerated PFCs with an integral number of bytes supported or padded _after_)
        ParameterCode destinationIdPc = settings.DestinationIdPc;
        if(destinationIdPc != null)
        {
            int insertedBits = ParameterConverter.InsertValue(buffer,
                                                              index * 8,
                                                              DestinationId,
                                                              destinationIdPc);
            index += ParameterConverter.GetByteSize(insertedBits);
        }

        // Time
        ParameterCode timePc;
        if (settings.TimePcPerApid.containsKey (ApplicationProcessId)) {
            timePc = settings.TimePcPerApid.get (ApplicationProcessId);
        }
        else {
            timePc = settings.DefaultTimePc;
        }
        if(timePc != null)
        {
            int insertedBits = ParameterConverter.InsertValue(buffer, index * 8, Time, timePc);
            index += ParameterConverter.GetByteSize(insertedBits);
        }

        // DFH Spare (alignment)
        int dfhAlignment = settings.DataFieldHeaderPadding; // alignment in bytes
        if(dfhAlignment != 0)
        {
            int dfhLength = index - start; // Compute current length of DHF
            index += (dfhAlignment - (dfhLength % dfhAlignment)) % dfhAlignment; // Add missing byte count to align
        }

        // Return number of bytes written
        return index - start;
    }

    /// <summary>Reads a <see cref="Telemetry"/> packet from a buffer.</summary>
    /// <param name="buffer">The buffer containing the <see cref="Telemetry"/> packet starting at byte 0.</param>
    /// <returns>The read <see cref="Telemetry"/> packet.</returns>
    public static Telemetry FromBuffer(byte[] buffer) throws ArgumentNullException, ArgumentException, InvalidChecksumException, NotSupportedException, NotImplementedException
    {
        return FromBuffer(buffer, 0);
    }

    /// <summary>Reads a <see cref="Telemetry"/> packet from a buffer.</summary>
    /// <param name="buffer">The buffer containing the <see cref="Telemetry"/> packet.</param>
    /// <param name="start">The index in bytes of the start of the <see cref="Telemetry"/> packet in the buffer.</param>
    /// <returns>The read <see cref="Telemetry"/> packet.</returns>
    public static Telemetry FromBuffer(byte[] buffer, int start) throws ArgumentNullException, ArgumentException, InvalidChecksumException, NotSupportedException, NotImplementedException
    {
        TelemetrySettings settings = EffectiveSettings;

        Telemetry telemetry = new Telemetry();

        // Get checksum type from settings
        telemetry.checksumType = settings.ChecksumType;

        // Current index in bytes of the decoding (absolute to the start of the buffer) 
        int index = start;

        // Packet Header and PEC check
        telemetry.FillHeadersAndPecFromBuffer(buffer, start);
        index += CcsdsPacket.HeaderLength;

        //#region Data Field Header
        // Check TM Packet PUS Version Number (only support 1)
        int tmSourcePacketPusVersionNumber = (buffer[index++] >> 4) & 0x07;
        if(tmSourcePacketPusVersionNumber != 1)
            throw new NotSupportedException("The telemetry packet contained in the buffer refers to an unsupported PUS version "+tmSourcePacketPusVersionNumber+", only 1 is supported." );

        // Service Type
        telemetry.ServiceType = buffer[index++];

        // Service Subtype
        telemetry.ServiceSubtype = buffer[index++];

        // Packet Subcounter
        if(settings.HasPacketSubcounter)
            telemetry.PacketSubcounter = buffer[index++];

        // Destination ID (only Enumerated PFCs with an integral number of bytes supported or padded after)
        ParameterCode destinationIdPc = settings.DestinationIdPc;
        if(destinationIdPc != null)
        {
            telemetry.DestinationId = (long) ParameterConverter.ExtractValue (buffer, destinationIdPc, index * 8); // offset is bytes, need bits!
            index += ParameterConverter.GetByteSize(ParameterConverter.GetBitsSize(destinationIdPc));
        }

        // Time
        ParameterCode timePc;
        if (settings.TimePcPerApid.containsKey (telemetry.ApplicationProcessId)) {
            timePc = settings.TimePcPerApid.get (telemetry.ApplicationProcessId);
        }
        else {
            timePc = settings.DefaultTimePc;
        }
        if(timePc != null)
        {
            telemetry.Time = (TimeSpan) ParameterConverter.ExtractValue(buffer, timePc, index * 8); // offset is bytes, need bits!
            index += ParameterConverter.GetByteSize(ParameterConverter.GetBitsSize(timePc)); ;
        }

        // DFH Spare (alignment)
        int dfhAlignment = settings.DataFieldHeaderPadding; // alignment in bytes
        if(dfhAlignment != 0)
        {
            int dfhLength = index - (start + CcsdsPacket.HeaderLength); // Compute current length of DHF
            index += (dfhAlignment - (dfhLength % dfhAlignment)) % dfhAlignment; // Add missing byte count to align
        }
        //#endregion

        // Data
        telemetry.FillDataFromBuffer(buffer, start, index);

        return telemetry;
    }

    /// <summary>Reads a <see cref="Telemetry"/> packet from a buffer using the specified <see cref="TelemetrySettings"/>.</summary>
    /// <param name="buffer">The buffer containing the <see cref="Telemetry"/> packet.</param>
    /// <param name="start">The index in bytes of the start of the <see cref="Telemetry"/> packet in the buffer.</param>
    /// <param name="settings">The <see cref="TelemetrySettings"/> to use to read the packet from the buffer.</param>
    /// <returns>The read <see cref="Telemetry"/> packet.</returns>
    public static Telemetry FromBuffer(byte[] buffer, int start, TelemetrySettings settings) throws ArgumentNullException, ArgumentException, InvalidChecksumException, NotSupportedException, NotImplementedException
    {
        // Save old settings and apply new ones
        TelemetrySettings oldSettings = Telemetry.Settings;
        Telemetry.Settings = settings;

        Telemetry telemetry;
        try
        {
            // Read telemetry
            telemetry = FromBuffer(buffer, start);
        }
        finally
        {
            // Put settings back
            Telemetry.Settings = oldSettings;
        }
        return telemetry;
    }
    //#endregion

    @Override
    public int PacketDataFieldAlignment() {
        PacketDataFieldAlignment=EffectiveSettings.DataFieldPadding;	
        return PacketDataFieldAlignment;
    }
}
