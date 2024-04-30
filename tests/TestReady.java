import fr.dgac.ivy.*;
import gnu.getopt.*;


/**
 * Ivy java ReadyMessage tester.
 * 
 * @author Francois-Regis Colin <mailto:fcolin@cena.fr>
 * 
 *         (c) CENA
 * 
 *         usage: java TestReady & java TestReady -o
 * 
 */

class TestReady implements IvyApplicationListener {

	static String me = "A";
	static String other = "B";
	static String ready_message = "A ready";
	static String ready_bind = "^B ready";
	
	private Ivy bus;
		private class Ready implements IvyMessageListener {
			public void receive (IvyClient app, String[] args)
		{
				int count = 0;
				try {
					count = bus.sendMsg ("are you there "+app.getApplicationName());
				}
				catch (IvyException ie) {
					System.err.println("can't sendMsg" + ie.getMessage());
				}
		        System.out.println("Application "+me+" received '"+ready_bind+"' from "+app.getApplicationName()+
		        		" sent question 'are you there "+app.getApplicationName()+"'= "+ count );
		}
		}
		private class Question implements IvyMessageListener {
			public void receive (IvyClient app, String[] args)
		{
				int count = 0;
				try {
					count = bus.sendMsg ("yes i am "+me);
				}
				catch (IvyException ie) {
					System.err.println("can't sendMsg" + ie.getMessage());
				}
		        System.out.println("Application "+me+" Reply to "+app.getApplicationName()+" are you there = "+count );

		}
		}
		private class Reply implements IvyMessageListener {
			public void receive (IvyClient app, String[] args)
		{
			System.out.println("Application "+app.getApplicationName()+" Reply to our question! "+args[0]);

		}
		}
	TestReady(String domain) {
		try {
		
			bus = new Ivy(me, ready_message, this);
			bus.bindMsg (ready_bind, new Ready());
			bus.bindMsg ("^are you there "+me, new Question());
			bus.bindMsg ("^(yes i am "+other+")", new Reply());

			bus.start(domain);
		} catch (IvyException ie) {
			System.err.println("can't run the Ivy bus" + ie.getMessage());
		}
	}


	public void disconnect(IvyClient client) {
	}

	public void directMessage(IvyClient client, int id, String msgarg) {
	}

	public void connect(IvyClient client) {
		
	}

	public void die(IvyClient client, int id, String msg) {
		System.out.println("argh. cya " + msg);
	}

	public static void main(String args[]) {
	Getopt opt = new Getopt("TestReady",args,"b:o");
		String domain=null;
		int c;
		boolean swap = false;
		while ((c = opt.getopt()) != -1) switch (c) {
		case 'b':
		  domain=opt.getOptarg();
		  break;
		case 'o':
		  swap=true;
		  break;
		default:
		  System.exit(0);
		}

		if (swap) {
			me = "B";
			other = "A";
			ready_message = "B ready";
			ready_bind = "^A ready";
		}
		System.out.println("I am "+me);
		new TestReady(domain);
	}
}
