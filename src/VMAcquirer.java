import java.io.IOException;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Connector.Argument;


class VMAcquirer {

	/**
	 * Call this with the localhost port to connect to.
	 */
	public VirtualMachine connect(String ip, int port) throws IOException {
		String strPort = Integer.toString(port);
		AttachingConnector connector = getConnector();
		try {
			VirtualMachine vm = connect(connector, ip, strPort);
			return vm;
		} catch (IllegalConnectorArgumentsException e) {
			throw new IllegalStateException(e);
		}
	}

	private AttachingConnector getConnector() {
		VirtualMachineManager vmManager = Bootstrap.virtualMachineManager();
		for (Connector connector : vmManager.attachingConnectors()) {
			System.out.println(connector.name());
			if ("com.sun.jdi.SocketAttach".equals(connector.name())) {
				return (AttachingConnector) connector;
			}
		}
		throw new IllegalStateException();
	}

	private VirtualMachine connect(AttachingConnector connector, String ip, String port)
			throws IllegalConnectorArgumentsException, IOException {
		Map<String, Connector.Argument> args = connector.defaultArguments();
		Connector.Argument pidArgument = args.get("port");
		if (pidArgument == null) {
			throw new IllegalStateException();
		}
		pidArgument.setValue(port);

		Connector.Argument hostArgument = args.get("hostname");
		if (hostArgument == null) {
			throw new IllegalStateException();
		}
		hostArgument.setValue(ip);

		return connector.attach(args);
	}
}