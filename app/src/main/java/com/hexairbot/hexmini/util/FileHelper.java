package com.hexairbot.hexmini.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.content.Context;
import android.os.Environment;

public class FileHelper {
	private Context context;
	private String FILESPATH;

	public FileHelper(Context context) {
		this.context = context;
		FILESPATH = this.context.getFilesDir().getPath() + "/";
	}

	public boolean delDataFile(String fileName) {
		File file = new File(FILESPATH + fileName);
		return delFile(file);
	}

	public boolean hasDataFile(String fileName) {
		File file = new File(FILESPATH + fileName);
		return file.exists() && file.isFile();
	}

	public boolean delFile(File file) {
		if (file.isDirectory())
			return false;
		return file.delete();
	}
}
