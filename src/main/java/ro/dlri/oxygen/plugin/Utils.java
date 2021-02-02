package ro.dlri.oxygen.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.IncorrectSchemaException;
import com.thaiopensource.validate.Schema;
import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.Validator;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.value.StringValue;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;

public class Utils {

	private static final Logger logger = Logger.getLogger(Utils.class.getName());
	protected static String xmlPi = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	protected static Charset utf8 = StandardCharsets.UTF_8;

	public static ArrayList<StringValue> validate() {
		WSEditor currentEditor = PluginWorkspaceProvider.getPluginWorkspace()
				.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
		logger.debug("currentEditor = " + currentEditor.getEditorLocation());

		ArrayList<StringValue> result = new ArrayList<StringValue>();

		try {
			SchemaReader schemaReader = new AutoSchemaReader();

			Schema schema = schemaReader.createSchema(
					new InputSource(new URL("file:" + getFrameworkDir() + "/schema/tei_all.rng").openStream()),
					PropertyMap.EMPTY);
			logger.debug("schema = " + schema);

			ErrorHandler errorHandler = new StringErrorHandler(result);

			PropertyMapBuilder propertyMapBuilder = new PropertyMapBuilder();
			propertyMapBuilder.put(ValidateProperty.ERROR_HANDLER, errorHandler);

			Validator validator = schema.createValidator(propertyMapBuilder.toPropertyMap());

			TransformerFactory.newInstance().newTransformer().transform(
					new StreamSource(currentEditor.createContentReader()),
					new SAXResult(validator.getContentHandler()));
		} catch (SAXException | IOException | IncorrectSchemaException | TransformerException
				| TransformerFactoryConfigurationError e) {
			logger.info("validation errors = " + e.getLocalizedMessage());
		}
		logger.debug("validationProblems = " + result);

		return result;
	}

	public static XdmValue transform(Reader xml, InputStream xquery, boolean omitXmlDeclaration, URI baseURI,
			Map<String, String> parameters) throws SaxonApiException, IOException {
		XdmValue result = null;

		Source xmlSrc = new StreamSource(xml);

		Configuration configuration = new Configuration();
		configuration.setErrorListener(new ErrorListener() {
			@Override
			public void warning(TransformerException exception) throws TransformerException {
				JOptionPane.showMessageDialog(null, "<html><body><p style='width: 200px;'>"
						+ exception.getMessageAndLocation() + "</p></body></html>", "Error", JOptionPane.ERROR_MESSAGE);
				System.out.println("=============== warning");
			}

			@Override
			public void error(TransformerException exception) throws TransformerException {

				JOptionPane.showMessageDialog(null, "<html><body><p style='width: 200px;'>"
						+ exception.getMessageAndLocation() + "</p></body></html>", "Error", JOptionPane.ERROR_MESSAGE);
				System.out.println("=============== error");
			}

			@Override
			public void fatalError(TransformerException exception) throws TransformerException {
				JOptionPane.showMessageDialog(null, "<html><body><p style='width: 200px;'>"
						+ exception.getMessageAndLocation() + "</p></body></html>", "Error", JOptionPane.ERROR_MESSAGE);
			}
		});

		Processor proc = new Processor(configuration);
		XQueryCompiler xqueryCompiler = proc.newXQueryCompiler();

		if (baseURI != null) {
			xqueryCompiler.setBaseURI(baseURI);
		}

		XQueryExecutable xqueryExecutable = xqueryCompiler.compile(xquery);
		XQueryEvaluator xqueryEvaluator = xqueryExecutable.load();
		xqueryEvaluator.setSource(xmlSrc);

		for (Entry<String, String> parameter : parameters.entrySet()) {
			xqueryEvaluator.setExternalVariable(new QName(parameter.getKey()),
					new XdmAtomicValue(parameter.getValue()));
		}

		result = xqueryEvaluator.evaluate();

		return result;
	}

	public static String getFrameworkDir() {
		WSEditor currentEditor = PluginWorkspaceProvider.getPluginWorkspace()
				.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);

		String frameworkDir = new File(currentEditor.getDocumentTypeInformation().getFrameworkStoreLocation())
				.getParent();
		logger.debug("frameworkDir = " + frameworkDir);

		return frameworkDir;
	}

	public static String expandEditorVariables(String editorVariable) {
		logger.debug("editorVariable = " + editorVariable);

		String expandedVariable = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess()
				.expandEditorVariables(editorVariable, null);
		logger.debug("expandedVariable = " + expandedVariable);

		return expandedVariable;
	}

	public static void writeStringToFile(Path filePath, String content) {
		try {
			Files.write(filePath, content.getBytes(utf8), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeLinesFile(Path filePath, List<String> lines) {
		try {
			Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String readFileToString(Path filePath) {
		String result = null;
		try {
			result = new String(Files.readAllBytes(filePath), utf8);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static void forceDelete(Path path) {
		try {
			Files.walk(path).sorted((a, b) -> b.compareTo(a)).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void updateDatalist(String datalistId, String labels, String values) {
		logger.debug("datalistId = " + datalistId);
		logger.debug("labels = " + labels);
		logger.debug("values = " + values);

		ArrayList<String> fileContent = new ArrayList<>();
		fileContent.add("@charset \"utf-8\";");
		fileContent.add("@" + datalistId + "-labels: \"" + labels + "\";");
		fileContent.add("@" + datalistId + "-values: \"" + values + "\";");
		logger.debug("fileContent = " + fileContent);

		String frameworkDir = getFrameworkDir();
		logger.debug("frameworkDir = " + frameworkDir);

		Path cssResourcesDirectory = Paths.get(frameworkDir, "resources", "css");
		logger.debug("cssResourcesDirectory = " + cssResourcesDirectory);

		Path datalistsDirectory = cssResourcesDirectory.resolve("datalists");
		logger.debug("datalistsDirectory = " + datalistsDirectory);

		try {
			Path datalistFile = Files.write(datalistsDirectory.resolve(datalistId + ".less"), fileContent, utf8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			logger.debug("datalistFile = " + datalistFile);

			Path frameworkDescriptorFile = cssResourcesDirectory.resolve("framework.less");
			logger.debug("frameworkDescriptorFile = " + frameworkDescriptorFile);

			long currentTime = System.currentTimeMillis();
			FileTime fileTime = FileTime.from(currentTime, TimeUnit.MILLISECONDS);

			Files.setLastModifiedTime(frameworkDescriptorFile, fileTime);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static class StringErrorHandler extends ErrorHandlerImpl {

		private ArrayList<StringValue> result = new ArrayList<StringValue>();

		public StringErrorHandler(ArrayList<StringValue> result) {
			this.result = result;
		}

		public void warning(SAXParseException e) throws SAXParseException {
			super.warning(e);
		}

		public void error(SAXParseException e) {
			super.error(e);
		}

		public void printException(Throwable e) {
			super.printException(e);
		}

		public void print(String message) {
			StringValue item = new StringValue(message);
			result.add(item);
		}
	}

}
