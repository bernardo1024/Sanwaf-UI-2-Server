package com.sanwaf.util;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GenerateXml {
  private static final String SLASH = "/";
  private static final String LESS_THAN = "<";
  private static final String LESS = "less";
  private static final String TRUE = "true";
  private static final String FALSE = "false";
  public static final String SANWAF_FILE_PLACEHOLDER_START = "<!-- ~~~SANWAF-UI-2-SERVER-PLACEHOLDER-START~~~ -->";
  public static final String SANWAF_FILE_PLACEHOLDER_END = "<!-- ~~~SANWAF-UI-2-SERVER-PLACEHOLDER-END~~~ -->";

  private static final char END_TYPE = '}';
  private static final String REGEX = "r{";
  private static final String INLINE_REGEX = "x{";
  private static final String DATA_SW_TYPE = "data-sw-type";
  private static final Logger LOGGER = Logger.getLogger(GenerateXml.class.getName());

  private String rootPath = "";
  private boolean doEndpoints = false;
  private boolean doNonAnnotated = false;
  private String strict = "";
  private String sanwafFile = "";
  private boolean appendToFile = false;
  private String providedPlaceholderStart = null;
  private String providedPlaceholderEnd = null;
  private FileFilter fileFilter = null;
  private StringBuilder sb = new StringBuilder();

  GenerateXml(String rootPath, List<String> extensions, boolean doEndpoints, boolean doNonAnnotated, String sanwafFile, boolean appendToFile, String strict) {
    this(rootPath, extensions, doEndpoints, doNonAnnotated, sanwafFile, appendToFile, strict, "", "");
  }

  GenerateXml(String rootPath, List<String> extensions, boolean doEndpoints, boolean doNonAnnotated, String sanwafFile, boolean appendToFile, String strict, String providedPlaceholderStart,
      String providedPlaceholderEnd) {
    this.rootPath = formatPath(rootPath, false, true);
    this.doEndpoints = doEndpoints;
    this.doNonAnnotated = doNonAnnotated;
    this.sanwafFile = sanwafFile;
    this.appendToFile = appendToFile;
    this.strict = strict;
    this.providedPlaceholderStart = providedPlaceholderStart;
    this.providedPlaceholderEnd = providedPlaceholderEnd;
    fileFilter = new FileFilter(extensions);
    
    if(this.providedPlaceholderStart == null || this.providedPlaceholderStart.length() == 0) {
      this.providedPlaceholderStart = SANWAF_FILE_PLACEHOLDER_START;
    }
    if(this.providedPlaceholderEnd == null || this.providedPlaceholderEnd.length() == 0) {
      this.providedPlaceholderEnd = SANWAF_FILE_PLACEHOLDER_END;
    }
    
  }

  public String process() throws IOException {
    if (rootPath == null) {
      LOGGER.severe("ERROR: rootPath is null.");
      return "";
    }
    generateXml(rootPath);

    if (sanwafFile != null && sanwafFile.length() > 0) {
      updateSanwafFile();
    }
    return sb.toString();
  }

  public void generateXml(String folderPath) throws IOException {
    File dir = new File(folderPath);
    File[] listFiles = dir.listFiles(fileFilter);
    if (listFiles == null) {
      LOGGER.log(Level.SEVERE, "ERROR: No files found in rootFolder: {0}", dir);
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
    if (s == null) {
      return s;
    }
    s = s.replaceAll("\\\\", SLASH);
    if (addEndSlash && !s.endsWith(SLASH)) {
      s = s + SLASH;
    }
    if (removeStartSlash && s.startsWith(SLASH)) {
      s = s.substring(1, s.length());
    }
    return s;
  }

  private void parseFile(File file) throws IOException {
    Document doc = Jsoup.parse(file, "UTF-8");
    Elements forms = doc.select("form");
    if (forms.size() == 0) {
      // nothing to do, skip it
      return;
    }
    StringBuilder sbThis = new StringBuilder();

    for (Element form : forms) {
      String action = form.attr("action");

      if (action == null || action.length() == 0) {
        // TODO: pull from attribute or page

      }

      Document formdoc = Jsoup.parse(form.toString());

      if (doEndpoints) {
        sbThis.append("<endpoint>\n");
        sbThis.append("<uri>").append(action).append("</uri>\n");

        if (strict != null && strict.length() > 0 && (TRUE.equalsIgnoreCase(strict) || FALSE.equalsIgnoreCase(strict) || LESS_THAN.equals(strict) || LESS.equalsIgnoreCase(strict))) {
          sbThis.append("<strict>").append(strict.toLowerCase()).append("</strict>\n");
        }
        sbThis.append("<items>\n");
      }

      boolean addedElements = false;
      for (Element elem : formdoc.getElementsByAttribute(DATA_SW_TYPE)) {
        sbThis.append(buildItemXml(formdoc, elem, doEndpoints)).append("\n");
        addedElements = true;
      }

      if (doNonAnnotated) {
        String s = getAllOptionSelectAsConstants(formdoc, "select", "option", "value");
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
  }

  String buildItemXml(Document doc, Element elem, boolean doEndpoints) {
    StringBuilder thisSb = new StringBuilder();
    getItemStart(doc, elem, thisSb);
    thisSb.append("<type>").append(getType(elem)).append("</type>");
    thisSb.append("<max>").append(elem.attr("data-sw-max-length")).append("</max>");
    thisSb.append("<min>").append(elem.attr("data-sw-min-length")).append("</min>");
    thisSb.append("<max-value>").append(elem.attr("data-sw-max-value")).append("</max-value>");
    thisSb.append("<min-value>").append(elem.attr("data-sw-min-value")).append("</min-value>");
    thisSb.append("<mask-err>").append(elem.attr("data-sw-mask-err")).append("</mask-err>");
    thisSb.append("<msg>").append(elem.attr("data-sw-err-msg")).append("</msg>");

    String format = elem.attr("data-sw-format");
    if (format.length() == 0) {
      format = elem.attr("data-sw-fixed-format");
    }
    thisSb.append("<format>").append(format).append("</format>");

    if (doEndpoints) {
      thisSb.append("<req>").append(elem.attr("data-sw-required")).append("</req>");
      thisSb.append("<related>").append(elem.attr("data-sw-related")).append("</related>");
    }

    thisSb.append("</item>");
    return thisSb.toString();
  }

  private void getItemStart(Document doc, Element elem, StringBuilder sb) {
    sb.append("<item>");
    String name = elem.attr("name");
    if (name.length() == 0) {
      name = elem.attr("name");
    }
    sb.append("<name>").append(name).append("</name>");
    String display = elem.attr("data-sw-display");
    if((display == null || display.length() == 0) && name.length() > 0) {
      Element e = doc.select("[for=\"" + name + "\"]").first();
      if(e == null ) {
        display = name;
      }
      else {
        display = e.html();
      }
    }
    sb.append("<display>").append(display).append("</display>");
  }

  private String getType(Element elem) {
    String type = elem.attr(DATA_SW_TYPE);
    if (type.startsWith(REGEX) || type.startsWith(INLINE_REGEX)) {
      String regex = type.substring(2, type.lastIndexOf(END_TYPE));
      type = INLINE_REGEX + regex + "}";
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
    StringBuilder thisSb = new StringBuilder();
    Elements elems = doc.select(element);

    for (Element elem : elems) {
      if (elem.attr(DATA_SW_TYPE).length() == 0) {
        getItemStart(doc, elem, thisSb);
        Elements subElems = elem.select(subElement);
        boolean foundFirst = false;
        for (Element subElem : subElems) {
          if (foundFirst) {
            thisSb.append(",");
          } else {
            thisSb.append("<type>k{");
            foundFirst = true;
          }
          thisSb.append(subElem.attr(attribute));
        }
        thisSb.append("}</type></item>\n");
      }
    }
    return thisSb.toString();
  }

  private void updateSanwafFile() throws IOException {
    File file = new File(sanwafFile);
    String contents = readFile(new FileInputStream(file));

    int start = contents.indexOf(providedPlaceholderStart);
    if (start > 0) {
      int end = contents.indexOf(providedPlaceholderEnd);
      if (end > start) {
        if (appendToFile) {
          contents = contents.substring(0, end) + "\n" + sb.toString() + "\n" + contents.substring(end, contents.length());
        } else {
          contents = contents.substring(0, start + providedPlaceholderStart.length()) + "\n" + sb.toString() + "\n" + contents.substring(end, contents.length());
        }
        writeFile(contents);
      }
    }
  }

  void writeFile(String data) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(sanwafFile))) {
      writer.write(data);
    }
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

  /******************************************************************************
   * GenerateXml Main Method Used by executable JAR file to create Sanwaf-server
   * XML
   * 
   * <pre>
   * Generates XML from Sanwaf-UI attributes to be consumed by Sanwaf-Server
   * </pre>
   * 
   * @param args
   *          String[] array of strings
   * 
   ******************************************************************************/
  public static void main(String[] args) {
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger("GenerateXml");
    if (args == null || args.length < 2 || args.length > 8) {
      printUsage(logger);
      return;
    }
    String rootpath = getParm(args, "--path:");
    String exts = getParm(args, "--extensions:");
    String endpoints = getParm(args, "--endpoints:");
    String nonSanwaf = getParm(args, "--nonSanwaf:");
    String sanwafFile = getParm(args, "--file:");
    String append = getParm(args, "--append:");
    String strict = getParm(args, "--strict:");
    String out = getParm(args, "--output");
    String startPos = getParm(args, "--placeholder-start");
    String endPos = getParm(args, "--placeholder-end");

    List<String> extensions = Arrays.asList(exts.split(","));
    boolean doEndpoints = Boolean.parseBoolean(endpoints);
    boolean doNonSanwaf = Boolean.parseBoolean(nonSanwaf);
    boolean appendToFile = Boolean.parseBoolean(append);
    boolean doOutput = Boolean.parseBoolean(out);
    if (!(strict.equalsIgnoreCase(TRUE) || strict.equalsIgnoreCase(FALSE) || strict.equalsIgnoreCase(LESS) || strict.equalsIgnoreCase(LESS_THAN))) {
      strict = FALSE;
    }

    if (rootpath.length() == 0 || extensions.isEmpty()) {
      logger.log(Level.SEVERE, "Invalid call.  --rootpath and --extensions are required parameters");
      return;
    }

    try {
      GenerateXml o = new GenerateXml(rootpath, extensions, doEndpoints, doNonSanwaf, sanwafFile, appendToFile, strict, startPos, endPos);
      String s = o.process();
      if (doOutput) {
        logger.log(Level.INFO, s);
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "{0}", e);
    }
  }

  private static String getParm(String[] args, String parm) {
    String out = "";

    for (String arg : args) {
      if (arg.startsWith(parm)) {
        int pos = arg.indexOf(':');
        if (pos > 0) {
          return arg.substring(pos + 1, arg.length());
        }
        break;
      }
    }

    return out;
  }

  private static void printUsage(java.util.logging.Logger logger) {
    if (logger.isLoggable(Level.INFO)) {
      StringBuilder sb = new StringBuilder("\nSanwaf-ui-2-server Generate XML Usage");
      sb.append("\n-------------------------------------");
      sb.append("\n\nCall Format:");
      sb.append("\n\tjava -cp \"./*\" com.sanwaf.util.GenerateXml [path] [extensions] [file] [append] [output] [nonSanwaf] [endpoints] [strict] [placeholder-start] [placeholder-end]");
      sb.append("\n\nwhere (order of parameters not relevant):");

      sb.append("\n\t[path]\t\tThe root path from where to start recursively scanning for files to parse");
      sb.append("\n\t\t\tFormat:\t\t--path:<path>");
      sb.append("\n\t\t\tExample:\t--path:/path/to/files/\n");

      sb.append("\n\t[extensions]\tComma separated list of file extension to search for");
      sb.append("\n\t\t\tFormat:\t--extensions:<list,of,extensions>");
      sb.append("\n\t\t\tExample:\t--extensions:.html,.jsp\n");

      sb.append("\n\t[file]\t\tFully pathed filename to place outputs into");
      sb.append("\n\t\t\tFormat:\t\t--file:<pathed filename>");
      sb.append("\n\t\t\tExample:\t--file:/folder/sanwaf.xml\n");

      sb.append("\n\t[append]\tFlag to specify whether to append or override file");
      sb.append("\n\t\t\tFormat:\t\t--append:<true/false(default)>");
      sb.append("\n\t\t\tExample:\t--append:true\n");

      sb.append("\n\t[output]\tFlag to specify to output XML to console");
      sb.append("\n\t\t\tFormat:\t\t--output:<true/false(default)>");
      sb.append("\n\t\t\tExample:\t--output:true\n");

      sb.append("\n\t[nonSanwaf]\tFlag to specify to include non sanwaf elements as constants");
      sb.append("\n\t\t\tFormat:\t\t--nonSanwaf:<true/false(default)>");
      sb.append("\n\t\t\tExample:\t--nonSanwaf:true\n");

      sb.append("\n\t[endpoints]\tFlag to specify to use endpoints format in output");
      sb.append("\n\t\t\tFormat:\t\t--endpoints:<true/false(default)>");
      sb.append("\n\t\t\tExample:\t--endpoints:true\n");

      sb.append("\n\t[strict]\tFlag to include 'strict' attribute in output (only for doEndpoints)");
      sb.append("\n\t\t\tFormat:\t\t--strict:<true/false(default)/less>");
      sb.append("\n\t\t\tExample:\t--strict:less");

      sb.append("\n\t[placeholder-start]\tUnidque string identifier used as the start position in the sanwaf.xml file. placeholder-end indicates end position");
      sb.append("\n\t                   \tValue must be in a valid xml comment format: <!--YOUR-STRING--> as the start & end markers are not replaced/removed");
      sb.append("\n\t                   \tIf not provided, the value defaults to: " + SANWAF_FILE_PLACEHOLDER_START);
      sb.append("\n\t\t\tFormat:\t\t--placeholder-start:<unique-string-indicating-start-position>");
      sb.append("\n\t\t\tExample:\t--placeholder-start:<!--~~endpoints-start-pos~~~-->");

      sb.append("\n\t[placeholder-end]\tUnidque string identifier used as the end position in the sanwaf.xml file. string must be in a valid xml comment format: <!--YOUR-STRING-->");
      sb.append("\n\t                 \tValue must be in a valid xml comment format: <!--YOUR-STRING--> as the start & end markers are not replaced/removed");
      sb.append("\n\t                 \tIf not provided, the value defaults to: " + SANWAF_FILE_PLACEHOLDER_END);
      sb.append("\n\t\t\tFormat:\t\t--placeholder-end:<unique-string-indicating-end-position>");
      sb.append("\n\t\t\tExample:\t--placeholder-end:<!--~~endpoints-end-pos~~~-->");

      sb.append("\n\n\tNote: When \"--file\" is specified, the file contents must include the following markers to place to generated XML:");
      sb.append("\n\t\tStart Marker: " + SANWAF_FILE_PLACEHOLDER_START);
      sb.append("\n\t\tEnd Marker:   " + SANWAF_FILE_PLACEHOLDER_END);
      sb.append("\n\n\tAs the Sanwaf.xml file contains many sections, this controls where the output is placed");

      logger.log(Level.INFO, sb.toString());
    }
  }

}
