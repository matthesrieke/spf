Sensor Platform Framework Common InputPlugins
=============================================

Installation
------------
Installing the InputPlugins is straightforward. Just copy over
the contents of the lib-folder into the lib-folder of your
SPFramework installation.

Configuration
------------
You need to register the InputPlugins via the main configuration
located at "config/spf.properties".
InputPlugins are registered using the IInputPlugin key. You need to
specify the full qualified java class name. The following plugins
are available within this package:


* GpxInputPlugin (org.n52.ifgicopter.spf.input.GpxInputPlugin):
Replays a stored GPX track. Also reads the gpx:desc contents which
can store observed phenomena.

* TimeSeriesSimulation (org.n52.ifgicopter.spf.input.TimeSeriesSimulation)
A simple simulation plugin. Can be used for quick tests of
OutputPlugins.


Contact
-------
Matthes Rieke - m.rieke@uni-muenster.de