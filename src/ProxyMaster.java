/**
 * ProxyMaster: Ivy relay, first attempt, DO NOT USE
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (c) ENAC
 *
 * changelog:
 *   1.2.16
 *    - switches from Vector to proper collections
 *   1.2.14
 *    - throws RuntimeException instead of System.exit(), allows code reuse
 *    - switch from gnu regexp (deprecated) to the built in java regexp
 *    - add generic types to declarations
 *   1.2.13
 *    - adds support for RESyntaxException
 *   1.2.12
 */
package fr.dgac.ivy.tools ; // TODO go into sub tools, and build a shell/.BAT script
import fr.dgac.ivy.* ;
import java.io.*;
import java.net.*;
import java.util.Map ;
import java.util.HashMap ;
import java.util.ArrayList ;
import java.util.Collection ;
import java.util.Properties ;
import gnu.getopt.Getopt;
import java.util.regex.*;

class ProxyMaster {

  private ServerSocket serviceSocket;
  private static boolean debug=false;
  private boolean doRun=true; // stops running when set to false
  private Collection<SubReader> proxyClients = new ArrayList<SubReader>();
  private Map<String,SubReader> ghostFathers = new HashMap<String,SubReader>(); // key: ghostId value: SubReader
  private static int serial=0;

  public static final int DEFAULT_SERVICE_PORT = 3456 ;
  public static final String DEFAULTNAME = "ProxyMaster";
  public static final String helpmsg = "usage: java fr.dgac.ivy.ProxyMaster [options]\n\t-p\tport number, default "+DEFAULT_SERVICE_PORT+"\n\t-q\tquiet, no tty output\n\t-d\tdebug\n\t-h\thelp\nListens on the TCP port for ProxyClients to join.\n";
  static Pattern helloRE, getId,fwdPuppet,fwdGhost;

  private static String name = DEFAULTNAME;

  static {
    try {
      helloRE=Pattern.compile("^Hello bus=(.*)");
      getId=Pattern.compile("^GetID id=(.*)");
      fwdPuppet=Pattern.compile("^ForwardPuppet id=(.*) buffer=(.*)");
      fwdGhost=Pattern.compile("^ForwardGhost id=(.*) buffer=(.*)");
    } catch ( PatternSyntaxException res ) {
      res.printStackTrace();
      System.out.println("Regular Expression bug in Ivy source code ... bailing out");
      throw new RuntimeException();
    }
  } 

  public static void main(String[] args) {
    Ivy bus;
    Getopt opt = new Getopt("ProxyMaster",args,"dqp:h");
    int c;
    int servicePort = DEFAULT_SERVICE_PORT;
    boolean quiet = false;
    while ((c = opt.getopt()) != -1) switch (c) {
    case 'q':
      quiet=true;
      break;
    case 'd':
      Properties sysProp = System.getProperties();
      sysProp.put("IVY_DEBUG","yes");
      break;
    case 'p':
      String s="";
      try {
        servicePort = Integer.parseInt(s=opt.getOptarg());
      } catch (NumberFormatException nfe) {
        System.out.println("Invalid port number: " + s );
	throw new RuntimeException();
      }
      break;
    case 'h':
    default:
      System.out.println(helpmsg);
      return;
    }
    try {
      if (!quiet) System.out.println("listening on "+servicePort);
      new ProxyMaster(servicePort);
    } catch (IOException ioe) {
      System.out.println("error, can't set up the proxy master");
      ioe.printStackTrace();
      return;
    }
  }

  public ProxyMaster(int servicePort) throws IOException {
    serviceSocket = new ServerSocket(servicePort) ;
    while ( true ) {
      try {
        new SubReader(serviceSocket.accept());
      } catch( IOException e ) {
	traceDebug("TCP socket reader caught an exception " + e.getMessage());
      }
    }
  }

  class SubReader extends Thread {
    BufferedReader in;
    PrintWriter out;
    String hostname=null;	// I will know from the socket
    String busDomain=null;	// I will know it from the Hello message

    SubReader(Socket socket) throws IOException {
      proxyClients.add(this);
      hostname = socket.getInetAddress().getHostName();
      in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
      start();
    }

    public void send(String s) { // sends a message to the SubReader peer ( a ProxyClient )
      out.println(s);
      out.flush();
    }

    public void run() {
      traceDebug("Subreader Thread started");
      String msg = null;
      try {
	while (doRun) {
	  msg=in.readLine();
	  if (msg==null) break;
	  parseMsg(msg);
	}
      } catch (IOException ioe) {
	traceDebug("Subreader exception ...");
	ioe.printStackTrace();
	throw new RuntimeException();
      }
      traceDebug("Subreader Thread stopped");
      System.out.println("ProxyClient on "+hostname+", bus "+busDomain+" disconnected");
      proxyClients.remove(this);
    }



    void parseMsg(String msg) {
      // System.out.println("PM parsing "+msg);
      Matcher m;
      if ((m=helloRE.matcher(msg)).matches()) {
	busDomain = m.group(1);
	System.out.println("PC connected from "+hostname+", on the bus "+busDomain);
      } else if ((m=getId.matcher(msg)).matches()) {
	// a new Ghost has appeared and requests an Id
	System.out.println("PM registering a new Ghost");
	String newGhostId = Integer.valueOf(serial++).toString();
	// I give it its ID
	send("ID id="+m.group(1)+" value="+newGhostId);
	ghostFathers.put(newGhostId,this); // remember the SubReader holding this Ghost
	// I ask all other Clients to prepare a puppet
	for (SubReader sr : proxyClients) {
	  if (sr!=SubReader.this) {
	    // System.out.println("propagate CreatePuppet to "+sr.busDomain);
	    sr.send("CreatePuppet id="+newGhostId);
	  } else {
	    // System.out.println("won't propagate CreatePuppet to "+sr.busDomain);
	  }
	}
      } else if ((m=fwdGhost.matcher(msg)).matches()) {
	System.out.println("PM forwarding ["+msg+"] to its Ghost");
	SubReader sr = ghostFathers.get(m.group(1));
	sr.send(msg);
      } else if ((m=fwdPuppet.matcher(msg)).matches()) {
	System.out.println("PM forwarding ["+msg+"] to all other PCs");
	for (SubReader sr: proxyClients)
	  if (sr!=SubReader.this) sr.send(msg);
      } else {
	System.out.println("error unknown message "+msg);
      }
    }
  } // class SubReader

  private static void traceDebug(String s){
    if (debug) System.out.println("-->ProxyMaster "+name+"<-- "+s);
  }

}
