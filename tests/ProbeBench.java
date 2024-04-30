import java.io.* ;
import fr.dgac.ivy.* ;
import fr.dgac.ivy.tools.* ;

public class ProbeBench {

  public ProbeBench(int test) throws IvyException {
    switch (test) {
      case 1: test1(); break;
    }
  }

  /*
   * Tests if the Probe exits on a die message
   */
  public void test1() throws IvyException {
    Probe p = new Probe(new BufferedReader(new InputStreamReader(System.in)),true,false,true);
    Ivy bus1 = new Ivy("ProbeTest","ProbeTest ready",null);
    bus1.start(null);
    System.out.println("starting the probe");
    p.start(bus1);
    System.out.println("sleeping 5 seconds");
    try { Thread.sleep(5000); } catch (InterruptedException ie) { }

    Ivy bus2 = new Ivy("ProbeKiller","ProbeKiller ready",null);
    bus2.start(null);
    System.out.println("starting the probe killer");
    new DieSender(bus2,"ProbeTest");
    System.out.println("sleeping 5 seconds");
    try { Thread.sleep(5000); } catch (InterruptedException ie) { }
    bus2.stop();
    try { System.in.close(); } catch (java.io.IOException ioe ) { }
    System.out.println("I should leave now");
  }

  public static void main(String[] args) throws IvyException {
    new ProbeBench(1);
  }

}
