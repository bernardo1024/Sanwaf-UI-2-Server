package com.sanwaf.util;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
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
  private boolean html5 = false;
  private String providedPlaceholderStart = null;
  private String providedPlaceholderEnd = null;
  private FileFilter fileFilter = null;
  private StringBuilder sb = new StringBuilder();

  GenerateXml(String rootPath, List<String> extensions, boolean doEndpoints, boolean doNonAnnotated, String sanwafFile, boolean appendToFile, String strict) {
    this(rootPath, extensions, doEndpoints, doNonAnnotated, sanwafFile, appendToFile, false, strict, "", "");
  }

  GenerateXml(String rootPath, List<String> extensions, boolean doEndpoints, boolean doNonAnnotated, String sanwafFile, boolean appendToFile, boolean html5, String strict, String providedPlaceholderStart,
      String providedPlaceholderEnd) {
    this.rootPath = formatPath(rootPath, false, true);
    this.doEndpoints = doEndpoints;
    this.doNonAnnotated = doNonAnnotated;
    this.sanwafFile = sanwafFile;
    this.appendToFile = appendToFile;
    this.html5 = html5;
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
      return;
    }
    StringBuilder sbThis = new StringBuilder();
    for (Element form : forms) {
      String action = form.attr("data-sw-actions");
      if(action == null || action.length() == 0) {
        action = form.attr("action");
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
      addedElements = parseAttributes(sbThis, formdoc, DATA_SW_TYPE, addedElements);
      if(html5) {
        boolean added = parseAttributes(sbThis, formdoc, "type", addedElements);
        if(added) {
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

  private boolean parseAttributes(StringBuilder sbThis, Document formdoc, String byAttType, boolean addedElements) {
    Elements elems = formdoc.select("[" + byAttType + "]");
    for (Element elem : elems) {
      if (!byAttType.equalsIgnoreCase(DATA_SW_TYPE) && elem.attr(DATA_SW_TYPE).length() > 0) {
        continue;
      }

      Atts atts = getAtts(formdoc, elem);
      if(atts.hasSignificantAttsForXml()) {
        sbThis.append(buildItemXmlFromAtts(formdoc, atts, doEndpoints)).append("\n");
        addedElements = true;
      }
    }

    if (doNonAnnotated) {
      //TODO: include other types?
      String s = getAllOptionSelectAsConstants(formdoc, "select", "option", "value");
      if (s.length() > 0) {
        sbThis.append(s);
        addedElements = true;
      }
    }
    return addedElements;
  }

  private String getAttrValue(Element e, String sanwafAtt, String html5Att, String def) {
    String s = e.attr(sanwafAtt);
    if(s != null && s.length() > 0) {
      return s;
    }
    if(html5) {
      if(html5Att.length() > 0) {
        s = e.attr(html5Att);
        if((s == null || s.length() == 0) && html5Att.equalsIgnoreCase("required")) {
          Attributes attributes = e.attributes();
          return  "" + attributes.hasKey("required");
        }
        return s;
      }
    }
    return def;
  }
  
  private Atts getAtts(Document doc, Element elem) {
    Atts atts = new Atts();
    atts.name = elem.attr("name");
    if (atts.name.length() == 0) {
      atts.name = elem.attr("id");
    }

    atts.display = elem.attr("data-sw-display");
    if((atts.display == null || atts.display.length() == 0) && atts.name.length() > 0) {
      atts.display = atts.name;
      try {
        Element e = doc.select("[for=\"" + atts.name + "\"]").first();
        if(e != null ) {
          atts.display = e.html();
        }
      }
      catch(Exception e) { }
    }
    
    atts.type = getType(elem);
    atts.maxLength = getAttrValue(elem, "data-sw-max-length", "maxlength", "");
    atts.minLength = getAttrValue(elem, "data-sw-min-length", "minlength", "");
    atts.maxValue = getAttrValue(elem, "data-sw-max-value", "max", "");
    atts.minValue = getAttrValue(elem, "data-sw-min-value", "min", "");
    atts.errMsg = getAttrValue(elem, "data-sw-err-msg", "", ""); 
    atts.maskErr = getAttrValue(elem, "data-sw-mask-err", "", "");
    atts.required = getAttrValue(elem, "data-sw-required", "required", "");
    atts.related = getAttrValue(elem, "data-sw-related", "", "");
    if(atts.type.length() == 0) {
      if(atts.maxValue.length() > 0 || atts.minValue.length() > 0) {
        atts.type = "n";
      }
      else if(atts.maxLength.length() > 0 || atts.minLength.length() > 0 || atts.required.length() > 0) {
        atts.type = "s";
      }
    }
    
    if(html5 && atts.type.equals("o") && atts.maskErr.length() == 0) {
      atts.maskErr = "****";
    }
    return atts;
  }

  String buildItemXmlFromAtts(Document doc, Atts atts, boolean doEndpoints) {
    StringBuilder sb = new StringBuilder();
    sb.append("<item>");
    sb.append("<name>").append(atts.name).append("</name>");
    sb.append("<display>").append(atts.display).append("</display>");
    sb.append("<type>").append(atts.type).append("</type>");
    sb.append("<max>").append(atts.maxLength).append("</max>");
    sb.append("<min>").append(atts.minLength).append("</min>");
    sb.append("<max-value>").append(atts.maxValue).append("</max-value>");
    sb.append("<min-value>").append(atts.minValue).append("</min-value>");
    sb.append("<mask-err>").append(atts.maskErr).append("</mask-err>");
    sb.append("<msg>").append(atts.errMsg).append("</msg>");
    if (doEndpoints) {
      sb.append("<req>").append(atts.required).append("</req>");
      sb.append("<related>").append(atts.related).append("</related>");
    }
    sb.append("</item>");
    return sb.toString();
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
    if(type == null || type.length() == 0) {
      if(html5) {
        String attType = elem.attr("type");
        if(attType.equalsIgnoreCase("email")) {
          type = "x{[^@\\s]+@[^@\\s]+\\.[^@\\s]+}";
        } else if (attType.equalsIgnoreCase("url")) {
          type = "x{https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)}";
        } else if (attType.equalsIgnoreCase("tel")) {
          type = "x{\\(?(\\d{3})\\)?[-\\.\\s]?(\\d{3})[-\\.\\s]?(\\d{4})}";
        } else if (attType.equalsIgnoreCase("number")) {
          type = "n";
        } else if (attType.equalsIgnoreCase("password")) {
          type = "o";
        }
        if (elem.attr("pattern").length() > 0) {
          type = "x{" + elem.attr("pattern") + "}";
        }
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

  private void getItemStart(Document doc, Element elem, StringBuilder sb) {
    sb.append("<item>");
    String name = elem.attr("name");
    if (name.length() == 0) {
      name = elem.attr("name");
    }
    sb.append("<name>").append(name).append("</name>");
    String display = elem.attr("data-sw-display");
    if((display == null || display.length() == 0) && name.length() > 0) {
      display = name;
      try {
        Element e = doc.select("[for=\"" + name + "\"]").first();
        if(e != null ) {
          display = e.html();
        }
      }
      catch(Exception e) {
        //ignore
      }
    }
    sb.append("<display>").append(display).append("</display>");
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
      writer.write(data.trim());
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
      sb.append(new String(data).trim());
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
    if (args == null || args.length < 2 || args.length > 11) {
      printUsage(logger);
      return;
    }
    String rootpath = getParm(args, "--path:");
    String exts = getParm(args, "--extensions:");
    if(rootpath.length() == 0 || exts.length() == 0) {
      printUsage(logger);
      return;
    }
    String endpoints = getParm(args, "--endpoints:");
    String nonSanwaf = getParm(args, "--nonSanwaf:");
    String sanwafFile = getParm(args, "--file:");
    String html5String = getParm(args, "--html5:");
    String append = getParm(args, "--append:");
    String strict = getParm(args, "--strict:");
    String out = getParm(args, "--output");
    String startPos = getParm(args, "--placeholder-start");
    String endPos = getParm(args, "--placeholder-end");

    List<String> extensions = Arrays.asList(exts.split(","));
    boolean doEndpoints = Boolean.parseBoolean(endpoints);
    boolean doNonSanwaf = Boolean.parseBoolean(nonSanwaf);
    boolean appendToFile = Boolean.parseBoolean(append);
    boolean html5 = Boolean.parseBoolean(html5String);
    boolean doOutput = Boolean.parseBoolean(out);
    if (!(strict.equalsIgnoreCase(TRUE) || strict.equalsIgnoreCase(FALSE) || strict.equalsIgnoreCase(LESS) || strict.equalsIgnoreCase(LESS_THAN))) {
      strict = FALSE;
    }

    if (rootpath.length() == 0 || extensions.isEmpty()) {
      logger.log(Level.SEVERE, "Invalid call.  --rootpath and --extensions are required parameters");
      return;
    }

    try {
      GenerateXml o = new GenerateXml(rootpath, extensions, doEndpoints, doNonSanwaf, sanwafFile, appendToFile, html5, strict, startPos, endPos);
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
      sb.append("\njava -cp ./* com.sanwaf.util.GenerateXml [path] [extensions] [file] [html5] [append] [output] [nonSanwaf] [endpoints] [strict] [placeholder-start] [placeholder-end]");
      sb.append("\n\nWhere (order of parameters not relevant):\n");

      sb.append("\n\t[path]\t\t\tThe root path from where to start recursively scanning for files to parse (mandatory)");
      sb.append("\n\t\t\t\tFormat:\t\t--path:<path>");
      sb.append("\n\t\t\t\tExample:\t--path:/path/to/files/\n");

      sb.append("\n\t[extensions]\t\tComma separated list of file extension to search for (mandatory)");
      sb.append("\n\t\t\t\tFormat:\t\t--extensions:<list,of,extensions>");
      sb.append("\n\t\t\t\tExample:\t--extensions:.html,.jsp\n");

      sb.append("\n\t[file]\t\t\tFully pathed filename to place outputs into");
      sb.append("\n\t\t\t\tFormat:\t\t--file:<pathed filename>");
      sb.append("\n\t\t\t\tExample:\t--file:/folder/sanwaf.xml\n");

      sb.append("\n\t[append]\t\tFlag to specify whether to append or override file");
      sb.append("\n\t\t\t\tFormat:\t\t--append:<true/false(default)>");
      sb.append("\n\t\t\t\tExample:\t--append:true\n");

      sb.append("\n\t[html5]\t\t\tFlag to specify whether to process HTML5 attributes");
      sb.append("\n\t\t\t\tFormat:\t\t--html5:<true/false(default)>");
      sb.append("\n\t\t\t\tExample:\t--html5:true\n");

      sb.append("\n\t[output]\t\tFlag to specify to output XML to console");
      sb.append("\n\t\t\t\tFormat:\t\t--output:<true/false(default)>");
      sb.append("\n\t\t\t\tExample:\t--output:true\n");

      sb.append("\n\t[nonSanwaf]\t\tFlag to specify to include non sanwaf elements as constants");
      sb.append("\n\t\t\t\tFormat:\t\t--nonSanwaf:<true/false(default)>");
      sb.append("\n\t\t\t\tExample:\t--nonSanwaf:true\n");

      sb.append("\n\t[endpoints]\t\tFlag to specify to use endpoints format in output");
      sb.append("\n\t\t\t\tFormat:\t\t--endpoints:<true/false(default)>");
      sb.append("\n\t\t\t\tExample:\t--endpoints:true\n");

      sb.append("\n\t[strict]\t\tFlag to include 'strict' attribute in output (only for doEndpoints)");
      sb.append("\n\t\t\t\tFormat:\t\t--strict:<true/false(default)/less>");
      sb.append("\n\t\t\t\tExample:\t--strict:less\n");

      sb.append("\n\t[placeholder-start]\tUnique string identifier used as the start position in the sanwaf.xml file to copy generated output");
      sb.append("\n\t\t\t\tMust be in a valid xml comment format: <!--YOUR-STRING--> as the start & end markers are not replaced/removed");
      sb.append("\n\t\t\t\tIf not provided, the value defaults to: " + SANWAF_FILE_PLACEHOLDER_START);
      sb.append("\n\t\t\t\tFormat:\t\t--placeholder-start:\"<unique-string-indicating-start-position>\"");
      sb.append("\n\t\t\t\tExample:\t--placeholder-start:\"<!--START-->\"\n");

      sb.append("\n\t[placeholder-end]\tUnique string identifier used as the end position in the sanwaf.xml file to copy generated output");
      sb.append("\n\t\t\t\tMust be in a valid xml comment format: <!--YOUR-STRING--> as the start & end markers are not replaced/removed");
      sb.append("\n\t\t\t\tIf not provided, the value defaults to: " + SANWAF_FILE_PLACEHOLDER_END);
      sb.append("\n\t\t\t\tFormat:\t\t--placeholder-end:\"<unique-string-indicating-end-position>\"");
      sb.append("\n\t\t\t\tExample:\t--placeholder-end:\"<!--END-->\"");

      sb.append("\n\n\tNote: use quotes around parameters if they contain spaces.\n");

      logger.log(Level.INFO, sb.toString());
    }
  }

}

class Atts{
  String name = "";
  String display = "";
  String type = "";
  String required = "false";
  String minLength = "";
  String maxLength = "";
  String minValue = "";
  String maxValue = "";
  String maskErr = "";
  String errMsg = "";
  String related = "";
  public boolean hasSignificantAttsForXml() {
    if(minLength.length() > 0 || maxLength.length() > 0 || minValue.length() > 0 || maxValue.length() > 0 || maskErr.length() > 0 || errMsg.length() > 0 || related.length() > 0) {
      return true;
    }
    
    if(type.equals("s") && !required.equalsIgnoreCase("true")) {
        return false;
    }
    
    return true;
  }
  
  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append("\nname=").append(name);
    sb.append("\ndisplay=").append(display);
    sb.append("\ntype=").append(type);
    sb.append("\nrequired=").append(required);
    sb.append("\nminLength=").append(minLength);
    sb.append("\nmaxLength=").append(maxLength);
    sb.append("\nminValue=").append(minValue);
    sb.append("\nmaxValue=").append(maxValue);
    sb.append("\nmaskErr=").append(maskErr);
    sb.append("\nerrMsg=").append(errMsg);
    sb.append("\nrelated=").append(related);
    return sb.toString();
  }
}
