import java.net.*;
import java.io.*;
import gnu.getopt.*;

class Sender {
  private static final int TIMEOUTLENGTH = 1000;
  private static final String MESSAGE = "toto";
  private DatagramSocket broadcast;
  private int nmessages;

  Sender(String broadcastStr, int port, int delay, int n) throws IOException, UnknownHostException, InterruptedException {
    nmessages = n;
    broadcast = Multicaster.myJoinGroup(broadcastStr, port);
    broadcast.setSoTimeout(TIMEOUTLENGTH);
    InetAddress group = InetAddress.getByName(broadcastStr);
    Runnable r = () -> {
      traceDebug("PacketSender thread started");
      Thread.currentThread().setName("UDP Packet sender");
      while (nmessages>0) {
        try {
	  String message = MESSAGE + nmessages;
	  DatagramPacket packet=new DatagramPacket(message.getBytes(), message.length(), group, port);
	  broadcast.send(packet);
	  nmessages--;
	  Thread.sleep(delay);
	} catch (InterruptedException e) {
		// i don't care
	} catch (InterruptedIOException e) {
	  e.printStackTrace(); traceDebug("IO interrupted during the broadcast. Do nothing");
	} catch ( IOException e ) {
	  System.out.println("Broadcast Error " + e.getMessage()+" continuing anyway");
	  e.printStackTrace();
	}
      }
      traceDebug("PacketSender thread stopped");
    };
    Thread.startVirtualThread(r).join();
  }

  private void traceDebug(String s){ System.out.println(s); }

  private final static String helpmsg = "Sender -b broadcast -p port -n numberOfPackets -d delayMsBetweenPackets";

  public static void main(String[] args) throws IOException, InterruptedException {
    Getopt opt = new Getopt("Sender",args,"b:p:n:d:");
    int c;
    int n = 100;			// number of packets to send
    String broadcastStr = "127.0.0.255";	// UDP broadcast address
    int port = 5555;  			// UDP port
    int delay = 1000;			// 1 second delay
    while ((c=opt.getopt()) != -1 ) switch(c) {
      case 'b': broadcastStr = opt.getOptarg(); break;
      case 'p': port = Integer.parseInt(opt.getOptarg()); break;
      case 'n': n = Integer.parseInt(opt.getOptarg()); break;
      case 'd': delay = Integer.parseInt(opt.getOptarg()); break;
      default:
	System.out.println(helpmsg);
	System.exit(0);
    }
    new Sender(broadcastStr,port,delay,n);
    System.out.println("done");
  }

}
