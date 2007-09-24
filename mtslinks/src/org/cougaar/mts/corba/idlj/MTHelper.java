package org.cougaar.mts.corba.idlj;


/**
* org/cougaar/mts/corba/idlj/MTHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from src/org/cougaar/mts/corba/MT.idl
* Wednesday, October 27, 2004 1:32:23 PM EDT
*/

abstract public class MTHelper
{
  private static String  _id = "IDL:cougaar/MT:1.0";

  public static void insert (org.omg.CORBA.Any a, org.cougaar.mts.corba.idlj.MT that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.cougaar.mts.corba.idlj.MT extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (org.cougaar.mts.corba.idlj.MTHelper.id (), "MT");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.cougaar.mts.corba.idlj.MT read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_MTStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.cougaar.mts.corba.idlj.MT value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static org.cougaar.mts.corba.idlj.MT narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof org.cougaar.mts.corba.idlj.MT)
      return (org.cougaar.mts.corba.idlj.MT)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      org.cougaar.mts.corba.idlj._MTStub stub = new org.cougaar.mts.corba.idlj._MTStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}
