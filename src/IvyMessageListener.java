/**
 * this interface specifies the methods of an IvyMessageListener
 *
 * @author	Francois-Rzgis Colin
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 */
package fr.dgac.ivy;

public interface IvyMessageListener extends java.util.EventListener
{
  /**
   * this callback is invoked when a message has been received
   * @param client the peer who sent the message
   * @param args the array of string, on string for each subregexp
   */
  void receive(IvyClient client, String[] args);
}
