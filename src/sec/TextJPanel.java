package sec;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TextJPanel extends JPanel implements DocumentListener {
	
	private final JTextArea textArea = new JTextArea();
	private final JToggleButton editButton = new JToggleButton("Edit");
	
	public TextJPanel (Secret secret) {
		super(new BorderLayout());
		setBorder(new TitledBorder("Plain Text"));
		
		textArea.setText(new String(secret.value, StandardCharsets.UTF_8));
		textArea.setCaretPosition(0);
		textArea.getDocument().addDocumentListener(this);
		textArea.setFont(new Font("monospaced", 0, 14));
		textArea.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed (MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup(e);
				}
			}
			
			@Override
			public void mouseReleased (MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup(e);
				}
			}
			
			private void popup (MouseEvent e) {
				JPopupMenu menu = createTextPopupMenu();
				if (menu.getComponentCount() > 0) {
					menu.show(textArea, e.getPoint().x, e.getPoint().y);
				}
			}
			
		});
		
		editButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged (ItemEvent e) {
				edit(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(editButton);
		JScrollPane pane = new JScrollPane(textArea);
		add(pane, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.NORTH);
		
		edit(false);
	}
	
	@Override
	public void removeUpdate (DocumentEvent e) {
		fireNodeModified();
	}
	
	@Override
	public void insertUpdate (DocumentEvent e) {
		fireNodeModified();
	}
	
	@Override
	public void changedUpdate (DocumentEvent e) {
		fireNodeModified();
	}
	
	private void fireNodeModified () {
		firePropertyChange(SecretsJPanel.NODE_MODIFIED_PROP, false, true);
	}
	
	private JPopupMenu createTextPopupMenu () {
		int s1 = textArea.getSelectionStart();
		int s2 = textArea.getSelectionEnd();
		JMenuItem cutItem = new JMenuItem("Cut");
		cutItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent ae) {
				copy(true);
			}
		});
		JMenuItem copyItem = new JMenuItem("Copy");
		copyItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent ae) {
				copy(false);
			}
		});
		JMenuItem pasteItem = new JMenuItem("Paste");
		pasteItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent ae) {
				paste();
			}
		});
		JPopupMenu menu = new JPopupMenu("Edit");
		if (s1 != s2) {
			if (textArea.isEditable()) {
				menu.add(cutItem);
			}
			menu.add(copyItem);
		}
		if (textArea.isEditable()) {
			menu.add(pasteItem);
		}
		return menu;
	}
	
	private void copy (boolean cut) {
		System.out.println("copy " + cut);
		int s1 = textArea.getSelectionStart();
		int s2 = textArea.getSelectionEnd();
		if (s1 != s2) {
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			String selectedText = textArea.getSelectedText();
			if (cut) {
				String text = textArea.getText();
				textArea.setText(text.substring(0, s1) + text.substring(s2));
				textArea.setCaretPosition(s1);
			} else {
				// trim to avoid whitespace before and after password
				selectedText = selectedText.trim();
			}
			clip.setContents(new StringSelection(selectedText), null);
		}
	}
	
	private void paste () {
		System.out.println("paste");
		if (textArea.isEditable()) {
			System.out.println("paste");
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable t = clip.getContents(null);
			try {
				String transText = (String) t.getTransferData(DataFlavor.stringFlavor);
				if (transText != null) {
					int s1 = textArea.getSelectionStart();
					int s2 = textArea.getSelectionEnd();
					String text = textArea.getText();
					textArea.setText(text.substring(0, s1) + transText + text.substring(s2));
					textArea.setCaretPosition(s2);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				SecretsJFrame.getInstance().showErrorDialog("Paste", "Could not paste text", e);
			}
		}
	}
	
	public byte[] getData () {
		return textArea.getText().getBytes(StandardCharsets.UTF_8);
	}
	
	public void edit (boolean edit) {
		editButton.setSelected(edit);
		if (edit) {
			textArea.setBackground(Color.white);
			textArea.setEditable(true);
			textArea.requestFocusInWindow();
		} else {
			textArea.setEditable(false);
			textArea.setBackground(null);
		}
	}
}
