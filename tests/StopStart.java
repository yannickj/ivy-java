import fr.dgac.ivy.* ;

class StopStart {

  Ivy bus;

  public static void main(String[] args) throws IvyException {
    String domain=Ivy.getDomainArgs("StopStartTest",args);
    new StopStart(domain);
  }

  public StopStart(String domain) throws IvyException {
    int n=1;
    bus = new Ivy("StopStart","StopStart ready", null);
    System.out.println("--------------- starting bus");
    bus.start(domain);
    System.out.println("--------------- sleeping "+n+" seconds");
    try { Thread.sleep(n*1000); } catch (InterruptedException ie) { }
    System.out.println("--------------- stopping bus");
    bus.stop();
    System.out.println("--------------- restarting bus");
    bus.start(domain);
    System.out.println("sleeping "+n+" seconds");
    try { Thread.sleep(n*1000); } catch (InterruptedException ie) { }
    System.out.println("--------------- restopping bus");
    bus.stop();
    System.out.println("--------------- good bye, program should exit now");
  }

}
