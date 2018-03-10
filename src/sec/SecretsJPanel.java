package sec;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

class SecretsJPanel extends JPanel {
	
	public static final String NODE_MODIFIED_PROP = "nodeModified";
	public static final String MODNODES_PROP = "modifiedNodes";
	public static final String PATHSEP = ":";
	
	private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("");
	private final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
	private final JTree tree = new JTree(treeModel);
	private final JSplitPane splitPane = new JSplitPane();
	private final JScrollPane treeScrollPane = new JScrollPane();
	private final Set<DefaultMutableTreeNode> modifiedNodes = new HashSet<>();
	private final JPanel valuePanel = new JPanel(new BorderLayout());
	
	private File file;
	private SecretKey secretKey;
	private DefaultMutableTreeNode currentNode;
	private CipherHelper cipherHelper;
	private JPanel currentPanel;
	
	public SecretsJPanel () {
		super(new BorderLayout());
		initComponents();
		rootNode.add(new DefaultMutableTreeNode(new Secret("Root", new byte[] { Secret.TEXT_TYPE }, new byte[0])));
		treeModel.reload();
	}
	
	public int getModifiedNodes () {
		return modifiedNodes.size();
	}
	
	public String getModifiedNodeNames () {
		StringBuilder sb = new StringBuilder();
		for (DefaultMutableTreeNode node : modifiedNodes) {
			Secret secret = (Secret) node.getUserObject();
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(secret.name);
		}
		return sb.toString();
	}
	
	public void clearModifiedNodes () {
		for (DefaultMutableTreeNode node : modifiedNodes) {
			Secret secret = (Secret) node.getUserObject();
			secret.modified = false;
			treeModel.nodeChanged(node);
		}
		modifiedNodes.clear();
	}
	
	public void setNodeModified (DefaultMutableTreeNode node) {
		Secret secret = (Secret) node.getUserObject();
		secret.modified = true;
		treeModel.nodeChanged(node);
		int oldValue = modifiedNodes.size();
		modifiedNodes.add(node);
		firePropertyChange(MODNODES_PROP, oldValue, modifiedNodes.size());
	}
	
	public void loadSecrets (Object[][] secretsArr) {
		rootNode.removeAllChildren();
		
		for (Object[] secretArr : secretsArr) {
			if (secretArr != null) {
				final byte[] keyData = (byte[]) secretArr[Secret.KEY_INDEX];
				final byte[] valueData = (byte[]) secretArr[Secret.VALUE_INDEX];
				final byte[] typeData = (byte[]) secretArr[Secret.TYPE_INDEX];
				final byte[] propertyData = secretArr.length > Secret.PROPERTIES_INDEX ? (byte[]) secretArr[Secret.PROPERTIES_INDEX] : null;
				
				String key = new String(keyData, StandardCharsets.UTF_8);
				List<String> path = new ArrayList<>();
				StringTokenizer st = new StringTokenizer(key, PATHSEP);
				while (st.hasMoreTokens()) {
					path.add(st.nextToken());
				}
				DefaultMutableTreeNode node = getNode(rootNode, path);
				Secret secret = (Secret) node.getUserObject();
				secret.value = valueData;
				secret.type = typeData;
				secret.properties = propertyData;
			}
		}
		
		treeModel.reload(rootNode);
	}
	
	private static DefaultMutableTreeNode getNode (final DefaultMutableTreeNode root, final List<String> path) {
		final String name = path.get(0);
		
		DefaultMutableTreeNode node = null;
		for (int n = 0; n < root.getChildCount(); n++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(n);
			final Secret secret = (Secret) child.getUserObject();
			if (secret.name.equals(name)) {
				node = child;
				break;
			}
		}
		
		if (node == null) {
			node = new DefaultMutableTreeNode(new Secret(name));
			root.add(node);
		}
		
		if (path.size() == 1) {
			return node;
			
		} else {
			return getNode(node, path.subList(1, path.size()));
		}
	}
	
	public Object[][] saveSecrets () {
		System.out.println("save secrets");
		saveCurrentNode();
		List<DefaultMutableTreeNode> l = collectNodes(new ArrayList<DefaultMutableTreeNode>(), rootNode);
		Object[][] secretsArr = new Object[l.size()][];
		for (int n = 1; n < l.size(); n++) {
			DefaultMutableTreeNode node = l.get(n);
			final Secret secret = (Secret) node.getUserObject();
			final String path = getNodePath(node, PATHSEP);
			Object[] secretArr = new Object[Secret.INDEXES];
			secretArr[Secret.KEY_INDEX] = path.getBytes(StandardCharsets.UTF_8);
			secretArr[Secret.VALUE_INDEX] = secret.value;
			secretArr[Secret.TYPE_INDEX] = secret.type;
			secretArr[Secret.PROPERTIES_INDEX] = secret.properties;
			secretsArr[n] = secretArr;
		}
		l.clear();
		return secretsArr;
	}
	
	private static String getNodePath (DefaultMutableTreeNode node, String sep) {
		Object[] objects = node.getUserObjectPath();
		StringBuilder sb = new StringBuilder();
		for (Object obj : objects) {
			if (obj instanceof Secret) {
				Secret secret = (Secret) obj;
				if (sb.length() > 0) {
					sb.append(sep);
				}
				sb.append(secret.name);
			}
		}
		if (sb.length() > 0) {
			return sb.toString();
		} else {
			return null;
		}
	}
	
	private List<DefaultMutableTreeNode> collectNodes (List<DefaultMutableTreeNode> l, DefaultMutableTreeNode node) {
		l.add(node);
		for (int n = 0; n < node.getChildCount(); n++) {
			DefaultMutableTreeNode c = (DefaultMutableTreeNode) node.getChildAt(n);
			collectNodes(l, c);
		}
		return l;
	}
	
	public void setFile (File f) {
		this.file = f;
	}
	
	public File getFile () {
		return file;
	}
	
	private void initComponents () {
		tree.setRootVisible(false);
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked (MouseEvent e) {
				if (e.getClickCount() == 2) {
					DefaultMutableTreeNode node = getSelectedNode();
					if (node != null && node.isLeaf()) {
						editSecret();
					}
				}
			}
			
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
				TreePath path = tree.getPathForLocation(e.getPoint().x, e.getPoint().y);
				if (path != null) {
					tree.setSelectionPath(path);
					JPopupMenu menu = createTreePopupMenu();
					menu.show(tree, e.getPoint().x, e.getPoint().y);
				}
			}
		});
		
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged (TreeSelectionEvent e) {
				saveCurrentNode();
				
				TreePath path = e.getNewLeadSelectionPath();
				if (path != null) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
					selectNode(node);
					
				} else {
					currentNode = null;
					valuePanel.removeAll();
				}
			}
		});
		
		treeScrollPane.setViewportView(tree);
		treeScrollPane.setBorder(new TitledBorder("Nodes"));
		
		JPanel treePanel = new JPanel(new BorderLayout());
		treePanel.add(treeScrollPane, BorderLayout.CENTER);
		
		splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(treePanel);
		splitPane.setRightComponent(valuePanel);
		splitPane.setResizeWeight(0.25);
		
		add(splitPane, BorderLayout.CENTER);
	}
	
	private JPopupMenu createTreePopupMenu () {
		JMenuItem renameItem = new JMenuItem("Rename");
		renameItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				renameNode();
			}
		});
		JMenuItem createTextItem = new JMenuItem("Text Node");
		createTextItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				createNode(Secret.TEXT_TYPE);
			}
		});
		JMenuItem createRtfItem = new JMenuItem("Rich Text Node");
		createRtfItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				createNode(Secret.RTF_TYPE);
			}
		});
		JMenuItem createFileItem = new JMenuItem("File Node");
		createFileItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				createNode(Secret.FILE_TYPE);
			}
		});
		JMenuItem removeItem = new JMenuItem("Remove");
		removeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				removeNode();
			}
		});
		JMenuItem propertiesItem = new JMenuItem("Properties");
		propertiesItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent e) {
				showProperties();
			}
		});
		JMenu createMenu = new JMenu("Create New");
		createMenu.add(createTextItem);
		createMenu.add(createRtfItem);
		createMenu.add(createFileItem);
		JPopupMenu menu = new JPopupMenu();
		menu.add(createMenu);
		menu.add(renameItem);
		menu.add(removeItem);
		menu.add(propertiesItem);
		return menu;
	}
	
	private void showProperties () {
		System.out.println("properties");
		DefaultMutableTreeNode node = getSelectedNode();
		if (node != null) {
			Secret secret = (Secret) node.getUserObject();
			Properties p = secret.getProperties();
			JOptionPane.showMessageDialog(this, WordUtils.wrap(p.toString(), 80), "Properties", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	private void removeNode () {
		System.out.println("remove secret");
		DefaultMutableTreeNode node = getSelectedNode();
		if (node != null) {
			if (node.getParent() == rootNode) {
				JOptionPane.showMessageDialog(this, "Can't remove root node");
				return;
			}
			Secret secret = (Secret) node.getUserObject();
			int opt = JOptionPane.showConfirmDialog(this, "Remove " + secret + " and any child nodes?", "Remove", JOptionPane.YES_NO_OPTION);
			if (opt == JOptionPane.YES_OPTION) {
				treeModel.removeNodeFromParent(node);
				setNodeModified(node);
			}
		}
	}
	
	private DefaultMutableTreeNode getSelectedNode () {
		TreePath path = tree.getSelectionPath();
		if (path != null) {
			return (DefaultMutableTreeNode) path.getLastPathComponent();
		}
		return null;
	}
	
	private void renameNode () {
		DefaultMutableTreeNode node = getSelectedNode();
		if (node != null) {
			Secret secret = (Secret) node.getUserObject();
			String name = StringUtils.trimToNull(JOptionPane.showInputDialog(this, "Rename", secret.name));
			if (name != null && name.length() > 0 && !name.equals(secret.name)) {
				final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
				if (parent != null && hasChild(parent, name)) {
					JOptionPane.showMessageDialog(this, "Node already exists");
				} else if (name.contains(PATHSEP)) {
					JOptionPane.showMessageDialog(this, "Invalid char " + PATHSEP);
				} else {
					secret.name = name;
					treeModel.nodeChanged(node);
					setNodeModified(node);
				}
			}
		}
	}
	
	private void createNode (byte type) {
		System.out.println("add secret");
		DefaultMutableTreeNode node = getSelectedNode();
		if (node != null) {
			// get name and data option
			String name = null;
			File file = null;
			if (type == Secret.FILE_TYPE) {
				file = FileJPanel.getFile(this);
				if (file != null) {
					name = file.getName();
				}
			} else {
				name = StringUtils.trimToNull(JOptionPane.showInputDialog(this, "Name", "New Secret"));
			}
			
			if (name != null) {
				if (name.contains(PATHSEP)) {
					showErrorDialog("Create", "Invalid character in name: " + PATHSEP);
					
				} else if (hasChild(node, name)) {
					showErrorDialog("Create", "Node already exists: " + name);
					
				} else {
					try {
						createNode(node, type, name, file);
					} catch (Exception e) {
						e.printStackTrace();
						showErrorDialog("Create", "Could not create node: " + file);
					}
				}
			}
		}
	}

	private void createNode (DefaultMutableTreeNode node, byte type, String name, File file) throws Exception {
		byte[] data;
		if (type == Secret.FILE_TYPE) {
			data = FileJPanel.loadFile(file);
		} else {
			data = new byte[0];
		}
		
		// create node
		final Secret secret = new Secret(name, new byte[] { type }, data);
		Properties p = secret.getProperties();
		p.setProperty("created", new Date().toString());
		secret.setProperties(p);
		
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(secret);
		node.add(newNode);
		treeModel.nodeStructureChanged(node);
		tree.setSelectionPath(new TreePath(newNode.getPath()));
		setNodeModified(newNode);
		editSecret();
	}
	
	private static boolean hasChild (DefaultMutableTreeNode node, String name) {
		for (int n = 0; n < node.getChildCount(); n++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(n);
			Secret no = (Secret) child.getUserObject();
			if (no.name.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
	
	public SecretKey getSecretKey () {
		return secretKey;
	}
	
	public void setSecretKey (SecretKey key) {
		this.secretKey = key;
	}
	
	private void saveCurrentNode () {
		// not the selected node...
		if (currentNode != null) {
			System.out.println("save current node");
			Secret secret = (Secret) currentNode.getUserObject();
			byte[] data = new byte[0];
			switch (secret.type[0]) {
				case Secret.TEXT_TYPE:
					data = ((TextJPanel)currentPanel).getData();
					break;
				case Secret.RTF_TYPE:
					data = ((RichTextJPanel)currentPanel).getData();
					break;
				case Secret.FILE_TYPE:
					// file node doesn't need saving
					return;
				default:
					throw new RuntimeException("unknown secret type " + secret.type[0]);
			}
			
			if (!Arrays.equals(secret.value, data)) {
				System.out.println("data modified");
				secret.value = data;
				Properties p = secret.getProperties();
				p.setProperty("modified", new Date().toString());
				secret.setProperties(p);
			}
		}
	}
	
	public CipherHelper getCipherHelper () {
		return cipherHelper;
	}
	
	public void setCipherHelper (CipherHelper cipherHelper) {
		this.cipherHelper = cipherHelper;
	}
	
	private void editSecret () {
		if (currentPanel != null) {
			System.out.println("edit secret");
			if (currentPanel instanceof TextJPanel) {
				((TextJPanel) currentPanel).edit(true);
			} else if (currentPanel instanceof RichTextJPanel) {
				((RichTextJPanel) currentPanel).edit();
			}
		}
	}
	
	private void selectNode (DefaultMutableTreeNode node) {
		// select only if changed
		if (currentNode != node) {
			System.out.println("select node " + node.getUserObject());
			valuePanel.removeAll();
			currentNode = node;
			
			Secret secret = (Secret) currentNode.getUserObject();
			switch (secret.type[0]) {
				case Secret.TEXT_TYPE: {
					currentPanel = new TextJPanel(secret);
					break;
				}
				case Secret.RTF_TYPE: {
					currentPanel = new RichTextJPanel(secret);
					break;
				}
				case Secret.FILE_TYPE: {
					currentPanel = new FileJPanel(secret);
					break;
				}
				default:
					throw new RuntimeException("unknown secret type " + secret.type[0]);
			}
			
			currentPanel.addPropertyChangeListener(NODE_MODIFIED_PROP, new PropertyChangeListener() {
				@Override
				public void propertyChange (PropertyChangeEvent evt) {
					setNodeModified(currentNode);
				}
			});
			valuePanel.add(currentPanel, BorderLayout.CENTER);
			valuePanel.revalidate();
			valuePanel.repaint();
		}
	}
	
	public void showErrorDialog(String title, String msg) {
		JOptionPane.showMessageDialog(this, WordUtils.wrap(msg, 80), title, JOptionPane.ERROR_MESSAGE);
	}
	
}
