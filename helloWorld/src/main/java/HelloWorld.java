
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.daffodil.japi.Compiler;
import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.Diagnostic;
import org.apache.daffodil.japi.ParseResult;
import org.apache.daffodil.japi.ProcessorFactory;
import org.apache.daffodil.japi.UnparseResult;
import org.apache.daffodil.japi.infoset.W3CDOMInfosetInputter;
import org.apache.daffodil.japi.infoset.W3CDOMInfosetOutputter;
import org.oclc.purl.dsdl.svrl.FailedAssert;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;

/**
 * Demonstrates using the Daffodil DFDL processor to
 * <ul>
 * <li>compile a DFDL schema
 * <li>parse non-XML data into XML,
 * <li>perform xsd validation,
 * <li>perform schematron validation,
 * <li>access the data it using XPath,
 * <li>transform the data using XSLT
 * <li>unparse the transformed data back to non-XML form.
 * </ul>
 */
public class HelloWorld {

	final static String SCHFAILURETAG = "SCHEMATRON_VIOLATION:";
	final static String XSDFAILURETAG = "XSD_VIOLATION:";
	final static TransformerFactory TRANSFACTORY = TransformerFactory.newInstance();

	public static void main(String[] args) throws IOException {

		String rootDir = "./";
		String testDir = rootDir + "src/test/resources/";
		String schemaFilePath = testDir + "helloWorld.dfdl.xsd";
		String dataFilePath = testDir + "helloWorld.dat";
		String schematronFilePath = testDir + "helloWorld.sch";

		//
		// First compile the DFDL Schema
		//
		Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(true); // makes sure the DFDL schema is valid itself.
		File schemaFile = new File(schemaFilePath);
		ProcessorFactory pf = c.compileFile(schemaFile);
		if (pf.isError()) {
			// didn't compile schema. Must be diagnostic of some sort.
			List<Diagnostic> diags = pf.getDiagnostics();
			for (Diagnostic d : diags) {
				System.err.println(d.getSomeMessage());
			}
			System.exit(1);
		}
		DataProcessor dp = pf.onPath("/");
		if (dp.isError()) {
			// didn't compile schema. Must be diagnostic of some sort.
			List<Diagnostic> diags = dp.getDiagnostics();
			for (Diagnostic d : diags) {
				System.err.println(d.getSomeMessage());
			}
			System.exit(1);
		}
		//
		// Parse - parse data to XML
		//
		System.out.println("**** Parsing data into XML *****");
		java.io.File file = new File(dataFilePath);
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels.newChannel(fis);
		//
		// Setup W3CDOM outputter
		//
		W3CDOMInfosetOutputter outputter = new W3CDOMInfosetOutputter();
		// Do the parse
		//
		ParseResult res = dp.parse(rbc, outputter);

		// Check for errors
		//
		boolean err = res.isError();
		if (err) {
			// didn't parse the data. Must be diagnostic of some sort.
			List<Diagnostic> diags = res.getDiagnostics();
			for (Diagnostic d : diags) {
				System.err.println(d.getSomeMessage());
			}
			System.exit(2);
		}

		Document doc = outputter.getResult();
		//
		// if we get here, we have a parsed infoset result!
		// Let's print the XML infoset.
		//
		// Note that if we had only wanted this text, we could have used
		// a different outputter to create XML text directly,
		// but below we're going to transform this DOM tree.
		//
		try {
			printDocument(doc, System.out);
		} catch (TransformerException aEx) {
			System.out.println(aEx.getMessage());
		}

		String documentName = doc.getDocumentElement().getNodeName();
		// validating XSD
		try {
			System.out.println("***** XSD VALIDATION *****");
			boolean isValid = validateXSD(schemaFile, doc);
			if (isValid) {
				System.out.println(documentName + " is VALIDATED by " + schemaFile.getPath());
			} else {
				System.out.println(documentName + " is NOT VALIDATED by " + schemaFile.getPath());
			}
		} catch (IllegalArgumentException aEx) {
			System.out.println(aEx.getMessage());
		}

		// validate schematron
		try {
			File schematronFile = new File(schematronFilePath);
			System.out.println("***** SCHEMATRON VALIDATION *****");
			boolean isValid = validateXMLViaPureSchematron(schematronFile, doc);
			if (isValid) {
				System.out.println(documentName + " is VALIDATED by " + schematronFile.getPath());
			} else {
				System.out.println(documentName + " is NOT VALIDATED by " + schematronFile.getPath());
			}
		} catch (IllegalArgumentException aEx) {
			System.out.println("IllegalArgException:" + aEx.getMessage());
		} catch (Exception aEx) {
			// catching generic exception thrown by ph-schematron library,
			// unsure of what type of exception that is thrown
			System.out.println("Exception:" + aEx.getMessage());
		}

		// If all you need to do is parse things to XML, then that's it.

		// XPATH - use it to access the data
		//

		String txt = "";
		try {
			System.out.println("**** Access with XPath *****");
			XPathExpression expr = setupXPath("/tns:helloWorld/word[2]/text()");
			
			txt = (String) expr.evaluate(doc, XPathConstants.STRING);
			System.out.println(String.format("XPath says we said hello to %s", txt));
		} catch (XPathExpressionException aEx) {
			System.out.println(aEx.getMessage());
		} finally {
			if (txt.isEmpty()) {
				System.err.println("XPath produced nothing.");
				System.exit(1);
			}
		}

		//
		// XSLT - use it to transform the data
		//

		// create xslt doc
		Document xsltDocument = null;
		try {
			String xsltFilePath = testDir + "helloWorld.xslt";
			xsltDocument = createDocument(new File(xsltFilePath));
		} catch (ParserConfigurationException aEx) {
			System.out.println(aEx.getMessage());
		} catch (SAXException aEx) {
			System.out.println(aEx.getMessage());
		}
		// exit if failed to create xslt document
		if (xsltDocument == null) {
			System.err.println("Failed to create XSLT document.");
			System.exit(1);
		}

		// transform the document
		Document doc2 = null;
		try {
			System.out.println("**** Transform with XSLT *****");
			doc2 = w3cDomtransformer(doc, xsltDocument);
		} catch (TransformerException aEx) {
			System.out.println(aEx.getMessage());
		} catch (ParserConfigurationException aEx) {
			System.out.println(aEx.getMessage());
		} catch (FactoryConfigurationError aEx) {
			System.out.println(aEx.getMessage());
		}
		// exit if failed to transform document
		if (doc2 == null) {
			System.err.println("Failed to transform document.");
			System.exit(1);
		}

		// print the transformed document
		try {
			printDocument(doc2, System.out);
		} catch (TransformerException aEx) {
			System.out.println(aEx.getMessage());
		}

		//
		// Unparse back to native format
		//

		// If you need to also convert XML back into the native data format
		// you need to "unparse" the infoset back to data.
		//
		// Not all DFDL schemas are setup for unparsing. There are some things
		// you need for unparsing that just don't need to be present in the
		// schema if you only intend to do parsing.
		//
		// But let's assume your DFDL schema is one that is able to be used both
		// for parsing and unparsing data.
		//
		// So let's try unparsing
		//
		// We'll just store the result of unparsing into this
		// ByteArrayOutputStream.
		//
		System.out.println("**** Unparsing XML infoset back into data *****");

		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		java.nio.channels.WritableByteChannel wbc = java.nio.channels.Channels.newChannel(bos);
		W3CDOMInfosetInputter inputter = new W3CDOMInfosetInputter(doc2);
		UnparseResult res2 = dp.unparse(inputter, wbc);
		err = res2.isError();

		if (err) {
			// didn't unparse. Must be diagnostic of some sort.
			List<Diagnostic> diags = res2.getDiagnostics();
			for (Diagnostic d : diags) {
				System.err.println(d.getSomeMessage());
			}
			System.exit(1);
		}

		// if we get here, unparsing was successful.
		// The bytes that have been output are in bos
		byte[] ba = bos.toByteArray();

		//
		// Display the resulting data, as text (iso-8859-1), and hex
		//

		// If your data format was textual, then you can print it out as text
		// but we need to know what the text encoding was
		String encoding = "iso-8859-1"; // an encoding where every byte value is a legal character.

		java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(ba);
		java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(bis, encoding));
		String line;
		System.out.println("Data as text in encoding " + encoding);
		while ((line = r.readLine()) != null) {
			System.out.println(line);
		}

		// If your data format was binary, then you can print it out as hex
		// just to get a look at it.
		System.out.println("Data as hex");
		for (byte b : ba) {
			int bi = b; // b could be negative, but we want the hex to look like it was unsigned.
			bi = bi & 0xFF;
			System.out.print(String.format("%02X ", bi));
		}
		System.out.println("");

	}

	/**
	 * Does the boilerplate stuff needed for xpath expression setup
	 * 
	 * @return the compiled XPathExpression object which can be evaluated to run it.
	 * @throws XPathExpressionException
	 */
	private static XPathExpression setupXPath(String xpathExpression) throws XPathExpressionException {
		// Need this namespace definition since the schema defines the root
		// element in this namespace.
		//
		// A real application would hoist this boilerplate all out so it's done
		// once, not each time we need to evaluate an XPath expression.
		//
		Map<String, String> nameSpaceMap = new HashMap<String, String>();
		nameSpaceMap.put("tns", "http://example.com/dfdl/helloworld/");
		NamespaceContext namespaces = new SimpleNamespaceContext(nameSpaceMap);

		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		xpath.setNamespaceContext(namespaces);
		XPathExpression expr = xpath.compile(xpathExpression);

		return expr;
	}

	/**
	 * checks if an XML document is validated by it's XSD
	 * 
	 * @param xsdFile the schema definition file.
	 * @param xmlDocument the xml document to be validated.
	 * @return true if xmlDocument is validated by the xsdFile, 
	 *         otherwise return false.
	 * @throws IOException if xsdFile isn't found.
	 * @throws IllegalArgumentException if schema is invalid.
	 */
	static boolean validateXSD(final File xsdFile, final Document xmlDocument)
			throws IOException, IllegalArgumentException {

		SchemaFactory tFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema tSchema = null;
		try {
			tSchema = tFactory.newSchema(xsdFile);
		} catch (SAXException aEx) {
			String message = xsdFile.getPath() + "\nCause:" + aEx.getMessage();
			throw new IllegalArgumentException("Invalid schema:" + message);
		}
		try {
			Validator tValidator = tSchema.newValidator();
			tValidator.validate(new DOMSource(xmlDocument));
		} catch (SAXException aEx) {
			System.out.println(XSDFAILURETAG + aEx.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * checks if an XML document is validated by it's schematron.
	 * 
	 * @param schematronFile the Schematron file.
	 * @param xmlDocument the xml document to be validated.
	 * @return true if xmlDocument is validated by the schematronFile, 
	 *         otherwisereturn false.
	 * @throws IllegalArgumentException if schematronFile is invalid.
	 * @throws Exception unknown exception thrown by phschematron library
	 */
	static boolean validateXMLViaPureSchematron(final File schematronFile, final Document xmlDocument)
			throws Exception {

		final ISchematronResource resPure = SchematronResourcePure.fromFile(schematronFile);
		if (!resPure.isValidSchematron()) {
			throw new IllegalArgumentException("Invalid Schematron:" + schematronFile.getPath());
		}

		SchematronOutputType schematronOutputType = 
				resPure.applySchematronValidationToSVRL(new DOMSource(xmlDocument));
		List<Object> failedAsserts = schematronOutputType.getActivePatternAndFiredRuleAndFailedAssert();

		boolean isValid = true;
		for (Object object : failedAsserts) {
			if (object instanceof FailedAssert) {
				isValid = false;
				FailedAssert failedAssert = (FailedAssert) object;
				System.out.println(SCHFAILURETAG + failedAssert.getTest());
				System.out.println(SCHFAILURETAG + failedAssert.getText());
			}
		}
		return isValid;
	}

	/**
	 * prints a w3c document nicely formatted.
	 * 
	 * @param doc the document to be printed.
	 * @param out the outpout stream.
	 * @throws IOException
	 * @throws TransformerException
	 */
	private static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {

		Transformer transformer = TRANSFACTORY.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		DOMSource ds = new DOMSource(doc);
		OutputStreamWriter os = new OutputStreamWriter(out, "UTF-8");
		StreamResult sr = new StreamResult(os);
		transformer.transform(ds, sr);
	}

	/**
	 * transforms a w3c document using xslt.
	 * 
	 * @param dxml the document to be transformed.
	 * @param xslt the xslt transfrom document.
	 * @return transformedDoc the transformed document.
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws FactoryConfigurationError
	 */
	private static Document w3cDomtransformer(Document xml, Document xslt)
			throws TransformerException, ParserConfigurationException, FactoryConfigurationError {

		Source xmlSource = new DOMSource(xml);
		Source xsltSource = new DOMSource(xslt);
		DOMResult result = new DOMResult();

		Transformer transformer = TRANSFACTORY.newTransformer(xsltSource);
		transformer.transform(xmlSource, result);
		Document transformedDoc = (Document) result.getNode();

		return transformedDoc;
	}

	/**
	 * creates a w3c document from xml source.
	 * 
	 * @param xmlFile the xml file.
	 * @return doc the created document.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	static Document createDocument(final File xmlFile) 
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xmlFile);
		return doc;
	}
	
	/**
	 * simple namespace context implementation needed to set the name space in order
	 * to acces data through xpath
	 */
	static class SimpleNamespaceContext implements NamespaceContext {

		private final Map<String, String> namespaceMap = new HashMap<String, String>();

		public SimpleNamespaceContext(final Map<String, String> map) {
			namespaceMap.putAll(map);
		}

		@Override
		public String getNamespaceURI(String prefix) {
			return namespaceMap.get(prefix);
		}

		@Override
		public String getPrefix(String uri) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<String> getPrefixes(String uri) {
			throw new UnsupportedOperationException();
		}
	}
}
