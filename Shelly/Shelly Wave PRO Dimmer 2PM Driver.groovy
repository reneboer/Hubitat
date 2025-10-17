/**
 *  Shelly Wave Pro Dimmer 2PM QPDM-0A2P01EU
 *  Device Handler
 *  Version 0.1
 *  Date: 15.3.2025
 *  Author: Rene Boer
 *  Copyright , none free to use
 *
 *  See https://kb.shelly.cloud/knowledge-base/shelly-wave-pro-dimmer-2pm-eu for full details on device.
 *
 *  CHANGELOG:
 *  0.1: First release
 */
import groovy.transform.Field

@Field String VERSION = "0.1"

// When commented out, there is no specific handler routine in this driver for the device
@Field static Map CMD_CLASS_VERS = [
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
  0x70: 4, // COMMAND_CLASS_CONFIGURATION_V4
  0x86: 3, // COMMAND_CLASS_VERSION_V3
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1
//  0x73: 1, // COMMAND_CLASS_POWER_LEVEL_V1
  0x60: 4, // COMMAND_CLASS_MULTI_CHANNEL_V4
//  0x87: 3, // COMMAND_CLASS_INDICATOR_V3,
  0x7A: 5, // COMMAND_CLASS_FIRMWARE_UPDATE_MD_V5
//  0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY_V1
  0x5B: 3, // COMMAND_CLASS_CENTRAL_SCENE_V3
//  0x8E: 3, // COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
//  0x59: 3, // COMMAND_CLASS_ASSOCIATION_GRP_INFO_V3
//  0x85: 2, // COMMAND_CLASS_ASSOCIATION_V2
  0x32: 5, // COMMAND_CLASS_METER_V5
  0x71: 8, // COMMAND_CLASS_NOTIFICATION_V8
  0x26: 3, // COMMAND_CLASS_SWITCH_MULTILEVEL_V3 (no V4 on Hubitat)
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
// List of end points, we use one switch child device for all four buttons, for four switches change #endpoints to 6 and #buttons to 1
@Field static NUMBER_OF_ENDPOINTS = 3
@Field static NUMBER_OF_BUTTONS = 4
@Field static endPointTypes = [
    1: [type: 'O', comp: "Generic Component Metering Dimmer"],
    2: [type: 'O', comp: "Generic Component Metering Dimmer"],
    3: [type: 'SW', comp: "Generic Component Button Controller"],
    4: [type: 'SW', comp: "Generic Component Button Controller"],
    5: [type: 'SW', comp: "Generic Component Button Controller"],
    6: [type: 'SW', comp: "Generic Component Button Controller"],
]

metadata {
  definition(name: 'Shelly Wave Pro Dimmer 2PM', namespace: "reneboer", author: "Rene Boer", importUrl: "https://raw.githubusercontent.com/reneboer/Hubitat/main/Shelly/Shelly%20Wave%20PRO%20Dimmer%202PM%20Driver.groovy") {
    capability "Configuration"
    capability "Refresh"
    command "forceDimmerCalibration", [[name: "Force Dimmer calibration*", type: "ENUM", description: " Device will start executing force calibration procedure for the selected channel", default: "O1", constraints: ["O1", "O2"]]]
    command "resetPower" //command to issue Meter Reset commands to reset accumulated power measurements
    command "remoteReboot", [[name: "Reboot device*", type: "ENUM", description: "Reboot the device. Use for troubleshooting only.", default: "Please select", constraints: ["Please select", "Do nothing", "Perform reboot"]]] // Send remote reboot command to device

    fingerprint mfr: "0460", prod: "0001", deviceId: "0082", deviceJoinName: "Shelly Wave Pro Dimmer 2PM"
    fingerprint mfr: "0460", prod: "0001", deviceId: "0082", inClusters: "0x5E,0x98,0x9F,0x55,0x6C", secureInClusters: "0x26,0x71,0x32,0x85,0x59,0x8E,0x5B,0x5A,0x7A,0x87,0x60,0x73,0x86,0x70,0x72", deviceJoinName: "Shelly Wave Pro Dimmer 2PM"
    fingerprint mfr: "0460", prod: "0001", deviceId: "0082", inClusters: "0x5E,0x98,0x9F,0x55,0x6C,0x26,0x71,0x32,0x85,0x59,0x8E,0x5B,0x5A,0x7A,0x87,0x60,0x73,0x86,0x70,0x72", deviceJoinName: "SShelly Wave Pro Dimmer 2PM"
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
    input name: "continousHeldEnable", type: "bool", title: "Continous Button Held reporting", defaultValue: true, description:"When true five held events are processed per second. When false only first held event is processed."
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
  }
}

void installed() {
  logInfo "installed(${VERSION})"
  runIn (5, 'getConfig')  // Get current device config after installed.
  runIn (15, 'refresh')  // Get the measurements from the device.
}
void uninstalled() {
  logInfo "${device.label} uninstalled()"
  if (childDevices) {
    logDebug "removing child devices"
    removeChildDevices(getChildDevices())
  }
}
private removeChildDevices(delete) {
  delete.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}

void configure() {
  logDebug "Entering configure()"
  uninstalled()
  installed()
}

void updated() {
  logDebug "Entering updated()"
  logWarn "debug logging is: ${logEnable == true}"
  logWarn "description logging is: ${txtEnable == true}"
  unschedule()
  createChildDevices()
  List<hubitat.zwave.Command> commands = []
  parameterMap.eachWithIndex {pnum, param, i ->
    if (!param.ro && this["$param.key"] != null && (state."$param.key".toString() != this["$param.key"].toString() )) {
      commands << zwave.configurationV4.configurationSet(scaledConfigurationValue: this["$param.key"].toInteger(), parameterNumber: pnum, size: param.size)
    }
  }
  if (commands.size() > 0) {
    runCommandsWithInterstitialDelay(commands, 300)
    runIn(10, 'getConfig')
  }
//  if (logEnable) runIn(3600, 'logsOff')
}

void refresh(ep = 0) {
  logDebug "refresh(ep : ${ep})"
  List<hubitat.zwave.Command> commands=[
    zwave.switchBinaryV2.switchBinaryGet()
  ]
  meterReportTypes.each { scale, data ->
    commands << zwave.meterV5.meterGet(scale: scale)
  }
  if (ep > 0) {
    // Refresh give end point.
    runCommandsWithInterstitialDelay(commands, 300, ep)
  } else if (ep == 0) {
    // Refresh all dimmer end points
    endPointTypes.eachWithIndex { e, data, i ->
      if (data.type == "O") runCommandsWithInterstitialDelay(commands, 300, e)
    }
  }
}

// We only create the two dimmer child devices, not the four switches for now.
private void createChildDevices() {
  logDebug "${device.label} creating child devices"
  def childDev
  String devLabel
  try {
    (1..NUMBER_OF_ENDPOINTS).each() {
      Map epd = endPointTypes[it]
      if (epd) {
        devLabel = "Shelly Wave Pro Dimmer ${epd.type}${epd.type == "O" ? it : NUMBER_OF_ENDPOINTS == 3 ? "1-4" : it - 2}"
        def cd = getChildDevice(${device.deviceNetworkId}-${it}")
        if (!cd) {
          childDev = addChildDevice("hubitat", epd.comp, "${device.deviceNetworkId}-${it}", [isComponent: true, label: devLabel, name: devLabel])
          childDev.updateDataValue("endPoint","${it}")
          if (epd.type == "O") {
            childDev.sendEvent(name: "switch", value: "off")
            childDev.sendEvent(name: "level", value: 0)
            childDev.sendEvent(name: "power", value: 0)
            childDev.sendEvent(name: "energy", value: 0)
          }
          if (epd.type == "SW") childDev.sendEvent(name: "numberOfButtons", value: NUMBER_OF_BUTTONS)
        } else {
        }
      }
    }
  } catch (e) {
    logWarn "${e}"
    sendEvent(descriptionText: "Child device creation failed. Please make sure that the \"Shelly Wave Pro Dimmer 2PM Child Device\" is installed and published.",
      eventType: "ALERT", name: "childDeviceCreation", value: "failed", displayed: true)
  }
}
// Commands for child devices
void componentOn(com.hubitat.app.DeviceWrapper cd) {
  logDebug "componentOn from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) runCommand(zwave.basicV2.basicSet(value: 0xFF), endPoint)
}
void componentOff(com.hubitat.app.DeviceWrapper cd) {
  logDebug "componentOff from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) runCommand(zwave.basicV2.basicSet(value: 0x00), endPoint)
}
void componentSetLevel(com.hubitat.app.DeviceWrapper cd, level, ramp = 0) {
  logDebug "componentSetLevel from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) {
    if (level > 99) level = 99
    if (ramp > 100) ramp = 100
    if (ramp < 1) ramp = 1
    logDebug "setLevel(value: ${ramp}, dimmingDuration: ${duration})"
    runCommand(zwave.switchMultilevelV4.switchMultilevelSet(value: level, dimmingDuration: ramp), endPoint)
  }
}
void componentStartLevelChange(com.hubitat.app.DeviceWrapper cd, direction) {
  logDebug "componentStartLevelChange from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) {
    Boolean upDownVal = direction == "down" ? true : false
    String param = "configParam${(endPoint == 1 ? 125 : 126)}"
    Short dimmingDuration = settings."${param}" != null ? settings."${param}" : 3
    Short startLevel = cd.currentValue("level")
    logDebug "startLevelChange(upDown: ${direction}, startLevel: ${startLevel}, dimmingDuration: ${dimmingDuration})"
    runCommand(zwave.switchMultilevelV4.switchMultilevelStartLevelChange(ignoreStartLevel: true, startLevel: startLevel, upDown: upDownVal, dimmingDuration: dimmingDuration), endPoint)
  }
}
void componentStopLevelChange(com.hubitat.app.DeviceWrapper cd) {
  logDebug "componentStopLevelChange from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) runCommand(zwave.switchMultilevelV4.switchMultilevelStopLevelChange(), endPoint)
}
void componentRefresh(com.hubitat.app.DeviceWrapper cd) {
  logDebug "componentRefresh from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) refresh(endPoint)
}
void componentPush(com.hubitat.app.DeviceWrapper cd, button) {
  logDebug "componentPush from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) sendButtonEvent("pushed", button, "digital", endPoint)
}
void componentDoubleTap(com.hubitat.app.DeviceWrapper cd, button) {
  logDebug "componentDoubleTap from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) sendButtonEvent("doubleTapped", button, "digital", endPoint)
}
void componentHold(com.hubitat.app.DeviceWrapper cd, button) {
  logDebug "componentHold from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) sendButtonEvent("held", button, "digital", endPoint)
}
void componentRelease(com.hubitat.app.DeviceWrapper cd, button) {
  logDebug "componentRelease from ${cd.displayName} (${cd.deviceNetworkId})"
  Integer endPoint = getChildEP(cd)
  if (endPoint) sendButtonEvent("released", button, "digital", endPoint)
}

void resetPower(Integer ep=0) {
  logDebug "resetPower() ep ${ep}"
  runIn (10, refresh)
  if (ep > 0) {
    // Reset give end point.
    runCommand(zwave.meterV5.meterReset(), ep)
  } else if (ep == 0) {
    // Reset all dimmer end points
    endPointTypes.eachWithIndex { e, data, i ->
      if (data.type == "O") runCommand(zwave.meterV5.meterReset(), e)
    }
  }
}

void remoteReboot(flag) {
  logDebug "remoteReboot(${flag})"
  if (flag == "Perform reboot") {
    logWarn "Rebooting device."
    runCommand zwave.configurationV1.configurationSet(parameterNumber: 117, size: 1, scaledConfigurationValue: 1)
  }
}
void forceDimmerCalibration(output) {
  logDebug "forceDimmerCalibration(${output})"
  pn = 78
  if (output == "O2") { pn = 89 }
  runCommand zwave.configurationV1.configurationSet(parameterNumber: pn, size: 1, scaledConfigurationValue: 1)
}
// Hande button event
private void sendButtonEvent(action, button, type, ep = 0){
  if (button >= 1 && button <= NUMBER_OF_BUTTONS) {
    sendEventWrapper(name:action, value:button, descriptionText:"button ${button} was ${action} [${type}]", isStateChange:true, type:type, ep)
  } else {
    logWarn "button${action} button number ${button} invalid."
  }	  
}
// Request parameters from device.
private void getConfig() {
  logDebug 'getConfig()'
  List<hubitat.zwave.Command> commands = []
  parameterMap.eachWithIndex {pnum, param, i ->
    if ( this["$param.key"] != null && state."$param.key".toString() != this["$param.key"].toString() ) {
      commands << zwave.configurationV4.configurationGet(parameterNumber: pnum)
    }
  }
  commands << zwave.versionV3.versionGet()
  if (!device.getDataValue("MSR")) commands << zwave.manufacturerSpecificV2.manufacturerSpecificGet()
  runCommandsWithInterstitialDelay commands
}
private void logsOff(){
  logWarn "debug logging disabled..."
  device.updateSetting("logEnable",[value:"false",type:"bool"])
}

/**
* Incomming zwave event handlers.
*/
void parse(String description) {
//  logger("debug", "parse() - description: ${description.inspect()}")
  hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
  if (cmd) {
    logDebug "parse() - parsed to cmd: ${cmd?.inspect()} with result: ${result?.inspect()}"
    zwaveEvent(cmd)
  } else {
    logErr "parse() - Non-parsed - description: ${description?.inspect()}"
  }
}

// Handle zwave events not expected
void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
  logDebug "Unhandled zwaveEvent: $cmd (ep ${ep}) [${getObjectClassName(cmd)}]"
}

// COMMAND_CLASS_CENTRAL_SCENE_V3
void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd, ep = 0) {
  logDebug "zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd, int endpoint = $ep)"
  Integer button = cmd.sceneNumber
  Integer key = cmd.keyAttributes
  String action
  switch (key){
    case 0: //pushed
      action = "pushed"
      break
    case 1:	//released, only after 2
      state."buttonHold${button}" = 0
      action = "released"
      break
    case 2:	//holding. We get 5 reports per second for as long button is held.
      if (continousHeldEnable) {
        action = "held"
      } else {
        if (state."buttonHold${button}" == 0){
          state."buttonHold${button}" = 1
          action = "held"
        }
      }
      break
    case 3:	//double tap, 4 is tripple tap
      action = "doubleTapped"
      break
	default:
      logWarn "zwaveEvent(CentralSceneNotification) - skipped. Unknown button action."
  }
  if (action) {
    if (NUMBER_OF_ENDPOINTS == 3) {
      sendButtonEvent(action, button, "physical", 3)
    } else {
      sendButtonEvent(action, 1, "physical", button + 2)
    }
  }
}

//COMMAND_CLASS_METER V5 (V6 not yet supported in Hubitat)
void zwaveEvent(hubitat.zwave.commands.meterv5.MeterReport cmd, ep = 0) {
  logDebug "zwaveEvent(hubitat.zwave.commands.meterv5.MeterReport cmd, int endpoint = $ep, scale = $cmd.scale)"

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
      }
    }
  } else {
    logWarn "Scale not implemented. ${cmd.scale}, ${cmd.scale2}: ${cmd.scaledMeterValue}"
  }
}

void zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport cmd) {
  logDebug "zwaveEvent(ConfigurationReport) - cmd: ${cmd.inspect()}"
  def newVal = cmd.scaledConfigurationValue.toInteger()

  Map param = parameterMap[cmd.parameterNumber.toInteger()]

  if (param) {
    def curVal = device.getSetting(param.key)
    if (param.type == "bool") { curVal = curVal == false ? 0 : 1}
    try {
      curVal = curVal.toInteger()
    }catch(Exception ex) {
      logWarn "Undefined parameter ${curVal}."
      curVal = null
    }
    Long sizeFactor = Math.pow(256,cmd.size).round()
    if (newVal < 0) { newVal += sizeFactor }
    if (curVal != newVal) {
      if (param.type == "enum") { newVal = newVal.toString()}
      if (param.type == "bool") { newVal = newVal == 0 ? false: true}
      device.updateSetting(param.key, [value: newVal, type: param.type])
      logDebug "Updating device parameter setting ${cmd.parameterNumber} from ${curVal} to ${newVal}."
    }
  } else {
    logWarn "Unsupported parameter ${cmd.parameterNumber}."
  }
}

void zwaveEvent(hubitat.zwave.commands.basicv2.BasicReport cmd, ep = 0){
  logDebug "zwaveEvent(BasicReport) - cmd: ${cmd.inspect()}, ep: ${ep}."
  sendEventWrapper(name: "switch", value: cmd.value ? "on" : "off", type: "physical", ep)
}
// Is send when button is detached, so do nothing.
void zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd, ep = 0){
  logDebug "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}, ep: ${ep}."
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logDebug "zwaveEvent(ManufacturerSpecificReport) - cmd: ${cmd.inspect()}"
  if (cmd.manufacturerName) { device.updateDataValue("manufacturer", cmd.manufacturerName) }
  if (cmd.productTypeId) { device.updateDataValue("productTypeId", cmd.productTypeId.toString()) }
  if (cmd.productId) { device.updateDataValue("deviceId", cmd.productId.toString()) }
  device.updateDataValue("MSR", String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId))
}
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
  hubitat.zwave.Command encapsulatedCmd = cmd.encapsulatedCommand(CMD_CLASS_VERS)
  logDebug "${cmd} --ENCAP-- ${encapsulatedCmd}"
  if (encapsulatedCmd) {
    zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
  } else {
    logWarn "Unable to extract encapsulated cmd from $cmd"
  }
}
void zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, ep = 0) {
  logDebug "zwaveEvent(SwitchMultilevelReport) - cmd: ${cmd.inspect()}"
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off", ep)
  sendEventWrapper(name:"level", value: cmd.value, unit:"%", descriptionText:"dimmed to ${cmd.value==255 ? 100 : cmd.value}%", ep)

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

//COMMAND_CLASS_SUPERVISION V1
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
  logDebug "zwaveEvent(SupervisionGet) - cmd: ${cmd.inspect()}"
  hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(commandClasses)
  if (encapsulatedCommand) {
    logDebug "zwaveEvent(SupervisionGet) - encapsulatedCommand: ${encapsulatedCommand}"
    zwaveEvent(encapsulatedCommand, ep)
  } else {
    logErr "SupervisionGet - Non-parsed - description: ${description?.inspect()}"
  }
  runCommand(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0), ep)
}

//COMMAND_CLASS_FIRMWARE_UPDATE_MD V5
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

/// Child Common Functions
private getChildByEP(endPoint) {
  endPoint = endPoint.toString()
  //Searching using endPoint data value
  def childDev = childDevices?.find { it.getDataValue("endPoint") == endPoint }
  if (childDev)
    logDebug "Found Child for endPoint ${endPoint} using data.endPoint: ${childDev.displayName} (${childDev.deviceNetworkId})"
  //If not found try deeper search using the child DNIs
  else {
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

private getChildEP(childDev) {
  Integer endPoint = safeToInt(childDev.getDataValue("endPoint"), null)
  if (!endPoint) {
    logWarn "Cannot determine endPoint number for ($childDev)"
//    executeProbeCmds()
//    runIn(2, createChildDevices)
  }
  return (endPoint ?: 0)
}

private String getChildDNI(epName) {
  return "${device.deviceId}-${epName}".toUpperCase()
}

// Wrapper for sendEvent to handle child events and support logging
private void sendEventWrapper(Map evt, ep = 0) {
  //Set description if not passed in evt
  evt.descriptionText = evt.descriptionText ?: "${evt.name} set to ${evt.value} ${evt.unit ?: ''}".trim()
  //Endpoint Events
  if (ep) {
    def childDev = getChildByEP(ep)
    if (childDev) {
      if (childDev.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
        evt.descriptionText = "${childDev}: ${evt.descriptionText}"
        childDev.parse([evt])
      } else {
        childDev.sendEvent(evt)
      }
    }
    else {
      if (state.deviceSync) { logDebug "No device for endpoint (${ep}) has been created yet..." }
      else { logErr "No device for endpoint (${ep}). Press Save Preferences (or Configure) to create child devices." }
    }
    return
  }
  //Main Device Events
  if (device.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
    logInfo "${evt.descriptionText}"
  } else {
    logDebug "${evt.descriptionText} [NOT CHANGED]"
  }
  //Always send event to update last activity
  sendEvent(evt)
}
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
private void runCommandsWithInterstitialDelay(List<hubitat.zwave.Command> commands, int delay = 300, int ep = 0) {
  logDebug "Entering runCommandsWithInterstitialDelay() with ${commands.size()} commands for ep ${ep}"
  if (ep > 0) {
    sendHubCommand(new hubitat.device.HubMultiAction( delayBetween(commands.collect { command ->
          zwaveSecureEncap(zwave.multiChannelV4.multiChannelCmdEncap(sourceEndPoint: 0, bitAddress: 0, res01: 0, destinationEndPoint: ep).encapsulate(command))
        }, delay), hubitat.device.Protocol.ZWAVE))
  } else {
    sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(commands.collect { command -> zwaveSecureEncap command }, delay), hubitat.device.Protocol.ZWAVE))
  }
}
private void runCommand(hubitat.zwave.Command command, ep = 0) {
  logDebug "Entering runCommand() for ep ${ep}"
  if (ep) {
    sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(zwave.multiChannelV4.multiChannelCmdEncap(sourceEndPoint: 0, bitAddress: 0, res01: 0, destinationEndPoint: ep).encapsulate(command)), hubitat.device.Protocol.ZWAVE))
  } else {
    sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(command), hubitat.device.Protocol.ZWAVE))
  }
}