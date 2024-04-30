/**
 * the Protocol magic numbers and chars.
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 *  CHANGELOG:
 *  1.2.16
 *    introduced to remove the int enum pattern
 */

package fr.dgac.ivy;

enum Protocol {

    BYE(0),     /* end of the peer */
    ADDREGEXP(1), /* the peer adds a regexp */
    MSG(2),     /* the peer sends a message */
    ERROR(3),    /* error message */
    DELREGEXP(4), /* the peer removes one of his regex */
    ENDREGEXP(5), /* no more regexp in the handshake */
    SCHIZOTOKEN(6),  /* avoid race condition in concurrent connexions, aka BeginRegexp in other implementations */
    DIRECTMSG(7), /* the peer sends a direct message */
    DIE(8),   /* the peer wants us to quit */
    PING(9),
    PONG(10);

    final static char STARTARG = '\u0002';/* begin of arguments */
    final static char ENDARG = '\u0003'; /* end of arguments */
    final static char ESCAPE = '\u001A';
    final static char NEWLINE = '\n';
    final static int PROTOCOLVERSION = 3 ;
    final static int PROTOCOLMINIMUM = 3 ;

    private int value = -1;
    private Protocol(int v) {this.value = v;}

    int value() {return value;}

    static Protocol fromString(String s) throws IvyException {
      try {
	return fromInt(Integer.parseInt(s));
      } catch (NumberFormatException nfe) {
	throw new IvyException("protocol problem: "+s+" is not a valid integer");
      }
    }

    static Protocol fromInt(int i) throws IvyException {
      for (Protocol p : Protocol.values())
	if (p.value() == i) return p;
      throw new IvyException("protocol magic number "+i+" not known");
    }

    @Override public String toString() { return ""+value; }

}
