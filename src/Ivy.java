/**
 * The Ivy software bus main class
 *
 * @author Yannick Jestin <a
 * href="mailto:yannick.jestin@enac.fr">yannick.jestin&enac.fr</a>
 * @author <a href="http://www2.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (c) CENA 1998-2006
 * (c) ENAC 2006-2021
 * (c) DTI 2021-
 *
 *<pre>
 *Ivy bus = new Ivy("Dummy agent", "ready", null);
 *bus.bindMsg("(.*)", myMessageListener);
 *bus.start(null);
 *</pre>
 *
 *  CHANGELOG:
 *  1.2.18
 *    - patch G.Alligier, it all passes lotsa tests !
 *    - disable ipv6 (because you know ...)
 *  1.2.16
 *    - uses a ThreadPoolExecutor
 *    - sendMsg goes synchronized
 *    - API break: getIvyClients now returns a Collection, instead of a Vector
 *    - fixes a concurent exception in the stop() method (no more
 *    removeClient , triggered in the SendNow test)
 *    - now uses the synchronized wrappers of the Java API for all collections
 *  1.2.15
 *    - allows the fine tuning of the IvyClient socket buffersize using
 *    IVY_BUFFERSIZE property
 *  1.2.14
 *    - added a lock mechanism to be sure that once a connexion has been
 *    initiated, the ready message will be sent before stopping the bus
 *    now: Ivy b = new Ivy(...); b.sendMsg("coucou"); b.stop(); should
 *    send messages (at least the ready message) if there is a connexion
 *    attempt made before b.stop() is effective. To be sure, there is a 200ms
 *    delay before b.stop() can be effective (the Threads stopped, the sockets
 *    closed)
 *    - reintroduced a mechanism to allow the drop of a double connexion
 *    attempt
 *    - removed protected methods from javadoc
 *    - switch to apache fop + docbook for documentation
 *    - added sypport to the Swing Dispatch Thread in the bindAsyncMsg api
 *    (this breaks the former API, this is BAAAAAD). Use BindType.SWING as the
 *    latter argument
 *    - javadoc updated
 *    - appName gone private, with a protected accessor
 *    - add a lockApp synchronization for application socket control
 *    - use of stringbuffers to concatenate strings, instead of using +, which
 *    could lead to a quadractic cost in the number of iteraction (the growing
 *    string was recopied in each iteration)
 *    - throws RuntimeException instead of System.exit(), allows code reuse
 *    - ready message is set to appName + " READY" if null has been provided
 *    - switch from gnu regexp (deprecated) to the built in java regexp
 *    - when possible, move the regexp Pattern.compile in static areas, to avoid multiple
 *    calls
 *    - add generic types to declarations
 *    - fxed a potential null pointer dereference on quit
 *    - lowercase CheckRegexp to checkRegexp (bad practice, thanks to FindBugs)
 *    - recopy the filter String[] in setfilter, to avoid exposing internal
 *      representation (unsafe operation)
 *  1.2.13:
 *    - adds support for RESyntaxException
 *  1.2.12:
 *    - directMessage goes protected
 *  1.2.9:
 *    - introducing setFilter()
 *    - introducing IVYRANGE in to allow the bus service socket to start on a
 *    specific port range ( think of firewalls ), using java -DIVYRANGE=4000-5000 e.g.
 *  1.2.8:
 *    - addclient and removeclient going synchronized
 *    - domainaddr goes protected in Domain ( gij compatibility )
 *    - checks if (Client)e.nextElement() each time we want to ...
 *    Multithreaded Enumerations ..., should fix [YJnul05]
 *    - added getDomainArgs(String, String[]) as a facility to parse the
 *    command line in search of a -b domain
 *    - added getWBUId(), un function returning a string ID to perform
 *    queries, computed strings look like IDTest0:1105029280616:1005891134
 *    - empties the watchers vector after a stop(), and handles the "stopped"
 *    better, FIXES FJ's bugreport stop/start
 *  1.2.7:
 *    - minor fixes for accessing static final values
 *  1.2.6:
 *    - added serial numbers for traceDebug
 *    - changed the semantic of -b a, b:port,c:otherport if no port is
 *      specified for a, it take the port from the next one. If none is
 *      specified, it takes DEFAULT_PORT
 *    - no more asynchronous sending of message ( async bind is ok though )
 *      because the tests are sooooo unsuccessful
 *    - use addElement/removeElement instead of add/remove is registering
 *      threads ( jdk1.1 backward compatibility )
 *  1.2.5:
 *    - protection of newlines
 *  1.2.4:
 *    - added an accessor for doSendToSelf
 *    - waitForMsg() and waitForClient() to make the synchronization with
 *      other Ivy agents easier
 *    - with the bindAsyncMsg() to subscribe and perform each callback in a
 *      new Thread
 *    - bindMsg(regexp,messagelistener,boolean) allow to subscribe with a
 *      synchrone/asynch exectution
 *    - API change, IvyException raised when \n or \0x3 are present in bus.sendMsg()
 *    - bindListener are now handled
 *    - removeApplicationListener can throw IvyException
 *    - bus.start(null) now starts on getDomain(null), first the IVYBUS
 *    property, then the DEFAULT_DOMAIN, 127:2010
 *    - bindMsg() now throws an IvyException if the regexp is invalid !!!
 *    BEWARE, this can impact lots of programs ! (fixes J007)
 *    - no more includes the "broadcasting on " in the domain(String) method
 *    - new function sendToSelf(boolean) allow us to send messages to
 *    ourselves
 *  1.2.3:
 *    - adds a IVYBUS property to propagate the domain once set. This way,
 *    children forked through Ivy java can inherit from the current value.
 *    - adds synchronized flags to allow early disconnexion
 *  1.2.2:
 *    added the String domains(String d) function, in order to display the
 *  domain list
 *  1.2.1:
 *    bus.start(null) now starts on DEFAULT_DOMAIN. ( correction 1.2.4 This was not true.)
 *    added the getDomains in order to correctly display the domain list
 *    checks if the serverThread exists before interrupting it
 *    no has unBindMsg(String)
 *  1.2.0:
 *    setSoTimeout is back on the server socket
 *    added a regression test main()
 *    clients is now a Hashtable. the deletion now works better
 *    getIvyClientsByName allows the research of IvyClient by name
 *    getDomain doesnt throw IvyException anymore
 *    removed the close() disconnect(IvyClient c). Fixes a big badaboum bug
 *    getDomain becomes public
 *    adding the sendToSelf feature
 *    fixed the printStackTrace upon closing of the ServerSocket after a close()
 */
package fr.dgac.ivy;

// import java.net.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.BindException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.util.Vector;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class Ivy implements Runnable {

  /**
   * the default size of the IvyClients' socket buffer.
   * defaults to 4096, can be adjusted through the use of IVY_BUFFERSIZE JVM
   * property
   */
 
  private static final int PREFFERREDBUFFERSIZE = 4096; // in bytes

  /**
   * the library version, useful for development purposes only, when java is
   * invoked with -DIVY_DEBUG
   */
  private  static final String LIBVERSION = "1.2.18";

  /*
   * private fields
   */
  private static final int GRACEDELAY = 200; // in milliseconds, the time to wait if we want to join a bus, send a message, and quit
  static final int TIMEOUTLENGTH = 1000;


  private String appName;
  private int applicationPort; /* Application port number */
  private String ready_message = null;
  private boolean doProtectNewlines = false;
  private int bufferSize = PREFFERREDBUFFERSIZE;
  private SelfIvyClient selfIvyClient;
  private Object lockApp = new Object();
  private boolean debug;
  private ServerSocket app;
  private Collection<IvyWatcher> watchers = new ArrayList<IvyWatcher>();
  private volatile boolean keeprunning = false;
  private Thread serverThread = null;

  private Map<Integer, IvyClient> clients = Collections.synchronizedMap(new HashMap<Integer, IvyClient>());
  private Map<Integer, IvyClient> half = Collections.synchronizedMap(new HashMap<Integer, IvyClient>());

  private Vector<IvyApplicationListener> ivyApplicationListenerList = new Vector<IvyApplicationListener>();
  private Vector<IvyBindListener> ivyBindListenerList = new Vector<IvyBindListener>();
  private Vector<Thread> sendThreads = new Vector<Thread>();
  private String[] filter = null;
  private boolean stopped = true;
  // private boolean starting = false;
  private volatile int nbThreads = 0;
  private Object readyToSend = new Object();
  private boolean doSendToSelf = false;
  private ExecutorService pool = null;

  // FIXME should not be static ? (findbugs)
  private static int serial = 0;
  private int myserial = serial++;
  private static long current = System.currentTimeMillis();
  private static java.util.Random generator = new java.util.Random(current * (serial + 1));
  private String watcherId = null;
  private static Pattern rangeRE; // tcp range min and max
  private static Pattern bounded;

  private static final Object lock = new Object();

  /**
   * Readies the structures for the software bus connexion.
   * The typical use of the constructor is the following
   * <br><code>Ivy bus = new Ivy("AgentName", "AgentName ready", null);</code>
   * <br>All the dirty work is done un the start() method
   *
   * @see #start
   * @param name The name of your Ivy agent on the software bus
   * @param message The hellow message you will send once ready. It can be
   * null, in which case "appname READY" will be the default
   * @param appcb A callback handling the notification of connexions and
   * disconnections, (may be null for most agents)
   */
  public Ivy(final String name, final String message, final IvyApplicationListener appcb) {
    appName = name;
    ready_message = (message == null) ? name + " READY" : message;
    debug =
      (System.getProperty("IVY_DEBUG") != null)
      || (System.getProperty("IVY_DEBUG") != null);
    if (System.getProperty("IVY_BUFFERSIZE") != null) {
	bufferSize = Integer.parseInt(System.getProperty("IVY_BUFFERSIZE"));
    }
    if ( appcb != null ) {
      ivyApplicationListenerList.addElement( appcb );
    }
    selfIvyClient = new SelfIvyClient(this , name);
  }

  /**
   * Waits for a message to be received.
   *
   * @since 1.2.4
   * @param regexp the message we're waiting for to continue the main thread.
   * @param timeout in millisecond, 0 if infinite
   * @throws IvyException if something bad happens
   * @return the IvyClient who sent the message, or null if the timeout is
   * reached
   */
  public final IvyClient waitForMsg(final String regexp , final int timeout) throws IvyException {
    Waiter w  = new Waiter(timeout);
    int re = bindMsg(regexp , w);
    IvyClient ic = w.waitFor();
    unBindMsg(re);
    return ic;
  }

  /**
   * Waits for an other IvyClient to join the bus.
   *
   * @since 1.2.4
   * @param name the name of the client we're waiting for to continue the main thread.
   * @param timeout in millisecond, 0 if infinite
   * @throws IvyException if something bad happens
   * @return the first IvyClient with the name or null if the timeout is
   * reached
   */
  public final IvyClient waitForClient(final String name , final int timeout) throws IvyException {
    IvyClient ic;
    if (name == null) {
      throw new IvyException("null name given to waitForClient");
    }
    // first check if client with the same name is on the bus
    ic = alreadyThere(clients , name);
    if (ic != null) return ic;
    // if not enter the waiting loop
    WaiterClient w  = new WaiterClient(name , timeout , clients);
    int i = addApplicationListener(w);
    ic = w.waitFor();
    removeApplicationListener(i);
    return ic;
  }

  /*
   * since 1.2.8
   */
  protected static IvyClient alreadyThere(final Map<Integer , IvyClient> c , final String name) {
    synchronized (c) {
      for (IvyClient ic : c.values()) {
	  if ((ic != null)&&(name.compareTo(ic.getApplicationName()) == 0)) return ic;
      }
    }
    return null;
  }

  /**
   * returns the domain bus.
   * @deprecated if needed, use bus.start(null), and it will be called
   * automatically
   *
   * @param domainbus if non null, returns the argument
   * @return It returns domainbus, if non null,
   * otherwise it returns the IVYBUS property if non null, otherwise it
   * returns Ivy.DEFAULT_DOMAIN
   */
  @Deprecated public static final String getDomain(final String domainbus) {
    return Domain.getDomain(domainbus);
  }

  /**
   * returns the domain bus.
   * @deprecated if needed, use Domain.getDomainArgs
   *
   * @since 1.2.8
   * @param progname The name of your program, for error message
   * @param args the String[] of arguments passed to your main()
   * @return returns the domain bus, ascending priority : ivy default bus, IVY_BUS
   * property, -b domain on the command line
   */
  @Deprecated public static final String getDomainArgs(final String progname, final String[] args) {
    return Domain.getDomainArgs(progname, args);
  }

  /**
   * connects the Ivy bus to a domain or list of domains.
   *
   * <ul>
   * <li>One thread (IvyWatcher) for each traffic rendezvous (either UDP broadcast or TCPMulticast)
   * <li>One thread (serverThread/Ivy) to accept incoming connexions on server socket
   * <li>a thread for each IvyClient when the connexion has been done
   * </ul>
   * @throws IvyException if there is a problem joining the bus
   * @param domainbus a domain of the form 10.0.0:1234, A good practice is to
   * sick to a null value, so that your agent will honor the IVY_BUS parameter
   * given to the jvm (java -DIVYBUS= ... . Otherwise, you can provide some
   * hard-coded value, similar to the
   * netmask without the trailing .255. This will determine the meeting point
   * of the different applications. Right now, this is done with an UDP
   * broadcast. Beware of routing problems ! You can also use a comma
   * separated list of domains.
   *
   * 1.2.8: goes synchronized. I don't know if it's really useful
   *
   */

  public final void start(final String domainbus) throws IvyException {
    if (!stopped) throw new IvyException("cannot start a bus that's already started");
    System.setProperty("java.net.preferIPv4Stack" , "true");
    pool = Executors.newCachedThreadPool();
    stopped = false;
    String db = domainbus;
    if (db == null) db = Domain.getDomain(null);
    Properties sysProp = System.getProperties();
    sysProp.put("IVYBUS" , db);
    String range = (String)sysProp.get("IVYRANGE");
    Matcher match;
    if ((range != null)&&(match = rangeRE.matcher(range)).matches()) {
      int rangeMin = Integer.parseInt(match.group(1));
      int rangeMax = Integer.parseInt(match.group(2));
      int index = rangeMin;
      traceDebug("trying to allocate a TCP port between " + rangeMin + " and " + rangeMax);
      boolean allocated = false;
      while (!allocated) try {
        if (index>rangeMax) throw new IvyException("no available port in IVYRANGE"  +  range );
        synchronized (lockApp) {
          app = new ServerSocket(index);
          app.setSoTimeout(TIMEOUTLENGTH);
          applicationPort = app.getLocalPort();
        }
        allocated = true;
      } catch (BindException e) {
        index++;
      } catch (IOException e) {
        throw new IvyException("can't open TCP service socket " + e );
      }
    }
    else try {
      synchronized (lockApp) {
        app = new ServerSocket(0);
        app.setSoTimeout(TIMEOUTLENGTH);
        applicationPort = app.getLocalPort();
      }
    } catch (IOException e) {
      throw new IvyException("can't open TCP service socket " + e );
    }
    traceDebug("lib: " + LIBVERSION + " protocol: " + Protocol.PROTOCOLVERSION + " TCP service open on port " + applicationPort);

    List<Domain> d = Domain.parseDomains(db);
    if (d.size() == 0) throw new IvyException("no domain found in " + db);
    watcherId = getWBUId().replace(' ' , '*'); // no space in the watcherId
    // readies the rendezvous : an IvyWatcher (thread) per domain bus
    for (Domain dom: d) watchers.add(new IvyWatcher(this , dom.getDomainaddr() , dom.getPort()));
    keeprunning = true;
    pool.execute(this);

    // sends the broadcasts and listen to incoming connexions
    for (IvyWatcher iw: watchers) iw.doStart();
  }

  private void waitForRemote(String s) {
    try {
      while (nbThreads > 0) {
	  traceDebug("I'm waiting before "+s+", a connecting tread is in progress");
	  Thread.sleep(GRACEDELAY);
	  traceDebug("I'm done waiting before "+s);
      }
    } catch (InterruptedException ie) {
      // should not happen, and it's not a problem anyway
    }
  }

  /**
   * disconnects from the Ivy bus.
   */
  public final void stop() {
    waitForRemote("stopping");
    if (stopped) return;
    stopped = true;
    keeprunning = false;
    traceDebug("beginning stopping");
    try {
      synchronized (lockApp) { app.close(); }

      // stopping the IvyWatchers
      for (IvyWatcher iw: watchers) iw.doStop();
      watchers.clear();
      // stopping the remaining IvyClients
      synchronized (clients) {
	  for (IvyClient c : clients.values())
	      if (c != null)
		  c.close(true);
      }
    } catch (IOException e) {
     traceDebug("IOexception Stop ");
    }
    pool.shutdown();
    traceDebug("end stopping");
  }

  /**
   * Toggles the sending of messages to oneself, the remote client's
   * IvyMessageListeners are processed first, and ourself afterwards.
   * @param b true if you want to send the message to yourself. Default
   * is false
   * @since 1.2.4
   */
  public final void sendToSelf(final boolean b) {
    doSendToSelf = b;
  }

  /**
   * do I send messsages to myself ?
   * @return a boolean
   * @since 1.2.4
   */
  public final boolean isSendToSelf() {
    return doSendToSelf;
  }

  /**
   * selfIvyClient accesssor.
   * @return our selfIvyClient
   * @since 1.2.4
   * @since 1.2.4
   */
  public final SelfIvyClient getSelfIvyClient() {
    return selfIvyClient;
  }

  /**
   * Toggles the encoding/decoding of messages to prevent bugs related to the
   * presence of a "\n".
   * @param b true if you want to enforce encoding of newlines. Default
   * is false. Every receiver will have to decode newlines
   * @since 1.2.5
   * The default escape character is a ESC 0x1A
   */
  public final void protectNewlines(final boolean b) {
    doProtectNewlines = b;
  }

  /**
   * Performs a pattern matching according to everyone's regexps, and sends
   * the results to the relevant ivy agents.
   * @throws IvyException if there is a problem sending the message
   * @param message A String which will be compared to the regular
   * expressions of the different clients
   * @return returns the number of messages actually sent
   *
   * since 1.2.16 goes synchronized to avoid concurrent access
   */
  synchronized public final int sendMsg(final String message) throws IvyException {
    int count = 0;
    waitForRemote("sending");
    synchronized (lock) {
      traceDebug("sending "+message);
      String msg = message;
      if (doProtectNewlines) msg = IvyClient.encode(message);
      else if ( (msg.indexOf(Protocol.NEWLINE) != -1)||(msg.indexOf(Protocol.ENDARG) != -1))
	throw new IvyException("newline character not allowed in Ivy messages");
      synchronized (clients)  {
	for ( IvyClient client : clients.values()) if (client != null) count += client.sendMsg(msg);
      }
      if (doSendToSelf) count += selfIvyClient.sendSelfMsg(msg);
      traceDebug("end sending "+message+" to "+count+" clients");
    }
    return count;
  }

  /**
   * Subscribes to a regular expression.
   *
   * The callback will be executed with
   * the saved parameters of the regexp as arguments when a message will sent
   * by another agent. A program <em>doesn't</em> receive its own messages.
   * <p>Example:
   * <br>the Ivy agent A performs <pre>b.bindMsg("^Hello (*)",cb);</pre>
   * <br>the Ivy agent B performs <pre>b2.sendMsg("Hello world");</pre>
   * <br>a thread in A will uun the callback cb with its second argument set
   * to a array of String, with one single element, "world"
   * @param sregexp a perl regular expression, groups are done with parenthesis
   * @param callback any objects implementing the IvyMessageListener
   * interface, on the AWT/Swing framework
   * @throws IvyException if there is a problem in the binding, be it regexp
   * or network
   * @return the id of the regular expression
   */
  public final int bindMsg(final String sregexp , final IvyMessageListener callback ) throws IvyException  {
    return bindMsg(sregexp , callback , BindType.NORMAL);
  }
    
  /**
   * Subscribes to a regular expression with asyncrhonous callback execution.
   *
   * Same as bindMsg, except that the callback will be executed in a separate
   * thread each time.
   * WARNING : there is no way to predict the order of execution
   * of the * callbacks, i.e. a message received might trigger a callback before
   * another one sent before
   *
   * @since 1.2.4
   * @param sregexp a perl compatible regular expression, groups are done with parenthesis
   * @param callback any objects implementing the IvyMessageListener
   * interface, on the AWT/Swing framework
   * @param type if set to NORMAL, it's a normal bind, if it's ASYNC, the
   * callback will be created in a newly spawned Thread (Heavy ressources), if
   * it's SWING, the callback will be deferred to the Swing Event Dispatch
   * Tread
   * @throws IvyException if there is a problem binding (network, regexp...)
   * @return the int ID of the regular expression.
   */
  public final int bindAsyncMsg(final String sregexp, final IvyMessageListener callback, BindType type ) throws IvyException  {
    return bindMsg(sregexp , callback , type);
  }

  /**
   * Subscribes to a regular expression.
   *
   * The callback will be executed with
   * the saved parameters of the regexp as arguments when a message will sent
   * by another agent. A program <em>doesn't</em> receive its own messages,
   * except if sendToSelf() is set to true.
   * <p>Example:
   * <br>the Ivy agent A performs <pre>b.bindMsg("^Hello (*)",cb);</pre>
   * <br>the Ivy agent B performs <pre>b2.sendMsg("Hello world");</pre>
   * <br>a thread in A will uun the callback cb with its second argument set
   * to a array of String, with one single element, "world"
   * @since 1.2.4
   * @param sregexp a perl regular expression, groups are done with parenthesis
   * @param callback any objects implementing the IvyMessageListener
   * interface, on the AWT/Swing framework
   * @param type  if NORMAL (default) it's a normal bind, if ASYNC, each callback will be run in a separate thread, if SWING, the callback will be deferred to the Swing Event Dispatch Thread
   * default is NORMAL
   * @throws IvyException if there is a problem binding (regexp, network)
   * @return the id of the regular expression
   */
  public final int bindMsg(final String sregexp , final IvyMessageListener callback , final BindType type ) throws IvyException {
    // adds the regexp to our collection in selfIvyClient
    int key = selfIvyClient.bindMsg(sregexp , callback , type);
    // notifies the other clients this new regexp
    synchronized (clients) {
      for (IvyClient c : clients.values() ) if (c != null) c.sendRegexp(key , sregexp);
    }
    return key;
  }

  /**
   * Subscribes to a regular expression for one time only, useful for
   * requests, in cunjunction with getWBUId().
   *
   * The callback will be executed once and only once, and the agent will
   * unsubscribe
   * @since 1.2.8
   * @param sregexp a perl regular expression, groups are done with parenthesis
   * @param callback any objects implementing the IvyMessageListener
   * interface, on the AWT/Swing framework
   * @throws IvyException if there is a problem during the binding
   * @return the id of the regular expression
   */
  public final int bindMsgOnce(final String sregexp, final IvyMessageListener callback ) throws IvyException  {
    Once once = new Once(callback);
    int id = bindMsg(sregexp , once);
    once.setRegexpId(id);
    return id;
  }

  /**
   * unsubscribes a regular expression using the id provided at bind time.
   *
   * @param id the id of the regular expression, returned when it was bound
   * @throws IvyException if the id is not valid anymore
   */
  public final void unBindMsg(final int id) throws IvyException {
    selfIvyClient.unBindMsg(id);
    synchronized (clients) {
      for (IvyClient ic : clients.values() ) if (ic != null) ic.delRegexp(id );
    }
  }

  /**
   * unsubscribes a regular expression based on its string.
   *
   * @return a boolean, true if the regexp existed, false otherwise or
   * whenever an exception occured during unbinding
   * @param re the string for the regular expression
   */
  public final boolean unBindMsg(final String re) { return selfIvyClient.unBindMsg(re); }

  /**
   * adds a bind listener to a bus.
   * @param callback is an object implementing the IvyBindListener interface
   * @return the id of the bind listener, useful if you wish to remove it later
   * @since 1.2.4
   */
  public final  int addBindListener(final IvyBindListener callback){
    ivyBindListenerList.addElement(callback);
    return ivyBindListenerList.indexOf(callback);
  }

  /**
   * removes a bind listener.
   * @param id the id of the bind listener to remove
   * @throws IvyException if id is not known
   * @since 1.2.4
   */
  public final void removeBindListener(final int id) throws IvyException {
    try {
      ivyBindListenerList.removeElementAt(id);
    } catch (ArrayIndexOutOfBoundsException aie) {
      throw new IvyException(id + " is not a valid Id");
    }
  }

  /**
   * adds an application listener to a bus.
   * @param callback is an object implementing the IvyApplicationListener
   * interface
   * @return the id of the application listener, useful if you wish to remove
   * it later
   */
  public synchronized final int addApplicationListener(final IvyApplicationListener callback){
    ivyApplicationListenerList.addElement(callback);
    return ivyApplicationListenerList.indexOf( callback );
  }

  /**
   * removes an application listener.
   * @param id the id of the application listener to remove
   * @throws IvyException if there is no such id
   */
  public synchronized final void removeApplicationListener(final int id) throws IvyException {
    try {
      ivyApplicationListenerList.removeElementAt(id);
    } catch (ArrayIndexOutOfBoundsException aie) {
      throw new IvyException(id + " is not a valid Id");
    }
  }

  /**
   * sets the filter expression.
   * @param f the extensive list of strings beginning the messages
   * @since 1.2.9
   * @throws IvyException if a filter is already set or the bus is already
   * started
   *
   * once this filter is set, when a client subscribes to a regexp of the
   * form "^dummystring...", there is a check against the filter list. If no
   * keyword is found to match, the binding is just ignored.
   */
  public final synchronized void setFilter(final String[] f) throws IvyException {
    if (filter != null) throw new IvyException("only one filter can be set");
    if (!stopped) throw new IvyException("cannot set a filter on a bus that's already started");
    filter = java.util.Arrays.copyOf(f , f.length);
  }

  static {
    // compiles the static regexps
    try {
      rangeRE = Pattern.compile("(\\d+)-(\\d+)"); // tcp range min and max
      bounded = Pattern.compile("^\\^([a-zA-Z0-9_-]+).*");
    } catch ( PatternSyntaxException res ) {
      res.printStackTrace();
      System.out.println("Regular Expression bug in Ivy source code ... bailing out");
    }
  }


  /**
   * checks the "validity" of a regular expression if a filter has been set.
   * @since 1.2.9
   * @param exp a string regular expression
   *  TODO must it be synchronized ( RE was not threadsafe, java regexp is )
   */
  public final boolean checkRegexp(final String exp) {
    if (filter == null) return true; // there's no message filter
    Matcher m = bounded.matcher(exp);
    if (!m.matches()) return true; // the regexp is not bounded
    //System.out.println("the regexp is bounded, "+bounded.getParen(1));
    // else the regexp is bounded. The matching string *must* be in the filter
    String prems = m.group(1);
    for (String f: filter) if (f.compareTo(prems) == 0) return true;
    // traceDebug(" classFilter ["+filter[i]+"] vs regexp ["+prems+"]");
    return false;
  }

  // a private class used by bindMsgOnce, to ensure that a callback will be
  // executed once, and only once
  private class Once implements IvyMessageListener {
    private boolean received = false;
    private int id = -1;
    private IvyMessageListener ocallback = null;
    Once(final IvyMessageListener callback){ ocallback = callback; }
    synchronized void setRegexpId(final int fid){ id = fid; }
    public void receive(final IvyClient ic , final String[] args){
      synchronized(Once.this) {
        // synchronized because it will most likely be called
        // concurrently, and I *do* want to ensure that it won't
        // execute twice
        if (received||(ocallback == null)||(id == -1)) return;
        received = true;
        try { Ivy.this.unBindMsg(id); } catch (IvyException ie) { ie.printStackTrace(); }
        ocallback.receive(ic , args);
      }
    }
  }

  /* invokes the application listeners upon arrival of a new Ivy client */
  protected synchronized final void clientConnects(final IvyClient client){
    for (IvyApplicationListener ial : ivyApplicationListenerList) ial.connect(client);
  }

  /* invokes the application listeners upon the departure of an Ivy client */
  protected synchronized final void clientDisconnects(final IvyClient client){
    for (IvyApplicationListener ial : ivyApplicationListenerList) ial.disconnect(client);
  }

  /* invokes the bind listeners */
  protected final void regexpReceived(final IvyClient client , final int id , final String sregexp){
    for (IvyBindListener ibl : ivyBindListenerList) ibl.bindPerformed(client , id , sregexp);
  }

  /* invokes the bind listeners */
  protected final void regexpDeleted(final IvyClient client , final int id , final String sregexp){
    for (IvyBindListener ibl : ivyBindListenerList) ibl.unbindPerformed(client , id , sregexp);
  }

  /*
   * invokes the application listeners when we are summoned to die
   * then stops
   */
  protected synchronized final void dieReceived(final IvyClient client , final int id , final String message){
    for (IvyApplicationListener ial : ivyApplicationListenerList) ial.die(client , id , message);
  }

  /* invokes the direct message callbacks */
  protected synchronized final void directMessage(final IvyClient client , final int id , final String msgarg ){
    for (IvyApplicationListener ial : ivyApplicationListenerList) ial.directMessage(client , id, msgarg);
  }

  /**
   * gives a list of IvyClient at a given instant.
   * @return a collection of IvyClients
   */
  public final Collection<IvyClient> getIvyClients() {
    Collection<IvyClient> v = new ArrayList<IvyClient>();
    synchronized (clients) {
      for (IvyClient ic : clients.values() ) if (ic != null) v.add(ic);
    }
    return v;
  }

  /**
   * gives a list of IvyClient with the name given in parameter.
   *
   * @param name The name of the Ivy agent you're looking for
   * @return a vector of IvyClients
   */
  public final Collection<IvyClient> getIvyClientsByName(final String name) {
    Collection<IvyClient> v = new ArrayList<IvyClient>();
    String icname;
    synchronized (clients) {
      for (IvyClient ic :  clients.values() ) {
	if ( (ic == null)||((icname = ic.getApplicationName()) == null) ) break;
	if (icname.compareTo(name) == 0) v.add(ic);
      }
    }
    return v;
  }

  /**
   * returns a "wana be unique" ID to make requests on the bus.
   *
   * @since 1.2.8
   * @return returns a string wich is meant to be noisy enough to be unique
   */
  public final String getWBUId() {
    return "ID<" + appName + myserial + ":" + nextId() + ":" + generator.nextInt() + ">";
  }


  @Override public String toString() {
    return "bus <"+appName+">[port:"+applicationPort+",serial:"+myserial+"]";
  }

  private synchronized long nextId() { return current++; }

  /////////////////////////////////////////////////////////////////:
  //
  // Protected methods
  //
  /////////////////////////////////////////////////////////////////:

  /**
   * @return false if the client has not been created, true otherwise
   */
  protected boolean createIvyClient(Socket s , int port, boolean domachin) throws IOException {
    IvyClient i = new IvyClient(this , s , port , domachin);
    try {
      pool.execute(i);
    } catch (RejectedExecutionException ree) {
      // in another thread, the pool is shut down
      traceDebug("in another thread, the pool is shut down");
      return false;
    }
    return true;
  }


  protected synchronized void removeClient(IvyClient c) {
    synchronized(lock) {
      synchronized (clients) {
	clients.remove(c.getClientKey());
      }
      traceDebug("removed " + c + " from clients: " + getClientNames(clients));
    }
  }

  protected synchronized void handShake(IvyClient c) {
    synchronized(lock) {
      removeHalf(c);
      if (clients == null||c == null) return;
      // TODO check if it's not already here !
      IvyClient peer = searchPeer(c);
      if ((peer == null) || peer.distanceTo(c)>0 ){
	synchronized (clients) {
	  clients.put(c.getClientKey() , c);
	}
	traceDebug("added " + c + " in clients: " + getClientNames(clients));
      } else {
	traceDebug("not adding "+c+" in clients, double connexion detected, removing lowest one");
	try {
	  c.close(false);
	} catch (IOException ioe) {
	  // TODO
	}
      }
    }
  }

  protected synchronized void addHalf(IvyClient c) {
    synchronized (half) { half.put(c.getClientKey() , c); }
    traceDebug("added " + c + " in half: " + getClientNames(half));
  }

  protected synchronized void removeHalf(IvyClient c) {
    if (half == null||c == null) return;
    synchronized (half) {
      half.remove(c.getClientKey());
    }
    traceDebug("removed " + c + " from half: " + getClientNames(half));
  }

  private synchronized IvyClient searchPeer(IvyClient ic) {
    synchronized (clients) {
      for (IvyClient peer : clients.values()) if ((peer != null)&&(peer.myEquals(ic))) return peer;
    }
    return null;
  }

  /*
   * the service socket thread reader main loop
   */
  public void run() {
    traceDebug("service thread started"); // THREADDEBUG
    serverThread = Thread.currentThread();
    serverThread.setName("Ivy TCP server Thread");
    //serverThread.setDaemon(true);
    Socket socket = null;
    while ( keeprunning ){
      try {
        synchronized (this) {
	  //System.out.println("DEBUG stopped: "+stopped);
	  if ( (!keeprunning) || stopped ) break; // early disconnexion
	}
	synchronized (lockApp) {
	  socket = app.accept(); // TODO I can't synchronize on (this) in the run
	}
	synchronized (this) {
	  if ( (!keeprunning) || stopped ) break; // early disconnexion
	  // the peer called me
	  if ( ! createIvyClient(socket , 0 , true) ) break;
        }
      } catch (InterruptedIOException ie) {
        // traceDebug("server socket was interrupted. good");
        if ( !keeprunning ) break;
      } catch( IOException e ) {
        if ( keeprunning ) {
          traceDebug("Error IvyServer exception:  "  +  e.getMessage());
          System.out.println("Ivy server socket reader caught an exception " + e.getMessage());
          System.out.println("this is probably a bug in your JVM ! (e.g. blackdown jdk1.1.8 linux)");
          throw new RuntimeException();
        } else {
          traceDebug("my server socket has been closed");
        }
      }
    }
    traceDebug("service thread stopped"); // THREADDEBUG
  }

  String getAppName() { return appName; }
  int getAppPort() { return applicationPort; }
  String getReadyMessage() { return ready_message; }
  boolean getProtectNewlines() { return doProtectNewlines; }
  String getWatcherId() { return watcherId; }
  int getBufferSize() { return bufferSize; }
  int getSerial() { return myserial; }
  ExecutorService getPool() { return pool; }

  protected void pushThread(String reason) {
    synchronized(readyToSend) {
      // nbThreads++;
      //System.out.println("DEBUG PUSH "+this+" -- threads: "+nbThreads + "; reason: "+reason);
    }
  }

  protected void popThread(String reason) {
    synchronized(readyToSend) {
      // nbThreads--; 
      //System.out.println("DEBUG POP "+this+" -- threads: "+nbThreads + "reason: "+reason);
    }
  }

  private void traceDebug(String s){
    if (debug) System.out.println("-->Ivy[" + myserial + "]<-- " + s);
  }

  // a small private method for debbugging purposes
  private String getClientNames(Map<Integer , IvyClient> t) {
    StringBuffer s = new StringBuffer();
    s.append("(");
    synchronized (t) {
      for (IvyClient ic : t.values() ) if (ic != null) s.append(ic.getApplicationName() + ",");
    }
    s.append(")");
    return s.toString();
  }

}
