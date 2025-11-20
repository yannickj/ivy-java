/**
 * a helper class to implement the Ivy.waitForClient method
 *
 * @see Ivy.waitForClient
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 *  CHANGELOG:
 *  1.2.8:
 *    added a test during the waiting loop
 */

package fr.dgac.ivy ;
import java.util.Map;

class WaiterClient extends WaitFor implements IvyApplicationListener {
  private String name;
  private Map <Integer,IvyClient>clients ;

  WaiterClient(String n,int timeout,Map <Integer,IvyClient>clients) {
    super(timeout);
    this.clients=clients;
    name=n;
    setName("Ivy Waiter thread, for client");
  }

  boolean check() {
    return (received=Ivy.alreadyThere(clients,name)) != null;
  }

  public void connect(fr.dgac.ivy.IvyClient client)  {
    if (name.compareTo(client.getApplicationName())!=0) return;
    received=client;
    interrupt();
  }

  public void disconnect( IvyClient client ) { }
  public void die( IvyClient client, int id, String msgarg) { }
  public void directMessage( IvyClient client, int id,String msgarg ) {}
}
