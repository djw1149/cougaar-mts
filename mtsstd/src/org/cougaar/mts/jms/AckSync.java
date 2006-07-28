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
package org.cougaar.mts.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.cougaar.core.mts.MessageAttributes;

/**
 * @author rshapiro
 *
 */
public class AckSync {
    private static final String ID_PROP = "JMS_MSG_ID";
    private static int ID = 0;
    
    private final Destination originator;
    private final Session session;
    private final Map producers;
    private final Map unacked;
    private final Map ackData;
    
    AckSync(Destination originator, Session session) {
	this.originator = originator;
	this.session = session;
	this.producers = new HashMap();
	this.unacked = new HashMap();
	this.ackData = new HashMap();
    }
    
    MessageAttributes sendMessage(Message msg, MessageProducer producer) throws JMSException {
	msg.setJMSReplyTo(originator);
	Integer id = new Integer(++ID);
	msg.setIntProperty(ID_PROP, id.intValue());
	Object lock = new Object();
	unacked.put(id, lock);
	synchronized (lock) {
	    producer.send(msg);
	    while (true) {
		try {
		    lock.wait();
		    break;
		} catch (InterruptedException ex) {
		    
		}
	    }
	}
	// System.err.println(id + " woke up!");
	MessageAttributes attrs = (MessageAttributes) ackData.get(id);
	ackData.remove(id);
	unacked.remove(id);
	/*
	 *  metadata = new MessageReply(message);
	    metadata.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE, 
		    MessageAttributes.DELIVERY_STATUS_DELIVERED);
	 */
	return attrs;
    }
    
    void ackMessage(ObjectMessage omsg, MessageAttributes reply) throws JMSException {
	Ack ack = new Ack(reply, omsg.getIntProperty(ID_PROP));
	ObjectMessage ackMsg = session.createObjectMessage(ack);
	Destination dest = omsg.getJMSReplyTo();
	MessageProducer producer = (MessageProducer) producers.get(dest);
	if (producer == null) {
	    producer = session.createProducer(dest);
	    producers.put(dest, producer);
	}
	producer.send(ackMsg);
    }
    
    boolean isAck(ObjectMessage msg) {
	try {
	    Object raw = msg.getObject();
	    if (raw instanceof Ack) {
		Ack ack = (Ack) raw;
		Integer id = new Integer(ack.getId());
		ackData.put(id, ack.getAttrs());
		Object lock = unacked.get(id);
		if (lock != null) {
		    synchronized (lock) {
			lock.notify();
		    }
		}
		return true;
	    } else {
		return false;
	    }
	} catch (JMSException e) {
	   return false;
	}
    }

}
