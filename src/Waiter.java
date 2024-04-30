/**
 * a helper class to implement "Wait for Message" in {@link Ivy.waitForMsg}
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 *  CHANGELOG:
 *  1.2.8:
 *    no more import of java.util.*
 */

package fr.dgac.ivy ;

class Waiter extends WaitFor implements IvyMessageListener {

  public Waiter(int timeout) {
    super(timeout);
    setName("Ivy Waiter thread, for message");
  }

  boolean check() { return false; }

  public void receive(IvyClient ic, String[] args) {
    received=ic;
    interrupt();
  }
}
