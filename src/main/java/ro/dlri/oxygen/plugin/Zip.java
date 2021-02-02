package ro.dlri.oxygen.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class Zip {

	public static void unZipAll(File source, File destination) throws ZipException, IOException {
		int BUFFER = 2048;

		ZipFile zip = new ZipFile(source);

		destination.getParentFile().mkdirs();
		Enumeration<?> zipFileEntries = zip.entries();

		// Process each entry
		while (zipFileEntries.hasMoreElements()) {
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			File destFile = new File(destination, currentEntry);
			// destFile = new File(newPath, destFile.getName());
			File destinationParent = destFile.getParentFile();

			// create the parent directory structure if needed
			destinationParent.mkdirs();

			if (!entry.isDirectory()) {
				BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
				int currentByte;
				// establish buffer for writing file
				byte data[] = new byte[BUFFER];

				// write the current file to disk
				FileOutputStream fos = new FileOutputStream(destFile);
				BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

				// read and write until last byte is encountered
				while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, currentByte);
				}
				dest.close();
				fos.close();
				is.close();
			} else {
				// Create directory
				destFile.mkdirs();
			}

			if (currentEntry.endsWith(".zip")) {
				// found a zip file, try to open
				unZipAll(destFile, destinationParent);
				if (!destFile.delete()) {
					System.out.println("Could not delete zip");
				}
			}
		}
		zip.close();
	}

}
