/**
 * our handle for our own agent on the bus
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 * @since 1.2.4
 *
 * the other agents are accessed throung IvyClient
 * @see IvyClient
 *
 *  CHANGELOG:
 *  1.2.16
 *    - now uses the synchronized wrappers of the Java API for all collections
 *  1.2.14
 *    - uses autoboxing for Boolean
 *    - switch from gnu regexp (deprecated) to the built in java regexp
 *    - add generic types to declarations
 *  1.2.7:
 *    - fixes a bug on unbindMsg(String) ( closes Matthieu's burreport )
 *  1.2.6:
 *    - jakarta regexp are not threadsafe, adding extra synch blocks
 *  1.2.5:
 *    - uses apache regexp instead of gnu regexp
 *  1.2.4:
 *    - adds a the threaded option for callbacks
 *    - Matthieu's bugreport on unBindMsg()
 */

package fr.dgac.ivy ;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.regex.*;

public class SelfIvyClient extends IvyClient {
    
  private Ivy bus;
  private static int serial=0;		/* an unique ID for each regexp */
  private Map<Integer,IvyMessageListener> callbacks=
    Collections.synchronizedMap(new HashMap<Integer,IvyMessageListener>());
  private Map<Integer,BindType> threadedFlag=
    Collections.synchronizedMap(new HashMap<Integer,BindType>());

  public void sendDirectMsg(int id,String message) {
    bus.directMessage(this,id,message);
  }
  public void sendDie(String message) { bus.dieReceived(this,0,message); }

  protected SelfIvyClient(Ivy bus,String appName) {
    this.bus=bus;
    // this.protocol=Ivy.PROTOCOLVERSION;
    this.appName=appName;
  }

  protected int bindMsg(String sregexp, IvyMessageListener callback, BindType type ) throws IvyException  {
    // creates a new binding (regexp,callback)
    try {
      Pattern re=Pattern.compile(sregexp,Pattern.DOTALL);
      Integer key = serial++;
      regexps.put(key,re);
      regexpsText.put(key,sregexp);
      synchronized (callbacks) {
	callbacks.put(key,callback);
      }
      synchronized (threadedFlag) {
	threadedFlag.put(key,type); // use autoboxing of boolean
      }
      return key.intValue();
    } catch (PatternSyntaxException ree) {
      throw new IvyException("Invalid regexp " + sregexp);
    }
  }

  protected synchronized void unBindMsg(int id) throws IvyException {
    Integer key = id;
    synchronized (regexps) { synchronized (callbacks) { synchronized (threadedFlag) {
      if ( ( regexps.remove(key) == null )
	  || (regexpsText.remove(key) == null )
	  || (callbacks.remove(key) == null )
	  || (threadedFlag.remove(key) == null )
	 )
	throw new IvyException("client wants to remove an unexistant regexp "+id);
    } } }
  }

  // unbinds to the first regexp
  protected synchronized boolean unBindMsg(String re) {
    synchronized (regexpsText) {
      if (!regexpsText.containsValue(re)) return false;
      for (Map.Entry<Integer,String> me :  regexpsText.entrySet()) {
	if ( me.getValue().equals(re) ) {
	  try {
	    bus.unBindMsg(me.getKey().intValue());
	  } catch (IvyException ie) {
	    return false;
	  }
	  return true;
	}
      }
    }
    return false;
  }

  protected int sendSelfMsg(String message) {
    int count = 0;
    traceDebug("trying to send to self the message <"+message+">");
    for (Integer key : regexps.keySet() ) {
      Pattern regexp = regexps.get(key);
      String sre = regexpsText.get(key);
      synchronized(regexp) {
	traceDebug("checking against: "+sre);
	Matcher m = regexp.matcher(message);
	if (!m.matches()) {
	  traceDebug("checking against: "+sre+" failed");
	  continue;
	}
	traceDebug("checking against: "+sre+" succeeded");
	count++;
	callCallback(this,key,toArgs(m));
      }
    }
    return count;
  }

  protected void callCallback(IvyClient client, Integer key, String[] tab) {
    IvyMessageListener callback;
    synchronized (callbacks) {
      callback=callbacks.get(key);
    }
    if (callback==null) {
      traceDebug("Not regexp matching id "+key.intValue()+", it must have been unsubscribed concurrently");
      return; 
      // DONE check that nasty synchro issue, test suite: Request
    }
    BindType type = threadedFlag.get(key);
    switch (type) {
     case NORMAL:
       // runs the callback in the same thread
       callback.receive(client, tab); // can tab can be faulty ?! TODO
       break;
     case ASYNC:
       // starts a new Thread for each callback ... ( Async API )
       new Runner(callback,client,tab);
       break;
     case SWING:
       // deferes the callback to the Event Dispatch Thread
       new SwingRunner(callback,client,tab);
       break;
    }
  }

  private String[] toArgs(Matcher m) {
    String[] args=
      (m.groupCount()>0) ? new String[m.groupCount()] : new String[0];
    //System.out.println("DEBUG "+args.length+" arguments");
    for(int sub=0;sub<m.groupCount();sub++) {
      args[sub]=m.group(sub+1);
      if (bus.getProtectNewlines()) args[sub]=decode(args[sub]);
      //System.out.println("DEBUG argument "+(sub)+"="+args[sub]);
    }
    return args;
  }

  public String toString() {
    return "IvyClient (ourself)"+bus.getAppName()+":"+appName;
  }

  // a class to perform the execution of each new callback within the Event
  // Dispatch Thread
  // this is an experimental feature introduced in 1.2.14
  static class SwingRunner implements Runnable {
    IvyMessageListener cb;
    IvyClient c;
    String[] args;
    public SwingRunner(IvyMessageListener cb,IvyClient c,String[] a) {
      this.cb=cb;
      this.c=c;
      args=a;
      javax.swing.SwingUtilities.invokeLater(SwingRunner.this);
    }
    public void run() { cb.receive(c,args); }
  }

  // a class to perform the threaded execution of each new message
  // this is an experimental feature introduced in 1.2.4
  class Runner implements Runnable {
    IvyMessageListener cb;
    IvyClient c;
    String[] args;
    private Thread t;
    public Runner(IvyMessageListener cb,IvyClient c,String[] a) {
      this.cb=cb;
      this.c=c;
      args=a;
      //t=new Thread(Runner.this);
      //bus.registerThread(t);
      //t.start();
      bus.getPool().execute(Runner.this);
      //bus.unRegisterThread(t);
    }
    public void run() {
      Thread.currentThread().setName("Ivy Runner Thread to execute an async callback");
      cb.receive(c,args);
    }
  } // class Runner

  private void traceDebug(String s){
    if (debug)
    System.out.println("-->SelfIvyClient "+bus.getAppName()+":"+appName+"<-- "+s);
  }

}
