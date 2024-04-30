/*
 * IvyHttpGatewayTest.java
 * Created on 18 avril 2005, 20:25
 */

package fr.dgac.ivy.tools;


/**
 * Dynamic tests of the light remote client for the Ivy HTTP Gateway.
 * <br><br>
 *
 * <strong>License:</strong><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 * See IvyHttpGatewayServlet
 *
 * @see IvyHttpGatewayClient
 * @see IvyHttpGatewayServlet
 * @author Francis JAMBON - CLIPS-IMAG/MultiCom
 * @version See IvyHttpGatewayClient
 */
public class IvyHttpGatewayTest {
    
    /**
     * Main method: for testing purpose.
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("### START OF TEST ###");
        IvyHttpGatewayClient test = new IvyHttpGatewayClient(
                "test",
                "http://localhost:8084/Ivy-HttpGateway/IvyHttpGatewayServlet");
        test.start(
                "228.1.2.4:5678");
        test.sendMsg(
                "hello");
        test.stop();
        System.out.println("### END OF TEST ###");
    }    
    
}
