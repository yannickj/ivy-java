/**
 * Ivy software bus tester
 *
 * @author	Yannick Jestin <jestin@cena.fr>
 *
 * (c) CENA 1998-2004
 *
 *  CHANGELOG
 *
 *  1.2.6
 *    goes jakarta regexp
 */

import java.lang.Thread;
import java.net.*;
import java.io.*;
import java.util.regex.*;
import gnu.getopt.*;

class TestNet implements Runnable {
  private String mydomain;
  private boolean watcherrunning = false;
  private boolean isMulticastAddress = false;
  private Thread broadcastListener ;
  private DatagramSocket broadcast;	/* supervision socket */
    // it can also be a MulticastSocket, which inherits from the previous
  
  public void run()  {
    byte buf[] = new byte[256];
    DatagramPacket packet=new DatagramPacket(buf, 256);
    int port;
    String s = "Server waiting for Broadcast on "+mydomain;
    s+=(isMulticastAddress)?" (TCP multicast)":" (UDP broadcast)"; 
    System.out.println(s);
    while( watcherrunning ) try {
	broadcast.receive(packet);
	String msg = new String(buf) ;
	// clean up the buffer after each message
	for (int i=0;i<buf.length;i++) { buf[i]=0; }
	InetAddress remotehost = packet.getAddress();
	System.out.println("Server Receive Broadcast from "+remotehost.getHostName()+":"+packet.getPort()+" ("+msg.length()+") ["+msg+"]");
	if (msg.charAt(0)=='x') {
	  watcherrunning = false;
	  System.out.println("I leave");
	}
    } catch (java.io.InterruptedIOException jii ){
      if (!watcherrunning) break;
    } catch (java.io.IOException ioe ){
     System.err.println("IvyWatcher IOException "+ ioe.getMessage() );
    }
    broadcast.close();
    System.out.println("Server normal shutdown");
  }
  
  void stop() { watcherrunning=false; }

  private static String getDomain(String net) {
    int sep_index = net.lastIndexOf( ":" );
    if ( sep_index != -1 ) { net = net.substring(0,sep_index); }
    net += ".255.255.255";
    Pattern exp = Pattern.compile( "^(\\d+\\.\\d+\\.\\d+\\.\\d+).*");
    return exp.matcher(net).group(1);
  }

  private static int getPort(String net) {
    int port;
    int sep_index = net.lastIndexOf( ":" );
    if ( sep_index == -1 ) {
      port = -1;
    } else { 
      port = Integer.parseInt( net.substring( sep_index +1 ));
    }
    //System.out.println("port: "+port);
    return port;
  }

  private static void send(String data, String net) {
    int port = getPort(net);
    net=getDomain(net);
    try {
      DatagramSocket send;
      InetAddress group = InetAddress.getByName(net);
      send = new MulticastSocket(port);
      if (group.isMulticastAddress()) { ((MulticastSocket)send).joinGroup(group); }
      DatagramPacket packet = new DatagramPacket(
	  data.getBytes(),
	  data.length(),
	  group,
	  send.getLocalPort() );
      System.out.println("Client sends Broadcast to "+net+":"+port+" ("+packet.getLength()+") ["+data+"]");
      send.send(packet);
    } catch ( UnknownHostException e ) {
      System.out.println("Broadcast sent on unknown network "+ e.getMessage());
    } catch ( IOException e ) {
      System.out.println("Broadcast error " + e.getMessage() );
    }
  }

  void start(String domain) {
    String domainaddr=getDomain(domain);
    int port=getPort(domain);
    mydomain=domainaddr+":"+port;
    try {
      InetAddress group = InetAddress.getByName(domainaddr);
      if (group.isMulticastAddress()) {
        isMulticastAddress = true;
        broadcast = new MulticastSocket(port ); // create the UDP socket
	((MulticastSocket)broadcast).joinGroup(group);
      } else {
        broadcast = new MulticastSocket(port ); // create the UDP socket
      }
    } catch ( IOException e ) {
      System.out.println("MulticastSocket I/O error" + e );
      return;
    } 
    try {
      broadcast.setSoTimeout(100);
    } catch ( java.net.SocketException jns ) {
      System.out.println("IvyWatcher setSoTimeout error" + jns.getMessage() );
    }
    // starts a Thread listening on the socket
    watcherrunning=true;
    broadcastListener = new Thread(this);
    broadcastListener.start();
  }

  public static final String helpmsg = "usage: java TestNet [options]\n\t-b BUS\tspecifies the Ivy bus domain\n\t-s\tclient mode (default)\n\t-s\tserver mode\n\t-h\thelp\n\n";
  public static void main(String[] args) {
    Getopt opt = new Getopt("TestNet",args,"b:csh");
    String domain = "228.0.0.0:4567";
    boolean server=false;
    int c;
    while ((c = opt.getopt()) != -1) switch (c) {
    case 'b':
      domain=opt.getOptarg();
      break;
    case 'c':
      server=false;
      break;
    case 's':
      server=true;
      break;
    case 'h':
    default:
	System.out.println(helpmsg);
	System.exit(0);
    } // getopt

    if (server) {
      TestNet s = new TestNet();
      s.start(domain);
    } else {
      TestNet.send("coucou1",domain);
      TestNet.send("coucou2",domain);
      TestNet.send("x",domain);
    }
  }

} // class TestNet
/* EOF */
