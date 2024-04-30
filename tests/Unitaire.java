/**
 * Ivy java library API tester.
 *
 * @author  Yannick Jestin <mailto:yannick.jestin@enac.fr>
 *
 * (c) ENAC
 *
 * usage: java Unitaire
 *
 */
import fr.dgac.ivy.*;

public class Unitaire  {

  public static void main(final String[] args) {
    Ivy bus = new Ivy("Test Unitaire" , "TU ready" , null);
    final int PORT_TEST = 5000;
    try {
      bus.start(Ivy.getDomainArgs("IvyTest" , args));
      System.out.println("waiting 5 seconds for a coucou");
      System.out.println(((bus.waitForMsg("^coucou" , PORT_TEST)) != null) ? "coucou received" : "coucou not received");
      System.out.println("waiting 5 seconds for IvyProbe");
      System.out.println(((bus.waitForClient("IVYPROBE" , PORT_TEST)) != null) ? "Ivyprobe joined the bus" : "nobody came");
      System.out.println("random values: " + bus.getWBUId() + ", " + bus.getWBUId() + ", " + bus.getWBUId());
      bus.stop();
    } catch (IvyException ie) {
      System.out.println("Ivy main test error");
      ie.printStackTrace();
    }
  }

} 
