# Shelly Wave 1PM Driver

Hubitat driver for Shelly Wave 1PM switch. 

Shell released new firmware for this switch, see https://support.shelly.cloud/en/support/solutions/articles/103000258471-firmware-updates. It is reccomended to perform this update. You can use the Hubitat device updater app for this. 

Version 1.2 changes: 
* Added support for Wave 1, Wave 1 Mini and Wave 1PM Mini. They share settings and commands. Only PM devices support Power and Energy reporting.

Version 1.1 changes: 
* Added reboot function. Use for troubleshooting only.
* Moved setting device configuration parameters for update function.
* Removed Notification report, not officially supported.
* Added FirmwareUpdate report handler.
* Fix for powerLow status value.


# Shelly Wave Plug S EU Driver

Hubitat driver for Shelly Wave Plug S EU version QNPL-0A112EU. 
