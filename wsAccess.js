function applicationStarted(pluginWorkspaceAccess) {
    Packages.java.lang.System.err.println("Application started " + pluginWorkspaceAccess);

	pluginWorkspaceAccess.addEditorChangeListener(new Packages.ro.sync.exml.workspace.api.listeners.WSEditorChangeListener() {
		@Override
		public void editorOpened(Packages.java.net.URL url) {
			var Packages.ro.sync.exml.workspace.api.editor.WSEditor editorAccess = pluginWorkspaceAccess.getEditorAccess(url, Packages.ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA);
			editorAccess.addEditorListener(new Packages.ro.sync.exml.workspace.api.listeners.WSEditorListener() {
				@Override
				public boolean editorAboutToBeSavedVeto(int operation) {
					if (editorAccess.getCurrentPageID().equals(EditorPageConstants.PAGE_AUTHOR)) {
						try {
							String currentEditorLocation = URLDecoder.decode(
									editorAccess.getEditorLocation().toURI().toASCIIString(),
									Utils.utf8.displayName());
							logger.debug("currentEditorLocation = " + currentEditorLocation);

							String renderedFileRelativePath = currentEditorLocation.substring(currentEditorLocation.indexOf("entries") + 7);
							URL giteaURL = new URL("http://188.212.37.221:3000/api/v1/repos/solirom/dlr-data/contents/2" + renderedFileRelativePath + "?access_token=" + giteaAccessToken);
							logger.debug("giteaURL = " + giteaURL);
							
							URLConnection getRequest = giteaURL.openConnection();
							BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getRequest.getInputStream()));
							String getRequestLine = bufferedReader.readLine();
							bufferedReader.close();
							
							logger.debug("getRequestLine = " + getRequestLine);

							//return JSON.parse(String(inputLine));
							
							
							logger.debug("renderedFileRelativePath = " + renderedFileRelativePath);
						} catch (URISyntaxException | IOException | IndexOutOfBoundsException
								| SaxonApiUncheckedException e) {
							JOptionPane.showMessageDialog(new JFrame(), e.getMessage(), "Error",
									JOptionPane.ERROR_MESSAGE);
						}

						// var githubUrl = new
						// java.net.URL("http://188.212.37.221:3000/api/v1/repos/solirom/solirom-ontology/contents/"
						// + fileName);

						// a6ddbb24ea29bee69670815cd4aca6b6703940cc

					}

					return true;
				}
			});
		}
	}, Packages.ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA);
}

function applicationClosing(pluginWorkspaceAccess) {
    Packages.java.lang.System.err.println("Application closing " + pluginWorkspaceAccess);
}