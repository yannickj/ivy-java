/**
 * TestIvySwing, a swing Ivy Java example to probe the Ivy software bus.
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (c) CENA
 *
 * it relies on the Swing toolkit, which is not standard on jdk1.1 platforms,
 * if you don't have swing, your can use TestIvy.
 *
 * New:
 *   1.2.3: use of Vector.addElement instead of add() and the old Properties
 *   model
 * 
 */
import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.* ;
import gnu.getopt.Getopt ;
import fr.dgac.ivy.*;
import java.util.*;

class TestIvySwing extends JPanel  implements IvyApplicationListener {

  public static final String helpmsg = "usage: java TestIvySwing [options]\n\t-b BUS\tspecifies the Ivy bus domain\n\t-q\tquiet, no tty output\n\t-d\tdebug\n\t-h\thelp\n";
  public static final int WIDTH=30;
  public static final int HEIGHT=30;

  private static int index;
  private static int nbTIS=0;
  private String localname;
  private Ivy bus ;
  private String domain;
  private String regexp = "(.*)";
  private JLabel laRegex;
  private JTextArea  ta ;
  private JTextField tfRegex, tfSend ;
  private JButton buApply, buSend, buClear ;
  private JComboBox ports;
  private int regexp_id;
  private REGCB reg;
  private java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("hh:mm:ss");
  private static String[] startDomainList = {
    "127.255.255.255:2010",
    "10.192.36:3110",
    "10.0.0:54321",
    "228.1.2.4:4567",
  };
  private static java.util.Vector domainList;

  static {
    // initialize the domainlist
    domainList = new java.util.Vector();
    for (int i = 0; i<startDomainList.length;i++)
      domainList.addElement(startDomainList[i]);
  }

  Ivy getBus() { return bus; }

  public static void main(String[] args) throws IvyException {
    String domain="127.255.255.255:2010";
    Getopt opt = new Getopt("Counter",args,"b:dhq");
    int c;
    boolean quiet=false;
    while ((c=opt.getopt()) != -1 ) switch(c) {
      case 'q':
	quiet=true;
	break;
      case 'b':
	domain=opt.getOptarg();
	break;
      case 'd':
	Properties sysProp = System.getProperties();
	sysProp.put("IVY_DEBUG","yes");
	break;
      case 'h':
      default:
	System.out.println(helpmsg);
	System.exit(0);
    }
    newTestIvy(domain);
  }

  private TestIvySwing(String domain) throws IvyException {
    super(new BorderLayout());
    this.domain=domain;
    nbTIS++;
    ta = new JTextArea(WIDTH,HEIGHT);
    ta.setEditable(false);
    add(new JScrollPane(ta),BorderLayout.CENTER);
    JPanel p = new JPanel(new BorderLayout());
    p.add(new JLabel("Regex:"),BorderLayout.WEST);
    tfRegex = new JTextField();
    tfRegex.addActionListener(reg=new REGCB());
    p.add(tfRegex,BorderLayout.CENTER);
    p.add(laRegex=new JLabel(regexp),BorderLayout.EAST);
    add(p,BorderLayout.NORTH);
    JPanel p2 = new JPanel(new BorderLayout());
    p2.add(new JLabel("Msg:"),BorderLayout.WEST);
    tfSend = new JTextField("");
    tfSend.addActionListener(new SENDCB());
    p2.add(tfSend,BorderLayout.CENTER);
    p.add(p2,BorderLayout.SOUTH);
    JButton tmpb ;
    (p = new JPanel()).add(tmpb=new JButton("spawn"));
    tmpb.addActionListener(new SPAWN(domain));
    p.add(tmpb=new JButton("clear"));
    tmpb.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ta.setText("");
      }
    });
    ports=new JComboBox();
    ports.setEditable(true);
    int index=0;
    for (java.util.Enumeration e=domainList.elements();e.hasMoreElements();index++) {
      String port = (String) e.nextElement();
      ports.addItem(port);
      if (port == domain ) { ports.setSelectedIndex(index); }
    }
    ports.addActionListener(new ComboCB());
    p.add(ports);
    add(p,BorderLayout.SOUTH);
    //tfRegex.setNextFocusableComponent(tfSend);
    //tfSend.setNextFocusableComponent(tfRegex);
    //tfSend.setRequestFocusEnabled(true);
    localname = "TestIvySwing "+Ivy.libVersion+" ("+index+")";
    index++;
    bus = new Ivy(localname,localname+" ready",this);
    regexp_id = bus.bindMsg(regexp,reg);
    bus.start(domain);
    append( "Ivy Domain: "+ bus.getDomain(domain) );
  }

  public void connect(IvyClient client) {
    append(client.getApplicationName() + " connected " );
  }

  public void disconnect(IvyClient client) {
    append(client.getApplicationName() + " disconnected " );
  }

  public void die(IvyClient client, int id,String msgarg) {
   append(client.getApplicationName() + " die "+ id + " " + msgarg);
  }

  public void directMessage(IvyClient client, int id, String arg) {
   append(client.getApplicationName() + " direct Message "+ id + arg );
  }

  private class ComboCB implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String newDomain=(String)ports.getSelectedItem();
      // if it's the same domain, don't do anything
      if (newDomain == domain) { return; }
      domain=newDomain;
      append( "deconnexion from domain "+ bus.getDomain(null));
      bus.stop();
      try {
        bus.start(newDomain);
	append( "Ivy Domain: "+ newDomain );
      } catch (IvyException ie ) {
        System.err.println("auuuugh "+newDomain);
      }
    }
  } // ComboCB

  private class REGCB implements ActionListener, IvyMessageListener {
    public void actionPerformed(ActionEvent e) {
      try {
        bus.unBindMsg(regexp_id);
      } catch (IvyException ie) {
        System.out.println("big badaboum"); // this cannot happen
      }
      regexp=tfRegex.getText();
      regexp.trim();
      try {
	regexp_id = bus.bindMsg(regexp,this);
	tfRegex.setText("");
	laRegex.setText(regexp);
      } catch (IvyException ie) {
        System.out.println("RE error "+regexp); // this should not happen
      }
    }
    public void receive(IvyClient client, String[] args) {
      String out="client " + client.getApplicationName() + " envoie: [ ";
      for (int i=0;i<args.length;i++) {
        out+=args[i]+ ((i<args.length-1)?" , ":"");
      }
      out+=" ]";
      append(out);
    }
  }

  private class SENDCB implements ActionListener {
    public void actionPerformed(ActionEvent e) {
    int count;
      String tosend = tfSend.getText();
      tfSend.setText("");
      try {
	if ( (count = bus.sendMsg(tosend)) != 0 ) 
	  append("Sending '" + tosend + "' count " + count );
	else
	  append("not Sending '" + tosend + "' nobody cares");
      } catch (IvyException ie ) {
	  append("problem Sending '" + tosend + "'");
      }
    }
  }

  private void append(String s) {
    ta.insert("[" + format.format(new java.util.Date()) + "] "+ s + "\n",0);
  }

  private static void newTestIvy(String domain) throws IvyException {
    TestIvySwing tb = new TestIvySwing(domain);
    JFrame f = new JFrame(tb.localname);
    f.addWindowListener( tb.new WCCB(f,tb)) ;
    f.getContentPane().add(tb, BorderLayout.CENTER);
    f.pack();
    f.setVisible(true);
  }

  private class WCCB extends WindowAdapter {
    private JFrame f;
    public WCCB(JFrame f, TestIvySwing b) { this.f=f; }
    public void windowClosing(WindowEvent e) {
      System.out.println("closing");
      bus.stop();
      f.dispose();
      if (--nbTIS == 0) System.exit(0); // I leave when the last TestIvySwing exits
      System.out.println("closed");
    }
    public void windowActivated(WindowEvent e) {tfSend.requestFocus();}
  }

  private class SPAWN implements ActionListener {
    private String domain;
    public SPAWN(String domain) {this.domain=domain;}
    public void actionPerformed(ActionEvent e) {
      try {
	newTestIvy(domain);
      } catch (IvyException ie) {
	ie.printStackTrace();
      }
    }
  }

}
