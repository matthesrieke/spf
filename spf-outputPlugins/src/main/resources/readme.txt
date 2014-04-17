Sensor Platform Framework Common OutputPlugins
=============================================

Installation
------------
Installing the OutputPlugins is straightforward. Just copy over
the contents of the lib-folder into the lib-folder of your
SPFramework installation.

Configuration
------------
You need to register the InputPlugins via the main configuration
located at "config/spf.properties".
InputPlugins are registered using the IInputPlugin key. You need to
specify the full qualified java class name. The following plugins
are available within this package:

* GpxOutputPlugin (org.n52.ifgicopter.spf.output.GpxOutputPlugin):
Logs the tracked position data of a sensor platform. Can store
observed phenomena in the gpx:desc element.

* FileWriterPlugin (org.n52.ifgicopter.spf.output.FileWriterPlugin)
Stores data in a comma separated value. Default delimiter (|) can be
set using the pseudo-constructor, e.g.:
org.n52.ifgicopter.spf.output.FileWriterPlugin(,)



Contact
-------
Matthes Rieke - m.rieke@uni-muenster.de