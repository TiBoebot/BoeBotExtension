import java.io.InputStream;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import bluej.extensions.BlueJ;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;


public class SetupPi extends JDialog {
	BlueJ bluej;
	
	JProgressBar progressBar;
	JLabel progressLabel;
	
	public static void main(String[] args)
	{
		new SetupPi(null, null);
	}
	
	public SetupPi(JPanel preferencesPanel, BlueJ bluej) {
		this.bluej = bluej;
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			
		setLocationRelativeTo(null);
		setSize(600, 150);

		JPanel panel = new JPanel(null);
		setContentPane(panel);
		
		panel.add(progressBar = new JProgressBar(0, 100));
		panel.add(progressLabel = new JLabel("..."));
		
		progressBar.setBounds(50, 10, 500, 20);
		progressLabel.setBounds(50, 65, 500, 50);
		
		
		
		new Thread(new Runnable() {
			public void run() {
				try
				{
					progressLabel.setText("Connecting...");
					JSch jsch = new JSch();
					Session session;
					if(SetupPi.this.bluej != null)
						session = jsch.getSession("pi", SetupPi.this.bluej.getExtensionPropertyString("BOEBOT-IP", "127.0.0.1"));//192.168.2.24");//TODO: add configurable host
					else
						session = jsch.getSession("pi", "192.168.137.205");//192.168.2.24");//TODO: add configurable host
					session.setPassword("pi");
					java.util.Properties config = new java.util.Properties(); 
					config.put("StrictHostKeyChecking", "no");
					session.setConfig(config);
					session.connect(1000);
					if(!session.isConnected())
					{
						progressLabel.setText("Unable to connect");
						return;
					}
					progressLabel.setText("Connected");
					
					String command = "wget -q \"http://svn.borf.info/avans/Kwartalen/Voltijd TI/TI-1.2 Voltijd/BoeBotExtension/BoeBotLib/install\" -O - | sudo sh";
					Channel execChannel = session.openChannel("exec");
					((ChannelExec) execChannel).setCommand(command);
					execChannel.setInputStream(null);
					// ((ChannelExec)channel).setErrStream(System.out);
					// ((ChannelExec)channel).setOutputStream(System.out);

					InputStream execIn = execChannel.getInputStream();
					InputStream execErr = ((ChannelExec)execChannel).getErrStream();

					
					execChannel.connect();						
					System.out.println("Running...");
					String line = "";
					while(true)
					{
						while(execIn.available() > 0)
							line += (char)execIn.read();
						while(execErr.available() > 0)
							line += (char)execErr.read();
						if(line.indexOf('\n') != -1)
						{
							progressLabel.setText(line.substring(0, line.indexOf('\n')));
							line = line.substring(line.indexOf('\n')+1);
						}
						if(line.equals("Done"))
							break;
						Thread.sleep(50);
					}
					dispose();
//`wget -q "http://svn.borf.info/avans/Kwartalen/Voltijd TI/TI-1.2 Voltijd/BoeBotExtension/BoeBotLib/build" -O -`					
					/*for(int i = 0; i < 100; i++)
					{
						progressBar.setValue(i);
						Thread.sleep(100);
					}*/
					
					
					
					
				} catch(Exception e)
				{
					e.printStackTrace();	
				}
			}
		}).start();
		
		setVisible(true);
	}

	
}
