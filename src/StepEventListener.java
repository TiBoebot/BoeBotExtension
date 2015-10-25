import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.EventRequest;


public class StepEventListener implements DebugEventListener {
	private BoeBotDebugger boeBotDebugger;

	public StepEventListener(BoeBotDebugger boeBotDebugger) {
		this.boeBotDebugger = boeBotDebugger;
	}

	public void onEvent(Event event, EventRequest request) {
		if(event instanceof StepEvent)
		{
			request.disable();
			boeBotDebugger.stepRequest = null;
			boeBotDebugger.updateView();
		}
		if(event instanceof BreakpointEvent)
		{
			boeBotDebugger.updateView();
		}

		
	}

}
