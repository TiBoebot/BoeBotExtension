import java.util.ArrayList;
import java.util.Iterator;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.StepRequest;


class EventReader implements Runnable
{
	EventQueue queue;
	Thread thread;
	ArrayList<DebugEventListener> listeners = new ArrayList<DebugEventListener>();
	boolean running = true;
	
	public EventReader(EventQueue queue)
	{
		this.queue = queue;
		thread = new Thread(this);
		thread.start();
	}
	public void run() {
		while(running)
		{
			try {
				EventSet events = queue.remove();

				Iterator<Event> it = events.iterator();
				while(it.hasNext())
				{
					Event event = it.next();
					EventRequest request = event.request();
					handleEvent(event, request);	
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void handleEvent(Event event, EventRequest request) {
		for(DebugEventListener l : listeners)
			l.onEvent(event, request);
	}
	public void waitForRequest(StepRequest request) {
		EventWaiter waiter = new EventWaiter(request);
		listeners.add(waiter);
		waiter.waitForEvent();
		listeners.remove(waiter);
	}
	
	
	public void stop()
	{
		running = false;
	}
	public void add(DebugEventListener eventListener) {
		listeners.add(eventListener);
	}
}