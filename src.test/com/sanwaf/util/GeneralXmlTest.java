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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GeneralXmlTest {
  static final String TEST_BUILD_PATH = "test-build-path";
  static String rootFolder = new File(GenerateXml.class.getClassLoader().getResource("").getFile() + TEST_BUILD_PATH).toString();
  static String basePath = new File(GenerateXml.class.getClassLoader().getResource("").getFile()).toString();
  
  @BeforeClass
  public static void setUpClass() {
  }

  @Test
  public void enpointTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String s = GenerateXml.doit(Arrays.asList(rootFolder), Arrays.asList(basePath), Arrays.asList(".html"), true, false);
    System.out.println(s);
  }

  @Test
  public void nonEnpointTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String s = GenerateXml.doit(Arrays.asList(rootFolder), Arrays.asList(basePath, "/path2"), Arrays.asList(".html"), false, false);
    System.out.println(s);
  }

  @Test
  public void nonEnpointNoMatchBaseUrlTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String s = GenerateXml.doit(Arrays.asList(rootFolder), Arrays.asList("C:\\b\\_myProjects\\git\\Sanwaf-ui-2-server\\invalid_base_url"), Arrays.asList(".html"), false, false);
    System.out.println(s);
  }

  @Test
  public void nonEnpointNoMatchBaseUrl2Test() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String s = GenerateXml.doit(Arrays.asList(rootFolder), Arrays.asList(""), Arrays.asList(".html"), false, false);
    System.out.println(s);
  }

  @Test
  public void enpointNoBaseUrlTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String s = GenerateXml.doit(Arrays.asList(rootFolder), Arrays.asList(), Arrays.asList(".html"), true, false);
    System.out.println(s);
  }

  @Test
  public void invalidfolderTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String s = GenerateXml.doit(Arrays.asList("C:\\b\\_myProjects\\git\\Sanwaf-ui-2-server\\src.test\\resources\\files_invalid_dir"), Arrays.asList(), Arrays.asList(".html"), true, false);
    System.out.println(s);
    assertTrue(s.length() == 0);
  }

  @Test
  public void contructorTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    GenerateXml o = new GenerateXml();
    assertTrue(o != null);
  }

  @Test
  public void includeNonAnnotatedTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
    String s = GenerateXml.doit(Arrays.asList(rootFolder), Arrays.asList(), Arrays.asList(".html"), true, true);
    System.out.println(s);
    assertTrue(s.length() != 0);
  }

  @Test
  public void testFileFilterFolders() {
    FileFilter filter = new FileFilter(Arrays.asList(".html"));
    String[] expectedFiles = { "badFormat.html", "badRegex.html", "file-in-root-folder.html", "folder-1", "folder-2" };
    File directory = new File(rootFolder);
    String[] actualFiles = directory.list(filter);
    Assert.assertArrayEquals(expectedFiles, actualFiles);
  }

  @Test
  public void testFileFilterFiles() {
    FileFilter filter = new FileFilter(Arrays.asList(".html"));
    String[] expectedFiles = { "folder1-sub-folder", "folder1.html" };
    File directory = new File(rootFolder + System.getProperty("file.separator") + "folder-1");
    String[] actualFiles = directory.list(filter);
    Assert.assertArrayEquals(expectedFiles, actualFiles);
  }

}