/**
 * Counter:Yet another Ivy java program example.
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * a software bus message "geiger counter" displaying each and every second
 * the number of messages sent on the bus during the past second, the past ten
 * seconds and the past minute.
 *
 * (c) CENA 1998-2003
 * This program is provided as is, under the LGPL licence with the ivy-java
 * package.
 *
 * New:
 *   1.2.3: use of Vector.addElement instead of add() and the old Properties
 *   model
 *
 */
import fr.dgac.ivy.* ;
import gnu.getopt.Getopt ;
import java.util.*;

/**
 * A program to count to the Ivy software bus messages.
 * The class itself can be used to collect data and send them on the terminal
 * or on the bus.
 */
public class Counter implements IvyMessageListener, Runnable {

  private Ivy bus ;
  private int[] secCount = new int[60];
  private int totalminute=0;
  private int totaldix=0;
  private int counter=0;
  private int moindix=secCount.length-10;
  private int moinune=1;
  private Thread thread;
  boolean isRunning=false;
  boolean quiet=false;

  public static final String helpmsg = "usage: java Counter -[options]\n\t-b BUS\tspecifies the Ivy bus domain\n\t-q\tquiet, no tty output\n\t-d\tdebug\n\t-h\thelp\n";

  public Counter(String domain,boolean quiet) throws IvyException {
    this.quiet=quiet;
    for (int j=0;j<secCount.length;j++) {secCount[j]=0;}
    bus = new Ivy("Counter","Counter ready",null);
    System.out.println(bus.domains(domain));
    System.out.println("stats:\t1s\t10s\t1m");
    bus.bindMsg("^EXHAUSTED$",new IvyMessageListener(){
      public void receive(IvyClient client,String[] args) {
	isRunning=false;
      }
    });
    thread = new Thread(this);
    isRunning=true;
    thread.start();
    try {
      bus.start(domain);
    } catch (IvyException ie) {
      ie.printStackTrace();
    }
  }

  // implements the Runnable interface
  public void run() {
    while (isRunning) {
      try {
	thread.sleep(1000);
      } catch (InterruptedException ie) {
      }
      totalminute+=secCount[counter]-secCount[moinune];
      totaldix+=secCount[counter]-secCount[moindix];
      String s = "stats:\t"+ secCount[counter]+"\t"+totaldix+"\t"+totalminute;
      if (!quiet) { System.out.println(s); }
      try { bus.sendMsg(s); } catch (IvyException ie) { }
      moinune=(moinune+1)%secCount.length;
      moindix=(moindix+1)%secCount.length;
      counter=(counter+1)%secCount.length;
      secCount[counter]=0;
    }
  }

  public void receive(IvyClient client,String[] args) { secCount[counter]++; }

  public static void main(String[] args) throws IvyException {
    String domain=Ivy.getDomain(null);
    Getopt opt = new Getopt("Counter",args,"b:dhq");
    int c;
    boolean quiet=false;
    while ((c=opt.getopt()) != -1 ) switch(c) {
      case 'q':
	quiet=true;
	break;
      case 'b':
	domain=opt.getOptarg();
	break;
      case 'd':
	Properties sysProp = System.getProperties();
	sysProp.put("IVY_DEBUG","yes");
	break;
      case 'h':
      default:
	System.out.println(helpmsg);
	System.exit(0);
    }
    new Counter(domain,quiet);
  } // main

} // class Counter
