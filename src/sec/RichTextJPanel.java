package sec;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.rtf.RTFEditorKit;

public class RichTextJPanel extends JPanel {
	
	private final JTextPane textPane = new JTextPane();
	
	public RichTextJPanel (Secret secret) {
		super(new BorderLayout());
		setBorder(new TitledBorder("Rich Text"));
		textPane.setEditorKit(new RTFEditorKit());
		JButton editButton = new JButton("Edit");
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				edit();
			}
		});
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(editButton);
		JScrollPane valueScrollPane = new JScrollPane(textPane);
		add(buttonPanel, BorderLayout.NORTH);
		add(valueScrollPane, BorderLayout.CENTER);
		setData(secret.value);
	}
	
	public byte[] getData() {
		Document doc = textPane.getDocument();
		if (doc.getLength() > 0) {
			EditorKit kit = textPane.getEditorKit();
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				kit.write(bos, doc, 0, doc.getLength());
				return bos.toByteArray();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			return new byte[0];
		}
	}
	
	public void setData(byte[] data) {
		final EditorKit kit = textPane.getEditorKit();
		final Document doc = kit.createDefaultDocument();
		if (data.length > 0) {
			try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
				kit.read(bis, doc, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		textPane.setEditable(false);
		textPane.setBackground(null);
		textPane.setDocument(doc);
		revalidate();
		repaint();
	}
	
	public void edit() {
		textPane.setBackground(Color.white);
		textPane.setEditable(true);
		textPane.requestFocusInWindow();
	}
}
