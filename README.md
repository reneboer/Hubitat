# Hubitat

Some of my first files for Hubitat

Qubino DIN Dimmer driver:
I started with a Qubino ZMNHSDx DIN Dimmer driver. The build in driver will show in Alexa as a temp sensor even if you do not have a temp sensor connected. This driver will have it show properly as a dimmer on Alexa. The device must be included without security or it will cause issues, also with the build in driver. Use a second controller to do this (zwave stick and SiLabs Windows Zwave Controller software).

Qubino Smart Plug 16A:
Added the Qubino ZMNHYDx Smart Plug 16A driver as the current Hubtat does not seem to have a driver for a switch with power measurements. This driver reports Watts, Kwh, Voltage and Amperage. The device can be included secure, but best be included without security if not realy needed. This can be done on the Hub directly.

The Basic Plus Z-Wave tool:
The Basic Plus Z-Wave tool is a version of Hubitat's Basic Z-Wave tool (https://github.com/hubitat/HubitatPublic/blob/fde8d98449eeb035990576cdc2caa385a9860261/examples/drivers/basicZWaveTool.groovy) with the function added to set device associations. Create a new driver with this code, and select ‘Basic Plus Z-Wave tool’ for the device Type. Restore original driver when done.

To add an association, enter the groupingIdentifier number and the nodeId of device(s) to set the association with. You must use the decimal device numbers (not Hex). Multiple numbers are comma separated (1,2,3). Leave the nodeId field empty to remove the associations for the group.

Note that for battery operated devices you must first wake up the device, and then hit setAssociation quickly.
