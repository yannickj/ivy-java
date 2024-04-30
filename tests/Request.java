/**
 * Another Ivy java library API tester: requests
 *
 * @author  Yannick Jestin <mailto:jestin@cena.fr>
 *
 * (c) CENA
 *
 * usage: java Request
 *
 * Changelog
 *   1.2.8 : first release
 *
 */
import fr.dgac.ivy.*;

class Request {

  public static void main(String[] args) throws IvyException {
    String domain=Ivy.getDomainArgs("RequestTest",args);
    new Request(domain);
  }

  int nb=0;
  private Ivy bus;
  String id;
  static int serial = 0;
  private static final int NBCOMP = 5;
  private static final int dodo = 0;

  public Request(String domain) throws IvyException {
    bus = new Ivy("RequestTest","RequestTest ready", null);
    id=bus.getWBUId();
    bus.bindMsg("^Computer\\d ready",new IvyMessageListener() {
      public void receive(IvyClient ic,String[] args) {
	try { bus.sendMsg("computesum id="+id+" a=3 b=4");} catch (IvyException ie ) { }
      }
    });
    bus.bindMsgOnce("^result id="+id+" value=([0-9]+)",new IvyMessageListener() {
      public void receive(IvyClient ic,String[] args) {
	System.out.println("result received: "+args[0]);
	try {Thread.sleep(1000);} catch (InterruptedException ie) { }
	System.exit(0);
      }
    });
    bus.start(domain);
    System.out.println("launching "+NBCOMP+" Computers");
    for (int i=0;i<NBCOMP;i++) new Computer(domain);
  }

  private class Computer implements IvyMessageListener {
    Ivy bus;
    String name;
    public Computer(String domain) throws IvyException {
      name = "Computer"+serial++;
      bus = new Ivy(name,name+" ready",null);
      bus.bindMsg("^computesum id=([^ ]*) a=([0-9]*) b=([0-9]*)",this);
      bus.start(domain);
    }
    public void receive(IvyClient ic,String[] args) {
      String id=args[0];
      int a=Integer.parseInt(args[1]);
      int b=Integer.parseInt(args[2]);
      System.out.println(name+" sending result, id: "+id+", a:"+a+", b:"+b);
      try { bus.sendMsg("result id="+id+" value="+(a+b));}
      catch (IvyException ie) { ie.printStackTrace(); }
    }
  }

}
