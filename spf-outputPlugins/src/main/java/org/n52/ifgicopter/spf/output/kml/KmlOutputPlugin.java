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

package org.n52.ifgicopter.spf.output.kml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import net.opengis.kml.x22.AbstractFeatureType;
import net.opengis.kml.x22.AbstractGeometryType;
import net.opengis.kml.x22.AbstractStyleSelectorType;
import net.opengis.kml.x22.AbstractViewType;
import net.opengis.kml.x22.BasicLinkType;
import net.opengis.kml.x22.DocumentType;
import net.opengis.kml.x22.ExtendedDataType;
import net.opengis.kml.x22.FolderType;
import net.opengis.kml.x22.IconStyleType;
import net.opengis.kml.x22.KmlDocument;
import net.opengis.kml.x22.KmlType;
import net.opengis.kml.x22.LinkType;
import net.opengis.kml.x22.LookAtType;
import net.opengis.kml.x22.NetworkLinkType;
import net.opengis.kml.x22.PlacemarkType;
import net.opengis.kml.x22.RefreshModeEnumType;
import net.opengis.kml.x22.SchemaDataType;
import net.opengis.kml.x22.SchemaType;
import net.opengis.kml.x22.StyleType;
import net.opengis.kml.x22.ViewRefreshModeEnumType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.ifgicopter.spf.common.IModule;
import org.n52.ifgicopter.spf.extension.FTPUploadExtension;
import org.n52.ifgicopter.spf.gui.ModuleGUI;
import org.n52.ifgicopter.spf.output.IOutputPlugin;
import org.n52.ifgicopter.spf.util.FileUtils;
import org.n52.ifgicopter.spf.util.XmlUtils;
import org.n52.ifgicopter.spf.xml.Location;
import org.n52.ifgicopter.spf.xml.Plugin;

import com.google.kml.ext.x22.SimpleArrayDataType;
import com.google.kml.ext.x22.SimpleArrayFieldType;
import com.google.kml.ext.x22.TrackType;

/**
 * http://www.opengeospatial.org/standards/kml
 * 
 * TODO wrap everything in one kmz file, allows to include images etc:
 * https://developers.google.com/kml/documentation/kmzarchives
 * 
 * @author Daniel Nüst (d.nuest@52north.org)
 * 
 */
public class KmlOutputPlugin implements IOutputPlugin {

    /**
     * https://developers.google.com/kml/documentation/kmlreference#link
     * 
     * @author Daniel Nüst (d.nuest@52north.org)
     * 
     */
    protected static class KmlLink {

        public URL href; // this possibly could be relative!

        public RefreshModeEnumType.Enum refreshMode = RefreshModeEnumType.ON_CHANGE;

        public ViewRefreshModeEnumType.Enum viewRefreshMode = ViewRefreshModeEnumType.ON_REQUEST;

        public int viewRefreshTime = 1;

        // public int viewBoundScale;
        // public String viewFormat;
        // public String httpQuery;
    }

    /**
     * For descriptions of the fields see
     * https://developers.google.com/kml/documentation/kmlreference#networklink
     * 
     * @author Daniel Nüst (d.nuest@52north.org)
     * 
     */
    protected static class NetworkLink {

        public String description;

        public boolean flyToView = true;

        public String id;

        public KmlLink link;

        public String name;

        public boolean refreshVisibility = true;

        public boolean visibility = true;

    }

    private static final int DEFAULT_INDENT = 4;

    private static final String DEFAULT_KML_FILE_PREFIX = "SPF";

    private static final double DEFAULT_REFRESH_INTERVAL = 0.5d;

    private static final String DYNAMIC_ROOT_FILENAME = "sensors";

    private static final String HTML_FILE_EXTENSION = "html";

    private static final String HTML_FILENAME = "";

    private static final String HTML_KML_REFERENCE_TAG = "$$KML_FILE$$";

    private static final String HTML_TEMPLATE_FILE = "/KmlOutputPlugin-GE-template.html";

    public static final XmlOptions KML_OPTIONS = new XmlOptions();

    protected static Log log = LogFactory.getLog(KmlOutputPlugin.class);

    private static FilenameFilter onlyKmlFiles = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(KmlConstants.KML_FILE_EXTENSION);
        }
    };

    private static final String PLUGIN_NAME = "KML Output";

    private static final String SAVE_CURRENT_FILE_TEXT = "Save Current File";

    public static final QName SCHEMA_LOCATION_ATTRIBUTE_QNAME = new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                                                                          "schemaLocation");

    private static final String SELECT_OUPUT_FOLDER = "Select Output Folder";

    private static final String STATIC_ROOT_FILENAME = "";

    static {
        KML_OPTIONS.setSavePrettyPrint();
        KML_OPTIONS.setSavePrettyPrintIndent(DEFAULT_INDENT);
        HashMap<String, String> suggestedPrefixes = new HashMap<String, String>();
        KmlConstants.addSuggestedPrefix(suggestedPrefixes);
        GxConstants.addSuggestedPrefix(suggestedPrefixes);
        KML_OPTIONS.setSaveSuggestedPrefixes(suggestedPrefixes);
    }

    public static void main(String[] args) throws Exception {
        KmlOutputPlugin p = new KmlOutputPlugin();
        p.init();
        p.handleNewSensor("asd", 1.0, 2.0, 3.0, new HashMap<String, Object>());
    }

    private Collection<String> copiedResources = new ArrayList<String>();

    private boolean createHTML = true; // TODO make property and settable in GUI

    private boolean createKMZ = false;

    private String filePrefix; // TODO make property and settable in GUI

    private ModuleGUI gui;

    private KmlDocument kmlDynamicRoot;

    private FolderType kmlFolder;

    private KmlDocument kmlStaticRoot;

    private String outputFolder; // TODO make property and settable in GUI

    private boolean saveBackupOnFileExists = false; // TODO make property and settable in GUI

    private Map<String, Map<String, SimpleArrayDataType>> sensorData;

    private HashMap<String, KmlDocument> sensorDocuments;

    private HashMap<String, TrackType> sensorTracks;

    private String statusString;

    public KmlOutputPlugin() {
        this(FTPUploadExtension.DEFAULT_FTP_WATCH_FOLDER, DEFAULT_KML_FILE_PREFIX);
    }

    public KmlOutputPlugin(String folder, String filePrefix) {
        this.outputFolder = folder;
        this.filePrefix = filePrefix;

        log.info("NEW " + this);
    }

    private void addKmlAndGxSchemaLocation(XmlObject xml) {
        XmlCursor c = xml.newCursor();
        c.setAttributeText(SCHEMA_LOCATION_ATTRIBUTE_QNAME,
                           KmlConstants.getSchemaLocation() + " " + GxConstants.getSchemaLocation());
        c.dispose();
    }

    private void copyFile(String source, String destination) {
        try {
            File in = new File(source);
            File out = new File(destination);
            
            log.debug("Copying file " + in.getAbsolutePath() + " to " + out.getAbsolutePath());

            InputStream is = new FileInputStream(in);

            OutputStream os = new FileOutputStream(out);
            byte[] buf = new byte[1024];
            int len;
            while ( (len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            is.close();
            os.close();

            log.info("Copied file " + source + " to " + destination);
        }
        catch (Exception e) {
            log.error("Could not copy file " + source + " >>> " + destination, e);
        }
    }

    private String getDataSchemaIdentifier(String id) {
        return "schema_" + XmlUtils.cleanIdentifier(id);
    }

    private String getFilename(String id, String extension) {
        // remove illegal characters
        String cleanId = FileUtils.cleanFileName(id);

        StringBuilder sb = new StringBuilder();
        sb.append(this.filePrefix);
        if ( !id.isEmpty()) {
            sb.append("_");
            sb.append(cleanId);
        }
        sb.append(".");
        sb.append(extension);

        return sb.toString();
    }

    private String getFilepath(String filename) {
        return this.outputFolder + "/" + filename;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#getName()
     */
    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#getStatus()
     */
    @Override
    public int getStatus() {
        return IModule.STATUS_RUNNING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#getStatusString()
     */
    @Override
    public String getStatusString() {
        return this.statusString;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#getUserInterface()
     */
    @Override
    public ModuleGUI getUserInterface() {
        // TODO create GUI
        return null;
    }

    private void handleNewSensor(String id, Double lat, Double lon, Double alt, Map<String, Object> data) {
        try {
            /*
             * create new file for the sensor
             */
            String kmlFileName = getFilename(id, KmlConstants.KML_FILE_EXTENSION);
            log.info("Creating KML document " + kmlFileName);
            KmlDocument kmlDoc = KmlDocument.Factory.newInstance();
            KmlType kml = kmlDoc.addNewKml();
            this.sensorDocuments.put(id, kmlDoc);

            // <Style id="golf-balloon-style"> <BalloonStyle> <text> <![CDATA[ This is $[name] This is hole
            // $[holeNumber] The par for this hole is $[holePar] The yardage is $[holeYardage] ]]> </text>
            // </BalloonStyle> </Style>

            /*
             * add document
             */
            AbstractFeatureType abstractFeatureGroup = kml.addNewAbstractFeatureGroup();
            DocumentType document = (DocumentType) abstractFeatureGroup.substitute(new QName(KmlConstants.KML_NAMESPACE_URI,
                                                                                             "Document"),
                                                                                   DocumentType.type);
            String cleanId = XmlUtils.cleanIdentifier(id);
            document.setDescription("KML Visualisation of sensor " + id + " using cleaned identifier " + cleanId);

            document.setId("doc_" + cleanId);
            document.setName("Sensor " + id);
            document.setVisibility(true);

            /*
             * style
             */
            /*
             * TODO add styling and make editable in GUI: thicker lines, more sensor-like icon
             */
            AbstractStyleSelectorType abstractStyleSelectorGroup = document.addNewAbstractStyleSelectorGroup();
            StyleType style = (StyleType) abstractStyleSelectorGroup.substitute(new QName(KmlConstants.KML_NAMESPACE_URI,
                                                                                          "Style"),
                                                                                StyleType.type);
            style.setId(KmlStyle.MD_ICON_STYLE);
            IconStyleType iconType = style.addNewIconStyle();
            BasicLinkType icon = iconType.addNewIcon();
            icon.setHref("md-png.png");

            /*
             * set a default view
             */
            AbstractViewType abstractViewGroup = document.addNewAbstractViewGroup();
            LookAtType lookAt = (LookAtType) abstractViewGroup.substitute(new QName(KmlConstants.KML_NAMESPACE_URI,
                                                                                    "LookAt"), LookAtType.type);
            lookAt.setLatitude(lat.doubleValue());
            lookAt.setLongitude(lon.doubleValue());
            lookAt.setAltitude(alt.doubleValue());

            /*
             * add schema for extended data, see
             * https://developers.google.com/kml/documentation/kmlreference#trackexample
             */
            SchemaType schema = document.addNewSchema();
            schema.setName("DataSchema");
            schema.setId("DataSchemaId"); // schema.setId(getDataSchemaIdentifier(id));

            Set<Entry<String, Object>> dataEntries = data.entrySet();
            for (Entry<String, Object> entry : dataEntries) {
                XmlObject schemaExtension = schema.addNewSchemaExtension();
                SimpleArrayFieldType simpleArrayFieldType = (SimpleArrayFieldType) schemaExtension.substitute(new QName(GxConstants.GX_NAMESPACE_URI,
                                                                                                                        "SimpleArrayField"),
                                                                                                              SimpleArrayFieldType.type);
                simpleArrayFieldType.setName(entry.getKey().toLowerCase());
                simpleArrayFieldType.setType("string");
                simpleArrayFieldType.setDisplayName(entry.getKey());
            }

            /*
             * add placemark holding a track
             */
            abstractFeatureGroup = document.addNewAbstractFeatureGroup();
            PlacemarkType placemark = (PlacemarkType) abstractFeatureGroup.substitute(new QName(KmlConstants.KML_NAMESPACE_URI,
                                                                                                "Placemark"),
                                                                                      PlacemarkType.type);
            AbstractGeometryType abstractGeometryGroup = placemark.addNewAbstractGeometryGroup();
            TrackType track = (TrackType) abstractGeometryGroup.substitute(new QName(GxConstants.GX_NAMESPACE_URI,
                                                                                     "Track"), TrackType.type);
            // style
            placemark.setStyleUrl("#" + KmlStyle.MD_ICON_STYLE);

            // altitudeMode
            XmlCursor cur = track.newCursor();
            cur.toNextToken();
            cur.insertElementWithText(new QName(KmlConstants.KML_NAMESPACE_URI, "altitudeMode", "kml"),
                                      "relativeToGround");
            cur.dispose();

            this.sensorTracks.put(id, track);

            /*
             * add SimpleArrayData for storing the values
             */
            this.sensorData.put(id, new HashMap<String, SimpleArrayDataType>());
            ExtendedDataType extendedData = track.addNewExtendedData();
            SchemaDataType schemaData = extendedData.addNewSchemaData();
            schemaData.setSchemaUrl("DataSchemaId"); // schemaData.setSchemaUrl("#" +
            // getDataSchemaIdentifier(id));
            for (Entry<String, Object> entry : dataEntries) {
                XmlObject schemaDataExtension = schemaData.addNewSchemaDataExtension();
                SimpleArrayDataType simpleArrayDataType = (SimpleArrayDataType) schemaDataExtension.substitute(new QName(GxConstants.GX_NAMESPACE_URI,
                                                                                                                         "SimpleArrayData"),
                                                                                                               SimpleArrayDataType.type);
                simpleArrayDataType.setName(entry.getKey());
                // no values yet, but save for later reference
                this.sensorData.get(id).put(entry.getKey(), simpleArrayDataType);
                log.debug("Added SimpleArrayData for " + entry.getKey() + " to SchemaData " + schemaData.getSchemaUrl());
            }

            // save file for the sensor
            addKmlAndGxSchemaLocation(kml);
            saveDocumentInFile(kmlDoc, getFilepath(kmlFileName));

            /*
             * add new sensor file to dynamic file
             */
            abstractFeatureGroup = this.kmlFolder.addNewAbstractFeatureGroup();
            NetworkLinkType rootToDynamicNetworkLink = (NetworkLinkType) abstractFeatureGroup.substitute(new QName(KmlConstants.KML_NAMESPACE_URI,
                                                                                                                   "NetworkLink"),
                                                                                                         NetworkLinkType.type);
            rootToDynamicNetworkLink.setName("Link to " + id);
            rootToDynamicNetworkLink.setId("link_" + id);
            LinkType rootToDynamicLink = rootToDynamicNetworkLink.addNewLink2();

            rootToDynamicLink.setHref(kmlFileName);
            rootToDynamicLink.setId("link_" + id);
            rootToDynamicLink.setRefreshMode(RefreshModeEnumType.ON_INTERVAL);
            rootToDynamicLink.setRefreshInterval(DEFAULT_REFRESH_INTERVAL);
            rootToDynamicLink.setViewRefreshMode(ViewRefreshModeEnumType.ON_STOP);
            rootToDynamicLink.setViewRefreshTime(DEFAULT_REFRESH_INTERVAL);
        }
        catch (Exception e) {
            log.error(e);
            return;
        }

        // save file with sensor list
        saveDocumentInFile(this.kmlDynamicRoot,
                           getFilepath(getFilename(DYNAMIC_ROOT_FILENAME, KmlConstants.KML_FILE_EXTENSION)));
    }

    /**
     * Using data representation with gx:Track,
     * https://developers.google.com/kml/documentation/kmlreference#gxtrack
     * 
     * specific elements can also be updated, see https://developers.google.com/kml/documentation/updates
     * TODO: consider using update mechanism
     * 
     * @param id
     * @param dataToSave
     * @param cal
     * @param alt
     * @param lon
     * @param lat
     */
    private void handleUpdateSensor(String id,
                                    Double lat,
                                    Double lon,
                                    Double alt,
                                    Calendar cal,
                                    Map<String, Object> data) {
        KmlDocument kmlDoc = this.sensorDocuments.get(id);
        TrackType track = this.sensorTracks.get(id);

        try {
            // XmlObject altitudeModeGroup = track.addNewAltitudeModeGroup();
            //
            // AltitudeModeEnumType altitudeMode = (AltitudeModeEnumType) altitudeModeGroup.substitute(new
            // QName(KML_NAMESPACE_URI,
            // "AltitudeModeGroup"),
            // AltitudeModeEnumType.type);
            // altitudeMode.set(AltitudeModeEnumType.ABSOLUTE); // TODO make altitude mode settable in GUI!

            /*
             * add time and coordinates
             */
            track.addWhen(cal);
            track.addCoord(lon.toString() + ", " + lat.toString() + ", " + alt.toString());

            /*
             * add data, see https://developers.google.com/kml/documentation/kmlreference#extendeddata
             */
            Set<Entry<String, Object>> entrySet = data.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                SimpleArrayDataType simpleArrayDataType = this.sensorData.get(id).get(entry.getKey());
                simpleArrayDataType.addValue(entry.getValue().toString());
            }
        }
        catch (Exception e) {
            log.error(e);
            return;
        }

        saveDocumentInFile(kmlDoc, getFilepath(getFilename(id, KmlConstants.KML_FILE_EXTENSION)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#init()
     */
    @Override
    public void init() throws Exception {
        File outF = new File(this.outputFolder);
        if ( !outF.exists()) {
            outF.mkdir();
            if (log.isDebugEnabled())
                log.debug("Created output folder " + outF.getAbsolutePath());
        }

        log.info("Using output folder " + outF.getAbsolutePath());

        initKmlDocuments();
        initHtmlDocument();
        initRequiredResources();

        this.gui = new ModuleGUI();
        this.gui.setMenu(makeMenu());
        this.gui.setGui(makeGUI());

        this.sensorDocuments = new HashMap<String, KmlDocument>();
        this.sensorTracks = new HashMap<String, TrackType>();
        this.sensorData = new HashMap<String, Map<String, SimpleArrayDataType>>();

        updateGUI();
        zipAllFiles();

        this.statusString = "Initalized | " + this.outputFolder;
    }

    // TODO create a button in the GUI that allows to "recreate" the HTML file
    private void initHtmlDocument() {
        if ( !this.createHTML)
            return;

        // load file
        StringBuilder contents = new StringBuilder();
        URL resource = getClass().getResource(HTML_TEMPLATE_FILE);
        if (resource == null) {
            log.error("Could not find template file resource " + HTML_TEMPLATE_FILE);
            return;
        }

        File f = new File(resource.getFile());
        FileReader fr;
        try {
            fr = new FileReader(f);
        }
        catch (FileNotFoundException e) {
            log.error("Could not read file " + resource);
            return;
        }

        BufferedReader br = new BufferedReader(fr);
        try {
            String line = null;
            while ( (line = br.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        }
        catch (IOException e) {
            log.error(e);
        }
        finally {
            try {
                br.close();
            }
            catch (IOException e) {
                log.error(e);
            }
        }

        // replace name of kml file
        String html = contents.toString();
        String rootKml = getFilename(STATIC_ROOT_FILENAME, KmlConstants.KML_FILE_EXTENSION);

        // ideas: static method > not so nice
        // rootKml = FTPUploadExtension.getFullUrl(rootKml); // FIXME
        // maybe with a singleton?
        // FTPUploadExtension.getInstance().getFullUrl(rootKml);

        // TODO remove hack for demo
        rootKml = "http://v-tml.uni-muenster.de/" + rootKml;

        html = html.replace(HTML_KML_REFERENCE_TAG, rootKml);

        // save as new file
        String htmlFileName = getFilename(HTML_FILENAME, HTML_FILE_EXTENSION);
        String newFile = getFilepath(htmlFileName);
        log.debug("Creating HTML file " + newFile + " referencing " + rootKml);

        Writer output = null;
        try {
            output = new BufferedWriter(new FileWriter(newFile));
            output.write(html);
        }
        catch (IOException e) {
            log.error(e);
            return;
        }
        finally {
            if (output != null)
                try {
                    output.close();
                }
                catch (IOException e) {
                    log.error(e);
                }
        }
    }

    private void initKmlDocuments() {
        /*
         * static root document
         */
        String kmlStaticRootFileName = getFilename(STATIC_ROOT_FILENAME, KmlConstants.KML_FILE_EXTENSION);
        log.info("Creating KML document " + kmlStaticRootFileName);
        this.kmlStaticRoot = KmlDocument.Factory.newInstance();
        KmlType kml = this.kmlStaticRoot.addNewKml();

        AbstractFeatureType abstractFeatureGroup = kml.addNewAbstractFeatureGroup();
        FolderType staticFolder = (FolderType) abstractFeatureGroup.substitute(new QName(KmlConstants.KML_NAMESPACE_URI,
                                                                                         "Folder"),
                                                                               FolderType.type);
        staticFolder.setName("Sensor Platform Framework Live Sensor Data");
        staticFolder.setVisibility(true);
        staticFolder.setOpen(true);
        staticFolder.setDescription("52°North Sensor Platform Framework for Live Sensor Data.");

        abstractFeatureGroup = staticFolder.addNewAbstractFeatureGroup();
        NetworkLinkType rootToDynamicNetworkLink = (NetworkLinkType) abstractFeatureGroup.substitute(new QName(KmlConstants.KML_NAMESPACE_URI,
                                                                                                               "NetworkLink"),
                                                                                                     NetworkLinkType.type);
        rootToDynamicNetworkLink.setName("SPF dynamic root");
        LinkType rootToDynamicLink = rootToDynamicNetworkLink.addNewLink2();

        // TODO check out using NetworkLinkControl, might be more powerful:
        // https://developers.google.com/kml/documentation/kmlreference#networklinkcontrol
        String kmlDynamicRootFileName = getFilename(DYNAMIC_ROOT_FILENAME, KmlConstants.KML_FILE_EXTENSION);
        rootToDynamicLink.setHref(kmlDynamicRootFileName);
        rootToDynamicLink.setRefreshMode(RefreshModeEnumType.ON_CHANGE);
        rootToDynamicLink.setRefreshInterval(1.0d);
        // rootToDynamicLink.setViewRefreshTime(1.0d);

        addKmlAndGxSchemaLocation(kml);
        saveDocumentInFile(this.kmlStaticRoot, getFilepath(kmlStaticRootFileName));

        /*
         * dynamic root document
         */
        log.info("Creating KML document " + kmlDynamicRootFileName);
        this.kmlDynamicRoot = KmlDocument.Factory.newInstance();
        kml = this.kmlDynamicRoot.addNewKml();

        abstractFeatureGroup = kml.addNewAbstractFeatureGroup();
        this.kmlFolder = (FolderType) abstractFeatureGroup.substitute(new QName(KmlConstants.KML_NAMESPACE_URI,
                                                                                "Folder"), FolderType.type);
        // into this folder, the network links for each sensor will be added
        this.kmlFolder.setName("SPF Sensors");
        this.kmlFolder.setVisibility(true);
        this.kmlFolder.setOpen(true);
        this.kmlFolder.setDescription("This folder holds the dynamic list of sensors updated by SPF.");

        addKmlAndGxSchemaLocation(kml);
        saveDocumentInFile(this.kmlDynamicRoot, getFilepath(kmlDynamicRootFileName));
    }

    private void initRequiredResources() {
        // FIXME move file list to config file
        this.copiedResources.add("/md-png.png");

        for (String s : this.copiedResources) {
            URL resource = getClass().getResource(s);
            if (resource == null) {
                log.error("Could not find file resource " + s);
                continue;
            }

            String newFile = getFilepath(s);

            copyFile(resource.getFile(), newFile);
        }
    }

    private JPanel makeGUI() {
        return null;
    }

    private JMenu makeMenu() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#processData(java.util.Map,
     * org.n52.ifgicopter.spf.xml.Plugin)
     */
    @Override
    public int processData(Map<Long, Map<String, Object>> data, Plugin plugin) {
        for (Entry<Long, Map<String, Object>> entry : data.entrySet()) {
            int i = processSingleData(entry.getValue(), entry.getKey(), plugin);
            if (i != IModule.STATUS_RUNNING)
                log.warn("Processing of data caused error: " + entry);
        }

        return IModule.STATUS_RUNNING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#processSingleData(java.util.Map, java.lang.Long,
     * org.n52.ifgicopter.spf.xml.Plugin)
     */
    @Override
    public int processSingleData(Map<String, Object> data, Long timestamp, Plugin plugin) {
        if (log.isDebugEnabled())
            log.debug("New data for KML output: " + data + " @ " + timestamp);

        String pluginName = plugin.getName();
        Location location = plugin.getLocation();
        Double lat = (Double) data.get(location.getFirstCoordinateName());
        Double lon = (Double) data.get(location.getSecondCoordinateName());
        Double alt = (Double) data.get(location.getAltitudeName());

        /*
         * is new sensor?
         */
        if ( !this.sensorDocuments.containsKey(pluginName)) {
            // no: create new kml document
            handleNewSensor(pluginName, lat, lon, alt, data);
        }

        Date d = new Date(timestamp.longValue());
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);

        /*
         * update KML document
         */
        handleUpdateSensor(pluginName, lat, lon, alt, cal, data);

        updateGUI();
        return IModule.STATUS_RUNNING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.output.IOutputPlugin#restart()
     */
    @Override
    public void restart() throws Exception {
        log.info("RESTART...");
        initKmlDocuments();

        // TODO leave the sensor docuements untouched??
    }

    private void saveDocumentInFile(KmlDocument document, String path) {
        if (log.isDebugEnabled())
            log.debug("Saving " + document.xmlText(KML_OPTIONS) + " in " + path);

        File f = new File(path);

        if (f.exists()) {
            log.warn("Overwriting file " + f.getAbsolutePath());
            if (this.saveBackupOnFileExists) {
                File dest = new File(path + "_backup" + UUID.randomUUID());
                log.debug("Saving backup in " + dest);
                f.renameTo(dest);
            }
        }

        try {
            boolean valid = document.validate();
            if (log.isDebugEnabled()) {
                log.debug("KML Document is valid=" + valid);
                if ( !valid) {
                    String errors = XmlUtils.validateAndIterateErrors(document);
                    log.warn(errors);
                }
            }

            document.save(f, KML_OPTIONS);
        }
        catch (IOException e) {
            log.error("Could not save KML file " + f.getAbsolutePath(), e);
        }
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
        if (log.isDebugEnabled())
            log.debug("New output folder: " + this.outputFolder);

        updateGUI();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.n52.ifgicopter.spf.common.IModule#shutdown()
     */
    @Override
    public void shutdown() throws Exception {
        zipAllFiles();

        this.kmlFolder = null;
        this.kmlStaticRoot = null;

        // TODO null all kml documents
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KmlOutputPlugin [output folder = ");
        sb.append(this.outputFolder);
        sb.append(", file prefix = ");
        sb.append(this.filePrefix);
        sb.append(", files = ");
        // sb.append(Arrays.toString(this.files)); // TODO add a list of all files currently used
        sb.append("]");
        return sb.toString();
    }

    private void updateGUI() {
        //
    }

    /**
     * http://java.sun.com/developer/technicalArticles/Programming/compression/
     * 
     * TODO see if zipping solves referencing problem:
     * https://developers.google.com/kml/documentation/kmzarchives
     */
    private void zipAllFiles() {
        if ( !this.createKMZ) {
            return;
        }

        // saveDocumentInFile(this.kmlStaticRoot, this.outputPath + this.kmlStaticRootFileName);

        int buffer = 2048;
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(this.outputFolder + File.separator + this.filePrefix + "."
                    + KmlConstants.KMZ_FILE_EXTENSION);
            if (log.isDebugEnabled())
                log.debug("Saving KML files to zip file " + dest);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            out.setMethod(ZipOutputStream.DEFLATED);

            byte data[] = new byte[buffer];

            File f = new File(this.outputFolder);
            File files[] = f.listFiles(onlyKmlFiles);

            for (int i = 0; i < files.length; i++) {
                if (log.isDebugEnabled())
                    log.debug("Zipping " + files[i]);

                FileInputStream fi = new FileInputStream(files[i].getAbsolutePath());
                origin = new BufferedInputStream(fi, buffer);
                ZipEntry entry = new ZipEntry(files[i].getName());
                out.putNextEntry(entry);
                int count;
                while ( (count = origin.read(data, 0, buffer)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            out.close();
        }
        catch (Exception e) {
            log.error(e);
        }
    }

}
