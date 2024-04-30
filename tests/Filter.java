/**
 * Another Ivy java library API tester: filters
 *
 * @author  Yannick Jestin <mailto:jestin@cena.fr>
 *
 * (c) CENA
 *
 * usage: java Request
 *
 * Changelog
 *   1.2.16 : first release
 *
 *   rationale:
 *   	Filter limits the bounded sends to  toto and blah
 *   	Remote subscribes to (.*), unbounded, and truc, bounded
 *   	Filter sends truc ble bli (should have one hit only )
 *   	Filter sends TOTO rules, one hit
 *   	total should be 3 (ready message)
 *
 */
import fr.dgac.ivy.*;

class Filter {

  public static void main(String[] args) throws IvyException {
    String domain=Ivy.getDomainArgs("FilterTest",args);
    new Filter(domain);
  }

  int nb=0;
  private Ivy bus;
  private String[] filterStrings = { "toto", "blah" };

  public Filter(String domain) throws IvyException {
    bus = new Ivy("FilterTest","FilterTest ready", null);
    bus.setFilter(filterStrings);
    bus.start(domain);
    new Remote(domain);
    IvyClient remote = bus.waitForClient("Remote", 0);
    bus.sendMsg("truc ble bli");
    bus.sendMsg("TOTO rules");
    remote.sendDie("goodbye");
    bus.stop();
    if (nb != 3) {
      System.out.println("n = "+nb+" should be 3");
      System.exit(-1);
    }
    System.out.println("Filter test successful");
  }

  private class Remote implements IvyMessageListener {
    Ivy bus;
    String name;
    public Remote(String domain) throws IvyException {
      bus = new Ivy("Remote","Remote ready",null);
      bus.bindMsg("^truc (.*)",this);
      bus.bindMsg("(.*)", new IvyMessageListener() {
	public void receive(IvyClient ic,String[] args) {
	  System.out.println("something received: "+args[0]);
	  nb++;
	}
      });
      bus.start(domain);
    }
    
    public void receive(IvyClient ic,String[] args) {
      System.out.println("truc received"+args[0]);
      nb++;
    }

  }

}
