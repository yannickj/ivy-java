/**
 * Ivy software bus package Enum helper utility, used from {@link Ivy.bindAsyncMsg} to choose whether the
 * callbacks will be performed either in the same thread of the ivy protocol * handling, or in the the Swing Thread, or in a newly created thread.
 * 
 *
 * @author Yannick Jestin <a
 * href="mailto:yannick.jestin@enac.fr">yannick.jestin&enac.fr</a>
 * @author <a href="http://www2.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (c) CENA 1998-2004
 * (c) ENAC 2005-2011
 *
 *  CHANGELOG:
 *  1.2.14
 *  	introduced
 */

package fr.dgac.ivy;

public enum BindType {NORMAL, ASYNC, SWING }
