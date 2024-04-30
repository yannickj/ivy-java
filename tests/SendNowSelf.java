/**
 * Ivy java library API tester.
 * TODO does not work if there is anotehr agent on the bus ...? FIXME 
 *
 * @author  Yannick Jestin <mailto:yannick.jestin@enac.fr>
 *
 * (c) ENAC
 *
 * usage: java Unitaire
 *
 */
import fr.dgac.ivy.*;

public class SendNowSelf {

  Ivy bus;

  public SendNowSelf(String domain) {
    bus = new Ivy("SendNowSelf" , null, null);
    try {
      System.out.println("starting sender");
      bus.sendToSelf(true);
      bus.bindMsg("^hop hop", new IvyMessageListener() {
	public void receive(IvyClient ic, String args[]){
	  System.out.println("stopping");
	  bus.stop();
	  System.out.println("end stopping");
	}
      });
      bus.start(domain);
      System.out.println("sending");
      if ( bus.sendMsg("hop hop") != 1) {
	System.out.println("error, lock, hop hop not received");
	System.exit(-1);
      }
    } catch (IvyException ie) {
      System.out.println("Ivy main test error");
      ie.printStackTrace();
    }
  }

  public static void main(final String[] args) throws IvyException,InterruptedException {
    new SendNowSelf(Ivy.getDomainArgs("SendNowSelf" , args));
  }

} 
