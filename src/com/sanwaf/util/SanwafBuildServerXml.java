package com.sanwaf.util;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SanwafBuildServerXml {
	public static void doit(List<String> folders, List<String> baseUrls, List<String> extensions, boolean doEndpoints) throws IOException {
		StringBuilder sb = new StringBuilder();
		if(doEndpoints) {
			sb.append("<endpoints>\n");
		}
		sb.append(generateServerXml(folders, baseUrls, extensions, doEndpoints));
		
		if(doEndpoints) {
			sb.append("</endpoints>\n");
		}
		
		System.out.println(sb.toString());
	}
	
	public static String generateServerXml(List<String> folders, List<String> baseUrls, List<String> extensions, boolean doEndpoints) throws IOException {
		StringBuilder sb = new StringBuilder();
		FileFilter fileFilter = new FileFilter(extensions);

		for(String folder: folders) {
			File dir = new File(folder);
			for(File file : dir.listFiles(fileFilter)) {
				if(file.isDirectory()) {
					sb.append(generateServerXml(Arrays.asList(file.toString()), baseUrls, extensions, doEndpoints));
				}
				else {
					if(doEndpoints) {
						sb.append("\t<endpoint>\n");
						sb.append("\t\t<uri>").append(refinePath(baseUrls, file.toString())).append("</uri>\n");
						sb.append("\t\t<items>\n");
					}

					Document doc = Jsoup.parse(file, "UTF-8");
					for(Element elem : doc.getElementsByAttribute("data-sw-type")) {
						if(doEndpoints) {
							sb.append("\t\t\t");
						}
						sb.append(buildItemXml(elem, doEndpoints)).append("\n");
					}
					
					sb.append(getAllOptionSelectAsConstants(doc, "select", "option", "value"));

					
					if(doEndpoints) {
						sb.append("\t\t</items>\n");
						sb.append("\t</endpoint>\n");
					}
				}
			}
		}
		return sb.toString();
	}
	
	static String refinePath(List<String> baseUrls, String file) {
		for(String baseurl : baseUrls) {
			if(file.startsWith(baseurl)) {
				return file.substring(baseurl.length(), file.length());
			}
		}
		return file;
	}
	
	static String buildItemXml(Element elem, boolean doEndpoints) {
		StringBuilder sb = new StringBuilder();
		sb.append("<item>");
		
		String name = elem.attr("name");
		if(name == null || name.length() == 0) { name = elem.attr("id"); }
		sb.append("<name>").append(name).append("</name>");
		sb.append("<type>").append(getType(elem)).append("</type>");

		sb.append("<max>").append(elem.attr("data-sw-max")).append("</max>");
		sb.append("<min>").append(elem.attr("data-sw-min")).append("</min>");
		sb.append("<max-value>").append(elem.attr("data-sw-max-value")).append("</max-value>");
		sb.append("<min-value>").append(elem.attr("data-sw-min-value")).append("</min-value>");
		sb.append("<msg>").append(elem.attr("data-sw-err-msg")).append("</msg>");

		String format = elem.attr("data-sw-format");
		if(format == null || format.length() == 0) {
			format = elem.attr("data-sw-fixed-format");
		}
		sb.append("<format>").append(format).append("</format>");

		if(doEndpoints) {
			sb.append("<req>").append(elem.attr("data-sw-req")).append("</req>");
			sb.append("<related>").append(elem.attr("data-sw-related")).append("</related>");
		}

		sb.append("</item>");
		return sb.toString();
	}
	
	private static String getType(Element elem) {
		String type = elem.attr("data-sw-type");
		if(type.startsWith("r{")){
			String regex = type.substring(2, type.lastIndexOf("}"));
			try {
				Pattern.compile(regex);
			}catch (PatternSyntaxException  pse) {
				System.out.println("ERROR: element: " + elem.id() + ", unable to compile regex pattern: " + regex + ", using type 's' instead.");
				return "s";
			}
		}
		return type;
	}
	
	

	private static String getAllOptionSelectAsConstants(Document doc, String element, String subElement, String attribute) {
		Elements elems = doc.select(element);
		
		for(Element elem : elems ) {
			System.out.println("id: " + elem.attr("id"));
			
			if(subElement != null) {
				Elements subElems = elem.select(subElement);
				for(Element subElem: subElems) {
					System.out.println("\tvalue: " + subElem.attr(attribute));
				}
			}
			else {
				System.out.println("\tvalue: " + elem.attr(attribute));
			}
		}
		
		return "";
	}
	
}
