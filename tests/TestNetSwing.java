/**
 * TestNetSwing , a network Ivy domain checker.
 * @author Yannick Jestin <mailto:jestin@cena.fr>
 *
 * (c) CENA
 * A simple Swing application in order to check a network
 *
 * Changelog:
 * 1.2.8
 *   - apache regexp instead of gnu regexp
 */
import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import fr.dgac.ivy.*;
import java.util.regex.*;
import gnu.getopt.*;

class TestNetSwing implements Runnable {

  private JFrame frame;
  private JLabel receive;
  private JButton send;
  private int alpha=0;
  private Color color;
  private static int FADINGSTEP = 20;
  private static int FADINGSLEEP = 100;
  private static int DEFAULT_RED=200;
  private static int DEFAULT_GREEN=10;
  private static int DEFAULT_BLUE=10;
  private static int DEFAULT_ALPHA=255;
  private static Color DEFAULT_COLOR = 
      new Color(DEFAULT_RED,DEFAULT_GREEN,DEFAULT_BLUE,DEFAULT_ALPHA);

  private Ageing age;
  private String domainAddr="none";
  private int port=0;
  private int serial=1;
  private boolean watcherrunning = false;
  private volatile Thread watcherThread;
  private boolean isMulticastAddress = false;
  private Thread broadcastListener ;
  private DatagramSocket broadcast;	/* supervision socket */
    // it can also be a MulticastSocket, which inherits from the previous

  public TestNetSwing(String da,int p) {
    this.domainAddr=da;
    this.port=p;
    createBroadcastListener();
    frame = new JFrame("TestNet "+domainAddr+":"+port);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Container cp = frame.getContentPane();
    cp.setLayout(new GridLayout(0,1));
    cp.add(send = new JButton("send packet to "+domainAddr+":"+port));
    send.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        send("hello",domainAddr,port);
      }
    });
    cp.add(receive = new JLabel(""));
    boolean sel = false;
    frame.pack();
    age=new Ageing();
    frame.setVisible(true);
  }
  
  public void run() {
    byte buf[] = new byte[256];
    Thread thisThread=Thread.currentThread();
    for (int i=0;i<buf.length;i++) { buf[i]=10; }
    DatagramPacket packet=new DatagramPacket(buf, 256);
    String s = "Server waiting for Broadcast on "+domainAddr+":"+port;
    s+=(isMulticastAddress)?" (TCP multicast)":" (UDP broadcast)"; 
    System.out.println(s);
    while( watcherThread==thisThread) try {
	broadcast.receive(packet);
	String msg = new String(buf) ;
	// clean up the buffer after each message
	for (int i=0;i<buf.length;i++) { buf[i]=10; }
	InetAddress remotehost = packet.getAddress();
	updateLabel("("+(serial++)+") "+ remotehost.getHostName()+":"+msg);
    } catch (java.io.InterruptedIOException jii ){
      if (watcherThread!=thisThread) break;
    } catch (java.io.IOException ioe ){
     System.err.println("IvyWatcher IOException "+ ioe.getMessage() );
    }
    broadcast.close();
    System.out.println("Server normal shutdown");
  }
  
  synchronized void stop() {
    Thread t = watcherThread;
    watcherThread = null;
    if (t!=null) { t.interrupt(); }
  }

  private void updateLabel(String text) {
    if (text==null) return;
    int i = text.indexOf(10);
    if (i>0) { receive.setText(text.substring(0,i)); }
    else { receive.setText(text); }
    alpha=DEFAULT_ALPHA;
    color=DEFAULT_COLOR;
    receive.setForeground(color);
  }

  private static void send(String data, String domain,int port) {
    try {
      DatagramSocket send;
      InetAddress group = InetAddress.getByName(domain);
      send = new MulticastSocket(port);
      if (group.isMulticastAddress()) { ((MulticastSocket)send).joinGroup(group); }
      DatagramPacket packet = new DatagramPacket(
	  data.getBytes(),
	  data.length(),
	  group,
	  send.getLocalPort() );
      //System.out.println("Client sends Broadcast to "+net+":"+port+" ("+packet.getLength()+") ["+data+"]");
      send.send(packet);
    } catch ( UnknownHostException e ) {
      System.out.println("Broadcast sent on unknown network "+ e.getMessage());
    } catch ( IOException e ) {
      System.out.println("Broadcast error " + e.getMessage() );
    }
  }

  private void createBroadcastListener() {
    try {
      InetAddress group = InetAddress.getByName(domainAddr);
      if (group.isMulticastAddress()) {
        isMulticastAddress = true;
        broadcast = new MulticastSocket(port); // create the UDP socket
	((MulticastSocket)broadcast).joinGroup(group);
      } else {
        broadcast = new MulticastSocket(port); // create the UDP socket
      }
    } catch ( IOException e ) {
      System.out.println("MulticastSocket I/O error" + e );
      e.printStackTrace();
      System.exit(0);
    } 
    watcherThread = new Thread(this); 
    watcherThread.start();
  }

  private class Ageing implements Runnable {
    boolean encore = true;
    public Ageing() {
      new Thread(this).start();
    }
    public void run() {
      while (encore) {
	try {
	  java.lang.Thread.sleep(FADINGSLEEP);
	} catch (InterruptedException ie) {
	}
	alpha-=FADINGSTEP;
	String s = receive.getText();
	if (alpha<0) {
	  alpha=0;
	  receive.setText(null);
	  s=null;
	}
	if (s!=null) { receive.setForeground(new Color(DEFAULT_RED,DEFAULT_GREEN,DEFAULT_BLUE,alpha)); }
      }
    }
    public void stop(){ encore=false; }
  }

  /*
   * copied verbatim from IvyWatcher , I should put TestNetSwing *in* the
   * distribution itself ...
   */
  static String getDomain(String net) throws IvyException {
    // System.out.println("debug: net=[" + net+ "]");
    int sep_index = net.lastIndexOf( ":" );
    if ( sep_index != -1 ) { net = net.substring(0,sep_index); }
    try {
      Pattern numbersPoint = Pattern.compile("([0-9]|\\.)+");
      if (!numbersPoint.matcher(net).matches()) {
	// traceDebug("should only have numbers and point ? I won't add anything... " + net);
	return net;
      }
      net += ".255.255.255";
      Pattern exp = Pattern.compile( "^(\\d+\\.\\d+\\.\\d+\\.\\d+).*");
      Matcher m = exp.matcher(net);
      if (!m.matches()) {
	System.out.println("Bad broascat addr " + net);
	throw new IvyException("bad broadcast addr");
      }
      net=m.group(1);
    } catch ( PatternSyntaxException e ){
      System.out.println(e);
      System.exit(0);
    }
    // System.out.println("debug: returning net=[" + net+ "]");
    return net;
  }

  static int getPort(String net) { // returns 0 if no port is set
    int sep_index = net.lastIndexOf( ":" );
    int port= ( sep_index == -1 ) ? 0 :Integer.parseInt( net.substring( sep_index +1 ));
    // System.out.println("net: ["+net+"]\nsep_index: "+sep_index+"\nport: "+port);
    return port;
  }


  public static final String helpmsg = "usage: java TestNetSwing [options]\n\t-b BUS\tspecifies the Ivy bus domain\n\t-h\thelp\n\n";
  public static void main(String[] args) throws IvyException {
    Getopt opt = new Getopt("TestNetSwing",args,"b:h");
    int c;
    String domainstring = null;
    while ((c = opt.getopt()) != -1) switch (c) {
    case 'b':
      domainstring=opt.getOptarg();
      break;
    case 'h':
    default:
	System.out.println(helpmsg);
	System.exit(0);
    } // getopt
    new TestNetSwing(getDomain(domainstring),getPort(domainstring));
  }

} // class TestNetSwing
/* EOF */
