import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;

import bluej.extensions.BlueJ;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.StepRequest;

public class BoeBotDebugger extends JFrame {
	
	private static final long serialVersionUID = 2879121929287423218L;
	public static String ip = "192.168.137.139"; 
	
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		new BoeBotDebugger("", null).setDefaultCloseOperation(EXIT_ON_CLOSE);
		
	}

	JSplitPane panel;
	JList<String> sourceFiles;
	private JTable watch;
	private JList<String> callstack;
	ArrayList<ClassDebugView> openFiles = new ArrayList<ClassDebugView>();
	JTabbedPane sourceFilesTabs;
	JLabel statusLabel;
	public VirtualMachine vm;
	private JButton stepInBtn;
	private JButton stepOverBtn;
	private JButton stepOutBtn;
	private JButton resumeBtn;
	private JButton breakBtn;
	public boolean running = false;
	public List<String> watchVariables = new ArrayList<String>();
	public List<String> watchValues = new ArrayList<String>();
	
	StepRequest stepRequest = null;
	

	private int callstackFrame = 0;
	private EventReader eventReader;
	
	public BoeBotDebugger(String packageDirectory, BlueJ bluej) {
		if(bluej != null)
		{
			ip = bluej.getExtensionPropertyString("BOEBOT-IP", "192.168.137.139");
		}
		this.setSize(1024, 768);
		this.setContentPane(panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT));
		sourceFilesTabs = new JTabbedPane();
		
		panel.setTopComponent(sourceFilesTabs);

	
		JPanel bottom = new JPanel(new BorderLayout());
		panel.setBottomComponent(bottom);
		panel.setDividerLocation(getHeight() - 200);

		JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		toolBar.add(stepInBtn = new JButton("Step in"));
		toolBar.add(stepOverBtn = new JButton("Step over"));
		toolBar.add(stepOutBtn = new JButton("Step out"));
		toolBar.add(resumeBtn = new JButton("Resume"));
		toolBar.add(breakBtn = new JButton("Break"));
		toolBar.add(statusLabel = new JLabel("Paused..."));
		bottom.add(toolBar, BorderLayout.NORTH);

		JSplitPane callstackWatch = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		callstackWatch.setDividerLocation((getWidth() - 200) / 2);
		bottom.add(callstackWatch, BorderLayout.CENTER);

		callstackWatch.setLeftComponent(new JScrollPane(callstack = new JList<String>()));
		callstackWatch.setRightComponent(new JScrollPane(watch = new JTable(new AbstractTableModel() {
			public int getColumnCount() {
				return 2;
			}
			public int getRowCount() {
				synchronized(watchVariables)
				{
					return watchVariables.size();
				}
			}

			@Override
			public String getColumnName(int arg0) {
				return new String[] { "Name", "Value" }[arg0];
			}
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				synchronized(watchVariables)
				{
					if(rowIndex >= watchVariables.size() || rowIndex >= watchValues.size())
						return "-";
					
					if(columnIndex == 0)
						return watchVariables.get(rowIndex);
					if(columnIndex == 1)
						return watchValues.get(rowIndex);
					return "";
				}
			}
		})));

	
		//openFile("Led.java");
		
		
		resumeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(vm == null)
					return;
				vm.resume();
				updateView();
			}
		});
		
		breakBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(vm == null)
					return;
				vm.suspend();
				updateView();
			}
		});
		
		stepInBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(vm == null)
					return;
				if(stepRequest != null)
				{
					stepRequest.disable();
				}
				stepRequest = vm.eventRequestManager().createStepRequest(getMainThread(), StepRequest.STEP_LINE, StepRequest.STEP_INTO);
				stepRequest.addClassExclusionFilter("java.*");
				stepRequest.addClassExclusionFilter("javax.*");
				stepRequest.addClassExclusionFilter("sun.*");
				stepRequest.addClassExclusionFilter("com.sun.*");
				stepRequest.addClassExclusionFilter("com.ibm.*");
				stepRequest.addClassExclusionFilter("TI.*");
				stepRequest.enable();
				updateView();
				vm.resume();
			}
		});
		stepOverBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(vm == null)
					return;
				if(stepRequest != null)
				{
					stepRequest.disable();
				}
				stepRequest = vm.eventRequestManager().createStepRequest(getMainThread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER);
				stepRequest.addClassExclusionFilter("java.*");
				stepRequest.addClassExclusionFilter("javax.*");
				stepRequest.addClassExclusionFilter("sun.*");
				stepRequest.addClassExclusionFilter("com.sun.*");
				stepRequest.addClassExclusionFilter("com.ibm.*");
				stepRequest.addClassExclusionFilter("TI.*");
				stepRequest.enable();
				updateView();
				vm.resume();
			}
		});
		
		stepOutBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(vm == null)
					return;
				if(stepRequest != null)
				{
					stepRequest.disable();
				}
				stepRequest = vm.eventRequestManager().createStepRequest(getMainThread(), StepRequest.STEP_LINE, StepRequest.STEP_OUT);
				stepRequest.addClassExclusionFilter("java.*");
				stepRequest.addClassExclusionFilter("javax.*");
				stepRequest.addClassExclusionFilter("sun.*");
				stepRequest.addClassExclusionFilter("com.sun.*");
				stepRequest.addClassExclusionFilter("com.ibm.*");
				stepRequest.addClassExclusionFilter("TI.*");
				stepRequest.enable();
				updateView();
				vm.resume();
			}
		});
		
		callstack.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}

			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2)
				{
					callstackFrame = callstack.getSelectedIndex();
					updateView();
				}
			}
		});
		
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setVisible(true);

		for(int i = 0; i < 10; i++)
		{
			try {
				System.out.println("Connecting debugger to " + ip + ":8000");
				vm = new VMAcquirer().connect(ip, 8000);
				vm.suspend();
				
				eventReader = new EventReader(vm.eventQueue());
				
				eventReader.add(new StepEventListener(this));
	
				updateView();
				break;
	
			} catch (IOException e) {
				statusLabel.setText("Could not connect debug session:");
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}	
		
		addWindowListener(new WindowListener() {
			public void windowOpened(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
			public void windowClosing(WindowEvent e) {
				if(eventReader != null)
					eventReader.stop();
				vm.dispose();
			}
			public void windowClosed(WindowEvent e) {
				if(eventReader != null)
					eventReader.stop();
				vm.dispose();
			}
			public void windowActivated(WindowEvent e) {}
		});
	
	}
	
	private synchronized String stringValue(Value v)
	{
		if (v instanceof BooleanValue)
			return ((BooleanValue) v).value() + "";
		if (v instanceof ByteValue)
			return ((ByteValue) v).value() + "";
		if (v instanceof CharValue)
			return ((CharValue) v).value() + "";
		if (v instanceof DoubleValue)
			return ((DoubleValue) v).value() + "";
		if (v instanceof FloatValue)
			return ((FloatValue) v).value() + "";
		if (v instanceof IntegerValue)
			return ((IntegerValue) v).value() + "";
		if (v instanceof LongValue)
			return ((LongValue) v).value() + "";
		if (v instanceof ShortValue)
			return ((ShortValue) v).value() + "";
		else if (v instanceof StringReference)
			return ((StringReference)v).value();
		else if (v instanceof ArrayReference)
		{
			String ret = "[";
			for(Value val : ((ArrayReference)v).getValues())
				ret += stringValue(val) + ", ";
			if(!((ArrayReference)v).getValues().isEmpty())
				ret = ret.substring(0, ret.length()-2);
			return ret + "]";
		}
		else if (v instanceof ObjectReference)
		{
			ObjectReference value = (ObjectReference)v;
			String ret = "{";
			for(Field f : value.referenceType().allFields())
			{
				ret += f.name() + " : " + value.getValue(f) + ", ";
			}			
			if(!(value.referenceType().allFields().isEmpty()))
				ret = ret.substring(0, ret.length()-2);
			return ret + "}";
			
		}
		else
			return "Unknown value: " + v.toString();
	}
	
	
	private synchronized ThreadReference getMainThread() {
		List<ThreadReference> threads = vm.allThreads();
		for (ThreadReference thread : threads) {
			if(thread.name().equals("main"))
				return thread;
		}
		return null;
	}
	
	
	public synchronized void updateView() {
		statusLabel.setText("Paused...");
		running = false;
		
		try {
			ThreadReference thread = getMainThread();
			String[] callstackData;
				callstackData = new String[thread.frames().size()];
			int i = 0;
			for (StackFrame frame : thread.frames())
			{
				callstackData[i] = frame.location().sourcePath() + ":" + frame.location().lineNumber() + " " + frame.location().method().name() + "(";
				try
				{
					for(LocalVariable arg : frame.location().method().arguments())
					{
						String type = arg.typeName();
						if(type.contains("."))
							type = type.substring(type.lastIndexOf('.')+1);
						callstackData[i] += type + " " + arg.name() + ", ";
					}
					if(frame.location().method().arguments().isEmpty())
						callstackData[i] += ")";
					else
						callstackData[i] = callstackData[i].substring(0, callstackData[i].length()-2) + ")";
				}
				catch(AbsentInformationException e)
				{
					callstackData[i] += "???)";
				}
				i++;
			}
			callstack.setListData(callstackData);
		} catch (IncompatibleThreadStateException e) {
			callstack.setListData(new String[] {});
			running = true;
			statusLabel.setText("Running...");
//			e.printStackTrace();
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		}
		
		if(callstackFrame >= callstack.getModel().getSize())
			callstackFrame = 0;
		
		for(int i = 0; i < callstack.getModel().getSize(); i++)
		{
			String name = callstack.getModel().getElementAt(i);
			name = name.substring(0, name.lastIndexOf('('));
			name = name.substring(0, name.lastIndexOf(' '));
			String fileName = name.substring(0, name.lastIndexOf(':'));
			int line = Integer.parseInt(name.substring(name.lastIndexOf(':')+1));
			
			if(line == -1)
				continue;
			if(i < callstackFrame)
				continue;
			
			try {
				ClassDebugView view = null;
				for(ClassDebugView v : openFiles)
					if(v.fileName.equals(fileName.toLowerCase()))
							view = v;
				if(view == null)
					openFile(fileName);
				for(ClassDebugView v : openFiles)
					if(v.fileName.equals(fileName.toLowerCase()))
							view = v;
				
				if(view != null)
				{
					sourceFilesTabs.setSelectedComponent(view.codeScrollPane);
					view.codeArea.removeAllLineHighlights();
					view.codeArea.addLineHighlight(line-1, Color.pink);
					view.codeArea.scrollRectToVisible(new Rectangle(0, view.codeArea.getLineHeight() * line, 10,100));
				}
				break;
			} catch (Exception e) {
				//e1.printStackTrace();
			}
		}		
		
	//	synchronized(watchVariables)
		{
			watchVariables.clear();
			watchValues.clear();
			ThreadReference mainThread = getMainThread();
			try {
				List<LocalVariable> variables = mainThread.frame(callstackFrame).visibleVariables();
				System.out.println(callstackFrame);
				
				watchVariables = new ArrayList<String>();
				watchValues = new ArrayList<String>();
				for(LocalVariable v : variables)
				{
					if(watchVariables.contains(v.name()))
							continue;
					watchVariables.add(v.name());
					
					if(callstackFrame < mainThread.frameCount() && callstackFrame >= 0)
						watchValues.add(stringValue(mainThread.frame(callstackFrame).getValue(v)));
				}
				
				((AbstractTableModel)watch.getModel()).fireTableDataChanged();
			
			} catch (AbsentInformationException | IncompatibleThreadStateException | IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}		
		
		stepInBtn.setEnabled(!running);
		
		int line = -1;
		if(callstack.getModel().getSize() > 0)
		{
			String name = callstack.getModel().getElementAt(0);
			name = name.substring(0, name.lastIndexOf('('));
			name = name.substring(0, name.lastIndexOf(' '));
			line = Integer.parseInt(name.substring(name.lastIndexOf(':')+1));
		}		
		stepOverBtn.setEnabled(!running && line != -1);
		stepOutBtn.setEnabled(!running);
		resumeBtn.setEnabled(!running);
		breakBtn.setEnabled(running);
		
		
	}
	private synchronized void openFile(final String filename) {
		try {
			System.out.println(filename);
			final ClassDebugView debugView = new ClassDebugView();
			debugView.fileName = new File(filename.toLowerCase()).getName();
			
			debugView.codeArea = new RSyntaxTextArea(20, 60);
			debugView.codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			debugView.codeArea.setCodeFoldingEnabled(true);
			List<String> lines = Files.readAllLines(Paths.get("c:/users/johan/desktop/led/" + filename));
			String str = "";
			for(String line : lines)
				str += line + "\n";
			debugView.codeArea.setText(str);
			debugView.codeArea.setEditable(false);
			debugView.codeArea.setHighlightCurrentLine(false);

			debugView.codeScrollPane = new RTextScrollPane(debugView.codeArea);
//			debugView.codeScrollPane.getGutter().setBookmarkIcon(new ImageIcon("breakpoint.png"));
			debugView.codeScrollPane.getGutter().setBookmarkIcon(new ImageIcon(getClass().getResource("breakpoint.png")));
			debugView.codeScrollPane.getGutter().setBookmarkingEnabled(true);
			
			debugView.codeScrollPane.getGutter().getBookmarks();
//			debugView.codeScrollPane.getGutter().toggleBookmark(10);
			
			
			debugView.breakpointTimer = new Timer(100, new ActionListener() {
				GutterIconInfo[] lastBookmarks;
				
				public void actionPerformed(ActionEvent e) {
					GutterIconInfo[] bookmarks = debugView.codeScrollPane.getGutter().getBookmarks();
					try {
						if(lastBookmarks != null)
						{
							for(int i = 0; i < lastBookmarks.length; i++)
							{
								boolean found = false;
								for(int ii = 0; ii < bookmarks.length; ii++)
									if(bookmarks[ii].equals(lastBookmarks[i]))
										found = true;
								if(!found)
									removeBookMark(filename, 1+debugView.codeArea.getLineOfOffset(lastBookmarks[i].getMarkedOffset()));
							}
							for(int i = 0; i < bookmarks.length; i++)
							{
								boolean found = false;
								for(int ii = 0; ii < lastBookmarks.length; ii++)
									if(bookmarks[i].equals(lastBookmarks[ii]))
										found = true;
								if(!found)
									addBookMark(filename, 1+debugView.codeArea.getLineOfOffset(bookmarks[i].getMarkedOffset()));
							}
						
						}
					} catch (BadLocationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					lastBookmarks = bookmarks;
				}
			});
			debugView.breakpointTimer.start();
			
			
			//debugView.className = file
			
			
			sourceFilesTabs.addTab(new File(filename).getName(), debugView.codeScrollPane);
			//new File(filename.toLowerCase()).getName()
			openFiles.add(debugView);
			
		} catch(NoSuchFileException e2)
		{
			return;			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	
	void addBookMark(String file, int line)
	{
		try {
			Location loc = vm.classesByName("Led").get(0).locationsOfLine(line).get(0);
			BreakpointRequest request = vm.eventRequestManager().createBreakpointRequest(loc);
			request.enable();
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		}	
	}
	void removeBookMark(String file, int line)
	{
		System.out.println("removing bookmark at " + file + ":" + line);
	}
	

}


class ClassDebugView
{
	public RSyntaxTextArea codeArea;
	public RTextScrollPane codeScrollPane;
	public String className;
	public String fileName;
	public ArrayList<BreakpointRequest> requests = new ArrayList<BreakpointRequest>();
	public Timer breakpointTimer;
}
