/**
 *  Shelly Wave 1PM QNSW-0001P16EU
 *  Device Handler
 *  Version 1.1
 *  Date: 9.1.2024
 *  Author: Rene Boer
 *  Copyright , none free to use
 *
 * |---------------------------- DEVICE HANDLER FOR QUBINO SMART PLUG 16A Z-WAVE DEVICE -------------------------------------------------------|
 *    The handler supports all unsecure functions of the Qubino Smart Plug 16A device. Configuration parameters and
 *    association groups can be set in the device's preferences screen, but they are applied on the device only after
 *    pressing the 'Set configuration' and 'Set associations' buttons on the bottom of the details view.
 *
 *    This device handler supports data values that are currently not implemented as capabilities, so custom attribute
 *    states are used. Please use a SmartApp that supports custom attribute monitoring with this device in your rules.
 * |-----------------------------------------------------------------------------------------------------------------------------------------------|
 *
 *  TO-DO:
 *
 *  CHANGELOG:
 *  0.1: First release
 *  1.0: Added firmware targets to version report.
 *  1.1: Added reboot function. Use for troubleshooting only. Moved setting device configuration parameters for update function. Removed Notification report, not officially supported. Added FirmwareUpdate report handler.  Fix for powerLow status value.
 */
import groovy.transform.Field

@Field String VERSION = "1.1"

metadata {
  definition(name: 'Shelly Wave 1PM', namespace: "reneboer", author: "Rene Boer", importUrl: "https://raw.githubusercontent.com/reneboer/Hubitat/main/Shelly/Shelly%20Wave%201PM%20Driver.groovy") {
    capability "Actuator"
    capability "Switch"
    capability "Power Meter"
    capability "EnergyMeter"
    capability "Configuration"
    capability "Refresh"

    attribute "powerHigh", "number"
    attribute "powerLow", "number"

    command "resetPower" //command to issue Meter Reset commands to reset accumulated power measurements
    command "remoteReboot", [[name: "Reboot device*", type: "ENUM", description: "Reboot the device. Use for troubleshooting only.", default: "Please select", constraints: ["Please select", "Do nothing", "Perform reboot"]]] // Send remote reboot command to device

    fingerprint mfr: "1120", prod: "2", deviceId: "132", inClusters: "0x5E,0x98,0x9F,0x55,0x6C", secureInClusters: "0x86,0x73,0x87,0x7A,0x5A,0x8E,0x59,0x85,0x70,0x25,0x71,0x32,0x72", deviceJoinName: "Shelly Wave 1PM"
    fingerprint mfr: "1120", prod: "2", deviceId: "132", inClusters: "0x5E,0x98,0x9F,0x55,0x86,0x6C,0x73,0x87,0x7A,0x5A,0x8E,0x59,0x85,0x70,0x25,0x71,0x32,0x72", deviceJoinName: "Shelly Wave 1PM"
  }

  preferences {
    configParams.each { input it.value.input }
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
  }
}

@Field static Map CMD_CLASS_VERS = [
  0x85: 2, // COMMAND_CLASS_ASSOCIATION_V2
  0x59: 3, // COMMAND_CLASS_ASSOCIATION_GRP_INFO_V3
//  0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY_V1
  0x7A: 5, // COMMAND_CLASS_FIRMWARE_UPDATE_MD_V5
//  0x87: 3, // COMMAND_CLASS_INDICATOR_V3,
//  0x71: 8, // COMMAND_CLASS_ALARM_V8
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
  0x8E: 3, // COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
  0x73: 1, // COMMAND_CLASS_POWER_LEVEL_V1
  0x98: 1, // COMMAND_CLASS_SECURITY_V1
  0x9F: 1, // COMMAND_CLASS_SECURITY_2_V1
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1
  0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE_V2
  0x86: 3, // COMMAND_CLASS_VERSION_V3
  0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO_V2
  0x20: 2, // COMMAND_CLASS_BASIC_V2
  0x70: 4, // COMMAND_CLASS_CONFIGURATION_V4
  0x32: 6, // COMMAND_CLASS_METER_V6
  0x25: 2  // COMMAND_CLASS_SWITCH_BINARY_V2
]
@Field static Map configParams = [
  1:  [input: [name: "configParam1", 
          title: "Switch type",
          description: "Defines how the Device should treat the switch (which type) connected to the SW (SW1) terminal.", 
          type: "enum", 
          defaultValue: 2,
          options:[0:"Momentary switch", 1:"Toggle switch (contact closed - ON / contact opened - OFF)", 2:"Toggle switch (device changes status when switch changes status)"],
          required: true
        ],
        parameterSize: 1],
  17: [input: [name: "configParam17", 
          title: "Action in case of power out", 
          description:"Determines if on/off status is saved and restored after power failure.", 
          type: "enum",
          defaultValue: 0, 
          options:[0:"Last status", 1:"Switch remains off"], 
          required: true
        ], 
        parameterSize: 1],
  19: [input: [name: "configParam19", 
          title: "Auto OFF with timer", 
          description:"If the load O (O1) is ON, you can schedule it to turn OFF automatically after the period of time.<br/>0 Disabled, 1-32535s", 
          type: "number",
          defaultValue: 0, 
          range: "0..32535", 
          required: false
        ], 
        parameterSize: 2],
  20: [input: [name: "configParam20", 
          title: "Auto ON with timer", 
          description:"If the load O (O1) is OFF, you can schedule it to turn ON automatically after the period of time.<br/>0 Disabled, 1-32535s", 
          type: "number",
          defaultValue: 0, 
          range: "0..32535", 
          required: false
        ], 
        parameterSize: 2],
//  23: [input: [name: "configParam23", 
//          title: "Contact type - NO/NC", 
//          description:"The set value determines the relay contact type for output O (O1). The relay contact type can be normally open (NO) or normally closed (NC).", 
//          type: "enum",
//          defaultValue: 0, 
//          options:[0:"Normal Open", 1:"Normal Closed"], 
//          required: false
//        ], 
//        parameterSize: 1],
//  25: [input: [name: "configParam25", 
//          title: "Set timer units to s or ms for O (O1)", 
//          description:"Set the timer in seconds or milliseconds in Parameters No. 19, 20.", 
//          type: "enum",
//          defaultValue: 0, 
//          options:[0:"Timer set in seconds", 1:"Timer set in milliseconds"], 
//          required: false
//        ], 
//        parameterSize: 1],
  36: [input: [name: "configParam36", 
          title: "Power report on change", 
          description:"The minimum change in consumed power that will result in sending a new report.<br/>0 Disabled, 1-100%", 
          type: "number",
          defaultValue: 50, 
          range: "0..100", 
          required: true
        ], 
        parameterSize: 1],
  39: [input: [name: "configParam39", 
          title: "Minimal time between reports", 
          description:"The minimum time that must elapse before a new power report.<br/>0 Disabled, 1-120s", 
          type: "number",
          defaultValue: 30, 
          range: "0..120", 
          required: true
        ], 
        parameterSize: 1],
  91: [input: [name: "configParam91", 
          title: "Water Alarm response", 
          description:"Determines if device should respond to a Water Alarm.", 
          type: "enum",
          defaultValue: 0, 
          options:[0:"No action", 1:"Open relay", 2:"Close relay"], 
          required: false, 
        ], 
        parameterSize: 4],
  92: [input: [name: "configParam92", 
          title: "Smoke Alarm response", 
          description:"Determines if device should respond to a Smoke Alarm.", 
          type: "enum",
          defaultValue: 0, 
          options:[0:"No action", 1:"Open relay", 2:"Close relay"], 
          required: false, 
        ], 
        parameterSize: 4],
  93: [input: [name: "configParam93", 
          title: "CO Alarm response", 
          description:"Determines if device should respond to a CO Alarm.", 
          type: "enum",
          defaultValue: 0, 
          options:[0:"No action", 1:"Open relay", 2:"Close relay"], 
          required: false, 
        ], 
        parameterSize: 4],
  94: [input: [name: "configParam94", 
          title: "Heat Alarm response", 
          description:"Determines if device should respond to a Heat Alarm.", 
          type: "enum",
          defaultValue: 0, 
          options:[0:"No action", 1:"Open relay", 2:"Close relay"], 
          required: false, 
        ], 
        parameterSize: 4]
]

void logsOff(){
  log.warn "debug logging disabled..."
  device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void installed() {
  log.info "installed(${VERSION})"
  runIn (10, refresh)  // Get current device config after installed.
}

void updated() {
  log.debug "updated()"
  log.warn "debug logging is: ${logEnable == true}"
  log.warn "description logging is: ${txtEnable == true}"
  unschedule()
  if (logEnable) runIn(3600, logsOff)
  List<hubitat.zwave.Command> cmds=[]
  configParams.each { param, data ->
  if (settings[data.input.name] != null) {
      cmds.add(configCmd(param, data.parameterSize, settings[data.input.name]))
    }
  }
  sendCommands(cmds, 500)
}

void refresh() {
  logger "info", "refresh()"
  List<hubitat.zwave.Command> cmds=[
    secureCmd(zwave.meterV6.meterGet(scale: 0x00)),
    secureCmd(zwave.meterV6.meterGet(scale: 0x02)),
    secureCmd(zwave.switchBinaryV2.switchBinaryGet())
  ]
  configParams.each { param, data ->
    cmds.add(secureCmd(zwave.configurationV4.configurationGet(parameterNumber: param.toInteger())))
  }
  sendCommands(cmds, 500)
}

void configure() {
  logger("debug", "configure()")

  List<hubitat.zwave.Command> cmds=[
    secureCmd(zwave.versionV3.versionGet())
  ]
  if (!device.getDataValue("MSR")) {
    cmds.add(secureCmd(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
  }
  runIn (cmds.size() * 2, refresh)
  sendCommands(cmds, 500)
}

private String configCmd(parameterNumber, size, Boolean boolConfigurationValue) {
  supervisionEncap(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: boolConfigurationValue ? 1 : 0))
}

private String configCmd(parameterNumber, size, String enumConfigurationValue) {
  supervisionEncap(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: enumConfigurationValue.toInteger()))
}

private String configCmd(parameterNumber, size, value) {
  List<Integer> confValue = []
  value = value.toInteger()
  switch(size) {
    case 1:
      confValue = [value.toInteger()]
      break
    case 2:
      short value1 = value & 0xFF
      short value2 = (value >> 8) & 0xFF
      confValue = [value2.toInteger(), value1.toInteger()]
      break
    case 3:
      short value1 = value & 0xFF
      short value2 = (value >> 8) & 0xFF
      short value3 = (value >> 16) & 0xFF
      confValue = [value3.toInteger(), value2.toInteger(), value1.toInteger()]
      break
    case 4:
      short value1 = value & 0xFF
      short value2 = (value >> 8) & 0xFF
      short value3 = (value >> 16) & 0xFF
      short value4 = (value >> 24) & 0xFF
      confValue = [value4.toInteger(), value3.toInteger(), value2.toInteger(), value1.toInteger()]
      break
  }
  supervisionEncap(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), configurationValue: confValue))
}

void on() {
  logger("debug", "on()")
  sendCommands(supervisionEncap(zwave.basicV2.basicSet(value: 0xFF)))
}

void off() {
  logger("debug", "off()")
  sendCommands(supervisionEncap(zwave.basicV2.basicSet(value: 0x00)))
}

void resetPower() {
  logger("debug", "resetPower()")
  runIn (10, refresh)
  sendCommands(supervisionEncap(zwave.meterV6.meterReset()))
  device.deleteCurrentState("powerHigh")
  device.deleteCurrentState("powerLow")
}

void remoteReboot(flag) {
  logger("debug", "remoteReboot(${flag})")
  if (flag == "Perform reboot") {
    logger ("warn", "Rebooting device.")
    sendCommands(supervisionEncap(zwave.configurationV1.configurationSet(parameterNumber: 117, size: 1, scaledConfigurationValue: 1)))
  }
}

/**
* Incomming zwave event handlers.
*/
void parse(String description) {
//  logger("debug", "parse() - description: ${description.inspect()}")
  hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
  if (cmd) {
    logger("debug", "parse() - parsed to cmd: ${cmd?.inspect()} with result: ${result?.inspect()}")
    zwaveEvent(cmd)
  } else {
    logger("error", "parse() - Non-parsed - description: ${description?.inspect()}")
  }
}

// Handle zwave events not expected
void zwaveEvent(hubitat.zwave.Command cmd) {
  logger("warn", "zwaveEvent(Command) - Unspecified - cmd: ${cmd.inspect()}")
}

void zwaveEvent(hubitat.zwave.commands.meterv6.MeterReport cmd) {
  logger("trace", "zwaveEvent(MeterReport) - cmd: ${cmd.inspect()}")
  switch(cmd.scale){
    case 0x00:
      sendEventWrapper(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"consumed ${cmd.scaledMeterValue} kWh")
      break;
    case 0x02:
      def val = cmd.scaledMeterValue
      sendEventWrapper(name:"power", value: val, unit:"W", descriptionText:"consumes ${val} W")
      // Update powerHigh/Low values when with in expected ranges
      if (val > 0 && val <= 4000) {
        def valLow = device.currentValue("powerLow")
        def valHigh = device.currentValue("powerHigh")
        if (val > valHigh || valHigh == null) sendEventWrapper(name:"powerHigh", value: val, unit:"W", descriptionText:"Highest power level is ${val} W.")
        if (val < valLow || valLow == null || valLow == 0.0) sendEventWrapper(name:"powerLow", value: val, unit:"W", descriptionText:"Lowest power level is ${val} W when on.")
      }
      break;
    default:
      logger("warn", "zwaveEvent(MeterReport) - skipped: ${cmd.inspect()}")
    }    
}

void zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport cmd){
  logger("trace", "zwaveEvent(ConfigurationReport) - cmd: ${cmd.inspect()}. No action.")
  def newVal = cmd.scaledConfigurationValue.toInteger()
  Map param = configParams[cmd.parameterNumber.toInteger()]
  if (param) {
    def curVal = device.getSetting(param.input.name)
    if (param.input.type == "bool") { curVal = curVal == false ? 0 : 1}
    try {
      curVal = curVal.toInteger()
    }catch(Exception ex) {
       logger ("warn", "Undefined parameter ${curVal}.")
       curVal = null
    }
    Long sizeFactor = Math.pow(256,cmd.size).round()
	  if (newVal < 0) { newVal += sizeFactor }
    if (curVal != newVal) {
      if (param.input.type == "enum") { newVal = newVal.toString()}
      if (param.input.type == "bool") { newVal = newVal == 0 ? false: true}
      device.updateSetting(param.input.name, [value: newVal, type: param.input.type])
      logger("debug", "Updating device parameter setting ${cmd.parameterNumber} from ${curVal} to ${newVal}.")
    }
  } else {
    logger ("warn", "Unsupported parameter ${cmd.parameterNumber}.")
  }
}

void zwaveEvent(hubitat.zwave.commands.basicv2.BasicReport cmd){
  logger("trace", "zwaveEvent(BasicReport) - cmd: ${cmd.inspect()}. No action.")
  sendEventWrapper(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd){
  logger("trace", "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}. No action.")
  sendEventWrapper(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logger("trace", "zwaveEvent(ManufacturerSpecificReport) - cmd: ${cmd.inspect()}")
  if (cmd.manufacturerName) { device.updateDataValue("manufacturer", cmd.manufacturerName) }
  if (cmd.productTypeId) { device.updateDataValue("productTypeId", cmd.productTypeId.toString()) }
  if (cmd.productId) { device.updateDataValue("deviceId", cmd.productId.toString()) }
  device.updateDataValue("MSR", String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId))
}

void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv5.FirmwareMdReport cmd) {
  logger("trace", "zwaveEvent(FirmwareMdReport) - cmd: ${cmd.inspect()}")
  logger ("debug", "Starting firmware update process...")
}

void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv5.FirmwareUpdateMdRequestReport cmd) {
  logger("trace", "zwaveEvent(FirmwareUpdateMdRequestReport) - cmd: ${cmd.inspect()}")
  if (cmd.status == 255) {
    logger ("debug", "Valid firmware for device. Firmware update continuing...")
  } else {
    logger ("warn", "Invalid firmware for device, error code ${cms.status}")
  }
}

void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv5.FirmwareUpdateMdStatusReport cmd) {
  logger("trace", "zwaveEvent(FirmwareUpdateMdStatusReport) - cmd: ${cmd.inspect()}")
  if (cmd.status == 255) {
    logger ("debug", "Firmware update succesfully completed.")
  } else {
    logger ("warn", "Error updating firmware for device, error code ${cmd.status}")
  }
}

void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv5.FirmwareUpdateMdGet  cmd) {
// Do nothing as there are a huge number of reports and we do not want to flud the logs.
//  logger("trace", "zwaveEvent(FirmwareMdReport ) - cmd: ${cmd.inspect()}")
}

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
  logger("trace", "zwaveEvent(VersionReport) - cmd: ${cmd.inspect()}")
  device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
  device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
  device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
  if (cmd.firmwareTargets > 0) {
    cmd.targetVersions.each { target ->
      device.updateDataValue("firmware${target.target}Version", "${target.version}.${target.subVersion}")
    }
  }
}

// Devices that support the Security command class can send messages in an encrypted form; they arrive wrapped in a SecurityMessageEncapsulation command and must be unencapsulated 
void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
  logger("trace", "zwaveEvent(SecurityMessageEncapsulation) - cmd: ${cmd.inspect()}")
  hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
  if (encapsulatedCommand) {
    logger("trace", "zwaveEvent(SecurityMessageEncapsulation) - encapsulatedCommand: ${encapsulatedCommand}")
    zwaveEvent(encapsulatedCommand)
  } else {
    logger("warn", "zwaveEvent(SecurityMessageEncapsulation) - Unable to extract Secure command from: ${cmd.inspect()}")
  }
}

// Handle S2 Supervision or device will think communication failed. Not sure it applies here. Just getting double events.
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
  logger("trace", "zwaveEvent(SupervisionGet) - cmd: ${cmd.inspect()}")
  hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
  if (encapsulatedCommand) {
    logger("trace", "zwaveEvent(SupervisionGet) - encapsulatedCommand: ${encapsulatedCommand}")
    zwaveEvent(encapsulatedCommand)
  } else {
    logger("error", "SupervisionGet - Non-parsed - description: ${description?.inspect()}")
  }
  sendCommands(secureCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)))
}

// Handle S2 Suportvision get. No multi channel support.
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionReport cmd) {
  logger("trace", "zwaveEvent(SupervisionReport) - cmd: ${cmd.inspect()}")
  if (!supervisedPackets."${device.id}") { supervisedPackets."${device.id}" = [:] }
  switch (cmd.status as Integer) {
    case 0x00: // "No Support"
    case 0x01: // "Working"
    case 0x02: // "Failed"
      logger("warn", "Supervision NOT Successful - SessionID: ${cmd.sessionID}, Status: ${cmd.status}")
      break
    case 0xFF: // "Success"
      if (supervisedPackets["${device.id}"][cmd.sessionID] != null) { supervisedPackets["${device.id}"].remove(cmd.sessionID) }
      break
  }
}

/**
* Wrapper for sendEvent to limit duplicate events and support logging
*/
private void sendEventWrapper(Map prop) {
  String cv = device.currentValue(prop.name)
  Boolean changed = (prop.isStateChange == true) || ((cv?.toString() != prop.value?.toString()) ? true : false)
  if (changed) sendEvent(prop)
  if (prop?.descriptionText) {
    if (txtEnable && changed) {
      log.info "${device.displayName} ${prop.descriptionText}"
    } else {
      logger("debug", "${prop.descriptionText}")
    }
  }
}

/**
 * @param level; Level to log at. Alwasy log errors and warnings.
 * @param msg; Message to log
 */
private logger(String level, String msg) {
  if (level == "error" || level == "warn") {
    log."${level}" "${device.displayName} ${msg}"
  } else{
    if (logEnable) log."${level}" "${device.displayName} ${msg}"
  }
}

// ====== Z-Wave send commands START ====== 
// Inspired by zooz drivers at https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz

//These send commands to the device either a list or a single command
void sendCommands(List<String> cmds, Long delay=200) {
  logger("debug", "sendCommands Commands($cmds), delay ($delay)")
  //Calculate supervisionCheck delay based on how many commands
  Integer packetsCount = supervisedPackets?."${device.id}"?.size()
  if (packetsCount > 0) {
    Integer delayTotal = (cmds.size() * delay) + 2000
    logger ("debug", "Setting supervisionCheck to ${delayTotal}ms | ${packetsCount} | ${cmds.size()} | ${delay}")
    runInMillis(delayTotal, supervisionCheck, [data:1])
   }
   //Send the commands
  sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, delay), hubitat.device.Protocol.ZWAVE))
}

//Single Command
void sendCommands(String cmd) {
  logger("debug", "sendCommands Command($cmd)")
  sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.ZWAVE))
}

//Secure and MultiChannel Encapsulate
String secureCmd(String cmd) {
//  logger("debug", "secureCmd String(${cmd})")
  return zwaveSecureEncap(cmd)
}
String secureCmd(hubitat.zwave.Command cmd) {
//  logger("debug", "secureCmd Command(${cmd})")
  return zwaveSecureEncap(cmd.format())
}

// ====== Supervision Encapsulate START ====== 
@Field static Map<String, Map<Short, String>> supervisedPackets = new java.util.concurrent.ConcurrentHashMap()
@Field static Map<String, Short> sessionIDs = new java.util.concurrent.ConcurrentHashMap()

String supervisionEncap(hubitat.zwave.Command cmd) {
  logger("trace", "supervisionEncap(): ${cmd}")
  if (getDataValue("S2")?.toInteger() != null) {
    //Encap with SupervisionGet
    Short sessId = getSessionId()
    def cmdEncap = zwave.supervisionV1.supervisionGet(sessionID: sessId).encapsulate(cmd)
    logger("debug", "New Supervised Packet for Session: ${sessId}")
    if (supervisedPackets["${device.id}"] == null) { supervisedPackets["${device.id}"] = [:] }
    supervisedPackets["${device.id}"][sessId] = cmdEncap
    //Calculate supervisionCheck delay based on how many cached packets
    Integer packetsCount = supervisedPackets?."${device.id}"?.size()
    Integer delayTotal = (packetsCount * 500) + 2000
    runInMillis(delayTotal, supervisionCheck, [data:1])
    //Send back secured command
    return secureCmd(cmdEncap)
  } else {
    //If supervision disabled just multichannel and secure
    return secureCmd(cmd)
  }
}

Short getSessionId() {
  Short sessId = sessionIDs["${device.id}"] ?: state.lastSupervision ?: 0
  sessId = (sessId + 1) % 64  // Will always will return between 0-63
  state.lastSupervision = sessId
  sessionIDs["${device.id}"] = sessId
  return sessId
}

void supervisionCheck(Integer num) {
  Integer packetsCount = supervisedPackets?."${device.id}"?.size()
  logger("debug", "Supervision Check #${num} - Packet Count: ${packetsCount}")
  if (packetsCount > 0 ) {
    List<String> cmds = []
    supervisedPackets["${device.id}"].each { sid, cmd ->
      logger("warn",  "Re-Sending Supervised Session: ${sid} (Retry #${num})")
      cmds << secureCmd(cmd)
    }
    sendCommands(cmds)
    if (num >= 3) { //Clear after this many attempts
      logger("warn",  "Supervision MAX RETIES (${num}) Reached")
      supervisedPackets["${device.id}"].clear()
    } else { //Otherwise keep trying
      Integer delayTotal = (packetsCount * 500) + 2000
      runInMillis(delayTotal, supervisionCheck, [data:num+1])
    }
  }
}
// ====== Supervision Encapsulate END ======

