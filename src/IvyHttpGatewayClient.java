/**
 *
 * a helper class for Ivy Web Services
 *
 * IvyHttpGatewayClient.java
 * @author Francis Jambon, IMAG
 */

package fr.dgac.ivy.tools;

import java.lang.*;
import java.io.*;
import java.net.*;


/**
 * Light remote client for the Ivy HTTP Gateway.
 * This code is given as example only: it is not compulsery
 * to use this client with the IvyHttpGatewayServlet.
 * <strong>Warning:</strong> the error management is rather poor!
 * <br><br>
 *
 * <strong>License:</strong><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 * See IvyHttpGatewayServlet
 *
 * @see IvyHttpGatewayServlet
 * @author Francis JAMBON - CLIPS-IMAG/MultiCom
 * @version 1.0.5
 */
public class IvyHttpGatewayClient {
    
    private String name; // bus name
    private String domain; // bus domain
    private String url; // servlet URL
    private boolean is_started; // bus state (supposed!)
    private static final boolean debug=true; // debug option
   
    
    /**
     * Creates a new instance of IvyHttpGatewayClient.
     * @param name the bus name
     * @param url the servlet URL (protocol, host, port and complete servlet path)
     */
    public IvyHttpGatewayClient(String name, String url) {
        this.name = name;
        this.url = url;
        this.domain = null;
        this.is_started = false;
    }
    
    
    /**
     * Starts the Ivy bus.
     * @param domain the bus domain (address and port)
     */
    public void start(String domain) {
        this.domain = domain;
        if (!is_started) {
            String request = this.url+"?"+"cmd=start"+"&name="+this.name+"&domain="+this.domain;
            doHttpRequest(request);
            this.is_started=true;
        }
    }
    
    
    /**
     * Stops the Ivy bus.
     */
    public void stop() {
        if (is_started) {
            String request = this.url+"?"+"cmd=stop"+"&name="+this.name+"&domain="+this.domain;
            doHttpRequest(request);
            this.is_started=false;
        }
    }
    
    
    /**
     * Sends a message to the Ivy bus.
     * The bus must be started before sending a message.
     * @param msg the message to be sent
     */
    public void sendMsg(String msg) {
        if (is_started) {
            String request = this.url+"?"+"cmd=sendmsg"+"&name="+this.name+"&domain="+this.domain+"&msg="+msg;
            doHttpRequest(request);
        }
    }
    
    
    /**
     * Generic request to the servlet.
     * @param request the URL (protocol, host, port, servlet path and parameters)
     */
    private static void doHttpRequest(String request) {
        if (debug) System.out.println();
        if (debug) System.out.println("-------------------- start of request ------------------");
        if (debug) System.out.println("IvyHttpGatewayClient");
        if (debug) System.out.println("REQUEST: "+request);
        try {
            URL servlet = new URI(request).toURL();
            URLConnection connection = servlet.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    servlet.openStream()));
            String input_line;
            if (debug) System.out.println("ANSWER:");
            while ((input_line=in.readLine()) != null)
                if (debug) System.out.println(input_line);
            in.close();
        } catch (MalformedURLException e) {
            System.err.println("IvyHttpGatewayClient ERROR: URL Malformed");
            if (debug) e.printStackTrace();
        } catch (URISyntaxException e) {
            System.err.println("IvyHttpGatewayClient ERROR: URL Malformed");
            if (debug) e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IvyHttpGatewayClient ERROR: Open connection failed");
            if (debug) e.printStackTrace();
        }
        if (debug) System.out.println("-------------------- end of request --------------------");
        if (debug) System.out.println();
    }  
    
}
