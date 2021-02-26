package ro.dlri.oxygen.templates.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Enumeration;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.log4j.Logger;

import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.access.AuthorEditorAccess;
import ro.sync.ecss.extensions.api.node.AuthorNode;

public class TreeTemplate extends JTree {

	private static final Logger logger = Logger.getLogger(TreeTemplate.class.getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = -1267108628921192715L;
	protected DefaultMutableTreeNode rootNode;
	public DefaultTreeModel treeModel;
	public static AuthorEditorAccess authorEditorAccess;
	public AuthorDocumentController documentController;
	public String itemLabel;
	public String parentNodePath;

	public TreeTemplate() {
		super();
		rootNode = new DefaultMutableTreeNode("Sensuri");
		treeModel = new DefaultTreeModel(rootNode);
		setDragEnabled(false);
		//setDropMode(DropMode.ON_OR_INSERT);
		setTransferHandler(new TreeTransferHandler());
		getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
		setEditable(true);
		setCellRenderer(new TreeTemplateCellRenderer());

	}

	public DefaultMutableTreeNode addItem(DefaultMutableTreeNode parent, Object child) {
		return addItem(parent, child, false);
	}

	public DefaultMutableTreeNode addItem(DefaultMutableTreeNode parent, Object child, boolean shouldBeVisible) {
		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

		if (parent == null) {
			parent = rootNode;
		}

		treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

		if (shouldBeVisible) {
			scrollPathToVisible(new TreePath(childNode.getPath()));
		}
		return childNode;
	}

	public void expand() {
		for (int i = 0; i < getRowCount(); i++) {
			expandRow(i);
		}
	}

	public void selectNode(String nodeId) {
		DefaultMutableTreeNode node = null;
		Enumeration<?> e = ((DefaultMutableTreeNode) this.getModel().getRoot()).breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			node = (DefaultMutableTreeNode) e.nextElement();
			if (nodeId.equals(node.getUserObject().toString())) {
				this.setSelectionPath(new TreePath(node.getPath()));
			}
		}
	}

	public static void main(String[] args) {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		String treeParentNodePath = "//dictScrap[@xml:id = 'senses']";
		TreeTemplate tree = new TreeTemplate();
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new TreeItemInfo("Sensuri", treeParentNodePath));
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

		for (int i = 0, il = 7; i < il; i++) {
			DefaultMutableTreeNode senseItem = new DefaultMutableTreeNode(new TreeItemInfo(
					Integer.toString(i + 1)
							+ " ăîșțââ ș long string long string long string long string long string long"
							+ " string long string long string long string long string long string long string long"
							+ " string long string long string long string long string long string long string",
					treeParentNodePath + "/sense[" + (i + 1) + "]"));
			treeModel.insertNodeInto(senseItem, rootNode, rootNode.getChildCount());
		}
		tree.setModel(treeModel);
		tree.setRowHeight(-1);
		JScrollPane sp = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		f.add(sp);
		f.setSize(1000, 400);
		f.setLocation(200, 200);
		f.setVisible(true);
	}

	public void generateTreeModel() {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new TreeItemInfo("Sensuri", parentNodePath));

		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

		AuthorNode[] senseNodesList = null;

		try {
			senseNodesList = documentController.findNodesByXPath(parentNodePath + "/sense", true, true, true);
		} catch (AuthorOperationException e) {
			e.printStackTrace();
		}

		int senseElementsNumber = senseNodesList.length;

		for (int i = 0, il = senseElementsNumber; i < il; i++) {
			AuthorNode senseNode = senseNodesList[i];
			Object[] treeItemLabelObjectList = null;

			try {
				treeItemLabelObjectList = documentController.evaluateXPath(itemLabel, senseNode, false, true, true,
						false);
			} catch (AuthorOperationException e) {
				e.printStackTrace();
			}

			DefaultMutableTreeNode senseItem = new DefaultMutableTreeNode(new TreeItemInfo(
					(String) treeItemLabelObjectList[0], parentNodePath + "/sense[" + (i + 1) + "]"));
			treeModel.insertNodeInto(senseItem, rootNode, rootNode.getChildCount());
			addTreeItems(documentController, senseNode, senseItem);
		}

		setModel(treeModel);

		expand();
	}

	private void addTreeItems(AuthorDocumentController documentController, AuthorNode parentSenseElement,
			DefaultMutableTreeNode parentSenseItem) {
		AuthorNode[] senseNodesList = null;
		try {
			senseNodesList = documentController.findNodesByXPath("sense", parentSenseElement, true, true, true, false);
		} catch (AuthorOperationException e1) {
			e1.printStackTrace();
		}

		for (int i = 0, il = senseNodesList.length; i < il; i++) {
			AuthorNode senseNode = senseNodesList[i];
			DefaultMutableTreeNode childSenseItem = null;
			TreeItemInfo parentItemInfo = (TreeItemInfo) parentSenseItem.getUserObject();
			Object[] treeItemLabelObjectList = null;

			try {
				treeItemLabelObjectList = documentController.evaluateXPath(itemLabel, senseNode, false, true, true,
						false);
			} catch (AuthorOperationException e) {
				e.printStackTrace();
			}

			childSenseItem = addItem(parentSenseItem, new TreeItemInfo((String) treeItemLabelObjectList[0],
					parentItemInfo.currentNodeXpathPath + "/sense[" + (i + 1) + "]"));
			addTreeItems(documentController, senseNode, childSenseItem);
		}
	}
}

class TreeTemplateCellRenderer extends JLayeredPane implements TreeCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5115694033002712002L;

	JPanel renderer;
	JPanel contentPanel;
	JPanel buttonPanel;
	JButton addFollowingSiblingButton;
	JButton addFirstChildButton;
	JButton editButton;
	JButton removeButton;
	JLabel itemLabel;
	DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
	Color backgroundSelectionColor;
	Color backgroundNonSelectionColor;

	public TreeTemplateCellRenderer() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		renderer = new JPanel(new GridBagLayout());

		buttonPanel = new JPanel();
		buttonPanel.setOpaque(false);

		itemLabel = new JLabel(":)");
		itemLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		renderer.add(itemLabel);
		backgroundSelectionColor = defaultRenderer.getBackgroundSelectionColor();
		backgroundNonSelectionColor = defaultRenderer.getBackgroundNonSelectionColor();
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		Component returnValue = null;

		if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
			if (((DefaultMutableTreeNode) value).isRoot()) {
				return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row,
						hasFocus);
			}
			Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
			if (userObject instanceof TreeItemInfo) {
				itemLabel.setText(((TreeItemInfo) userObject).toString());

				if (selected) {
					renderer.setBackground(backgroundSelectionColor);
				} else {
					renderer.setBackground(backgroundNonSelectionColor);
				}

				renderer.setEnabled(tree.isEnabled());

				returnValue = renderer;
			}
		}
		if (returnValue == null) {
			returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row,
					hasFocus);
		}
		return renderer;
	}
}
