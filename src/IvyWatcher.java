/**
 * IvyWatcher, A private Class for the Ivy rendezvous
 *
 * @author	Yannick Jestin
 * @author	Francois-Regis Colin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (C) CENA
 *
 * right now, the rendez vous is either an UDP socket or a TCP multicast.
 * The watcher will answer to
 * each peer advertising its arrival on the bus. The intrinsics of Unix are so
 * that the broadcast is done using the same socket, which is not a good
 * thing.
 *
 * CHANGELOG:
 *  1.2.16
 *    - now uses the synchronized wrappers of the Java API for all collections
 *    - move out the Domain related-code to the Domain class
 *  1.2.15
 *    - allows the fine tuning of the IvyClient socket buffersize using
 *    IVY_BUFFERSIZE property
 *  1.2.14
 *    - tries to fix a lock on accept() by becoming a Thread instead of
 *    runnalbe (see tests/test2)
 *    - removed unread field (domainaddr, e.g.)
 *    - throws RuntimeException instead of System.exit(), allows code reuse
 *    - switch from gnu regexp (deprecated) to the built in java regexp
 *    - add generic types to declarations
 * 1.2.13:
 *  - TCP_NO_DELAY to disable Nagle's algorithm
 *  - private static ?! pour already present ...
 * 1.2.9:
 *  - added an application Id in the UDP broadcast. It seems to be ok with
 *  most implementations ( VERSION PORT APPID APPNAME \n) is compatible with (VERSION
 *  APPID). If I receive a broadcast with with the same TCP port number,
 *  I ignore the first and accept the new ones
 * 1.2.8:
 *  - alreadyBroadcasted was static, thus Ivy Agents within the same JVM used
 *    to share the list of agents already connected. A nasty bug.
 * 1.2.7:
 *  - better handling of multiple connexions from the same remote agent when
 *    there are different broadcast addresses ( introduced the alreadyBroadcasted
 *    function )
 * 1.2.6:
 *  - IOException now goes silent when we asked the bus to stop()
 *  - use a new buffer for each Datagram received, to prevent an old bug
 * 1.2.5:
 *  - getDomain now sends IvyException for malformed broadcast addresses
 *  - uses apache jakarta-regexp instead of gnu-regexp
 *  - throws an IvyException if the broadcast domain cannot be resolved
 * 1.2.4:
 *  - sends the broadcast before listening to the other's broadcasts.
 *    I can't wait for all the broadcast to be sent before starting the listen
 *    mode, otherwise another agent behaving likewise could be started
 *    meanwhile, and one would not "see" each other.
 *  - (REMOVED) allows the connexion from a remote host with the same port number
 *    it's too complicated to know if the packet is from ourselves...
 *  - deals with the protocol errors in a more efficient way. The goal is not
 *    to loose our connectivity because of a rude agent.
 *    fixes Bug J005 (YJ + JPI)
 * 1.2.3:
 *  - the packet sending is done in its own thread from now on (PacketSender)
 *    I don't care stopping it, since it can't be blocked.
 *  - checks whether I have been interrupted just after the receive (start()
 *  then stop() immediately).
 * 1.2.1:
 *  - can be Interrupted during the broadcast Send. I catch the
 *    and do nothing with it. InterruptedIOException
 *  - changed the fill character from 0 to 10, in order to prevent a nasty bug
 *    on Windows XP machines
 *  - fixed a NullPointerException while trying to stop a Thread before having
 *    created it.
 * 1.0.12:
 *  - setSoTimeout on socket
 *  - the broadcast reader Thread goes volatile
 * 1.0.10:
 *  - isInDomain() is wrong  in multicast. I've removed it
 *  - there was a remanence effect in the datagrampacket buffer. I clean it up after each message
 *  - cleaned up the getDomain() and getPort() code 
 *  - close message sends an interruption on all threads for a clean exit
 *  - removed the timeout bug eating all the CPU resources
 *  - now handles a Vector of broadcast listeners
 */
package fr.dgac.ivy ;
import java.lang.Thread;
import java.net.*;
import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.*;

class IvyWatcher implements Runnable {
  private static boolean debug = (System.getProperty("IVY_DEBUG")!=null);
  private boolean alreadyIgnored = false;
  private Ivy bus;			/* master bus controler */
  private DatagramSocket broadcast;	/* supervision socket */
  private int port;
  private String domainaddr;
  private volatile boolean keeprunning = false;
  private Thread listenThread = null;
  private InetAddress group;
  // FIXME should not be static ? (findbugs)
  private static int serial=0;
  private int myserial=serial++;
  private String busWatcherId = null;
  private static Pattern recoucou;


  /**
   * creates an Ivy watcher
   * @param bus the bus
   * @param net the domain
   */
  IvyWatcher(Ivy bus,String domainaddr,int port) throws IvyException {
    this.bus = bus;
    this.port = port;
    this.domainaddr = domainaddr;
    busWatcherId=bus.getWatcherId();
    keeprunning = true ;
    // create the MulticastSocket
    try {
      group = InetAddress.getByName(domainaddr);
      broadcast = new MulticastSocket(port);
      if (group.isMulticastAddress()) {
	  ((MulticastSocket)broadcast).joinGroup(group);
      }
      broadcast.setSoTimeout(Ivy.TIMEOUTLENGTH);
    } catch ( UnknownHostException uhe ) {
      throw new IvyException("IvyWatcher unknown host exception" + uhe );
    } catch ( IOException e ) {
      throw new IvyException("IvyWatcher I/O error" + e );
    }
  }
  
  /**
   * the behaviour of each thread watching the UDP socket.
   */
  public void run() {
    traceDebug("Thread started"); // THREADDEBUG
    listenThread=Thread.currentThread();
    listenThread.setName("Ivy Watcher thread for "+domainaddr+":"+port);
    // listenThread.setDaemon(true); // not possible in the treadpool ? FIXME
    traceDebug("beginning of a watcher Thread");
    try {
      while( keeprunning ) {
	try {
	  byte buf[] = new byte[256];
	  DatagramPacket packet=new DatagramPacket(buf,buf.length);
	  broadcast.receive(packet);
	  bus.pushThread("UDP packet received");
	  if ( !keeprunning ) break; // I was summoned to leave during the receive
	  String msg = new String(buf,0,packet.getLength());
	  boolean b = (parsePacket(packet, msg));
	  bus.popThread("UDP packet processed");
	  if (b) continue; else break;
	} catch (InterruptedIOException jii ){
	  // System.out.println("WTF UDP packet interrupted");
	  // another thread took place, not important
	  if ( !keeprunning ) { break ;}
	}
      } // while
    } catch (java.net.SocketException se ){
      if ( keeprunning ) {
	traceDebug("socket exception, continuing anyway on other Ivy domains "+se);
      }
    } catch (IOException ioe ){
      System.out.println("IO Exception, continuing anyway on other Ivy domains "+ioe);
    }
    traceDebug("Thread stopped"); // THREADDEBUG
  } 

  /**
   * parses the content of a received packet.
   * @return true if the watcher can keep looping (continue), false if there's
   * a big problem (break).
   *
   * first checks if the message structure is correct
   * then checks if it's our own broadcasts
   * then checks if there's already a concurrent connexion in progress
   * if it's ok, creates a new IvyClient
   *
   */
  private boolean parsePacket(DatagramPacket packet, String msg) {
    int remotePort=0;
    InetAddress remotehost = null;
    String remotehostname = null;
    try {
      remotehost=packet.getAddress();
      remotehostname=remotehost.getHostName();
      Matcher m = recoucou.matcher(msg);
      // is it a correct broadcast packet ?
      if (!m.matches()) {
	System.err.println("Ignoring bad format broadcast from "+ remotehostname+":"+packet.getPort());
	return true;
      }
      // is it the correct protocol version ?
      int version = Integer.parseInt(m.group(1));
      if ( version < Protocol.PROTOCOLMINIMUM ) {
	System.err.println("Ignoring bad format broadcast from "+
	    remotehostname+":"+packet.getPort()
	    +" protocol version "+remotehost+" we need "+Protocol.PROTOCOLMINIMUM+" minimum");
	return true;
      }
      // is it my own broadcast ?
      remotePort = Integer.parseInt(m.group(2));
      if (bus.getAppPort()==remotePort) { // if (same port number)
	if (busWatcherId!=null) {
	  traceDebug("there's an appId: "+m.group(3));
	  String otherId=m.group(3);
	  String otherName=m.group(4);
	  if (busWatcherId.compareTo(otherId)==0) {
	    // same port #, same bus Id, It's me, I'm outta here
	    traceDebug("ignoring my own broadcast");
	    return true;
	  } else {
	    // same port #, different bus Id, it's another agent
	    // implementing the Oh Soooo Cool watcherId undocumented
	    // unprotocolar Ivy add on 
	    traceDebug("accepting a broadcast from a same port by "+otherName);
	  }
	} else {
	  // there's no watcherId in the broacast. I fall back to a
	  // crude strategy: I ignore the first broadcast with the same
	  // port number, and accept the following ones
	  if (alreadyIgnored) {
	    traceDebug("received another broadcast from "+ remotehostname+":"+packet.getPort()
		+" on my port number ("+remotePort+"), it's probably someone else");
	  } else {
	    alreadyIgnored=true;
	    traceDebug("ignoring a broadcast from "+ remotehostname+":"+packet.getPort()
		+" on my port number ("+remotePort+"), it's probably me");
	    return true;
	  }
	}
      } // end if (same port #)

      // it's definitively not me, let's shake hands !
      traceDebug("broadcast accepted from " +remotehostname
	  +":"+packet.getPort()+", port:"+remotePort+", protocol version:"+version);

      if (!alreadyBroadcasted(remotehost.toString(),remotePort)) {
	traceDebug("no known agent originating from " + remotehost + ":" + remotePort);
	try {
	  Socket s = new Socket(remotehost,remotePort);
	  s.setReceiveBufferSize(bus.getBufferSize());
	  s.setTcpNoDelay(true);
	  if (!bus.createIvyClient(s,remotePort,false)) return false ;
	} catch ( java.net.ConnectException jnc ) {
	  traceDebug("cannot connect to "+remotehostname+":"+remotePort+", he probably stopped his bus");
	}
      } else {
	traceDebug("there is already a request originating from " + remotehost + ":" + remotePort);
      }
    } catch (NumberFormatException nfe) {
      System.err.println("Ignoring bad format broadcast from "+remotehostname);
      return true;
    } catch ( UnknownHostException e ) {
      System.err.println("Unkonwn host "+remotehost +","+e.getMessage());
    } catch ( IOException e) {
      System.err.println("can't connect to "+remotehost+" port "+ remotePort+e.getMessage());
      e.printStackTrace();
    }
    return true;
  }

  /**
   * stops the thread waiting on the broadcast socket
   */
  synchronized void doStop() {
    traceDebug("begining stopping");
    keeprunning = false; 
    if (listenThread != null) listenThread.interrupt();  
    broadcast.close();
    traceDebug("stopped");
  }

  private class PacketSender implements Runnable {
    // do I need multiple packetsenders ? Well, there is one PacketSender per
    // domain.
    DatagramPacket packet;
    String data;
    Ivy bus;

    public PacketSender(String data, Ivy b) {
      this.data=data;
      bus = b;
      packet=new DatagramPacket(data.getBytes(),data.length(),group,port);
      bus.getPool().execute( PacketSender.this );
    }

    public void run() {
      bus.pushThread("packet sender started");
      traceDebug("PacketSender thread started"); // THREADDEBUG
      Thread.currentThread().setName("Ivy Packet sender");
      try {
	broadcast.send(packet);
      } catch (InterruptedIOException e) {
	// somebody interrupts my IO. Thread, do nothing.
	System.out.println(e.bytesTransferred+" bytes transferred anyway, out of " + data.length());
	e.printStackTrace();
	traceDebug("IO interrupted during the broadcast. Do nothing");
      } catch ( IOException e ) {
	System.out.println("Broadcast Error " + e.getMessage()+" continuing anyway");
	// cannot throw new IvyException in a run ...
	e.printStackTrace();
      }
      try { Thread.sleep(100); } catch (InterruptedException ie ){ }
      traceDebug("PacketSender thread stopped"); // THREADDEBUG
      bus.popThread("packet sender finished"); // one of the senders has finished its work, plus extra time
    }
  }

  synchronized void doStart() throws IvyException {
    // String hello = Ivy.PROTOCOLVERSION + " " + bus.getAppPort() + "\n";
    String hello = Protocol.PROTOCOLVERSION + " " + bus.getAppPort() + " "+busWatcherId+" "+bus.getSelfIvyClient().getApplicationName()+"\n";
    if (broadcast==null) throw new IvyException("IvyWatcher PacketSender null broadcast address");
    bus.getPool().execute(this);
    new PacketSender(hello,bus); // notifies our arrival on each domain: protocol version + port
  }

  /*
   * since 1.2.7 pre ....
   * went local instead of static ! fixed a nasty bug in 1.2.8
   * checks if there is already a broadcast received from the same address
   * on the same port
   *
   */
  private Map<String,Integer> alreadySocks=Collections.synchronizedMap(new HashMap<String,Integer>());
  private boolean alreadyBroadcasted(String s,int port) {
    // System.out.println("DEBUUUUUUUG " + s+ ":" + port);
    if (s==null) return false;
    synchronized (alreadySocks) {
      Integer i = alreadySocks.get(s);
      if (((i!=null)&&(i.compareTo(port))==0)) return true;
      alreadySocks.put(s,port);
      return false;
    }
  }

  private void traceDebug(String s){
    if (debug) System.out.println("-->IvyWatcher["+myserial+","+bus.getSerial()+"]<-- "+s);
  }

  static {
    try {
      recoucou  = Pattern.compile("([0-9]+) ([0-9]+) ([^ ]*) (.*)",Pattern.DOTALL);
    } catch (PatternSyntaxException res) {
      res.printStackTrace();
      System.out.println("Regular Expression bug in Ivy source code ... bailing out");
      throw new RuntimeException();
    }
  }
  

} // class IvyWatcher
