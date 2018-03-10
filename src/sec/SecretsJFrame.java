package sec;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Arrays;
import java.util.zip.*;

import javax.crypto.*;
import javax.swing.*;

import org.apache.commons.lang3.text.WordUtils;

public class SecretsJFrame extends JFrame {
	
	public static final String TITLE = "Secrets";
	private static SecretsJFrame instance;
	
	private static final SecretFileFilter AES_FILTER = new SecretFileFilter(CipherHelper.AES, "Secret File (AES-128)");
	private static final SecretFileFilter DES_FILTER = new SecretFileFilter(CipherHelper.DES, "Secret File (Triple DES)");
	private static final SecretFileFilter BF_FILTER = new SecretFileFilter(CipherHelper.BF, "Secret File (Blowfish-128)");
	
	public static void main (String[] args) {
		boolean loaded = false;
		boolean load = false;
		for (String arg : args) {
			if (arg.length() > 0) {
				System.out.println("arg=" + arg);
				load = true;
				loaded |= getInstance().openFile(new File(arg));
			}
		}
		if (load && !loaded) {
			// exit if user pressed cancel on only file
			System.exit(1);
		}
		getInstance().setVisible(true);
	}
	
	public static SecretsJFrame getInstance() {
		if (instance == null) {
			instance = new SecretsJFrame();
		}
		return instance;
	}
	
	private final Timer timer;
	
	public SecretsJFrame () {
		super("Secrets");
		
		// exit if no activity in 60 minutes
		timer = new Timer(60*60*1000, new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				exit();
			}
		});
		timer.setRepeats(false);
		timer.start();
		
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			@Override
			public void eventDispatched (AWTEvent e) {
				timer.restart();
			}
		}, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing (WindowEvent e) {
				exit();
			}
		});
		
		JMenuBar bar = new JMenuBar();
		bar.add(createFileMenu());
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setPreferredSize(new Dimension(640, 480));
		setJMenuBar(bar);
		pack();
	}
	
	private JMenu createFileMenu () {
		JMenuItem newItem = new JMenuItem("New");
		newItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				newFile();
			}
		});
		JMenuItem openItem = new JMenuItem("Open");
		openItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				openFile();
			}
		});
		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				saveFile(false);
			}
		});
		JMenuItem saveAsItem = new JMenuItem("Save As");
		saveAsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				saveFile(true);
			}
		});
		JMenuItem closeItem = new JMenuItem("Close");
		closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				closeFile();
			}
		});
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				exit();
			}
		});
		JMenu file = new JMenu("File");
		file.add(newItem);
		file.add(openItem);
		file.add(saveItem);
		file.add(saveAsItem);
		file.add(closeItem);
		file.add(exitItem);
		return file;
	}
	
	private void closeFile () {
		if (saveFileOpt()) {
			setContentPane(new JPanel());
			revalidate();
			repaint();
			updateTitle();
		}
	}
	
	private void exit () {
		if (!saveFileOpt()) {
			return;
		}
		System.exit(0);
	}
	
	private boolean saveFileOpt () {
		System.out.println("save file opt");
		final Container cp = getContentPane();
		if (cp instanceof SecretsJPanel) {
			SecretsJPanel sp = (SecretsJPanel) cp;
			int mod = sp.getModifiedNodes();
			if (mod > 0) {
				String names = sp.getModifiedNodeNames();
				int opt = JOptionPane.showConfirmDialog(this, WordUtils.wrap("Save nodes " + names + "?", 80), "Save", JOptionPane.YES_NO_CANCEL_OPTION);
				if (opt == JOptionPane.YES_OPTION) {
					return saveFile(false);
				} else if (opt == JOptionPane.NO_OPTION) {
					return true;
				} else {
					return false;
				}
			}
		}
		return true;
	}
	
	private void newFile () {
		if (saveFileOpt()) {
			final SecretsJPanel sp = new SecretsJPanel();
			sp.addPropertyChangeListener(SecretsJPanel.MODNODES_PROP, new PropertyChangeListener() {
				@Override
				public void propertyChange (PropertyChangeEvent evt) {
					updateTitle();
				}
			});
			setContentPane(sp);
			updateTitle();
			revalidate();
			repaint();
		}
	}
	
	/** show open dialog and open file */
	private void openFile () {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new SecretFileFilter(null, "Secret File"));
		int opt = fc.showOpenDialog(this);
		if (opt == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			openFile(f);
		}
	}
	
	/** show password dialog and open file */
	public boolean openFile (final File file) {
		System.out.println("load file " + file);
		if (!file.exists()) {
			showErrorDialog("Open", "File not found: " + file);
			return false;
		}
		
		if (file.length() == 0) {
			showErrorDialog("Open", "File is empty: " + file);
			return false;
		}
		
		char[] input = showPasswordDialog("Open", "Password for " + file.getName());
		if (input == null || input.length == 0) {
			return false;
		}
		
		return openFile(file, input);
	}

	/** open file */
	private boolean openFile (final File file, char[] input) {
		try {
			try (FileInputStream fis = new FileInputStream(file)) {
				byte magic = (byte) fis.read();
				CipherHelper helper = CipherHelper.getHelper(magic);
				SecretKey key = helper.getKey(input);
				final Cipher cipher = helper.getDecryptCipher(fis, key);
				
				try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
					CipherHelper.unpad(cis);
					try (GZIPInputStream gis = new GZIPInputStream(cis)) {
						try (ObjectInputStream ois = new ObjectInputStream(gis)) {
							Object[][] secretsArr = (Object[][]) ois.readObject();
							SecretsJPanel sp = new SecretsJPanel();
							sp.setCipherHelper(helper);
							sp.loadSecrets(secretsArr);
							sp.setSecretKey(key);
							sp.setFile(file);
							sp.addPropertyChangeListener(SecretsJPanel.MODNODES_PROP, new PropertyChangeListener() {
								@Override
								public void propertyChange (PropertyChangeEvent evt) {
									updateTitle();
								}
							});
							setContentPane(sp);
						}
					}
				}
			}
			
			updateTitle();
			revalidate();
			repaint();
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog("Load", "Could not load file: " + file, e);
			return false;
		}
	}
	
	private boolean saveFile (boolean saveAs) {
		System.out.println("save file");
		final Container cp = getContentPane();
		
		if (cp instanceof SecretsJPanel) {
			SecretsJPanel sp = (SecretsJPanel) cp;
			File file = sp.getFile();
			CipherHelper helper = sp.getCipherHelper();
			SecretKey key = sp.getSecretKey();
			
			if (helper == null || file == null || saveAs) {
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(AES_FILTER);
				fc.addChoosableFileFilter(DES_FILTER);
				fc.addChoosableFileFilter(BF_FILTER);
				fc.setAcceptAllFileFilterUsed(false);
				int opt = fc.showSaveDialog(this);
				if (opt != JFileChooser.APPROVE_OPTION) {
					return false;
				}
				file = fc.getSelectedFile();
				if (!file.getName().contains(".")) {
					file = new File(file.getAbsolutePath() + ".sec");
				}
				SecretFileFilter filter = (SecretFileFilter) fc.getFileFilter();
				helper = filter.helper;
				key = null;
			}
			
			try {
				if (key == null) {
					char[] password = showPasswordDialog("Save", "Password for " + file.getName());
					if (password == null || password.length == 0) {
						return false;
					}
					char[] password2 = showPasswordDialog("Save", "Confirm password for " + file.getName());
					if (password2 == null || password2.length == 0) {
						return false;
					}
					if (!Arrays.equals(password, password2)) {
						JOptionPane.showMessageDialog(this, "Passwords not equal", "Save", JOptionPane.WARNING_MESSAGE);
						return false;
					}
					key = helper.getKey(password);
					System.out.println("key=" + key);
				}
				
				File backupFile = null;
				if (file.exists()) {
					File backupDir = new File(file.getParentFile(), "Backup Secrets");
					backupDir.mkdirs();
					for (int n = 0; true; n++) {
						backupFile = new File(backupDir, file.getName() + "." + n);
						if (!backupFile.exists()) {
							break;
						}
					}
				}
				
				File tempFile = File.createTempFile("temp", "", file.getParentFile());
				
				System.out.println("write file " + tempFile);
				try (FileOutputStream fos = new FileOutputStream(tempFile)) {
					fos.write(helper.getMagic());
					final Cipher cipher = helper.getEncryptCipher(fos, key);
					try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
						CipherHelper.pad(cos);
						try (GZIPOutputStream gos = new GZIPOutputStream(cos)) {
							try (ObjectOutputStream oos = new ObjectOutputStream(gos)) {
								final Object[][] secrets = sp.saveSecrets();
								oos.writeObject(secrets);
							}
						}
					}
					
					if (backupFile != null) {
						System.out.println("rename " + file + " to " + backupFile);
						file.renameTo(backupFile);
					}
					
					System.out.println("rename " + tempFile + " to " + file);
					tempFile.renameTo(file);
				}
				
				sp.setCipherHelper(helper);
				sp.setFile(file);
				sp.setSecretKey(key);
				sp.clearModifiedNodes();
				updateTitle();
				
			} catch (Exception e) {
				e.printStackTrace();
				showErrorDialog("Save", "Could not save file", e);
				return false;
			}
			
		}
		
		return true;
	}
	
	private void updateTitle () {
		System.out.println("update title");
		Container cp = getContentPane();
		if (cp instanceof SecretsJPanel) {
			SecretsJPanel sp = (SecretsJPanel) cp;
			File file = sp.getFile();
			CipherHelper helper = sp.getCipherHelper();
			if (file != null) {
				int mod = sp.getModifiedNodes();
				String modStr = mod > 0 ? " (modified: " + mod + ")" : "";
				String helperStr = helper != null ? " [" + helper + "]" : "";
				setTitle(TITLE + " - " + file.getName() + modStr + helperStr);
				return;
			}
		}
		setTitle(TITLE);
	}
	
	public void showErrorDialog (String title, String msg, Exception e) {
		JOptionPane.showMessageDialog(this, WordUtils.wrap(msg + ": " + e.toString(), 80), title, JOptionPane.ERROR_MESSAGE);
	}

	public void showErrorDialog(String title, String msg) {
		JOptionPane.showMessageDialog(this, WordUtils.wrap(msg, 80), title, JOptionPane.ERROR_MESSAGE);
	}
	
	private char[] showPasswordDialog(String title, String message) {
		final JPasswordField passwordField = new JPasswordField(10);
		JPanel panel = new JPanel(new GridLayout(2, 1));
		panel.add(new JLabel(message));
		panel.add(passwordField);
		JOptionPane optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = optionPane.createDialog(this, title);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened (WindowEvent e) {
				passwordField.requestFocusInWindow();
			}
		});
		dialog.setVisible(true);
		if (optionPane.getValue() != null && optionPane.getValue().equals(JOptionPane.OK_OPTION)) {
			return passwordField.getPassword();
		}
		return null;
	}
	
}
