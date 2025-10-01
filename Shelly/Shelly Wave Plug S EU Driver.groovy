/**
 *  Shelly Wave Plug S EU QNPL-0A112EU
 *  Device Handler
 *  Version 1.0
 *  Date: 1.10.2025
 *  Author: Rene Boer
 *  Copyright , none free to use
 *
 * |---------------------------- DEVICE HANDLER FOR SHELL WAVE PLUG S EU Z-WAVE DEVICE -------------------------------------------------------|
 *    The handler supports all functions of the Shelly Wave Plug S EU QNPL-0A112EU device, EU version. Configuration parameters can be set in the 
 *    device's preferences screen.
 *  Note: US and UK versions may not work as the Firmware seems to be different.
 * |-----------------------------------------------------------------------------------------------------------------------------------------------|
 *
 *  TO-DO:
 *
 *  CHANGELOG:
 *  1.0 : First release
 */
import groovy.transform.Field

@Field String VERSION = "1.0"

// When commented out, there is no specific handler routine in this driver for the device
@Field static Map CMD_CLASS_VERS = [
  0x70: 4, // COMMAND_CLASS_CONFIGURATION V4 
//  0x20: 2, // COMMAND_CLASS_BASIC V2
//  0x59: 3, // COMMAND_CLASS_ASSOCIATION_GRP_INFO V3
//  0x85: 2, // COMMAND_CLASS_ASSOCIATION_V2 
  0x71: 8, // COMMAND_CLASS_NOTIFICATION_V8
  0x32: 5, // COMMAND_CLASS_METER V5
  0x25: 2, // COMMAND_CLASS_SWITCH_BINARY V2
//  0x73: 1, // COMMAND_CLASS_POWERLEVEL_V1
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
//  0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY_V1
  0x86: 3, // COMMAND_CLASS_VERSION V3
  0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO_V2
//  0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE_V2
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1
//  0x87: 3, // COMMAND_CLASS_INDICATOR V3
  0x7A: 5, // COMMAND_CLASS_FIRMWARE_UPDATE_MD V5
//  0x8E: 3, // COMMAND_CLASS_MULTICHANNEL_ASSOCIATION V3
  0x98: 1, // COMMAND_CLASS_SECURITY V1
  0x9F: 1  // COMMAND_CLASS_SECURITY_2 V1
]

@Field static Map parameterMap = [
  17: [ key: "configParam17", title: "Restore the output (O1) state after a power failure", 
        desc: "Determines if the on/off status is saved and restored for the load connected to O (O1) after a power failure.", 
        type: "enum", def: 0, options: [0:"Last status", 1:"Switch is off"], required: true, size: 1],
  19: [ key: "configParam19", title: "Output (O1) Auto OFF with timer", 
        desc: "If the load O (O1) is ON, you can schedule it to turn OFF automatically after the period of time defined in this parameter. The timer resets to zero each time the Device receives an ON command, either remotely (from the gateway or associated device) or locally from the switch.<br>0 Disabled, 1..32535 s/ms", 
        type: "number", def: 0, min: 0, max: 32535, required: false, size: 2],
  20: [ key: "configParam20", title: "Output (O1) Auto ON with timer", 
        desc: "If the load O (O1) is OFF, you can schedule it to turn ON automatically after the period of time defined in this parameter. The timer resets to zero each time the Device receives an OFF command, either remotely (from the gateway or associated device) or locally from the switch.<br>0 Disabled, 1..32535 s/ms", 
        type: "number", def: 0, min: 0, max: 32535, required: false, size: 2],
  23: [ key: "configParam23", title: "Output (O1) contact type - NO/NC", 
        desc: "The set value determines the relay contact type for output O (O1). The relay contact type can be normally open (NO) or normally closed (NC).", 
        type: "enum", def: 0, options: [0:"Normal Open", 1:"Normal Closed"], required: true, size: 1],
  25: [ key: "configParam25", title: "Set timer units to s or ms", 
        desc: "Set the timer units to seconds or milliseconds. Choose if you want to set the timer in seconds or milliseconds in Parameters No. 19, 20.", 
        type: "enum", def: 0, options:[0:"Timer set in seconds", 1:"Timer set in milliseconds"], required: true, size: 1],
  36: [ key: "configParam36", title: "Power report on change - percentage", 
        desc: "Determines the minimum change in consumed power that will result in sending a new report to the gateway.<br/>0 Disabled, 1-100%", 
        type: "number", def: 50, min: 0, max: 100, required: true, size: 1],
  39: [ key: "configParam39", title: "Minimum time between reports", 
        desc: "Determines the minimum time that must elapse before a new power report on O (O1) is sent to the gateway.", 
        type: "enum", def: 0, options:[0:"Reports disabled", 30:"30 seconds", 60:"1 minute", 120:"2 minutes"], required: true, size: 1],
  105:[ key: "configParam105", title: "LED Signalisation intensity", 
        desc: "Determines the intensity of the LED on the Device.<br/>0-100%", 
        type: "number", def: 0, min: 0, max: 100, required: true, size: 1]
]

// report type definitions by MeterReport scale supported by device.
@Field static meterReportTypes = [
    0: [type: "energy", unit: "kWh", special: false, description: "consumed"],
    2: [type: "power", unit: "W", special: false, description: "consumes", max: 4000, highlow: true]
]

// report type definitions by NotificationReport types supported by device.
@Field static notificationReportTypes = [
    0x04: [type: "Heat Alarm", 0x02: [name: "Overheat detected", desc: "Device turned off. Short press S Button or Power cycle to restore"]],
    0x08: [type: "Power Management", 0x06: [name: "Over-current detected", desc: "Device turned off. Short press S Button or Power cycle to restore"],
           0x07: [name: "Over-voltage detected", desc: "Device turned off. Short press S Button or Power cycle to restore"]]
]

metadata {
  definition(name: 'Shelly Wave Plug S EU', namespace: "reneboer", author: "Rene Boer", importUrl: "https://github.com/reneboer/Hubitat/blob/main/Shelly/Shelly%20Wave%20Plug%20S%20EU%20Driver.groovy") {
    capability 'Actuator'
    capability 'Switch'
    capability 'Outlet'
    capability 'PowerMeter'
    capability 'EnergyMeter'
    capability 'Sensor'
    capability 'Refresh'

    attribute  'powerHigh', 'number'
    attribute  'powerLow', 'number'

    command 'resetPower' //command to issue Meter Reset commands to reset accumulated power measurements
    command 'remoteReboot', [[name: "Reboot device*", type: "ENUM", description: "Reboot the device. Use for troubleshooting only.", default: "Please select", constraints: ["Please select", "Do nothing", "Perform reboot"]]] // Send remote reboot command to device

    fingerprint mfr:"0460", prod:"0002", deviceId: "0087", inClusters:"0x5E,0x98,0x9F,0x55,0x6C", secureInClusters: "0x85,0x59,0x8E,0x5A,0x7A,0x87,0x71,0x73,0x86,0x25,0x70,0x72,0x32", deviceJoinName: "Shelly Wave Plug S"
    fingerprint mfr:"0460", prod:"0002", deviceId: "0087", inClusters:"0x5E,0x98,0x9F,0x55,0x85,0x59,0x8E,0x5A,0x7A,0x87,0x71,0x73,0x6C,0x86,0x25,0x70,0x72,0x32", deviceJoinName: "Shelly Wave Plug S"
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
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
  }  
}


//  --------    HANDLE COMMANDS SECTION    --------
void logsOff(){
  logWarn "debug logging disabled..."
  device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void installed() {
  logInfo "installed(${VERSION})"
  runIn (10, 'getConfig')  // Get current device config after installed.
  runIn (15, 'refresh')  // Get current device config after installed.
}
// Handle device removal
def uninstalled() {
	logInfo "${device.label} uninstalled()"
}

void updated() {
  logInfo 'updated()'
  logWarn "debug logging is: ${logEnable == true}"
  logWarn "description logging is: ${txtEnable == true}"
  unschedule()
  if (logEnable) runIn(3600, logsOff)
  runIn (10, 'getConfig')  // Get current device config after updated.
  List<hubitat.zwave.Command> commands=[]
  parameterMap.eachWithIndex {pnum, param, i ->
   	if ( this["$param.key"] != null && (state."$param.key".toString() != this["$param.key"].toString() )) {
     	commands << zwave.configurationV4.configurationSet(scaledConfigurationValue: this["$param.key"].toInteger(), parameterNumber: pnum, size: param.size)
    }
  }
  runCommandsWithInterstitialDelay commands
}

void refresh() {
  logDebug 'refresh()'
  List<hubitat.zwave.Command> commands=[
    zwave.switchBinaryV2.switchBinaryGet()
  ]
  meterReportTypes.each { scale, data ->
    commands << zwave.meterV5.meterGet(scale: scale)
  }
  runCommandsWithInterstitialDelay commands 
}

void on() {
  logDebug 'on()'
  runCommand zwave.switchBinaryV2.switchBinarySet(switchValue: 0xFF)
}

void off() {
  logDebug 'off()'
  runCommand zwave.switchBinaryV2.switchBinarySet(switchValue: 0x00)
}

void resetPower() {
  logDebug 'resetPower()'
  runCommand zwave.meterV5.meterReset()
  meterReportTypes.each { scale, data ->
    if (data.highlow) {
      device.deleteCurrentState("${data.type}High")
      device.deleteCurrentState("${data.type}Low")
    }
  }
  runIn (5, 'refresh')
}

void remoteReboot(flag) {
  logDebug "remoteReboot(${flag})"
  if (flag == "Perform reboot") {
    logWarn "Rebooting device."
    runCommand zwave.configurationV4.configurationSet(parameterNumber: 117, size: 1, scaledConfigurationValue: 1)
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
/*
*	--------	EVENT PARSER SECTION	--------
*/
void parse(String description) {
  logDebug "Entering parse()"
  logDebug "description: ${description.inspect()}"
  hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
  if (cmd) {
    zwaveEvent(cmd)
  } else {
    logWarn "parse() - Non-parsed - description: ${description?.inspect()}"
  }
}

void zwaveEvent(hubitat.zwave.Command cmd, ep = 0) {
  logWarn "zwaveEvent(Command) - No specific handler - cmd: ${cmd.inspect()}"
}

void zwaveEvent(hubitat.zwave.commands.meterv5.MeterReport cmd) {
  logDebug "zwaveEvent(MeterReport) - cmd: ${cmd.inspect()}"
  Map measurement = meterReportTypes[cmd.scale as Integer]
  if (measurement?.hasScale2) {
  	measurement = meterReportTypes[cmd.scale as Integer][cmd.scale2 as Integer]
  }
  if (measurement) {
    float val = cmd.scaledMeterValue.toFloat()
    sendEventWrapper(name: measurement.type, value: val, unit: measurement.unit, descriptionText:"${measurement.description} ${cmd.scaledMeterValue} ${measurement.unit}")
    if (measurement.highlow) {
      def maxVal = measurement.max ? measurement.max : 36000  // Avoid excess values that are sometimes reported. Need a solution for energy.
      if (val > 0 && val < maxVal) {
        createMeterHistoryEvents(measurement, val, true)
        createMeterHistoryEvents(measurement, val, false)
      }
    }
  } else {
   	logWarn "Scale not implemented. ${cmd.scale}, ${cmd.scale2}: ${cmd.scaledMeterValue}"
  }
}
// See if High/Low attributes needs an update.
private void createMeterHistoryEvents(Map measurement, mainVal, Boolean lowEvent) {
  String name = "${measurement.type}${lowEvent ? 'Low' : 'High'}"
  def val = device.currentValue("${name}")
  if ((val == null) || (val == 0) || (lowEvent && (mainVal < val)) || (!lowEvent && (mainVal > val))) {
    sendEventWrapper(name:name, value: mainVal, unit:unit)
  }
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd) {
  logDebug "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}"
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off", type: "digital")
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
void zwaveEvent(hubitat.zwave.commands.versionv3.VersionCommandClassReport  cmd) {
  logDebug "zwaveEvent(VersionCommandClassReport ) - cmd: ${cmd.inspect()}"
}

void zwaveEvent(hubitat.zwave.commands.notificationv4.NotificationReport cmd) {
  logDebug "zwaveEvent(NotificationReport) - cmd: ${cmd.inspect()}"
  Map notif = notificationReportTypes[cmd.notificationType]
  if (notif) {
    Map detail = notif[cmd.event]
    if (detail) {
      sendEventWrapper(name: notif.type, descriptionText: "${detial.name}: ${detial.desc}", value: 0x00)
    } else {
      logWarn "Unhandled ${notif.type} event: ${cmd.event}"
    }
  } else {
    logWarn "Unhandled notifcation type: ${cmd?.notificationType}"
  }
}

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

// Handle S2 Supervision or device will think communication failed.
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
  logDebug "zwaveEvent(SupervisionGet) - cmd: ${cmd.inspect()}"
  hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
  if (encapsulatedCommand) {
    logDebug "zwaveEvent(SupervisionGet) - encapsulatedCommand: ${encapsulatedCommand}"
    zwaveEvent(encapsulatedCommand)
  } else {
    logErr "SupervisionGet - Non-parsed - description: ${description?.inspect()}"
  }
  runCommand zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)
}

// ===== Support functions START ======
private void sendEventWrapper(Map prop) {
  String cv = device.currentValue(prop.name)
  Boolean changed = (prop.isStateChange == true) || ((cv?.toString() != prop.value?.toString()) ? true : false)
  if (changed) sendEvent(prop)
  if (prop?.descriptionText) {
    if (txtEnable && changed) {
      logInfo "${device.displayName} ${prop.descriptionText}"
    } else {
      logDebug "${prop.descriptionText}"
    }
  }
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

// zwave command handlers
private void runCommandsWithInterstitialDelay(List<hubitat.zwave.Command> commands, int delay = 300) {
    logDebug "Entering runCommandsWithInterstitialDelay() with ${commands.size()} commands"
    sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(commands.collect { command -> zwaveSecureEncap command }, delay), hubitat.device.Protocol.ZWAVE))
}
private void runCommand(hubitat.zwave.Command command) {
    logDebug "Entering runCommand()"
    sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(command), hubitat.device.Protocol.ZWAVE))
}