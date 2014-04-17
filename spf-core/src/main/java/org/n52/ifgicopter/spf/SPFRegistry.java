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

package org.n52.ifgicopter.spf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.n52.ifgicopter.spf.common.IExtension;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.common.IOutputMessageListener;
import org.n52.ifgicopter.spf.common.IStatusChangeListener;
import org.n52.ifgicopter.spf.common.SPFProperties;
import org.n52.ifgicopter.spf.common.SPFThreadPool;
import org.n52.ifgicopter.spf.data.AbstractDataProcessor;
import org.n52.ifgicopter.spf.data.AbstractInterpolator;
import org.n52.ifgicopter.spf.gui.SPFMainFrame;
import org.n52.ifgicopter.spf.gui.SplashDialog;
import org.n52.ifgicopter.spf.input.IInputPlugin;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.xml.Plugin;

/**
 * Global singleton class which provides access to needed resources (see {@link IModule}). Also is the main
 * entry point to the program.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class SPFRegistry {

    private static SPFRegistry _instance;

    private static final String PROP_URL = "config/spf.properties";
    private static final Log log = LogFactory.getLog(SPFRegistry.class);
    private static final Log lifecycleLog = LogFactory.getLog("org.n52.ifgicopter.spf.lifecycle");

	private static final String PROPERTIES_COMMENT = "SPFramework configuration:"+ System.getProperty("line.separator")+ System.getProperty("line.separator") +
		"IInputPlugins and IOutputPlugins are stored in a semicolon separated list as full qualified class names, e.g.:"+ System.getProperty("line.separator") +
		"org.n52.ifgicopter.spf.input.DummyInputPlugin; org.n52.ifgicopter.spf.xmlrpc.XMLRPCInputPlugin"+ System.getProperty("line.separator")+ System.getProperty("line.separator") +
		"Arguments can be passed using pseudo-constructor syntax, e.g.:"+ System.getProperty("line.separator") +
		"org.n52.ifgicopter.spf.output.FileWriterPlugin(|) - indicating a '|' as the delimiter for CSV output."+ System.getProperty("line.separator")+ System.getProperty("line.separator") +
		"Implementations must extend org.n52.ifgicopter.spf.input.IInputPlugin or org.n52.ifgicopter.spf.output.IOutputPlugin";

	/**
     * configuration key for the {@link IOutputPlugin} instances
     */
    public static final String OUTPUT_PLUGINS_PROP = "IOutputPlugins";
    
    /**
     * configuration key for the {@link IInputPlugin} instances
     */
    public static final String INPUT_PLUGINS_PROP = "IInputPlugins";
	
    /**
     * configuration key for the {@link AbstractDataProcessor} instances
     */
    public static final String ABSTRACT_DATA_PROCESSORS = "AbstractDataProcessors";

    /**
     * used to define the number of cores on this machine. multiple cores lead to multiple working threads.
     */
    public static final String PROCESSOR_COUNT_PROP = "ProcessorCount";
    
    /**
     * config key for the {@link IExtension} instances
     */
    public static final String EXTENSIONS_PROP = "IExtensions";

    /**
     * property defining the plugin class which should be activated at startup
     */
    public static final String DEFAULT_ACTIVE_PLUGIN = "DEFAULT_ACTIVE_PLUGIN";

    /**
     * property key for xml validation
     */
    public static final String VALIDATE_XML_PROP = "ValidateXML";

    /**
     * the property key for the {@link AbstractInterpolator}
     */
    public static final String ABSTRACT_INTERPOLATOR = "AbstractInterpolator";

	/**
	 * the configurations list separator
	 */
	public static final String LIST_SEPARATOR = ";";

	/**
	 * the configurations parameters separator fro pseudo-constructors
	 */
	public static final String PARAMETER_SEPARATOR = ",";
	
	/**
	 * config key for enabling full screen
	 */
	public static final String MAXIMIZED = "START_MAXIMIZED";

	
	/**
	 * config key for enabling the overview map
	 */
	public static final String OVERVIEW_MAP_ENABLED = "OVERVIEW_MAP_ENABLED";



    private Properties properties;
    private List<IModule> modules;
    private boolean shuttingDown = false;
    private SPFThreadPool threadPool;
    private SPFEngine engine;
    private SPFMainFrame mainFrame;
    private List<IStatusChangeListener> statusChangeListeners;
    private List<Class< ? >> dataProcessors;
    private List<IOutputMessageListener> outputMessageListeners;
	private List<IExtension> extensions;


    private SPFRegistry() {
        // private constructor for singleton pattern
    	lifecycleLog.info("Initialising Sensor Platform Framework...");
    }

    /**
     * @return the singelton instance of {@link SPFRegistry}.
     */
    public static synchronized SPFRegistry getInstance() {
        if (_instance == null) {
            init();
            if (_instance == null) {
                return null;
            }
        }
        return _instance;
    }

    /**
     * Inits the registry. should be called from the root window.
     * 
     * @param mainWindow
     *        thw root window
     */
    private static synchronized void init() {
        if (_instance == null)
            _instance = new SPFRegistry();

        _instance.properties = new SPFProperties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(PROP_URL));
            _instance.properties.load(fis);
        }
        catch (MalformedURLException e) {
            log.error("malformed URL for config file.", e);
        }
        catch (IOException e) {
            log.error("Could not read config file '" + PROP_URL + "'", e);
        }
        
        try {
            _instance.initModules();
        }
        catch (ClassNotFoundException e) {
            log.warn(null, e);
        }
        catch (InstantiationException e) {
            log.warn(null, e);
        }
        catch (IllegalAccessException e) {
            log.warn(null, e);
        }

        try {
            if (fis != null)
                fis.close();
        }
        catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Initialize all registered modules.
     * 
     * @throws ClassNotFoundException
     *         if the class to be reflected is not in the classpath.
     * @throws InstantiationException
     *         if the class to be reflected could not be instantiated.
     * @throws IllegalAccessException
     *         if the class to be reflected is not accessible
     */
    private void initModules() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException e) {
            log.warn(e.getMessage(), e);
        }
        catch (InstantiationException e) {
            log.warn(e.getMessage(), e);
        }
        catch (IllegalAccessException e) {
            log.warn(e.getMessage(), e);
        }
        catch (UnsupportedLookAndFeelException e) {
            log.warn(e.getMessage(), e);
        }

        SplashDialog splash = new SplashDialog();
        Thread t = new Thread(splash);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();

        this.modules = new ArrayList<IModule>();
        this.engine = new SPFEngine();
        this.mainFrame = new SPFMainFrame();
        this.engine.setMainFrame(this.mainFrame);
        this.engine.addPositionListener(this.mainFrame.getMapPanel());
        this.statusChangeListeners = new ArrayList<IStatusChangeListener>();
        this.outputMessageListeners = new ArrayList<IOutputMessageListener>();

        /*
         * IOutputPlugins
         */
        List<IOutputPlugin> oplugs = new ArrayList<IOutputPlugin>();

        String tmp = this.properties.getProperty(OUTPUT_PLUGINS_PROP);
        if (tmp == null)
            log.warn("No config parameter " + OUTPUT_PLUGINS_PROP + " given.");
        else {

            tmp = tmp.trim();
            String[] oplugins = tmp.split(LIST_SEPARATOR);
            String[] params = null;
            Class< ? >[] paramsClassArray = null;

            if ( !tmp.equals("")) {

                // int opCount = 1;
                for (String classString : oplugins) {
                    classString = classString.trim();

                    if (classString.length() == 0) continue;
                    
                    /*
                     * do we have any parameters?
                     */
                    int pos = classString.indexOf("(");
                    if (pos > 0) {
                        params = classString.substring(pos + 1, classString.length() - 1).split(",");
                        paramsClassArray = new Class[params.length];
                        if (params.length > 0) {
                            for (int i = 0; i < params.length; i++) {
                                paramsClassArray[i] = String.class;
                                params[i] = params[i].trim();
                            }
                            classString = classString.substring(0, pos);
                        }
                    }

                    Class< ? > clazz;
                    try {
                        clazz = Class.forName(classString);
                    }
                    catch (ClassNotFoundException e) {
                        log.error(null, e);
                        continue;
                    }
                    catch (NoClassDefFoundError e) {
                    	log.error(null, e);
                        continue;
                    }

                    try {
                        IOutputPlugin oplugin = null;

                        if (params != null && params.length > 0) {
                            Constructor< ? > constructor = clazz.getConstructor(paramsClassArray);
                            oplugin = (IOutputPlugin) constructor.newInstance((Object[]) params);
                            params = null;
                            paramsClassArray = null;
                        }
                        else {
                            try {
                                oplugin = ((IOutputPlugin) clazz.newInstance());
                            }
                            catch (InstantiationException e) {
                                log.error(null, e);
                                continue;
                            }
                        }

                        oplugs.add(oplugin);

                    }
                    catch (IllegalAccessException e) {
                        log.error("Could not access class with name '" + classString + "'.", e);
                    }
                    catch (InstantiationException e) {
                        log.error("Could not instantiate class with name '" + classString + "'.", e);
                    }
                    catch (SecurityException e) {
                        log.error(null, e);
                    }
                    catch (NoSuchMethodException e) {
                        log.error("A constructor with "
                                          + ( (paramsClassArray == null) ? "NULL"
                                                                        : Integer.valueOf(paramsClassArray.length))
                                          + " String arguments for Class " + clazz.getName() + " could not be found.",
                                  e);
                    }
                    catch (IllegalArgumentException e) {
                        log.error(null, e);
                    }
                    catch (InvocationTargetException e) {
                        log.error(null, e);
                    }
                    catch (ClassCastException e) {
                        log.error(clazz.getName() + " is not an instance of IOutputPlugin! Could not instantiate.", e);
                    }
                    catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                    // opCount++;
                }
            }
            else
                log.warn(OUTPUT_PLUGINS_PROP + " is empty, no output plugins defined.");
        }

        /*
         * worker threadpool
         */
        this.threadPool = SPFThreadPool.getInstance();
        this.modules.add(this.threadPool);

        /*
         * IInputPlugins
         */
        List<IInputPlugin> iplugs = new ArrayList<IInputPlugin>();

        tmp = this.properties.getProperty(INPUT_PLUGINS_PROP);
        if (tmp == null) {
            log.warn("No config parameter " + INPUT_PLUGINS_PROP + " given.");
        }
        else {
            tmp = tmp.trim();
            String[] params = null;
            Class< ? >[] paramsClassArray = null;
            String[] iplugins = tmp.split(LIST_SEPARATOR);

            if ( !tmp.equals("")) {
                for (String classString : iplugins) {
                    classString = classString.trim();

                    if (classString.length() == 0) continue;
                    
                    /*
                     * do we have any parameters?
                     */
                    int pos = classString.indexOf("(");
                    if (pos > 0) {
                        params = classString.substring(pos + 1, classString.length() - 1).split(",");
                        paramsClassArray = new Class[params.length];
                        if (params.length > 0) {
                            for (int i = 0; i < params.length; i++) {
                                paramsClassArray[i] = String.class;
                                params[i] = params[i].trim();
                            }
                            classString = classString.substring(0, pos);
                        }
                    }

                    Class< ? > clazz;
                    try {
                        clazz = Class.forName(classString);
                    }
                    catch (ClassNotFoundException e) {
                        log.error(null, e);
                        continue;
                    }
                    catch (NoClassDefFoundError e) {
                    	log.error(null, e);
                        continue;
                    }

                    if (clazz != null) {
                        IInputPlugin iplugin = null;
                        if (pos > 0) {
                            try {
                                Constructor< ? > constructor = clazz.getConstructor(paramsClassArray);
                                iplugin = (IInputPlugin) constructor.newInstance((Object[]) params);
                            }
                            catch (IllegalAccessException e) {
                                log.error("Could not access class with name '" + classString + "'.", e);
                            }
                            catch (InstantiationException e) {
                                log.error("Could not instantiate class with name '" + classString + "'.", e);
                            }
                            catch (SecurityException e) {
                                log.error(null, e);
                            }
                            catch (NoSuchMethodException e) {
                                log.error("A constructor with "
                                                  + ( (paramsClassArray == null) ? "NULL"
                                                                                : Integer.valueOf(paramsClassArray.length))
                                                  + " String arguments for Class " + clazz.getName()
                                                  + " could not be found.",
                                          e);
                            }
                            catch (IllegalArgumentException e) {
                                log.error(null, e);
                            }
                            catch (InvocationTargetException e) {
                                log.error(null, e);
                            }
                            catch (ClassCastException e) {
                                log.error(clazz.getName()
                                        + " is not an instance of IInputPlugin! Could not instantiate.", e);
                            }

                            params = null;
                            paramsClassArray = null;
                        }
                        else {
                            try {
                                iplugin = (IInputPlugin) clazz.newInstance();
                            }
                            catch (InstantiationException e) {
                                log.error(null, e);
                                continue;
                            }
                        }
                        iplugs.add(iplugin);
                    }
                }
            }
            else
                log.warn(INPUT_PLUGINS_PROP + " is empty, no input plugins defined.");
        }

        /*
         * AbstractDataProcessors
         */
        tmp = this.properties.getProperty(ABSTRACT_DATA_PROCESSORS).trim();
        String[] dps = tmp.split(LIST_SEPARATOR);

        this.dataProcessors = new ArrayList<Class< ? >>();
        if ( !tmp.equals("")) {
            for (String classString : dps) {
                String[] params = null;
                Class< ? >[] paramsClassArray = null;
                classString = classString.trim();

                /*
                 * do we have any parameters?
                 */
                int pos = classString.indexOf("(");
                if (pos > 0) {
                    params = classString.substring(pos + 1, classString.length() - 1).split(",");
                    paramsClassArray = new Class[params.length];
                    if (params.length > 0) {
                        for (int i = 0; i < params.length; i++) {
                            paramsClassArray[i] = String.class;
                            params[i] = params[i].trim();
                        }
                        classString = classString.substring(0, pos);
                    }
                }

                Class< ? > clazz;
                try {
                    clazz = Class.forName(classString);
                }
                catch (ClassNotFoundException e) {
                    log.error(null, e);
                    continue;
                }
                catch (NoClassDefFoundError e) {
                	log.error(null, e);
                    continue;
                }

                this.dataProcessors.add(clazz);
            }
        }
        
        loadExtensions();

        splash.activateProgressBar(this.modules.size() + this.dataProcessors.size() + iplugs.size() + oplugs.size());

        /*
         * initialise all modules
         */
        for (IModule module : this.modules) {
            if (module != null) {
                try {
                    module.init();
                }
                catch (Exception e) {
                    log.warn(null, e);
                }
            }
            splash.processFinished();
        }

        /*
         * as a last step startup of all output and input plugins as they should not create data before other
         * parts are running.
         */
        for (IOutputPlugin iOutputPlugin : oplugs) {
            if (iOutputPlugin == null)
                continue;
            try {
                registerOutputPlugin(iOutputPlugin);
            }
            catch (Throwable e) {
                log.warn(e.getMessage(), e);
            }
            splash.processFinished();
        }

        for (IInputPlugin iInputPlugin : iplugs) {
            if (iInputPlugin == null)
                continue;
            try {
                registerInputPlugin(iInputPlugin);
            }
            catch (Throwable e) {
                log.warn(e.getMessage(), e);
            }
            splash.processFinished();
        }

        /*
         * add all status change listeners
         */
        this.statusChangeListeners.add(this.mainFrame);
        this.outputMessageListeners.add(this.mainFrame);

        /*
         * set the default view
         */
        this.mainFrame.switchToDefaultTab();
        
        splash.removeSelf();

        this.mainFrame.setVisible(true);
        this.engine.start();
        
        lifecycleLog.info("Module loading completed.");
    }

    private void loadExtensions() {
    	 String tmp = this.properties.getProperty(EXTENSIONS_PROP);
    	 
    	 if (tmp == null) {
    		 log.info("No extensions found.");
    		 return;
    	 }
    	 
         String[] dps = tmp.trim().split(LIST_SEPARATOR);

         this.extensions = new ArrayList<IExtension>();
         if ( !tmp.equals("")) {
             for (String classString : dps) {
                 String[] params = null;
                 Class< ? >[] paramsClassArray = null;
                 classString = classString.trim();

                 /*
                  * do we have any parameters?
                  */
                 int pos = classString.indexOf("(");
                 if (pos > 0) {
                     params = classString.substring(pos + 1, classString.length() - 1).split(",");
                     paramsClassArray = new Class[params.length];
                     if (params.length > 0) {
                         for (int i = 0; i < params.length; i++) {
                             paramsClassArray[i] = String.class;
                             params[i] = params[i].trim();
                         }
                         classString = classString.substring(0, pos);
                     }
                 }

                 Class< ? > clazz;
                 try {
                     clazz = Class.forName(classString);
                 }
                 catch (ClassNotFoundException e) {
                     log.error(null, e);
                     continue;
                 }
                 catch (NoClassDefFoundError e) {
                 	log.error(null, e);
                     continue;
                 }

                 IExtension ext = (IExtension) instantiateWithParameters(clazz, paramsClassArray, params, IExtension.class);
                 this.extensions.add(ext);
                 this.modules.add(ext);
             }
         }		
	}

	private Object instantiateWithParameters(Class<?> clazz,
			Class<?>[] paramsClassArray, String[] params, Class<IExtension> interfaze) {
		if (!interfaze.isAssignableFrom(clazz)) {
			return null;
		}
		
		Object result = null;
		
		if (params != null && params.length > 0) {
            try {
                Constructor< ? > constructor = clazz.getConstructor(paramsClassArray);
                result = constructor.newInstance((Object[]) params);
            }
            catch (IllegalAccessException e) {
                log.error("Could not access class with name '" + clazz.getName() + "'.", e);
            }
            catch (InstantiationException e) {
                log.error("Could not instantiate class with name '" + clazz.getName() + "'.", e);
            }
            catch (SecurityException e) {
                log.error(null, e);
            }
            catch (NoSuchMethodException e) {
                log.error("A constructor with "
                                  + ( (paramsClassArray == null) ? "NULL"
                                                                : Integer.valueOf(paramsClassArray.length))
                                  + " String arguments for Class " + clazz.getName()
                                  + " could not be found.",
                          e);
            }
            catch (IllegalArgumentException e) {
                log.error(null, e);
            }
            catch (InvocationTargetException e) {
                log.error(null, e);
            }
            catch (ClassCastException e) {
                log.error(clazz.getName()
                        + " is not an instance of IInputPlugin! Could not instantiate.", e);
            }

            params = null;
            paramsClassArray = null;
        }
        else {
            try {
				result = clazz.newInstance();
            }
            catch (InstantiationException e) {
                log.error(null, e);
            } catch (IllegalAccessException e) {
            	 log.error(null, e);
			}
        }
		
		return result;
	}

	/**
     * @param module
     *        the module to add (is then shutdown)
     */
    private void addModule(IModule module) {
        this.modules.add(module);
    }

    /**
     * Use this method to register an {@link IInputPlugin} at runtime programmatically.
     * 
     * @param iplugin
     *        the new plugin
     * @throws Exception
     *         initialisation could fail
     */
    public void registerInputPlugin(IInputPlugin iplugin) throws Throwable {
        addModule(iplugin);
        this.engine.registerInputPlugin(iplugin, this.dataProcessors);
        iplugin.init();
        this.mainFrame.addInputPlugin(iplugin);
    }

    /**
     * Use this method to register an {@link IOutputPlugin} at runtime programmatically.
     * 
     * @param oplugin
     *        the new {@link IOutputPlugin}
     * @throws Exception
     *         fails of init() of plugin throw exception
     */
    public void registerOutputPlugin(IOutputPlugin oplugin) throws Throwable {
        addModule(oplugin);
        this.engine.registerOutputPlugin(oplugin);
        oplugin.init();
        this.mainFrame.addOutputPlugin(oplugin);
    }
    
    

	/**
     * @throws Exception
     *         if an exception occured while shutting down a module.
     */
    public void shutdownModules() throws Exception {
        this.shuttingDown = true;
        lifecycleLog.info("Shutting down modules...");
        for (IModule module : this.modules) {
            try {
                module.shutdown();
            }
            catch (Exception e) {
                log.error(null, e);
            }
        }
        lifecycleLog.info("Shutdown complete!");
    }

    /**
     * Global method to shutdown the program.
     * 
     * @throws Exception
     *         if shutdown failed
     */
    public void shutdownSystem() throws Exception {
        this.engine.shutdown();

        shutdownModules();
    }

    /**
     * set the pnp mode in the engine
     * 
     * @param selected
     *        active or not
     */
    public void setPNPMode(boolean selected) {
        this.engine.setPNPMode(selected);
    }

    /**
     * Retunrs the plugin description for a plugin name.
     * 
     * @param name
     *        the plugin name
     * @return the description
     */
    public Plugin getPluginForName(String name) {
        return this.engine.getPluginForName(name);
    }

    /**
     * @return if shutdown was called.
     */
    public boolean isShuttingDown() {
        return this.shuttingDown;
    }

    /**
     * @return the singleton threadpool
     */
    public SPFThreadPool getThreadPool() {
        return this.threadPool;
    }

    /**
     * @param key
     *        property key
     * @return the string value for this configuration property
     */
    public String getConfigProperty(String key) {
        return this.properties.getProperty(key);
    }

    /**
     * @return a list of {@link IStatusChangeListener}
     */
    public List<IStatusChangeListener> getStatusChangeListeners() {
        return this.statusChangeListeners;
    }

    /**
     * @return a list of {@link IOutputMessageListener}
     */
    public List<IOutputMessageListener> getOutputMessageListeners() {
        return this.outputMessageListeners;
    }


	
	/**
	 * @return the modules
	 */
	public List<IModule> getModules() {
		return this.modules;
	}

    
    /**
     * this method gets called whenever the metadata (input, output) of an InputPlugin has changed. The
     * restart is needed because one OutputPlugin may define its behaviour based on input fields.
     */
    public void restartOutputPlugins() {
        this.engine.restartOutputPlugins();
    }
    

	/**
	 * @param key the key
	 * @param string the string
	 */
	public void setConfigProperty(String key, String string) {
		this.properties.setProperty(key, string);
	}
	
    public void saveConfiguration() throws IOException {
    	this.properties.store(new FileOutputStream(PROP_URL), PROPERTIES_COMMENT);
    }

    /************
     * 
     * ###### Main entry point of the framework. ######
     * 
     * @param args
     *        unused
     ************/
    public static void main(String[] args) {
        SPFRegistry.getInstance();
    }


}
