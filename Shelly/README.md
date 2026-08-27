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

Version 1.1 changes: 
* Added findDevice command. Will flash LED to identify installed device.
* Added missed SupervisionReport handler.
* Setting ass grp 1 to Hub on update as device seems to loose it.
  

# Shelly  Wave Pro Dimmer 2PM Driver

Hubitat driver for Shelly Wave Pro Dimmer 2PM QPDM-0A2P01EU. Make sure you have the 12.07 or later firmware for the device from Shelly else it does not operate as advertised.
Get the firmware at [FW Wave Pro Dimmer 2PM QPDM-0A2P01EU](https://github.com/QubinoHelp/Shelly_Wave_FW_OTA/tree/main/Wave_Pro_Dimmer_2PM). You must change the file name to 
upload it to the Hubitat Device Firmware Updater app.

Upload this driver before including the device. After inclusion go to the Preferences and click Save. This will create the two child Dimmer devices that will control the 
dimmer outputs. The buttons are handled by the main device, although the driver can quickly be changed to have child devices for those as well. The buttons will only send 
the events when the Switch type is set to Push Button and is in Detached mode. In Normal mode, any switch attached to the SW1-4 inputs will control the matching output.

After physical installation with the loads you will use you should calibrate the outputs by running the Force Dimmer Calibration commands.

Full device instructions can be found at https://kb.shelly.cloud/knowledge-base/shelly-wave-pro-dimmer-2pm-eu

Version 1.3 changes: 
* Added attribute for central scene refresh rate. Definition commands do not have default.

Version 1.2 changes: 
* Debug logging also set child devices logging

Version 1.1 changes: 
* Added setCentralSceneRefreshRate
