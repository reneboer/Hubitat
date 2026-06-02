/**
 *  Shelly Wave Pro Dimmer 2PM QPDM-0A2P01EU
 *  Device Handler
 *  Date: 02.06.2026
 *  Author: Rene Boer
 *  Copyright , none free to use
 *
 *  See https://kb.shelly.cloud/knowledge-base/shelly-wave-pro-dimmer-2pm-eu for full details on device.
 *  Note: Make sure to use firmware 12.06 or later on the device for propper operation.
 *
 *  CHANGELOG:
 *  1.0: First release
 *  1.1: Added setCentralSceneRefreshRate
 *  1.2: Debug logging also set child devices logging
 *  1.3: Added attribute for central scene refresh rate. Definition commands do not have default.
 */
import groovy.transform.Field

@Field static String VERSION = "1.3"
@Field static String DRIVER_NAME = "Shelly Wave Pro Dimmer 2PM"

// When commented out, there is no specific handler routine in this driver for the device
@Field static Map CMD_CLASS_VERS = [
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
  0x70: 4, // COMMAND_CLASS_CONFIGURATION_V4
  0x86: 3, // COMMAND_CLASS_VERSION_V3
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1
  0x73: 1, // COMMAND_CLASS_POWER_LEVEL_V1
  0x60: 4, // COMMAND_CLASS_MULTI_CHANNEL_V4
//  0x87: 3, // COMMAND_CLASS_INDICATOR_V3,
  0x7A: 5, // COMMAND_CLASS_FIRMWARE_UPDATE_MD_V5
//  0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY_V1
  0x5B: 3, // COMMAND_CLASS_CENTRAL_SCENE_V3
//  0x8E: 3, // COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
//  0x59: 3, // COMMAND_CLASS_ASSOCIATION_GRP_INFO_V3
//  0x85: 2, // COMMAND_CLASS_ASSOCIATION_V2
  0x32: 3, // COMMAND_CLASS_METER_V6
  0x71: 8, // COMMAND_CLASS_NOTIFICATION_V8
  0x26: 4, // COMMAND_CLASS_SWITCH_MULTILEVEL_V4
//  0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE_V2
  0x9F: 1, // COMMAND_CLASS_SECURITY_2_V1
  0x98: 1, // COMMAND_CLASS_SECURITY_V1
  0x5E: 2,  // COMMAND_CLASS_ZWAVEPLUS_INFO_V2
  0x20: 2, // COMMAND_CLASS_BASIC_V2
  0x25: 2 // COMMAND_CLASS_SWITCH_BINARY_V2
]
// Device parameters
@Field static Map parameterMap = [
  1:  [ key: "configParam1", title: "SW1 Switch type",
        desc: "Defines how the to treat the switch connected to SW1.",
        type: "enum", defaultValue: 0,
        options:[0:"Push Button", 1:"Toggle state (closed - ON/opened - OFF)", 2:"Toggle change (changes status when switch changes status)"],
        required: true, size: 1],
  2:  [ key: "configParam2", title: "SW2 Switch type",
        desc: "Defines how the to treat the switch connected to SW2.",
        type: "enum", defaultValue: 0,
        options:[0:"Push Button", 1:"Toggle state (closed - ON/opened - OFF)", 2:"Toggle change (changes status when switch changes status)"],
        required: true, size: 1],
  3:  [ key: "configParam3", title: "SW3 Switch type",
        desc: "Defines how the to treat the switch connected to SW3.",
        type: "enum", defaultValue: 0,
        options:[0:"Push Button", 1:"Toggle state (closed - ON/opened - OFF)", 2:"Toggle change (changes status when switch changes status)"],
        required: true, size: 1],
  4:  [ key: "configParam4", title: "SW4 Switch type",
        desc: "Defines how the to treat the switch connected to SW4.",
        type: "enum", defaultValue: 0,
        options:[0:"Push Button", 1:"Toggle state (closed - ON/opened - OFF)", 2:"Toggle change (changes status when switch changes status)"],
        required: true, size: 1],
  7:  [ key: "configParam7", title: "SW1 detach mode",
        desc: "If enabled the switch SW1 will not change output1.",
        type: "enum", defaultValue: 0, options:[0:"Normal mode", 1:"Detached mode"],
        required: true, size: 1],
  8:  [ key: "configParam8", title: "SW2 detach mode",
        desc: "If enabled the switch SW2 will not change output 1. If normal mode Par 131 must be Active",
        type: "enum", defaultValue: 0, options:[0:"Normal mode", 1:"Detached mode"],
        required: true, size: 1],
  9:  [ key: "configParam9", title: "SW3 detach mode",
        desc: "If enabled the switch SW3 will not change output 2.",
        type: "enum", defaultValue: 0, options:[0:"Normal mode", 1:"Detached mode"],
        required: true, size: 1],
  10: [ key: "configParam10", title: "SW4 detach mode",
        desc: "If enabled the switch SW4 will not change output 2. If normal mode Par 132 must be Active",
        type: "enum", defaultValue: 0, options:[0:"Normal mode", 1:"Detached mode"],
        required: true, size: 1],
  17: [ key: "configParam17", title: "O1 action in case of power out",
        desc: "Determines if on/off status is saved and restored after power failure.",
        type: "enum", defaultValue: 0, options:[0:"Last status", 1:"Switch remains off"],
        required: true, size: 1],
  18: [ key: "configParam18", title: "O2 action in case of power out",
        desc: "Determines if on/off status is saved and restored after power failure.",
        type: "enum", defaultValue: 0, options:[0:"Last status", 1:"Switch remains off"],
        required: true, size: 1],
  19: [ key: "configParam19", title: "O1 Auto OFF with timer",
        desc: "If the load O1 is ON, you can schedule it to turn OFF automatically after the period of time.<br/>0 Disabled, 1-32535s",
        type: "number", defaultValue: 0, min: 0, max: 32535,
        required: false, size: 2],
  20: [ key: "configParam20", title: "O1 Auto ON with timer",
        desc: "If the load O1 is OFF, you can schedule it to turn ON automatically after the period of time.<br/>0 Disabled, 1-32535s",
        type: "number", defaultValue: 0, min: 0, max: 32535,
        required: false, size: 2],
  21: [ key: "configParam21", title: "O2 Auto OFF with timer",
        desc: "If the load O2 is ON, you can schedule it to turn OFF automatically after the period of time.<br/>0 Disabled, 1-32535s",
        type: "number", defaultValue: 0, min: 0, max: 32535,
        required: false, size: 2],
  22: [ key: "configParam22", title: "O2 Auto ON with timer",
        desc: "If the load O2 is OFF, you can schedule it to turn ON automatically after the period of time.<br/>0 Disabled, 1-32535s",
        type: "number", defaultValue: 0, min: 0, max: 32535,
        required: false, size: 2],
  25: [ key: "configParam25", title: "Set timer units to s or ms for O1",
        desc: "Set the timer in seconds or milliseconds in Parameters No. 19, 20.",
        type: "enum", defaultValue: 0, options:[0:"Timer set in seconds", 1:"Timer set in milliseconds"],
        required: false, size: 1],
  26: [ key: "configParam26", title: "Set timer units to s or ms for O2",
        desc: "Set the timer in seconds or milliseconds in Parameters No. 21, 22.",
        type: "enum", defaultValue: 0, options:[0:"Timer set in seconds", 1:"Timer set in milliseconds"],
        required: false, size: 1],
  36: [ key: "configParam36", title: "Power report on change for O1",
        desc: "The minimum change in consumed power that will result in sending a new report.<br/>0 Disabled, 1-100%",
        type: "number", defaultValue: 10, min: 0, max: 100, required: true, size:1 ],
  37: [ key: "configParam37", title: "Power report on change for O2",
        desc: "The minimum change in consumed power that will result in sending a new report.<br/>0 Disabled, 1-100%",
        type: "number", defaultValue: 10, min: 0, max: 100, required: true, size:1 ],
  39: [ key: "configParam39", title: "Minimal time between reports for O1",
        desc: "The minimum time that must elapse before a new power report.<br/>0 Disabled, 1-120s",
        type: "enum", def: 0, options:[0:"Reports disabled", 30:"30 seconds", 60:"1 minute", 120:"2 minutes"], required: true, size: 1],
  40: [ key: "configParam40", title: "Minimal time between reports for O2",
        desc: "The minimum time that must elapse before a new power report.<br/>0 Disabled, 1-120s",
        type: "enum", def: 0, options:[0:"Reports disabled", 30:"30 seconds", 60:"1 minute", 120:"2 minutes"], required: true, size: 1],
  78: [ key: "configParam78", title: "Forced Dimmer calibration O1",
        desc: "Read only, use command button.",
        type: "enum", def: 3, options:[2:"Device is calibrated", 3:"Device is not calibrated", 4:"Calibration error"], required: false, size: 1, ro: true],
  89: [ key: "configParam89", title: "Forced Dimmer calibration O2",
        desc: "Read only, use command button.",
        type: "enum", def: 3, options:[2:"Device is calibrated", 3:"Device is not calibrated", 4:"Calibration error"], required: false, size: 1, ro: true],
  121:[ key: "configParam121", title: "Maximum Dimming value O1",
        desc: "Determines maximum dimming value.<br/>1-99%",
        type: "number", defaultValue: 99, min: 1, max: 99, required: true, size: 1],
  122:[ key: "configParam122", title: "Maximum Dimming value O2",
        desc: "Determines maximum dimming value.<br/>1-99%",
        type: "number", defaultValue: 99, min: 1, max: 99, required: true, size: 1],
  123:[ key: "configParam123", title: "Minimum Dimming value O1",
        desc: "Determines minimum dimming value.<br/>0-98%",
        type: "number", defaultValue: 15, min: 0, max: 98, required: true, size: 1],
  124:[ key: "configParam124", title: "Minimum Dimming value O2",
        desc: "Determines minimum dimming value.<br/>0-98%",
        type: "number", defaultValue: 15, min: 0, max: 98, required: true, size: 1],
  125:[ key: "configParam125", title: "Dimming time (soft on/off) O1",
        desc: "Time during which the device will move between the min. and max. dimming values by a short press.<br/>1-10s",
        type: "number", defaultValue: 3, min: 1, max: 10, required: true, size: 1],
  126:[ key: "configParam126", title: "Dimming time (soft on/off) O2",
        desc: "Time during which the device will move between the min. and max. dimming values by a short press.<br/>1-10s",
        type: "number", defaultValue: 3, min: 1, max: 10, required: true, size: 1],
  127:[ key: "configParam127", title: "Fade rate O1",
        desc: "Time during which the Dimmer will move between the min. and max. dimming values during a button hold.",
        type: "enum", defaultValue: 3, options:[1:"5% per sec", 2:"7% per sec", 3:"10% per sec", 4:"15% per sec", 5:"20% per sec"], required: true, size: 1],
  128:[ key: "configParam128", title: "Fade rate O2",
        desc: "Time during which the Dimmer will move between the min. and max. dimming values during a button hold.",
        type: "enum", defaultValue: 3, options:[1:"5% per sec", 2:"7% per sec", 3:"10% per sec", 4:"15% per sec", 5:"20% per sec"], required: true, size: 1],
  129:[ key: "configParam129", title: "Minimum brightens on toggle O1",
        desc: "Minimum brightness on toggle.<br/>1-99%",
        type: "number", defaultValue: 15, min: 1, max: 99, required: true, size: 1],
  130:[ key: "configParam130", title: "Minimum brightens on toggle O2",
        desc: "Minimum brightness on toggle.<br/>1-99%",
        type: "number", defaultValue: 15, min: 1, max: 99, required: true, size: 1],
  131:[ key: "configParam131", title: "SW1 & SW2 Dual Button Mode",
        desc: "If enabled SW1 will be on button, SW2 off button. Switch types 1,2 must be Push button",
        type: "enum", defaultValue: 0, options:[0:"Inactive", 1:"Active"], required: true, size: 1],
  132:[ key: "configParam132", title: "SW3 & SW4 Dual Button Mode",
        desc: "If enabled SW3 will be on button, SW4 off button. Switch types 3,4 must be Push button",
        type: "enum", defaultValue: 0, options:[0:"Inactive", 1:"Active"], required: true, size: 1]
]

// report type definitions by MeterReport scale supported by device.
@Field static meterReportTypes = [
    0: [type: "energy", unit: "kWh", special: false, description: "consumed"],
    2: [type: "power", unit: "W", special: false, description: "consumes", max: 4000, highlow: true]
]

// report type definitions by NotificationReport types supported by device.
@Field static notificationReportTypes = [
    0x04: [type: "Heat Alarm", 0x02: [name: "Overheat detected", desc: "Device turned off. Short press S Button or Power cycle to restore."]],
    0x08: [type: "Power Management", 0x06: [name: "Over-current detected", desc: "Device turned off. Short press S Button or Power cycle to restore."],
           0x07: [name: "Over-voltage detected", desc: "Device turned off. Short press S Button or Power cycle to restore."]]
]
// List of end points, 
// If number of end points is two we use the main device for the four buttons.
// If the number of end points is six, each button will have its own child device.
@Field static NUMBER_OF_ENDPOINTS = 2
@Field static NUMBER_OF_BUTTONS = 4
@Field static endPointTypes = [
    1: [type: 'O', comp: "Generic Component Metering Dimmer", label: "O1"],
    2: [type: 'O', comp: "Generic Component Metering Dimmer", label: "O2"],
    3: [type: 'SW', comp: "Generic Component Button Controller", label: "SW1"],
    4: [type: 'SW', comp: "Generic Component Button Controller", label: "SW2"],
    5: [type: 'SW', comp: "Generic Component Button Controller", label: "SW3"],
    6: [type: 'SW', comp: "Generic Component Button Controller", label: "SW4"]
]

metadata {
  definition(name: DRIVER_NAME, namespace: "reneboer", author: "Rene Boer", importUrl: "https://raw.githubusercontent.com/reneboer/Hubitat/refs/heads/main/Shelly/Shelly%20Wave%20PRO%20Dimmer%202PM%20Driver.groovy") {
    capability "Refresh"

//  Disable the button capabilities when using six end points as the button child devices will have those.
    capability "PushableButton"
    capability "HoldableButton"
    capability "ReleasableButton"
    capability "DoubleTapableButton"
          
    command "setCentralSceneRefreshRate", [[name: "Send held down notifications at slow rate*", type: "ENUM", description: "When true the KeyHeldDown notifications are send every 55s. Whan false, the notifications are send every 200ms", constraints: ["False", "True"]]]
    command "forceDimmerCalibration", [[name: "Force Dimmer calibration*", type: "ENUM", description: "Device will start executing force calibration procedure for the selected channel", constraints: ["O1", "O2"]]]
    command "resetPower" //command to issue Meter Reset commands to reset accumulated power measurements
    command "remoteReboot", [[name: "Reboot device*", type: "ENUM", description: "Reboot the device. Use for troubleshooting only.", constraints: ["Do nothing", "Perform reboot"]]] // Send remote reboot command to device
	  command "identify" // implements the Z-Wave Plus identify function which can flash device indicators.

    attribute "centralSceneRefreshRate", "enum", ["False", "True"] // Report current value based on CentralSceneNotification and CentralSceneConfigurationReport
      
    fingerprint mfr: "0460", prod: "0001", deviceId: "0082", deviceJoinName: DRIVER_NAME
    fingerprint mfr: "0460", prod: "0001", deviceId: "0082", inClusters: "0x5E,0x98,0x9F,0x55,0x6C", secureInClusters: "0x26,0x71,0x32,0x85,0x59,0x8E,0x5B,0x5A,0x7A,0x87,0x60,0x73,0x86,0x70,0x72", deviceJoinName: DRIVER_NAME
    fingerprint mfr: "0460", prod: "0001", deviceId: "0082", inClusters: "0x5E,0x98,0x9F,0x55,0x6C,0x26,0x71,0x32,0x85,0x59,0x8E,0x5B,0x5A,0x7A,0x87,0x60,0x73,0x86,0x70,0x72", deviceJoinName: DRIVER_NAME
  }

  preferences {
    parameterMap.eachWithIndex {pnum, param, i ->
      input (
        name: param.key,
        title: "${pnum}. ${param.title}",
        type: param.type,
        options: param.options,
        range: (param.min != null && param.max != null) ? "${param.min}..${param.max}" : null,
        defaultValue: param.def,
        description: param.desc,
        required: (param.required) ? true : false
      )
    }
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
  }
}

// Driver is installed, do setup.
void installed() {
	logInfo "installed(${VERSION})"
	List<hubitat.zwave.Command> commands = [
		zwave.versionV3.versionGet(),
		zwave.manufacturerSpecificV2.manufacturerSpecificGet()
  ]
  runCommandsWithInterstitialDelay(commands, 300)
}

// Driver gets initialized on system restart (not at installation)
void initialize() {
	logInfo "${device.label} initialize()"
  runIn (5, 'refresh')
}

// Driver gets uninstalled, cleanup
void uninstalled() {
  logInfo "${device.label} uninstalled()"
  if (childDevices) {
    logDebug "removing child devices"
    removeChildDevices(getChildDevices())
  }
}

// Device gets configured after install or when user click Configure
void configure() {
  logInfo "${device.label} configure()"
  if (NUMBER_OF_ENDPOINTS == 2) {
    sendEvent(name: "numberOfButtons", value: NUMBER_OF_BUTTONS)
  }
  runIn (2, 'configureAssocsCmds')  // Need to set assocs else multi channel is not working from device
  runIn (5, 'getConfig')  // Get current device config after installed.
}

// User clicked save preferences, set parameters and read them back.
void updated() {
  logDebug "Entering updated()"
  logWarn "debug logging is: ${logEnable == true}"
  logWarn "description logging is: ${txtEnable == true}"
  unschedule()
	List<hubitat.zwave.Command> commands = [
		zwave.multiChannelV4.multiChannelEndPointGet(),  // Triggers create child devices if not existing.
    zwave.centralSceneV3.centralSceneConfigurationGet()  // Get refresh rate
  ]
  parameterMap.eachWithIndex {pnum, param, i ->
    if (!param.ro && this["$param.key"] != null && (state."$param.key".toString() != this["$param.key"].toString() )) {
      commands << zwave.configurationV4.configurationSet(scaledConfigurationValue: this["$param.key"].toInteger(), parameterNumber: pnum, size: param.size)
    }
  }
  if (commands.size() > 0) {
    runCommandsWithInterstitialDelay(commands, 300)
    runInMillis((commands.size() * 300), 'getConfig')
  }
  // Turn on/off debug logging for any child device
  if (childDevices) {
    childDevices.each {
      it.updateSetting("logEnable",[value:logEnable,type:"bool"])
      it.updateSetting("txtEnable",[value:txtEnable,type:"bool"])
    }
  }
  // Schedule turning off debug logging in an hour
  if (logEnable) runIn(3600, 'logsOff')
}

// Poll current device status to sync values.
void refresh(ep = 0) {
	logDebug "refresh(ep : ${ep})"
  if (childDevices) {
  	List<hubitat.zwave.Command> commands=[zwave.centralSceneV3.centralSceneConfigurationGet()]
  	meterReportTypes.each { scale, data ->
      commands << zwave.meterV3.meterGet(scale: scale)
    }
    commands << zwave.switchMultilevelV4.switchMultilevelGet()
    if (ep > 0) {
      // Refresh give end point.
      runCommandsWithInterstitialDelay(commands, 300, ep)
    } else if (ep == 0) {
      // Refresh all dimmer end points
      //runCommandsWithInterstitialDelay(commands, 300)
      endPointTypes.eachWithIndex { e, data, i ->
        if (data.type == "O") runCommandsWithInterstitialDelay(commands, 300, e)
      }
    }
  } else {
    logDebug "No child devices yet."
  }
}

// Flash device indicator
void identify() {
  Short indId = 0x50
  Short togglingPeriods = 0x10
  Short togglingCycles = 0x0A
  Short togglingOnTime = 0x15
  List<Map<String, Short>> indicators = [["indicatorId": indId, "propertyId": 0x03, "value": togglingPeriods], ["indicatorId": indId, "propertyId": 0x04, "value": togglingCycles], ["indicatorId": indId, "propertyId": 0x05, "value": togglingOnTime]]
  runCommand(zwave.indicatorV3.indicatorSet (indicatorCount: 3, value: 1, indicatorValues: indicators))
}

// Commands for child dimmer devices
void componentOn(com.hubitat.app.DeviceWrapper cd) {
  logDebug "componentOn from ${cd.displayName} (${cd.deviceNetworkId} turningOn: ${turningOn})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) sendOnOffCmds(0xFF, endPoint)
}
void componentOff(com.hubitat.app.DeviceWrapper cd) {
  logDebug "componentOff from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) sendOnOffCmds(0x00, endPoint)
}
void componentSetLevel(com.hubitat.app.DeviceWrapper cd, level, Number duration = null) {
  logDebug "componentSetLevel from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) sendSetLevelCmds(level, duration, endPoint)
}
void componentStartLevelChange(com.hubitat.app.DeviceWrapper cd, direction) {
  logDebug "componentStartLevelChange from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) {
    Boolean upDownVal = direction == "down" ? true : false
    String param = "configParam${(endPoint == 1 ? 125 : 126)}"
    Short dimmingDuration = settings."${param}" != null ? settings."${param}" : 0xFF
    Short startLevel = cd.currentValue("level")
    logDebug "componentStartLevelChange(upDown: ${direction}, startLevel: ${startLevel}, dimmingDuration: ${dimmingDuration})"
    runCommand(zwave.switchMultilevelV4.switchMultilevelStartLevelChange(ignoreStartLevel: false, startLevel: startLevel, upDown: upDownVal, dimmingDuration: dimmingDuration), endPoint)
  }
}
void componentStopLevelChange(com.hubitat.app.DeviceWrapper cd) {
  logDebug "componentStopLevelChange from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) runCommand(zwave.switchMultilevelV4.switchMultilevelStopLevelChange(), endPoint)
}
void componentRefresh(com.hubitat.app.DeviceWrapper cd) {
  logDebug "componentRefresh from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) refresh(endPoint)
}

// Handle button commands if we have button child devices (NUMBER_OF_ENDPOINTS = 6).
void componentPush(com.hubitat.app.DeviceWrapper cd, button) {
  logDebug "componentPush from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) sendButtonEvent("pushed", 1, "digital", endPoint)
}
void componentDoubleTap(com.hubitat.app.DeviceWrapper cd, button) {
  logDebug "componentDoubleTap from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) sendButtonEvent("doubleTapped", 1, "digital", endPoint)
}
void componentHold(com.hubitat.app.DeviceWrapper cd, button) {
  logDebug "componentHold from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) sendButtonEvent("held", 1, "digital", endPoint)
}
void componentRelease(com.hubitat.app.DeviceWrapper cd, button) {
  logDebug "componentRelease from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint > 0) sendButtonEvent("released", 1, "digital", endPoint)
}

// Handle button commands when main device handles buttons (NUMBER_OF_ENDPOINTS = 2).
void push(button){
  sendButtonEvent("pushed", button, "digital")
}
void hold(button){
  sendButtonEvent("held", button, "digital")
}
void release(button){
  sendButtonEvent("released", button, "digital")
}
void doubleTap(button){
  sendButtonEvent("doubleTapped", button, "digital")
}

// Handle special commands
void setCentralSceneRefreshRate(state) {
  logDebug "setCentralSceneRefreshRate(${state})"
  runCommand zwave.centralSceneV3.centralSceneConfigurationSet(slowRefresh: state == "True")
}
void resetPower(Integer ep=0) {
  logDebug "resetPower() ep ${ep}"
  if (ep > 0) {
    // Reset given end point.
    runCommand(zwave.meterV3.meterReset(), ep)
  } else if (ep == 0) {
    // Reset all dimmer end points
    endPointTypes.eachWithIndex { e, data, i ->
      if (data.type == "O") runCommand(zwave.meterV3.meterReset(), e)
    }
  }
  runIn (5, 'refresh')
}
// Send a reboot configuratin command. Parameter not included in user preferences.
void remoteReboot(flag) {
  logDebug "remoteReboot(${flag})"
  if (flag == "Perform reboot") {
    logWarn "Rebooting device."
    runCommand zwave.configurationV1.configurationSet(parameterNumber: 117, size: 1, scaledConfigurationValue: 1)
  }
}
// Send the dimmer calibration configuration command. Parameters are read only in user preferences.
void forceDimmerCalibration(output) {
  logDebug "forceDimmerCalibration(${output})"
  Integer pn = 78
  if (output == "O2") { pn = 89 }
  runCommand zwave.configurationV1.configurationSet(parameterNumber: pn, size: 1, scaledConfigurationValue: 1)
}

/**
* Incomming zwave event handlers.
*/
void parse(String description) {
  hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
  if (cmd) {
    logDebug "parse() - parsed to cmd: ${cmd?.inspect()}"
    zwaveEvent(cmd)
  } else {
    logErr "parse() - Non-parsed - description: ${description?.inspect()}"
  }
}

// Handle zwave events not expected
void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
  logWarn "zwaveEvent(Command) - No specific handler - ep: ${ep}, cmd: ${cmd.inspect()}"
}

// Handle CentralSceneConfigurationReport
void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneConfigurationReport cmd, ep=0) {
  logDebug "zwaveEvent(CentralSceneConfigurationReport) - ${cmd}"
  updateCentralSceneRefreshRate(cmd.slowRefresh)
}

// Handle button push when in Detached Push Botton mode. Not received in other modes.
void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd, ep = 0) {
  logDebug "zwaveEvent(CentralSceneNotification ${cmd}, endpoint = ${ep})"
  Integer button = cmd.sceneNumber
  Integer key = cmd.keyAttributes
  Boolean refreshRate = cmd.slowRefresh
  String action
  switch (key){
    case 0: //pushed
      action = "pushed"
      break
    case 1:	//released
      action = "released"
      break
    case 2:	//holding.
      action = "held"
      break
    case 3:	//double tap
      action = "doubleTapped"
      break
	default:
      logWarn "zwaveEvent(CentralSceneNotification) - skipped. Unknown button action."
  }
  if (action) {
    if (NUMBER_OF_ENDPOINTS == 2) {
      sendButtonEvent(action, button, "physical")
    } else {
      sendButtonEvent(action, 1, "physical", button + 2)
    }
    updateCentralSceneRefreshRate(cmd.slowRefresh)
  }
}

// Hanlde Meter events for chaild dimmer devices
void zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd, ep = 0) {
    logDebug "zwaveEvent(MeterReport ${cmd}, endpoint = ${ep}, scale = ${cmd.scale}, value = ${cmd.scaledMeterValue})"

  Map measurement = meterReportTypes[cmd.scale as Integer]
  if (measurement?.hasScale2) {
    measurement = meterReportTypes[cmd.scale as Integer][cmd.scale2 as Integer]
  }
  if (measurement) {
    // Update parent with totals value
    if (ep > 0) {
      // Update child with value
      def cd = getChildDevice("${device.deviceNetworkId}-${ep}")
      if (cd) {
        logDebug "Updating child device ${device.deviceNetworkId}-${ep}, scale ${cmd.scale}"
        cd.sendEvent([name: measurement.type, value: cmd.scaledMeterValue, unit: measurement.unit])
      } else {
        logWarn "Child device not found for ${device.deviceNetworkId}-${ep}"
      }
    } else {
      // Should not happen.
      logDebug "Updating parent device ${device.deviceNetworkId}, scale ${cmd.scale}"
      sendEventWrapper(name: measurement.type, value: cmd.scaledMeterValue, unit: measurement.unit)
    }
  } else {
    // Update meterReportTypes table when this occures.
    logWarn "Scale not implemented. ${cmd.scale}, ${cmd.scale2}: ${cmd.scaledMeterValue}"
  }
}

// Handle incomming config parameter and set in user preferences
void zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport cmd) {
  logDebug "zwaveEvent(ConfigurationReport) - cmd: ${cmd.inspect()}"
  def newVal = cmd.scaledConfigurationValue.toInteger()
	def pnum = cmd.parameterNumber.toInteger()
  Map param = parameterMap[pnum]

  if (param) {
   	def curVal = device.getSetting(param.key)
   	if (param.type == "bool") { curVal = curVal == false ? 0 : 1}
   	try {
   		curVal = curVal.toInteger()
   	} catch(Exception ex) {
  		//logWarn "Undefined parameter ${curVal}."
  		curVal = null
  	}
  	Long sizeFactor = Math.pow(256,cmd.size).round()
    if (newVal < 0) { newVal += sizeFactor }
    if (curVal != newVal) {
    	if (param.type == "enum") { newVal = newVal.toString()}
    	if (param.type == "bool") { newVal = newVal == 0 ? false: true}
    		device.updateSetting(param.key, [value: newVal, type: param.type])
     		logDebug "Updating device parameter setting ${cmd.parameterNumber} from ${curVal} to ${newVal}."
    } else {
    	logDebug "No need to update device parameter setting ${cmd.parameterNumber} from ${curVal} to ${newVal}."
    }
  } else {
   	logWarn "Unsupported parameter ${cmd.parameterNumber}."
  }
}

// Basic report reports same as SwitchMultiLevelReport.
void zwaveEvent(hubitat.zwave.commands.basicv2.BasicReport cmd, ep = 0){
  logDebug "zwaveEvent(BasicReport) - cmd: ${cmd.inspect()}, ep: ${ep}."
  sendSwitchEvents(cmd.value, "digital", ep)
}
// Is send when button is detached, so do nothing.
void zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd, ep = 0){
  logDebug "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}, ep: ${ep}."
//  sendButtonEvent(cmd.value ? "pushed" : "released", 1, "physical", ep)
}
void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelReport cmd, ep = 0) {
  logDebug "zwaveEvent(SwitchMultilevelReport) - cmd: ${cmd.inspect()}, endpoint = ${ep}"
  sendSwitchEvents(cmd.value, "digital", ep)
}

// Set device details in device info
void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logDebug "zwaveEvent(ManufacturerSpecificReport) - cmd: ${cmd.inspect()}"
  if (cmd.manufacturerName) { device.updateDataValue("manufacturer", cmd.manufacturerName) }
  if (cmd.productTypeId) { device.updateDataValue("productTypeId", cmd.productTypeId.toString()) }
  if (cmd.productId) { device.updateDataValue("deviceId", cmd.productId.toString()) }
  device.updateDataValue("MSR", String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId))
}
// Set device version details in device info
void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
  logDebug "zwaveEvent(VersionReport) - cmd: ${cmd.inspect()}"
  device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
  device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
  device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
  if (cmd.firmwareTargets > 0) {
    cmd.targetVersions.each { target ->
      device.updateDataValue("firmware${target.target}Version", "${target.version}.${target.subVersion}")
    }
  }
}
//Decodes Multichannel Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd) {
  logDebug "zwaveEvent(MultiChannelCmdEncap) - cmd: ${cmd.inspect()}"
  hubitat.zwave.Command encapsulatedCmd = cmd.encapsulatedCommand(CMD_CLASS_VERS)
  logDebug "${cmd} --ENCAP-- ${encapsulatedCmd}"
  if (encapsulatedCmd) {
    zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
  } else {
    logWarn "Unable to extract encapsulated cmd from $cmd"
  }
}
// Devices that support the Security command class can send messages in an encrypted form; they arrive wrapped in a SecurityMessageEncapsulation command and must be unencapsulated
void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
  logDebug "zwaveEvent(SecurityMessageEncapsulation) - cmd: ${cmd.inspect()}"
  hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
  if (encapsulatedCommand) {
    logDebug "zwaveEvent(SecurityMessageEncapsulation) - encapsulatedCommand: ${encapsulatedCommand}"
    zwaveEvent(encapsulatedCommand)
  } else {
    logWarn "zwaveEvent(SecurityMessageEncapsulation) - Unable to extract Secure command from: ${cmd.inspect()}"
  }
}
// Get all active associations
void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
  logDebug "zwaveEvent(AssociationGroupingsReport) cmd: ${cmd.inspect()}"
	List<hubitat.zwave.Command> commands = []
    (1..cmd.supportedGroupings).each() {
      commands << zwave.associationV2.associationGet(groupingIdentifier: it)
    }
	if (commands.size() > 0) {
		sendCommands(commands, 300)
	}
}
void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    logDebug "zwaveEvent(AssociationReport) cmd: ${cmd.inspect()}"
}
void zwaveEvent(hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport cmd) {
    logDebug "zwaveEvent(MultiChannelAssociationReport) cmd: ${cmd.inspect()}"
}

//Central Scene (buttons) Detection
void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneSupportedReport cmd) {
  logDebug "zwaveEvent(CentralSceneSupportedReport) - cmd: ${cmd.inspect()}"
	/** Figure out the max key presses per button
  result:  CentralSceneSupportedReport(
			supportedScenes:2, 
			identical:true, 
			slowRefresh:true, 
			supportedKeyAttributes:[[sceneNumber:1, keyPress1x:true, keyRelease:true, keyHold:true, keyPress2x:true, keyPress3x:false, keyPress4x:false, keyPress5x:false]]
	)
  */
  updateCentralSceneRefreshRate(cmd.slowRefresh)
}

// Handle super vision if included secure.
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
  logDebug "zwaveEvent(SupervisionGet) - cmd: ${cmd.inspect()}, endpoint = ${ep}"
  hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(commandClasses)
  if (encapsulatedCommand) {
    logDebug "zwaveEvent(SupervisionGet) - encapsulatedCommand: ${encapsulatedCommand}"
    zwaveEvent(encapsulatedCommand, ep)
  } else {
    logErr "SupervisionGet - Non-parsed - description: ${description?.inspect()}"
  }
  runCommand(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0), ep)
}
// Report requested by updated. Create child devices if missing.
void zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelEndPointReport cmd) {
  logDebug "MultiChannelEndPointReport: dynamic: ${cmd.dynamic} endPoints: ${cmd.endPoints} identical: ${cmd.identical}"
  createChildDevices()
}

// Report progress on Firemware updates.
void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv5.FirmwareMdReport cmd) {
  logDebug "zwaveEvent(FirmwareMdReport) - cmd: ${cmd.inspect()}"
  logInfo "Starting firmware update process..."
}
void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv5.FirmwareUpdateMdRequestReport cmd) {
  logDebug "zwaveEvent(FirmwareUpdateMdRequestReport) - cmd: ${cmd.inspect()}"
  if (cmd.status == 255) {
    logInfo "Valid firmware for device. Firmware update continuing..."
  } else {
    logErr "Invalid firmware for device, error code ${cms.status}"
  }
}
void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv5.FirmwareUpdateMdStatusReport cmd) {
  logDebug "zwaveEvent(FirmwareUpdateMdStatusReport) - cmd: ${cmd.inspect()}"
  if (cmd.status == 255) {
    logInfo "Firmware update succesfully completed."
  } else {
    logErr "Error updating firmware for device, error code ${cmd.status}"
  }
}
void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv5.FirmwareUpdateMdGet  cmd) {
  logDebug "zwaveEvent(FirmwareUpdateMdGet ) - cmd: ${cmd.inspect()}"
}

/*
** Support functions
*/
// We only create the two dimmer child devices, not the four switches for now.
private void createChildDevices() {
  	logDebug "${device.label} creating child devices if missing"
  	def childDev
  	String devLabel
  	String devID
  	try {
    	(1..NUMBER_OF_ENDPOINTS).each() {
    		Map epd = endPointTypes[it]
    		if (epd) {
//       	devLabel = "${DRIVER_NAME} ${epd.label}" 	// Use when calling from installed, label will not yet be entered by user.
       		devLabel = "${device.label} ${epd.label}"	// Use when calling from updated. Is preferred when multiple of these devices are installed.
          devID = "${device.deviceNetworkId}-${it}"
       		def cd = getChildDevice(devID)
       		if (!cd) {
       			childDev = addChildDevice("hubitat", epd.comp, devID, [isComponent: true, label: devLabel, name: devLabel, completedSetup: true])
       			childDev.updateDataValue("endPoint","${it}")
       			if (epd.type == "O") {
       				childDev.sendEvent(name: "switch", value: "off")
         			childDev.sendEvent(name: "level", value: 0)
         			childDev.sendEvent(name: "power", value: 0.0)
         			childDev.sendEvent(name: "energy", value: 0.0)
              // Send off command to sync switch status.
              runIn(6, 'newChildSwitchOff', [overwrite: false, data: [endPoint: it]])
     				}
     				if (epd.type == "SW") childDev.sendEvent(name: "numberOfButtons", value: 1)
          	logDebug "Created child device ${devLabel}, ID: ${devID}"
          }
    		}
		}
  	} catch (e) {
    	logWarn "${e}"
      sendEvent(descriptionText: "Child device creation failed. Missing drivers '${endPointTypes[1].comp}' and/or '${endPointTypes[3].comp}'",
  		eventType: "ALERT", name: "childDeviceCreation", value: "failed", displayed: true)
  	}
}
// Remove any child devices
private removeChildDevices(delete) {
  delete.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}
// Turn off switch
private void newChildSwitchOff(data) {
  logDebug "newChildSwitchOff(${data})"
  Integer ep = data.endPoint
  sendOnOffCmds(0x00, ep)
}
// Deduct switch and dimmer status based on level report
private void sendSwitchEvents(rawVal, String type, Integer ep=0) {
  logDebug "sendSwitchEvents - value: ${rawVal}, type: ${type}, ep: ${ep}"
  devID = "${device.deviceNetworkId}-${ep}"
  // Set switch state to on when level is above zero
  String value = (rawVal ? "on" : "off")
	String desc = "switch is turned ${value}" + (type ? " (${type})" : "")
  sendEventWrapper(name:"switch", value:value, type:type, desc:desc, ep)
  // Set level
	if (rawVal) {
		Integer level = (rawVal == 99 ? 100 : rawVal)
		desc = "level is set to ${level}%"
		if (type) desc += " (${type})"
		sendEventWrapper(name:"level", value:level, type:type, unit:"%", desc:desc, ep)
	}
}
// Hande button event for main or optional child button devices
private void sendButtonEvent(action, button, type, Integer ep = 0){
  logDebug "sendButtonEvent(action: ${action}, button: ${button}, type: ${type}, ep: ${ep})"
  if (ep > 0) {
    sendEventWrapper(name: action, value: button, type: type, descriptionText:"button ${button} was ${action} [${type}]", isStateChange:true, ep)
  } else {
    if (button >= 1 && button <= NUMBER_OF_BUTTONS) {
      sendEventWrapper(name: action, value: button, type: type, descriptionText:"button ${button} was ${action} [${type}]", isStateChange:true)
    } else {
      logWarn "button${action} button number ${button} invalid."
    }	  
  }	  
}
// Update central scene refresh rate attribute
private void updateCentralSceneRefreshRate(Boolean slowRefresh){
  sendEventWrapper(name: "centralSceneRefreshRate", value: slowRefresh ? "True":"False")
}
// Request configuration parameters from device.
private void getConfig() {
  logDebug 'getConfig()'
  List<hubitat.zwave.Command> commands = []
  parameterMap.eachWithIndex {pnum, param, i ->
    commands << zwave.configurationV4.configurationGet(parameterNumber: pnum)
  }
  runCommandsWithInterstitialDelay commands
}
// Turn off debug logging
private void logsOff(){
  logWarn "debug logging disabled..."
  device.updateSetting("logEnable",[value:"false",type:"bool"])
  // Turn off for any child device
  if (childDevices) {
    childDevices.each {
      it.updateSetting("logEnable",[value:"false",type:"bool"])
    }
  }
}
// Set assoc so child devices report changes properly.
private void configureAssocsCmds() {
  logDebug 'configureAssocsCmds()'
  List<hubitat.zwave.Command> commands = []
  commands << zwave.associationV2.associationRemove(groupingIdentifier: 1, nodeId: [])
	commands << zwave.multiChannelAssociationV3.multiChannelAssociationSet(groupingIdentifier: 1, multiChannelNodeIds: [[nodeId: zwaveHubNodeId, bitAddress:0, endPointId: 0]])
	commands << zwave.multiChannelAssociationV3.multiChannelAssociationGet(groupingIdentifier: 1)
  runCommandsWithInterstitialDelay commands
}
// Send the switch on/off command as setlevel
private void sendOnOffCmds(val, Integer endPoint=0) {
	sendSetLevelCmds(val ? 0xFF : 0x00, null, endPoint)
}
// Send the set level command with right duration and  level to the correct end point.
private void sendSetLevelCmds(Number level, Number duration=null, Integer endPoint=0) {
	Short levelVal = safeToInt(level, 99)
	// level 0xFF tells device to use last level, 0x00 is off
	if (levelVal != 0xFF && levelVal != 0x00) {
		// Convert level in range of min/max
		levelVal = validateRange(levelVal, 99, 1, 99)
	}

	// Duration Encoding:
	// 0x01..0x7F 1 second (0x01) to 127 seconds (0x7F) in 1 second resolution.
	// 0x80..0xFE 1 minute (0x80) to 127 minutes (0xFE) in 1 minute resolution.
	// 0xFF Factory default duration.
	// Convert seconds to minutes
	if (duration > 120) {
		logDebug "getSetLevelCmds converting ${duration}s to ${Math.round(duration/60)}min"
		duration = (duration / 60) + 127
	}

	//If no duration specified, use device defaults
	Short durationVal = validateRange(duration, -1, -1, 254)
	if (duration == null || durationVal == -1) {
		durationVal = 0xFF
  }
	logDebug "getSetLevelCmds output [level:${levelVal}, duration:${durationVal}, endPoint:${endPoint}]"
  runCommand(zwave.switchMultilevelV4.switchMultilevelSet(value: levelVal, dimmingDuration: durationVal), endPoint)
}

// Find child device by end point number
private getChildByEP(endPoint) {
  endPoint = endPoint.toString()
  //Searching using endPoint data value
  def childDev = childDevices?.find { it.getDataValue("endPoint") == endPoint }
  if (childDev)
    logDebug "Found Child for endPoint ${endPoint} using data.endPoint: ${childDev.displayName} (${childDev.deviceNetworkId})"
  else {
	  // If not found try deeper search using the child DNIs
    String dni = getChildDNI(endPoint)
    childDev = childDevices?.find { it.deviceNetworkId == dni }
    if (childDev) {
      logDebug "Found Child for endPoint ${endPoint} parsing DNI: ${childDev.displayName} (${childDev.deviceNetworkId})"
      //Save the EP on the device so we can find it easily next time
      childDev.updateDataValue("endPoint","$endPoint")
    }
  }
  return childDev
}
// Get end point number for child device
private getChildEP(childDev) {
  Integer endPoint = safeToInt(childDev.getDataValue("endPoint"), null)
  if (!endPoint) {
    logWarn "Cannot determine endPoint number for ($childDev)"
//    runIn(2, createChildDevices)
  }
  return (endPoint ?: 0)
}
private String getChildDNI(epName) {
  return "${device.deviceId}-${epName}".toUpperCase()
}

// Wrapper for sendEvent to handle child events and support logging
private void sendEventWrapper(Map evt, Integer ep = 0) {
  // Set description if not passed in evt
  evt.descriptionText = evt.descriptionText ?: "${evt.name} set to ${evt.value} ${evt.unit ?: ''}".trim()
  // Endpoint Events
  if (ep > 0) {
    def childDev = getChildByEP(ep)
    if (childDev) {
      // S type always an event.
      if ((ep < 3) && (childDev.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange)) {
        evt.descriptionText = "${childDev}: ${evt.descriptionText}"
        childDev.parse([evt])
      } else {
        childDev.sendEvent(evt)
      }
    }
    else {
      if (state.deviceSync) { logDebug "No device for endpoint (${ep}) has been created yet..." }
      else { logErr "No device for endpoint (${ep}). Press Save Preferences to create child devices." }
    }
    return
  }
  // Main Device Events
  if (device.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
    logInfo "${evt.descriptionText}"
  } else {
    logDebug "${evt.descriptionText} [Not changed]"
  }
  // Always send event to update last activity
  sendEvent(evt)
}
// Validate value to be in specified range
private Integer validateRange(val, Integer defaultVal, Integer lowVal, Integer highVal) {
	Integer intVal = safeToInt(val, defaultVal)
	if (intVal > highVal) {
		return highVal
	} else if (intVal < lowVal) {
		return lowVal
	} else {
		return intVal
	}
}
// convert to Integer with default
private Integer safeToInt(val, defaultVal=0) {
  if ("${val}"?.isInteger())    { return "${val}".toInteger() }
  else if ("${val}"?.isNumber())  { return "${val}".toDouble()?.round() }
  else { return defaultVal }
}

// Logger wrapers
private void logErr(message) {
    log.error message
}
private void logWarn(message) {
    log.warn message
}
private void logInfo(message) {
    log.info message
}
private void logDebug(message) {
    if (logEnable) log.debug message
}

// zwave command handlers multi channel
private void runCommandsWithInterstitialDelay(List<hubitat.zwave.Command> commands, int delay = 300, Integer ep = 0) {
  logDebug "Entering runCommandsWithInterstitialDelay(${commands}) with ${commands.size()} commands for ep ${ep}"
  if (ep > 0) {
    sendHubCommand(new hubitat.device.HubMultiAction( delayBetween(commands.collect { command ->
          zwaveSecureEncap(zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: ep).encapsulate(command))
        }, delay), hubitat.device.Protocol.ZWAVE))
  } else {
    sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(commands.collect { command -> zwaveSecureEncap(command) }, delay), hubitat.device.Protocol.ZWAVE))
  }
}
private void runCommand(hubitat.zwave.Command command, Integer ep = 0) {
  logDebug "Entering runCommand(${command}) for ep ${ep}"
  if (ep > 0) {
    sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: ep).encapsulate(command)), hubitat.device.Protocol.ZWAVE))
  } else {
    sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(command), hubitat.device.Protocol.ZWAVE))
  }
}