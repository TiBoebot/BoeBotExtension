import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;


public class BoeBotControlFrame extends JFrame implements ActionListener {


	String packageDirectory;
	
	JComboBox<String> mainClass;
	JComboBox<String> versions;
	JTextArea log;
	JLabel statusLabel;
	
	
	Session session = null;
	JSch jsch = null;
	Channel execChannel = null;
	InputStream execIn = null;
	InputStream execErr = null;
	
	Thread connectThread = null;
	BlueJ bluej;
	BoeBotExtension extension = null;
	String projectName;
	
	public BoeBotControlFrame(final String packageDirectory, final String projectName, String mainClass, BoeBotExtension extension) 
	{
		super("Boebot monitor for " + projectName + " in path " + packageDirectory);
		this.projectName = projectName;
		this.packageDirectory = packageDirectory;
		this.extension = extension;
		if(extension != null)
			this.bluej = extension.bluej;
		setSize(800, 600);
		if(bluej == null)
			setDefaultCloseOperation(EXIT_ON_CLOSE);
		JPanel panel = new JPanel(new BorderLayout());
		setContentPane(panel);

		
		panel.add(new JScrollPane(log = new JTextArea(25, 80)), BorderLayout.CENTER);
		log.setFont(new Font("Monospaced", Font.PLAIN, 14));
		log.setEditable(false);
		log.setBorder(BorderFactory.createEtchedBorder());
		
		
		JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		panel.add(statusBar, BorderLayout.SOUTH);
		
		statusBar.add(statusLabel = new JLabel("Connecting..."));
		statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
		statusLabel.setMinimumSize(new Dimension(400, 20));
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
		panel.add(topPanel, BorderLayout.NORTH);

		topPanel.add(new JLabel("Main Class"));
		topPanel.add(this.mainClass = new JComboBox<String>(new String[] { mainClass } ));
		topPanel.add(this.versions = new JComboBox<String>());
		
		
		
		final JButton uploadButton = new JButton("Upload");
		final JButton runButton = new JButton("Run");
		final JButton debugButton = new JButton("Debug");

		statusLabel.setText("Not connected");
		debugButton.setEnabled(false);
		uploadButton.setEnabled(false);
		runButton.setEnabled(false);

		this.mainClass.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				if(BoeBotControlFrame.this.extension != null)
				{
					ArrayList<String> mainClasses = BoeBotControlFrame.this.extension.getMainClasses();
					BoeBotControlFrame.this.mainClass.removeAllItems();
					for(String item : mainClasses)
						BoeBotControlFrame.this.mainClass.addItem(item);
					

				}
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});
		
		
		this.versions.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				String selected = (String) versions.getSelectedItem();
				versions.removeAllItems();
				for(String item : getVersions())
					versions.addItem(item);
				if(selected != null)
					versions.setSelectedItem(selected);
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
			}
			public void popupMenuCanceled(PopupMenuEvent arg0) {
			}
		});
		
		
		topPanel.add(uploadButton);
		uploadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				(new Thread()
				{
					public void run()
					{
						if(!BoeBotControlFrame.this.isVisible())
							return;
						if(session.isConnected())
						{
							log.setText("");
							if(BoeBotControlFrame.this.bluej != null)
							{
								BlueJ b = BoeBotControlFrame.this.bluej;
								BProject[] projects =  b.getOpenProjects();
								if(projects.length == 0)
									return;
								BProject project = projects[0];
								BPackage[] packages;
								try {
									packages = project.getPackages();
									packages[0].compile(true);
									
									//TODO: check if main method is still valid
								} catch (ProjectNotOpenException e) {
									e.printStackTrace();
								} catch (PackageNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (CompilationNotStartedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							DateFormat df = new SimpleDateFormat("yyyy-MM-dd_hh.mm.ss");
							Date now = Calendar.getInstance().getTime();        
							String version = df.format(now);
							
							versions.addItem(version);
							versions.setSelectedItem(version);
							
							uploadFiles();
							runCode();
						}
					}
				}).start();
			}
		});
		
		topPanel.add(runButton);
		
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(session.isConnected())
				{
					log.setText("");
					runCode();
				}
			}
		});
		
		topPanel.add(debugButton);
		
		debugButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(session.isConnected())
				{
					log.setText("");
			//		runCode(true);
			//		delay(250);
					new BoeBotDebugger(packageDirectory, bluej);
				}
			}
		});
		
		
		
		
		new Timer(10, this).start();
		
	
		jsch = new JSch();
		session = null;

		
		
		connectThread = new Thread()
		{
			public void run()
			{
				while(true)
				{
					try {
						if(session == null || !session.isConnected())
						{
							statusLabel.setText("Not connected");
							debugButton.setEnabled(false);
							uploadButton.setEnabled(false);
							runButton.setEnabled(false);

							
							if(session != null)
								session.disconnect();
							
							String ip = "10.10.10.1";							
							
							if(BoeBotControlFrame.this.bluej != null)
								ip = BoeBotControlFrame.this.bluej.getExtensionPropertyString("BOEBOT-IP", "10.10.10.1");
							System.out.println("Connecting to " + ip);
							session = jsch.getSession("pi", ip);

							session.setPassword("pi");
							java.util.Properties config = new java.util.Properties(); 
							config.put("StrictHostKeyChecking", "no");
							session.setConfig(config);
							
							session.connect(1000);
							
							if(session.isConnected())
							{
								statusLabel.setText("Connected");
								//debugButton.setEnabled(true);
								uploadButton.setEnabled(true);
								runButton.setEnabled(true);
							}
						}
						else
							session.sendKeepAliveMsg();
							
					} catch (JSchException e) {
						e.printStackTrace();
						session = null;
					} catch (Exception e) {
						session = null;
						e.printStackTrace();
					}
					delay(2000);
				}
			}
		};
		connectThread.start();
		
		
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				connectThread.stop();
			}
			public void windowClosed(WindowEvent e) {
				connectThread.stop();
			}
		}); 
			
		
		pack();
		setVisible(true);
	}
	
	void runCode()
	{
		runCode(false);
	}
	
	void runCode(boolean suspend)
	{
		try {
			if(execChannel != null)
			{
				execIn.close();
				execChannel.disconnect();
			}
			
			String command = "cd /home/pi/upload/"+projectName + "/" + versions.getSelectedItem()+"; sudo killall -q java; sudo java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend="+(suspend ? "y" : "n")+" -cp \".:/home/pi/BoeBotLib/BoeBotLib.jar\" -Djava.library.path=/home/pi/BoeBotLib " + BoeBotControlFrame.this.mainClass.getSelectedItem();
			
			execChannel = session.openChannel("exec");
			((ChannelExec) execChannel).setCommand(command);
			execChannel.setInputStream(null);
			// ((ChannelExec)channel).setErrStream(System.out);
			// ((ChannelExec)channel).setOutputStream(System.out);

			execIn = execChannel.getInputStream();
			execErr = ((ChannelExec)execChannel).getErrStream();

			
			execChannel.connect();	
			log.append("Running " + mainClass.getSelectedItem() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSchException e) {
			e.printStackTrace();
		}		
	}
	
	//timer
	public void actionPerformed(ActionEvent arg0) {
		byte[] tmp = new byte[1024];
		
		if(execChannel != null && execChannel.isConnected())
		{
			try {
				while (execIn.available() > 0) {
					int i = execIn.read(tmp, 0, 1024);
					if (i < 0)
						break;
					if(!new String(tmp, 0, i).equals("Listening for transporyt dt_socket at address: 8000\n"))
						log.append(new String(tmp, 0, i));
				}
				while (execErr.available() > 0) {
					int i = execErr.read(tmp, 0, 1024);
					if (i < 0)
						break;
					log.append(new String(tmp, 0, i));
				}
				
				if (execChannel.isClosed()) {
					if (execIn.available() > 0 && execErr.available() > 0)
						return;
					System.out.println("exit-status: "
							+ execChannel.getExitStatus());
					return;
				}		
			} catch (IOException e) {
				e.printStackTrace();
				execIn = null;
				execErr = null;
				execChannel.disconnect();
				session.disconnect();
				session = null;
			}
		}
	}
	
	
	void uploadFiles()
	{
		final ArrayList<Path> files = new ArrayList<Path>();
		try {
			Files.walkFileTree(Paths.get(packageDirectory), new FileVisitor<Path>() {
				public FileVisitResult postVisitDirectory(Path arg0, IOException arg1) throws IOException {
					return FileVisitResult.CONTINUE;
				}
				public FileVisitResult preVisitDirectory(Path arg0, BasicFileAttributes arg1) throws IOException {
					return FileVisitResult.CONTINUE;
				}
				public FileVisitResult visitFile(Path arg0,BasicFileAttributes arg1) throws IOException {
					if(arg0.toString().endsWith(".class") || arg0.toString().endsWith(".java"))
						files.add(arg0);
					return FileVisitResult.CONTINUE;
				}
				public FileVisitResult visitFileFailed(Path arg0, IOException arg1) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		String version = (String) versions.getSelectedItem();
		log.append("New version is called '" + version + "'\n");
		log.append("Uploading " + files.size() + " files....");
		mkdir("/home/pi/upload/" +projectName, session);
		mkdir("/home/pi/upload/" +projectName+"/"+version, session);
		for(Path p : files)
		{
			if(Paths.get(packageDirectory).relativize(p).getParent() != null)
				mkdir("/home/pi/upload/" +projectName + "/" + version + "/" +  Paths.get(packageDirectory).relativize(p).getParent().toString().replace('\\', '/'), session);
			sendFile(p.toString(), "/home/pi/upload/" + projectName + "/" + version + "/" + Paths.get(packageDirectory).relativize(p).toString().replace('\\', '/'), session);
			log.append(".");
		}
		log.append("done\n");
	}
	
	
	private void mkdir(String path, Session session) {
		//statusLog.append("Making path " + path + "\n");
		
		String command = "mkdir " + path;
		try {
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			channel.connect();
			while(channel.isConnected())
				delay(1);
				
			channel.disconnect();

		} catch (JSchException e) {
			e.printStackTrace();
		}

		
	}




	void sendFile(String lfile, String rfile, Session session) {
		try {
			//statusLog.append("Uploading " + lfile + " to " + rfile + "\n"); // out.flush();
			boolean ptimestamp = true;

			// exec 'scp -t rfile' remotely
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				System.exit(0);
			}

			File _lfile = new File(lfile);

			if (ptimestamp) {
				command = "T" + (_lfile.lastModified() / 1000) + " 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					return;
				}
			}

			// send "C0644 filesize filename", where filename should not
			// include '/'
			long filesize = _lfile.length();
			command = "C0666 " + filesize + " ";
			if (lfile.lastIndexOf('/') > 0) {
				command += lfile.substring(lfile.lastIndexOf('/') + 1);
			} else {
				command += lfile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				return;
			}

			// send a content of lfile
			FileInputStream fis;
			fis = new FileInputStream(lfile);

			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				return;
			}
			out.close();

			channel.disconnect();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				log.append(sb.toString()); // out.flush();
			}
			if (b == 2) { // fatal error
				log.append(sb.toString()); // out.flush();
			}
		}
		return b;
	}		
	
	
	void delay(int ms)
	{
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> getVersions() {
		ArrayList<String> result = new ArrayList<String>();

			String command = "ls /home/pi/upload/" + projectName;
			try {
				Channel channel = session.openChannel("exec");
				((ChannelExec) channel).setCommand(command);
				// get I/O streams for remote scp
				OutputStream out = channel.getOutputStream();
				InputStream in = channel.getInputStream();
				channel.connect();
				
				long timeout = System.currentTimeMillis() + 1000;
				
				while(in.available() == 0 && System.currentTimeMillis() < timeout)
					delay(1);

				String name = "";
				while(in.available() > 0)
				{
					int i = in.read();
					if(i == '\n')
					{
						result.add(name);
						name = "";
					}
					else
						name += (char)i;
				}
				if(name != "")
					result.add(name);

				channel.disconnect();

			} catch (JSchException | IOException e) {
				e.printStackTrace();
			}
		
		return result;
	}
	
	
}
