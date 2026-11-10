import java.io.*;
import java.net.*;
import java.util.*;

class Multicaster {


  public static final int TIMEOUTLENGTH = 1000;

  public static void main(String[] args) throws Exception {
    String broadcastStr = "224.1.2.3";
    int port = 5555;
    System.out.println("trying addr: " + broadcastStr + ", port: " + port + ", authorized interfaces (TODO not used): " + String.join(", ", getInterfaces()));
    DatagramSocket broadcast = myJoinGroup(broadcastStr, port);
  }

  public static DatagramSocket myJoinGroup(String broadcastStr, int port) throws IOException, SocketException{
    InetAddress mcastAddr = InetAddress.getByName(broadcastStr);
    DatagramSocket broadcast = new MulticastSocket(port);
    if ( ! mcastAddr.isMulticastAddress()) {
      System.out.println("it is *not* a multicastaddress");
      return broadcast;
    }
    System.out.println("it *is* a multicastaddress");
    // InetSocketAddress group = new InetSocketAddress(mcastAddr, port);
    broadcast.setSoTimeout(TIMEOUTLENGTH);
    MulticastSocket s = new MulticastSocket(port);
    // fait la pub sur toutes les interfaces intéressantes
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    for (NetworkInterface _ni: Collections.list(interfaces)) {
	    //NetworkInterface _ni = NetworkInterface.getByName(_if);
	    if (_ni.isLoopback()) continue;
	    // if (_ni.isLoopback()) continue;
	    if (! _ni.isUp()) continue;
	    Enumeration<InetAddress> addresses = _ni.getInetAddresses();
	    for (InetAddress _ia : Collections.list(addresses)) { 
		    if (_ia instanceof Inet6Address) continue;
		    // if (!_ia.isReachable(500)) continue;
		    System.out.println("I try joinGroup for " + _ia + ", if: "+ _ni.getName());
		    s.joinGroup(new InetSocketAddress(mcastAddr,0), _ni);
		    System.out.println("ok");
	    }
    }
    return s;
  } 

  public static String[] getInterfaces() {
	  String[] ret = { "lo0", "en0" };
	  return ret;
  }

}
