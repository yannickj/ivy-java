/**
 * a helper class to implement "Wait for Message/Client" 
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 *  CHANGELOG:
 *  1.2.16:
 *    - factorize code from Waiter and WaiterClient
 */

package fr.dgac.ivy ;

abstract class WaitFor implements Runnable {
  private static final int INCREMENT = 100;
  int timeout;
  IvyClient received=null;
  boolean forever=false;
  private Thread t;

  void setName(String s) { t.setName(s); }
  void interrupt() { t.interrupt(); }

  WaitFor(int timeout) {
    this.timeout=timeout;
    if (timeout<=0) forever=true;
    t=new Thread(this);
  }

  public IvyClient waitFor() {
    t.start();
    try { t.join(); } catch (InterruptedException ie) { return null; }
    return received;
  }

  public void run() {
    boolean encore=true;
    // System.out.println("DEV Waiter start");
    while (encore) {
      try {
        if (INCREMENT>0) Thread.sleep(INCREMENT);
        if (!forever) {
          timeout-=INCREMENT;
          if (timeout<=0) encore=false;
        }
      } catch (InterruptedException ie) {
        break;
      }
      if (check()) break;
    }
    // System.out.println("DEV Waiter stop");
  }

  abstract boolean check(); // is called in the thread, leaves if true

}
