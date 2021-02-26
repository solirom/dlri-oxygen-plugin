package ro.dlri.oxygen.templates.tree;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;

import ro.dlri.oxygen.plugin.Utils;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorConstants;
import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.access.AuthorEditorAccess;
import ro.sync.ecss.extensions.api.access.AuthorWorkspaceAccess;
import ro.sync.ecss.extensions.api.editor.AuthorInplaceContext;
import ro.sync.ecss.extensions.api.editor.InplaceEditingListener;
import ro.sync.ecss.extensions.api.editor.InplaceEditorRendererAdapter;
import ro.sync.ecss.extensions.api.editor.RendererLayoutInfo;
import ro.sync.ecss.extensions.api.node.AttrValue;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.commons.id.GenerateIDElementsInfo;
import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.view.graphics.Point;
import ro.sync.exml.view.graphics.Rectangle;
import ro.sync.exml.workspace.api.Platform;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.actions.AuthorActionsProvider;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * A simple text area based form control.
 */

public class TreeFormControl extends InplaceEditorRendererAdapter {

	/**
	 * Logger for logging.
	 */
	private static final Logger logger = Logger.getLogger(TreeFormControl.class.getName());

	/**
	 * Access to the author specific functions.
	 */
	private AuthorAccess authorAccess;
	private AuthorEditorAccess authorEditorAccess;
	private AuthorActionsProvider authorActionsProvider;
	private AuthorWorkspaceAccess authorWorkspaceAccess;
	private List<InplaceEditingListener> listeners = new ArrayList<InplaceEditingListener>();
	private JPanel componentPanel = new JPanel();
	private TreeTemplate tree;
	private static String ADD_FOLLOWING_SIBLING_COMMAND = "add-following-sibling";
	private static String ADD_FIRST_CHILD_COMMAND = "add-first-child";
	private static String REMOVE_COMMAND = "remove";
	private static String EDIT_COMMAND = "edit";
	private static String xmlPi = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	// define the ActionListener
	public ActionListener defaultAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();

			AuthorDocumentController authorDocumentController = authorAccess.getDocumentController();
			authorWorkspaceAccess = authorAccess.getWorkspaceAccess();
			UtilAccess utilAccess = authorAccess.getUtilAccess();

			if (tree.getSelectionPath() == null) {
				authorWorkspaceAccess.showErrorMessage("Pentru a acționa asupra arborelui trebuie să selectați un articol.");
				return;
			}

			try {
				DefaultMutableTreeNode currentTreeItem = (DefaultMutableTreeNode) tree.getSelectionPath()
						.getLastPathComponent();

				TreeItemInfo currentTreeItemInfo = (TreeItemInfo) currentTreeItem.getUserObject();
				String currentNodeXpathPath = currentTreeItemInfo.currentNodeXpathPath;
				logger.debug("currentNodeXpathPath = " + currentNodeXpathPath);

				AuthorNode[] currentSenseNodeList = null;
				try {
					currentSenseNodeList = authorDocumentController.findNodesByXPath(currentNodeXpathPath, true, true,
							true);
				} catch (AuthorOperationException e1) {
					e1.printStackTrace();
				}

				AuthorNode currentNode = currentSenseNodeList[0];
				int currentOffset = currentNode.getStartOffset() + 1;
				logger.debug("currentOffset = " + currentOffset);

				AuthorElement currentAuthorElement = (AuthorElement) currentNode;
				logger.debug("currentAuthorElement = " + currentAuthorElement);

				// TODO: here has to be the xpath for title
				String currentElementId = currentAuthorElement.getAttribute("xml:id").getValue();
				final String openedXpathExpr = "//*[@xml:id = '" + currentElementId + "']";
				logger.debug("openedXpathExpr = " + openedXpathExpr);
				// TODO
				// xpointer(id('résumé'))

				final URL openerLocation = authorEditorAccess.getEditorLocation();
				String openerFileName = utilAccess.uncorrectURL(utilAccess.getFileName(openerLocation.toString()));
				// String openedFileName = openerFileName + "#" +
				// currentElementId +
				// ".xml";
				// File treeItemFile = new File(frameworkDir + "tmp/" +
				// openedFileName);

				// final File openedFile = File.createTempFile(openerFileName +
				// "#", ".xml");
				final Path openedFile = Files.createTempFile(openerFileName + "#", ".xml");
				logger.debug("openedFile = " + openedFile);

				final String openedFileName = openedFile.getFileName().toString();
				logger.debug("openedFileName = " + openedFileName);

				final URL openedFileUrl = openedFile.toUri().toURL();

				authorDocumentController.beginCompoundEdit();
				if (ADD_FIRST_CHILD_COMMAND.equals(command)) {

					if (currentTreeItem.getLevel() == 7) {
						authorWorkspaceAccess.showErrorMessage("Nu puteți adăuga mai mult de șapte niveluri!");
						return;
					}

					_changeCurrentElementDisplayStatus(new AttrValue("true", "true", false), authorDocumentController,
							currentAuthorElement);
					authorActionsProvider.invokeAuthorExtensionActionInContext(
							authorActionsProvider.getAuthorExtensionActions().get("insertSenseElementAsFirstChild"),
							currentOffset);
					_changeCurrentElementDisplayStatus(new AttrValue("false", "false", false), authorDocumentController,
							currentAuthorElement);

				} else if (ADD_FOLLOWING_SIBLING_COMMAND.equals(command)) {

					_changeCurrentElementDisplayStatus(new AttrValue("true", "true", false), authorDocumentController,
							currentAuthorElement);
					authorActionsProvider.invokeAuthorExtensionActionInContext(authorActionsProvider
							.getAuthorExtensionActions().get("insertSenseElementAsFollowingSibling"), currentOffset);
					_changeCurrentElementDisplayStatus(new AttrValue("false", "false", false), authorDocumentController,
							currentAuthorElement);

				} else if (REMOVE_COMMAND.equals(command)) {
					logger.debug("claudius = " + currentOffset);
					if (currentNodeXpathPath.equals(tree.parentNodePath + "/sense[1]")) {
						authorWorkspaceAccess.showErrorMessage(
								"Nu puteți șterge acest sens, deoarece o intrare trebuie să aibă cel puțin un sens!");
						return;
					}

					_changeCurrentElementDisplayStatus(new AttrValue("true", "true", false), authorDocumentController, currentAuthorElement);
					logger.debug("claudius 2 = " + currentOffset);
					authorActionsProvider.invokeAuthorExtensionActionInContext(authorActionsProvider.getAuthorExtensionActions().get("deleteSenseElement"), currentOffset);
					logger.debug("claudius 3 = " + currentOffset);
					_changeCurrentElementDisplayStatus(new AttrValue("false", "false", false), authorDocumentController, currentAuthorElement);
					logger.debug("claudius 4 = " + currentOffset);
					if (Files.exists(openedFile)) {
						authorWorkspaceAccess.close(openedFileUrl);
						Utils.forceDelete(openedFile);
					}
					logger.debug("claudius 5 = " + currentOffset);
				} else if (EDIT_COMMAND.equals(command)) {

					AuthorDocumentFragment currentNodeAsDocumentFragment = null;
					String currentNodeAsString = null;

					try {
						authorDocumentController.beginCompoundEdit();
						currentNodeAsDocumentFragment = authorDocumentController.createDocumentFragment(currentNode,
								true);
						currentNodeAsString = authorDocumentController.serializeFragmentToXML(currentNodeAsDocumentFragment);
						logger.debug("currentNodeAsString = " + currentNodeAsString);
					} catch (BadLocationException e1) {
						e1.printStackTrace();
					} finally {
						authorDocumentController.endCompoundEdit();
					}

					// ***********************************************
					// start open in other tab of the element

					Utils.writeStringToFile(openedFile, xmlPi + currentNodeAsString);

					final PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
					pluginWorkspace.getOptionsStorage().setOption(openedFileName + " xpath", openedXpathExpr);

					Thread openThread = new Thread(new Runnable() {
						@Override
						public void run() {

							pluginWorkspace.open(openedFileUrl, EditorPageConstants.PAGE_AUTHOR, "text/xml");

							WSEditor secondaryEditor = pluginWorkspace.getEditorAccess(openedFileUrl,
									PluginWorkspace.MAIN_EDITING_AREA);
							secondaryEditor.addEditorListener(new OpenNewEditorListener(openerLocation, openedFileUrl));
						}
					}, "open-secondary-editor");
					openThread.start();

					// end open in other tab of the element
					// ***********************************************
				}

				// save the document
				authorEditorAccess.save();

				authorDocumentController.endCompoundEdit();
				AuthorNode treeRootNode = null;
				try {
					treeRootNode = authorDocumentController.findNodesByXPath(tree.parentNodePath, true, true, true)[0];
				} catch (AuthorOperationException e1) {
					e1.printStackTrace();
				}
				authorEditorAccess.setCaretPosition(treeRootNode.getStartOffset() + 1);

				tree.generateTreeModel();

			} catch (IOException e4) {
				e4.printStackTrace();
			}

		}

		private void _changeCurrentElementDisplayStatus(AttrValue currentlyEditedAttrValue, AuthorDocumentController authorDocumentController, AuthorElement currentNode) {

			String currentNodeName = currentNode.getName();

			authorDocumentController.setAttribute("currentlyEdited", currentlyEditedAttrValue, (AuthorElement) currentNode);

			AuthorNode[] currentSenseNodeParentsList = null;

			currentSenseNodeParentsList = null;
			try {
				currentSenseNodeParentsList = authorDocumentController
						.findNodesByXPath("ancestor-or-self::" + currentNodeName, currentNode, true, true, true, false);
			} catch (AuthorOperationException e1) {
				e1.printStackTrace();
			}

			for (int i = 0, il = currentSenseNodeParentsList.length; i < il; i++) {
				AuthorNode currentSenseNodeParent = currentSenseNodeParentsList[i];
				authorDocumentController.setAttribute("currentlyEditedParent", currentlyEditedAttrValue,
						(AuthorElement) currentSenseNodeParent);
			}
		}
	};

	/**
	 * Constructor.
	 */
	public TreeFormControl() {

		// Create a tree that allows one selection at a time.
		tree = new TreeTemplate();

		componentPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

		JButton addFollowingSiblingButton = new JButton("Adăugare după");
		addFollowingSiblingButton.setActionCommand(ADD_FOLLOWING_SIBLING_COMMAND);
		addFollowingSiblingButton.addActionListener(defaultAction);

		JButton addFirstChildButton = new JButton("Adăugare copil");
		addFirstChildButton.setActionCommand(ADD_FIRST_CHILD_COMMAND);
		addFirstChildButton.addActionListener(defaultAction);

		JButton editButton = new JButton("Editare");
		// JButton editButton = new JButton(new
		// ImageIcon(TreeTemplate.editButtonIcon));
		editButton.setActionCommand(EDIT_COMMAND);
		editButton.addActionListener(defaultAction);
		editButton.setToolTipText("Editare");

		JButton removeButton = new JButton("Ștergere");
		// JButton removeButton = new JButton(new
		// ImageIcon(TreeTemplate.deleteButtonIcon));
		removeButton.setActionCommand(REMOVE_COMMAND);
		removeButton.addActionListener(defaultAction);
		removeButton.setToolTipText("Ștergere");

		JScrollPane scrollPane = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(1050, 300));
		componentPanel.add(scrollPane);

		JPanel buttonPanel = new JPanel(new GridLayout(0, 4));
		buttonPanel.add(addFollowingSiblingButton);
		buttonPanel.add(addFirstChildButton);
		buttonPanel.add(editButton);
		buttonPanel.add(removeButton);

		componentPanel.add(buttonPanel, BorderLayout.SOUTH);

	}

	/**
	 * @see ro.sync.ecss.extensions.api.editor.InplaceEditorRendererAdapter#getEditorComponent(ro.sync.ecss.extensions.api.editor.AuthorInplaceContext,
	 *      ro.sync.exml.view.graphics.Rectangle, ro.sync.exml.view.graphics.Point)
	 */
	@Override
	public Object getEditorComponent(AuthorInplaceContext context, Rectangle allocation, Point mouseLocation) {
		prepareComponent(context);

		return componentPanel;
	}

	/**
	 * @see ro.sync.ecss.extensions.api.editor.InplaceEditorRendererAdapter#getValue()
	 */
	@Override
	public Object getValue() {
		return null;
	}

	/**
	 * @see ro.sync.ecss.extensions.api.editor.InplaceEditorRendererAdapter#stopEditing()
	 */
	@Override
	public void stopEditing() {
		listeners.get(0).editingCanceled();
	}

	/**
	 * @see ro.sync.ecss.extensions.api.editor.InplaceEditorRendererAdapter#addEditingListener(ro.sync.ecss.extensions.api.editor.InplaceEditingListener)
	 */
	@Override
	public void addEditingListener(InplaceEditingListener editingListener) {
		listeners.add(editingListener);
	}

	/**
	 * @see ro.sync.ecss.extensions.api.editor.InplaceEditorRendererAdapter#removeEditingListener(ro.sync.ecss.extensions.api.editor.InplaceEditingListener)
	 */
	@Override
	public void removeEditingListener(InplaceEditingListener editingListener) {
		listeners.remove(editingListener);
	}

	/**
	 * @see ro.sync.ecss.extensions.api.editor.InplaceEditorRendererAdapter#getTooltipText(ro.sync.ecss.extensions.api.editor.AuthorInplaceContext,
	 *      int, int)
	 */
	@Override
	public String getTooltipText(AuthorInplaceContext context, int x, int y) {
		return "";
	}

	/**
	 * @see ro.sync.ecss.extensions.api.editor.InplaceRenderer#getRendererComponent(ro.sync.ecss.extensions.api.editor.AuthorInplaceContext)
	 */
	@Override
	public Object getRendererComponent(AuthorInplaceContext context) {
		prepareComponent(context);

		return componentPanel;
	}

	/**
	 * @see ro.sync.ecss.extensions.api.editor.InplaceRenderer#getRenderingInfo(ro.sync.ecss.extensions.api.editor.AuthorInplaceContext)
	 */
	@Override
	public RendererLayoutInfo getRenderingInfo(AuthorInplaceContext context) {
		// prepare(context);

		String widthArgument = context.getArguments().get("treeWidth").toString().replaceAll("\"", "");
		int width = Integer.parseInt(widthArgument);

		String heightArgument = context.getArguments().get("treeHeight").toString().replaceAll("\"", "");
		int height = Integer.parseInt(heightArgument) + 20;

		ro.sync.exml.view.graphics.Dimension size = new ro.sync.exml.view.graphics.Dimension(width, height);

		return new RendererLayoutInfo(componentPanel.getBaseline(width, height), size);
	}

	/**
	 * Initialize the tree.
	 * 
	 * @throws AuthorOperationException
	 * @throws BadLocationException
	 */
	private void prepareComponent(final AuthorInplaceContext context) {

		this.authorAccess = context.getAuthorAccess();
		this.authorEditorAccess = authorAccess.getEditorAccess();
		this.authorActionsProvider = authorEditorAccess.getActionsProvider();

		boolean isSA = PluginWorkspaceProvider.getPluginWorkspace().getPlatform() == Platform.STANDALONE;

		Runnable runnable = new Runnable() {
			@SuppressWarnings("unused")
			private AuthorAccess authorAccess;
			@SuppressWarnings("unused")
			private AuthorInplaceContext authorContext;

			public void run() {
				this.authorAccess = context.getAuthorAccess();
				this.authorContext = context;
			}
		};

		try {
			if (isSA || SwingUtilities.isEventDispatchThread()) {
				runnable.run();

				tree.setSelectionRow(0);
				AuthorDocumentController documentController = authorAccess.getDocumentController();
				TreeTemplate.authorEditorAccess = authorAccess.getEditorAccess();
				tree.documentController = documentController;
				tree.itemLabel = (String) context.getArguments().get("itemLabel");
				((TreeTransferHandler) tree.getTransferHandler()).authorDocumentController = documentController;
				tree.parentNodePath = (String) context.getArguments().get("treeParentNodePath");
				tree.generateTreeModel();
			} else {
				SwingUtilities.invokeAndWait(runnable);
			}
		} catch (InvocationTargetException ex) {
			ex.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		runnable.run();
	}

	/**
	 * @return Returns the autoGenerateElementsInfo.
	 */
	public GenerateIDElementsInfo getGenerateIDElementsInfo() {
		if (authorAccess != null) {
			return new GenerateIDElementsInfo(authorAccess, new GenerateIDElementsInfo(true,
					GenerateIDElementsInfo.DEFAULT_ID_GENERATION_PATTERN, new String[] { "sense" }));
		} else {
			return null;
		}
	}

	private static class OpenNewEditorListener extends WSEditorListener {
		private final URL openedLocation;
		private final URL openerLocation;

		public OpenNewEditorListener(URL openerLocation, URL openedLocation) {
			this.openedLocation = openedLocation;
			this.openerLocation = openerLocation;
		}

		@Override
		public void editorSaved(int operationType) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
					UtilAccess utilAccess = pluginWorkspace.getUtilAccess();

					logger.debug("openedLocation in editorSaved(int operationType) = " + openedLocation);
					logger.debug("openerLocation in editorSaved(int operationType) = " + openerLocation);

					String openedFileName = utilAccess.uncorrectURL(utilAccess.getFileName(openedLocation.toString()));
					logger.debug("openedFileName in editorSaved(int operationType) = " + openedFileName);

					String currentContent = "";
					try {
						currentContent = Utils.readFileToString(Paths.get(openedLocation.toURI()));
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
					currentContent = currentContent.substring(xmlPi.length());
					logger.debug("currentContent in editorSaved(int operationType) = " + currentContent);

					WSEditor mainEditor = pluginWorkspace.getEditorAccess(openerLocation,
							PluginWorkspace.MAIN_EDITING_AREA);
					WSAuthorEditorPage mainWSAuthorEditorPage = (WSAuthorEditorPage) mainEditor.getCurrentPage();
					AuthorDocumentController openerAuthorDocumentController = mainWSAuthorEditorPage
							.getDocumentController();

					String openedXpathExpr = pluginWorkspace.getOptionsStorage().getOption(openedFileName + " xpath",
							"");
					logger.debug("openedXpathExpr in editorSaved(int operationType) = " + openedXpathExpr);

					AuthorNode targetNode = null;
					try {
						targetNode = openerAuthorDocumentController.findNodesByXPath(openedXpathExpr, true, true,
								true)[0];
						logger.debug("targetNode in editorSaved(int operationType) = " + targetNode);

						openerAuthorDocumentController.insertXMLFragment(currentContent, targetNode,
								AuthorConstants.POSITION_AFTER);
					} catch (AuthorOperationException e) {
						e.printStackTrace();
					}

					int currentOffset = targetNode.getStartOffset();
					logger.debug("currentOffset in editorSaved(int operationType) = " + currentOffset);

					openerAuthorDocumentController.deleteNode(targetNode);
					mainEditor.save();

					// int caretPosition = ((WSAuthorEditorPage)
					// purePage).getCaretOffset();
					// ((WSAuthorEditorPage)
					// purePage).setCaretPosition(caretPosition);
				}
			});

		}
	}

	public boolean insertContent(String arg0) {
		return false;
	}

	public void refresh(AuthorInplaceContext arg0) {
	}
}
