package ro.dlri.oxygen.plugin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.apache.log4j.Logger;

import net.sf.saxon.s9api.SaxonApiUncheckedException;
import ro.sync.db.DBConnectionInfo;
import ro.sync.db.core.DBSourceDriverInfo;
import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;
import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.options.PerspectivesLayoutInfo;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.options.DataSourceConnectionInfo;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.MenuBarCustomizer;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ToolbarComponentsCustomizer;
import ro.sync.exml.workspace.api.standalone.ToolbarInfo;

/**
 * Plugin extension - dlri extension.
 */
public class DlriOxygenPluginExtension implements WorkspaceAccessPluginExtension {

	private static final Logger logger = Logger.getLogger(DlriOxygenPluginExtension.class.getName());

	static boolean isWindows;

	private static String frameworkId = "dlri";

	private static String datasourceName = frameworkId;
	private static String connectionName = frameworkId + ".ro";
	private static String workCollectionPath = "/db/data/dlr/entries/";
	private Path pluginInstallDir;
	private File frameworkContainerDir;
	private String frameworkDir;
	private Path frameworkDirPath;
	private Path templatesDir;
	private Path resourcesDir;
	private Path viewsDirPath;
	private Path renderingDirPath;
	private static Path eXistFilesBaseDir;
	private String renderingTemplateNamePrefix = "redare_";

	private static String dlr_host = "exist.solirom.ro";
	private static String dlr_port = "";
	private static String dlr_app_url = "http://" + dlr_host + dlr_port + "/exist/apps/dlr-api/";
	private static String dlr_app_apis_url = dlr_app_url + "api/";
	private static String get_usernames_api_url = dlr_app_apis_url + "users/";
	private static String get_server_version_api_url = dlr_app_apis_url + "system/version/";
	private static String get_jnlp_jars_urls_api_url = dlr_app_apis_url + "system/jnlp-jar-urls/";
	private static String user_login_api_url = dlr_app_apis_url + "users/login";
	private static String get_entry_full_api_url = dlr_app_apis_url + "entry/full";
	private static String get_sigla_api_url = "http://" + dlr_host + dlr_port + "/exist/rest/db/data/dlr/bibliographic-references.xml";

	public JFrame parentFrame;

	private StandalonePluginWorkspace pluginWorkspaceAccess;
	private Action fileExitAction = null;
	private Action fileSaveAction = null;
	private Action addonUpdatesAction = null;
	private WSOptionsStorage optionsStorage = null;
	private DataSourceConnectionInfo dataSourceConnectionInfo = null;

	static {
		isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
	}

	@Override
	public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
		this.setPluginWorkspaceAccess(pluginWorkspaceAccess);

		pluginInstallDir = Paths.get(PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess()
				.expandEditorVariables("${pluginDir(ro.dlri.oxygen.plugin)}", null));
		logger.debug("pluginInstallDir = " + pluginInstallDir);

		frameworkDir = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess()
				.expandEditorVariables("${frameworkDir(" + frameworkId + ")}", null);
		logger.debug("frameworkDir = " + frameworkDir);

		frameworkDirPath = Paths.get(frameworkDir);
		logger.debug("frameworkDirPath = " + frameworkDirPath);

		frameworkContainerDir = frameworkDirPath.toFile();
		logger.debug("frameworkContainerDir = " + frameworkContainerDir);

		templatesDir = frameworkDirPath.resolve("templates");
		logger.debug("templatesDir = " + templatesDir);

		resourcesDir = frameworkDirPath.resolve("resources");
		logger.debug("resourcesDir = " + resourcesDir);

		viewsDirPath = frameworkDirPath.resolve(Paths.get("views"));
		logger.debug("viewsDirPath = " + viewsDirPath);

		eXistFilesBaseDir = Paths.get(getPluginWorkspaceAccess().getPreferencesDirectory())
				.resolve("eXistdb/" + dlr_host + "_" + dlr_port);
		logger.debug("eXistFilesBaseDir = " + eXistFilesBaseDir);

		try {
			renderingDirPath = Files.createDirectories(frameworkDirPath.resolve(Paths.get("tmp", "EntryRendering")));
			logger.debug("renderingDirPath = " + renderingDirPath);

			if (!Files.exists(eXistFilesBaseDir)) {
				Files.createDirectories(eXistFilesBaseDir);
				logger.debug("created eXistFilesBaseDir at " + eXistFilesBaseDir);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		pluginWorkspaceAccess.addMenuBarCustomizer(new MenuBarCustomizer() {
			@Override
			public void customizeMainMenu(JMenuBar mainMenuBar) {
				int menuCount = mainMenuBar.getMenuCount();
				// Iterate over menus to find the needed actions
				for (int i = 0; i < menuCount; i++) {
					// Revert action index in menu
					JMenu menu = mainMenuBar.getMenu(i);
					int itemCount = menu.getItemCount();
					for (int j = 0; j < itemCount; j++) {
						JMenuItem item = menu.getItem(j);
						if (item != null) {
							Action action = item.getAction();
							String oxygenActionID = pluginWorkspaceAccess.getOxygenActionID(action);
							if (oxygenActionID != null) {
								// logger.debug("oxygenActionID" + oxygenActionID);
								switch (oxygenActionID) {
								case "File/File_Exit":
									fileExitAction = action;
									break;
								case "File/File_Save":
									fileSaveAction = action;
									break;
								case "Help/Check_for_addons_updates":
									addonUpdatesAction = action;
									break;
								}
							}
						}
					}
				}

				// mainMenuBar.removeAll();
			}
		});

		Path layoutFile = frameworkDirPath.resolve(Paths.get(frameworkDir, "layout", "dlr.layout"));
		logger.debug("layoutFile = " + layoutFile);

		if (Files.exists(layoutFile)) {
			PerspectivesLayoutInfo info = new PerspectivesLayoutInfo(true, false, "", layoutFile.toString());
			pluginWorkspaceAccess.setGlobalObjectProperty("perspectives.layout.info", info);
		}

		// TODO: temporary fix for ${uuid} in template - this is fixed in v. 21
		pluginWorkspaceAccess.setGlobalObjectProperty("new.dita.topic.use.file.name.for.root.id", Boolean.FALSE);

		// set up the data source and connection
		optionsStorage = getPluginWorkspaceAccess().getOptionsStorage();

		dataSourceConnectionInfo = getPluginWorkspaceAccess().getDataSourceAccess()
				.getDataSourceConnectionInfo(connectionName);

		downloadExistDbFiles();

		// download and store bibliographic-references.xml
		downloadAndStoreBinaryFile(get_sigla_api_url, "bibliographic-references.xml", resourcesDir.resolve("xml"));

		Optional.ofNullable(dataSourceConnectionInfo)
				.map(d1 -> d1.getProperty(DataSourceConnectionInfo.URL).toString().contains("188.212.37.221") ? "exist.solirom.ro"
						: null)
				.ifPresent(d2 -> generateDatasourceConnection());
		Optional.ofNullable(dataSourceConnectionInfo)
				.map(d1 -> d1.getProperty(DataSourceConnectionInfo.INITIAL_DATABASE).toString().contains("dlr-app")
						? "dlr"
						: null)
				.ifPresent(d2 -> generateDatasourceConnection());
		Optional.ofNullable(dataSourceConnectionInfo).orElseGet(() -> generateDatasourceConnection());

		// add plugin's toolbar
		pluginWorkspaceAccess.addToolbarComponentsCustomizer(new ToolbarComponentsCustomizer() {
			public void customizeToolbar(final ToolbarInfo toolbarInfo) {

				String toolbarId = toolbarInfo.getToolbarID();
				List<JComponent> components = new ArrayList<JComponent>();

				if (toolbarId.equals("toolbar.review")) {
					toolbarInfo.setComponents(components.toArray(new JComponent[0]));
				}

				if (toolbarId.equals("DlrToolbar")) {
					// parentFrame = (JFrame)
					// pluginWorkspaceAccess.getParentFrame();
					//

					//
					// final String content = new
					// Scanner(getClass().getResourceAsStream("toolbar.html"),
					// Utils.utf8).useDelimiter("\\A").next();
					//
					// JavaFXPanel panel = new JavaFXPanel(content, null, new
					// Java());
					//
					// panel.setPreferredSize(new Dimension(300, 45));

					JButton lemmaButton = new JButton("Lemă");
					lemmaButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							String templateContent = Utils.readFileToString(templatesDir.resolve("lemma.xml"));
							pluginWorkspaceAccess.createNewEditor("xml", "text/xml", PluginWorkspaceProvider
									.getPluginWorkspace().getUtilAccess().expandEditorVariables(templateContent, null));
						}
					});

					JButton variantButton = new JButton("Variantă");
					variantButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							String templateContent = Utils.readFileToString(templatesDir.resolve("variant.xml"));
							pluginWorkspaceAccess.createNewEditor("xml", "text/xml", PluginWorkspaceProvider
									.getPluginWorkspace().getUtilAccess().expandEditorVariables(templateContent, null));
						}
					});

					JButton saveButton = new JButton();
					saveButton.setAction(fileSaveAction);
					saveButton.setText("Salvare");

					JButton validationButton = new JButton("Validare");
					validationButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent event) {
							WSEditor currentEditor = pluginWorkspaceAccess
									.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);

							try {
								URL currentEditorLocation = new URL(
										URLDecoder.decode(currentEditor.getEditorLocation().toURI().toASCIIString(),
												Utils.utf8.displayName()));

								String validationDir = frameworkDir + "services/validation/";
								logger.debug("validationDir = " + validationDir);

								String xqueryScriptURL = validationDir + "validation.xq";
								xqueryScriptURL = xqueryScriptURL.replaceAll("/", "%2F");
								logger.debug("xqueryScriptURL = " + xqueryScriptURL);

								URL newEditorURL = new URL("convert:/processor=xquery;ss=" + xqueryScriptURL + "!/"
										+ currentEditorLocation);
								logger.debug("newEditorURL = " + newEditorURL);

								pluginWorkspaceAccess.open(newEditorURL, EditorPageConstants.PAGE_AUTHOR, "text/html");
								WSEditor newEditor = pluginWorkspaceAccess
										.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
								newEditor.setEditorTabText(
										"validate:" + new File(currentEditorLocation.getFile()).getName());
							} catch (MalformedURLException | UnsupportedEncodingException | URISyntaxException e) {
								e.printStackTrace();
							}
						}
					});

					JButton renderButton = new JButton("Redare");
					renderButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent event) {
							WSEditor currentEditor = pluginWorkspaceAccess
									.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
							WSAuthorEditorPage authorEditorPage = (WSAuthorEditorPage) currentEditor.getCurrentPage();
							AuthorDocumentController authorDocumentController = authorEditorPage
									.getDocumentController();
							AuthorNode rootNode = null;
							try {
								rootNode = authorDocumentController.findNodesByXPath("/*", true, true, true)[0];
							} catch (AuthorOperationException e1) {
								e1.printStackTrace();
							}
							String documentID = ((AuthorElement) rootNode).getAttribute("xml:id").getValue();
							String documentURL = get_entry_full_api_url + "?id=" + documentID;

							logger.debug("documentURL = " + documentURL);

							try {
								URL currentEditorLocation = new URL(
										URLDecoder.decode(currentEditor.getEditorLocation().toURI().toASCIIString(),
												Utils.utf8.displayName()));
								logger.debug("currentEditorLocation = " + currentEditorLocation);

								String renderedFileName = Paths.get(currentEditorLocation.getFile()).getFileName()
										.toString();
								logger.debug("renderedFileName = " + renderedFileName);

								String renderingTemplateContent = Utils.readFileToString(
										resourcesDir.resolve(Paths.get("xml", "empty-template-for-browser.xml")));
								renderingTemplateContent = renderingTemplateContent.replaceFirst("\\$\\{content-url\\}",
										documentURL);

								Path renderingTemplatePath = renderingDirPath
										.resolve(renderingTemplateNamePrefix + renderedFileName);
								if (!Files.exists(renderingTemplatePath)) {
									Files.createFile(renderingTemplatePath);
								}
								logger.debug("renderingTemplatePath = " + renderingTemplatePath);

								Utils.writeStringToFile(renderingTemplatePath, renderingTemplateContent);

								pluginWorkspaceAccess.open(renderingTemplatePath.toUri().toURL(),
										EditorPageConstants.PAGE_AUTHOR, "text/xml");

								optionsStorage.setOption(renderedFileName, currentEditorLocation.toExternalForm());

								// pluginWorkspaceAccess.addEditorChangeListener(new WSEditorChangeListener() {
								// @Override
								// public void editorSelected(URL editorLocation) {
								// String renderedFileName =
								// PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess()
								// .getFileName(editorLocation.toString());
								//
								// if (renderedFileName.startsWith(renderingTemplateNamePrefix)) {
								// renderedFileName =
								// renderedFileName.substring(renderingTemplateNamePrefix.length());
								// String renderedFilePath = optionsStorage.getOption(renderedFileName, "");
								//
								// try {
								// WSEditor renderedEditor = getPluginWorkspaceAccess().getEditorAccess(
								// new URL(URLDecoder.decode(renderedFilePath,
								// Utils.utf8charset.displayName())),
								// PluginWorkspace.MAIN_EDITING_AREA);
								// writeEditorContentToFile(renderedEditor,
								// renderingDirPath.resolve(renderedFileName + ".html"));
								// } catch (IndexOutOfBoundsException | SaxonApiUncheckedException
								// | IOException | SaxonApiException e) {
								// JOptionPane.showMessageDialog(null,
								// "<html><body><p style='width: 200px;'>" + e.getMessage()
								// + "</p></body></html>",
								// "Error", JOptionPane.ERROR_MESSAGE);
								// }
								// }
								// }
								// }, PluginWorkspace.MAIN_EDITING_AREA);

							} catch (URISyntaxException | IOException | IndexOutOfBoundsException
									| SaxonApiUncheckedException e) {
								JOptionPane.showMessageDialog(new JFrame(), e.getMessage(), "Error",
										JOptionPane.ERROR_MESSAGE);
							}
						}
					});

					JButton entriesButton = new JButton("Intrări");
					entriesButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent event) {
							String templateContent = Utils.readFileToString(
									resourcesDir.resolve(Paths.get("html", "entries", "entries.xml")));
							pluginWorkspaceAccess.createNewEditor("xml", "text/xml", PluginWorkspaceProvider
									.getPluginWorkspace().getUtilAccess().expandEditorVariables(templateContent, null));

							// String content = Get
							// .run("http://188.212.37.221:" + dlr_port +
							// "/apps/dlr-app/services/views/get-redactor-entries.xq?redactor-name="
							// + optionsStorage.getOption("dlri.username", ""));
							//
							// final DialogModel dialogModel = new
							// DialogModel("redactor-entries-dialog",
							// "modal", "Intrări", 700, 400, "both", new
							// String[] { "auto" }, "",
							// content);
							// SwingUtilities.invokeLater(new Runnable() {
							// public void run() {
							// new
							// JavaFXDialog(AddonBuilderPluginExtension.parentFrame,
							// dialogModel);
							// }
							// });
						}
					});

					JButton exitButton = new JButton();
					exitButton.setAction(fileExitAction);
					exitButton.setText("Ieșire");

					JButton addonUpdatesButton = new JButton();
					addonUpdatesButton.setAction(addonUpdatesAction);
					addonUpdatesButton.setText("Actualizare");

					pluginWorkspaceAccess.addMenuBarCustomizer(new MenuBarCustomizer() {
						@Override
						public void customizeMainMenu(JMenuBar mainMenuBar) {

						}
					});

					// comps.add(lemmaButton);
					// comps.add(variantButton);
					components.add(entriesButton);
					components.add(saveButton);
					components.add(validationButton);
					components.add(renderButton);
					components.add(addonUpdatesButton);
					components.add(exitButton);

					toolbarInfo.setComponents(components.toArray(new JComponent[0]));

					toolbarInfo.setTitle("DLR Toolbar");
				}
			}
		});

	}

	@Override
	public boolean applicationClosing() {
		Utils.forceDelete(renderingDirPath);

		return true;
	}

	public StandalonePluginWorkspace getPluginWorkspaceAccess() {
		return pluginWorkspaceAccess;
	}

	public void setPluginWorkspaceAccess(StandalonePluginWorkspace pluginWorkspaceAccess) {
		this.pluginWorkspaceAccess = pluginWorkspaceAccess;
	}

	@SuppressWarnings("rawtypes")
	private DefaultComboBoxModel getUsernames() {
		List<Username> usernames = Arrays.stream(downloadStringFromUrl(get_usernames_api_url).split("\n"))
				.map(Username::new).collect(Collectors.toList());

		@SuppressWarnings("unchecked")
		DefaultComboBoxModel searchCriterionModel = new DefaultComboBoxModel(usernames.toArray());

		return searchCriterionModel;
	}

	private DataSourceConnectionInfo generateDatasourceConnection() {
		JFrame parentFrame = (JFrame) getPluginWorkspaceAccess().getParentFrame();
		OKCancelDialog credentialsDialog = new OKCancelDialog(parentFrame, "Introduceți datele pentru conectare", true);
		credentialsDialog.setLayout(new BoxLayout(credentialsDialog.getContentPane(), BoxLayout.Y_AXIS));
		credentialsDialog.setPreferredSize(new Dimension(500, 200));

		JPanel upperPanel = new JPanel();
		upperPanel.setPreferredSize(new Dimension(100, 100));
		JLabel usernameLabel = new JLabel("Selectați numele de utilizator");
		upperPanel.add(usernameLabel);
		JComboBox<String> searchCriterionComboBox = new JComboBox<String>();
		searchCriterionComboBox.setPrototypeDisplayValue("123456789012345678901234567");
		searchCriterionComboBox.setModel(getUsernames());
		upperPanel.add(searchCriterionComboBox);

		JPanel middlePanel = new JPanel();
		middlePanel.setPreferredSize(new Dimension(100, 100));
		JLabel passwordLabel = new JLabel("Introduceți parola");
		middlePanel.add(passwordLabel);
		JPasswordField passwordField = new JPasswordField("", 22);
		middlePanel.add(passwordField);

		JPanel bottomPanel = new JPanel();
		JLabel messageLabel = new JLabel();
		bottomPanel.add(messageLabel);

		credentialsDialog.getContentPane().add(upperPanel);
		credentialsDialog.getContentPane().add(middlePanel);
		credentialsDialog.getContentPane().add(bottomPanel);

		JButton okButton = credentialsDialog.getOkButton();
		for (ActionListener al : okButton.getActionListeners()) {
			okButton.removeActionListener(al);
		}

		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				messageLabel.setText("Verificare ...");

				String username = ((Username) searchCriterionComboBox.getSelectedItem()).getUserid();
				logger.debug("username = " + username);
				String password = new String(passwordField.getPassword());
				logger.debug("password = " + password);

				String userLoggedIn = downloadStringFromUrl(
						user_login_api_url + "?username=" + username + "&password=" + password);
				logger.debug("userLoggedIn = " + userLoggedIn);

				if (userLoggedIn.equals("false")) {
					messageLabel.setText("Numele de utilizator sau parola nu este corectă!");
				} else {
					credentialsDialog.dispose();
				}
			}
		});

		JButton cancelButton = credentialsDialog.getCancelButton();
		for (ActionListener al : cancelButton.getActionListeners()) {
			cancelButton.removeActionListener(al);
		}

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		credentialsDialog.pack();
		credentialsDialog.setLocationRelativeTo(null);
		credentialsDialog.setVisible(true);
		credentialsDialog.requestFocus();
		credentialsDialog.repaint();

		String username = ((Username) searchCriterionComboBox.getSelectedItem()).getUserid();
		logger.debug("username = " + username);
		String password = new String(passwordField.getPassword());
		logger.debug("password = " + password);

		// download the needed eXist files
		Path eXistFilesDir = pluginInstallDir.resolve("exist");
		logger.debug("eXistFilesDir = " + eXistFilesDir);

		optionsStorage.setOption("dlri.eXistVersion", "");
		URL[] existDbJars = downloadExistDbFiles();

		PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();

		DBSourceDriverInfo dsdi = new DBSourceDriverInfo("eXist", datasourceName, "", existDbJars);

		pluginWorkspace.setGlobalObjectProperty("database.jdbc.drivers.1", new DBSourceDriverInfo[] { dsdi });

		DBConnectionInfo eXistSessionInfo = new DBConnectionInfo(connectionName, datasourceName,
				"xmldb:exist://" + dlr_host + ":" + dlr_port + "/exist/xmlrpc", username, password, null, null,
				workCollectionPath + username);
		pluginWorkspace.setGlobalObjectProperty("database.stored.sessions1",
				new DBConnectionInfo[] { eXistSessionInfo });

		optionsStorage.setOption("dlri.username", username);
		optionsStorage.setOption("dlri.datasourceName", datasourceName);
		optionsStorage.setOption("dlri.connectionName", connectionName);

		return null;
	}

	private URL[] downloadExistDbFiles() {
		String eXistVersion = downloadStringFromUrl(get_server_version_api_url);
		logger.debug("eXistVersion = " + eXistVersion);

		String currentExistVersion = optionsStorage.getOption("dlri.eXistVersion", "");
		logger.debug("currentExistVersion = " + currentExistVersion);

		if (!eXistVersion.equals(currentExistVersion)) {
			optionsStorage.setOption("dlri.eXistVersion", eXistVersion);
			return Pattern.compile(", ").splitAsStream(downloadStringFromUrl(get_jnlp_jars_urls_api_url))
					.map(DlriOxygenPluginExtension::downloadAndStoreEXistBinaryFile).toArray(URL[]::new);
		}

		return null;
	}

	private static URL downloadAndStoreEXistBinaryFile(String sourceUrl) {
		String fileName = sourceUrl.substring(sourceUrl.lastIndexOf('/') + 1, sourceUrl.length());

		return downloadAndStoreBinaryFile(sourceUrl, fileName, eXistFilesBaseDir);
	}

	private static URL downloadAndStoreBinaryFile(String sourceUrl, String fileName, Path targetDirectory) {
		logger.debug("downloading binary file " + sourceUrl);
		URL urlObj;
		URLConnection conn = null;
		URL fileUrl = null;
		InputStream is = null;

		try {
			urlObj = new URL(sourceUrl);
			conn = urlObj.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(70000);
			is = conn.getInputStream();

			ByteArrayOutputStream os = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];
			int len;

			while ((len = is.read(buffer)) != -1) {
				os.write(buffer, 0, len);
			}

			fileUrl = Files.write(targetDirectory.resolve(fileName), os.toByteArray(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING).toUri().toURL();
			logger.debug("stored binary file " + fileUrl);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return fileUrl;

		// solution in java 9
		// try (InputStream in = url.openStream()) {
		// return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		// }
	}

	private static String downloadStringFromUrl(String sourceUrl) {
		String urlData = "";
		URL urlObj;
		URLConnection conn = null;
		logger.debug("downloading text file " + sourceUrl);

		try {
			urlObj = new URL(sourceUrl);
			conn = urlObj.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			urlData = reader.lines().collect(Collectors.joining("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return urlData;

		// solution in java 9
		// try (InputStream in = url.openStream()) {
		// return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		// }
	}
}
