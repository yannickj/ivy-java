/**
 * the local handle to a peer on the bus, a Thread is associated to each
 * instance, performing the socket handling, the protocol parsing, and the
 * callback running.
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * each time a connexion is made with a remote peer, the regexp are exchanged
 * once ready, a ready message is sent, and then we can send messages, 
 * die messages, direct messages, add or remove regexps, or quit. A thread is
 * created for each remote client.
 *
 *  CHANGELOG:
 *  1.2.17
 *    - fixes a synchronization issue in sendMsg
 *  1.2.16
 *    - now uses the synchronized wrappers of the Java API for all collections
 *  1.2.14
 *  - use autoboxing for the creation of Integer (instead of
 *  new Integer(int). This alows caching, avoids object allocation, and the
 *  code will be faster
 *  - removed the synchronized on boxed primitive (Integer(0) for lock, which
 *  could be cached and reused elsewhere). Lock is now a new Object()
 *  - remove the Thread.start() from the constructor, to avoid mulithread issues
 *  	see *  	http://findbugs.sourceforge.net/bugDescriptions.html#SC_START_IN_CTOR
 *  	now ,we have to call IvyClient.start() after it has been created
 *  - add generic types to declarations
 *  - remove sendBye(), which is never called
 *  - switch from gnu regexp (deprecated) to the built in java regexp
 *  1.2.12
 *  - Ping and Pong are back ...
 *  1.2.8
 *  - no CheckRegexp anymore
 *  - synchronized(regexps) pour le match et le getParen():
 *    quoting http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html  ,
 *    However, RE and RECompiler are not threadsafe (for efficiency reasons,
 *    and because requiring thread safety in this class is deemed to be a rare
 *    requirement), so you will need to construct a separate compiler or
 *    matcher object for each thread (unless you do thread synchronization
 *    yourself)
 *  - reintroduces bugs for multibus connexions. I can't fix a cross
 *    implementation bug.
 *  1.2.6
 *  - major cleanup to handle simultaneous connections, e.g., between two
 *    busses  within the same process ( AsyncAPI test is very stressful )
 *    I made an assymetric processing to elect the client that should
 *    disconnect based on the socket ports ... might work...
 *  - jakarta regexp are not meant to be threadsafe, so for match() and
 *    compile() must be enclaused in a synchronized block
 *  - now sends back an error message when an incorrect regexp is sent
 *    the message is supposed to be readable
 *  - sendMsg has no more async parameter
 *  1.2.5:
 *  - no more java ping pong
 *  1.2.5:
 *  - use org.apache.regexp instead of gnu-regexp
 *    http://jakarta.apache.org/regexp/apidocs/
 *  1.2.4:
 *  - sendBuffer goes synchronized
 *  - sendMsg now has a async parameter, allowing the use of threads to
 *    delegate the sending of messages
 *  - API change, IvyException raised when \n or \0x3 are present in bus.sendMsg()
 *  - breaks the connexion with faulty Ivy clients (either protocol or invalid
 *    regexps, closes bug J007 (CM))
 *  - sendDie now always requires a reason
 *  - invokes the disconnect applicationListeners at the end of the run()
 *    loop.
 *    closes Bug J006 (YJ)
 *  - changed the access of some functions ( sendRegexp, etc ) to protected
 *  1.2.3:
 *  - silently stops on InterruptedIOException.
 *  - direct Messages
 *  - deals with early stops during readline
 *  1.2.2:
 *  - cleared a bug causing the CPU to be eating when a remote client left the
 *    bus. closes Damien Figarol bug reported on december, 2002. It is handled
 *    in the readline() thread
 *  1.2.1:
 *  - removes a NullPointerException when stops pinging on a pinger that
 *    wasn't even started
 *  1.0.12:
 *  - introducing a Ping and Pong in the protocol, in order to detect the loss of
 *    connection faster. Enabled through the use of -DIVY_PING variable only
 *    the PINGTIMEOUT value in milliseconds allows me to have a status of the
 *    socket guaranteed after this timeout
 *  - right handling of IOExceptions in sendBuffer, the Client is removed from
 *    the bus
 *  - sendDie goes public, so does sendDie(String)
 *  - appName visibility changed from private to protected
 *  1.0.10:
 *  - removed the timeout bug eating all the CPU resources
 */
package fr.dgac.ivy ;
import java.lang.Thread;
import java.net.*;
import java.io.*;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.regex.*;
import java.util.Collection;

public class IvyClient implements Runnable {

  // private variables
  private final static int MAXPONGCALLBACKS = 10;
  // FIXME should not be static ? (findbugs)
  private static int pingSerial = 0;
  private static final Object lock = new Object();
  private static int clientSerial=0;	/* an unique ID for each IvyClient */
  private SortedMap <Integer,PingCallbackHolder>PingCallbacksTable =
    Collections.synchronizedSortedMap(new TreeMap<Integer,PingCallbackHolder>());
  private Ivy bus;
  private Socket socket;
  private BufferedReader in;
  private OutputStream out;
  private int remotePort=0;
  private volatile boolean keepgoing = true;// volatile to ensure the atomicity
  private Integer clientKey;
  private boolean discCallbackPerformed = false;
  private String remoteHostname="unresolved";

  // protected variables
  String appName="none";
  Map <Integer,Pattern>regexps = Collections.synchronizedMap(new HashMap<Integer,Pattern>());
  Map <Integer,String>regexpsText = Collections.synchronizedMap(new HashMap<Integer,String>());
  static boolean debug = (System.getProperty("IVY_DEBUG")!=null) ;
  // int protocol;
  private boolean incoming;


  IvyClient() { } // required for SelfIvyClient FIXME ?!

  IvyClient(Ivy bus, Socket socket,int remotePort,boolean incoming) throws IOException {
    synchronized(lock) { clientKey=clientSerial++; }
    this.bus = bus;
    this.remotePort = remotePort;
    this.incoming = incoming;
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    out = socket.getOutputStream();
    incoming=(remotePort==0);
    traceDebug(((incoming)?"incoming":"outgoing")+" connection on "+socket);
    this.socket = socket;
    if (!incoming) {
      synchronized(bus) {
	bus.addHalf(this); // register a half connexion
	sendSchizo();
	// the handShake will take place at the reception of the regexps.
      }
    }
    remoteHostname = socket.getInetAddress().getHostName();
  }

  // sends our ID, whether we initiated the connexion or not
  // the ID is the couple "host name,application Port", the host name
  // information is in the socket itself, the port is not known if we
  // initiate the connexion
  synchronized private void sendSchizo() throws IOException {
    traceDebug("sending our service port "+bus.getAppPort());
    Map<Integer,String> tosend=bus.getSelfIvyClient().regexpsText;
    synchronized (tosend) {
      sendString(Protocol.SCHIZOTOKEN,bus.getAppPort(),bus.getAppName());
      for (Map.Entry<Integer,String> me : tosend.entrySet())
	sendRegexp( me.getKey().intValue() , me.getValue() );
      sendString( Protocol.ENDREGEXP,0,"");
    }
  }

  public String toString() {
    return "IC["+clientKey+","+bus.getSerial()+"] "+bus.getAppName()+":"+appName+":"+remotePort; }

  public String toStringExt() {
    return "client socket:"+socket+", remoteport:" + remotePort;
  }

  /**
   *  returns the name of the remote agent.
   */
  public String getApplicationName() { return appName ; }

  /**
   *  returns the host name of the remote agent.
   *  @since 1.2.7
   */
  public String getHostName() { return remoteHostname ; }

  /**
   *  allow an Ivy package class to access the list of regexps at a
   *  given time.
   *  perhaps we should implement a new IvyApplicationListener method to
   *  allow the notification of regexp addition and deletion
   *  The content is not modifyable because String are not mutable, and cannot
   *  be modified once they are create.
   */
  public Collection<String> getRegexps() { return new ArrayList<String>(regexpsText.values()); }

  /**
   *  allow an Ivy package class to access the list of regexps at a
   *  given time.
   *  @since 1.2.4
   */
  public String[] getRegexpsArray() {
    String[] s = new String[regexpsText.size()];
    int i=0;
    for (String sr : getRegexps()) s[i++]=sr;
    return s;
  }

  /**
   * sends a direct message to the peer
   * @param id the numeric value provided to the remote client
   * @param message the string that will be match-tested
   */
  public void sendDirectMsg(int id,String message) throws IvyException {
    if ( (message.indexOf(Protocol.NEWLINE)!=-1)||(message.indexOf(Protocol.ENDARG)!=-1))
      throw new IvyException("newline character not allowed in Ivy messages");
    sendString(Protocol.DIRECTMSG,id,message);
  }
  
  /* closes the connexion to the peer  */
  protected void close(boolean notify) throws IOException {
    traceDebug("closing connexion to "+appName);
    if (notify) sendBye("hasta la vista");
    stopListening();
    socket.close(); // TODO is it necessary ? trying to fix a deadlock
  }

  /**
   * asks the remote client to leave the bus.
   * @param  message the message that will be carried
   */
  public void sendDie(String message) {
    sendString(Protocol.DIE,0,message);
  }

  /**
   * triggers a Ping, and executes the callback
   * @param  pc the callback that will be triggerred (once) when the ponc is
   * received
   */
  public void ping(PingCallback pc) throws IvyException {
    PCHadd(pingSerial,pc);
    sendString(Protocol.PING,pingSerial,"");
    incSerial();
  }

  private synchronized static void incSerial() {pingSerial++;}

  ///////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ///////////////////////////////////////////////////

  static String decode(String s) { return s.replace(Protocol.ESCAPE,'\n'); }
  static String encode(String s) { return s.replace('\n',Protocol.ESCAPE); }
  Integer getClientKey() { return clientKey ; }
  protected void sendRegexp(int id,String regexp) {sendString(Protocol.ADDREGEXP,id,regexp);}
  protected void delRegexp(int id) {sendString(Protocol.DELREGEXP,id,"");}

  protected int sendMsg(String message) {
    int count = 0;
    synchronized (regexps) {
      for (Integer key : regexps.keySet()) {
	Pattern regexp = regexps.get(key);
	Matcher m = regexp.matcher(message);
	if (m.matches())  {
	  count++; // match
	  sendResult(Protocol.MSG,key,m);
	}
      }
    }
    return count;
  }

  ///////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ///////////////////////////////////////////////////

  /* interrupt the listener thread */
  private void stopListening() {
    if ( !keepgoing ) return; // we can be summoned to quit from two path at a time
    keepgoing = false; 
    interrupt();  
  }

  /*
   * compares two peers the id is the couple (host,service port).
   * true if the peers are similar. This should not happen, it is bad
   */
  protected int distanceTo(IvyClient clnt) {
    // return clnt.clientKey.compareTo(clientKey); // Wrong. it's random...
    return (clnt.socket.getPort()-socket.getLocalPort());
  }

  //FIXME !!!! @override ? Object ?
  protected boolean myEquals(IvyClient clnt) {
    if (clnt==this) return true;
    // TODO go beyond the port number ! add some host processing, cf:
    // IvyWatcher ...
    if (remotePort==clnt.remotePort) return true;
    /*
       e.g.
       if (socket.getInetAddress()==null) return false;
       if (clnt.socket.getInetAddress()==null) return false;
       if (!socket.getInetAddress().equals(clnt.socket.getInetAddress())) return false;
    */
    return false;
  }

  /*
   * the code of the thread handling the incoming messages.
   */
  public void run() {
    traceDebug("Thread started");
    Thread thisThread = Thread.currentThread();
    thisThread.setName("Ivy client thread to "+remoteHostname+":"+remotePort);
    String msg = null;
    try {
      traceDebug("connection established with "+
	  socket.getInetAddress().getHostName()+ ":"+socket.getPort());
    } catch (Exception ie) {
      traceDebug("Interrupted while resolving remote hostname");
    }
    while ( keepgoing ) {
      try {
	if ((msg=in.readLine()) != null ) {
	  if ( !keepgoing  ) break; // early stop during readLine()
	  if (!newParseMsg(msg)) {
	    close(true);
	    break;
	  }
	} else {
	  traceDebug("readline null ! leaving the thread");
	  break;
	}
      } catch (IvyException ie) {
	traceDebug("caught an IvyException");
	ie.printStackTrace();
      } catch (InterruptedIOException ioe) {
	traceDebug("I have been interrupted. I'm about to leave my thread loop");
	if ( !keepgoing) break;
      } catch (IOException e) {
	if ( !keepgoing ) {
	  traceDebug("abnormally Disconnected from "+ socket.getInetAddress().getHostName()+":"+socket.getPort());
	}
	break;
      }
    }
    traceDebug("normally Disconnected from "+ bus.getAppName());
    bus.removeClient(this);
    // invokes the disconnect applicationListeners
    if (!discCallbackPerformed) bus.clientDisconnects(this);
    discCallbackPerformed=true;
    traceDebug("Thread stopped");
  }

  void interrupt(){
    Thread.currentThread().interrupt();  
    try {
      if (socket!=null) socket.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  protected synchronized void sendBuffer( String buffer ) throws IvyException {
    buffer += "\n";
    try {
      out.write(buffer.getBytes() );
      out.flush();
    } catch ( IOException e ) {
      traceDebug("I can't send my message to this client. He probably left");
      // first, I'm not a first class IvyClient any more
      bus.removeClient(this);
      // invokes the disconnect applicationListeners
      if (!discCallbackPerformed) bus.clientDisconnects(this);
      discCallbackPerformed=true;
      try {
        close(false);
      } catch (IOException ioe) {
	throw new IvyException("close failed"+ioe.getMessage());
      }
    }
  }

  private void sendString(Protocol type, int id, String arg) {
    try {
      sendBuffer(type.value()+" "+id+Protocol.STARTARG+arg);
    } catch (IvyException ie ) {
      System.err.println("received an exception: " + ie.getMessage());
      ie.printStackTrace();
    }
  }

  private void sendResult(Protocol type,Integer id, Matcher m) {
    try {
      StringBuffer buffer = new StringBuffer();
      buffer.append(type.value());
      buffer.append(" ");
      buffer.append(id);
      buffer.append(Protocol.STARTARG);
      for(int i=1;i<=m.groupCount();i++){
	buffer.append(m.group(i));
	buffer.append(Protocol.ENDARG);
      }
      sendBuffer(buffer.toString());
    } catch (IvyException ie ) {
      System.err.println("received an exception: " + ie.getMessage());
      ie.printStackTrace();
    } catch (StringIndexOutOfBoundsException sioobe) {
      System.out.println("arg: "+m.groupCount()+" "+m);
      sioobe.printStackTrace();
    }
  }

  private String dumpHex(String s) {
    byte[] b = s.getBytes();
    StringBuffer outDump = new StringBuffer();
    StringBuffer zu = new StringBuffer("\t");
    for (int i=0;i<b.length;i++) {
      char c = s.charAt(i);
      outDump.append(((int)c));
      outDump.append(" ");
      zu.append((c>15) ? c : 'X');
      zu.append(" ");
    }
    outDump.append(zu);
    return outDump.toString();
  }

  private String dumpMsg(String s) {
    StringBuffer deb = new StringBuffer(" \""+s+"\" "+s.length()+" cars, ");
    for (int i=0;i<s.length();i++) {
      deb.append("[");
      deb.append(s.charAt(i));
      deb.append("]:");
      deb.append(s.charAt(i));
      deb.append(", ");
    }
    return deb.toString();
  }

  protected boolean newParseMsg(String s) throws IvyException {
    if (s==null) throw new IvyException("null string to parse in protocol");
    byte[] b = s.getBytes();
    int from=0,to=0;
    Protocol msgType;
    Integer msgId;
    while ((to<b.length)&&(b[to]!=' ')) to++;
    // return false au lieu de throw
    if (to>=b.length) {
      System.out.println("Ivy protocol error from "+appName);
      return false;
    }
    msgType = Protocol.fromString(s.substring(from,to));
    from=to+1;
    while ((to<b.length)&&(b[to]!=2)) to++;
    if (to>=b.length) {
      System.out.println("Ivy protocol error from "+appName);
      return false;
    }
    try {
      msgId = Integer.valueOf(s.substring(from,to));
    } catch (NumberFormatException nfe) {
      System.out.println("Ivy protocol error from "+appName+" "+s.substring(from,to)+" is not a number");
      return false;
    }
    from=to+1;
    switch (msgType) {
      case DIE:
	traceDebug("received die Message from " + appName);
	// first, I'm not a first class IvyClient any more
	bus.removeClient(this);
	// invokes the die applicationListeners
	String message=s.substring(from,b.length);
	bus.dieReceived(this,msgId.intValue(),message);
	// makes the bus die
	bus.stop();
	try {
	  close(false);
	} catch (IOException ioe) {
	  throw new IvyException(ioe.getMessage());
	}
	break;
      case BYE:
	// the peer quits
	traceDebug("received bye Message from "+appName);
	// first, I'm not a first class IvyClient any more
	bus.removeClient(this);
	// invokes the die applicationListeners
	if (!discCallbackPerformed) bus.clientDisconnects(this);
	discCallbackPerformed=true;
	try {
	  close(false);
	} catch (IOException ioe) {
	  throw new IvyException(ioe.getMessage());
	}
	break;
      case PONG:
        PCHget(msgId);
	break;
      case PING:
	sendString(Protocol.PONG,msgId.intValue(),"");
	break;
      case ADDREGEXP:
	String regexp=s.substring(from,b.length);
	if ( bus.checkRegexp(regexp) ) {
	  try {
	    regexps.put(msgId,Pattern.compile(regexp,Pattern.DOTALL));
	    regexpsText.put(msgId,regexp);
	    bus.regexpReceived(this,msgId.intValue(),regexp);
	  } catch (PatternSyntaxException e) {
	    // the remote client sent an invalid regexp !
	    traceDebug("invalid regexp sent by " +appName+" ("+regexp+"), I will ignore this regexp");
	    sendBuffer(Protocol.ERROR.value()+" "+e.toString());
	  }
	} else {
	  // throw new IvyException("regexp Warning exp='"+regexp+"' can't match removing from "+appName);
	  traceDebug("Warning "+appName+" subscribes to '"+regexp+"', it can't match our message filter");
	  bus.regexpReceived(this,msgId.intValue(),regexp);
	}
	break;
      case DELREGEXP:
	regexps.remove(msgId);
	String text=(String)regexpsText.remove(msgId);
	bus.regexpDeleted(this,msgId.intValue(),text);
	break;
      case ENDREGEXP:
	bus.clientConnects(this);
	String srm = bus.getReadyMessage();
	if (srm!=null) sendMsg(srm);
	break;
      case MSG:
	Vector <String>v = new Vector<String>();
	while (to<b.length) {
	  while ( (to<b.length) && (b[to]!=3) ) to++;
	  if (to<b.length) {
	    v.addElement(decode(s.substring(from,to)));
	    to++;
	    from=to;
	  }
	}
	String[] tab = new String[v.size()];
	int i=0;
	for (String st: v) tab[i++]=st;
	// for developpemnt purposes
	traceDebug(tab);
	bus.getSelfIvyClient().callCallback(this,msgId,tab);
	break;
      case ERROR:
	String error=s.substring(from,b.length);
	traceDebug("Error msg "+msgId+" "+error);
  	break;
      case SCHIZOTOKEN: // aka BeginRegexp in other implementations, or MsgSync
	appName=s.substring(from,b.length);
	remotePort=msgId.intValue();
	traceDebug("the peer sent his service port: "+remotePort);
	if (incoming) {
	  // incoming connexion, I wait for his token to send him mine ...
	  synchronized(bus) {
	    try {
	      bus.addHalf(this);
	      sendSchizo();
	      bus.handShake(this); // 
	    } catch (IOException ioe) {
	      throw new IvyException(ioe.toString());
	    }
	  }
	} else {
	  // outgoing connexion
	  // I already have sent him a token
	  bus.handShake(this);
	}
	break;
      case DIRECTMSG: 
	String direct=s.substring(from,b.length);
	bus.directMessage( this, msgId.intValue(), direct );
	break;
      default:
        System.out.println("protocol error from "+appName+", unknown message type "+msgType);
	return false;
    }
    return true;
  }

  //private void sendBye() {sendString(Bye,0,"");}
  private void sendBye(String message) {sendString(Protocol.BYE,0,message);}

  private void traceDebug(String s){
    String app="noname";
    int serial=0;
    if (bus!=null) {
      serial=bus.getSerial();
      app=bus.getAppName();
    }
    if (debug) System.out.println("-->IvyClient["+clientKey+","+serial+"] "+app+" (remote "+appName+")<-- "+s);
  }

  private void traceDebug(String[] tab){
    StringBuffer s = new StringBuffer(" string array ");
    s.append(tab.length);
    s.append(" elements: ");
    for (String ss: tab) {
      s.append("(");
      s.append(ss);
      s.append(") ");
    }
    traceDebug(s.toString());
  }

  void PCHadd(int serial,PingCallback pc) {
    synchronized (PingCallbacksTable) {
      PingCallbacksTable.put(serial,new PingCallbackHolder(pc));
      if (PingCallbacksTable.size()>MAXPONGCALLBACKS) {
	// more than MAXPONGCALLBACKS callbacks, we ought to limit to prevent a
	// memory leak
	// TODO remove the first
	Integer smallest=PingCallbacksTable.firstKey();
	PingCallbackHolder pch = PingCallbacksTable.remove(smallest);
	System.err.println("no response from "+getApplicationName()+" to ping "+smallest+" after "+pch.age()+" ms, discarding");
      }
    }
  }

  void PCHget(Integer serial) {
    synchronized (PingCallbacksTable) {
      PingCallbackHolder pc = (PingCallbackHolder)PingCallbacksTable.remove(serial);
      if (pc==null) {
	System.err.println("warning: pong received for a long lost callback");
	return;
      }
      pc.run();
    }
  }

  private class PingCallbackHolder {
    PingCallback pc;
    long epoch;
    int age() { return (int)(System.currentTimeMillis()-epoch); }
    PingCallbackHolder(PingCallback pc) {
      this.pc=pc;
      epoch=System.currentTimeMillis();
    }
    void run() {
      pc.pongReceived(IvyClient.this,age());
    }
  }

  public static void main(String[] args) {
    String s="hello\nworld";
    String dest=encode(s);
    System.out.println("avant: <"+s+">\naprès: <"+dest+">");
    System.out.println("tailles: "+s.length()+" "+dest.length());
  }

}
