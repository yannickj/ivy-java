import java.net.*;
import java.io.*;
import gnu.getopt.*;

class Receiver {
  private static final int TIMEOUTLENGTH = 1000;
  private DatagramSocket broadcast;
  private InetAddress group;
  private boolean keeprunning = true;

  Receiver(String broadcastStr, int port)
		  throws IOException, UnknownHostException, InterruptedException
  {
    broadcast = Multicaster.myJoinGroup(broadcastStr, port);
    broadcast.setSoTimeout(TIMEOUTLENGTH);
    traceDebug("PacketReceiver started");
    while (keeprunning) {
      byte[] buf = new byte[256];
      DatagramPacket packet=new DatagramPacket(buf,buf.length);
      try {
        broadcast.receive(packet); 
        parsePacket(packet);
      } catch (SocketTimeoutException _) { }
    };
    traceDebug("PacketReceiver thread stopped");
  }

  private void parsePacket(DatagramPacket dp) {
    System.out.format("received %s from %s port %d\n",
		    new String(dp.getData()),
		    dp.getAddress(),
		    dp.getPort()
		    );
  }

  private void traceDebug(String s){ System.out.println(s); }

  private final static String helpmsg = "Receiver -b broadcast -p port";

  public static void main(String[] args) throws IOException, InterruptedException {
    Getopt opt = new Getopt("Receiver",args,"b:p:");
    int c;
    String broadcast = "127.0.0.255";	// UDP broadcast address
    int port = 5555;  			// UDP port
    while ((c=opt.getopt()) != -1 ) switch(c) {
      case 'b': broadcast = opt.getOptarg(); break;
      case 'p': port = Integer.parseInt(opt.getOptarg()); break;
      default:
	System.out.println(helpmsg);
	System.exit(0);
    }
    new Receiver(broadcast,port);
    System.out.println("done");
  }

}
