/* 
 * <copyright> 
 *  Copyright 1999-2004 Cougaar Software, Inc.
 *  under sponsorship of the Defense Advanced Research Projects 
 *  Agency (DARPA). 
 *  
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).  
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright> 
 */ 
 
package org.cougaar.mts.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;

import javax.servlet.Servlet;

import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ServletService;
import org.cougaar.lib.web.service.RootServletService;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.LinkProtocol;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.AttributedMessage;

/**
 * There is no javadoc on this class.
 */
public class HTTPLinkProtocol extends RPCLinkProtocol 
{
  
    private LoggingService logger;
    private ServletService _servletService;
    private boolean servant_made = false;
    public final String SERVLET_URI = "/httpmts";
     

    public void load() 
    {
	super.load();
	logger = getLoggingService(); // from BoundComponent 
    } 
  

    /**
     * We release the ServletService here because in doing so,
     * the ServletService.unregisterAll() is invoked. 
     */
    public void unload() {
	ServiceBroker sb = getServiceBroker();
	sb.releaseService(this, ServletService.class, _servletService);  
	super.unload();
    }

  
    /**
     * Get the WP Entry Type for registering and querying for WP entries.
     */
    public String getProtocolType() 
    {
	return "-HTTP"; 
    }
  
    /**
     * Get the protocol to use for http connections.
     */
    public String getProtocol() 
    {
	return "http";
    }
  
    /**
     * determined the underlying socket is encrypted.
     */
    protected Boolean usesEncryptedSocket() 
    {
	return Boolean.FALSE;
    }
  
    /**
     * Returns 500 (hard-coded value less than RMI).
     */
    protected int computeCost(AttributedMessage message) 
    {
	return 500;
    }
  
    protected String getPath()
    {
	return SERVLET_URI;
    }
  
    /**
     * Create servlet that handle java serialized messages over HTTP.
     */
    protected Servlet createServlet() 
    {
	return new HTTPLinkProtocolServlet(getDeliverer(), logger);
    }
  
    /**
     * Create destination link to stream java serialized messages over
     * HTTP. 
     */
    protected DestinationLink createDestinationLink(MessageAddress addr) 
    {
	return new HTTPDestinationLink(addr); 
    }


  

    /**
     * Register the Servlet that will handle the messages on the
     * receiving end.
     */
    private void registerServlet(ServiceBroker sb) 
    {
	_servletService = (ServletService) 
	    sb.getService(this, ServletService.class, null);
	try {
	    if(logger.isDebugEnabled()) {
		logger.debug("registering " + getPath() + " with " + _servletService);
	    }
	    _servletService.register(getPath(), createServlet());
	} catch(IllegalArgumentException iae) {
	    // an IllegalArgumentException could occur if the servlet
	    // path has already been registered.  for example, both
	    // the HTTP and HTTPS LinkProtocols could be installed.
	    logger.warn(getPath() + " already register.");
	} catch(Exception e) {
	    logger.error(getPath() + " failed to register.");
	}
    
	// we release the ServletService in the unload() method
	// because in doing so, the ServletService.unregisterAll() is
	// invoked.
    }
    
 
    /**
     * This function binds the url in the wp early.  But the servlet
     * at that url can't be made until the ServletService is
     * available.  This is handled by registerServlet(), which won't
     * be called until the ServiceAvailableEvent says it's time.
     */
    protected void findOrMakeNodeServant()
    {
	if (servant_made) return;

	ServiceBroker sb = getServiceBroker();
	RootServletService ss = (RootServletService) 
	    sb.getService(this, RootServletService.class, null);
	int port = ss.getHttpPort();
	MessageAddress node_addr = getNameSupport().getNodeMessageAddress();
	String node_name = node_addr.toAddress();
	try {
	    InetAddress me = InetAddress.getLocalHost();
	    URI nodeURI = new URI(getProtocol() + "://" + me.getHostName() +
				  ':' + port + "/$" + node_name + getPath());
	    setNodeURI(nodeURI);
	} catch (Exception e) {
	    e.printStackTrace();
	}
	sb.releaseService(this, RootServletService.class, ss); 


	// Call registerServlet only if/when BlackboardService is
	// available.  The BlackboardService is required because we
	// want to register our servlet with that ServiceBroker.
	if (sb.hasService(BlackboardService.class)) {
	    registerServlet(sb);
	} else {
	    sb.addServiceListener(new ServiceAvailableListener() {
		    public void serviceAvailable(ServiceAvailableEvent ae) {
			Class svc_class = ae.getService();
			if (BlackboardService.class.isAssignableFrom(svc_class))
			    {
				ServiceBroker svc_sb = ae.getServiceBroker();
				registerServlet(svc_sb);
				svc_sb.removeServiceListener(this);
			    }
		    }
		});
	}   

	servant_made = true;

    }

    /**
     * Servlets handle the new-address case automatically, so this is
     * a no-op.
     */
    protected void remakeNodeServant()
    {
    }



    protected class HTTPDestinationLink extends Link {

	public HTTPDestinationLink(MessageAddress target) 
	{
	    super(target);
	}

	public Class getProtocolClass() {
	    return HTTPLinkProtocol.this.getClass();
	}


	protected Object decodeRemoteRef(URI ref)
	    throws Exception
	{
	    if (ref == null) 
		return null;
	    else
		return ref.toURL();
	}

	/**
	 * Posts the message to the target Agent's HTTP Link Protocol Servlet.
	 */
	protected MessageAttributes forwardByProtocol(Object remote_ref,
						      AttributedMessage message) 
	    throws NameLookupException, UnregisteredNameException, 
		   CommFailureException, MisdeliveredMessageException 
	{
	    try {
		Object response = postMessage((URL) remote_ref, message);
		if (response instanceof MessageAttributes) {
		    return (MessageAttributes) response;
		} else if (response instanceof MisdeliveredMessageException) {
		    decache();
		    throw (MisdeliveredMessageException) response;
		} else {
		    throw new CommFailureException((Exception) response);
		}
	    } catch (Exception e) {
		//e.printStackTrace();
		throw new CommFailureException(e);
	    }
	}

    
	/**
	 * This method streams serialized java objects over HTTP, and
	 * could be overridden if streaming format is different (e.g.,
	 * SOAP) 
	 */
	protected Object postMessage(URL url, AttributedMessage message) 
	    throws IOException, ClassNotFoundException, UnknownHostException 
	{
	    ObjectInputStream ois = null;
	    ObjectOutputStream out = null;
	    try {
		if(logger.isDebugEnabled()) {
		    logger.debug("sending " + 
			       message.getRawMessage().getClass().getName() + 
			       "(" + 
			       message.getOriginator() + "->" +
			       message.getTarget() + ") to " + url);
		}
		// NOTE: Performing a URL.openConnection() does not
		// necessarily open a new socket.  Specifically,
		// HttpUrlConnection reuses a previously opened socket
		// to the target, and there is no way to force the
		// underlying socket to close.  From the javadoc:
		// "Calling the disconnect() method may close the
		// underlying socket if a persistent connection is
		// otherwise idle at that time."
		//
		// However, This could pose a resource consumption
		// issue.  If this is the case, we need to use a
		// different HTTP Client implementation such as
		// Jakarta's Common HTTP Client.
		HttpURLConnection conn = (HttpURLConnection)
		    url.openConnection();
		// Don't follow redirects automatically.
		conn.setInstanceFollowRedirects(false);
		// Let the system know that we want to do output
		conn.setDoOutput(true);
		// Let the system know that we want to do input
		conn.setDoInput(true);
		// No caching, we want the real thing
		conn.setUseCaches(false);
		// Specify the content type
		conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
		conn.setRequestMethod("POST");
		// write object to output stream
		out = new ObjectOutputStream(conn.getOutputStream());
		out.writeObject(message);
		out.flush();

		// get response
		ois = new ObjectInputStream(conn.getInputStream());
		return ois.readObject();
	    } catch(Exception e) {
		logger.debug("Exception in postMessge", e);
	    } finally {
		if (out != null) {
		    out.close();
		}
		if (ois != null) {
		    ois.close();
		}
	    }
	    return null;
	}

    }
  
  
}
