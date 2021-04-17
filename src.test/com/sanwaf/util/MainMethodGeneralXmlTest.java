package com.sanwaf.util;

import org.junit.Test;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MainMethodGeneralXmlTest {
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  static final String TEST_BUILD_PATH = "test-build-path";
  static String rootPath = new File(GenerateXml.class.getClassLoader().getResource("").getFile() + TEST_BUILD_PATH).toString();
  static String sanwafFilePath = new File(GenerateXml.class.getClassLoader().getResource("").getFile() + TEST_BUILD_PATH + "/sanwaf.xml").toString();
  List<String> extentions = Arrays.asList(".html");
  
  @Before
  public void setUpStreams() {
    //System.setOut(new PrintStream(outContent));
  }

  @After
  public void restoreStreams() {
    //System.setOut(originalOut);
  }

  @Test
  public void testMainUsage() throws IOException {
    String[] args = null;
    GenerateXml.main(args);

    args = new String[] {"1"};
    GenerateXml.main(args);

    args = new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    GenerateXml.main(args);
    //String s = outContent.toString();
    //assertTrue(s.contains("Sanwaf-ui-2-server Generate XML Usage"));
}
  
  @Test
  public void testMainInvalidPath() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    //GenerateXml o = new GenerateXml(rootPath, extensions, true, false, null, false, null); 

    String[] args = new String[] {"--path:", "--exts:.html,.jsp", "--output:true"};
    GenerateXml.main(args);
  }

  @Test
  public void testMainValidPath() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String[] args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true"};
    GenerateXml.main(args);
  }

  @Test
  public void testMainNonSanwaf() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String[] args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--endpoints:true", "--nonSanwaf:true"};
    GenerateXml.main(args);
  }

  @Test
  public void testMainEndpoints() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String[] args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--endpoints:true"};
    GenerateXml.main(args);
  }

  @Test
  public void testMainEndpointsStrict() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String[] args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--endpoints:true", "--strict:true"};
    GenerateXml.main(args);

    args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--endpoints:true", "--strict:false"};
    GenerateXml.main(args);

    args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--endpoints:true", "--strict:less"};
    GenerateXml.main(args);

    args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--endpoints:true", "--strict:<"};
    GenerateXml.main(args);

    args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--endpoints:true", "--strict:invalid"};
    GenerateXml.main(args);
  }

  @Test
  public void testMainUpdateFile() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String[] args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--file:" + sanwafFilePath};
    GenerateXml.main(args);
  }

  @Test
  public void testMainUpdateFileAppend() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String[] args = new String[] {"--path:" + rootPath, "--extensions:.html,.jsp", "--output:true", "--file:" + sanwafFilePath, "--append:true"};
    GenerateXml.main(args);
  }

}
