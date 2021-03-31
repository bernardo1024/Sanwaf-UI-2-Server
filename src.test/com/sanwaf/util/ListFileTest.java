package com.sanwaf.util;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ListFileTest {

	@BeforeClass
	public static void setUpClass() {
	}

	@Test
  public void enpointTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
		SanwafBuildServerXml.doit(
				Arrays.asList("C:\\b\\_myProjects\\git\\Sanwaf-ui-2-server\\src.test\\resources\\files"), 
				Arrays.asList("C:\\b\\_myProjects\\git\\Sanwaf-ui-2-server\\src.test\\"), 
				Arrays.asList(".html"),
				true
				);
  }

	@Test
  public void nonEnpointTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
		SanwafBuildServerXml.doit(
				Arrays.asList("C:\\b\\_myProjects\\git\\Sanwaf-ui-2-server\\src.test\\resources\\files"), 
				Arrays.asList("C:\\b\\_myProjects\\git\\Sanwaf-ui-2-server\\src.test\\"), 
				Arrays.asList(".html"),
				false
				);
  }
	
	@Test
  public void enpointNoBaseUrlTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
		SanwafBuildServerXml.doit(
				Arrays.asList("C:\\b\\_myProjects\\git\\Sanwaf-ui-2-server\\src.test\\resources\\files"), 
				Arrays.asList(), 
				Arrays.asList(".html"),
				true
				);
  }
	
	
	
}