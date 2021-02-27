/*
 * Created on Sep 27, 2006
 *
 * EMC Documentum Developer Program 2006
 */
package com.documentum.devprog.eclipse.common;

import com.documentum.devprog.eclipse.DevprogPlugin;
import com.documentum.devprog.eclipse.rcpapp.InstallerHelp;
import com.documentum.devprog.eclipse.rcpapp.RepointInstallerDialog;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Aashish Patil(patil_aashish@emc.com)
 */
public final class DFCConfigurationHelper {
	private DFCConfigurationHelper() {
	}

	// Windows specific filesystem roots
	private static final ArrayList<String> FS_ROOTS = new ArrayList<String>(
			Arrays.asList("c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));

	/**
	 *
	 */
	public static void configureDfcPropertiesFile() {
		IPreferenceStore preferences = DevprogPlugin.getDefault().getPreferenceStore();
		String cFldr = preferences.getString(PreferenceConstants.P_CONFIG);
		if (StringUtils.isEmpty(cFldr)) {
			cFldr = DFCConfigurationHelper.guessConfigLocation();
		}
		if (StringUtils.isNotEmpty(cFldr)) {
			File configFolder = new File(cFldr);
			if (configFolder.exists() && configFolder.isDirectory()) {
				preferences.setValue(PreferenceConstants.P_CONFIG, cFldr);
				//TODO find all dfc.properties and dfc.properties.* files - create class dfcPropertyFile and make a list of them
			} else {
				preferences.setValue(PreferenceConstants.P_CONFIG, "");
			}
		} else {
			preferences.setValue(PreferenceConstants.P_CONFIG, "");
		}
	}

	/**
	 * Prompts the user to get the location of the DFC folder and the
	 * configuration folder and then configures Repoint for using DFC.
	 *
	 * @return -1 to perform a restart. All other values do not mean anything
	 * for now.
	 */
	public static int configureDfcJars(boolean overwrite) {
		try {
			//new File("") returns current directory
			File home = new File("");
			File libFldr = InstallerHelp.getDfcPluginLibFolder(home);
			if (libFldr != home) {
				File dfcJar = new File(libFldr.getAbsolutePath() + File.separator + "dfc.jar");
				if (!dfcJar.exists() || overwrite) {
					RepointInstallerDialog rd = new RepointInstallerDialog(Display.getDefault().getActiveShell());
					int status = rd.open();
					if (status == RepointInstallerDialog.OK) {
						return -1;
					} else {
						return 0;
					}

				}
			} else {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Cannot locate DFC Plugin. ");
			}

			/*
			 * System.out.println("lib fldr: " + libFldr.getAbsolutePath());
			 *
			 * File confFldr = InstallerHelp.getDfcPluginConfFolder(home);
			 * System.out.println("conf fldr: " + confFldr.getAbsolutePath());
			 *
			 * InstallerHelp.copyFolderContents(new File(dfcFldr), libFldr,
			 * null);
			 *
			 * File confJar = new File(confFldr.getAbsolutePath() +
			 * File.separator + "config.jar");
			 * InstallerHelp.createPropertiesJar(new File(cFldr), confJar);
			 */
		} catch (Exception ex) {
			ex.printStackTrace();
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error initializing DFC", ex.getMessage());
		}
		return 0;
	}

	/**
	 * Guess location of DFC jars.
	 * NOTE This method is platform specific to Windows
	 */
	public static String guessDFCLocation() {
		return checkFolderExists("Program Files\\Documentum\\Shared\\");
	}

	/**
	 * Guess location of dfc.properties & log4j.properties.
	 * NOTE this method is platform specific to Windows.
	 *
	 * @return config path
	 */
	//
	public static String guessConfigLocation() {
		return checkFolderExists("Documentum\\Config\\");
	}

	private static String checkFolderExists(String path) {
		if (SWT.getPlatform().equalsIgnoreCase("win32")) {
			return null;
		}
		for (String cp : FS_ROOTS) {
			File fl = new File(cp + ":\\" + path);
			if (fl.exists() && fl.isDirectory()) {
				return fl.getAbsolutePath();
			}
		}
		return null;
	}

}
