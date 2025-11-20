/**
 * the Ghost peers on the bus ( Proxy scheme ), not finished yet, do not use !
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 *  CHANGELOG:
 *  1.2.12
 */
package fr.dgac.ivy ;
import java.io.*;
import java.net.*;
import java.util.*;

class Ghost extends IvyClient {

  private String id; // given to the Proxy by the Master
  private ProxyClient pc;

  Ghost(Ivy bus, Socket socket,int remotePort,boolean incoming,String id,ProxyClient pc)
  throws IOException {
    super(bus,socket,remotePort,incoming);
    this.id=id;
    this.pc=pc;
    System.out.println("Ghost["+id+"] created");
  }

  // ProxyClient -> Ghost -> Ivy Bus
  protected synchronized void sendBuffer( String s ) throws IvyException {
    System.out.println("Ghost["+id+"] sending ["+s+"]");
    super.sendBuffer(s); // and to all the agents on the Ghost bus ? I'm not sure
  }

  // Bus -> Ghost -> ProxyClient -> ProxyMaster -> other buses
  protected boolean newParseMsg(String s) throws IvyException {
    // I received a message from an agent on the bus
    if (pc!=null) {
      System.out.println("Ghost["+id+"] forwarding ["+s+"]");
      pc.forwardPuppet(id,s); // forward to all the puppets
    } else {
      System.out.println("Warning, Ghost ["+id+"] could not forward ["+s+"] to null pc");
    }
    return super.newParseMsg(s); // I'm a normal Ivy citizen
  }

}
