package org.cougaar.mts.corba.idlj;


/**
* org/cougaar/mts/corba/idlj/CorbaMisdeliveredMessage.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from src/org/cougaar/mts/corba/MT.idl
* Wednesday, October 27, 2004 1:32:23 PM EDT
*/

public final class CorbaMisdeliveredMessage extends org.omg.CORBA.UserException
{

  public CorbaMisdeliveredMessage ()
  {
    super(CorbaMisdeliveredMessageHelper.id());
  } // ctor


  public CorbaMisdeliveredMessage (String $reason)
  {
    super(CorbaMisdeliveredMessageHelper.id() + "  " + $reason);
  } // ctor

} // class CorbaMisdeliveredMessage
