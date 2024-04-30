/**
 * Translate, an Ivy java program sample.
 *
 * @author Yannick Jestin <jestin@cena.fr>
 * 
 * (c) CENA
 *
 * 1.2.6
 *  - goes apache jakarta regexp
 *
 */
import fr.dgac.ivy.* ;
import java.io.* ;
import org.apache.regexp.* ;

class Translate {

    private Ivy bus;

    Translate(String filename) throws IvyException {
      bus = new Ivy("Translater","Hello le monde",null);
      parseFile(filename);
      bus.bindMsg("^Bye$",new IvyMessageListener() {
	// callback for "Bye" message
	public void receive(IvyClient client, String[] args) {System.exit(0);}
      });
      try {
	// starts the bus on the default domain or IVY_DOMAIN property
	bus.start(null);
      } catch (IvyException ie) {
	System.err.println("can't run the Ivy bus" + ie.getMessage());
      }
    }

    private void parseFile(String filename) {
      try {
	BufferedReader in = new BufferedReader(new FileReader(new File(filename)));
	String s;
	RE re = new RE("\"([^\"]*)\" \"([^\"]*)\"");
	while ( (s=in.readLine()) != null ) {
	  if (re.match(s)) {
	    System.out.println("binding " +re.getParen(1)+" and translating to " + re.getParen(2));
	    try {
	      bus.bindMsg(re.getParen(1),new TALK(re.getParen(2)));
	    } catch (IvyException ie) {
	      System.out.println(re.getParen(1)+" is not a valid PCRE regex");
	    }
	  }
	}
	in.close();
      } catch (FileNotFoundException fnfe) {
	System.out.println("file "+filename+" not found. Good bye !");
	System.exit(-1);
      } catch (IOException ioe) {
	System.out.println("error reading "+filename+". Good bye !");
	System.exit(-1);
      }
    }

    private class TALK implements IvyMessageListener {
      private String go;
      TALK(String s) {go=s;}
      public void receive(IvyClient client, String[] args) {
	try {
	  bus.sendMsg(go);
	} catch (IvyException ie) {
	}
      }
    }

    public void receive(IvyClient client, String[] args) {
      try {
	bus.sendMsg("Bonjour"+((args.length>0)?args[0]:""));
      } catch (IvyException ie) {
      }
    }

    public static void main(String args[]) throws IvyException {
      new Translate("translation.txt");
    }
}
