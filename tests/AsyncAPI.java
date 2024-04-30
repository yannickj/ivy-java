/**
 * Ivy java Async API tester: bindAsyncMsg, sendAsyncMsg.
 *
 * @author  Yannick Jestin <mailto:jestin@cena.fr>
 *
 * (c) CENA
 *
 * usage: java AsyncAPI -h
 *
 * this program tests the Asynchronous reception of messages ( each callback
 * is performed in a separate threads ). It also exhibits the behaviour of the
 * library with regards to concurrent connections ! To stress the test, try it
 * with different JVM ( kaffe is especially hard to pass ), and on SMP
 * machines ...
 *
 * changelog
 *
 *  1.2.6 : async sending seems utterly buggy ...
 *
 */
import fr.dgac.ivy.*;
import gnu.getopt.*;

class AsyncAPI {

  public static final int MSGSIZE = 10;
  public static final int NBITER = 100;
  public static final int DELAYMS = 1000;
  public static final String HEADER = "ASYNCPACKET";
  public static final String TOSUBSCRIBE = "^"+HEADER+"([0-9]+) (.*)";
  public static final String RECEIVENAME = "MSreceive";
  public static final String SENDNAME = "MSsend";
  private static long epoch = System.currentTimeMillis();

  private Ivy bus;
  DelayAnswer delay;
  int re;
  private String name;
  boolean verbose;
  private int nbpacket;
  private int wait=0;

  public AsyncAPI(int nb,String domain,int d, boolean v,boolean async) throws IvyException {
    verbose=v;
    nbpacket=nb;
    bus = new Ivy(RECEIVENAME,null, null);
    wait = d;
    delay=new DelayAnswer();
    if (async) re = bus.bindAsyncMsg(TOSUBSCRIBE,delay,BindType.ASYNC);
    else re = bus.bindMsg(TOSUBSCRIBE,delay);
    bus.start(domain);
  }

  private static java.text.DateFormat df = java.text.DateFormat.getTimeInstance();
  private static String date() {
    return  "["+df.format(new java.util.Date())+"] ";
  }

  
  static Object truc = new Object();
  volatile int count=0,total=0;
  int status = 0;

  synchronized void huh(IvyClient ic, String[] args) {
    count++;
    if (verbose) { 
      System.out.println(date()+"RECEIVE "+ count+"/"+nbpacket+" packets received arg:("+args[0]+")");
      int nb = Integer.parseInt(args[0]);
      total+=nb;
      if (nb!=count) {
	System.out.println("RECEIVE *** ERROR *** "+count+"!="+nb+ " - probable double connexion");
	for (IvyClient i : bus.getIvyClients() ) System.out.println("client: "+i);
	ic.sendDie("nok, bye");
	bus.stop();
	System.exit(-1);
      }
    }
    if (count<nbpacket) return;
    if (total==(((nbpacket+1)*nbpacket)/2)) {
      System.out.println("RECEIVE receiver quitting the bus normally");
      ic.sendDie("ok, bye");
      bus.stop();
    } else {
      System.out.println("RECEIVE wrong count and total, hit ^C to exit");
      //System.exit(-1);
    }
  }

  class DelayAnswer implements IvyMessageListener {
    public void receive(IvyClient ic, String[] args) {
      huh(ic,args);
      try { 
	System.out.println("RECEIVE Sleeping "+wait+"ms");
	Thread.sleep(wait);
	System.out.println("RECEIVE Finished Sleeping");
      } catch (InterruptedException ie) {
	System.out.println("RECEIVE Sleeping interrupted, not a problem");
      }
    }
  }

  public static final String helpmsg = "usage: java "+SENDNAME+" [options]\n\t-b domain\n\t-r\t to enable async reception\n\t-x to enable async sending\n\t-l loop count (default "+NBITER+")\n\t-s msgsize\t int value (default "+MSGSIZE+")\n\t-d delay in milliseconds (default "+DELAYMS+")\n\t-q \tquiet\n\t-e\tsend bus stop at the end\n\t-h\thelp\n\n";
  public static void main(String[] args) throws IvyException {
    Getopt opt = new Getopt(SENDNAME,args,"b:l:d:s:xrqeh");
    String domain=null;
    int nb = NBITER, delay = DELAYMS, c, size = MSGSIZE;
    boolean doasyncSend=false, doasyncBind=false, verbose=true, exit=false;
    while ((c = opt.getopt()) != -1) switch (c) {
      case 'b':
	domain=opt.getOptarg();
	break;
      case 's':
	size=Integer.parseInt(opt.getOptarg());
	break;
      case 'l':
	nb=Integer.parseInt(opt.getOptarg());
	break;
      case 'd':
	delay=Integer.parseInt(opt.getOptarg());
	break;
      case 'e':
	exit=true;
	break;
      case 'q':
	verbose=false;
	break;
      case 'x':
	System.out.println("async sending is not robust enough. end of test.");
	System.exit(-1);
	doasyncSend=true;
	break;
      case 'r':
	doasyncBind=true;
	break;
    case 'h':
    default:
	System.out.println(helpmsg);
	System.exit(0);
    }
    System.out.println("bus:"+domain+" loop:"+nb+" delay:"+delay+" verbose:"+verbose+" asyncBind:"+
	doasyncBind+" asyncSend:"+doasyncSend+" msgsize:"+size);

    AsyncAPI receiver = new AsyncAPI(nb,domain,delay,verbose,doasyncBind);
    Ivy mainbus = new Ivy(SENDNAME,null, null);
    mainbus.start(domain);
    if ((mainbus.waitForClient(RECEIVENAME,5000))==null) {
      System.out.println(RECEIVENAME+" did not join the bus. Quitting");
      System.exit(-1);
    }
    System.out.println(RECEIVENAME+" is here, sending packets");
    StringBuffer tosend = new StringBuffer(size);
    for (int i=0;i<size;i++) tosend.append("a");
    for (int i=1;i<=nb;i++) {
      if (verbose) System.out.println(date()+"SENDER sending packet "+i);
      mainbus.sendMsg(HEADER+i+" "+tosend.toString());
      // TODO mainbus.sendMsg(HEADER+i+" "+tosend.toString(),doasyncSend);
    }
    System.out.println(date()+"SENDER sender has sent all its packets, waiting for a die message");
    // i won't stop the sender's bus here, otherwise the all the packet
    // can still be unprocessed
    // TODO regession test for Ivy.stop()
  }

}
