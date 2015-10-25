import com.sun.jdi.event.Event;
import com.sun.jdi.request.EventRequest;


class EventWaiter implements DebugEventListener
{
	private EventRequest request;
	private Object signaller = new Object();
	public EventWaiter(EventRequest request)
	{
		this.request = request;
	}

	public void onEvent(Event event, EventRequest request) {
		if(request == this.request)
			synchronized(signaller)
			{
				signaller.notify();
			}
	}
	
	public void waitForEvent()
	{
		System.out.println("Waiting for event...");
		try {
			synchronized(signaller)
			{
				signaller.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done waiting for event...");
	}
}