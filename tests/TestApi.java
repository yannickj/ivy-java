/*
 * Ivy java library API tester.
 *
 * @author  Yannick Jestin <mailto:jestin@cena.fr>
 *
 * (c) CENA
 *
 * usage: java TestApi
 *
 */
import fr.dgac.ivy.*;

class TestApi implements IvyMessageListener, IvyApplicationListener {

  public static final String TestApiReadyMsg = "TestAPI ready";
  public static final String TestMsg = "Test Message";

  private Ivy bus;
  private int test=0;
  private String domain;

  public TestApi(String domain) throws IvyException {
    System.out.println("TestApi joining the bus");
    bus = new Ivy("TestAPI",TestApiReadyMsg, null);
    bus.addApplicationListener(this);
    bus.bindMsg("^"+TestMsg+"$",this);
    bus.start(this.domain=domain);
    bus.sendToSelf(true);
    bus.bindMsg("^go$",new Self(domain));
    System.out.println("sending go ...");
    try { bus.sendMsg("go"); } catch (IvyException ie) {
      ie.printStackTrace();
    }
    System.out.println("go sent");
  }

  public void receive(IvyClient ic,String[] args) {
    String s = "[X] received message";
    for (int i=0;i<args.length;i++) s+=": "+args[i];
    System.out.println(s);test++;
  }

  public void connect(IvyClient ic) {
    if (ic.getApplicationName().compareTo("Sender")!=0) return;
    System.out.println("[X] Sender connected");test++;
  }

  public void disconnect(IvyClient ic) {
    if (ic.getApplicationName().compareTo("Sender")!=0) return;
    System.out.println("[X] Sender disconnected");test++;
  } 

  public void directMessage(IvyClient ic,int id,String arg) {
    if (id!=1) return;
    System.out.println("[X] Direct message received, ID=1");test++;
  }

  public void die(IvyClient ic,int reason,String msg) {
    System.out.println("[X] Die received "+msg);test++;
    System.out.println(test+" tests successful, good bye");
    System.out.println("TestApi leaving the bus");
    bus.stop();
    System.out.println("TestApi has left");
  }

  class Self implements IvyMessageListener {
    private String domain;
    public Self(String domain)  {this.domain=domain;}
    public void receive(IvyClient c,String[] args){
      System.out.println("[X] received my own go message");test++;
      bus.sendToSelf(false);
      new Sender(domain) ;
    }
  }

  class Sender implements IvyMessageListener {
    private Ivy sbus;
    private String domain;
    public Sender(String domain)  {
      try {
	System.out.println("starting Sender on domain "+domain);
	sbus = new Ivy("Sender","Sender ready", null);
	sbus.bindMsg("^"+TestApiReadyMsg+"$",this);
	sbus.start(this.domain=domain);
      } catch (IvyException ie) {
	ie.printStackTrace();
      }
    }
    public void receive(IvyClient c,String[] args) {
      try {
	sbus.sendMsg(TestMsg);
	c.sendDirectMsg(1,"bye bye");
      } catch (IvyException ie) {
      }
      System.out.println("Sender leaving the bus");
      sbus.stop();
      System.out.println("Sender has left the bus");
      new Killer(domain,("^"+TestApiReadyMsg+"$"));
    }
  }

  public static void main(String[] args) throws IvyException {
    new TestApi(Ivy.getDomainArgs("TestApi",args));
  }

}
