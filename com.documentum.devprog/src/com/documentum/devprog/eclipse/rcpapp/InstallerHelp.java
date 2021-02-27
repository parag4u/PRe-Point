/* ******************************************************************************
 * Copyright (c) 2005-2006, EMC Corporation
 * All rights reserved.

 * Redistribution and use in source and binary forms,
 * with or without modification, are permitted provided that
 * the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * - Neither the name of the EMC Corporation nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *******************************************************************************/

/*
 * Created on Sep 26, 2006
 *
 * EMC Documentum Developer Program 2006
 */
package com.documentum.devprog.eclipse.rcpapp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Aashish Patil(patil_aashish@emc.com)
 */
public final class InstallerHelp {
	private InstallerHelp() {
	}

	private static void copyFile(File srcFile, File targetFolder) throws IOException {
		BufferedInputStream bufIn = new BufferedInputStream(new FileInputStream(srcFile));

		String targetFilePath = targetFolder.getAbsolutePath() + File.separator + srcFile.getName();

		BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(targetFilePath));

		byte[] buf = new byte[1024];
		int cnt;
		while ((cnt = bufIn.read(buf)) != -1) {
			bufOut.write(buf, 0, cnt);
		}

		bufOut.close();
		bufIn.close();
	}

	static void copyFolderContents(File srcFldr, File targetFldr) throws IOException {
		File[] fllist = srcFldr.listFiles();
		if (fllist != null) {
			for (File aFllist : fllist) {
				if (aFllist.isFile() && aFllist.getName().toLowerCase().endsWith(".jar")) {
					copyFile(aFllist, targetFldr);
				}
				if (aFllist.isDirectory()) {
					copyFolderContents(aFllist, targetFldr);
				}
			}
		}
	}

	@Deprecated
	public static File createPropertiesJar(File configFldr, File targetFile) throws IOException {
		BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(targetFile));
		JarOutputStream jout = new JarOutputStream(bufOut);

		File[] lst = configFldr.listFiles();
		if (lst != null) {
			for (File aLst : lst) {
				if (!aLst.isDirectory()) {
					String name = aLst.getName();
					if (name.equalsIgnoreCase("dfc.properties") || name.equalsIgnoreCase("log4j.properties") || name.equalsIgnoreCase("dbor.properties")) {
						addFileToJar(aLst, jout, "");
					}
				}
			}
		}

		jout.close();
		return targetFile;
	}

	private static void addFileToJar(File file, ZipOutputStream zipStream, String path) throws IOException {
		FileInputStream fin = new FileInputStream(file);

		int bufSize = 1024;
		byte[] ipBuf = new byte[bufSize];
		int lenRead = 0;

		String filename = path + file.getName();
		zipStream.putNextEntry(new ZipEntry(filename));

		while ((lenRead = fin.read(ipBuf)) > 0) {
			zipStream.write(ipBuf, 0, lenRead);
		}

		zipStream.closeEntry();
		fin.close();
	}

	static File getPluginXmlFile(File pluginFolder) {
		String absPath = pluginFolder.getAbsolutePath();
		if (!absPath.endsWith(File.separator)) {
			absPath += File.separator;
		}
		return new File(absPath + "plugin.xml");
	}

	//	public static File getPluginRootFolder(final File home) {
	//		StringBuilder absPath = new StringBuilder().append(home.getAbsolutePath());
	//		if (!absPath.toString().endsWith(File.separator)) {
	//			absPath.append(File.separator);
	//		}
	//		absPath.append("plugins");
	//		return new File(absPath.toString());
	//	}

	/**
	 * Search for first com.documentum.dfc* folder in pluginsFolder.
	 *
	 * @param pluginsFolder folder, where plugins are stored
	 * @return plugins folder File
	 */
	private static File getDfcPluginFolder(File pluginsFolder) {
		File[] lst = pluginsFolder.listFiles();
		if (lst != null) {
			for (File aLst : lst) {
				if (aLst.isDirectory() && aLst.getName().startsWith("com.documentum.dfc")) {
					return aLst;
				}
			}
		}
		return null;
	}

	/**
	 * return Dfc Plugin Library Folder.
	 *
	 * @param home home can be repoint home or eclipse home
	 * @return %home%/plugins/com.documentum.dfc/lib or null
	 */
	public static File getDfcPluginLibFolder(final File home) {
		File dfcFolder = getDfcPluginFolder(new File(normalizeHome(home) + "plugins"));
		if (dfcFolder != null) {
			return new File(dfcFolder.getAbsolutePath() + File.separator + "lib");
		}
		return null;
	}

	/**
	 * @param home home can be repoint home or eclipse home
	 * @return %home%/plugins/com.documentum.dfc/config or null
	 */
	static File getDfcPluginConfFolder(final File home) {
		File dfcFolder = getDfcPluginFolder(new File(normalizeHome(home) + "plugins"));
		if (dfcFolder != null) {
			return new File(dfcFolder.getAbsolutePath() + File.separator + "config");
		}
		return null;
	}

	/**
	 * @param home path
	 * @return String, never null
	 */
	static String normalizeHome(final File home) {
		String absPath;
		if (home == null) {
			absPath = File.separator;
		} else {
			absPath = home.getAbsolutePath();
			if (!absPath.endsWith(File.separator)) {
				absPath += File.separator;
			}
		}
		return absPath;
	}
}
