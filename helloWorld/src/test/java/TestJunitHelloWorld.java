import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class TestJunitHelloWorld {

	private String rootDir = "./";
	private String testDir = rootDir + "src/test/resources/";
	private String schematronPath = testDir + "helloWorld.sch";
	private String invalidSchematronPath = testDir + "invalidSchematron.sch";
	private String validXMLPath = testDir + "helloWorld.xml";
	private String invalidElementChildNumXMLPath = testDir + "testIncorrectNumChildren.xml";
	private String invalidContentXMLPath = testDir + "testIncorrectContent.xml";
	private String validXSDPath = testDir + "helloWorld.dfdl.xsd";
	private String invalidXSDTypePath = testDir + "incorrectType.xsd";
	private String invalidXSDElementPath = testDir + "incorrectElement.xsd";
	private String invalidXSDMaxOccursPath = testDir + "incorrectMaxOccurs.xsd";
	private String invalidXSDPath = testDir + "invalidXSD.xsd";
	private OutputStream outputStream;
	PrintStream stdout;

	@Before
	public void setUp() {
		// save current PrintStream
		stdout = System.out;

		// Redirect output
		outputStream = new ByteArrayOutputStream();
		PrintStream tPrintStream = new PrintStream(outputStream);
		System.setOut(tPrintStream);
	}

	// schematron validation unit test cases

	@Test
	public void testPassSCHValidateXML() throws Exception {
		// test passed schematron validation
		Document doc = HelloWorld.createDocument(new File(validXMLPath));
		assertTrue(HelloWorld.validateXMLViaPureSchematron(new File(schematronPath), doc));
	}

	@Test
	public void testFailSCHValidateXMLIncorrectElementChildNum() throws Exception {
		// test failed schematron validation due to
		// element containing more children than defined
		Document doc = HelloWorld.createDocument(new File(invalidElementChildNumXMLPath));
		assertFalse(HelloWorld.validateXMLViaPureSchematron(new File(schematronPath), doc));
		// test failure cause is dumped to console
		assertTrue(outputStream.toString().contains(HelloWorld.SCHFAILURETAG));
	}

	@Test
	public void testFailSCHValidateXMLInvalidContent() throws Exception {
		// test failed schematron validation due to invalid content
		Document doc = HelloWorld.createDocument(new File(invalidContentXMLPath));
		assertFalse(HelloWorld.validateXMLViaPureSchematron(new File(schematronPath), doc));
		// test failure cause is dumped to console
		assertTrue(outputStream.toString().contains(HelloWorld.SCHFAILURETAG));
	}

	@Test
	public void testInvalidSchematron() throws Exception {
		// test exception is thrown for invalid schematron
		Document doc = HelloWorld.createDocument(new File(validXMLPath));
		try {
			HelloWorld.validateXMLViaPureSchematron(new File(invalidSchematronPath), doc);
		} catch (Exception aEx) {
			assertTrue(aEx instanceof IllegalArgumentException);
		}
	}

	// xsd validation unit test cases

	@Test
	public void testPassXSDValidateXML() throws ParserConfigurationException, SAXException, IOException {
		// test passed xsd validation
		Document doc = HelloWorld.createDocument(new File(validXMLPath));
		assertTrue(HelloWorld.validateXSD(new File(validXSDPath), doc));
	}

	@Test
	public void testFailXSDValidateXMLInvalidType() throws ParserConfigurationException, SAXException, IOException {
		// test failed xsd validation due to invalid type
		Document doc = HelloWorld.createDocument(new File(validXMLPath));
		assertFalse(HelloWorld.validateXSD(new File(invalidXSDTypePath), doc));
		// test failure cause is dumped to console
		assertTrue(outputStream.toString().contains(HelloWorld.XSDFAILURETAG));
	}

	@Test
	public void testFailXSDValidateXMLInvalidElement() throws ParserConfigurationException, SAXException, IOException {
		// test failed xsd validation due to invalid element
		Document doc = HelloWorld.createDocument(new File(validXMLPath));
		assertFalse(HelloWorld.validateXSD(new File(invalidXSDElementPath), doc));
		// test failure cause is dumped to console
		assertTrue(outputStream.toString().contains(HelloWorld.XSDFAILURETAG));
	}

	@Test
	public void testFailXSDValidateXMLInvalidXSDMaxOccurs()
			throws ParserConfigurationException, SAXException, IOException {
		// test failed xsd validation due to with invalid element max occur violation
		Document doc = HelloWorld.createDocument(new File(validXMLPath));
		assertFalse(HelloWorld.validateXSD(new File(invalidXSDMaxOccursPath), doc));
		// test failure cause is dumped to console
		assertTrue(outputStream.toString().contains(HelloWorld.XSDFAILURETAG));
	}

	@Test
	public void testInvalidSchema() throws ParserConfigurationException, SAXException, IOException {
		// test exception is thrown for invalid schema
		Document doc = HelloWorld.createDocument(new File(validXMLPath));
		try {
			HelloWorld.validateXSD(new File(invalidXSDPath), doc);
		} catch (Exception aEx) {
			assertTrue(aEx instanceof IllegalArgumentException);
		}
	}

	@After
	public void tearDown() {
		System.setOut(stdout);
	}
}