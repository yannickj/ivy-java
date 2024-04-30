/**
 * signals that an unrecoverrable Ivy exception has occured.
 *
 * @author	Francois-Rzgis Colin
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * changelog:
 * 1.2.8:
 * 	added a serialVersionUID to be compatible with jdk1.5
 * 1.0.12 changed default access constructor to public access
 */
package fr.dgac.ivy;

public class IvyException extends Exception {
  static final long serialVersionUID=1L;
  public IvyException(String s) { super(s); }
}
