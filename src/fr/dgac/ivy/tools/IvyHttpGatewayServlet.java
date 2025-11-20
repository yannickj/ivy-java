/**
 * a helper class for Ivy Web Services
 *
 * @author Francis Jambon
 */

package fr.dgac.ivy.tools;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import fr.dgac.ivy.*;


/**
 * Simple gateway between HTTP and Ivy software bus protocols.
 * <br><br>
 * The gateway enables remote clients to manage Ivy clients via
 * HTTP protocol. It aims at solving Ivy limitations to local networks.
 * Features are limited to Ivy bus start(), stop() and sendMsg() methods.
 * The Ivy HTTP gateway considers two Ivy instances as identical if their
 * buses names, buses domains and clients IP names equals pair-by-pair.
 * <br><br>
 * Ivy main web page:
 * <a target="_blank" href="http://www.tls.cena.fr/products/ivy/">
 * http://www.tls.cena.fr/products/ivy/</a>
 * <br>
 * Author home page:
 * <a target="_blank" href="http://www-clips.imag.fr/multicom/User/francis.jambon/index-en.html">
 * http://www-clips.imag.fr/multicom/User/francis.jambon/index-en.html</a>
 * <br><br>
 *
 * <strong>Requests examples:</strong>
 * <pre>
 * - start bus: http://host/IvyHttpGatewayServlet?cmd=start&name=test&domain=228.1.2.4:5678
 * - start bus (with message): http://host/IvyHttpGatewayServlet?cmd=start&name=test&domain=228.1.2.4:5678&msg=ready
 * - stop bus: http://host/IvyHttpGatewayServlet?cmd=stop&name=test&domain=228.1.2.4:5678
 * - send message: http://host/IvyHttpGatewayServlet?cmd=sendmsg&name=test&domain=228.1.2.4:5678&msg=hello
 * - interactive webpage: http://host/IvyHttpGatewayServlet
 * - status information: http://host/IvyHttpGatewayServlet?cmd=status
 * - kill all buses managed by the servlet: http://host/IvyHttpGatewayServlet?cmd=killall
 * </pre>
 *
 * <strong>Versions history:</strong>
 * <pre>
 * - 1.1: new package structure, default address and documentation
 *        non-regression tests with new Ivy 1.2.8-rc2 library
 * - 1.0.5: correction of the form encoding attribute incompatibility with IE
 * - 1.0.4: correction of the button element incompatibility with IE
 * - 1.0.3: correction of the remote host identification bug
 * - 1.0.2: start command can be invoked without message
 * - 1.0.1: licence and some name modifications
 * - 1.0: first working release
 * </pre>
 *
 * <strong>Bugs:</strong>
 * <pre>
 * - the POST method does not work in the interactive webpage
 *   (the parameters are not transmitted)
 * </pre>
 *
 * <strong>License:</strong>
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 * GNU Lesser General Public License, see <a target="_blank" href="http://www.gnu.org/copyleft/lesser.html">http://www.gnu.org/copyleft/lesser.html</a>
 *
 * @author Francis JAMBON - CLIPS-IMAG/MultiCom
 * @version 1.1
 */
public class IvyHttpGatewayServlet extends HttpServlet {
    
    // Ivy bus(es) list
    private Hashtable<IvyHashKey,Ivy> buses;
    
    // Valid name, domain and msg regexps
    private static final String NAME_REGEXP="[^(\\r)]+";
    private static final String DOMAIN_REGEXP="(\\d){1,3}+(\\.)(\\d){1,3}+(\\.)(\\d){1,3}+(\\.)(\\d){1,3}+(\\:)(\\d){1,5}+";
    private static final String MSG_REGEXP="[^(\\r)]*";
    
    // Default values for the interactive webpage
    private static final String DEFAULT_NAME="test";
    private static final String DEFAULT_DOMAIN="228.1.2.4:5678";
    private static final String DEFAULT_MSG="hello";
    
    
    /**
     * Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.buses = new Hashtable<IvyHashKey,Ivy>();
    }
    
    
    /**
     * Destroys the servlet.
     */
    public void destroy() {
        for (Ivy eb : this.buses.values() ) eb.stop();
        // wait 100ms to prevent ugly Ivy bus(es) disconnection
        try { Thread.sleep(100); } catch (InterruptedException ie) {}
        this.buses.clear();
        this.buses=null;
    }
    
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected synchronized void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        if (request.getQueryString()!=null) {
            if (request.getParameter("cmd").equals("start"))
                processRequestStart(request,response);
            else if (request.getParameter("cmd").equals("stop"))
                processRequestStop(request,response);
            else if (request.getParameter("cmd").equals("sendmsg"))
                processRequestSendMsg(request,response);
            else if (request.getParameter("cmd").equals("status"))
                processRequestStatus(request,response);
            else if (request.getParameter("cmd").equals("killall"))
                processRequestKillAll(request,response);
            else
                processRequestError(request, response);
        } else
            processRequestWebpage(request,response);
    }
    
    
    /**
     * Starts an Ivy bus and adds it to the list.
     */
    private void processRequestStart(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Ivy HTTP Gateway</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<hr>");
        out.println("<pre>");

        String from = request.getRemoteHost();
        String name = request.getParameter("name");
        String domain = request.getParameter("domain");
        String msg = request.getParameter("msg");
        
        out.println("Ivy HTTP Gateway");
        out.println("Request from ["+from+"]");
        out.println();
        out.println("START");
        out.println("- name="+name);
        out.println("- domain="+domain);
        if (msg!=null) out.println("- msg="+msg);
        out.println();
        
        if ( domain!=null && domain.matches(DOMAIN_REGEXP) &&
                name!=null && name.matches(NAME_REGEXP) &&
                ( msg==null || (msg!=null && msg.matches(MSG_REGEXP)) ) )  {
            
            IvyHashKey ihk = new IvyHashKey(from, name, domain);
            if (!this.buses.containsKey(ihk)) {
                try {
                    Ivy bus = new Ivy(name, msg, null);
                    bus.start(domain);
                    this.buses.put(ihk,bus);
                    out.println("Ivy bus started");
                } catch (IvyException ie) {
                    out.println("ERROR while starting Ivy bus: "+ie.getMessage());
                }
            } else {
                out.println("ERROR: Similar Ivy bus already started");
            }
        } else {
            out.println("ERROR: Parameter(s) not valid");
        }
        
        out.println("</pre>");
        out.println("<hr>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }
    
    
    /**
     * Stops an Ivy bus and removes it from the list.
     */
    private void processRequestStop(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Ivy HTTP Gateway</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<hr>");
        out.println("<pre>");
        
        String from = request.getRemoteHost();
        String name = request.getParameter("name");
        String domain = request.getParameter("domain");
        
        out.println("Ivy HTTP Gateway");
        out.println("Request from ["+from+"]");
        out.println();
        out.println("STOP");
        out.println("- name="+name);
        out.println("- domain="+domain);
        out.println();
        
        if ( domain!=null && domain.matches(DOMAIN_REGEXP) &&
                name!=null && name.matches(NAME_REGEXP) ) {
            
            IvyHashKey ihk = new IvyHashKey(from, name, domain);
            if (this.buses.containsKey(ihk)) {
                Ivy bus = (Ivy)(this.buses.get(ihk));
                bus.stop();
                // wait 100ms to prevent ugly Ivy bus disconnection
                try { Thread.sleep(100); } catch (InterruptedException ie) {}
                this.buses.remove(ihk);
                bus=null;
                out.println("Ivy bus stopped");
            } else {
                out.println("ERROR: Ivy bus does not exist");
            }
        } else {
            out.println("ERROR: Parameter(s) not valid");
        }
        
        out.println("</pre>");
        out.println("<hr>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }
    
    
    /**
     * Sends a message to an Ivy bus.
     */
    private void processRequestSendMsg(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Ivy HTTP Gateway</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<hr>");
        out.println("<pre>");
        
        String from = request.getRemoteHost();
        String name = request.getParameter("name");
        String domain = request.getParameter("domain");
        String msg = request.getParameter("msg");
        
        out.println("Ivy HTTP Gateway");
        out.println("Request from ["+from+"]");
        out.println();
        out.println("SEND MESSAGE");
        out.println("- name="+name);
        out.println("- domain="+domain);
        out.println("- msg="+msg);
        out.println();
        
        if ( domain!=null && domain.matches(DOMAIN_REGEXP) &&
                name!=null && name.matches(NAME_REGEXP) &&
                msg !=null && msg.matches(MSG_REGEXP) ) {
            
            IvyHashKey ihk = new IvyHashKey(from, name, domain);
            if (this.buses.containsKey(ihk)) {
                try {
                    Ivy bus = (Ivy)(this.buses.get(ihk));
                    int count = bus.sendMsg(msg);
                    out.println("Message sent to "+count+" client(s)");
                } catch (IvyException ie) {
                    out.println("ERROR sending message: "+ie.getMessage());
                }
            } else {
                out.println("ERROR: Ivy bus does not exist");
            }
        } else {
            out.println("ERROR: Parameter(s) not valid");
        }
        
        out.println("</pre>");
        out.println("<hr>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }
    
    
    /**
     * Status page.
     */
    private void processRequestStatus(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Ivy HTTP Gateway</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<hr>");
        out.println("<pre>");
        
        out.println("Ivy HTTP Gateway");
        out.println("Request from ["+request.getRemoteHost()+"]");
        out.println();
        out.println("STATUS");
        out.println();
        out.println(this.buses.size()+" Ivy bus(es) alive");
        
        for (IvyHashKey ihk : this.buses.keySet()) {
            out.println("- Ivy bus ["+ihk.getName()+
                    "] on domain ["+ihk.getDomain()+
                    "] managed by ["+ihk.getFrom()+"]");
        }
        
        out.println("</pre>");
        out.println("<hr>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }
    
    
    /**
     * Stops all Ivy buses.
     */
    private void processRequestKillAll(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Ivy HTTP Gateway</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<hr>");
        out.println("<pre>");
        
        out.println("Ivy HTTP Gateway");
        out.println("Request from ["+request.getRemoteHost()+"]");
        out.println();
        out.println("KILL ALL");
        out.println();
        
        if (this.buses.size()!=0) {
            for (Ivy eb : this.buses.values() ) eb.stop();
            for ( IvyHashKey ihk : this.buses.keySet()) {
                out.println("Ivy bus ["+ihk.getName()+
                        "] on domain ["+ihk.getDomain()+
                        "] managed by ["+ihk.getFrom()+
                        "] stopped");
            }
            // wait 100ms to prevent ugly Ivy bus(es) disconnection
            try { Thread.sleep(100); } catch (InterruptedException ie) {}
            this.buses.clear();
        } else {
            out.println("NOTHING TO DO: no alive Ivy bus");
        }
        
        out.println("</pre>");
        out.println("<hr>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }
    
    
    /**
     * Error (request not understood).
     */
    private void processRequestError(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Ivy HTTP Gateway</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<hr>");
        out.println("<pre>");
        
        out.println("Ivy HTTP Gateway");
        out.println("Request from ["+request.getRemoteHost()+"]");
        out.println();
        out.println("ERROR: request not understood");
        out.println();
        out.println("Parameters string readed: "+request.getQueryString());
        
        out.println("</pre>");
        out.println("<hr>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }
    
    
    /**
     * Interactive webpage (request without parameter).
     */
    private void processRequestWebpage(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("    <head>");
        out.println("        <title>Ivy HTTP Gateway</title>");
        out.println("    </head>");
        out.println("    <body>");
        out.println("    <h2>Ivy HTTP Gateway Servlet Interactive Webpage</h2>");
        out.println("    <hr>");
        out.println("    <h3>Global Commands</h3>");
        out.println("    <form method=\"get\"");
        out.println("        action=\""+request.getRequestURL()+"\"");
        out.println("        name=\"Global Commands\">");
        out.println("        <table style=\"text-align: left;\" border=\"0\" cellpadding=\"0\" cellspacing=\"6\">");
        out.println("            <tbody>");
        out.println("                <tr>");
        out.println("                    <td>Ivy bus(es) status: </td>");
        out.println("                    <td><input type=\"submit\" value=\"status\" name=\"cmd\"></td>");
        out.println("                <tr>");
        out.println("                </tr>");
        out.println("                    <td>Kill all (warning!): </td>");
        out.println("                    <td><input type=\"submit\" value=\"killall\" name=\"cmd\"></td>");
        out.println("                </tr>");
        out.println("            </tbody>");
        out.println("        </table>");
        out.println("    </form>");
        out.println("    <hr>");
        out.println("    <h3>Bus Commands</h3>");
        out.println("    <form method=\"get\"");
        out.println("        action=\""+request.getRequestURL()+"\"");
        out.println("        name=\"Bus Commands\">");
        out.println("        <table style=\"text-align: left;\" border=\"0\" cellpadding=\"0\" cellspacing=\"6\">");
        out.println("            <tbody>");
        out.println("                <tr>");
        out.println("                    <td><label>Name:</label> </td>");
        out.println("                    <td><input name=\"name\" value=\""+DEFAULT_NAME+"\"> </td>");
        out.println("                    <td>(name of the Ivy agent - <strong>required</strong>)</td>");
        out.println("                </tr>");
        out.println("                <tr>");
        out.println("                    <td><label>Domain:</label> </td>");
        out.println("                    <td><input value=\""+DEFAULT_DOMAIN+"\" name=\"domain\"> </td>");
        out.println("                    <td>(IP address and port - <strong>required</strong>) </td>");
        out.println("                </tr>");
        out.println("                <tr>");
        out.println("                    <td><label>Message:</label> </td>");
        out.println("                    <td><input name=\"msg\" value=\""+DEFAULT_MSG+"\"> </td>");
        out.println("                    <td>(message to be sent)</td>");
        out.println("                </tr>");
        out.println("            </tbody>");
        out.println("        </table>");
        out.println("        <table style=\"text-align: left;\" border=\"0\" cellpadding=\"0\" cellspacing=\"6\">");
        out.println("            <tbody>");
        out.println("                <tr>");
        out.println("                    <td>Commands: </td>");
        out.println("                    <td><input type=\"submit\" value=\"start\" name=\"cmd\"> </td>");
        out.println("                    <td><input type=\"submit\" value=\"stop\" name=\"cmd\"> </td>");
        out.println("                    <td><input type=\"submit\" value=\"sendmsg\" name=\"cmd\"> </td>");
        out.println("                </tr>");
        out.println("            </tbody>");
        out.println("        </table>");
        out.println("    </form>");
        out.println("    <hr>");
        out.println("    <code>Request from ["+request.getRemoteHost()+"]</code>");
        out.println("    <br>");
        out.println("    <code>CLIPS-IMAG/MultiCom</code>");
        out.println("    </body>");
        out.println("</html>");
        out.close();
    }
    
    
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    
    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    
    /**
     * Returns a short description of the servlet.
     * @return the short description
     */
    public String getServletInfo() {
        return "Ivy HTTP Gateway";
    }
    
    
    /**
     * Internal class for Ivy bus hashkey definition.
     */
    private class IvyHashKey {
        
        private String from;
        private String name;
        private String domain;
        
        public IvyHashKey(String from, String name, String domain) {
            this.from=from;
            this.name=name;
            this.domain=domain;
        }
        
        public String getFrom() { return this.from; }
        
        public String getName() { return this.name; }
        
        public String getDomain() { return this.domain; }
        
        @Override public boolean equals(Object o) {
            if (o!=null && o instanceof IvyHashKey) {
                IvyHashKey ihk = (IvyHashKey)o;
                return (this.from.equals(ihk.from) &&
                        this.name.equals(ihk.name) &&
                        this.domain.equals(ihk.domain) );
            } else return false;
        }
        
        public int hashCode() {
            return this.name.hashCode();
        }
        
    }
    
    
}
