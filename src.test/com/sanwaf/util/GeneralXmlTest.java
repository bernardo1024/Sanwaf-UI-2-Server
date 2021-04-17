package com.sanwaf.util;

import org.junit.Test;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GeneralXmlTest {
  static final String TEST_BUILD_PATH = "test-build-path";
  static String rootPath = new File(GenerateXml.class.getClassLoader().getResource("").getFile() + TEST_BUILD_PATH).toString();
  static String sanwafFilePath = new File(GenerateXml.class.getClassLoader().getResource("").getFile() + TEST_BUILD_PATH + "/sanwaf.xml").toString();
  List<String> extensions = Arrays.asList(".html");
  
  @BeforeClass
  public static void setUpClass() {
  }

  @Test
  public void enpointTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml(rootPath, extensions, true, false, null, false, null); 
    String s = o.process();
    System.out.println(s);
  }

  @Test
  public void nonEnpointTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml(rootPath, extensions, false, false, null, false, ""); 
    String s = o.process();
    System.out.println(s);
  }

  @Test
  public void nonEnpointNoMatchBaseUrlTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml("/some/invalid/path", extensions, false, false, null, false, ""); 
    String s = o.process();
    System.out.println(s);
  }

  @Test
  public void nonEnpointNoMatchBaseUrl2Test() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml(rootPath, extensions, false, false, null, false, ""); 
    String s = o.process();
    System.out.println(s);
  }

  @Test
  public void enpointNoBaseUrlTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml(null, extensions, true, false, null, false, ""); 
    String s = o.process();
    System.out.println(s);
    assertTrue(s.length() == 0);
  }

  @Test
  public void invalidfolderTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml("/some/invalid/path", extensions, true, false, null, false, ""); 
    String s = o.process();
    System.out.println(s);
    assertTrue(s.length() == 0);
  }

  @Test
  public void includeNonAnnotatedTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml(rootPath, extensions, true, true, null, false, ""); 
    String s = o.process();
    System.out.println(s);
    assertTrue(s.length() != 0);
  }

  @Test
  public void strictTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml(rootPath, extensions, true, true, null, false, "true"); 
    String s = o.process();
    System.out.println(s);
    assertTrue(s.contains("<strict>true</strict>"));

    o = new GenerateXml(rootPath, extensions, true, true, null, false, "false"); 
    s = o.process();
    System.out.println(s);
    assertTrue(s.contains("<strict>false</strict>"));

    o = new GenerateXml(rootPath, extensions, true, true, null, false, "<"); 
    s = o.process();
    System.out.println(s);
    assertTrue(s.contains("<strict><</strict>"));
    
    o = new GenerateXml(rootPath, extensions, true, true, null, false, "less"); 
    s = o.process();
    System.out.println(s);
    assertTrue(s.contains("<strict>less</strict>"));
    
    o = new GenerateXml(rootPath, extensions, true, true, null, false, ""); 
    s = o.process();
    System.out.println(s);
    assertTrue(!s.contains("<strict>"));
    
    o = new GenerateXml(rootPath, extensions, true, true, null, false, null); 
    s = o.process();
    System.out.println(s);
    assertTrue(!s.contains("<strict>"));
  }

  @Test
  public void updateSanwafFileTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml(rootPath, extensions, true, true, sanwafFilePath, false, ""); 
    String s = o.process();
    System.out.println(s);
    assertTrue(s.length() != 0);
  }

  @Test
  public void updateSanwafFileAppendTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml(rootPath, extensions, true, true, sanwafFilePath, true, ""); 
    String s = o.process();
    System.out.println(s);
    assertTrue(s.length() != 0);
  }

  @Test
  public void testFileFilterFolders() {
    FileFilter filter = new FileFilter(Arrays.asList(".html"));
    String[] expectedFiles = { "badFormat.html", "badRegex.html", "file-in-root-folder.html", "folder-1", "folder-2", "index.html" };
    File directory = new File(rootPath);
    String[] actualFiles = directory.list(filter);
    Assert.assertArrayEquals(expectedFiles, actualFiles);
  }

  @Test
  public void testFileFilterFiles() {
    FileFilter filter = new FileFilter(Arrays.asList(".html"));
    String[] expectedFiles = { "folder1-sub-folder", "folder1.html" };
    File directory = new File(rootPath + System.getProperty("file.separator") + "folder-1");
    String[] actualFiles = directory.list(filter);
    Assert.assertArrayEquals(expectedFiles, actualFiles);
  }

}