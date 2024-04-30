import fr.dgac.ivy.* ;
import gnu.getopt.*;

class NewLine {

  Ivy bus;

  public static void main(String[] args) throws IvyException {
    Getopt opt = new Getopt("NewLine",args,"b:n:");
    String domain=null;
    int c;
    int nb = 10000;
    while ((c = opt.getopt()) != -1) switch (c) {
    case 'b':
      domain=opt.getOptarg();
      break;
    case 'n':
      nb=Integer.parseInt(opt.getOptarg());
      break;
    default:
      System.exit(0);
    }
    new NewLine(domain,nb);
  }

  private int recus = 0;
  private int nbmsg;

  public NewLine(String domain,int n) throws IvyException {
    System.out.println("trying Newline on " + n + " tests");
    nbmsg=n;
    bus = new Ivy("NewLine","NewLine ready", null);
    bus.protectNewlines(true);
    bus.sendToSelf(true);
    bus.bindMsg("^coucou([^m])monde",new IvyMessageListener() {
      public void receive(IvyClient ic,String[] a) {
	recus++;
	if (recus==nbmsg) System.out.println("received "+nbmsg+" ["+a[0]+"]");
      }
    });
    bus.start(domain);
    long t1,t2,t3;
    t1=(new java.util.Date()).getTime();
    System.out.println("sending "+nbmsg+" protected newlines");
    for (int i=0;i<n;i++ ) {
      try {
	bus.sendMsg("coucou\nmonde");
      } catch (IvyException ie) {
	System.out.println("exception raised. Exitting");
	bus.stop();
	System.exit(-1);
      }
    }
    System.out.println("sending "+nbmsg+" unprotected newlines");
    bus.protectNewlines(false);
    recus=0;
    t2=(new java.util.Date()).getTime();
    for (int i=0;i<n;i++ ) bus.sendMsg("coucou monde");
    t3=(new java.util.Date()).getTime();
    System.out.println("with protection " + (t2-t1) +"ms, without "+(t3-t2)+"ms");
    bus.stop();
  }

}
