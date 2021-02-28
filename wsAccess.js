function applicationStarted(pluginWorkspaceAccess) {
    Packages.java.lang.System.out.println("Application started " + pluginWorkspaceAccess);

    var editorOpenedListener = {
        editorOpened: function(editorLocation) {
            var editor = pluginWorkspaceAccess.getEditorAccess(editorLocation, Packages.ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA);

            preSaveListener = {

                /* Called when a document is about to be Saved */
                editorAboutToBeSavedVeto: function(operationType) {
                    if (operationType == Packages.ro.sync.exml.workspace.api.listeners.WSEditorListener.SAVE_OPERATION) {
                        Packages.java.lang.System.out.println("editorAboutToBeSavedVeto " + editorLocation);

                        /* Access root Element of Document and read oxyFramework attribute
                        str = editor.createContentInputStream();
                        reader = new Packages.java.io.InputStreamReader(str, "UTF-8");
                        is = new Packages.org.xml.sax.InputSource(reader);
                        is.setEncoding("UTF-8");
                        dbf = new Packages.javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        db = dbf.newDocumentBuilder();
                        doc = db.parse(is);
                        rootElement = doc.getDocumentElement();
                        */
                    }

                    return true;
                }
            }

            preSaveListener = new JavaAdapter(Packages.ro.sync.exml.workspace.api.listeners.WSEditorListener, preSaveListener);
            if (editor != 0) {
                editor.addEditorListener(preSaveListener);

            }
        }
    }
    var editorOpenedListenerAdapter = new JavaAdapter(Packages.ro.sync.exml.workspace.api.listeners.WSEditorChangeListener, editorOpenedListener);
    /* Add the editor changed listener */
    pluginWorkspaceAccess.addEditorChangeListener(editorOpenedListenerAdapter, Packages.ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA);


    // function for reloading the rendering
    var editorSelectedListener = {
        /*Called when a document  is opened*/
        editorSelected: function(editorLocation) {
            Packages.java.lang.System.out.println("editorSelectedListener " + editorLocation);
        }
    }
    var editorSelectedListenerAdapter = new JavaAdapter(Packages.ro.sync.exml.workspace.api.listeners.WSEditorChangeListener, editorSelectedListener);
    pluginWorkspaceAccess.addEditorChangeListener(editorSelectedListenerAdapter, Packages.ro.sync.exml.workspace.api.PluginWorkspace.MAIN_EDITING_AREA);
}

  
   /* 
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
*/
function applicationClosing(pluginWorkspaceAccess) {
    Packages.java.lang.System.err.println("Application closing " + pluginWorkspaceAccess);
}
