package com.amoebaman.gamemaster.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class FileUtils {

	public static void save(Serializable object, File file) throws IOException{
		if(!file.exists())
			file.createNewFile();
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.writeObject(object);
		oos.flush();
		oos.close();
	}
	
	public static Object load(File file) throws IOException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		Object object = ois.readObject();
		ois.close();
		return object;
	}
	
}
