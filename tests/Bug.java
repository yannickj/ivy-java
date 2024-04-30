import fr.dgac.ivy.* ;

class Bug implements IvyApplicationListener {

    private Ivy bus;

    Bug() {
      bus = new Ivy("Bug","Hello le monde",this);
      try {
	bus.start(null);
      } catch (IvyException ie) {
	System.err.println("can't run the Ivy bus" + ie.getMessage());
      }
    }

    public void disconnect(IvyClient client) { }

    public void directMessage(IvyClient client,int id,String msgarg) {}

    public void connect(IvyClient client) {
      System.out.println("sending messages");
      try {
	bus.sendMsg("coucou");
	bus.sendMsg("titi");
	bus.sendMsg("tata");
	bus.sendMsg("toto");
      } catch (IvyException ie) { }
      System.out.println("done");
    }

    public void die(IvyClient client,int id,String msg) {
      System.out.println("argh. cya "+msg);
    }

    public static void main(String args[]) {
      new Bug();
    }
}
