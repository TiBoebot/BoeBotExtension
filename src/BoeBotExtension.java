import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.extensions.BClass;
import bluej.extensions.BMethod;
import bluej.extensions.BPackage;
import bluej.extensions.BlueJ;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.Extension;
import bluej.extensions.MenuGenerator;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.PreferenceGenerator;
import bluej.extensions.ProjectNotOpenException;


public class BoeBotExtension extends Extension implements PreferenceGenerator
{
	public static void main(String[] args)
	{
		new BoeBotControlFrame("C:/Users/johan_000/Desktop/Led", "Led", "Afstandsbediening", null);
	}
	BlueJ bluej;
	
	JPanel preferencesPanel;
	JTextField ipField;
	JButton scanForPi;
	
	public String getName() {
		return "Avans BoeBot Extension";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

	@Override
	public boolean isCompatible() {
		return true;
	}

	public void startup(BlueJ bluej) {
		this.bluej = bluej;
		
		preferencesPanel = new JPanel();
		preferencesPanel.add(new JLabel("IP"));
		preferencesPanel.add(ipField = new JTextField(40));
		preferencesPanel.add(scanForPi = new JButton("Scan"));
		
		final BlueJ b = bluej;
		/*setupPi.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				if(JOptionPane.showConfirmDialog(preferencesPanel, "This will reinstall the BoeBot library on your pi. Are you sure?") == JOptionPane.YES_OPTION)
				{
					new SetupPi(preferencesPanel, b);	
				}
			}
		});*/
		
		scanForPi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new BoeBotDiscoverScreen(BoeBotExtension.this);
			}
		});
		
		
		
		loadValues();
		bluej.setPreferenceGenerator(this);
		bluej.setMenuGenerator(new MenuGenerator()
		{

			public JMenuItem getToolsMenuItem(BPackage bp) {
				JMenuItem item = new JMenuItem("Upload to BoeBot");
				final BPackage bpackage = bp;
				item.addActionListener(new ActionListener() 
				{
					public void actionPerformed(ActionEvent event)
					{
						new Thread()
						{
							public void run()
							{
								try {
									ArrayList<String> mainClasses = getMainClasses();
									if(mainClasses.isEmpty())
										JOptionPane.showMessageDialog(null, "Could not find main class\nPlease create a class with a \npublic static void main(String[] args)\n{\n}\nmethod");
									else
										new BoeBotControlFrame(bpackage.getDir().toString(), BoeBotExtension.this.bluej.getCurrentPackage().getProject().getName().replaceAll(" ",  "_"), mainClasses.get(0), BoeBotExtension.this);
								} catch (ProjectNotOpenException | PackageNotFoundException e) {
									e.printStackTrace();
								}
							}
						}.start();
					}
				});
				return item;
			}
			
		});
	}

	@Override
	public String getDescription() {
		return "Avans BoeBot connector. Connects BlueJ to the BoeBot, allowing you to experiment with robotics";
	}

	@Override
	public URL getURL() {
		return super.getURL();
	}

	public JPanel getPanel() {
		return preferencesPanel;
	}

	public void loadValues() {
		ipField.setText(bluej.getExtensionPropertyString("BOEBOT-IP", "10.10.10.1"));
	}

	public void saveValues() {
		bluej.setExtensionPropertyString("BOEBOT-IP", ipField.getText());
	}

	
	
	public ArrayList<String> getMainClasses()
	{
		ArrayList<String> mainClasses = new ArrayList<String>();
		try {
			BPackage bpackage = bluej.getCurrentPackage();
			bpackage.compile(false);
			BClass[] classes = bpackage.getClasses();
			for(BClass c: classes)
			{
				BMethod[] methods = c.getMethods();
				for(BMethod m : methods)
				{
					if(m.getName().equals("main") && m.getModifiers() == (java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC))
						mainClasses.add(c.getName());
				}
			}
		} catch (ProjectNotOpenException e) {
			e.printStackTrace();
		} catch (PackageNotFoundException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (CompilationNotStartedException e) {
			e.printStackTrace();
		}
		return mainClasses;			
	}
	
}
