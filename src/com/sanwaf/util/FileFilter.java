package com.sanwaf.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public class FileFilter implements FilenameFilter {
	List<String> extensions;
	
	public FileFilter(List<String> extensions) {
		this.extensions = extensions;
	}

	@Override
	public boolean accept(File loc, String name) {
		if(loc.isDirectory()) {
			return true;
		}
		int pos = name.lastIndexOf('.');
		if (pos > 0) {
			String str = name.substring(pos);
			for(String ext : extensions) {
				if (str.equalsIgnoreCase(ext)) {
					return true;
				}
			}
		}
		return false;
	}
}