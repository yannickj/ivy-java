/**
 * Ivy software bus bench tester
 *
 * @author	Yannick Jestin <yannick.jestin@enac.fr>
 *
 * (c) CENA 1998-2004
 * (c) ENAC 2005-2011
 *
 *  a program with 2 busses testing
 *  - the rendez vous
 *  - the IvyMessageListener interface
 *  - disconnect part of the IvyApplicationListener interface 
 *
 *  CHANGELOG
 *  1.2.6
 *    - added a timestamp to facilitate the batch processing
 *    - no more necessary to invoque system.exit() when both busses have left.
 *    it used to produce an Exception ...
 *
 */
import gnu.getopt.*;
import fr.dgac.ivy.*;

class BenchLocal  {

  public static final String helpmsg = "usage: java TestLocal [options]\n\t-b domain\n\t-d delay (in ms)\n\t-t test number\n\t-h\thelp\n\n";

  public static void main(String[] args) throws IvyException {
    Getopt opt = new Getopt("BenchLocal",args,"t:b:d:h");
    String domain=null;
    int delay=2000;
    int c;
    int testtoperform=1;
    while ((c = opt.getopt()) != -1) switch (c) {
    case 'b':
      domain=opt.getOptarg();
      break;
    case 'd':
      delay=Integer.parseInt(opt.getOptarg());
      break;
    case 't':
      testtoperform=Integer.parseInt(opt.getOptarg());
      break;
    case 'h':
    default:
	System.out.println(helpmsg);
	System.exit(0);
    }
    new BenchLocal(testtoperform,domain,delay);
  }

  public BenchLocal(int testtoperform,String domain,int delay) throws IvyException {
    System.out.println("test to perform: "+testtoperform);
    switch (testtoperform) {
      case 2:
	testRegex(domain,delay);
	break;
      case 1:
      default:
	test2bus(domain,delay);
	break;
    }
  }

  void sleep(int delay) {
    log("waiting "+delay+" ms");
    if (delay==0) return; // a Kaffe bug ?
    try { Thread.sleep(delay); } catch (InterruptedException ie) { } 
  }

  public void testRegex(String domain,int delay) throws IvyException {
    Ivy bus1,bus2;
    IAL ial=new IAL();
    bus1=new Ivy("BUS1","Bus1 ready",ial);
    bus2=new Ivy("BUS2","Bus2 ready",ial);
    SuccessStory success=new SuccessStory(bus1,bus2);
    bus1.bindMsg("^Bus2 ready",new RML(bus1,delay));
    bus2.bindMsg("^([^ ]*) ([^ ]*) ([^ ]*)$",new RMLAnswer1(success));
    bus2.bindMsg("blah(.*)",new RMLAnswer2(success));
    bus2.bindMsg(".*y=([^ ]+).*",new RMLAnswer3(success));
    bus1.start(domain);
    bus2.start(domain);
  }

  private class RML implements IvyMessageListener  {
    Ivy b;
    int delay;
    public RML(Ivy b,int delay) { this.b=b;this.delay=delay; }
    public void receive(IvyClient c,String[] args) {
      try {
	b.sendMsg("a b c");
	sleep(delay);
	b.sendMsg("blah");
	sleep(delay);
	b.sendMsg("x=1 y=2 z=3");
      } catch (IvyException ie) {
	ie.printStackTrace();
      }
      b.stop();
    }
  }

  private class SuccessStory {
    Object lock = new Object();
    private int i=0;
    Ivy b1,b2;
    public SuccessStory(Ivy b1,Ivy b2){this.b1=b1;this.b2=b2;}
    public void incr(){
      synchronized (lock) {
	i++;
	log("regex "+i+" successful");
	if (i>=3) {
	  log("quitting the bus");
	  b1.stop();
	  b2.stop();
	} else {
	  log("pas encore, j'ai reçu "+i+" <3");
	}
      }
    }
  }

  private class RMLAnswer1 implements IvyMessageListener {
    SuccessStory ss;
    public RMLAnswer1(SuccessStory ss) {this.ss=ss;}
    public void receive(IvyClient c,String[] args) {
      if ( (args.length==3)
	  && (args[0].compareTo("a")==0)
	  && (args[1].compareTo("b")==0)
	  && (args[2].compareTo("c")==0)) {
	ss.incr();
	System.out.println("answer 1 ok");
	  }
      }
  }

  private class RMLAnswer2 implements IvyMessageListener {
    SuccessStory ss;
    public RMLAnswer2(SuccessStory ss) {this.ss=ss;}
    public void receive(IvyClient c,String[] args) {
      if ( (args.length==1) && (args[0].compareTo("")==0) ) {
	ss.incr();
	System.out.println("answer 2 ok");
      }
    }
  }

  private class RMLAnswer3 implements IvyMessageListener {
    SuccessStory ss;
    public RMLAnswer3(SuccessStory ss) {this.ss=ss;}
    public void receive(IvyClient c,String[] args) {
      if ( (args.length==1) && (args[0].compareTo("2")==0)
	 ) {
	ss.incr();
	System.out.println("answer 3 ok");
      }
    }
  }

  public void test2bus(String domain,int delay) throws IvyException {
    Ivy bus1,bus2;
    IAL ial=new IAL();
    log("starting with delay="+delay+" ms between the two starts");
    bus1=new Ivy("BUS1","Bus1 ready",ial);
    bus2=new Ivy("BUS2","Bus2 ready",ial);
    bus1.bindMsg("^Bus2 ready",new SENDOUT(bus1));
    bus2.bindMsg("^out$",new DIE(bus2));
    log("starting Bus1");
    bus1.start(domain);
    sleep(delay);
    log("starting Bus2");
    bus2.start(domain);
  }

  private class SENDOUT implements IvyMessageListener {
    Ivy b;
    public SENDOUT(Ivy b) {this.b=b;}
    public void receive(IvyClient client,String[] arg) {
      log("received ready message");
      try {
	b.sendMsg("out");
      } catch (IvyException ie) {
	ie.printStackTrace();
      }
      b.stop();
    }
  }

  private class IAL extends IvyApplicationAdapter  {
    public void disconnect(IvyClient c) { log(c.getApplicationName()+" left"); }
  }

  private class DIE  implements IvyMessageListener {
    Ivy b;
    public DIE(Ivy b) {this.b=b; }
    public void receive(IvyClient client,String[] arg) {
      log("received out message, b="+b);
      b.stop(); // I leave the bus
    }
  }

  private static java.text.DateFormat df = java.text.DateFormat.getTimeInstance();
  private void log(String s) {
    java.util.Date d = new java.util.Date();
    System.out.println("* ["+df.format(d)+"] "+s);
  }

} 
