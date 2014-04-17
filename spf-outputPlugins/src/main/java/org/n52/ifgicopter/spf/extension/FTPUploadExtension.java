/**
 * ﻿Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License serviceVersion 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.ifgicopter.spf.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.n52.ifgicopter.spf.common.IExtension;
import org.n52.ifgicopter.spf.gui.ModuleGUI;

public class FTPUploadExtension implements IExtension {

    private class WatchfolderThread implements Runnable {

        private boolean running = true;

        public WatchfolderThread() {
            //
        }

        private List<File> getChangedFiles() {
            ArrayList<File> result = new ArrayList<File>();

            if ( !FTPUploadExtension.this.watchFolder.isDirectory()) {
                return result;
            }

            File[] files = FTPUploadExtension.this.watchFolder.listFiles(FTPUploadExtension.this.onlyFilesFilter);
            HashSet<File> newSet = new HashSet<File>(Arrays.asList(files));

            Map<File, Long> tmpMap = new HashMap<File, Long>();
            for (File f : newSet) {
                if (FTPUploadExtension.this.oldSet.keySet().contains(f)) {
                    if (f.lastModified() != FTPUploadExtension.this.oldSet.get(f).longValue()) {
                        result.add(f);
                    }
                }
                else {
                    result.add(f);
                }

                tmpMap.put(f, Long.valueOf(f.lastModified()));
            }

            FTPUploadExtension.this.oldSet = tmpMap;

            return result;
        }

        @Override
        public void run() {
            List<File> files;
            while (this.running) {

                files = getChangedFiles();

                if ( !files.isEmpty()) {
                    try {
                        addOrUpdateFiles(files);
                    }
                    catch (IOException e) {
                        FTPUploadExtension.logger.warn(e.getMessage());
                    }
                }

                /*
                 * sleep for seconds
                 */
                if (this.running) {
                    try {
                        Thread.sleep(FTPUploadExtension.this.watchIntervalMillis);
                    }
                    catch (InterruptedException e) {
                        FTPUploadExtension.logger.warn(e.getMessage());
                    }
                }

            }
        }

        public void setRunning(boolean r) {
            this.running = r;
        }

    }

    protected static final Log logger = LogFactory.getLog(FTPUploadExtension.class);

    public static final String DEFAULT_FTP_WATCH_FOLDER = "ftp-uploads";

    private static final String EXTENSION_NAME = "FTP Uploader";

    public static void main(String[] args) throws Exception {
        FTPUploadExtension f = new FTPUploadExtension(DEFAULT_FTP_WATCH_FOLDER);
        f.init();
    }

    /**
     * properties file for FTP connection settings, other settings should go into the constructor and be
     * changed in spf.properties
     */
    private Properties ftpConfig;

    private FTPClient ftp;

    protected Map<File, Long> oldSet = new HashMap<File, Long>();

    protected FilenameFilter onlyFilesFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
            File tmp = new File(dir.getAbsolutePath() + File.separator + name);
            if (tmp.isDirectory())
                return false;
            return true;
        }
    };

    protected File watchFolder;

    /**
     * the globally used watch folder for pending FTP uploads
     */
    private String watchFolderPath = DEFAULT_FTP_WATCH_FOLDER;

    protected int watchIntervalMillis = 2000;

    private WatchfolderThread watchThread;

    public FTPUploadExtension() {
        this(DEFAULT_FTP_WATCH_FOLDER);
    }

    public FTPUploadExtension(String path) {
        this.watchFolderPath = path;

        logger.info("NEW " + this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(EXTENSION_NAME);
        sb.append(" [watchFolderPath = ");
        sb.append(this.watchFolderPath);
        sb.append(", watchInterval = ");
        sb.append(this.watchIntervalMillis);
        if (this.watchFolder != null) {
            sb.append(", watchFolder = ");
            sb.append(this.watchFolder.getAbsolutePath());
        }
        sb.append("]");
        return sb.toString();
    }

    public void addOrUpdateFiles(List<File> files) throws IOException {
        connect();

        FileInputStream fis;
        for (File file : files) {
            fis = new FileInputStream(file);
            try {
                if ( !this.ftp.storeFile("/" + file.getName(), fis)) {
                    throw new IOException("Could not transfer file '" + file.getName() + "' to server '"
                            + this.ftp.getRemoteAddress().toString() + "'");
                }
            }
            catch (IOException e) {
                throw new IOException("Could not transfer file '" + file.getName() + "' to server '"
                        + this.ftp.getRemoteAddress().toString() + "'", e);
            }
            fis.close();
        }

        disconnect();
    }

    private void connect() throws IOException {
        this.ftp.connect(this.ftpConfig.getProperty("HOST"), Integer.parseInt(this.ftpConfig.getProperty("PORT")));

        if (this.ftp.isConnected()) {
            if ( !this.ftp.login(this.ftpConfig.getProperty("USER"), this.ftpConfig.getProperty("PASSWORD"))) {
                throw new IOException("Could not log-in to FTP-Server. Wrong user-info?");
            }

        }
        else {
            throw new IOException("Could not connect to FTP-Server.");
        }
    }

    private void disconnect() throws IOException {
        this.ftp.logout();
        this.ftp.disconnect();
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @Override
    public ModuleGUI getUserInterface() {
        return null;
    }

    @Override
    public void init() throws Exception {
        this.ftpConfig = new Properties();
        this.ftpConfig.load(getClass().getResourceAsStream("/FTPUploadExtension.properties"));

        // TODO check in spf deploy environment
        this.watchFolder = new File(this.watchFolderPath).getAbsoluteFile();

        this.ftp = new FTPClient();
        connect();
        disconnect();

        readInitialFolderContents();

        this.watchThread = new WatchfolderThread();
        new Thread(this.watchThread).start();

        logger.debug("Initialized " + this);
    }

    private void readInitialFolderContents() {
        if ( !this.watchFolder.exists()) {
            this.watchFolder.mkdir();
        }
        File[] files = this.watchFolder.listFiles(this.onlyFilesFilter);
        if (files == null)
            return;
        HashSet<File> newSet = new HashSet<File>(Arrays.asList(files));

        for (File f : newSet) {
            this.oldSet.put(f, Long.valueOf(f.lastModified()));
        }
    }

    @Override
    public void shutdown() throws Exception {
        this.ftp.disconnect();
        this.watchThread.setRunning(false);
    }
    
}
