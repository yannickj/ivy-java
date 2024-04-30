/*
 * sends die to the client specified by a string.
 */
import fr.dgac.ivy.* ;

public class DieSender extends IvyApplicationAdapter {

  private String tokill;
  private int targets = 0;

  public DieSender(Ivy bus,String mtokill) throws IvyException {
    this.tokill=mtokill;
    bus.addApplicationListener(this);
  }

  /*
  public void disconnect(IvyClient c) {
    System.out.println(c.getApplicationName());
    if (c.getApplicationName().compareTo(tokill)==0) {
     System.out.println(tokill+" left the bus");
    }
  }
  */

  public void connect(IvyClient c) {
    if (c.getApplicationName().compareTo(tokill)==0) {
     System.out.println("found a "+tokill+" on the bus, sending Die Message");
     c.sendDie("meurs !");
    }
  }

  public static final String DEFAULTTOKILL = "JPROBE" ;

  public static void main(String[] args) throws IvyException {
    String mtokill = DEFAULTTOKILL;
    String domain = Domain.getDomainArgs("DieSender",args);
    System.out.println("will kill each and every " + mtokill + " on the " + domain +" bus");
    Ivy bus = new Ivy("DieSender","DieSender ready",null);
    bus.start(domain);
    new DieSender(bus,mtokill);
  }

}
