package com.amoebaman.gamemaster.objects;

import java.io.File;
import java.io.FileFilter;

public class MapFileFilter implements FileFilter{
	
	public boolean accept(File file) {
		return file.getName().endsWith(".map");
	}
	
}
