/**
 * Ivy java library API tester.
 *
 * @author  Yannick Jestin <mailto:jestin@cena.fr>
 *
 * (c) CENA
 *
 */
import fr.dgac.ivy.*;

public class Killer implements IvyMessageListener {

  private Ivy kbus;
  private String killOnMsg;

  public Killer(String domain,String killOnMsg)  {
    try {
      System.out.println("Killer joining the bus");
      kbus = new Ivy("Killer","Killer ready", null);
      this.killOnMsg=killOnMsg;
      kbus.bindMsg(killOnMsg,this);
      kbus.start(domain);
    } catch (IvyException ie) {
      ie.printStackTrace();
    }
  }

  public void receive(IvyClient c,String[] args) {
    c.sendDie("bye bye ! you told me "+killOnMsg);
    System.out.println("Killer leaving the bus");
    kbus.stop();
    System.out.println("Killer has left the bus");
  }

  public static void main(String[] args) throws IvyException {
    new Killer(Ivy.getDomainArgs("Killer",args),"^coucou$");
  }

}
