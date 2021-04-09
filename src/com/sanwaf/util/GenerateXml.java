package com.sanwaf.util;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GenerateXml {
  private static final String SANWAF_FILE_PLACEHOLDER_START = "<!-- ~~~SANWAF-UI-2-SERVER-PLACEHOLDER-START~~~ -->";
  private static final String SANWAF_FILE_PLACEHOLDER_END = "<!-- ~~~SANWAF-UI-2-SERVER-PLACEHOLDER-END~~~ -->";
  private static final String DATA_SW_TYPE = "data-sw-type";
  private static final Logger LOGGER = Logger.getLogger(GenerateXml.class.getName());
  
  private String rootPath = "";
  private List<String> subPathsToCut = null;
  private boolean doEndpoints = false;
  private boolean doNonAnnotated = false;
  private String strict = "";
  private String sanwafFile = "";
  private FileFilter fileFilter = null;
  private StringBuilder sb = new StringBuilder();


  GenerateXml(String rootPath, 
      List<String> subPathsToCut, 
      List<String> extensions, 
      boolean doEndpoints, 
      boolean doNonAnnotated, 
      String sanwafFile, 
      String strict
      ) {
    this.rootPath = formatPath(rootPath, false, true);
    this.subPathsToCut = subPathsToCut;
    this.doEndpoints = doEndpoints;
    this.doNonAnnotated = doNonAnnotated;
    this.sanwafFile = sanwafFile;
    this.strict = strict;
    fileFilter = new FileFilter(extensions);
  }

  public String process() throws IOException {
    if(rootPath == null) {
      LOGGER.severe("ERROR: rootPath is null.");
      return "";
    }
    generateXml(rootPath);

    if (doEndpoints && sb.length() > 0) {
      sb = new StringBuilder( "<endpoints>\n" + sb.toString() + "</endpoints>\n");
    }
    if(sanwafFile != null) {
      updateSanwafFile();
    }
    return sb.toString();
  }
  
  public void generateXml(String folderPath) throws IOException {
    File dir = new File(folderPath);
    File[] listFiles = dir.listFiles(fileFilter);
    if(listFiles == null) {
      LOGGER.severe("ERROR: No files found in rootFolder: " + dir);
      return;
    }

    for (File file : listFiles) {
      if (file.isDirectory()) {
        generateXml(formatPath(file.toString(), true, true));
      } else {
        parseFile(file);
      }
    }
  }

  private String formatPath(String s, boolean removeStartSlash, boolean addEndSlash) {
    if(s == null) {
      return s;
    }
    s = s.replaceAll("\\\\", "/");
    if (addEndSlash && !s.endsWith("/")) {
      s = s + "/";
    }
    if (removeStartSlash && s.startsWith("/")) {
      s = s.substring(1, s.length());
    }
    return s;
  }

  private void parseFile(File file) throws IOException {
    StringBuilder sbThis = new StringBuilder();
    if (doEndpoints) {
      sbThis.append("<endpoint>\n");
      String filepath = formatPath(file.toString(), false, false);
      String path = formatPath(cutPath(filepath), false, false);
      sbThis.append("<uri>").append(path).append("</uri>\n");
      if(strict != null && strict.length() > 0) {
        sbThis.append("<strict>").append(strict).append("</strict>\n");
      }
      sbThis.append("<items>\n");
    }
    boolean addedElements = false;
    Document doc = Jsoup.parse(file, "UTF-8");
    for (Element elem : doc.getElementsByAttribute(DATA_SW_TYPE)) {
      sbThis.append(buildItemXml(elem, doEndpoints)).append("\n");
      addedElements = true;
    }

    if (doNonAnnotated) {
      String s = getAllOptionSelectAsConstants(doc, "select", "option", "value");
      if (s.length() > 0) {
        sbThis.append(s);
        addedElements = true;
      }
    }

    if (doEndpoints) {
      sbThis.append("</items>\n");
      sbThis.append("</endpoint>\n");
    }
    if (addedElements) {
      sb.append(sbThis);
    }
  }

  String cutPath(String filenameWithPath) {
    List<String> paths = subPathsToCut;
    if (subPathsToCut == null || subPathsToCut.isEmpty()) {
      paths = Arrays.asList("");
    }
    for (String subPath : paths) {
      subPath = formatPath(subPath, true, true);
      if (filenameWithPath.startsWith(rootPath + subPath)) {
        filenameWithPath = filenameWithPath.substring(rootPath.length() + subPath.length(), filenameWithPath.length());
      }
    }
    return filenameWithPath;
  }

  String buildItemXml(Element elem, boolean doEndpoints) {
    StringBuilder sb = new StringBuilder();
    getItemStart(elem, sb);
    sb.append("<type>").append(getType(elem)).append("</type>");
    sb.append("<max>").append(elem.attr("data-sw-max")).append("</max>");
    sb.append("<min>").append(elem.attr("data-sw-min")).append("</min>");
    sb.append("<max-value>").append(elem.attr("data-sw-max-value")).append("</max-value>");
    sb.append("<min-value>").append(elem.attr("data-sw-min-value")).append("</min-value>");
    sb.append("<msg>").append(elem.attr("data-sw-err-msg")).append("</msg>");

    String format = elem.attr("data-sw-format");
    if (format.length() == 0) {
      format = elem.attr("data-sw-fixed-format");
    }
    sb.append("<format>").append(format).append("</format>");

    if (doEndpoints) {
      sb.append("<req>").append(elem.attr("data-sw-req")).append("</req>");
      sb.append("<related>").append(elem.attr("data-sw-related")).append("</related>");
    }

    sb.append("</item>");
    return sb.toString();
  }

  private void getItemStart(Element elem, StringBuilder sb) {
    sb.append("<item>");

    String name = elem.attr("name");
    if (name.length() == 0) {
      name = elem.attr("id");
    }
    sb.append("<name>").append(name).append("</name>");
  }

  private String getType(Element elem) {
    String type = elem.attr(DATA_SW_TYPE);
    if (type.startsWith("r{")) {
      String regex = type.substring(2, type.lastIndexOf('}'));
      try {
        Pattern.compile(regex);
      } catch (PatternSyntaxException pse) {
        LOGGER.severe("ERROR: element: " + elem.id() + ", unable to compile regex pattern: " + regex + ", using type 's' instead.");
        return "";
      }
    }
    return type;
  }

  // getAllOptionSelectAsConstants(doc, "select", "option", "value")
  private String getAllOptionSelectAsConstants(Document doc, String element, String subElement, String attribute) {
    StringBuilder sb = new StringBuilder();
    Elements elems = doc.select(element);

    for (Element elem : elems) {
      if (elem.attr(DATA_SW_TYPE).length() == 0) {
        getItemStart(elem, sb);
        Elements subElems = elem.select(subElement);
        boolean foundFirst = false;
        for (Element subElem : subElems) {
          if (foundFirst) {
            sb.append(",");
          } else {
            sb.append("<type>k{");
            foundFirst = true;
          }
          sb.append(subElem.attr(attribute));
        }
        sb.append("}</type></item>\n");
      }
    }
    return sb.toString();
  }


  private void updateSanwafFile() throws FileNotFoundException, IOException {
    File file = new File(sanwafFile);
    String contents = readFile(new FileInputStream(file));
    
    int start = contents.indexOf(SANWAF_FILE_PLACEHOLDER_START);
    if(start > 0) {
      int end = contents.indexOf(SANWAF_FILE_PLACEHOLDER_END);
      if(end > start) {
        contents = contents.substring(0, start) + sb.toString() + contents.substring(end + SANWAF_FILE_PLACEHOLDER_END.length(), contents.length());
        writeFile(contents);
      }
    }
  }
  
  void writeFile(String data) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(sanwafFile));
    writer.write(data);
    writer.close();
  }
  static String readFile(InputStream is) throws IOException {
    StringBuilder sb = new StringBuilder();
    int read = 0;
    byte[] data = new byte[1024];
    while (true) {
      read = is.read(data);
      if (read < 0) {
        break;
      }
      sb.append(new String(data));
      data = new byte[1024];
    }
    is.close();
    return sb.toString();
  }


  /*
   * need main method that takes: 1. root path to start search 2. optional,
   * comma delim list of sub-paths (if not specified, uses 1 only) 3. optional,
   * comma delim list of extension (if not specified, uses html) 4. output file
   * 
   * for the output file, search for a begin & end marker to replace the value
   * with what is generated
   * 
   * for output, remove the path+subpath from the URI generated
   * 
   * path = /foo/bar/webapp subpath=jsp,html
   * 
   * ./foo/bar/webapp/jsp1/*.jsp ./foo/bar/webapp/html/*.html
   * 
   * 
   * path = eclipseProjects subpath=pccweb/jsp,pccadmin/jsp
   * 
   * eclipseProjects/pccweb/jsp eclipseProjects/pccadmin/jsp
   * 
   * 
   * Example:
   * 
   * java -jar genSanwafXml --path:<path> --subpathlist:<path1,path2...>
   * --extentions:<html,jsp,...> --output:<sanwaf.xml>
   * 
   * 
   * 
   * 
   * NOTES: all URI generated needs paths to be "/" not "\"
   * 
   * 
   */

}
