/**
 * Intern representation of a domain, plus a set of helper static methods to
 * parse strings like 127:2010,224.5.6.7:8910.
 *
 * Usually, you don't have to use this, and just rely on {@link bus.start}
 * with a null parameter. However, if you want to parse a command line
 * parameter, or output the result of Ivy libary guessing order, then you can
 * use those methods.
 *
 * @author Yannick Jestin <a * href="mailto:yannick.jestin@enac.fr">yannick.jestin&enac.fr</a>
 * @since 1.2.16
 */
package fr.dgac.ivy;

import java.util.regex.*;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import gnu.getopt.Getopt;

public class Domain {

  /**
   * the port for the UDP rendez vous, if none is supplied.
   */
  public static final int DEFAULT_PORT = 2010;

  /**
   * the domain for the UDP rendez vous.
   */
  public static final String DEFAULT_DOMAIN = "127.255.255.255:" + DEFAULT_PORT;

  // private fields
  private String domainaddr;
  private int port;
  private static Pattern numbersPoint, exp;

  public Domain(String ddomainaddr , int dport) { this.domainaddr = ddomainaddr;this.port = dport; }

  @Override public String toString() { return domainaddr + ":" + port; }

  // accessors
  public String getDomainaddr() { return domainaddr; }
  public int getPort() { return port; }


  static {
    try {
      numbersPoint = Pattern.compile("([0-9]|\\.)+");
      exp = Pattern.compile( "^(\\d+\\.\\d+\\.\\d+\\.\\d+).*");
    } catch (PatternSyntaxException res) {
      res.printStackTrace();
      System.out.println("Regular Expression bug in Ivy source code ... bailing out");
      throw new RuntimeException();
    }
  }
  

  static final String getDomain(final String domainbus) {
    String db = null;
    db = domainbus;
    if ( db == null ) db = System.getProperty("IVYBUS");
    if ( db == null ) db = DEFAULT_DOMAIN;
    return db;
  }

  /*
  private boolean isInDomain( InetAddress host ){
    return true;
   // TODO check if this function is useful. for now, it always returns true
   // deprecated since we use Multicast. How to check when we are in UDP
   // broadcast ?
   //
    byte rem_addr[] = host.getAddress();
    for ( int i = 0 ; i < domainaddrList.size(); i++ ) {
      byte addr[] = ((InetAddress)domainaddrList.elementAt(i)).getAddress();
      int j ;
      for (  j = 0 ; j < 4 ; j++  )
        if ( (addr[j] != -1) && (addr[j] != rem_addr[j]) ) break;
      if ( j == 4 ) {
        traceDebug( "host " + host + " is in domain\n" );
	return true;
      }
    }
    traceDebug( "host " + host + " Not in domain\n" );
    return false;
  }
  */

  private static int extractPort(String net) { // returns 0 if no port is set
    int sep_index = net.lastIndexOf( ":" );
    int port= ( sep_index == -1 ) ? 0 :Integer.parseInt( net.substring( sep_index +1 ));
    // System.out.println("net: ["+net+"]\nsep_index: "+sep_index+"\nport: "+port);
    //System.out.println("next port: "+port);
    return port;
  }

  private static String expandDomain(String net) throws IvyException {
    // System.out.println("debug: net=[" + net+ "]");
    int sep_index = net.lastIndexOf( ":" );
    if ( sep_index != -1 ) { net = net.substring(0,sep_index); }
    try {
      Matcher m = numbersPoint.matcher(net);
      if (!m.matches()) {
	// traceDebug("should only have numbers and point ? I won't add anything... " + net);
	return "127.255.255.255";
	// return net;
      }
      net += ".255.255.255";
      Matcher mm= exp.matcher(net);
      if (!mm.matches()) {
	System.out.println("Bad broascat addr " + net);
	throw new IvyException("bad broadcast addr");
      }
      net=mm.group(1);
    } catch ( PatternSyntaxException e ){
      e.printStackTrace();
      throw new RuntimeException();
    }
    //System.out.println("next domain: "+net);
    return net;
  }

  /**
   * returns the domain bus.
   *
   * @since 1.2.8
   * @param progname The name of your program, for error message
   * @param args the String[] of arguments passed to your main()
   * @return returns the domain bus, ascending priority : ivy default bus, IVY_BUS
   * property, -b domain on the command line
   */

  public static final String getDomainArgs(final String progname, final String[] args) {
    Getopt opt = new Getopt(progname , args , "b:");
    int c;
    if ( ((c = opt.getopt()) != -1) && c == 'b' ) return opt.getOptarg();
    return getDomain(null);
  }

  final static List<Domain> parseDomains(final String domainbus) {
    // assert(domainbus!=null);
    List<Domain> d = new ArrayList<Domain>();
    for (String s: domainbus.split(",")) {
      try {
        d.add(new Domain(expandDomain(s) , extractPort(s)));
      } catch (IvyException ie) {
        // do nothing
        ie.printStackTrace();
      }
    }
    // fixes the port values ...
    Collections.reverse(d);
    int lastport = DEFAULT_PORT;
    for (Domain dom : d) {
      if (dom.port == 0) dom.port = lastport;
      lastport = dom.port;
    }
    Collections.reverse(d);
    return d;
  }


  /**
   * prints a human readable representation of the list of domains.
   *
   * @since 1.2.9
   */
  static public String domains(String toparse) {
    StringBuffer s = new StringBuffer();
    if (toparse == null) toparse = getDomain(toparse);
    for (Domain dd : parseDomains(toparse)) s.append(dd.getDomainaddr() + ":" + dd.getPort() + " ");
    return s.toString();
  }

}
