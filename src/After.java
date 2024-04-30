/**
 * an utility waiting for a message to come, then exiting.
 *
 * the typical use is to stop a script execution flow until a message has been
 * received in a shell script the following way
 *
 * <br>
 * <code>echo "waiting for Toto"
 * java -classpath ivy-java.jar fr.dgac.ivy.tools.After -b 224.5.6.7:8910 "Toto READY"
 * echo "Toto is here, continuing".
 * </code>
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (c) CENA
 *
 * Changelog:
 * 1.2.14
 *    - gracefully quits when message is received, by quitting the Ivy
 *    threads with bus.stop() , instead of invoking system.exit
 * 1.2.8: new in the ivy package
 */
package fr.dgac.ivy.tools ;
import fr.dgac.ivy.* ;
import gnu.getopt.Getopt;

public class After extends IvyApplicationAdapter implements IvyMessageListener {

  public final static int DEFAULTTIMEOUT = 0 ;

  public static final String helpmsg = "usage: java fr.dgac.ivy.After [options] regexp\n\t-b BUS\tspecifies the Ivy bus domain\n\t-t\ttime out in seconds, defaults to "+DEFAULTTIMEOUT+"\n\t-h\thelp\n\n\t regexp is a Perl5 compatible regular expression";

  public static void main(String[] args) throws IvyException {
    Getopt opt = new Getopt("After", args, "b:t:");
    int c;
    String domain = null;
    String name = "AFTER";
    int timeout = DEFAULTTIMEOUT;
    while ((c = opt.getopt()) != -1) switch (c) {
      case 'b': domain = opt.getOptarg(); break;
      case 't': timeout = Integer.parseInt(opt.getOptarg()); break;
      case 'h':
      default: System.out.println(helpmsg); return;
    }
    if (opt.getOptind() != args.length-1) { System.out.println(helpmsg); return; }
    String regexp=args[opt.getOptind()]; 
    Ivy bus = new Ivy(name, name+" ready", null);
    bus.bindMsgOnce(regexp, new After(bus));
    bus.start(domain);
    if (timeout>0) {
      System.out.println("waiting "+timeout+"s for "+regexp);
      try { Thread.sleep(timeout*1000); } catch (InterruptedException ie) { }
      System.out.println(regexp+" not received, bailing out");
      bus.stop();
    } else {
      System.out.println("waiting forever for "+regexp);
    }
  }

  private Ivy bus;
  public After(Ivy b) {
    bus = b;
    bus.addApplicationListener(this);
  }

  public void die( IvyClient client, int id, String msgarg) {
    System.out.println("die received, bailing out");
    bus.stop();
  }

  public void receive(IvyClient ic, String[] args) {
    bus.stop();
  }

}
