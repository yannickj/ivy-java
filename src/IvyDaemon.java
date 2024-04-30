/**
 * IvyDaemon: simple TCP to Ivy relay, can be useful if a shell command wants
 * to send a message on the bus.
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * This is a sample implementation of an Ivy Daemon, like ivyd
 * sends anonymous messages to an Ivy bus through a simple tcp socket,
 * line by line. The default port is 3456.
 *
 * (c) CENA
 *
 * changelog:
 *   1.2.16
 *    - now uses a Thread Pool Executor
 *    - now parses the messages: if the message is ".die IvyDaemon", quits the
 *    bus
 *   1.2.14
 *    - remove the Thread.start() from the constructor, to avoid mulithread issues
 *  	see *  	http://findbugs.sourceforge.net/bugDescriptions.html#SC_START_IN_CTOR
 *  	now ,we have to call IvyClient.start() after it has been created
 *    - gracefully quits when message is received, by quitting the Ivy
 *   1.2.8
 *    - goes into tools subpackage
 *   1.2.3
 *    - adds the traceDebug
 *    - uses the daemonThread paradigm to programm the thread sync
 *    - invalid port number as a command line argument now stops the program
 *    - cleans up the code
 *    - adds a "quiet" option on the command line
 *   1.2.2
 *    - changes the setProperty to a backward compatible construct
 *   1.0.12
 *    - class goes public access !
 */
package fr.dgac.ivy.tools ;
import fr.dgac.ivy.* ;
import java.io.*;
import java.net.*;
import java.util.Properties ;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import gnu.getopt.Getopt;

public class IvyDaemon implements Runnable, IvyApplicationListener  {

  private ServerSocket serviceSocket;
  private static boolean debug = (System.getProperty("IVY_DEBUG")!=null) ;
  private Thread daemonThread;// volatile to ensure the quick communication
  private volatile boolean keeprunning = false ;// volatile to ensure the quick communication
  private Ivy bus;
  private ExecutorService pool = null;
  private static volatile int serial = 0;

  public static final int DEFAULT_SERVICE_PORT = 3456 ;
  public static final String DEFAULTNAME = "IvyDaemon";
  public static final String helpmsg = "usage: java fr.dgac.ivy.tools.IvyDaemon [options]\n\t-b BUS\tspecifies the Ivy bus domain\n\t-p\tport number, default "+DEFAULT_SERVICE_PORT+"\n\t-n ivyname (default "+DEFAULTNAME+")\n\t-q\tquiet, no tty output\n\t-d\tdebug\n\t-h\thelp\nListens on the TCP port, and sends each line read on the Ivy bus. It is useful to launch one Ivy Daemon and let scripts send their message on the bus.\n";

  private static String name = DEFAULTNAME;
  public static void main(String[] args) throws IvyException, IOException {
    Ivy bus;
    Getopt opt = new Getopt("IvyDaemon",args,"n:b:dqp:h");
    int c;
    int servicePort = DEFAULT_SERVICE_PORT;
    boolean quiet = false;
    String domain=null;
    while ((c = opt.getopt()) != -1) switch (c) {
    case 'n':
      name=opt.getOptarg();
      break;
    case 'b':
      domain=opt.getOptarg();
      break;
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
	return;
      }
      break;
    case 'h':
    default:
      System.out.println(helpmsg);
      return;
    }
    bus=new Ivy(name,name+" ready",null);
    if (!quiet) System.out.println("broadcasting on "+Domain.domains(domain));
    bus.start(domain);
    if (!quiet) System.out.println("listening on "+servicePort);
    new IvyDaemon(bus,servicePort);
  }

  public IvyDaemon(Ivy bus,int servicePort) throws IOException {
    this.bus=bus;
    bus.addApplicationListener(this);
    serviceSocket = new ServerSocket(servicePort) ;
    pool = Executors.newCachedThreadPool();
    keeprunning = true ;
    pool.execute(this);
  }


  /*
   * the service socket reader. 
   * it could be a thread, but as long as we've got one ....
   */
  public void run() {
    daemonThread = Thread.currentThread();
    daemonThread.setName("Ivy Daemon tool thread");
    traceDebug("Thread started");
    while ( keeprunning ) {
      /* there is no way out of here, except ^C */
      try {
        new SubReader(serviceSocket.accept());
      } catch( IOException e ) {
	traceDebug("TCP socket reader caught an exception " + e.getMessage());
      }
    }
    System.out.println("outta here");
    traceDebug("Thread stopped");
    pool.shutdown();
  }

  class SubReader implements Runnable {
    BufferedReader in;

    SubReader(Socket socket) throws IOException {
      in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
      // setName("Subreader "+serial++);
      pool.execute(SubReader.this);
    }

    public void run() {
      traceDebug("Subreader Thread started");
      try {
	while (true) {
	  String msg=in.readLine();
	  if (msg==null) break;
	  if (msg.compareTo(".die IvyDaemon") == 0) break;
	  try {
	    bus.sendMsg(msg);
	  } catch (IvyException ie) {
	    System.out.println("incorrect characters whithin the message. Not sent");
	  }
	}
      } catch (IOException ioe) {
        traceDebug("Subreader exception ...");
	ioe.printStackTrace();
	throw new RuntimeException();
      }
      traceDebug("Subreader Thread stopped");
      try {
	in.close();
      } catch (IOException ioe) {
	// do nothing
      }
      pool.shutdown();
      bus.stop();
      System.exit(0);
    }
  }

  public void connect( IvyClient client ) { }
  public void disconnect( IvyClient client ) { }
  public void die( IvyClient client, int id, String msgarg) {
    keeprunning = false;
    if ( daemonThread != null ) daemonThread.interrupt();
    try {
      serviceSocket.close();
    } catch (IOException ioe) {
      // I don't care
    }
  }

  public void directMessage( IvyClient client, int id,String msgarg ) {}

  private static void traceDebug(String s){
    if (debug) System.out.println("-->IvyDaemon "+name+"<-- "+s);
  }

}
