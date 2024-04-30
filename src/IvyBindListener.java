/**
 * this interface specifies the methods of a BindListener; it is only useful if you want
 * to develop a bus monitor (Ivy Probe, spy, ivymon)
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * Changelog:
 * 1.2.8
 *   - removed the public abstract modifiers, which are redundant
 */
package fr.dgac.ivy;

public interface IvyBindListener extends java.util.EventListener {

  /**
   * invoked when a Ivy Client performs a bind
   * @param client the peer
   * @param id the regexp ID
   * @param regexp the regexp
   */
  void bindPerformed(IvyClient client,int id, String regexp);

  /**
   * invoked when a Ivy Client performs a unbind
   * @param client the peer
   * @param id the regexp ID
   * @param regexp the regexp
   */
  void unbindPerformed(IvyClient client,int id,String regexp);

}
