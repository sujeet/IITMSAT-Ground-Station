package data_Ccsds.Function;

/// <summary>Possible values for the result of a check on an housekeeping parameter value after calibration.</summary>
public enum MonitoringCheck //:byte
{
	/// <summary>Value is nominal.</summary>
	Nominal,
	/// <summary>Value is in warning.</summary>
	Warning,
	/// <summary>Value is in warning because it is under the lower warning bound.</summary>
	WarningLow,
	/// <summary>Value is in warning because it is higher than the upper warning bound.</summary>
	WarningHigh,
	/// <summary>Value is in danger.</summary>
	Danger,
	/// <summary>Value is in danger because it is under the lower warning bound.</summary>
	DangerLow,
	/// <summary>Value is in danger because it is higher than the upper warning bound.</summary>
	DangerHigh
}
