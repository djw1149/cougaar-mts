/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.mts.base;
import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.std.MulticastMessageAddress;

/**
 * The MessageTransportRegistry singleton is a utility instance that
 * helps certain pieces of the message transport subsystem to find one
 * another. */
public interface MessageTransportRegistryService extends Service
{


    // public void setNameSupport(NameSupport nameSupport);
    // public void setReceiveLinkFactory(ReceiveLinkFactory receiveLinkFactory);

    void addLinkProtocol(LinkProtocol lp);
    boolean hasLinkProtocols(); // only useful for the LinkProtocolFactory
    String getIdentifier();
    boolean isLocalClient(MessageAddress id);
    ReceiveLink findLocalReceiveLink(MessageAddress id);
    Iterator findLocalMulticastReceivers(MulticastMessageAddress addr);
    Iterator findRemoteMulticastTransports(MulticastMessageAddress addr);
    void registerClient(MessageTransportClient client);
    void unregisterClient(MessageTransportClient client);
    boolean addressKnown(MessageAddress address);
    ArrayList getDestinationLinks(MessageAddress destination);
    AgentState getAgentState(MessageAddress agent);
    void removeAgentState(MessageAddress agent);
}