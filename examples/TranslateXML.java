/**
 * TranslateXML, an Ivy tranlater based on xml rules
 *
 * @author  Yannick Jestin
 * @author  <a href="mailto:jestin@cena.fr"></a>
 *
 * (c) CENA Centre d'Etudes de la Navigation Aerienne
 * this program is LGPL ... etc etc
 *
 * New:
 *   1.2.12
 *     leaving NanoXML in favor to the full featured xpath etc...
 *   1.2.6
 *     get compatible with new Ivy version
 *   1.2.3
 *     use of Vector.addElement instead of add() and the old Properties
 *     model
 *
 */
import fr.dgac.ivy.* ;
import gnu.getopt.Getopt;
import java.io.* ;
import java.util.*;
import javax.xml.parsers.*; 
import javax.xml.xpath.*; 
import org.xml.sax.*;  
import org.w3c.dom.*;


class TranslateXML {

    private Ivy bus;

    TranslateXML(String domain, String name, String filename) throws IvyException {
      bus = new Ivy(name,"Hello le monde",null);
      parseFile(filename);
      bus.bindMsg("^Bye$",new IvyMessageListener() {
	// callback for "Bye" message
	public void receive(IvyClient client, String[] args) {System.exit(0);}
      });
      try {
	// starts the bus on the default domain or IVY_DOMAIN property
	bus.start(domain);
      } catch (IvyException ie) {
	System.err.println("can't run the Ivy bus" + ie.getMessage());
      }
    }

    private void parseFile(String filename) {
      try {
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder = factory.newDocumentBuilder();
	Document trans = builder.parse(new File(filename));
	XPath xpath = XPathFactory.newInstance().newXPath();
	NodeList list = (NodeList)xpath.evaluate("/translations/translate",trans,XPathConstants.NODESET);
	for (int i=0;i<list.getLength();i++) {
	  NamedNodeMap nnm = list.item(i).getAttributes();
	  String from = nnm.getNamedItem("from").getNodeValue();
	  String to = nnm.getNamedItem("to").getNodeValue();
	  if ((from!=null)&&(to!=null)&&(!from.equals(to))) {
	    System.out.println("translating every \""+from+"\" in \""+to+"\"");
	    bus.bindMsg(from,new TALK(to));
	  }
	}
      } catch (Exception e) {
	e.printStackTrace();
	System.exit(-1);
      }
    }

    private class TALK implements IvyMessageListener {
      private String go;
      TALK(String s) {go=s;}
      public void receive(IvyClient client, String[] args) {
	try { bus.sendMsg(go); } catch (IvyException ie) {
	  System.out.println(" can't send " + go +" on the Ivy bus");
	}
      }
    }

    // callback associated to the "Hello" messages"
    public void receive(IvyClient client, String[] args) {
      try {
	bus.sendMsg("Bonjour"+((args.length>0)?args[0]:""));
      } catch (IvyException ie) {
      }
    }

    public static final String helpmsg = "usage: java TranslateXML [options]\n\t-f filename.xml\tspecifies the XML file with tranlations\n\t-b BUS\tspecifies the Ivy bus domain (can be overriden by XML file)\n\t-n ivyname (default TranslateXML)\n\t-d\tdebug\n\t-h\thelp\n";

    public static void main(String args[]) throws IvyException {
      Getopt opt = new Getopt("TranslateXML",args,"f:n:b:dht");
      int c;
      String domain=Ivy.getDomain(null);
      String name="TranslateXML";
      String filename="translation.xml";
      while ((c = opt.getopt()) != -1) switch (c) {
	case 'f':
	  filename=opt.getOptarg();
	  break;
	case 'b':
	  domain=opt.getOptarg();
	  break;
	case 'n':
	  name=opt.getOptarg();
	  break;
	case 'd':
	  java.util.Properties sysProp = System.getProperties();
	  sysProp.put("IVY_DEBUG","yes");
	  break;
	case 'h':
	default:
	  System.out.println(helpmsg);
	  System.exit(0);
      } // getopt
      new TranslateXML(domain,name,filename);
    }
}
