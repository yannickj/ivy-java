/**
 * Part of a Ivy-level proxy, still in development, DO NOT USE
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (c) CENA 1998-2004
 *
 *  CHANGELOG:
 *  1.2.16
 *    uses Protocol enum
 *  1.2.14
  *   - switch from gnu regexp (deprecated) to the built in java regexp
 *    - add generic types to declarations
 *  1.2.13:
 *    - adds support for RESyntaxException
 */

package fr.dgac.ivy ;
import java.lang.Thread;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

class Puppet {

  // the mapping between Ghost regexp and local bus regexp numbers
  Hashtable<String,String> bound = new Hashtable<String,String>(); // ghostID localID
  Hashtable<String,String>regexps = new Hashtable<String,String>(); // ghostID textRegexp
  String domain;
  String appName;
  ProxyClient pc;
  String id;
  boolean started;
  PuppetIvy bus;

  Puppet(ProxyClient pc,String id,String domain) {
    this.domain=domain;
    this.pc=pc;
    this.id=id;
  }

  void sendGhost(String s) { pc.send("ForwardGhost id="+id+" buffer="+s); }

  class ForwardMessenger implements IvyMessageListener {
    String localId,ghostId;
    public ForwardMessenger(String ghostId,String re) throws IvyException {
      this.ghostId=ghostId;
      this.localId = Integer.valueOf(bus.bindMsg(re,ForwardMessenger.this)).toString();
      bound.put(ghostId,localId);
    }
    public void receive(IvyClient ic,String args[]) {
      StringBuffer tosend = new StringBuffer(Protocol.MSG.value());
      tosend.append(" ");
      tosend.append(ghostId);
      tosend.append(Protocol.STARTARG);
      for (int i=0;i<args.length;i++) {
	tosend.append(args[i]);
	tosend.append(Protocol.ENDARG);
      }
      sendGhost(tosend.toString());
    }
  } // ForwardMessenger

  void addRegexp(String ghostId,String re) {
    regexps.put(ghostId,re);
    try {
      if (started) new ForwardMessenger(ghostId,re);
    } catch( IvyException ie) { ie.printStackTrace(); }
  }

  void removeRegexp(String ghostId) {
    try {
      bus.unBindMsg(Integer.parseInt((String)bound.remove(ghostId)));
    } catch( IvyException ie) { ie.printStackTrace(); }
  }

  void stop() {
    if (started) bus.stop();
  }

  // ivy forwarded protocol message
  static Pattern ivyProto;

  static {
    try {
      ivyProto = Pattern.compile("(\\d+) (\\d+)\\02(.*)");
    } catch (PatternSyntaxException res ) {
      res.printStackTrace();
      System.out.println("Regular Expression bug in Ivy source code ... bailing out");
      System.exit(0);
    }
  }

  void parse(String s) throws IvyException {
    Matcher m;
    if (!(m=ivyProto.matcher(s)).matches()) { System.out.println("Puppet error, can't parse "+s); return; } 
    Protocol pcode=Protocol.fromString(m.group(1));
    String pid=m.group(2);
    String args=m.group(3);
    trace("must parse code:"+pcode+" id:"+pid+" args:"+args);
    switch (pcode) {
      case ADDREGEXP: // the Ghost's peer subscribes to something
	addRegexp(pid,args);
      	break;
      case DELREGEXP: // the Ghost's peer unsubscribes to something
	removeRegexp(pid);
        break;
      case BYE:	// the Ghost's peer disconnects gracefully
      	bus.stop();
	// TODO end of the puppet ?
      	break;
      case DIE:
      	// the Ghost's peer wants to ... kill ProxyClient ?
      	break;
      case MSG:
      	// the Ghost's peer sends a message to ProxyClient, with regard to one
	// of our subscriptions
	// TODO a qui le faire passer ?
      	break;
      case SCHIZOTOKEN:
      	appName = args;
	bus = new PuppetIvy(appName,appName+" fakeready",null);
	for ( String ghostId: regexps.keySet() )new ForwardMessenger(ghostId,regexps.get(ghostId));
	started=true;
	trace("starting the bus on "+domain);
	bus.start(domain);
        break;
      case ERROR:
      case ENDREGEXP:
      case DIRECTMSG:
      case PING:
      case PONG:
      default:
    	trace("unused Ivy protocol code "+pcode);
    }
  }

  static class PuppetIvy extends Ivy {
    PuppetIvy(String name,String ready,IvyApplicationListener ial){super(name,ready,ial);}
    protected boolean createIvyClient(Socket s,int port, boolean domachin) throws IOException {
      new PuppetIvyClient(PuppetIvy.this,s,port,domachin);
      return true;
    }
    int getAP() {return getAppPort();}
  }

  static class PuppetIvyClient extends IvyClient {
    PuppetIvyClient(Ivy bus,Socket s,int port,boolean b) throws IOException  { super(bus,s,port,b); }
    protected synchronized void sendBuffer( String s ) throws IvyException {
      super.sendBuffer(s); // and to all the agents on the Ghost bus ? I'm not sure
    }
    protected boolean newParseMsg(String s) throws IvyException {
      return super.newParseMsg(s); // I'm a normal Ivy citizen
    }
  }

  void trace(String s) { System.out.println("Puppet["+id+"] "+s);}

}
