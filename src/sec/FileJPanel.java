package sec;

import java.awt.Component;
import java.awt.event.*;
import java.io.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.IOUtils;

public class FileJPanel extends JPanel {
	
	public static File getFile (Component comp) {
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
		int opt = fc.showOpenDialog(comp);
		if (opt == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		return null;
	}
	
	public static byte[] loadFile (File file) throws Exception {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
				try (FileInputStream fis = new FileInputStream(file)) {
					IOUtils.copy(fis, gos);
				}
			}
			return bos.toByteArray();
		}
	}
	
	private final JLabel fileLabel = new JLabel();
	private final Secret secret;
	
	public FileJPanel (Secret secret) {
		setBorder(new TitledBorder("File"));
		this.secret = secret;
		
		JButton loadButton = new JButton("Load");
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				loadFile();
			}
		});
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				saveFile();
			}
			
		});
		JButton openButton = new JButton("Open");
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				openFile(false);
			}
		});
		// JButton editButton = new JButton("Edit");
		// editButton.addActionListener(new ActionListener() {
		// @Override
		// public void actionPerformed (ActionEvent e) {
		// openFile(true);
		// }
		// });
		
		add(fileLabel);
		add(loadButton);
		add(saveButton);
		add(openButton);
		
		updateLabel();
	}
	
	private void updateLabel () {
		fileLabel.setText(secret != null ? secret.value.length + " bytes" : "null");
	}
	
	private void loadFile () {
		try {
			File file = getFile(this);
			if (file != null) {
				secret.value = loadFile(file);
				firePropertyChange(SecretsJPanel.NODE_MODIFIED_PROP, false, true);
				updateLabel();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			SecretsJFrame.getInstance().showErrorDialog("Load File", "Could not load file", e);
		}
	}
	
	private void saveFile () {
		try {
			JFileChooser fc = new JFileChooser();
			int opt = fc.showSaveDialog(this);
			if (opt == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				try (FileOutputStream fos = new FileOutputStream(file)) {
					try (ByteArrayInputStream fis = new ByteArrayInputStream(secret.value)) {
						try (GZIPInputStream gis = new GZIPInputStream(fis)) {
							IOUtils.copy(gis, fos);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			SecretsJFrame.getInstance().showErrorDialog("Save File", "Could not save file", e);
		}
	}
	
	private void openFile (boolean edit) {
		System.out.println("open file " + edit);
		File tempFile = null;
		try {
			if (secret != null) {
				tempFile = File.createTempFile("secret", "." + secret.name);
				tempFile.deleteOnExit();
				System.out.println("write " + tempFile);
				try (FileOutputStream fos = new FileOutputStream(tempFile)) {
					try (ByteArrayInputStream fis = new ByteArrayInputStream(secret.value)) {
						try (GZIPInputStream gis = new GZIPInputStream(fis)) {
							IOUtils.copy(gis, fos);
						}
					}
				}
				System.out.println("exec " + tempFile);
				// TODO os x, generic
				final String[] cmd = new String[] { "cmd", "/c", tempFile.getAbsolutePath() };
				Process proc = Runtime.getRuntime().exec(cmd);
				proc.waitFor();
				if (edit) {
					// TODO reload file
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			SecretsJFrame.getInstance().showErrorDialog("Open File", "Could not open file", e);
			
		} finally {
			if (tempFile != null && tempFile.exists()) {
				System.out.println("delete " + tempFile);
				try (FileOutputStream fos = new FileOutputStream(tempFile)) {
					fos.write(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
				tempFile.delete();
			}
		}
	}
}
