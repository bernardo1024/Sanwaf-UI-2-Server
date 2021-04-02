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
  private static final String DATA_SW_TYPE = "data-sw-type";
  private static final Logger LOGGER = Logger.getLogger(GenerateXml.class.getName());

  GenerateXml() {}

  public static String doit(List<String> folders, List<String> baseUrls, List<String> extensions, boolean doEndpoints, boolean doNonAnnotated) throws IOException {
    String s = generateServerXml(folders, baseUrls, extensions, doEndpoints, doNonAnnotated);

    if (s.length() > 0 && doEndpoints) {
      return "<endpoints>\n" + s + "</endpoints>\n";
    }
    return s;
  }

  public static String generateServerXml(List<String> folders, List<String> baseUrls, List<String> extensions, boolean doEndpoints, boolean doNonAnnotated) throws IOException {
    StringBuilder sb = new StringBuilder();
    FileFilter fileFilter = new FileFilter(extensions);

    for (String folder : folders) {
      File dir = new File(folder);
      File[] listFiles = dir.listFiles(fileFilter);
      if (listFiles != null) {
        for (File file : listFiles) {
          if (file.isDirectory()) {
            sb.append(generateServerXml(Arrays.asList(file.toString()), baseUrls, extensions, doEndpoints, doNonAnnotated));
          } else {
            handleElement(baseUrls, doEndpoints, doNonAnnotated, sb, file);
          }
        }
      }
    }
    return sb.toString();
  }

  private static void handleElement(List<String> baseUrls, boolean doEndpoints, boolean doNonAnnotated, StringBuilder sb, File file) throws IOException {
    if (doEndpoints) {
      sb.append("\t<endpoint>\n");
      sb.append("\t\t<uri>").append(refinePath(baseUrls, file.toString())).append("</uri>\n");
      sb.append("\t\t<items>\n");
    }

    Document doc = Jsoup.parse(file, "UTF-8");
    for (Element elem : doc.getElementsByAttribute(DATA_SW_TYPE)) {
      if (doEndpoints) {
        sb.append("\t\t\t");
      }
      sb.append(buildItemXml(elem, doEndpoints)).append("\n");
    }

    if (doNonAnnotated) {
      sb.append(getAllOptionSelectAsConstants(doc, "select", "option", "value"));
    }

    if (doEndpoints) {
      sb.append("\t\t</items>\n");
      sb.append("\t</endpoint>\n");
    }
  }

  static String refinePath(List<String> baseUrls, String file) {
    for (String baseurl : baseUrls) {
      if (file.startsWith(baseurl)) {
        file = file.substring(baseurl.length(), file.length());
      }
    }
    return file;
  }

  static String buildItemXml(Element elem, boolean doEndpoints) {
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

  private static void getItemStart(Element elem, StringBuilder sb) {
    sb.append("<item>");

    String name = elem.attr("name");
    if (name.length() == 0) {
      name = elem.attr("id");
    }
    sb.append("<name>").append(name).append("</name>");
  }

  private static String getType(Element elem) {
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

  //getAllOptionSelectAsConstants(doc, "select", "option", "value")
  private static String getAllOptionSelectAsConstants(Document doc, String element, String subElement, String attribute) {
    StringBuilder sb = new StringBuilder();
    Elements elems = doc.select(element);

    for (Element elem : elems) {
      if(elem.attr(DATA_SW_TYPE).length() == 0) {
        getItemStart(elem, sb);
        Elements subElems = elem.select(subElement);
        boolean foundFirst = false;
        for (Element subElem : subElems) {
          if(foundFirst) {
            sb.append(",");
          }
          else {
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

}
