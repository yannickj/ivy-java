/**
 * terminal implementation in java of the ivyprobe.
 *
 * @author	Yannick Jestin
 * @author	<a href="http://www.tls.cena.fr/products/ivy/">http://www.tls.cena.fr/products/ivy/</a>
 *
 * (c) CENA
 *
 * Changelog:
 * 1.2.16
 *    - now uses the synchronized wrappers of the Java API for all collections
 *    - gets read of Thread mumbo jumbo, and no more System.exit() on die, we
 *    prefer System.in.close() !
 * 1.2.14
 *   - uses the "new" for: loop construct of Java5
 *   - throws RuntimeException instead of System.exit(), allows code reuse
 *   - switch from gnu regexp (deprecated) to the built in java regexp
 * 1.2.13
 *   - adds support for RESyntaxException
 * 1.2.12
 *   - .ping is back
 * 1.2.9
 *   - added the .time command
 *   - added the .bound *
 * 1.2.8
 *   - added a fr.dgac.ivy.tools subsection
 * 1.2.7
 *   - added a .where function
 *   - added a .bound function
 *   - added a .dieall-yes-i-am-sure function
 * 1.2.6
 *   - no more java ping
 * 1.2.5
 *   - uses apache regexp instead of gnu regexp
 * 1.2.4
 *   - now uses the bindListener paradigm to display the binding/unbinding dynamically
 *   - adds the -s (send to self) command line switch
 * 1.2.3
 *   - now allows directMessages with the .direct command
 *   - the parseMsg is being rewritten with regexps
 *   - now handles the IVY_DEBUG property
 *   - now handles the -q quiet option
 *   - now handles the ^D ( end of input ) in a clean fashion
 *   - the constructor is public, you can embed a Probe in other programs
 * 1.2.2
 *   - changes setProperty to a backward-compatible construct
 *   - now uses the bus.domains(String domain) in order to display the domain
 *   list
 * 1.2.1
 *   - new -t switch to print the date for each ivy message
 *   - now displays the correct domain list
 *   - now has .bind and .unbind commands
 * 1.2.0
 *   - Probe can now send empty strings on keyboard input
 *   - rewritten with a looping thread on stdin to allow a cleaner exit on die
 *     message : not very good
 *   - processes .help, .die , .quit and .bye commands
 *   - it is possible to rename the JPROBE on the bus with the -n switch, it can
 *     circumvent name collisions during tests
 *     e.g: java fr.dgac.ivy.tools.Probe -n JPROBE2
 * 1.0.10
 * 	exits on end of user input
 *	handles multiple domains - it was a IvyWatcher problem -
 */
package fr.dgac.ivy.tools ;
import fr.dgac.ivy.* ;
import java.io.*;
import java.util.*;
import gnu.getopt.Getopt;
import java.util.regex.*;

public class Probe implements IvyApplicationListener, IvyMessageListener, IvyBindListener {

  public static final String helpCommands = "Available commands:\n"+
  ".die CLIENT\t\t\t* sends a die message\n"+
  ".dieall-yes-i-am-sure\t\t* sends a die to all clients\n"+
  ".direct CLIENT ID MSG\t\t* sends the direct message to the client\n"+
  ".bye\t\t\t\t* quits the probe\n"+
  ".quit\t\t\t\t* quites the probe\n"+
  ".list\t\t\t\t* lists the clients on the bus\n"+
  ".bind REGEXP\t\t\t* subscribes to a regexp\n"+
  ".unbind REGEXP\t\t\t* unsubscribes to a regexp\n"+
  ".bound CLIENT\t\t\t* lists the subscriptions of a client, .bound * to get all\n"+
  ".bound\t\t\t\t* lists the probe's subscriptions\n"+
  ".where CLIENT\t\t\t* displays the host where a client runs\n"+
  ".time COUNT MESSAGE\t\t\t* measures the time it takes to send COUNT messages\n"+
  ".ping CLIENT\t\t\t* measures the roundtrip time in millisecond to reach a client\n"
  ;

  public static final String helpmsg =
    "usage: java fr.dgac.ivy.tools.Probe [options] [regexp]\n"+
    "\t-b BUS\tspecifies the Ivy bus domain\n"+
    "\t-c CLASS\tuses a message class CLASS=Word1,Word2,Word3...\n"+
    "\t-n ivyname (default JPROBE)\n"+
    "\t-q\tquiet, no tty output\n"+
    "\t-d\tdebug\n"+
    "\t-t\ttime stamp each message\n"+
    "\t-s\tsends to self\n"+
    "\t-h\thelp\n"+
    "\n\t regexp is a Perl5 compatible regular expression";

  public static void main(String[] args) throws IvyException {
    Getopt opt = new Getopt("Probe",args,"n:b:c:dqsht");
    int c;
    boolean timestamp=false;
    boolean quiet=false;
    boolean sendsToSelf=false;
    String domain=null;
    String name="JPROBE";
    String[] messageClass=null;
    while ((c = opt.getopt()) != -1) switch (c) {
    case 'd':
      Properties sysProp = System.getProperties();
      sysProp.put("IVY_DEBUG","yes");
      break;
    case 'b': domain=opt.getOptarg(); break;
    case 'c':
      messageClass = opt.getOptarg().split(",");
      break;
    case 'n': name=opt.getOptarg(); break;
    case 'q': quiet=true; break;
    case 't': timestamp=true; break;
    case 's': sendsToSelf=true; break;
    case 'h':
    default: System.out.println(helpmsg); return;
    }
    Probe p = new Probe(new BufferedReader(new InputStreamReader(System.in)),timestamp,quiet,System.getProperty("IVY_DEBUG")!=null);
    p.setExitOnDie(true);
    Ivy bus=new Ivy(name,name+" ready",null);
    bus.addBindListener(p);
    bus.sendToSelf(sendsToSelf);
    if (messageClass!=null) {
      System.out.println("using a message class filter of "+messageClass.length+" elements");
      for (String cls: messageClass) System.out.println("\t "+cls);
      bus.setFilter(messageClass);
    }
    for (int i=opt.getOptind();i<args.length;i++) {
      try {
	bus.bindMsg(args[i],p);
	if (!quiet) System.out.println("you have subscribed to " + args[i]);
      } catch (IvyException ie) {
	System.out.println("you have not subscribed to " + args[i]+ ", this regexp is invalid");
      }
    }
    if (!quiet) System.out.println("broadcasting on "+Domain.domains(domain));
    bus.start(domain);
    p.start(bus);
  }

  private BufferedReader in;
  private Ivy bus;
  private boolean timestamp,quiet,debug,exitOnDie=false, encore=true;
  private static Pattern directMsgRE, timeCountRE;

  static {
    try {
      directMsgRE = Pattern.compile("^\\.direct ([^ ]*) ([0-9]+) (.*)");
      timeCountRE = Pattern.compile("^\\.time (\\d+) (.*)");
    } catch ( PatternSyntaxException res ) {
      res.printStackTrace();
      System.out.println("Regular Expression bug in Ivy source code ... bailing out");
      throw new RuntimeException();
    }
  }

  public Probe(BufferedReader in, boolean timestamp,boolean quiet,boolean debug) {
    this.in=in;
    this.timestamp=timestamp;
    this.quiet=quiet;
    this.debug = debug;
  }


  public void setExitOnDie(boolean b) { exitOnDie=b; }

  public void start(Ivy bus) throws IvyException {
    this.bus=bus;
    bus.addApplicationListener(this);
    traceDebug("Probe Loop started");
    String s;
    SelfIvyClient sic = bus.getSelfIvyClient();
    println(sic.getApplicationName()+ " ready, type .help and return to get help");
    // "infinite" loop on keyboard input
    while ( encore ) {
      try {
	s=in.readLine();
	if (s==null) break;
	if (!parseCommand(s)) break;
      } catch (NullPointerException e) {
	// EOF triggered by a ^D, for instance
	break;
      } catch (IOException e) {
	// System input was closed
	break;
      }
    }
    println("End of input. Good bye !");
    bus.stop();
    traceDebug("Probe Loop stopped");
  }

  boolean parseCommand(String s) throws IOException {
    traceDebug("parsing the ["+s+"] (length "+s.length()+") string");
    // crude parsing of the ".xyz" commands
    Matcher m = directMsgRE.matcher(s);
    Matcher mtime = timeCountRE.matcher(s);
    if (s.length()==0) {
      try {
	println("-> Sent to " +bus.sendMsg(s)+" peers");
      } catch (IvyException ie) {
	println("-> not sent, the message contains incorrect characters");
      }
    } else if (m.matches()) {
      String target = m.group(1);
      int id = Integer.parseInt(m.group(2));
      String message = m.group(3);
      Collection<IvyClient>v=bus.getIvyClientsByName(target);
      if (v.size()==0) println("no Ivy client with  the name \""+target+"\"");
      try {
	for (IvyClient ic : v) ic.sendDirectMsg(id,message);
      } catch (IvyException ie) {
	println("-> not sent, the message contains incorrect characters");
      }
    } else if (s.lastIndexOf(".dieall-yes-i-am-sure")>=0){
      for (IvyClient ic: bus.getIvyClients()) ic.sendDie("java probe wants you to leave the bus");
    } else if (s.lastIndexOf(".die ")>=0){
      String target=s.substring(5);
      Collection<IvyClient>v=bus.getIvyClientsByName(target);
      if (v.size()==0) println("no Ivy client with  the name \""+target+"\"");
      for (IvyClient ic: v) ic.sendDie("java probe wants you to leave the bus");
    } else if (s.lastIndexOf(".unbind ")>=0){
      String regexp=s.substring(8);
      if (bus.unBindMsg(regexp)) {
        println("you have unsubscribed to " + regexp);
      } else {
        println("you can't unsubscribe to " + regexp + ", your're not subscribed to it");
      }
    } else if (s.lastIndexOf(".bound *")>=0){
      int total=0;
      int boundedtotal=0;
      for (IvyClient ic: bus.getIvyClients()) {
	for (String r : ic.getRegexps()) {
	  total++;
	  if (r.startsWith("^")) boundedtotal++;
	  println(ic.getApplicationName()+" has subscribed to: "+r); 
	}
	System.out.println("total: "+total+", unbounded:"+(total-boundedtotal));
      }
    } else if (s.lastIndexOf(".bound ")>=0){
      int total=0;
      int boundedtotal=0;
      String target=s.substring(7);
      Collection<IvyClient>v=bus.getIvyClientsByName(target);
      if (v.size()==0) println("no Ivy client with  the name \""+target+"\"");
      for (IvyClient ic:v) {
	for (String r : ic.getRegexps()) {
	  total++;
	  if (r.startsWith("^")) boundedtotal++;
	  println(target+" has subscribed to: "+r);
	}
	System.out.println("total: "+total+", unbounded:"+(total-boundedtotal));
      }
    } else if (s.lastIndexOf(".bound")>=0){
      println("you have subscribed to:"); 
      for (String re : bus.getSelfIvyClient().getRegexps()) println("\t"+re);
    } else if (s.lastIndexOf(".bind ")>=0){
      String regexp=s.substring(6);
      try {
	bus.bindMsg(regexp,this);
	println("you have now subscribed to " + regexp);
      } catch (IvyException ie) {
	System.out.println("warning, the regular expression '" + regexp + "' is invalid. Not bound !");
      }
    } else if ( (s.lastIndexOf(".quit")>=0)||(s.lastIndexOf(".bye")>=0)){
      bus.stop();
      return false;
    } else if (s.lastIndexOf(".list")>=0) {
      Collection<IvyClient> c = bus.getIvyClients();
      println(c.size()+" clients on the bus");
      for (IvyClient ic: c) println("-> "+ic.getApplicationName());
    } else if ( s.lastIndexOf(".ping ")>=0) {
      String target=s.substring(6);
      Collection<IvyClient>v=bus.getIvyClientsByName(target);
      if (v.size()==0) println("no Ivy client with  the name \""+target+"\"");
      for (IvyClient ic:v) {
	try {
	  ic.ping(new PingCallback() {
	  public void pongReceived(IvyClient ic,int elapsedTime){
	  System.out.println("round trip to "+ic.getApplicationName()+" "+elapsedTime+" ms");
	  }
	  });
	} catch (IvyException ie) {
	  println("-> ping not sent, the remote client must have disconnected");
	}
      }
    } else if ( s.lastIndexOf(".where ")>=0) {
      String target=s.substring(7);
      Collection<IvyClient>v=bus.getIvyClientsByName(target);
      if (v.size()==0) println("no Ivy client with  the name \""+target+"\"");
      for (IvyClient ic: v) println(target+" runs on "+ic.getHostName());
    } else if (mtime.matches()) {
      long before = new java.util.Date().getTime();
      int times=Integer.parseInt(mtime.group(1));
      try {
	int n=0;
	for (int i=0;i<times;i++) n=bus.sendMsg(mtime.group(2));
	long after = new java.util.Date().getTime();
	println("-> it took "+(after-before)+"ms to send "+times+" to " +n+" peers");
      } catch (IvyException ie) {
	println("-> not sent, the line contains incorrect characters");
      }
    } else if ( s.lastIndexOf(".help")>=0) {
      println(helpCommands);
    } else if ( s.charAt(0)=='.') {
      println("this command is not recognized");
      println(helpCommands);
    } else {
      try {
	println("-> Sent to " +bus.sendMsg(s)+" peers");
      } catch (IvyException ie) {
	println("-> not sent, the line contains incorrect characters");
      }
    }
    return true;
  } // parseCommand

  public void bindPerformed(IvyClient client,int id,String re) {
    String s="";
    if (!bus.checkRegexp(re)) s=" WITH NO EFFECT, because of the filters";
    println(client.getApplicationName() + " subscribes to " +re+s);
  }

  public void unbindPerformed(IvyClient client,int id,String re) {
    println(client.getApplicationName() + " unsubscribes to " +re );
  }

  public void connect(IvyClient client) {
    println(client.getApplicationName() + " connected " );
  }

  public void disconnect(IvyClient client) {
    println(client.getApplicationName() + " disconnected " );
  }

  public void die(IvyClient client, int id,String msgarg) {
    println("received die msg from " + client.getApplicationName() +" with the message: "+msgarg+", good bye");
    /* I cannot stop the readLine(), because it is native code ?! */
    if (exitOnDie) {
      encore = false;
      try {
	System.in.close();
      } catch (IOException ie) {
	// perfect !
      }
      //System.exit(0);
    }
  }

  public void directMessage(IvyClient client, int id, String arg) {
   println(client.getApplicationName() + " sent the direct Message, id: "+ id + ", arg: "+ arg );
  }

  public void receive(IvyClient client, String[] args) {
    StringBuffer s=new StringBuffer(client.getApplicationName());
    s.append(" sent");
    for (String arg: args) {
      s.append(" '");
      s.append(arg);
      s.append("'");
    }
    println(s.toString());
  }

  private void traceDebug(String s){ if (debug) System.out.println("-->Probe<-- "+s); }
  private void println(String s){ if (!quiet) System.out.println(date()+s); }
  private String date() {
    if (!timestamp) return "";
    Date d = new Date();
    java.text.DateFormat df = java.text.DateFormat.getTimeInstance();
    return "["+df.format(d)+"] ";
  }

}
