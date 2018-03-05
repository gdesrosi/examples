import java.io.File;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestJunitHelloWorld {
	
	private String rootDir = "./";
	private String testDir = rootDir
				+ "src/test/resources/";
	private String schematronPath = testDir +"helloWorld.sch";
	private String invalidSchematronPath = testDir +"invalidSchematron.sch";
	private String validXMLPath = testDir +"helloWorld.xml";
	private String invalidElementChildNumXMLPath = testDir +"testIncorrectNumChildren.xml";
	private String invalidContentXMLPath = testDir +"testIncorrectContent.xml";
	   
    @Test
    public void testvalidateXMLViaPureSchematron() throws Exception{
	    
    	//using reflection to test the private method from HelloWorld class
	    HelloWorld instance = new HelloWorld();
	    Method validateSchematron = HelloWorld.class.getDeclaredMethod("validateXMLViaPureSchematron", 
		  	   File.class, File.class);
	    validateSchematron.setAccessible(true);
	   
	    //test valid schematron 
        assertEquals(true, (boolean)validateSchematron.invoke(instance,
           	new File(schematronPath), new File(validXMLPath)));
        
        //test schematron with element containing more children than defined
        assertEquals(false, (boolean)validateSchematron.invoke(instance,
     		new File(schematronPath), new File(invalidElementChildNumXMLPath)));
       
        //test schematron containing invalid content
        assertEquals(false, (boolean)validateSchematron.invoke(instance,
      		new File(schematronPath), new File(invalidContentXMLPath)));
       
        //test schematron for illegalArg exception
        try {
            boolean isValid = (boolean)validateSchematron.invoke(instance,
           		  new File(invalidSchematronPath), new File(validXMLPath));
           
            isValid = (boolean)validateSchematron.invoke(instance,
             		  new File(schematronPath), new File("NONE_EXISTENT_PATH"));   
        } catch (Exception aEx) {
            assertTrue(aEx.getCause() instanceof IllegalArgumentException);
        }
    }
}