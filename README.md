# Hubitat

Some of my first files for Hubitat

The Basic Plus Z-Wave tool:
The Basic Plus Z-Wave tool is a version of Hubitat's Basic Z-Wave tool (https://github.com/hubitat/HubitatPublic/blob/fde8d98449eeb035990576cdc2caa385a9860261/examples/drivers/basicZWaveTool.groovy) with the function added to set device associations. Create a new driver with this code, and select ‘Basic Plus Z-Wave tool’ for the device Type. Restore original driver when done.

To add an association, enter the groupingIdentifier number and the nodeId of device(s) to set the association with. You must use the decimal device numbers (not Hex). Multiple numbers are comma separated (1,2,3). Leave the nodeId field empty to remove the associations for the group.

Note that for battery operated devices you must wake up the device so the commands can be send.
