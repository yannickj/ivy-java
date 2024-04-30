/**
 * ProxyClient: Ivy relay, first attempt, still in beta stage DO NOT USE
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (c) ENAC
 *
 * changelog:
 *  1.2.14
 *    - remove the Thread.start() from the constructor, to avoid mulithread issues
 *  	see *  	http://findbugs.sourceforge.net/bugDescriptions.html#SC_START_IN_CTOR
 *  	now ,we have to call IvyClient.start() after it has been created
 *    - throws RuntimeException instead of System.exit(), allows code reuse
 *    - switch from gnu regexp (deprecated) to the built in java regexp
 *    - add generic types to declarations
 *   1.2.13
 *    - adds support for RESyntaxException
 *
 */
package fr.dgac.ivy ;
import java.io.*;
import java.net.*;
import java.util.HashMap ;
import java.util.Map ;
import java.util.Properties ;
import gnu.getopt.Getopt;
import java.util.regex.*;

class ProxyClient extends Ivy {

  private Socket clientSocket;
  private PrintWriter out;
  private BufferedReader in;
  private static boolean debug = (System.getProperty("IVY_DEBUG")!=null) ;
  private volatile Thread clientThread;		// volatile to ensure the quick communication
  private Map<String,String> id=new HashMap<String,String>();
  private Map<String,Ghost>ghosts = new HashMap<String,Ghost>();
  private Map<String,Puppet> puppets =new HashMap<String,Puppet>(); // key=id value=Puppet
  String domain=null;

  public static final int DEFAULT_SERVICE_PORT = 3456 ;
  public static final String DEFAULTNAME = "ProxyClient";
  public static final String helpmsg = "usage: java fr.dgac.ivy.ProxyClient [options] hostname\n\t-b BUS\tspecifies the Ivy bus domain\n\t-p\tport number, default "+DEFAULT_SERVICE_PORT+"\n\t-n ivyname (default "+DEFAULTNAME+")\n\t-q\tquiet, no tty output\n\t-d\tdebug\n\t-h\thelp\ncontacts the ProxyMaster on hostname\n";

  private static String name = DEFAULTNAME;
  public static void main(String[] args) {
    Ivy bus;
    Getopt opt = new Getopt("ProxyClient",args,"n:b:dqp:h");
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
	throw new RuntimeException();
      }
      break;
    case 'h':
    default:
      System.out.println(helpmsg);
      return ;
    }
    String hostname="localhost";
    try {
      new ProxyClient(hostname,servicePort,domain).start();
    } catch (IvyException ie) {
      System.out.println("error, can't connect to Ivy");
      ie.printStackTrace();
      throw new RuntimeException();
    } catch (IOException ioe) {
      System.out.println("error, can't connect to the proxy master");
      ioe.printStackTrace();
      throw new RuntimeException();
    }
  }

  public ProxyClient(String hostname,int servicePort,String domain) throws IOException, IvyException {
    super(name,name+" ready",null); // I will join the bus
    System.out.println("PC contacting tcp:"+hostname+":"+servicePort);
    clientSocket = new Socket(hostname,servicePort) ; // contacting hostname:servicePort
    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
    clientThread=new Thread(new Servicer()); //
    this.domain=domain;
  }

  protected void start() throws IvyException {
    clientThread.start();
    start(domain);
    send("Hello bus="+domain);
  }
  
  static Pattern getId,fwdGhost,fwdPuppet,puppetRe;

  static {
    try {
      getId=Pattern.compile("^ID id=(.*) value=(.*)");
      fwdGhost=Pattern.compile("^ForwardGhost id=(.*) buffer=(.*)");
      fwdPuppet=Pattern.compile("^ForwardPuppet id=(.*) buffer=(.*)");
      puppetRe=Pattern.compile("^CreatePuppet id=(.*)");
    } catch ( PatternSyntaxException res ) {
      res.printStackTrace();
      System.out.println("Regular Expression bug in Ivy source code ... bailing out");
      throw new RuntimeException();
    }
  }

  void parseMsg(String msg) {
    // System.out.println("PC parsing "+msg);
    Matcher m;
    if ((m=getId.matcher(msg)).matches()) {
      id.put(m.group(1),m.group(2));
    } else if ((m=puppetRe.matcher(msg)).matches()) { // I must create a puppet
      String puppetId = m.group(1);
      puppets.put(puppetId,new Puppet(this,puppetId,domain));
    } else if ((m=fwdGhost.matcher(msg)).matches()) { // I must forward to the ghost
      Ghost g = ghosts.get(m.group(1));
      try { g.sendBuffer(m.group(2)); } catch( IvyException ie) { ie.printStackTrace(); }
    } else if ((m=fwdPuppet.matcher(msg)).matches()) { // I must forward to the puppet
      Puppet p = puppets.get(m.group(1));
      try { p.parse(m.group(2)); } catch( IvyException ie) { ie.printStackTrace(); }
    } else {
      System.out.println("unknown message "+msg);
    }
  }

  class Servicer implements Runnable {
    public void run() {
      //Thread thisThread = Thread.currentThread();
      traceDebug("Thread started");
      String msg;
      try {
        while (true) {
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
      System.out.println("connexion to ProxyMaster lost");
      for (Puppet p:puppets.values()) p.stop();
      stop(); // leave the bus TODO: make a disconnexion/reconnexion possible ?
    }
  }

  void send(String s) { // sends a message to the proxyMaster
    out.println(s);
    out.flush();
  }

  /*
   * here, I create a ghost instead of a normal Ivy Client, to catch the
   * protocol and forward everything to the proxies.
   * TODO: remember everything in case a new proxy client comes ?
   */
  protected boolean createIvyClient(Socket s,int port, boolean domachin) throws IOException {
    IvyClient i;
    // TODO si c'est un puppet, je ne dois pas creer de Ghost
    // voir meme me deconnecter du biniou ?
    for (Puppet p:puppets.values()) {
      if (( p.bus.getAP() == port ) && !domachin ) {
        // this new Ivy agent is in fact one of my puppets ...
	System.out.println("not Ghosting this (probable) Puppet Ivy agent");
	i= new IvyClient(this,s,port,domachin);
	p.bus.getPool().execute(i);
	return true;
      }
    }
    String key = getWBUId();
    String ghostId;
    send("GetID id="+key); // asks a centralized ID from ProxyMaster
    try { // waits for the answer
      while ((ghostId=id.get(key))==null) { Thread.sleep(200); }
      Ghost g = new Ghost(this,s,port,domachin,ghostId,this);
      ghosts.put(ghostId,g);
      return true;
    } catch (InterruptedException ie) { ie.printStackTrace(); }
    System.out.println("error waiting");
    throw new RuntimeException();
  }

  /*
   * the function to forward the protocol to all the Puppets through the
   * ProxyMaster
   *
   */
  protected void forwardPuppet(String id,String buffer) {
    out.println("ForwardPuppet id="+id+" buffer="+buffer);
    out.flush();
  }

  private static void traceDebug(String s){
    if (debug) System.out.println("-->ProxyClient "+name+"<-- "+s);
  }

  

}
