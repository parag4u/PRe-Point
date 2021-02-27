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
 * Created on Apr 29, 2004
 *
 * Documentum Developer Program 2004
 */
package com.documentum.devprog.eclipse.common;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.devprog.eclipse.model.DocbaseInfo;
import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfLogger;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfLoginInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class maintains the plugin sessions.
 *
 * @author Aashish Patil (aashish.patil@documentum.com)
 */
public final class PluginState {
	private static Map<String, DocbaseInfo> sessMgrs = new HashMap<String, DocbaseInfo>();
	private static Map<String, Object> stateMap;
	private static String currentDocbase = null;
	private static String logId = PluginState.class.getName();
	private static String dmConfigFolder = null;

	static {
		stateMap = new HashMap<String, Object>();
	}

	private PluginState() {	}

	/**
	 * Gets an existing session manager or creates a new one if one does not
	 * exist.
	 *
	 * @return Documentum session manager
	 */
	public static IDfSessionManager getSessionManager() {
		return sessMgrs.get(currentDocbase).getSessionManager();
	}

	public static Map<String, DocbaseInfo> getSessionManagers() {
		return sessMgrs;
	}

	public static Object get(String key) {
		return stateMap.get(key);
	}

	public static void set(String key, Object value) {
		stateMap.put(key, value);
	}


	/**
	 * @param docbase documentum base name
	 * @return true if a sessionManager for the docbase exists.
	 */
	public static boolean setDocbase(String docbase) {
		boolean ret = false;
		if (sessMgrs.containsKey(docbase)) {
			currentDocbase = docbase;
			ret = true;
		} else {
			currentDocbase = null;
		}
		return ret;
	}

	public static String getDocbase() {
		return currentDocbase;
	}

	/**
	 * List of documentum base names.
	 * @return List of base names
	 */
	public static List<String> getDocbases() {
		return new ArrayList<String>(sessMgrs.keySet());
	}

	/**
	 * Adds a new identity to the session manager. If an identity already
	 * exists, it is cleared and the new identity added.
	 *
	 * @param username documentum username
	 * @param password documentum password
	 * @param domain windows domain (for windows servers only)
	 * @param docbase documentum base name
	 * @param authenticate Flag to indicate whether user should be immediately
	 *                     authenticated. false indicates that authentication should be
	 *                     delayed till a session is actually requested.
	 */
	static boolean addIdentity(String username, String password, String domain, String docbase, boolean authenticate) {
		try {
			IDfLoginInfo li = new DfLoginInfo();
			li.setUser(username);
			li.setPassword(password);
			li.setDomain(domain);

			IDfClient localClient = DfClient.getLocalClient();
			String docbroker = localClient.getClientConfig().getString("primary_host");
			IDfSessionManager sessMgr;
			if (setDocbase(docbase)) {
				sessMgr = getSessionManager();
				if (hasIdentity(docbase)) {
					sessMgr.clearIdentity(docbase);
				}
//				sessMgr.setIdentity(docbase, li);
//				if (authenticate) {
//					sessMgr.authenticate(docbase);
//				}
			} else {
				// SessionManage not available (yet).
				sessMgr = localClient.newSessionManager();
				sessMgr.clearIdentity(docbase);
			}
			sessMgr.setIdentity(docbase, li);
			if (authenticate) {
				sessMgr.authenticate(docbase);
			}

			sessMgrs.put(docbase, new DocbaseInfo(sessMgr, localClient, docbase, docbroker, username, password, true));
			setDocbase(docbase);
			return true;
		} catch (DfException dfe) {
			DfLogger.error(logId, "Error while adding identity", null, dfe);
			return false;
		}
	}

	/**
	 * Adds a new external identity to the session manager. If an identity already
	 * exists, it is cleared and the new identity added.
	 *
	 * @param username documentum username
	 * @param password documentum password
	 * @param domain windows domain (for windows servers only)
	 * @param docbase documentum base name
	 * @param docbroker documentum broker
	 * @param authenticate Flag to indicate whether user should be immediately
	 *                     authenticated. false indicates that authentication should be
	 *                     delayed till a session is actually requested.
	 * @param savePassword keep password
	 * @return true if succesfull
	 */
	public static boolean addExternalIdentity(String username, String password, String domain, String docbase, String docbroker, boolean authenticate, boolean savePassword) {
		try {
			IDfLoginInfo li = new DfLoginInfo();
			li.setUser(username);
			li.setPassword(password);
			li.setDomain(domain);

			IDfClient externalClient;
			externalClient = DfClient.getLocalClientEx();
			IDfTypedObject config = externalClient.getClientConfig();
			config.setString("primary_host", docbroker);
			config.setInt("primary_port", 1489);
			IDfSessionManager sessMgr = externalClient.newSessionManager();
			sessMgr.clearIdentity(docbase);
			sessMgr.setIdentity(docbase, li);

			if (authenticate) {
				sessMgr.authenticate(docbase);
			}

			sessMgrs.put(docbase, new DocbaseInfo(sessMgr, externalClient, docbase, docbroker, username, password, savePassword));
			setDocbase(docbase);
			return true;
		} catch (DfException dfe) {
			DfLogger.error(logId, "Error while adding external identity", null, dfe);
			return false;
		}
	}

	/**
	 * Checks if a identity has been associated for the specified docbase.
	 *
	 * @param docbase documentum base name
	 * @return true if docbase identity is known for session manager
	 */
	public static boolean hasIdentity(String docbase)  {
		DocbaseInfo docbaseInfo = sessMgrs.get(docbase);
		if (docbaseInfo == null) {
			return false;
		}
		return docbaseInfo.getSessionManager().hasIdentity(docbase);
	}

	/**
	 * Releases the session.
	 *
	 * @param sess documentum session
	 */
	public static void releaseSession(IDfSession sess) {
		if (sess != null) {
			try {
				sessMgrs.get(sess.getDocbaseName()).getSessionManager().release(sess);
			} catch (DfException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Gets a session based on object id. It first gets the docbase name based
	 * on id and then gets the session based on docbase name. This is a
	 * convenience method.
	 *
	 * @param id id
	 * @return documentum session
	 */
	public static IDfSession getSessionById(String id) {
		try {
			IDfId objId = new DfId(id);
			for (DocbaseInfo docbaseInfo : sessMgrs.values()) {
				String docbase = docbaseInfo.getClient().getDocbaseNameFromId(objId);
				if (docbase != null) {
					//redundant, because setSession() calls setDocbase()
					setDocbase(docbaseInfo.getDocbaseName());
					return getSession(docbaseInfo.getDocbaseName());
				}
			}
			throw new DfException("No matching session");
		} catch (DfException dfe) {
			DfLogger.error(logId, "Error getting session by id", null, dfe);
			return null;
		}
	}

	/**
	 * Gets a session based on object id. It first gets the docbase name based
	 * on id and then gets the session based on docbase name. This is a
	 * convenience method.
	 *
	 * @param id documentum object id
	 * @return documentum session
	 */
	public static IDfSession getSessionById(IDfId id) {
		return getSessionById(id.toString());
	}

	/**
	 * Gets a session based on docbase name.
	 *
	 * @param docbase documentum base name
	 * @return documentum session
	 */
	public static IDfSession getSession(String docbase) {
		try {
			setDocbase(docbase);
			return getSessionManager().getSession(docbase);
		} catch (DfException dfe) {
			DfLogger.debug(logId, "Error getting session", null, dfe);
			return null;
		}
	}

	/**
	 * Gets the session based on current docbase. This session must be released
	 * using releaseSession(...) method call.
	 *
	 * @return documentum session
	 */
	public static IDfSession getSession() {
		try {
			return getSessionManager().getSession(getDocbase());
		} catch (DfException dfe) {
			return null;
		}
	}

	/**
	 * Creates new DfClientX.
	 * @return new DfClientX
	 */
	public static IDfClientX getClientX() {
		return new DfClientX();
	}

	/**
	 * Creates local documentum client object.
	 * @return local documentum client object.
	 * @throws DfException Common documentum exception
	 */
	public static IDfClient getLocalClient() throws DfException {
		return getClientX().getLocalClient();
	}

	/**
	 * 	NOTE this method is platform specific to windows.
	 *
 	 */
	private static String guessConfigLocation() {
		return DFCConfigurationHelper.guessConfigLocation();
	}

	/**
	 * NOTE This is a System dependent method and currently only works on Windows.
	 *
	 * @return config location
	 */
	public static String getDmConfigFolder() {
		if (dmConfigFolder == null) {
			dmConfigFolder = guessConfigLocation();
		}
		return dmConfigFolder;
	}

	/**
	 * Get the location of the dfc.properties file. Note that this is a best guess
	 *
	 * @return location of dfc.properties file
	 */
	public static String getDFCPropertiesFile() {
		String cfldr = PluginState.getDmConfigFolder();
		if (cfldr != null) {
			if (!cfldr.endsWith(File.separator)) {
				cfldr += File.separator;
			}
			cfldr += "dfc.properties";
			return cfldr;
		} else {
			return null;
		}
	}

	/**
	 * Get the location of the log4j.properties file. Note that this is a best
	 * guess
	 *
	 * @return location of the log4j.properties file
	 */
	public static String getLog4jPropertiesFile() {
		String cfldr = PluginState.getDmConfigFolder();
		if (cfldr != null) {
			if (!cfldr.endsWith(File.separator)) {
				cfldr += File.separator;
			}
			cfldr += "log4j.properties";
			return cfldr;
		} else {
			return null;
		}
	}
}
