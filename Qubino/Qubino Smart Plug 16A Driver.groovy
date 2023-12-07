/**
 *  Qubino Smart Plug 16A ZMNHYDx
 *  Device Handler
 *  Version 1.0
 *  Date: 07.12.2023
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
 * Protect powerHigh/Low values agains impossible readings. Reset values on resetPower
 *
 *  CHANGELOG:
 *  0.01: First release
 *  0.02: Added default for preferences, removed debug messages when debug is off.
 *  0.03: Added power & amperage refresh after switch off.
 *  0.04: Added html formatted attribute to display W, V, A and KWh in one tile
 *  0.05: Fixed refreshPowerConsumption. Only updating tile on power value change to reduce events.
 *  0.06: Added finger print and associations.
 *  0.10: Added secure mode support. Some rewrites.
 *  0.11: Added High/Low states
 *  1.0 : Complete rewrite. Added S2 Securyty support.
 */
import groovy.transform.Field

@Field String VERSION = "1.0"

metadata {
  definition(name: 'Qubino Smart Plug 16A', namespace: "reneboer", author: "Rene Boer", importUrl: "https://github.com/reneboer/Hubitat/blob/main/Qubino/Qubino%20Smart%20Plug%2016A%20Driver.groovy") {
    capability 'Actuator'
    capability 'Switch'
    capability 'PowerMeter'
    capability 'EnergyMeter'
    capability 'VoltageMeasurement'
    capability 'CurrentMeter'
    capability 'Sensor'
    capability 'Configuration' //Needed for configure() function to set any specific configurations
    capability "Refresh"

    attribute 'htmlTile', 'string'  // To display all readings in one tile.
    attribute  'amperageHigh', 'number'
    attribute  'amperageLow', 'number'
        // attribute  'energyDuration', 'number'
    attribute  'powerHigh', 'number'
    attribute  'powerLow', 'number'
    attribute  'voltageHigh', 'number'
    attribute  'voltageLow', 'number'

    command 'resetPower' //command to issue Meter Reset commands to reset accumulated pwoer measurements
    command 'setAssociation' //command to issue Association Set commands to the modules according to user preferences

    fingerprint mfr:'0159', prod:'0002', deviceId:'0054', inClusters:'0x5E,0x25,0x85,0x59,0x55,0x86,0x72,0x5A,0x70,0x32,0x71,0x73,0x9F,0x6C,0x7A'
  }
  preferences {
    configParams.each { input it.value.input }
    input name: 'assocGroup2', type: 'text', required: false, title: 'Association group 2: \nBasic on/off.'
    input name: 'assocGroup3', type: 'text', required: false, title: 'Association group 3: \nPlug Threshold'
    input name: "tileEnable", type: "bool", title: "Create HTML Tile", description: "If true an HTML tile is created with power, current, energy and voltage.", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
  }  
}

@Field static Map CMD_CLASS_VERS = [
  0x70: 1, // COMMAND_CLASS_CONFIGURATION_V1 
  0x59: 2, // COMMAND_CLASS_ASSOCIATION_GRP_INFO_V2
  0x85: 2, // COMMAND_CLASS_ASSOCIATION_V2 
  0x71: 5, // COMMAND_CLASS_NOTIFICATION_V5 
  0x32: 4, // COMMAND_CLASS_METER_V4
  0x25: 2, // COMMAND_CLASS_SWITCH_BINARY_V2
  0x73: 1, // COMMAND_CLASS_POWERLEVEL_V1
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
  0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY_V1
  0x86: 2, // COMMAND_CLASS_VERSION_V2 
  0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO_V2
  0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE_V2
  0x9F: 1, // COMMAND_CLASS_SECURITY_2_V1
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1
  0x7A: 4  // COMMAND_CLASS_FIRMWARE_UPDATE_MD_V4
]

@Field static Map configParams = [
  11: [input: [name: "configParam11", 
          title: "Automatic turning off", 
          description:"Time for the dimmer to turn off automatically after turning it on.<br/>0 Disabled, 1-32536", 
          type: "number", 
          defaultValue: 0,
          range: "0..32536", 
          required: true
        ],
        parameterSize: 2],
  12: [input: [name: "configParam12", 
          title: "Automatic turning on", 
          description:"Time for the dimmer to turn on automatically after turning it off.<br/>0 Disabled, 1-32536", 
          type: "number", 
          defaultValue: 0,
          range: "0..32536", 
          required: true
        ], 
        parameterSize: 2],
  30: [input: [name: "configParam30", 
          title: "Restore on/off status after a power failure", 
          description:"Determines if on/off status is saved and restored after power failure.", 
          type: "enum",
          defaultValue: 0, 
          options:[0:"Return to state prior to power failure", 1:"Set off after power failure"], 
          required: true
        ],
        parameterSize: 1],
  40: [input: [name: "configParam40", 
          title: "Power reporting on power change", 
          description:"How much power consumption needs to increase or decrease to be reported.<br/>0 Disabled, 1-100%", 
          type: "number", 
          defaultValue: 20,
          range: "0..100", 
          required: true
        ], 
        parameterSize: 1],
  42: [input: [name: "configParam42",
          title: "Power reporting by time interval", 
          description:"The time interval with which power consumption in Watts is reported.<br/>0 Disabled, 30-32767s", 
          type: "number", 
          defaultValue: 0,
          range: "0..32767", 
          required: true
        ],
        parameterSize: 2],
  50: [input: [name: "configParam50", 
          title: "Down value", 
          description:"Lower power threshold used in parameter no. 52.", 
          type: "number", 
          defaultValue: 30, 
          range: "0..4000", 
          required: true
        ], 
        parameterSize: 2],
  51: [input: [name: "configParam51", 
          title: "Up value", 
          description:"Upper power threshold used in parameter no. 52.", 
          type: "number", 
          defaultValue: 50, 
          range: "0..4000", 
          required: true
        ], 
        parameterSize: 2],
  52: [input: [name: "configParam52", 
          type: "enum",
          title: "Action in case of exceeding Lower/Upper power values", 
          description:"Defines the way 3rd association group devices are controlled, depending on the current power load.", 
          defaultValue: 6, 
          options:[0:"function inactive", 1:"1 - turn the associated devices on, once the power drops below Down value", 2:"2 - turn the associated devices off, once the power drops below Down value", 3:"3 - turn the associated devices on, once the power rises above Up value",
                   4:"4 - turn the associated devices off, once the power rises above Up value", 5:"1 and 4 combined", 6:"2 and 3 combined"],
          required: true
        ], 
        parameterSize: 1],
  70: [input: [name: "configParam70", 
          title: "Overload safety switch", 
          description:"Turn off in case of exceeding the defined power for more than 3 seconds. 0 = inactive.", 
          type: "number", 
          defaultValue: 0, 
          range: "0..4000", 
          required: true
        ], 
        parameterSize: 2],
  71: [input: [name: "configParam71", 
          title: "Time interval", 
          description:"Set the power threshold for triggering the Program started notification. 0 = inactive.", 
          type: "number", 
          defaultValue: 0, 
          range: "0..4000", 
          required: true
        ], 
        parameterSize: 2],
  72: [input: [name: "configParam72", 
          title: "Power threshold", 
          description:"Set the time interval for triggering the Program completed notification.", 
          type: "number", 
          defaultValue: 1, 
          range: "0..125", 
          required: true
        ], 
        parameterSize: 1],
  73: [input: [name: "configParam73", 
          title: "Turn Smart Plug OFF", 
          description:"Turn the output to OFF once the time interval is expired and the Program completed notification is sent.", 
          type: "enum",
          defaultValue: 0, 
          options:[0:"function disabled", 1:"turn OFF"], 
          required: true
        ],
        parameterSize: 1],
  74: [input: [name: "configParam74", 
          title: "Enable/disable LED", 
          description:"Enable or disable the Smart Plug LED.", 
          type: "enum",
          defaultValue: 1, 
          options:[0:"LED is disabled", 1:"LED is enabled"], 
          required: true
        ],
        parameterSize: 1]
]

/*
* Supported command classes
*/
private static Map getCommandClassVersions() {
    [
        0x7A: 4, // Firmware Update Md
        0x6C: 1, // Supervision
        0x9F: 1, // Command Class SecurityS2
        0x73: 1, // Powerlevel
        0x71: 5, // Notification
        0x32: 4, // Meter
        0x70: 1, // Configuration
        0x5A: 1, // Device Reset Locally
        0x72: 2, // Manufacturer Specific
        0x86: 2, // Version
        0x55: 2, // Transport Service
        0x59: 2, // Association Grp Info
        0x85: 2, // Association
        0x25: 2, // Switch Binary
        0x5E: 2, // Zwaveplus Info
    ]
}

//  --------    HANDLE COMMANDS SECTION    --------
void logsOff(){
  log.warn "debug logging disabled..."
  device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void installed() {
  log.info "installed(${VERSION})"
  runIn (10, refresh)  // Get current device config after installed.
}

void updated() {
  log.info "updated()"
  log.warn "debug logging is: ${logEnable == true}"
  log.warn "description logging is: ${txtEnable == true}"
  unschedule()
  if (logEnable) runIn(86400, logsOff)
  if (VERSION == null) { // Older version clear attributes we renamed
    device.deleteCurrentState("powerH")
    device.deleteCurrentState("powerL")
    device.deleteCurrentState("amperageH")
    device.deleteCurrentState("amperageL")
    device.deleteCurrentState("voltageHh")
    device.deleteCurrentState("voltageL")
  }
}

void refresh() {
  logger "info", "refresh()"
  List<hubitat.zwave.Command> cmds=[
    secureCmd(zwave.meterV4.meterGet(scale: 0x00)),
    secureCmd(zwave.meterV4.meterGet(scale: 0x02)),
    secureCmd(zwave.meterV4.meterGet(scale: 0x04)),
    secureCmd(zwave.meterV4.meterGet(scale: 0x05))
  ]
  configParams.each { param, data ->
    cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: param.toInteger())))
  }
  sendCommands(cmds, 500)
}

void configure() {
  logger("info", "configure()")
  List<hubitat.zwave.Command> cmds=[
    supervisionEncap(zwave.associationV2.associationRemove(groupingIdentifier:1)),
    supervisionEncap(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId))
  ]
  configParams.each { param, data ->
    if (settings[data.input.name] != null) {
      cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name]))
    }
  }
  if (!device.getDataValue("MSR")) {
    cmds.add(secureCmd(zwave.versionV2.versionGet()))
    cmds.add(secureCmd(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
  }
  runIn (cmds.size() * 2, refresh)
  sendCommands(cmds, 1000)
}

private List<String> configCmd(parameterNumber, size, Boolean boolConfigurationValue) {
  return [
    supervisionEncap(zwave.configurationV1.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: boolConfigurationValue ? 1 : 0))//,
//    secureCmd(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber.toInteger()))
  ]
}

private List<String> configCmd(parameterNumber, size, value) {
  List<Integer> confValue = []
  value = value.toInteger()
  switch(size) {
    case 1:
      confValue = [value.toInteger()]
      break
    case 2:
      short value1   = value & 0xFF
      short value2 = (value >> 8) & 0xFF
      confValue = [value2.toInteger(), value1.toInteger()]
      break
    case 3:
      short value1   = value & 0xFF
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
  return [
     supervisionEncap(zwave.configurationV1.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), configurationValue: confValue))//,
//     secureCmd(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber.toInteger()))
  ]
}

void on() {
  logger("debug", "on()")
  sendCommands(supervisionEncap(zwave.basicV1.basicSet(value: 0xFF)))
}

void off() {
  logger("debug", "off()")
  sendCommands(supervisionEncap(zwave.basicV1.basicSet(value: 0x00)))
}

void setAssociation() {
  logger("debug", "setAssociation()")

  def assocSet = []
  def associationGroups = 3
  for (int i = 2; i <= associationGroups; i++) {
    if (settings."assocGroup${i}" != null) {
      logger("debug", "associationSet(groupingIdentifier:${i})")
      def groupparsed = settings."assocGroup${i}".tokenize(',')
      if (groupparsed == null) {
        assocSet << supervisionEncap(zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:settings."assocGroup${i}"))
      } else {
        groupparsed = convertStringListToIntegerList(groupparsed)
        assocSet << supervisionEncap(zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:groupparsed))
      }
    } else {
      logger("debug", "associationRemove(groupingIdentifier:${i})")
      assocSet << supervisionEncap(zwave.associationV2.associationRemove(groupingIdentifier:i))
    }
  }
  if (assocSet.size() > 0) {
     sendCommands(assocSet)
  }
}

void resetPower() {
  logger("debug", "resetPower()")
  runIn (10, refresh)
  sendCommands(supervisionEncap(zwave.meterV2.meterReset()))
  device.deleteCurrentState("powerHigh")
  device.deleteCurrentState("powerLow")
  device.deleteCurrentState("amperageHigh")
  device.deleteCurrentState("amperageLow")
  device.deleteCurrentState("voltageHigh")
  device.deleteCurrentState("voltageLow")
}

/*
*	--------	EVENT PARSER SECTION	--------
*/
void parse(String description) {
  logger("debug", "parse() - description: ${description.inspect()}")
  hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
  if (cmd) {
    logger("debug", "parse() - parsed to cmd: ${cmd?.inspect()} with result: ${result?.inspect()}")
    zwaveEvent(cmd)
  } else {
    logger("error", "parse() - Non-parsed - description: ${description?.inspect()}")
  }
}

void zwaveEvent(hubitat.zwave.Command cmd, ep = 0) {
  logger("warn", "zwaveEvent(Command) - No specific handler - cmd: ${cmd.inspect()}")
}

void zwaveEvent(hubitat.zwave.commands.meterv4.MeterReport cmd) {
  logger("trace", "zwaveEvent(MeterReport) - cmd: ${cmd.inspect()}")
  updateReports(cmd)
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd) {
  logger("trace", "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}")
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off")
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
  logger("trace", "zwaveEvent(ConfigurationReport) - cmd: ${cmd.inspect()}")
  def newVal = cmd.scaledConfigurationValue.toInteger()
  Map param = configParams[cmd.parameterNumber.toInteger()]
  if (param) {
    def curVal
    try {
      curVal = device.getSetting(param.input.name).toInteger()
    }catch(Exception ex) {
       logger ("warn", "Undefined parameter ${curVal}.")
       curVal = null
    }
    Long sizeFactor = Math.pow(256,cmd.size).round()
	  if (newVal < 0) { newVal += sizeFactor }
    if (curVal != newVal) {
      if (param.input.type == "enum") { newVal = newVal.toString()}
      if (param.input.type == "bool") { newVal = newVal == 0 ? "false": "true"}
      device.updateSetting(param.input.name, [value: newVal, type: param.input.type])
      logger("debug", "Updating device parameter setting ${cmd.parameterNumber} from ${curVal} to ${newVal}.")
    }
  } else {
    logger ("warn", "Unsupported parameter ${cmd.parameterNumber}.")
  }
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logger("trace", "zwaveEvent(ManufacturerSpecificReport) - cmd: ${cmd.inspect()}")
  if (cmd.manufacturerName) { device.updateDataValue("manufacturer", cmd.manufacturerName) }
  if (cmd.productTypeId) { device.updateDataValue("productTypeId", cmd.productTypeId.toString()) }
  if (cmd.productId) { device.updateDataValue("deviceId", cmd.productId.toString()) }
  device.updateDataValue("MSR", String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId))
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
  logger("trace", "zwaveEvent(VersionReport) - cmd: ${cmd.inspect()}")
  device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
  device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
  device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void zwaveEvent(hubitat.zwave.commands.notificationv5.NotificationReport cmd) {
  logger("trace", "zwaveEvent(NotificationReport) - cmd: ${cmd.inspect()}")
  switch (cmd.notificationType) {
    case 0x08: // Power management notification
      switch (cmd.event) {
        case 0x05:
          sendEventWrapper(name: "powerManagement", descriptionText: "Voltage drop/drift", value: 0x05)
          break
        case 0x06:
          sendEventWrapper(name: "powerManagement", descriptionText: "Over-current detected", value: 0x06)
          break
        case 0x07:
          sendEventWrapper(name: "powerManagement", descriptionText: "Over-voltage detected", value: 0x07)
          break
        case 0x08:
          sendEventWrapper(name: "powerManagement", descriptionText: "Over-load detected", value: 0x08)
          break
        default:
          if (cmd.event) logger ("warn", "Unhandled power notifcation event: ${cmd.event}")
      }
    case 0x0C: // Applicance
      switch  (cmd.event) {
        case 0x01:
          sendEventWrapper(name: "applicance", descriptionText: "Program started", value: 0x01)
          break
        case 0x02:
          sendEventWrapper(name: "applicance", descriptionText: "Program in progress", value: 0x02)
          break
        case 0x03:
          sendEventWrapper(name: "applicance", descriptionText: "Program completed", value: 0x03)
          break
        default:
          if (cmd.event) logger ("warn", "Unhandled appliance notifcation event: ${cmd.event}")
      }
    default:
      if (cmd.event) logger ("warn", "Unhandled notifcation tpe: ${cmd.event}")
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

// ====== Event handler wrapper and logging START ====== 
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
  sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.ZWAVE))
}

//Secure and MultiChannel Encapsulate
String secureCmd(String cmd) {
  logger("debug", "secureCmd String(${cmd})")
  return zwaveSecureEncap(cmd)
}
String secureCmd(hubitat.zwave.Command cmd) {
  logger("debug", "secureCmd Command(${cmd})")
  return zwaveSecureEncap(cmd)
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
        if (tileEnable) runIn(1, 'updateTile')
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
        logger ("warn", "Unsupported scale. Skipped cmd: ${cmd}")
      }
  }
  else {
    logger("warn", "Unsupported MeterType. Skipped cmd: ${cmd}")
  }
  if (name) {
    if (val > minVal && val < maxVal) {
      sendEventWrapper(name: name, value: val, unit: unit, descriptionText: "${label} ${val} ${unit}")
      if (name != "energy" && val != 0){
        createMeterHistoryEvents(name, val, unit, true)
        createMeterHistoryEvents(name, val, unit, false)
      }
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

// Update summary tile for Dashboard.
private void updateTile(  ) {
  String val

  // Create special compound/html tile
  val = '<B>Power : </B> ' + device.currentValue('power') + ' W</BR><B>Amperage : </B> ' + device.currentValue('amperage').toString() + ' A</BR><B>Energy : </B> ' + device.currentValue('energy').toString() + ' KWh</BR><B>Voltage : </B> ' + device.currentValue('voltage').toString() + ' V'
  if (device.currentValue('htmlTile').toString() != val) {
    sendEvent(name: 'htmlTile', value: val)
  }
}

// Converts a list of String type node id values to Integer type.
private convertStringListToIntegerList(stringList) {
  logger ("debug", "${stringList}")
  if (stringList != null) {
    for (int i = 0; i < stringList.size(); i++) {
      stringList[i] = stringList[i].toInteger()
    }
  }
  return stringList
}

