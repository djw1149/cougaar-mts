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
import java.util.LinkedList;

import org.cougaar.core.component.Container;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.mts.std.AttributedMessage;

/**
 * An abstract class which manages a circular queue of messages, and
 * runs its own thread to pop messages off that queue.  The method
 * <strong>dispatch</strong>, provided by instantiable subclasses, is
 * invoked on each message as it's popped off. */
abstract class MessageQueue 
    extends BoundComponent
    implements Runnable
{

    // Simplified queue
    private static class SimpleQueue extends LinkedList {
	Object next() {
	    return removeFirst();
	}
    }

    
    private SimpleQueue queue;
    private Schedulable thread;
    private String name;
    private AttributedMessage pending;
    private Object pending_lock;


    MessageQueue(String name, Container container) {
	this.name = name;
	pending_lock = new Object();
	queue = new SimpleQueue();
    }



    int getLane() {
	return ThreadService.BEST_EFFORT_LANE;
    }

    public void load() {
	super.load();
	thread = threadService.getThread(this, this, name, getLane());
    }


    String getName() {
	return name;
    }


    public void removeMessages(UnaryPredicate pred, ArrayList removed) {
	synchronized (queue) {
	    Iterator itr = queue.iterator();
	    while (itr.hasNext()) {
		AttributedMessage msg = (AttributedMessage) itr.next();
		if (pred.execute(msg)) {
		    removed.add(msg);
		    itr.remove();
		}
	    }
	}
	synchronized (pending_lock) {
	    if (pending != null && pred.execute(pending)) {
		removed.add(pending);
		pending = null;
	    }
	}
    }



    private static final long HOLD_TIME = 500;

    // Process the last failed message, if any, followed by as many
    // items as possible from the queue, with a max time as given by
    // HOLD_TIME.
    public void run() {
	long endTime= System.currentTimeMillis() + HOLD_TIME;

	// Retry the last failed dispatch before looking at the
	// queue.
	synchronized (pending_lock) {
	    if (pending != null) {
		if (dispatch(pending)) {
		    pending = null;
		} else {
		    // The dispatch code has already scheduled the
		    // thread to run again later
		    return;
		}
	    }
	}

	// Now process the queued items. 
	AttributedMessage message;
	while (System.currentTimeMillis() <= endTime) {
	    synchronized (queue) {
		if (queue.isEmpty()) break; // done for now
		message = (AttributedMessage) queue.next(); // from top
	    }


	    if (message == null || dispatch(message)) {
		// Processing succeeded, continue popping the queue
		continue;
	    } else {
		// Remember the failed message.  The dispatch code
		// has already scheduled the thread to run again later
		pending = message;
		return;
	    }
	}
	restartIfNotEmpty();

    }

    // Restart the thread immediately if the queue is not empty.
    private void restartIfNotEmpty() {
	synchronized (queue) {
	    if (!queue.isEmpty()) thread.start();
	}
    }

    void scheduleRestart(int delay) {
	thread.schedule(delay);
    }

    /** 
     * Enqueue a message. */
    void add(AttributedMessage message) {
	synchronized (queue) {
	    queue.add(message);
	}
	thread.start();
    }


     public AttributedMessage[] snapshot() 
     {
         AttributedMessage head;
         synchronized (pending_lock) {
             head = pending;
         }
         synchronized (queue) {
             int size = queue.size();
             if (head != null) size++;
	     AttributedMessage[] ret = new AttributedMessage[size];
             int i = 0;
	     Iterator iter = queue.iterator();
             if (head != null) ret[i++] = head;
	     while (i < size) ret[i++] = (AttributedMessage) iter.next();
             return ret;
         }
     }

    /**
     * Process a dequeued message.  Return value indicates success or
     * failure or the dispatch.  Failed dispatches will be tried again
     * before any further queue entries are dispatched. */
    abstract boolean dispatch(AttributedMessage m);

    /**
     * Number of messages waiting in the queue.
     */
    public int size (){
	return queue.size();
    }
      
}
