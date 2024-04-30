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

public class SendNow {

  Ivy bus;

  public SendNow (String args[]) throws IvyException {
    bus = new Ivy("ReceiveNow",null,null);
    bus.bindMsg("^hop hop",new IvyMessageListener() {
      public void receive(IvyClient ic,String args[]){
	System.out.println("hop hop received ! quitting");
	bus.stop();
      }
    });
    System.out.println("starting receiver");
    bus.start(Ivy.getDomainArgs("IvyTest" , args));
  }

  public static void main(final String[] args) throws IvyException,InterruptedException {
    Ivy sendbus = new Ivy("SendNow" , null, null);
    new SendNow(args);
    //Thread.sleep(10);
    // no sleep
    try {
      System.out.println("starting sender");
      sendbus.start(Ivy.getDomainArgs("IvyTest" , args));
      System.out.println("sending");
      // THIS IS WRONG ON PURPOSE, to test a race condidition on startup
      // Correct code to add is
      // sendbus.waitForClient("ReceiveNow",0);
      int i = sendbus.sendMsg("hop hop");
      System.out.println("stopping");
      sendbus.stop();
      System.out.println("end stopping");
      if (i==0) {
	// it can fail in the following case:
	// SendNow has started, sent "hop hop", and starts closing *before*
	// the hanshake is initiated. There's no way of knowing if somebody
	// else is joining the bus
	// we could add something at the JVM level, but it's no use for inter
	// process anyway. 200ms before close + lock is long enough
	System.out.println("the Receiver has not received our message, quitting anyway");
	System.exit(-1);
      }
    } catch (IvyException ie) {
      System.out.println("Ivy main test error");
      ie.printStackTrace();
    }
  }

} 
