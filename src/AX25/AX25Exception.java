package AX25;

/// AX.25 Exception
public class AX25Exception extends Exception
{
     private static final long serialVersionUID = 8431557668496929392L;

     /// AX.25 Exception constructor
     public AX25Exception()
          { super (); }

     /// AX.25 Exception constructor
     /// <param name="message">Message</param>
     public AX25Exception(String message)
          { super (message);}

     /// AX.25 Exception constructor
     /// <param name="message">Message</param>
     /// <param name="innerException">Inner exception</param>
     public AX25Exception(String message, Exception innerException)
          {super (message, innerException);}

     /// AX.25 Exception constructor
     /// <param name="info">SerializationInfo</param>
     /// <param name="context">StreamingContext</param>
     // protected AX25Exception(SerializationInfo info, StreamingContext context)
     //      { super (info, context); }
}
