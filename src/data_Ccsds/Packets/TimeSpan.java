package data_Ccsds.Packets;

public class TimeSpan
{
    public TimeSpan (long ticks)
    {
        long remaining_ticks = ticks;
        this.Days = remaining_ticks / TicksPerDay;
        remaining_ticks -= this.Days * TicksPerDay;
        
        this.Hours = remaining_ticks / TicksPerHour;
        remaining_ticks -= this.Hours * TicksPerHour;
        
        this.Minutes = remaining_ticks / TicksPerMinute;
        remaining_ticks -= this.Minutes * TicksPerMinute;
        
        this.Seconds = remaining_ticks / TicksPerSecond;
        remaining_ticks -= this.Seconds * TicksPerSecond;
        
        this.Milliseconds = remaining_ticks / TicksPerMillisecond;
        remaining_ticks -= this.Milliseconds * TicksPerMillisecond;
        
        this.Microseconds = remaining_ticks / TicksPerMicrosecond;
        remaining_ticks -= this.Microseconds * TicksPerMicrosecond;
    }
    
    public static final long TicksPerMillisecond = 10000;
    
    public static final long TicksPerDay = 24 * 60 * 60 * 1000 * TicksPerMillisecond;
    public static final long TicksPerHour = TicksPerDay / 24;
    public static final long TicksPerMinute = TicksPerHour / 60;
    public static final long TicksPerSecond = TicksPerMinute / 60;
    public static final long TicksPerMicrosecond = TicksPerMillisecond / 1000;
    
    public long Days;
    public long Hours;
    public long Minutes;
    public long Seconds;
    public long Milliseconds;
    public long Microseconds;
    public long Ticks;

}
