/**
 *  Aeotec Smart Switch 7 EU ZW175-C16
 *  Device Handler
 *  Version 1.4
 *  Date: 25.9.2025
 *  Author: Rene Boer
 *  Copyright , none free to use
 *
 * |---------------------------- DEVICE HANDLER FOR AEOTEC SMART SWITCH 7 F Z-WAVE DEVICE -------------------------------------------------------|
 *    The handler supports all functions of the Aeotec Smart Smart Switch 7 device, EU version. Configuration parameters can be set in the 
 *    device's preferences screen.
 * |-----------------------------------------------------------------------------------------------------------------------------------------------|
 *
 *  TO-DO:
 *
 *  CHANGELOG:
 *  0.1 : First release
 *  1.0 : Added device Clock update when off on config. Added firmware targets to version report. Moved setting device configuration parameters for update function. Added FirmwareUpdate report handler. Fixed Reset Device function.
 *  1.1 : Add power poll when turned on or off.
 *  1.2 : Updated parameter 91 description to match V1.03 firmware
 *  1.3 : Corrected CLASS_FIRMWARE_UPDATE_MD to V2
 *  1.4 : Some rewrites and code cleanups, removed summary HTML tile, added parameter numbers to preferences screen, removed setAssociations command.
 */
import groovy.transform.Field

@Field String VERSION = "1.4"

metadata {
  definition(name: 'Aeotec Smart Switch 7 EU', namespace: "reneboer", author: "Rene Boer", importUrl: "https://github.com/reneboer/Hubitat/blob/main/Aeotec/Aeotec%20Smart%20Swtich%207%20EU%20Driver.groovy") {
    capability 'Actuator'
    capability 'Switch'
    capability 'Outlet'
    capability 'PowerMeter'
    capability 'EnergyMeter'
    capability 'VoltageMeasurement'
    capability 'CurrentMeter'
    capability 'Sensor'
    capability 'Refresh'

    attribute  'amperageHigh', 'number'
    attribute  'amperageLow', 'number'
        // attribute  'energyDuration', 'string'
    attribute  'powerHigh', 'number'
    attribute  'powerLow', 'number'
    attribute  'voltageHigh', 'number'
    attribute  'voltageLow', 'number'

    command 'resetPower' //command to issue Meter Reset commands to reset accumulated power measurements
    command 'startStopBlinking', [[name: "Start or stop LED blinking*", type: "NUMBER", description: "0 = Stop blinking. 1-255s = Blinking duration."]] // Send Start/stop blink command to device
    command 'resetDevice', [[name: "Reset device*", type: "ENUM", description: "Use with Care! Perform parameters reset to defaults, or a full factory reset. The latter will remove the switch from your Z-Wave network.", constraints: ["Please select", "Set device default configuration", "Factory Reset (Removes switch from network!)"]]] // Send Factory Reset or Initialization command to device

    fingerprint mfr:"0345", prod:"0002", deviceId: "0084", inClusters:"0x5E,0x55,0x22,0x98,0x9F,0x6C", secureInClusters: "0x85,0x59,0x70,0x2C,0x2B,0x81,0x71,0x32,0x25,0x33,0x26,0x75,0x73,0x7A,0x86,0x5A,0x72", deviceJoinName: "Aeotec Smart Switch 7", model:"Smart Switch 7", manufacturer:"Aeotec"
    fingerprint mfr:"0345", prod:"0002", deviceId: "0084", inClusters:"0x5E,0x85,0x59,0x55,0x70,0x2C,0x2B,0x81,0x71,0x32,0x25,0x33,0x26,0x86,0x72,0x5A,0x22,0x75,0x73,0x98,0x9F,0x6C,0x7A", deviceJoinName: "Aeotec Smart Switch 7", model:"Smart Switch 7", manufacturer:"Aeotec"
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
     		required: param.required
     	)
    }

    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
  }  
}

// When commented out, there is no specific handler routine in this driver for the device
@Field static Map CMD_CLASS_VERS = [
  0x70: 1, // COMMAND_CLASS_CONFIGURATION_V1 
//  0x59: 1, // COMMAND_CLASS_ASSOCIATION_GRP_INFO_V1
//  0x85: 2, // COMMAND_CLASS_ASSOCIATION_V2 
  0x71: 4, // COMMAND_CLASS_NOTIFICATION_V4
  0x32: 4, // COMMAND_CLASS_METER_V4
  0x25: 1, // COMMAND_CLASS_SWITCH_BINARY_V1
//  0x73: 1, // COMMAND_CLASS_POWERLEVEL_V1
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
//  0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY_V1
  0x86: 2, // COMMAND_CLASS_VERSION_V2 
  0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO_V2
//  0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE_V2
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1
  0x7A: 2, // COMMAND_CLASS_FIRMWARE_UPDATE_MD_V2
//  0x2C: 1, // COMMAND_CLASS_SCENE_ACTUATOR_CONF_V1
//  0x2B: 1, // COMMAND_CLASS_SCENE_ACTIVATION_V1
  0x81: 1, // COMMAND_CLASS_CLOCK_V1
//  0x33: 1, // COMMAND_CLASS_SWITCH_COLOR_V1
  0x26: 2, // COMMAND_CLASS_SWITCH_MULTI_LEVEL_V2
//  0x22: 1, // COMMAND_CLASS_APPLICATIPON_STATUS_V1
  0x75: 2, // COMMAND_CLASS_PROTECTION V2
  0x98: 1, // COMMAND_CLASS_SECURITY V1
  0x9F: 1  // COMMAND_CLASS_SECURITY_2 V1
]

@Field static Map parameterMap = [
  4:  [ key: "configParam4", title: "Over-load protection", 
        desc: "Define a threshold power and automatically turn off switch when the load connected bypasses the maximum allowed power regardless of always on setting.<br/>0 Disabled, 1-2415W", 
        type: "number", def: 2415, min: 0, max: 2415, required: true, size: 2],
  8:  [ key: "configParam8", title: "Switch reaction to alarm", desc: "Set the response of the switch in response to an operation performed when an alarm is received.", 
        type: "enum", def: 0, options: [0:"No action", 1:"Turn switch off", 2:"Turn switch on", 3:"Toggle switch turn on then turn off in a 10 sec cycle until alarm is disables"], 
        required: true,size: 1],
  9:  [ key: "configParam9", title: "Alarm reaction when received", 
        desc: "Configure what alarms Smart Switch 7 will react to from other Z-Wave devices. See manual for details.<br>0, 1, 256..32512", 
        type: "number", def: 0, min: 0, max: 32512, required: false, size: 2],
  10: [ key: "configParam10", title: "Release/Disable alarm", 
        desc: "Determines the method of disabling the alarm of the device.<br>0 3x tapping Action Button, 1 notification idle events, 10..255 sets time in minutes on how long the alarm state should be held.", 
        type: "number", def: 0, min: 0, max: 255, required: false, size: 1],
  18: [ key: "configParam18", title: "LED blinking frequency", desc: "Set amount of blinks per seconds.<br/>1-9s", 
        type: "number", def: 2, min: 1, max: 9, required: false, size: 1],
  20: [ key: "configParam20", title: "Action in case of power out", desc: "Determines if on/off status is saved and restored after power failure.", 
        type: "enum", def: 0, options:[0:"Last status", 1:"Switch is on", 2:"Switch is off"], required: true, size: 1],
  80: [ key: "configParam80", title: "Liveline command", desc:"Configure what command will be sent via Lifeline when switch state has changed.", 
        type: "enum", def: 2, options:[0:"None", 1:"Basic Report", 2:"Binary Switch Report"], required: true, size: 1],
  81: [ key: "configParam81", title: "Load Indicator Mode setting", desc: "See user guide for details.", 
        type: "enum", def: 2, options:[0:"Disable Mode", 1:"Night Light Mode", 2:"ON/OFF Mode"], required: false, size: 1],
  82: [ key: "configParam82", title: "Night Light Mode", desc: "Enable or disable Night Light Mode during specific times. See manual.", 
        type: "number", def: 0x12000800, min: 0, max: 389748539, required: false, size: 4],
  91: [ key: "configParam91", title: "Threshold Power", desc: "Threshold Power (W) If Watt passes the threshold setting by + or -, a Watt report will be sent to update its value.<br/>0 Disabled, 1-2300W", 
        type: "number", def: 0, min: 0, max: 2300, required: true, size: 2],
  92: [ key: "configParam92", title: "Threshold Energy", desc: "Threshold Energy (kWh) for inducing automatic report.<br/>0 Disabled, 1-10000KWh", 
        type: "number", def: 0, min: 0, max: 100000, required: true, size: 2],
  93: [ key: "configParam93", title: "Threshold Current", desc: "Threshold Current (A) for inducing automatic report.<br/>0 Disabled, 1-100 in 0.1 steps", 
        type: "number", def: 0, min: 0, max: 100, required: true, size: 1],
  101:[ key: "configParam101", title: "Meter Lifeline Reporting", desc: "Configure which meter reading will be periodically report via Lifeline.", 
        type: "enum", def: 0x0F, options:[2:"Power only", 8:"Current only", 10:"Power and Current", 3:"Power and Energy", 11:"Power, Current and Energy", 15:"Power, Current, Energy and Voltage"], required: true, size: 4],
  111:[ key: "configParam111", title: "Meter Reporting Frequency", desc: "Configure the sending frequency of Meter Report.<br/>0 Disabled, 30-2592000s.", 
        type: "number", def: 600, min: 0, max: 2592000, required: true, size: 4]
]

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
     	commands << zwave.configurationV1.configurationSet(scaledConfigurationValue: this["$param.key"].toInteger(), parameterNumber: pnum, size: param.size)
    }
  }
  runCommandsWithInterstitialDelay commands
}

void refresh() {
  logDebug 'refresh()'
  List<hubitat.zwave.Command> commands=[
    zwave.meterV4.meterGet(scale: 0x00),
    zwave.meterV4.meterGet(scale: 0x02),
    zwave.meterV4.meterGet(scale: 0x04),
    zwave.meterV4.meterGet(scale: 0x05),
    zwave.switchBinaryV1.switchBinaryGet()
  ]
  runCommandsWithInterstitialDelay commands 
}

void on() {
  logDebug 'on()'
  runCommand zwave.basicV1.basicSet(value: 0xFF)
//  runIn(3, 'refresh')
}

void off() {
  logDebug 'off()'
  runCommand zwave.basicV1.basicSet(value: 0x00)
//  runIn(2, 'refresh')
}

void resetPower() {
  logDebug 'resetPower()'
  runCommand zwave.meterV2.meterReset()
  device.deleteCurrentState("powerHigh")
  device.deleteCurrentState("powerLow")
  device.deleteCurrentState("amperageHigh")
  device.deleteCurrentState("amperageLow")
  device.deleteCurrentState("voltageHigh")
  device.deleteCurrentState("voltageLow")
  runIn (5, 'refresh')
}

void startStopBlinking (duration) {
  logDebug "stopBlinking( ${duration} )"
  runCommand zwave.configurationV1.configurationSet(parameterNumber: 19, size: 2, scaledConfigurationValue: duration)
}

void resetDevice (flag) {
  logDebug "resetDevice( ${flag} )"
  if (flag != "Please select") {
    Integer value = flag == "Factory Reset (Removes switch from network!)" ? 0x55555555 : 0x00
    runCommand zwave.configurationV1.configurationSet(parameterNumber: 255, size: 4, scaledConfigurationValue: value)
  }  
}

// Request parameters from device.
private void getConfig() {
    logDebug 'getConfig()'
   	List<hubitat.zwave.Command> commands = []
    parameterMap.eachWithIndex {pnum, param, i ->
      if ( this["$param.key"] != null && state."$param.key".toString() != this["$param.key"].toString() ) {
          commands << zwave.configurationV1.configurationGet(parameterNumber: pnum)
      } 
    }
    commands << zwave.versionV2.versionGet()
    commands << zwave.clockV1.clockGet()
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

void zwaveEvent(hubitat.zwave.commands.meterv4.MeterReport cmd) {
  logDebug "zwaveEvent(MeterReport) - cmd: ${cmd.inspect()}"
  updateReports(cmd)
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  logDebug "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}"
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off", type: "digital")
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
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

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
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
void zwaveEvent(hubitat.zwave.commands.versionv2.VersionCommandClassReport  cmd) {
  logDebug "zwaveEvent(VersionCommandClassReport ) - cmd: ${cmd.inspect()}"
}

void zwaveEvent(hubitat.zwave.commands.notificationv4.NotificationReport cmd) {
  logDebug "zwaveEvent(NotificationReport) - cmd: ${cmd.inspect()}"
  switch (cmd.notificationType) {
    case 0x08: // Power management notification
      switch (cmd.event) {
        case 0x00:
          sendEventWrapper(name: "powerManagement", descriptionText: "Idle", value: 0x00)
          break
        case 0x06:
          sendEventWrapper(name: "powerManagement", descriptionText: "Over-current detected", value: 0x06)
          break
        case 0x08:
          sendEventWrapper(name: "powerManagement", descriptionText: "Over-load detected", value: 0x08)
          break
        default:
          if (cmd.event) logWarn "Unhandled power notifcation event: ${cmd.event}"
      }
    case 0x09: // System
      switch  (cmd.event) {
        case 0x00:
          sendEventWrapper(name: "system", descriptionText: "Idle", value: 0x00)
          break
        case 0x03:
          sendEventWrapper(name: "system", descriptionText: "System hardware failure", value: 0x03)
          break
        default:
          if (cmd.event) logWarn "Unhandled system notifcation event: ${cmd.event}"
      }
    default:
      if (cmd.event) logWarn "Unhandled notifcation tpe: ${cmd.event}"
  }
}

void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  logDebug "zwaveEvent(FirmwareMdReport) - cmd: ${cmd.inspect()}"
  logInfo "Starting firmware update process..."
}

void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareUpdateMdRequestReport cmd) {
  logDebug "zwaveEvent(FirmwareUpdateMdRequestReport) - cmd: ${cmd.inspect()}"
  if (cmd.status == 255) {
    logInfo "Valid firmware for device. Firmware update continuing..."
  } else {
    logErr "Invalid firmware for device, error code ${cms.status}"
  }
}

void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareUpdateMdStatusReport cmd) {
  logDebug "zwaveEvent(FirmwareUpdateMdStatusReport) - cmd: ${cmd.inspect()}"
  if (cmd.status == 255) {
    logInfo "Firmware update succesfully completed."
  } else {
    logErr "Error updating firmware for device, error code ${cmd.status}"
  }
}

void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareUpdateMdGet  cmd) {
  logDebug "zwaveEvent(FirmwareUpdateMdGet ) - cmd: ${cmd.inspect()}"
}

void zwaveEvent(hubitat.zwave.commands.protectionv1.ProtectionReport cmd) {
  logDebug "zwaveEvent(ProtectionReport) - cmd: ${cmd.inspect()}"
}

// Correct device clock when off.
void zwaveEvent(hubitat.zwave.commands.clockv1.ClockReport cmd) {    
  logDebug "zwaveEvent(ClockReport) - cmd: ${cmd.inspect()}"
    
  def now = Calendar.instance
  def dayOfWeek = now.get(Calendar.DAY_OF_WEEK) - 1
  if (dayOfWeek == 0) dayOfWeek = 7
  if(cmd.weekday != dayOfWeek || cmd.hour != now.get(Calendar.HOUR_OF_DAY) || cmd.minute != now.get(Calendar.MINUTE)) {
    runCommand zwave.clockV1.clockSet(hour: now.get(Calendar.HOUR_OF_DAY), minute: now.get(Calendar.MINUTE), weekday: dayOfWeek)
    logInfo "Updating device clock settings due to mismatch: was ${cmd.weekday}, ${cmd.hour}:${cmd.minute}; set to ${dayOfWeek}, ${now.get(Calendar.HOUR_OF_DAY)}:${now.get(Calendar.MINUTE)}"
  } else {
    logDebug "Device clock settings are correct: ${cmd.weekday}, ${cmd.hour}:${cmd.minute}"
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

// ====== Event handler wrapper and logging START ====== 
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

// ===== Support functions START ======
void updateReports(cmd) {
  String name
  String unit
  String label
  Double maxVal = -1
  Double minVal = 0
  def val = cmd.scaledMeterValue

  if (cmd.meterType == 1) {
    switch (cmd.scale) {
      case 0:
        name = 'energy'
        unit = 'kWh'
        label = 'consumed'
        break
      case 1:
        name = 'energy'
        unit = 'kVah'
        label = 'consumed'
        break
      case 2:
        name = 'power'
        unit = 'W'
        label = 'consumes'
        maxVal = 4000
        minVal = -4000
        break
      case 3:
        name = 'frequency'
        unit = 'Hz'
        label = 'level'
        maxVal = 70
        minVal = 40
        break
      case 4:
        name = 'voltage'
        unit = 'V'
        label = 'level'
        maxVal = 260
        minVal = 90
        break
      case 5:
        name = 'amperage'
        unit = 'A'
        label = 'consumes'
        maxVal = 20
        minVal = -20
        break
      default:
        logWarn "Unsupported scale. Skipped cmd: ${cmd}"
      }
  }
  else {
    logDebug "Unsupported MeterType. Skipped cmd: ${cmd}"
  }
  if (name) {
    if (name != "energy") {
      if (val > minVal && val < maxVal) {
        sendEventWrapper(name: name, value: val, unit: unit, descriptionText: "${label} ${val} ${unit}")
        if (val != 0){
          createMeterHistoryEvents(name, val, unit, true)
          createMeterHistoryEvents(name, val, unit, false)
        }
      }
    } else {
      sendEventWrapper(name: name, value: val, unit: unit, descriptionText: "${label} ${val} ${unit}")
    }  
  }
}

// See if High/Low attributes needs an update.
void createMeterHistoryEvents(String mainName, mainVal, String unit, Boolean lowEvent) {
  String name = "${mainName}${lowEvent ? 'Low' : 'High'}"
  def val = device.currentValue("${name}")
  if ((val == null) || (val == 0) || (lowEvent && (mainVal < val)) || (!lowEvent && (mainVal > val))) {
    sendEventWrapper(name:name, value: mainVal, unit:unit)
  }
}

// Converts a list of String type node id values to Integer type.
private convertStringListToIntegerList(stringList) {
  logDebug "${stringList}"
  if (stringList != null) {
    for (int i = 0; i < stringList.size(); i++) {
      stringList[i] = stringList[i].toInteger()
    }
  }
  return stringList
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
