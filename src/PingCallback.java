/**
 * this interface specifies the methods of an PingCallback
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * helps the probe utility to measure the time af a ping roundtrip to a remote
 * agent.
 *
 * Changelog:
 * 1.2.12
 */
package fr.dgac.ivy;

public interface PingCallback {
  /**
   * invoked when a Pong is received
   * @param elapsedTime the elapsed time in milliseconds between the ping and
   * the pong
   */
  void pongReceived(IvyClient ic,int elapsedTime);
}
