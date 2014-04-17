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

package org.n52.ifgicopter.spf.xml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.gml.BoundingShapeType;
import net.opengis.gml.DirectPositionType;
import net.opengis.gml.EnvelopeType;
import net.opengis.sensorML.x101.AbstractProcessType;
import net.opengis.sensorML.x101.IoComponentPropertyType;
import net.opengis.sensorML.x101.ResponsiblePartyDocument.ResponsibleParty;
import net.opengis.sensorML.x101.SensorMLDocument.SensorML;
import net.opengis.sensorML.x101.SensorMLDocument.SensorML.Member;
import net.opengis.sensorML.x101.TermDocument.Term;
import net.opengis.sensorML.x101.SystemType;
import net.opengis.sensorML.x101.ContactDocument.Contact;
import net.opengis.sensorML.x101.ContactInfoDocument.ContactInfo;
import net.opengis.sensorML.x101.ContactInfoDocument.ContactInfo.Address;
import net.opengis.sensorML.x101.IdentificationDocument.Identification;
import net.opengis.sensorML.x101.IdentificationDocument.Identification.IdentifierList;
import net.opengis.sensorML.x101.IdentificationDocument.Identification.IdentifierList.Identifier;
import net.opengis.sensorML.x101.InputsDocument.Inputs.InputList;
import net.opengis.sensorML.x101.PositionDocument.Position;
import net.opengis.swe.x101.AbstractDataRecordType;
import net.opengis.swe.x101.PositionType;
import net.opengis.swe.x101.TimeDocument;
import net.opengis.swe.x101.UomPropertyType;
import net.opengis.swe.x101.VectorPropertyType;
import net.opengis.swe.x101.VectorType;
import net.opengis.swe.x101.BooleanDocument.Boolean;
import net.opengis.swe.x101.QuantityDocument.Quantity;
import net.opengis.swe.x101.TextDocument.Text;
import net.opengis.swe.x101.VectorType.Coordinate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.n52.ifgicopter.spf.SPFRegistry;
import org.n52.ifgicopter.spf.xml.CompoundItem;
import org.n52.ifgicopter.spf.xml.Item;
import org.n52.ifgicopter.spf.xml.Location;
import org.n52.ifgicopter.spf.xml.Plugin;
import org.n52.ifgicopter.spf.xml.PluginMetadata;
import org.n52.ifgicopter.spf.xml.Time;
import org.n52.ifgicopter.spf.xml.parser.SWECommonParser;

import de.uniMuenster.ifgi.mRiek02.spf.x01.AbstractBehaviourType;
import de.uniMuenster.ifgi.mRiek02.spf.x01.AvailabilityBehaviourType;
import de.uniMuenster.ifgi.mRiek02.spf.x01.PeriodBehaviourType;
import de.uniMuenster.ifgi.mRiek02.spf.x01.PluginDocument;
import de.uniMuenster.ifgi.mRiek02.spf.x01.PluginType;
import de.uniMuenster.ifgi.mRiek02.spf.x01.AvailabilityBehaviourType.OutputProperties;
import de.uniMuenster.ifgi.mRiek02.spf.x01.PluginType.Output;
import de.uniMuenster.ifgi.mRiek02.spf.x01.PluginType.Output.MandatoryProperties;

public class PluginXMLTools {
	
	private static final Log log = LogFactory.getLog(PluginXMLTools.class);
	
	public static Plugin parsePlugin(InputStream is) throws Exception {
		/*
		 * the final result get init
		 */
		Plugin result = new Plugin();
		
		List<String> outputProperties = new ArrayList<String>();
		List<String> mandatoryProperties = new ArrayList<String>();
		List<String> inputProperties = new ArrayList<String>();
		Map<String, Item> items = new HashMap<String, Item>();
		
		StringBuilder sb = new StringBuilder();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		while (br.ready()) {
			sb.append(br.readLine());
		}

		PluginDocument doc = null;
		try {
			doc = PluginDocument.Factory.parse(sb.toString());
		} catch (XmlException e) {
			throw e;
		}

		/*
		 * do we validate?
		 */
		if (java.lang.Boolean.parseBoolean(SPFRegistry.getInstance().getConfigProperty(SPFRegistry.VALIDATE_XML_PROP))) {
			XmlOptions opts = new XmlOptions();
			List<XmlError> errors = new ArrayList<XmlError>();
			opts.setErrorListener(errors);
			if (!doc.validate(opts)) {
				throw new Exception("Document is not a valid plugin description. Please recheck: "+errors);
			}
		}

		PluginType plugin = doc.getPlugin();

		String name = plugin.getName().trim();

		/*
		 * 
		 * PARSE the SensorML
		 * 
		 */

		SensorML sensorML = plugin.getSensorML();
		
		PluginMetadata metadata = new PluginMetadata();
		metadata.setName(name);

		/*
		 * do schematron XSLT transformations
		 */
		SchematronValidator schematron = new SchematronValidator(sb);
		if (!schematron.validate()) {
			throw new Exception("The plugin description file is not a valid SPF-schematron SensorML profile instance: "
					+ schematron.getAssertFails());
		}
		/*
		 * get the input items
		 */
		boolean mobile = true;
		Location location = null;
		Time time = null;
		
		AbstractProcessType process = sensorML.getMemberArray(0).getProcess();
		if (process instanceof SystemType) {
			/*
			 * parse the system
			 */
			SystemType system = (SystemType) process;

			/*
			 * parse contact and identification
			 */
			Contact[] cont = system.getContactArray();
			if (cont.length > 0) {
				if (cont[0].isSetResponsibleParty() && cont[0].getResponsibleParty().isSetContactInfo() &&
						cont[0].getResponsibleParty().getContactInfo().isSetAddress() &&
						cont[0].getResponsibleParty().getContactInfo().getAddress().isSetElectronicMailAddress()) {
					metadata.setContactEmail(cont[0].getResponsibleParty()
							.getContactInfo().getAddress().getElectronicMailAddress().trim());
				}
			}

			Identification[] ident = system.getIdentificationArray();
			if (ident.length > 0) {

				if (ident[0].isSetIdentifierList()) {
					for (int j = 0; j < ident[0].getIdentifierList().getIdentifierArray().length; j++) {
						if (ident[0].getIdentifierList().getIdentifierArray()[j].isSetName() &&
								ident[0].getIdentifierList().getIdentifierArray()[j].getName().equals("uniqueID")) {
							metadata.setUniqueID(
									ident[0].getIdentifierList().getIdentifierArray()[j].getTerm().getValue().trim());
						}
					}
				}

			}

			
			/*
			 * check if we have a mobile or a stationary platform
			 */
			if (system.isSetBoundedBy()) {
				/*
				 * this indicates a mobile sensor
				 * -> a swe:Position should be available
				 * in the sml:inputs
				 */
				BoundingShapeType boundedBy = system.getBoundedBy();
				if (boundedBy.isSetEnvelope()) {
					EnvelopeType envelope = boundedBy.getEnvelope();

					if ((envelope.isSetLowerCorner() && envelope.isSetUpperCorner()) ||
							envelope.getPosArray().length == 2) {
						/*
						 * valid envelope defintion
						 */
						mobile = true;
						metadata.setMobile(true);
						metadata.setPosition(envelope.getLowerCorner().getStringValue().trim());
					}
				}

			}

			else if (system.isSetPosition()) {
				/*
				 * if we have a static platform use the sml:location element
				 * -> stationary
				 */
				location = new Location("staticPosition");

				Position position = system.getPosition();
				VectorPropertyType loc = position.getPosition().getLocation();
				if (loc.isSetVector()) {
					VectorType vector = loc.getVector();
					location.setReferenceFrame(position.getPosition().getReferenceFrame());

					/*
					 * get the axis order dependent on
					 * the CRS.
					 * the coordinate array will then be treated
					 * according to this axis order.
					 */
					String[] axisOrder = getAxisOrder(location.getReferenceFrame());

					int axis = 0;
					for (Coordinate coord : vector.getCoordinateArray()) {
						if (coord.isSetQuantity()) {
							location.setAxis(axisOrder[axis], coord.getQuantity().getValue());
						}
						else if (coord.isSetCount()) {
							location.setAxis(axisOrder[axis], coord.getCount().getValue().doubleValue());
						}

						location.setDimension(++axis);
					}

					mobile = false;
					metadata.setPosition(location.getX() + " "+ location.getY());
					metadata.setMobile(false);
				}

			}


			InputList inputs = system.getInputs().getInputList();


			if (inputs != null) {
				for (IoComponentPropertyType input : inputs.getInputArray()) {
					/*
					 * parse the time
					 */
					if (input.isSetTime()) {
						TimeDocument.Time prop = input.getTime();
						time = new Time(input.getName());
						time.setDefinition(SWECommonParser.getTimeDefinition(prop));
						time.setReferenceFrame(prop.getReferenceFrame());

						items.put(time.getProperty(), time);
					}
					else if (input.isSetAbstractDataRecord()) {
						AbstractDataRecordType prop = input.getAbstractDataRecord();

						/*
						 * is this the position?
						 */
						if (prop instanceof PositionType) {
							PositionType pos = (PositionType) prop;
							pos.getReferenceFrame();

							/*
							 * use a temporary object because perhaps
							 * we have a stationary platform defined
							 * and are not allowed to overwrite the
							 * field "location"
							 */
							Location tmp = new Location(input.getName());

							/*
							 * use the reference frame attribute
							 */
							tmp.setReferenceFrame(pos.getReferenceFrame());
							items.put(tmp.getProperty(), tmp);

							VectorPropertyType loc = pos.getLocation();
							Coordinate[] coords = loc.getVector().getCoordinateArray();

							/*
							 * go through all coordinates
							 * treat axis ordering according to their order
							 */
							for (int i = 0; i < coords.length; i++) {
								if (i == 0) {
									/*
									 * first axis
									 */
									Item first = new Item(coords[i].getName());
									tmp.addCompoundedItem(first);
									tmp.setFirstCoordinateName(first.getProperty());

									if (coords[i].isSetQuantity() || coords[i].isSetCount()) {
										first.setDataType(Double.class);
										if (coords[i].isSetQuantity()) {
											Quantity quan = coords[i].getQuantity();
											if (quan.isSetUom()) {
												first.setUom(quan.getUom().getCode());
											}
										}
									}
									else {
										first.setDataType(String.class);
									}

								}
								else if (i == 1) {
									/*
									 * second axis
									 */
									Item second = new Item(coords[i].getName());
									tmp.addCompoundedItem(second);
									tmp.setSecondCoordinateName(second.getProperty());

									if (coords[i].isSetQuantity() || coords[i].isSetCount()) {
										second.setDataType(Double.class);
										if (coords[i].isSetQuantity()) {
											Quantity quan = coords[i].getQuantity();
											if (quan.isSetUom()) {
												second.setUom(quan.getUom().getCode());
											}
										}
									}
									else {
										second.setDataType(String.class);
									}

								}
								else {
									/*
									 * third axis
									 */
									Item third = new Item(coords[i].getName());
									tmp.addCompoundedItem(third);
									tmp.setAltitudeName(third.getProperty());

									if (coords[i].isSetQuantity() || coords[i].isSetCount()) {
										third.setDataType(Double.class);
										if (coords[i].isSetQuantity()) {
											Quantity quan = coords[i].getQuantity();
											if (quan.isSetUom()) {
												third.setUom(quan.getUom().getCode());
											}
										}
									}
									else {
										third.setDataType(String.class);
									}
								}

							}

							if (!mobile) {
								log.warn("The SensorML indicated a stationary platform (sml:position was specified). The input " +
										"property '"+ input.getName() + "' will not be treated as the dynamic position " +
								"of the platform but as a normal input property.");
							}
							else {
								/*
								 * we are allowed to use this object
								 * as location because the platform
								 * is defined mobile
								 */
								location = tmp;
							}
						}
						else {
							//TODO: do other data records
						}

					}

					/*
					 * parse other values, probably phenomenons
					 */
					else if (input.isSetQuantity() || input.isSetCount()) {
						/*
						 * real numbers
						 */
						Item item = new Item(input.getName());
						item.setDataType(Double.class);

						if (input.isSetQuantity()) {
							Quantity quan = input.getQuantity();
							if (quan.isSetUom()) {
								item.setUom(quan.getUom().getCode());
							}
							if (quan.isSetDefinition()) {
								item.setDefinition(quan.getDefinition());
							}
						}
						items.put(item.getProperty(), item);
					}

					if (input.isSetText() || input.isSetBoolean() || input.isSetCategory()) {
						/*
						 * treat this as string
						 */
						Item item = new Item(input.getName());
						item.setDataType(String.class);
						items.put(item.getProperty(), item);
					}
				}
			}
			
			/*
			 * check if a time is present, otherwise set default time property
			 */
			if (time == null) {
				/*
				 * define default time property
				 */
				time = new Time(Plugin.TIME_DEFAULT_NAME);
				time.setReferenceFrame("urn:ogc:def:unit:iso8601");
				time.setDefinition("urn:ogc:def:phenomenon:time");
				items.put(time.getProperty(), time);
			}
		}

		/*
		 * 
		 * PARSE the outputType
		 * 
		 */
		Output outputElem = plugin.getOutput();
		AbstractBehaviourType behave = outputElem.getBehaviour();
		String outputType = "";
		if (behave instanceof AvailabilityBehaviourType) {
			outputType = Plugin.AVAILABLE_BEHAVIOUR;
			AvailabilityBehaviourType avail = (AvailabilityBehaviourType) behave;

			for (String prop : avail.getOutputProperties().getPropertyArray()) {
				Item item = items.get(prop);
				if (item instanceof CompoundItem) {
					addLeafProperties((CompoundItem) item, outputProperties);
				}
				else {
					outputProperties.add(prop);
				}
			}
		}
		else if (behave instanceof PeriodBehaviourType) {
			outputType = Plugin.PERIOD_BEHAVIOUR;
			result.setTimeDelta(((PeriodBehaviourType) behave).getTimedelta());
		}


		/*
		 * output if and only if all items are present
		 */

		/*
		 * these properties are needed for a data tuple
		 */
		if (outputElem.isSetMandatoryProperties()) {
			//get from the mandatory list
			for (String prop : outputElem.getMandatoryProperties().getPropertyArray()) {
				Item item = items.get(prop);
				if (item instanceof CompoundItem) {
					addLeafProperties((CompoundItem) item, mandatoryProperties);
				}
				else {
					mandatoryProperties.add(prop);
				}
			}
		}
		else if (outputElem.isSetOutputOnAllItems()) {
			// all items are needed
			for (Item item : items.values()) {
				if (item instanceof CompoundItem) {
					addLeafProperties((CompoundItem) item, mandatoryProperties);
				}
				else if (item instanceof Time) {
					//do not add this as the time property is the key
					//for maps and hence not hold separatly
				}
				else {
					mandatoryProperties.add(item.getProperty());
				}
			}
		}
		else if (outputElem.isSetSingleOutputAllowed()) {
			/*
			 * nothing. leave mandatoryProperties empty
			 */
		}

		/*
		 * finally save all leaf properties (a CompoundField never comes in as a
		 * property of its name. get its leafs as the input properties ->
		 * SimpleFields)
		 */
		for (Item item : items.values()) {
			if (item instanceof CompoundItem) {
				addLeafProperties((CompoundItem) item, inputProperties);
			}
			else {
				inputProperties.add(item.getProperty());
			}
		}
		
		result.setLocation(location);
		result.setTime(time);
		result.setMobile(mobile);
		result.setMetadata(metadata);
		result.setName(name);
		result.setSensorML(sensorML);
		result.setInputProperties(inputProperties);
		result.setOutputProperties(outputProperties);
		result.setMandatoryProperties(mandatoryProperties);
		result.setItems(items);
		result.setOutputType(outputType);
		
		return result;
	}
	
	/**
	 * helper method to get CRS specific axis ordering.
	 * this can be understood as some kind of catalog.
	 * 
	 * @param referenceFrame the CRS
	 * @return 3-element String array containg "x", "y" and "z"
	 */
	private static String[] getAxisOrder(String referenceFrame) {
		if (referenceFrame.equals("urn:ogc:def:crs:EPSG::4326")) {
			return new String[] {"y", "x", "z"};
		}

		return new String[] {"x", "y", "z"};
	}
	
	/**
	 * adds all leaf properties of a {@link CompoundItem} (e.g., 
	 * {@link Location}) to a collection.
	 * 
	 * @param item the {@link CompoundItem}
	 * @param coll the collection
	 */
	private static void addLeafProperties(CompoundItem item, Collection<String> coll) {
		for (Item it : item.getCompoundItems()) {
			if (it instanceof CompoundItem) {
				addLeafProperties((CompoundItem) it, coll);
			}
			else {
				coll.add(it.getProperty());
			}
		}
	}
	
	public static void updateSensorML(Plugin plug) {
		AbstractProcessType prcs = plug.getSensorML().getMemberArray(0).getProcess();

		if (prcs instanceof SystemType) {
			SystemType system = (SystemType) prcs;

			/*
			 * metadata
			 */
			if (plug.getMetadata() != null) {

				/*
				 * contact
				 */
				if (system.getContactArray().length > 0) {
					for (int i = 0; i < system.getContactArray().length; i++) {
						system.removeContact(0);
					}
				}
				Contact contact = system.addNewContact();
				ResponsibleParty party = contact.addNewResponsibleParty();
				ContactInfo info = party.addNewContactInfo();
				Address addr = info.addNewAddress();
				addr.setElectronicMailAddress(plug.getMetadata().getContactEmail());

				/*
				 * identification
				 */
				if (system.getIdentificationArray().length > 0) {
					for (int i = 0; i < system.getIdentificationArray().length; i++) {
						system.removeIdentification(0);
					}
				}
				Identification ident = system.addNewIdentification();
				IdentifierList list = ident.addNewIdentifierList();
				Identifier id = list.addNewIdentifier();
				id.setName("uniqueID");
				Term term = id.addNewTerm();
				term.setDefinition("urn:ogc:def:identifier:OGC:uniqueID");
				term.setValue(plug.getMetadata().getUniqueID());

				if (plug.getMetadata().isMobile()) {
					/*
					 * generate a boundedBy element
					 */
					BoundingShapeType bby = null;
					if (system.isSetBoundedBy()) {
						bby = system.getBoundedBy();
					}
					else {
						bby = system.addNewBoundedBy();
					}

					EnvelopeType enve = EnvelopeType.Factory.newInstance();
					enve.setSrsName("urn:ogc:def:crs:EPSG::4326");
					DirectPositionType low = enve.addNewLowerCorner();
					DirectPositionType hi = enve.addNewUpperCorner();
					String[] pos = plug.getMetadata().getPosition().split(" ");
					if (pos.length == 2) {
						double lowLat = Double.parseDouble(pos[0]) - 1.0;
						double lowLon = Double.parseDouble(pos[1]) - 1.0;
						double hiLat = Double.parseDouble(pos[0]) + 1.0;
						double hiLon = Double.parseDouble(pos[1]) + 1.0;

						low.setStringValue(lowLat +" "+ lowLon);
						hi.setStringValue(hiLat +" "+ hiLon);
					} else {
						low.setStringValue(plug.getMetadata().getPosition());
						hi.setStringValue(plug.getMetadata().getPosition());
					}

					bby.setEnvelope(enve);
				}
				else {
					/*
					 * generate a static position
					 */
					Position posi = null;
					if (system.isSetPosition()) {
						posi = system.getPosition();
					}
					else {
						posi = system.addNewPosition();	
					}

					posi.setName("position");
					PositionType postype = PositionType.Factory.newInstance();
					postype.setReferenceFrame("urn:ogc:def:crs:EPSG::4326");
					VectorPropertyType loc = postype.addNewLocation();
					VectorType vec = loc.addNewVector();

					String[] pos = plug.getMetadata().getPosition().split(" ");
					if (pos.length == 2) {
						/*
						 * latitude
						 */
						Coordinate coord = vec.addNewCoordinate();
						coord.setName("latitude");
						Quantity quan = coord.addNewQuantity();
						UomPropertyType uom = quan.addNewUom();
						uom.setCode("deg");
						quan.setValue(Double.parseDouble(pos[0]));

						/*
						 * longitude
						 */
						Coordinate coord2 = vec.addNewCoordinate();
						coord2.setName("longitude");
						Quantity quan2 = coord2.addNewQuantity();
						UomPropertyType uom2 = quan2.addNewUom();
						uom2.setCode("deg");
						quan2.setValue(Double.parseDouble(pos[1]));
					}

					posi.setPosition(postype);
				}
			}

			/*
			 * generate output xml of inputlist
			 */
			InputList inputs = system.getInputs().getInputList();
			
			if (inputs == null) {
				/*
				 * minimal document before. create inputlist
				 */
				inputs = system.getInputs().addNewInputList();
			}
			
			List<String> smlInputNames = new ArrayList<String>();
			/*
			 * first gather all names
			 */
			for (IoComponentPropertyType in : inputs.getInputArray()) {
				smlInputNames.add(in.getName());
			}

			/*
			 * we need to check if outputs are there
			 */
			if (!system.getOutputs().isSetOutputList()) {
				system.getOutputs().addNewOutputList();
			}

			for (String key : plug.getItems().keySet()) {
				if (!smlInputNames.contains(key)) {
					/*
					 * perhaps its the time?
					 */
					if (plug.getTime().getProperty().equals(key)) {
						IoComponentPropertyType newIn = system.getInputs().getInputList().addNewInput();
						IoComponentPropertyType newOut = system.getOutputs().getOutputList().addNewOutput();
						newIn.setName(key);
						newOut.setName(key);

						TimeDocument.Time t = newIn.addNewTime();
						t.setReferenceFrame(plug.getTime().getReferenceFrame());
						t.setDefinition(plug.getTime().getDefinition());

						TimeDocument.Time t2 = newOut.addNewTime();
						t2.setReferenceFrame(plug.getTime().getReferenceFrame());
						t2.setDefinition(plug.getTime().getDefinition());

						continue;
					}

					/*
					 * this has been added probably by pnp mode
					 */
					IoComponentPropertyType newIn = system.getInputs().getInputList().addNewInput();
					IoComponentPropertyType newOut = system.getOutputs().getOutputList().addNewOutput();
					newIn.setName(key);
					newOut.setName(key);
					Item item = plug.getItems().get(key);
					Class<?> type = item.getDataType();

					if (type == Double.class) {
						Quantity quan = newIn.addNewQuantity();
						Quantity quan2 = newOut.addNewQuantity();
						UomPropertyType uom = UomPropertyType.Factory.newInstance();
						uom.setCode(item.getUom());
						quan.setUom(uom);
						quan2.setUom(uom);
						quan.setDefinition(item.getDefinition());
						quan2.setDefinition(item.getDefinition());
					}
					else if (type == String.class) {
						Text text = newIn.addNewText();
						Text text2 = newOut.addNewText();
						text.setDefinition(item.getDefinition());
						text2.setDefinition(item.getDefinition());
					}
					else if (type == Boolean.class) {
						Boolean bool = newIn.addNewBoolean();
						Boolean bool2 = newOut.addNewBoolean();
						bool.setDefinition(item.getDefinition());
						bool2.setDefinition(item.getDefinition());
					}
				}
			}
		}
	}
	
	public static void newSensorML(Plugin plug) {
		SensorML sml = SensorML.Factory.newInstance();
		
		sml.setVersion("1.0.1");
		Member member = sml.addNewMember();

		SystemType system = SystemType.Factory.newInstance();


		system.addNewInputs().addNewInputList();
		system.addNewOutputs().addNewOutputList();

		member.setProcess(system);
		XMLTools.replaceXsiTypeWithInstance(member.getProcess(), 
				new QName(Plugin.SENSORML_NAMESPACE, "System"));

		plug.setSensorML(sml);
		updateSensorML(plug);
	}

	public static String toXML(Plugin plug) {
		/*
		 * define some settings
		 */
		Map<String,String> pres = new HashMap<String, String>();
		pres.put(Plugin.SENSORML_NAMESPACE, "sml");
		pres.put(Plugin.SPF_PLUGIN_NAMESPACE, "spf");

		XmlOptions opts = new XmlOptions();
		opts.setSavePrettyPrint();
		opts.setSaveSuggestedPrefixes(pres);
		opts.setSaveAggressiveNamespaces();

		PluginDocument pdoc = PluginDocument.Factory.newInstance(opts);
		PluginType plugin = pdoc.addNewPlugin();

		Output output = plugin.addNewOutput();

		/*
		 * we have period behaviour
		 */
		if (plug.getOutputType().equals(Plugin.PERIOD_BEHAVIOUR)) {
			PeriodBehaviourType period = PeriodBehaviourType.Factory.newInstance(opts);
			period.setTimedelta(plug.getTimeDelta());

			output.setBehaviour(period);
			XMLTools.replaceXsiTypeWithInstance(output.getBehaviour(),
					new QName(Plugin.SPF_PLUGIN_NAMESPACE, "PeriodBehaviour"));
		}
		/*
		 * we have availability behaviour
		 */
		else {
			AvailabilityBehaviourType avail = AvailabilityBehaviourType.Factory.newInstance(opts);
			OutputProperties props = avail.addNewOutputProperties();

			for (String key : plug.getOutputProperties()) {
				props.addProperty(key);
			}

			output.setBehaviour(avail);
			XMLTools.replaceXsiTypeWithInstance(output.getBehaviour(),
					new QName(Plugin.SPF_PLUGIN_NAMESPACE, "AvailabilityBehaviour"));
		}

		/*
		 * check if we have mandatories
		 */
		if (plug.getInputProperties().size() == plug.getMandatoryProperties().size()) {
			/*
			 * "on all"
			 */
			output.setOutputOnAllItems(true);
		}
		else if (plug.getMandatoryProperties().size() == 0) {
			/*
			 * "single allowed"
			 */
			output.setSingleOutputAllowed(true);
		}
		else {
			/*
			 * mandatories
			 */
			MandatoryProperties mandas = output.addNewMandatoryProperties();

			for (String key : plug.getMandatoryProperties()) {
				mandas.addProperty(key);
			}
		}

		/*
		 * we need a Time item
		 */
		boolean hasTime = false;
		for (Item item : plug.getItems().values()) {
			if (item instanceof Time) {
				hasTime = true;
				break;
			}
		}

		if (!hasTime) {
			/*
			 * add a standard time item
			 */
			plug.setTime(new Time("time"));
			plug.getTime().setReferenceFrame("urn:ogc:def:unit:iso8601");
			plug.getTime().setDefinition("urn:ogc:def:phenomenon:time");
			plug.getItems().put(plug.getTime().getProperty(), plug.getTime());
		}

		/*
		 * SensorML parts
		 */
		if (plug.getSensorML() != null) {
			updateSensorML(plug);
		}
		else {
			newSensorML(plug);
		}

		plugin.setSensorML(plug.getSensorML());

		plugin.setName(plug.getName());

		/*
		 * output of the xml
		 */
		XmlOptions opts2 = new XmlOptions();
		opts2.setSavePrettyPrint();
		pres = new HashMap<String, String>();
		pres.put(Plugin.SENSORML_NAMESPACE, "sml");
		opts2.setSaveSuggestedPrefixes(pres);

		try {
			PluginDocument tmp = PluginDocument.Factory.parse(pdoc.getDomNode());
			return tmp.xmlText(opts2);
		} catch (XmlException e) {
		    log.error(e);
		}

		return pdoc.xmlText(opts2);
	}
}
