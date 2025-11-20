/**
 * this interface specifies the methods of an ApplicationListener
 * @see IvyApplicationAdapter
 *
 * @author	Francois-Rzgis Colin
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * The ApplicatinListenr  for receiving application level events on the Ivy
 * bus: connexion, disconnexion, direct messages or requests to quit. 
 *
 * Changelog:
 * 1.2.8
 *   - removed the public abstract modifiers, which are redundant
 * 1.2.4
 *   - sendDie now requires a String argument ! It is MANDATORY, and could
 *   impact your implementations !
 */
package fr.dgac.ivy;

public interface IvyApplicationListener extends java.util.EventListener {
  /**
   * invoked when a Ivy Client has joined the bus
   * @param client the peer
   */
  void connect(IvyClient client);
  /**
   * invoked when a Ivy Client has left the bus
   * @param client the peer
   */
  void disconnect(IvyClient client);
  /**
   * invoked when a peer request us to leave the bus
   * @param client the peer
   */
  void die(IvyClient client, int id,String msgarg);
  /**
   * invoked when a peer sends us a direct message
   * @param client the peer
   * @param id 
   * @param msgarg the message itself
   *
   * there is no need to use a bus close() or stop() operation within a die()
   * method, it will be called automatically. Furthermore, it is considered
   * poor style to enforce the end of a program with System.exit(), you should
   * consider terminating all threads ( AWT, etc )
   */
  void directMessage( IvyClient client, int id,String msgarg );
}
