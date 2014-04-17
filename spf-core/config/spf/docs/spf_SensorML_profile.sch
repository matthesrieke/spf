<?xml version="1.0" encoding="UTF-8"?>
<!--
SCHEMATRON PROFILE FOR SensorML PARTS FOR
INPUT-PLUGIN-DESCRIPTIONS OF THE Sensor Platform Framework
-->
<schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sml="http://www.opengis.net/sensorML/1.0.1"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:spf="http://ifgi.uni-muenster.de/~m_riek02/spf/0.1"
	xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:gml="http://www.opengis.net/gml"
	xmlns:xlink="http://www.w3.org/1999/xlink"
	xsi:schemaLocation="http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd"
	schemaVersion="ISO19757-3">

	<ns prefix="spf" uri="http://ifgi.uni-muenster.de/~m_riek02/spf/0.1" />
	<ns prefix="sml" uri="http://www.opengis.net/sensorML/1.0.1" />
	<ns prefix="gml" uri="http://www.opengis.net/gml" />
	<ns prefix="swe" uri="http://www.opengis.net/swe/1.0.1" />
	<ns prefix="xlink" uri="http://www.w3.org/1999/xlink" />

	<pattern id="SystemValidation">

		<!-- A SensorML document contains one "member" element. Each "member" must 
			contain a "System". -->
		<rule context="/spf:plugin">
			<assert test="count(sml:SensorML/sml:member) = 1">Error: platform description must have exactly one	'sml:member'.</assert>
			<assert test="count(sml:SensorML/sml:member/sml:System) = 1">Error: 'sml:member' element must contain one 'sml:System' element.</assert>
		</rule>

		<!-- sml:System asserts -->
		<rule context="//sml:System">
			<assert test="sml:contact">Error: 'sml:contact' mandatory in 'sml:System'</assert>
			<assert test="sml:identification">Error: 'sml:identification' mandatory in 'sml:System'</assert>
			
			<!-- check if we have elements to determine mobile or stationary platform -->
			<assert test="(count(sml:position/swe:Position/swe:location) + count(
				gml:boundedBy)) = 1">Error: Exactly one 'sml:position/swe:Position/swe:location' (for stationary sensors) or one gml:boundedBy (indicating a mobile sensors) must be present. Neither both nor none.</assert>
			
			<!-- check if we have a spatial reference system -->
			<assert test="(count(sml:position/swe:Position[@referenceFrame]) = 1 and
				count(sml:position/swe:Position[@referenceFrame != '']) = 1) or 
				(count(gml:boundedBy/gml:Envelope[@srsName]) = 1 and
				count(gml:boundedBy/gml:Envelope[@srsName != '']) = 1)">Error: A spatial reference frame must be present in sml:position (attribute 'referenceFrame' of swe:Position) or gml:boundedBy (attribute 'srsName' of gml:Envelope).</assert>
			
			<!-- we need inputs and outputs -->
			<assert test="sml:inputs">Error: 'sml:inputs' mandatory in 'sml:System'</assert>
			<assert test="sml:outputs">Error: 'sml:outputs' mandatory in 'sml:System'</assert>
		</rule>

		<!-- only allow one level of tree structures -->
		<rule context="//sml:System/sml:inputs/sml:InputList">
			<assert	test="count(sml:input/swe:DataRecord) = count(sml:input//swe:DataRecord)">Error: Only one level of swe:DataRecord is allowed (a swe:DataRecord must not contain another swe:DataRecord)</assert>
		</rule>

		<rule context="//sml:System/sml:outputs/sml:OutputList">
			<assert	test="count(sml:output/swe:DataRecord) = count(sml:output//swe:DataRecord)">Error: Only one level of swe:DataRecord	is allowed (a swe:DataRecord must not contain another swe:DataRecord)</assert>
		</rule>

	</pattern>

</schema>
