/**
 * this class is a dummy ApplicationListener
 *
 * @author	Francois-Rzgis Colin
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * an ApplicationListener class for handling application-level request on the
 * Ivy bus. The methods in this class are empty. This class exists as a
 * convenience for implementing a subset of the methods of the
 * applicationlistener. See the AWT 1.1 framework for further information on
 * this.
 *
 * changelog:
 * 1.0.12: fixed a missing id in the parameters
 */
package fr.dgac.ivy;
 
public abstract class IvyApplicationAdapter implements IvyApplicationListener {
  public void connect( IvyClient client ) { }
  public void disconnect( IvyClient client ) { }
  public void die( IvyClient client, int id, String msgarg) { }
  public void directMessage( IvyClient client, int id,String msgarg ) {}
}
