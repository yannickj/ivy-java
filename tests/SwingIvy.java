/**
 * Ivy java library API tester.
 *
 * @author  Yannick Jestin <mailto:yannick.jestin@enac.fr>
 *
 * (c) ENAC
 *
 */
import fr.dgac.ivy.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SwingIvy implements Runnable {

  private Ivy bus;
  private final static int DODO = 2000;
  private final static String BLAH = "ivy blah blah blah";
  private final String[] data = {"one","two","three","four"};

  DefaultListModel model = new DefaultListModel();
  JFrame f = new JFrame("Test Ivy Swing");
  JSlider scale = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
  JTextArea text = new JTextArea("type anything inside this area");
  JToggleButton startstop = new JToggleButton("trigger ivy messages");
  JList list = new JList(model);
  volatile Thread runThread = null;
  boolean doSend = false;

  public SwingIvy(String domain)  {
    int index=0;
    for (String s : data) { model.add(index++,s); }
    f.getContentPane().add( scale, BorderLayout.PAGE_START );
    f.getContentPane().add( text, BorderLayout.CENTER );
    f.getContentPane().add( list, BorderLayout.LINE_END );
    f.getContentPane().add( startstop, BorderLayout.PAGE_END );
    f.addWindowListener(new WindowAdapter(){
      public void windowClosing(WindowEvent e){
	System.out.println("closing gracefully");
	f.dispose();
	bus.stop();
	Thread t = runThread;
	runThread = null;
	//if (t!=null) t.interrupt();
	t.interrupt();
      }
    });
    text.setRows(25);
    text.setColumns(40);
    startstop.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e){
	doSend = startstop.isSelected();
      }
    });
    f.pack();
    f.setVisible(true);
    runThread = new Thread(this);
    runThread.start();
    try {
      bus = new Ivy("SwingIvy",null, null);
      bus.bindAsyncMsg("^AddList (.*)", new IvyMessageListener() {
	public void receive(IvyClient c,String[] args) {
	  //System.out.println("SetText received");
	  model.add(model.getSize() , args[0]);
	}
      }, BindType.SWING);
      bus.bindAsyncMsg("^SetText (.*)", new IvyMessageListener() {
	public void receive(IvyClient c,String[] args) {
	  //System.out.println("SetText received");
	  text.append(args[0]+"\n");
	}
      }, BindType.SWING);
      bus.bindAsyncMsg("^SetRange ([0-9]+)", new IvyMessageListener() {
	public void receive(IvyClient c,String[] args) {
	  int i = Integer.parseInt(args[0]);
	  scale.setValue(i);
	  //System.out.println("SetRange received: "+i);
	}
      }, BindType.SWING);
      bus.sendToSelf(true);
      bus.start(domain);
    } catch (IvyException ie) {
      ie.printStackTrace();
    }
  }

  public void run() {
    int intRange=0;
    Thread thisThread=Thread.currentThread();
    while(runThread ==thisThread) {
      try {
	Thread.sleep(DODO);
	intRange++;
	if (doSend) {
	  if (intRange>99) intRange=0;
	  bus.sendMsg("SetRange "+intRange);
	  bus.sendMsg("SetText "+BLAH);
	  bus.sendMsg("AddList "+intRange);
	}
      } catch (IvyException e) {
	e.printStackTrace();
      } catch (InterruptedException e) {
	if (thisThread!=runThread) { break ;}
      }   
    }
  }

  public static void main(String[] args) throws IvyException {
    new SwingIvy(Ivy.getDomainArgs("SwingIvy",args));
  }

}
