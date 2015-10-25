import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class BoeBotDiscoverScreen extends JFrame {
	private static final long serialVersionUID = -813823263030923419L;
	JPanel bottomPanel;
	JList<String> list;
	JButton btnOk, btnCancel;
	
	
	public static void main(String[] args) {
		new BoeBotDiscoverScreen(null);
	}

	public BoeBotDiscoverScreen(final BoeBotExtension boeBotExtension) {
		super("Boebots in network");
		setSize(350, 300);
		if(boeBotExtension == null)
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		else
			setLocationRelativeTo(boeBotExtension.preferencesPanel);

		JPanel mainPanel = new JPanel(new BorderLayout());
		
		mainPanel.add(new JScrollPane(list = new JList<String>(new DefaultListModel<String>())), BorderLayout.CENTER);
		mainPanel.add(bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)), BorderLayout.SOUTH);
		bottomPanel.add(btnCancel = new JButton("Cancel"));
		bottomPanel.add(btnOk = new JButton("Ok"));
		
		((DefaultListModel<String>)list.getModel()).addElement("loading...");
		list.setEnabled(false);

		setContentPane(mainPanel);
		
		setVisible(true);
		
		new Thread(new Runnable() {
			public void run() {
				
				Set<String> items = discover();
				((DefaultListModel<String>)list.getModel()).clear();
				for(String element : items)
					((DefaultListModel<String>)list.getModel()).addElement(element);;
				list.setEnabled(true);
			}
		}).start();
		
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(list.getSelectedValue() != null)
				{
					String ip = list.getSelectedValue();
					ip = ip.substring(4);
					ip = ip.substring(0, ip.indexOf(' '));
					boeBotExtension.ipField.setText(ip);
				}
				dispose();
			}
		});
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		list.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if(e.getClickCount() == 2)
					btnOk.doClick();
			}
			
		});
	}

	public Set<String> discover() {
		Set<String> response = new HashSet<String>();
		DatagramSocket c = null;
		try {
			c = new DatagramSocket();
			c.setBroadcast(true);

			byte[] sendData = "BOEBOT1.0_DISCOVER".getBytes();

			try {
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
				c.send(sendPacket);
				System.out.println(getClass().getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Broadcast the message over all the network interfaces
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();

				if (networkInterface.isLoopback() || !networkInterface.isUp()) {
					continue; // Don't want to broadcast to the loopback
								// interface
				}

				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcast = interfaceAddress.getBroadcast();
					if (broadcast == null) {
						continue;
					}

					// Send the broadcast package!
					try {
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
						c.send(sendPacket);
					} catch (Exception e) {
					}

					System.out.println(getClass().getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
				}
			}
			System.out.println(getClass().getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

			c.setSoTimeout(1000);
			for (int i = 0; i < 10; i++) {
				byte[] recvBuf = new byte[15000];
				DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
				c.receive(receivePacket);

				String message = new String(receivePacket.getData()).trim();
				// We have a response
				System.out.println(getClass().getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress() + ", " + message);

				// Check if the message is correct
				if (message.substring(0, 13).equals("BOEBOT1.0_ACK")) {
					response.add("IP: " + receivePacket.getAddress().toString().substring(1) + "     Serial " + message.substring(14));
				}
				else
					System.out.println("not a boebot");
			}

			// Close the port!
		} catch (SocketTimeoutException e) {
			// expected
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (c != null)
				c.close();
		}
		return response;
	}
}
