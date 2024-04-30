/**
 * TestIvy, an AWT Ivy Java program example.
 *
 * toy tool to probe the Ivy software bus it relies on the AWT, and is less useable than
 * TestIvySwing, which should be preferred if swing is on your JDK.
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 * @see		fr.dgac.ivy.TestIvySwing
 *
 * (c) CENA
 *
 */
import java.awt.* ;
import java.awt.event.* ;
import fr.dgac.ivy.*;

class TestIvy extends Frame implements IvyApplicationListener,IvyMessageListener { 
  public static String DEFAULTREGEXP = "(.*)";
  private Ivy bus ;
  private String regexp="";
  private Label tfBound;
  private TextField tfRegex, tfSend ;
  private TextArea  ta ;
  private Button buApply, buSend, buClear ;
  private java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("hh:mm:ss");
  private int regexp_id;

  public TestIvy() throws IvyException {
    addWindowListener( new WindowAdapter() { public void windowClosing(WindowEvent e) { System.exit(0); } });
    setLayout(new BorderLayout());
    ta = new TextArea();
    ta.setEditable(false);
    add(ta,BorderLayout.CENTER);
    Panel p = new Panel(new BorderLayout());
    add(p,BorderLayout.NORTH);
    p.add(tfBound=new Label("Boung to: "+DEFAULTREGEXP),BorderLayout.WEST);
    tfRegex = new TextField(regexp);
    tfRegex.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  try { bus.unBindMsg(regexp_id); } catch (IvyException ie) {
	    System.out.println("Big badaboum"); // this should not happen
	    System.exit(0);
	  }
	  regexp=tfRegex.getText();
	  tfBound.setText("Bound to: " +regexp);
	  regexp.trim();
	  try { regexp_id = bus.bindMsg(regexp,TestIvy.this); } catch (IvyException ie) { }
	  tfRegex.setText("");
	  pack();
	}
    });
    p.add(tfRegex,BorderLayout.CENTER);
    p = new Panel(new BorderLayout());
    p.add(new Label("Msg:"),BorderLayout.WEST);
    tfSend = new TextField("");
    tfSend.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	int count;
	String tosend = tfSend.getText();
	tfSend.setText("");
	try { count = bus.sendMsg(tosend);
	  append("Sending '" + tosend + "' count " + count );
	} catch (IvyException ie) {
	  append("*Error* cant't send " + tosend );
	}
      }
    });
    p.add(tfSend,BorderLayout.CENTER);
    add(p,BorderLayout.SOUTH);
    bus = new Ivy("JAVATESTBUS","Testbus is ready",this);
    regexp_id = bus.bindMsg(DEFAULTREGEXP,this);
    bus.start(Ivy.getDomain(null));
    append( "Ivy Domain: "+ bus.getDomain(null) );
  }

  public static void main(String[] args) throws IvyException {
    TestIvy tb = new TestIvy();
    tb.pack();
    tb.setVisible(true);
  }

  public void connect(IvyClient client) { append(client.getApplicationName() + " connected " ); }
  public void disconnect(IvyClient client) { append(client.getApplicationName() + " disconnected " ); }
  public void die(IvyClient client, int id,String msgarg) { System.exit(0); }
  public void directMessage(IvyClient client, int id, String arg) {
    append(client.getApplicationName() + " direct Message "+ id + arg );
  }

  public void receive(IvyClient client, String[] args) {
    String out="client " + client.getApplicationName() + " envoie: [ ";
    for (int i=0;i<args.length;i++) out+=args[i]+ ((i<args.length-1)?" , ":"");
    out+= " ]";
    append(out);
  }

  private void append(String s) { ta.append("[" + format.format(new java.util.Date()) + "] "+ s + "\n"); }

}
