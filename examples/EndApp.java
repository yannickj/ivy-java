/**
 * example of close code
 * (c) CENA
 * Changelog:
 * 1.2.12
 */
import fr.dgac.ivy.* ;
import javax.swing.*;

public class EndApp extends IvyApplicationAdapter {

  public static void main(String[] args) throws IvyException {
    Ivy bus=new Ivy("EndApp","EndApp ready",null);
    EndApp e = new EndApp(bus); // a frame is opened, and the Swing Thread is started
    bus.addApplicationListener(e);
    bus.start(Ivy.getDomain(null));  // Ivy threads are up and running
    // the control flow won't stop until the end of all above threads
  }

  private Ivy bus;
  JFrame f;

  public EndApp(Ivy b) {
    this.bus=b;
    f=new JFrame("test");
    f.getContentPane().add(new JLabel("some label"),java.awt.BorderLayout.CENTER);
    f.pack();
    f.setVisible(true);
  }

  public void die(IvyClient client, int id,String msgarg) {
    System.out.println("received die msg from " + client.getApplicationName());
    f.dispose(); // closes the only window, thus quitting the swing thread
  } // end of die callback, the Ivy threads are stopped

}
