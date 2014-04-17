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

package org.n52.ifgicopter.spf.input;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.n52.ifgicopter.spf.SPFEngine;
import org.n52.ifgicopter.spf.SPFRegistry;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.common.IPositionListener;
import org.n52.ifgicopter.spf.data.AbstractDataProcessor;
import org.n52.ifgicopter.spf.data.AbstractInterpolator;
import org.n52.ifgicopter.spf.gui.PNPDialog;
import org.n52.ifgicopter.spf.util.ConcurrentSortedList;
import org.n52.ifgicopter.spf.util.LongComparator;
import org.n52.ifgicopter.spf.xml.Item;
import org.n52.ifgicopter.spf.xml.Plugin;

/**
 * Class for wrapping a {@link Plugin}. Every data is collected here and processed when requested by the
 * output thread.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class InputPluginCollector implements IModule {

    Plugin plugin;

    /**
     * this Map saves a timestamp-ordered Map for each inputProperty of the plugin
     */
    Map<String, SortedMap<Long, Object>> itemCollection = new HashMap<String, SortedMap<Long, Object>>();

    /**
     * static because only one instance is needed
     */
    static AbstractInterpolator interpolator;

    /**
     * reflections instantation of AbstractInterpolator. static because only one instance is needed
     */
    static {
        Class< ? > clazz = null;
        try {
            clazz = Class.forName(SPFRegistry.getInstance().getConfigProperty(SPFRegistry.ABSTRACT_INTERPOLATOR).trim());
        }
        catch (ClassNotFoundException e) {
            LogFactory.getLog(InputPluginCollector.class).error(null, e);
            System.exit(1);
        }

        try {
            if (clazz != null)
                interpolator = (AbstractInterpolator) clazz.newInstance();
        }
        catch (Exception e) {
            LogFactory.getLog(InputPluginCollector.class).error(null, e);
            System.exit(1);
        }
    }

    /**
     * the currently active timestamps. this is needed for garbage threads. the garbage thread will not remove
     * data greater or equal to the smallest contained timestamp.
     */
    List<Long> activeTimestamps = new ConcurrentSortedList<Long>(new LongComparator());

    /**
     * this is needed for garbage threads. the garbage thread will not remove data greater or equal to this
     * timestamp.
     */
    long lastOutputTimeStamp;

    protected static Log log = LogFactory.getLog(InputPluginCollector.class);

    List<OnAvailableOutputThread> outputThreads = new ArrayList<OnAvailableOutputThread>();

    /**
     * used to add data to the collection. this is used to not block InputPlugins while an OutputPlugin does
     * heavy output calculations.
     */
    private ExecutorService addDataExecutor = Executors.newSingleThreadExecutor();

    /**
     * only do output if data is available for all items of this plugin
     */
    boolean outputOnAllItems = false;

    /**
     * do output every time new data is available
     */
    boolean availabilityBehaviour = false;

    private PeriodOutputThread periodOutputThread;
    private GarbageWorkerThread garbageThread;

    SPFEngine engine;

    private List<AbstractDataProcessor> dataProcessors = new ArrayList<AbstractDataProcessor>();

    /**
     * flag determining the availabilty of minimum one item per mandatoryProperty
     */
    private boolean hasMandatories = false;

    /**
     * if set to true the framework tries to recognize unknown data properties
     */
    private boolean plugAndPlayBehaviour = false;

    /**
     * @param plugin
     *        the plugin to be wrapped in the collector instance.
     * @param engine
     *        the SPF engine instance for populating data
     */
    public InputPluginCollector(Plugin plugin, SPFEngine engine) {
        this.plugin = plugin;
        this.engine = engine;

        for (String it : this.plugin.getInputProperties()) {
            if (it.equals(this.plugin.getTime().getProperty())) {
                continue;
            }
            this.itemCollection.put(it, new TreeMap<Long, Object>());
        }

        /*
         * for periodly pushing start a timer thread
         */
        if (this.plugin.getOutputType().equals("period")) {
            this.periodOutputThread = new PeriodOutputThread(this.plugin.getTimeDelta());
            this.periodOutputThread.start();
        }
        /*
         * only do output if data is avaiable
         */
        else if (this.plugin.getOutputType().equals("available")) {
            this.availabilityBehaviour = true;
        }

        /*
         * set up the garbage thread
         */
        this.garbageThread = new GarbageWorkerThread();
        this.garbageThread.start();

        log.debug("New InputPluginConnector started for " + plugin);
    }

    /**
     * @param newData
     *        new data
     * @throws Exception
     *         if processing failed
     */
    public void addNewData(final Map<String, Object> newData) {

        /*
         * do we have IPositionListeners?
         */
        if (this.engine.getPositionListeners().size() > 0) {
            if (InputPluginCollector.this.plugin.isMobile()) {
                final Object first = newData.get(InputPluginCollector.this.plugin.getLocation().getFirstCoordinateName());
                final Object second = newData.get(InputPluginCollector.this.plugin.getLocation().getSecondCoordinateName());

                /*
                 * start a new thread to avoid blocking
                 */
                SPFRegistry.getInstance().getThreadPool().submitTask(new Runnable() {
                    @Override
                    public void run() {
                        if (first != null && second != null) {
                            for (IPositionListener ipl : InputPluginCollector.this.engine.getPositionListeners()) {
                                ipl.positionUpdate(InputPluginCollector.this.plugin, newData);
                            }
                        }
                    }
                });

            }
        }

        /*
         * we need to check which type the time parameter is of
         */
        Object tmp = newData.get(InputPluginCollector.this.plugin.getTime().getProperty());

        Long time = null;
        if (tmp instanceof Long) {
            time = (Long) tmp;
        }
        else if (tmp instanceof String) {
            try {
                /*
                 * try millis as string
                 */
                time = Long.valueOf((String) tmp);
            }
            catch (NumberFormatException e) {
                /*
                 * try iso-date
                 */
                time = Long.valueOf(new DateTime(tmp).getMillis());
            }
        }
        else if (tmp instanceof Date) {
            time = Long.valueOf(new DateTime(tmp).getMillis());
        }
        else if (tmp instanceof DateTime) {
            time = Long.valueOf( ((DateTime) tmp).getMillis());
        }

        if (time == null) {
            log.warn("Could not process the timestamp '" + tmp + "'. Using current system time.");
            time = Long.valueOf(System.currentTimeMillis());
        }

        synchronized (InputPluginCollector.this.itemCollection) {

            /*
             * check for p'n'p feature
             */
            if (this.plugAndPlayBehaviour) {
                for (String key : newData.keySet()) {
                    if ( !InputPluginCollector.this.plugin.getInputProperties().contains(key)) {
                        /*
                         * try to recognize unknown property
                         */
                        PNPDialog pnp = this.engine.doPNP(key);

                        if (pnp.isCanceled()) {
                            /*
                             * the dialog was cancelled continue
                             */
                            continue;
                        }

                        Item item = new Item(key);
                        item.setDataType(pnp.getDatatype());
                        item.setDefinition(pnp.getDefintion());
                        item.setUom(pnp.getUom());

                        /*
                         * add to the plugin description
                         */
                        if (pnp.isOutput()) {
                            InputPluginCollector.this.plugin.addOutputProperty(item);
                        }
                        else if (pnp.isMandatory()) {
                            InputPluginCollector.this.plugin.addMandatoryProperty(item);
                        }
                        else {
                            InputPluginCollector.this.plugin.addInputProperty(item);
                        }

                        InputPluginCollector.this.itemCollection.put(key, new TreeMap<Long, Object>());
                    }
                }
            }

            for (Entry<String, Object> entry : newData.entrySet()) {
                if ( !entry.getKey().equals(InputPluginCollector.this.plugin.getTime().getProperty())) {
                    if ( !InputPluginCollector.this.plugin.getInputProperties().contains(entry.getKey())) {
                        continue;
                    }
                    Map<Long, Object> collection = InputPluginCollector.this.itemCollection.get(entry.getKey());

                    /*
                     * put the data of this item to this itemlist.
                     */
                    collection.put(time, entry.getValue());
                }
            }

            /*
             * first check if there is minimum one item per mandatory property in the Collector. if not
             * checked, we will be stuck with a deadlock in the OnAvailableOutputThread.
             */
            if ( !InputPluginCollector.this.hasMandatories) {
                if (InputPluginCollector.this.plugin.getMandatoryProperties().size() == 0) {
                    InputPluginCollector.this.hasMandatories = true;
                }
                for (String prop : InputPluginCollector.this.plugin.getMandatoryProperties()) {
                    SortedMap<Long, Object> data = InputPluginCollector.this.itemCollection.get(prop);
                    if ( !data.isEmpty() && data.firstKey().longValue() <= time.longValue()) {
                        /*
                         * ok, continue;
                         */
                        InputPluginCollector.this.hasMandatories = true;
                    }
                    else {
                        InputPluginCollector.this.hasMandatories = false;
                        return;
                    }
                }
            }
        }

        if (InputPluginCollector.this.availabilityBehaviour) {
            /*
             * check if outputItems are here
             */
            Set<String> containsOutputs = new HashSet<String>();

            for (String prop : InputPluginCollector.this.plugin.getOutputProperties()) {
                if (newData.keySet().contains(prop)) {
                    containsOutputs.add(prop);
                }
            }

            if (containsOutputs.size() > 0) {
                /*
                 * This is an output creating property!!
                 * 
                 * start new thread and put register all items
                 */
                if (InputPluginCollector.this.activeTimestamps.contains(time)) {
                    /*
                     * do not create a thread if we already have one for this timestamp -> data would be
                     * duplicated
                     */
                    return;
                }
                OnAvailableOutputThread ot = new OnAvailableOutputThread(time);
                for (String item : newData.keySet()) {
                    /*
                     * register items at the new thread only add outputProperties or mandatoryProperties items
                     * (others should not be included in this data tuple)
                     */
                    if (containsOutputs.contains(item)
                            || InputPluginCollector.this.plugin.getMandatoryProperties().contains(item)) {
                        ot.registerItem(item);
                    }
                }
                synchronized (InputPluginCollector.this.outputThreads) {
                    InputPluginCollector.this.outputThreads.add(ot);
                }
                SPFRegistry.getInstance().getThreadPool().submitTask(ot);
            }
            else {
                /*
                 * register at the threads - perhaps one or more are waiting
                 */
                synchronized (InputPluginCollector.this.outputThreads) {
                    for (OnAvailableOutputThread ot : InputPluginCollector.this.outputThreads) {
                        for (String item : newData.keySet()) {

                            if (newData.get(item) == null)
                                continue;
                            /*
                             * only add mandatoryProperties. the other will not be added at this data tuple.
                             */
                            if (InputPluginCollector.this.plugin.getMandatoryProperties().contains(item)) {
                                ot.registerItem(item);
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * @return the plugin wrapped in this collector instance.
     */
    public Plugin getPlugin() {
        return this.plugin;
    }

    /**
     * This methods realizes the extension point for data processing methods. An implementing class of
     * {@link AbstractDataProcessor} is called and then processes all the data according to its
     * implementation.
     * 
     * @param data
     * @return the processed data
     */
    public Map<String, Object> callDataProcessors(Map<String, Object> data) {
        if (this.dataProcessors.isEmpty())
            return data;

        Map<String, Object> tmp = new HashMap<String, Object>(data);
        for (AbstractDataProcessor adp : this.dataProcessors) {
            tmp = adp.processData(tmp);
        }

        return tmp;
    }

    /**
     * this method instantiates all data processors for this plugin.
     * 
     * @param dataPocs
     *        set a list of data processors.
     */
    public void setDataProcessors(List<Class< ? >> dataPocs) {

        /*
         * instatiate all for this plugin
         */
        for (Class< ? > clazz : dataPocs) {
            try {
                Constructor< ? > constructor = clazz.getConstructor(new Class[] {Plugin.class});
                this.dataProcessors.add((AbstractDataProcessor) constructor.newInstance(new Object[] {this.plugin}));
            }
            catch (IllegalArgumentException e) {
                log.warn(e.getMessage(), e);
            }
            catch (InstantiationException e) {
                log.warn(e.getMessage(), e);
            }
            catch (IllegalAccessException e) {
                log.warn(e.getMessage(), e);
            }
            catch (InvocationTargetException e) {
                log.warn(e.getMessage(), e);
            }
            catch (SecurityException e) {
                log.warn(e.getMessage(), e);
            }
            catch (NoSuchMethodException e) {
                log.warn(e.getMessage(), e);
            }
        }

    }

    /**
     * use this method to enable/disable plug'n'play behaviour of the framework. in pnp-mode the framework
     * tries to recognize unkown data properties. activating this feautre slows down the processing of data.
     * 
     * @param plugAndPlayBehaviour
     *        the plugAndPlayBehaviour to set
     */
    public void setPlugAndPlayBehaviour(boolean plugAndPlayBehaviour) {
        this.plugAndPlayBehaviour = plugAndPlayBehaviour;
    }

    @Override
    public void init() throws Exception {
        //
    }

    @Override
    public void shutdown() throws Exception {
        if (this.periodOutputThread != null) {
            this.periodOutputThread.setRunning(false);
        }

        this.garbageThread.setRunning(false);

        this.addDataExecutor.shutdown();
        this.addDataExecutor.awaitTermination(10, TimeUnit.SECONDS);
        this.addDataExecutor.shutdownNow();
    }

    /**
     * Thread for periodly pulling one data set data from the corresponding collector.
     * 
     * @author Matthes Rieke <m.rieke@uni-muenster.de>
     * 
     */
    public class PeriodOutputThread extends Thread {

        private int period;
        private boolean running = true;

        /**
         * @param delta
         *        the period
         */
        public PeriodOutputThread(int delta) {
            this.period = delta;
            this.setName(InputPluginCollector.this.getPlugin().getName() + "-period-thread");
        }

        @Override
        public void run() {
            super.run();

            while (this.running) {
                /*
                 * pull periodly
                 */
                try {
                    Thread.sleep(this.period);
                }
                catch (InterruptedException e) {
                    InputPluginCollector.log.warn(null, e);
                }

                /*
                 * output here
                 */
                Map<String, Object> data = this.getLatestData();

                /*
                 * call all extension points for data processing
                 */
                data = callDataProcessors(data);

                InputPluginCollector.this.engine.doSingleOutput(data, InputPluginCollector.this.getPlugin());

            }
        }

        /**
         * Returns one set of data items.
         * 
         * @return the last available data. the timestamp is the one for which all items are available.
         */
        public Map<String, Object> getLatestData() {
            /*
             * get the timestamp that is available for all plugin items.
             */
            long latestTimestamp = Long.MAX_VALUE;
            Map<String, long[]> intervals = new HashMap<String, long[]>();
            for (String item : InputPluginCollector.this.itemCollection.keySet()) {
                SortedMap<Long, Object> map = InputPluginCollector.this.itemCollection.get(item);

                if (InputPluginCollector.this.outputOnAllItems && map.isEmpty()) {
                    /*
                     * we only generate output if all items are available
                     */
                    return null;
                }

                if ( !map.isEmpty()) {
                    if (map.lastKey().longValue() < latestTimestamp) {
                        latestTimestamp = map.lastKey().longValue();
                    }

                    // put into intervals
                    intervals.put(item, new long[] {map.firstKey().longValue(), map.lastKey().longValue()});
                }

            }

            /*
             * did we already do output for this timestamp?
             */
            if (InputPluginCollector.this.lastOutputTimeStamp == latestTimestamp) {
                return null;
            }

            /*
             * now as we have the latest timestamp we have to check if all items have data before and after
             * (or at same time) so interpolation can be computed
             */
            for (Entry<String, long[]> entry : intervals.entrySet()) {
                if ( ! (latestTimestamp >= entry.getValue()[0] && latestTimestamp <= entry.getValue()[1])) {
                    return null;
                }
            }

            InputPluginCollector.this.lastOutputTimeStamp = latestTimestamp;

            Map<String, Object> result = null;

            result = InputPluginCollector.interpolator.interpolateForTimestamp(InputPluginCollector.this.itemCollection,
                                                                               latestTimestamp);
            result.put(InputPluginCollector.this.plugin.getTime().getProperty(), Long.valueOf(latestTimestamp));

            return result;
        }

        /**
         * @param running
         *        set false to stop the thread gracefully
         */
        public void setRunning(boolean running) {
            this.running = running;
        }

    }

    /**
     * This thread listens for new data items and generates output.
     * 
     * @author Matthes Rieke <m.rieke@uni-muenster.de>
     * 
     */
    public class OnAvailableOutputThread implements Runnable {

        private Set<String> availableItems = new HashSet<String>();
        private Long timestamp;

        /**
         * @param time
         *        the timestamp for which this thread should do output
         */
        public OnAvailableOutputThread(Long time) {
            this.timestamp = time;

            InputPluginCollector.this.activeTimestamps.add(this.timestamp);

            // synchronized (InputPluginCollector.this.outputBuffer) {
            // InputPluginCollector.this.outputBuffer.add(this.timestamp);
            // }
        }

        /**
         * @param item
         *        the newly available item
         */
        public void registerItem(String item) {
            synchronized (this.availableItems) {
                this.availableItems.add(item);

                /*
                 * wake the sleeping thread.
                 */
                this.availableItems.notifyAll();
            }
        }

        @Override
        public void run() {
            synchronized (this.availableItems) {
                if (log.isDebugEnabled())
                    log.debug("Available: " + this.availableItems + "\tMandatory: "
                            + Arrays.toString(InputPluginCollector.this.plugin.getMandatoryProperties().toArray()));

                /*
                 * wait until we have all items we need for output
                 */
                if (!this.availableItems.containsAll(InputPluginCollector.this.plugin.getMandatoryProperties())) {
                    log.info("[potentially waiting] Not all mandatory properties "
                            + Arrays.toString(InputPluginCollector.this.plugin.getMandatoryProperties().toArray())
                            + " found in available items " + this.availableItems + ", waiting...");
                }
                while ( !this.availableItems.containsAll(InputPluginCollector.this.plugin.getMandatoryProperties())) {
                    try {
                        this.availableItems.wait();
                    }
                    catch (InterruptedException e) {
                        InputPluginCollector.log.warn(null, e);
                    }
                }
            }

            if (log.isDebugEnabled())
                log.debug("All required items found.");

            /*
             * remove self from this.outputThreads because all items are here
             */
            synchronized (InputPluginCollector.this.outputThreads) {
                InputPluginCollector.this.outputThreads.remove(this);
            }

            /*
             * we have all items -> do the output
             */
            Map<String, Object> outData = null;
            synchronized (InputPluginCollector.this.itemCollection) {
                try {
                    outData = InputPluginCollector.interpolator.interpolateForTimestamp(InputPluginCollector.this.itemCollection,
                                                                                        this.timestamp,
                                                                                        this.availableItems);
                }
                catch (Exception e) {
                    log.warn(e);
                }
            }

            /*
             * send to engine
             */
            if (outData == null) {
                /*
                 * this can only happen if we received output generating properties for a time period and
                 * later on received the mandatory properties.
                 */
                InputPluginCollector.log.warn("Got a null tuple from interpolator. This is strange!");

                /*
                 * we can not process this, remove from activity list
                 */
                synchronized (InputPluginCollector.this.activeTimestamps) {
                    InputPluginCollector.this.activeTimestamps.remove(this.timestamp);
                    InputPluginCollector.this.activeTimestamps.notifyAll();
                }
            }
            else {
                outData.put(InputPluginCollector.this.plugin.getTime().getProperty(), this.timestamp);

                /*
                 * call all AbstractDataProcesser instances
                 */
                outData = callDataProcessors(outData);

                if (InputPluginCollector.log.isDebugEnabled()) {
                    InputPluginCollector.log.error("Doing output for " + this.timestamp);
                }

                /*
                 * wait until this thread is the one with the earliest timestamp
                 */
                synchronized (InputPluginCollector.this.activeTimestamps) {
                    /*
                     * we can be sure that there is one timestamp minimally (ours) wait until ours is the
                     * latest. this ensures timestamp ordering of output
                     */
                    while (this.timestamp != InputPluginCollector.this.activeTimestamps.get(0)) {
                        try {
                            InputPluginCollector.this.activeTimestamps.wait();
                        }
                        catch (InterruptedException e) {
                            InputPluginCollector.log.warn(e.getMessage(), e);
                        }
                    }

                    InputPluginCollector.this.activeTimestamps.remove(this.timestamp);
                    InputPluginCollector.this.activeTimestamps.notifyAll();
                }

                InputPluginCollector.this.engine.doSingleOutput(outData, InputPluginCollector.this.getPlugin());
            }

            /*
             * set this timestamp as the last used
             */
            InputPluginCollector.this.lastOutputTimeStamp = this.timestamp.longValue();
        }

    }

    /**
     * Cleans the {@link InputPluginCollector#itemCollection} from unneeded data.
     * 
     * @author Matthes Rieke <m.rieke@uni-muenster.de>
     * 
     */
    public class GarbageWorkerThread extends Thread {

        private boolean running = true;
        private static final int PERIOD_IN_SEC = 15;

        /**
         * default constructor which starts the thread automatically
         */
        public GarbageWorkerThread() {
            this.setName(InputPluginCollector.this.getPlugin().getName() + "-plugin-garbage-thread");
        }

        @Override
        public void run() {
            while (this.running) {
                /*
                 * collect garbage every PERIOD_IN_SEC seconds
                 */
                try {
                    Thread.sleep(PERIOD_IN_SEC * 1000);
                }
                catch (InterruptedException e) {
                    InputPluginCollector.log.warn(e.getMessage(), e);
                }

                synchronized (InputPluginCollector.this.itemCollection) {
                    SortedMap<Long, Object> data;

                    /*
                     * get the timestamp for which last time output was generated or which is still
                     * processing. this timestamp plus the minimum count used by the interpolator MUST remain
                     * in the collection.
                     */
                    Long lastActive = null;
                    if (InputPluginCollector.this.activeTimestamps.size() > 0) {
                        lastActive = InputPluginCollector.this.activeTimestamps.get(0);
                    }
                    else {
                        /*
                         * this is the timestamp used by the PeriodOutputThread or if the activeTimestamp list
                         * is empty. so this IS really the last used timestamp if queried.
                         */
                        lastActive = Long.valueOf(InputPluginCollector.this.lastOutputTimeStamp);
                    }

                    /*
                     * could be if no data has arrived yet
                     */
                    if (lastActive == null)
                        continue;

                    int minimumCount = InputPluginCollector.interpolator.getMinimumDataCount();

                    for (String item : InputPluginCollector.this.itemCollection.keySet()) {
                        data = InputPluginCollector.this.itemCollection.get(item);

                        if (data.size() > minimumCount * 10) {

                            Long newFirstKey = null;

                            /*
                             * go through collection until we have a timestamp bigger than the last output
                             * timestamp -> use the predecessor as new head of collection
                             */
                            Iterator<Long> it = data.keySet().iterator();
                            int threshold = data.size() - 1 - minimumCount;

                            if (threshold < 0)
                                continue;

                            for (int i = 0; i < threshold; i++) {
                                Long key = it.next();
                                if (key.longValue() > lastActive.longValue()) {
                                    break;
                                }
                                newFirstKey = key;
                            }

                            if (newFirstKey != null) {
                                InputPluginCollector.this.itemCollection.put(item, data.tailMap(newFirstKey));
                            }
                        }
                    }
                }
            }
        }

        /**
         * @param running
         *        set false to stop this thread gracefully
         */
        public void setRunning(boolean running) {
            this.running = running;
        }

    }

}
