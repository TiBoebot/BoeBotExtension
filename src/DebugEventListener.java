import com.sun.jdi.event.Event;
import com.sun.jdi.request.EventRequest;



interface DebugEventListener
{
	void onEvent(Event event, EventRequest request);
}