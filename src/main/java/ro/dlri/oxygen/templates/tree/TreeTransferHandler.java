package ro.dlri.oxygen.templates.tree;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorConstants;
import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.ecss.extensions.api.node.AuthorNode;

public class TreeTransferHandler extends TransferHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3246414810042964003L;
	DataFlavor nodesFlavor;
	DataFlavor[] flavors = new DataFlavor[1];
	DefaultMutableTreeNode[] nodesToRemove;
	public AuthorAccess authorAccess;
	public AuthorDocumentController authorDocumentController;
	public TreeTemplate tree;

	public TreeTransferHandler() {
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
					+ javax.swing.tree.DefaultMutableTreeNode[].class.getName() + "\"";
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
		} catch (ClassNotFoundException e) {
			System.out.println("ClassNotFound: " + e.getMessage());
		}
	}

	public boolean canImport(TransferHandler.TransferSupport support) {
		if (!support.isDrop()) {
			return false;
		}
		support.setShowDropLocation(true);
		if (!support.isDataFlavorSupported(nodesFlavor)) {
			return false;
		}
		// Do not allow a drop on the drag source selections.
		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		JTree tree = (JTree) support.getComponent();
		int dropRow = tree.getRowForPath(dl.getPath());
		int[] selRows = tree.getSelectionRows();
		for (int i = 0; i < selRows.length; i++) {
			if (selRows[i] == dropRow) {
				return false;
			}
		}

		return true;
	}

	protected Transferable createTransferable(JComponent c) {
		JTree tree = (JTree) c;
		TreePath[] paths = tree.getSelectionPaths();
		if (paths != null) {
			// Make up a node array of copies for transfer and
			// another for/of the nodes that will be removed in
			// exportDone after a successful drop.
			List<DefaultMutableTreeNode> copies = new ArrayList<DefaultMutableTreeNode>();
			List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
			DefaultMutableTreeNode copiedNode = copy(node);
			copies.add(copiedNode);
			toRemove.add(node);
			for (int i = 1; i < paths.length; i++) {
				DefaultMutableTreeNode next = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
				// Do not allow higher level nodes to be added to list.
				if (next.getLevel() < node.getLevel()) {
					break;
				} else if (next.getLevel() > node.getLevel()) { // child node
					copiedNode.add(copy(next));
					// node already contains child
				} else { // sibling
					copies.add(copy(next));
					toRemove.add(next);
				}
			}
			DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
			nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
			return new NodesTransferable(nodes);
		}
		return null;
	}

	/** Defensive copy used in createTransferable. */
	private DefaultMutableTreeNode copy(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode copiedNode = new DefaultMutableTreeNode(node);
		Object obj = node.getUserObject();
		copiedNode.setUserObject(obj);
		return copiedNode;
	}

	protected void exportDone(JComponent source, Transferable data, int action) {
		if ((action & MOVE) == MOVE) {
			JTree tree = (JTree) source;
			DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
			// Remove nodes saved in nodesToRemove in createTransferable.
			for (int i = 0, il = nodesToRemove.length; i < il; i++) {
				model.removeNodeFromParent(nodesToRemove[i]);
			}
		}
	}

	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}
		// Extract transfer data.
		DefaultMutableTreeNode[] nodes = null;
		try {
			Transferable t = support.getTransferable();
			nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
		} catch (UnsupportedFlavorException ufe) {
			System.out.println("UnsupportedFlavor: " + ufe.getMessage());
		} catch (java.io.IOException ioe) {
			System.out.println("I/O error: " + ioe.getMessage());
		}
		// Get drop location info.
		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		int childIndex = dl.getChildIndex();
		TreePath dest = dl.getPath();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
		String parentNodeXpathString = ((TreeItemInfo) parent.getUserObject()).currentNodeXpathPath;

		System.out.println("parentNodeXpathString: " + parentNodeXpathString);
		TreeTemplate tree = (TreeTemplate) support.getComponent();
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		// Configure for drop mode.
		int index = childIndex; // DropMode.INSERT
		if (childIndex == -1) { // DropMode.ON
			index = parent.getChildCount();
		}
		System.out.println("index: " + index);

		String authorInsertPosition = AuthorConstants.POSITION_INSIDE_LAST;

		// Add data to model.
		for (int i = 0; i < nodes.length; i++) {
			DefaultMutableTreeNode node = nodes[i];
			String sourceNodeXpathString = ((TreeItemInfo) node.getUserObject()).currentNodeXpathPath;

			AuthorNode[] sourceNodeList = null;
			try {
				sourceNodeList = authorDocumentController.findNodesByXPath(sourceNodeXpathString, true, true, true);
			} catch (AuthorOperationException e1) {
				e1.printStackTrace();
			}

			AuthorNode sourceNode = sourceNodeList[0];

			AuthorDocumentFragment sourceNodeAsDocumentFragment = null;
			String sourceNodeAsString = null;

			if (index != 0) {
				authorInsertPosition = AuthorConstants.POSITION_AFTER;
				parentNodeXpathString += "/sense[" + index + "]";
			}

			try {
				authorDocumentController.beginCompoundEdit();
				sourceNodeAsDocumentFragment = authorDocumentController.createDocumentFragment(sourceNode, true);
				authorDocumentController.deleteNode(sourceNode);
				sourceNodeAsString = authorDocumentController.serializeFragmentToXML(sourceNodeAsDocumentFragment);
				authorDocumentController.insertXMLFragmentSchemaAware(sourceNodeAsString, parentNodeXpathString,
						authorInsertPosition);
			} catch (BadLocationException e) {
				e.printStackTrace();
			} catch (AuthorOperationException e) {
				e.printStackTrace();
			} finally {
				authorDocumentController.endCompoundEdit();
			}

			System.out.println("\nparentNodeXpathString:\n" + parentNodeXpathString + "\n");

			tree.generateTreeModel();

			index++;
		}

		System.out.println("drop()!");

		return true;
	}

	public String toString() {
		return getClass().getName();
	}

	public class NodesTransferable implements Transferable {
		DefaultMutableTreeNode[] nodes;

		public NodesTransferable(DefaultMutableTreeNode[] nodes) {
			this.nodes = nodes;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor))
				throw new UnsupportedFlavorException(flavor);
			return nodes;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return nodesFlavor.equals(flavor);
		}
	}

}
