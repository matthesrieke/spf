/**
 * ﻿Copyright (C) 2009
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
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

package org.n52.ifgicopter.spf.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.input.IInputPlugin;
import org.n52.ifgicopter.spf.output.IOutputPlugin;

/**
 * The {@link PluginRegistry} searches all jars in the plugin folder for {@link IInputPlugin} and
 * {@link IOutputPlugin} implementations.
 * 
 * @author matthes rieke
 * 
 */
public class PluginRegistry {

    private final static Log log = LogFactory.getLog(PluginRegistry.class);

    protected String pluginFolder = "./plugins/";
    protected String binFolder = "./bin/";
    protected String extension = ".jar";

    protected FilenameFilter extensionFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(PluginRegistry.this.extension);
        }
    };

    private List<String> inputPlugins = new ArrayList<String>();
    private List<String> outputPlugins = new ArrayList<String>();

    /**
     * Searches all jars in the plugin folder. Stores classes implementing {@link IInputPlugin} as well as
     * {@link IOutputPlugin}
     * 
     * @throws IOException
     */
    public void lookupPlugins() throws IOException {
        this.inputPlugins.clear();
        this.outputPlugins.clear();

        File[] jars1 = new File(this.pluginFolder).listFiles(this.extensionFilter);
        File[] jars2 = new File(this.binFolder).listFiles(this.extensionFilter);

        ArrayList<File> jars = new ArrayList<File>();

        if (jars1 != null && jars1.length > 0) {
            jars.addAll(Arrays.asList(jars1));
        }
        if (jars2 != null && jars2.length > 0) {
            jars.addAll(Arrays.asList(jars2));
        }

        if (jars.size() == 0) {
            log.info("No plugin libraries found");
        }
        else {
            for (File extensionJar : jars) {
                List<String> classes = listClasses(extensionJar);

                for (String c : classes) {
                    Class< ? > clazz;
                    try {
                        clazz = Class.forName(c, false, this.getClass().getClassLoader());
                        if (IInputPlugin.class.isAssignableFrom(clazz) && !IInputPlugin.class.getName().equals(c)) {
                            this.inputPlugins.add(clazz.getName());
                        }
                        else if (IOutputPlugin.class.isAssignableFrom(clazz)
                                && !IOutputPlugin.class.getName().equals(c)) {
                            this.outputPlugins.add(clazz.getName());
                        }
                    }
                    catch (NoClassDefFoundError e) {
                        // log.debug("No class found.");
                    }
                    catch (ClassNotFoundException e) {
                        // log.debug("No class found.");
                    }

                }

            }
        }

    }

    /**
     * @param jar
     *        the jar
     * @return list of fqcn
     */
    private List<String> listClasses(File jar) throws IOException {
        ArrayList<String> result = new ArrayList<String>();

        JarInputStream jis = new JarInputStream(new FileInputStream(jar));
        JarEntry je;

        while (jis.available() > 0) {
            je = jis.getNextJarEntry();

            if (je != null) {
                if (je.getName().endsWith(".class")) {
                    result.add(je.getName().replaceAll("/", "\\.").substring(0, je.getName().indexOf(".class")));
                }
            }
        }

        return result;
    }

    /**
     * @return the registered inputPlugins
     */
    public List<String> getInputPlugins() {
        return this.inputPlugins;
    }

    /**
     * @return the registered outputPlugins
     */
    public List<String> getOutputPlugins() {
        return this.outputPlugins;
    }

}