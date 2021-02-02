package ro.dlri.oxygen.templates.tree;

import javax.swing.tree.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;

public class TreeExample extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7616506225769375560L;
	private JTree tree;
	private int w = 400;
	private int h = 400;
	private JScrollPane sp;
	private int xOffset;
	private int offset;
	private int gap = 3;

	TreeExample() {
		createTree();
		sp = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		System.out.println(sp.getVerticalScrollBar().getSize());
		getContentPane().add(sp);
		setSize(w, h);
		setVisible(true);
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Dimension d = getSize();
				w = d.width;
				h = d.height;
				offset = xOffset + sp.getVerticalScrollBar().getSize().width + gap;

				boolean rootVisible = tree.isRootVisible();
				tree.setRootVisible(!rootVisible);
				tree.setRootVisible(rootVisible);

			}
		});

	}

	private void createTree() {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Major Heading");
		createNodes(top);

		tree = new JTree(top);
		tree.setCellRenderer(new MyCellRenderer());
		tree.setCellEditor(new MyCellEditor());
		tree.setEditable(true);
		tree.putClientProperty("JTree.lineStyle", "None");
//		tree.setRowHeight(17);
		BasicTreeUI basicTreeUI = (BasicTreeUI) tree.getUI();
		basicTreeUI.setRightChildIndent(8);
		basicTreeUI.setLeftChildIndent(5);
		Rectangle rectangle = tree.getPathBounds(tree.getPathForRow(2));
		xOffset = 2 * rectangle.x;
		System.out.println(xOffset);
	}

	private void createNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode category = null;
		DefaultMutableTreeNode book = null;

		category = new DefaultMutableTreeNode("Text 1");
		top.add(category);

		book = new DefaultMutableTreeNode(" ăîșțâ A control that displays a set of hierarchical data as an"
				+ "outline. You can find task-oriented documentation and examples of using"
				+ "trees in How to Use Trees, a section in The Java Tutorial.");

		category.add(book);
		category = new DefaultMutableTreeNode("Text 2");
		top.add(category);

		book = new DefaultMutableTreeNode(" A control that displays a set of hierarchical data as an"
				+ "outline. You can find task-oriented documentation and examples of using"
				+ "trees in How to Use Trees, a section in The Java Tutorial.");

		category.add(book);

	}

	class MyCellRenderer extends JEditorPane implements TreeCellRenderer {
		JEditorPane dummy = new JEditorPane();

		MyCellRenderer() {
			setContentType("text/html");
			dummy.setContentType("text/html");
		}

		public Component getTreeCellRendererComponent(javax.swing.JTree tree, Object obj,
				boolean isSelected, boolean isExtended, boolean isLeaf, int row, boolean hasFocus) {

			setText((String) obj.toString());
			setEditable(false);
			return this;
		}

		public Dimension getPreferredSize() {
			dummy.setText(getText());
			dummy.setSize(w - offset, Integer.MAX_VALUE);
			Dimension d = dummy.getPreferredScrollableViewportSize();
			return new Dimension(w - offset, d.height);
		}
	}

	class MyCellEditor extends JTextPane implements TreeCellEditor {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6929419892786124902L;
		JEditorPane dummy = new JEditorPane();

		public MyCellEditor() {
			this.setContentType("text/html");
			dummy.setContentType("text/html");
		}

		public Component getTreeCellEditorComponent(javax.swing.JTree tree, Object value,
				boolean isSelected, boolean isExpanded, boolean isLeaf, int row) {

			String txt = value.toString();
			this.setText(txt);
			this.setEditable(false);
			return this;

		}

		public Dimension getPreferredSize() {
			dummy.setText(getText());
			dummy.setSize(w - offset, Integer.MAX_VALUE);
			Dimension d = dummy.getPreferredScrollableViewportSize();
			return new Dimension(w - offset, d.height);
		}

		public void cancelCellEditing() {
			this.setEditable(false);
		}

		public boolean isCellEditable(EventObject event) {
			return true;
		}

		public Object getCellEditorValue() {
			return this.getText();
		}

		public boolean shouldSelectCell(EventObject event) {
			return false;
		}

		public void addCellEditorListener(CellEditorListener l) {
		}

		public void removeCellEditorListener(CellEditorListener l) {
		}

		public boolean stopCellEditing() {
			return true;
		}

	}

	public static void main(String[] s) {
		TreeExample te = new TreeExample();
		te.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}
