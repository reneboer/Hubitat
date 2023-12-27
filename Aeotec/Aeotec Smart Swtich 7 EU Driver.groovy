/**
 *  Aeotec Smart Switch 7 EU ZW175-C16
 *  Device Handler
 *  Version 0.1
 *  Date: 27.12.2023
 *  Author: Rene Boer
 *  Copyright , none free to use
 *
 * |---------------------------- DEVICE HANDLER FOR AEOTEC SMART SWITCH 7 F Z-WAVE DEVICE -------------------------------------------------------|
 *    The handler supports all functions of the Aeotec Smart Smart Switch 7 device, EU version. Configuration parameters and
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
 *  0.01: First release
 */
import groovy.transform.Field

@Field String VERSION = "0.1"

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
    capability 'Configuration' //Needed for configure() function to set any specific configurations
    capability 'Refresh'

    attribute  'htmlTile', 'string'  // To display all readings in one tile.
    attribute  'amperageHigh', 'number'
    attribute  'amperageLow', 'number'
        // attribute  'energyDuration', 'string'
    attribute  'powerHigh', 'number'
    attribute  'powerLow', 'number'
    attribute  'voltageHigh', 'number'
    attribute  'voltageLow', 'number'

    command 'resetPower' //command to issue Meter Reset commands to reset accumulated power measurements
    command 'startStopBlinking', [[name: "Start or stop LED blinking*", type: "NUMBER", description: "0 = Stop blinking. 1-255s = Blinking duration."]] // Send Start/stop blink command to device
    command 'deviceReset', [[name: "Reset device*", type: "ENUM", description: "", constraints: [0:"Default configuration",1:"Factory Reset"]]] // Send Factory Reset or Initialization command to device
    command 'setAssociation' //command to issue Association Set commands to the modules according to user preferences

    fingerprint mfr:"0345", prod:"0002", deviceId: "0084", inClusters:"0x5E,0x55,0x22,0x98,0x9F,0x6C", secureInClusters: "0x85,0x59,0x70,0x2C,0x2B,0x81,0x71,0x32,0x25,0x33,0x26,0x75,0x73,0x7A,0x86,0x5A,0x72", deviceJoinName: "Aeotec Smart Switch 7", model:"Smart Switch 7", manufacturer:"Aeotec"
    fingerprint mfr:"0345", prod:"0002", deviceId: "0084", inClusters:"0x5E,0x85,0x59,0x55,0x70,0x2C,0x2B,0x81,0x71,0x32,0x25,0x33,0x26,0x86,0x72,0x5A,0x22,0x75,0x73,0x98,0x9F,0x6C,0x7A", deviceJoinName: "Aeotec Smart Switch 7", model:"Smart Switch 7", manufacturer:"Aeotec"
  }
  preferences {
    configParams.each { input it.value.input }
    input name: 'assocGroup2', type: 'text', required: false, title: 'Association group 2: \nRetransmit Basic Set, Binary Switch Set or Scene Activation Set.'
    input name: "tileEnable", type: "bool", title: "Create HTML Tile", description: "If true an HTML tile is created with power, current, energy and voltage.", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
  }  
}

// When commented out, there is no specific handler routine
@Field static Map CMD_CLASS_VERS = [
  0x70: 1, // COMMAND_CLASS_CONFIGURATION_V1 
//  0x59: 1, // COMMAND_CLASS_ASSOCIATION_GRP_INFO_V1
  0x85: 2, // COMMAND_CLASS_ASSOCIATION_V2 
  0x71: 4, // COMMAND_CLASS_NOTIFICATION_V4
  0x32: 4, // COMMAND_CLASS_METER_V4
  0x25: 1, // COMMAND_CLASS_SWITCH_BINARY_V1
  0x73: 1, // COMMAND_CLASS_POWERLEVEL_V1
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
//  0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY_V1
  0x86: 2, // COMMAND_CLASS_VERSION_V2 
//  0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO_V2
//  0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE_V2
  0x9F: 1, // COMMAND_CLASS_SECURITY_2_V1
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1
//  0x7A: 4, // COMMAND_CLASS_FIRMWARE_UPDATE_MD_V4
//  0x2C: 1, // COMMAND_SCENE_ACTUATOR_CONF_V1
//  0x2B: 1, // COMMAND_SCENE_ACTIVATION_V1
//  0x81: 1, // COMMAND_CLOCK_V1
//  0x33: 1, // COMMAND_SWITCH_COLOR_V1
  0x26: 2, // COMMAND_SWITCH_MULTI_LEVEL_V2
//  0x22: 1, // COMMAND_APPLICATIPON_STATUS_V1
//  0x75: 2, // COMMAND_PROTECTION_V2
  0x98: 1  // COMMAND_SECURITY_V1
]

@Field static Map configParams = [
  4: [input: [name: "configParam4", 
          title: "Over-load protection", 
          description:"Define a threshold power and automatically turn off switch when the load connected bypasses the maximum allowed power regardless of always on setting.<br/>0 Disabled, 1-2415W", 
          type: "number", 
          defaultValue: 2415,
          range: "0..2415", 
          required: true
        ],
        parameterSize: 2],
  8: [input: [name: "configParam8", 
          title: "Alarm Response", 
          description:"Enabled by (Alarm Settings), and determines what the switch does in the case an alarm is triggered.", 
          type: "emun", 
          defaultValue: 0,
          options:[0:"Disable, no reaction to alarm settings", 1:"Switch is ON", 2:"Switch is OFF", 3:"Switch will turn ON then turn OFF in a 10 sec cycle until user disables"], 
          required: true
        ], 
        parameterSize: 1],
// Need to think how to implement these.
//  9: [input: [name: "configParam9", 
//          title: "Alarm Settings", 
//          description:"Determine if alarms are enabled in Switch, and what Switch will react to which alarm.", 
//          type: "enum",
//          defaultValue: 0, 
//          options:[0:"Not yet supported by driver"], 
//          required: false
//        ],
//        parameterSize: 2],
//  10: [input: [name: "configParam10", 
//          title: "Setting to disable alarm", 
//          description:"Determines the method of disabling the alarm of the device.<br>0 3x tapping Action Button within 1 second, 1 when receives a State Idle corresponding to the alarm, 10..255 Sets the duration of the alarm in seconds.", 
//          type: "number", 
//          defaultValue: 0,
//          range: "0..255", 
//          required: false
//        ], 
//        parameterSize: 1],
  18: [input: [name: "configParam18",
          title: "LED blinking frequency", 
          description:"Set amount of blinks per seconds.<br/>1-9s", 
          type: "number", 
          defaultValue: 2,
          range: "1..9", 
          required: false
        ],
        parameterSize: 1],
  20: [input: [name: "configParam20", 
          title: "Action in case of power out", 
          description:"Determines if on/off status is saved and restored after power failure.", 
          type: "enum",
          defaultValue: 0, 
          options:[0:"Last status", 1:"Switch is on", 2:"Switch is off"], 
          required: true
        ], 
        parameterSize: 1],
  80: [input: [name: "configParam80", 
          title: "Liveline command", 
          description:"Configure what command will be sent via Lifeline when switch state has changed.", 
          type: "enum",
          defaultValue: 2, 
          options:[0:"None", 1:"Basic Report", 2:"Binary Switch Report"], 
          required: true
        ], 
        parameterSize: 1],
  81: [input: [name: "configParam81", 
          title: "Load Indicator Mode setting", 
          description:"See user guide for details.", 
          type: "enum",
          defaultValue: 2, 
          options:[0:"Disable Mode", 1:"Night Light Mode", 2:"ON/OFF Mode"], 
          required: false
        ], 
        parameterSize: 1],
  82: [input: [name: "configParam82", 
          title: "Night Light Mode", 
          description:"Enable or disable Night Light Mode during specific times. See manual.", 
          type: "number", 
          defaultValue: 0x12000800, 
          range: "0..389748539", 
          required: false
        ], 
        parameterSize: 4],
  91: [input: [name: "configParam91", 
          title: "Threshold Power", 
          description:"Threshold Power (W) for inducing automatic report when Watt become 5% more or less than the value.<br/>0 Disabled, 1-2300W", 
          type: "number", 
          defaultValue: 0, 
          range: "0..2300", 
          required: true
        ], 
        parameterSize: 2],
  92: [input: [name: "configParam92", 
          title: "Threshold Energy", 
          description:"Threshold Energy (kWh) for inducing automatic report.<br/>0 Disabled, 1-10000KWh", 
          type: "number", 
          defaultValue: 0, 
          range: "0..100000", 
          required: true
        ],
        parameterSize: 2],
  93: [input: [name: "configParam93", 
          title: "Threshold Current", 
          description:"Threshold Current (A) for inducing automatic report.<br/>0 Disabled, 1-100 in 0.1 steps", 
          type: "number", 
          defaultValue: 0, 
          range: "0..100", 
          required: true
        ],
        parameterSize: 1],
  101: [input: [name: "configParam101", 
          title: "Meter Lifeline Reporting", 
          description:"Configure which meter reading will be periodically report via Lifeline.", 
          type: "enum",
          defaultValue: 0x0F, 
          options:[2:"Power only", 8:"Current only", 10:"Power and Current", 3:"Power and Energy", 11:"Power, Current and Energy", 15:"Power, Current, Energy and Voltage"], 
          required: true
        ],
        parameterSize: 4],
  111: [input: [name: "configParam111", 
          title: "Meter Reporting Frequency", 
          description:"Configure the sending frequency of Meter Report.<br/>0 Disabled, 30-2592000s.", 
          type: "number", 
          defaultValue: 600, 
          range: "0..2592000", 
          required: true
        ],
        parameterSize: 4]
]

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
  if (logEnable) runIn(3600, logsOff)
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
  sendCommands(cmds, 500)
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
  def associationGroups = 2
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

void startStopBlinking (duration) {
  logger("debug", "stopBlinking( ${duration} )")
  sendCommands(supervisionEncap(zwave.configurationV1.configurationSet(parameterNumber: 19, size: 2, scaledConfigurationValue: duration)))
}

void resetDevice (flag) {
  logger("debug", "resetDevice( ${flag} )")
  def value = flag == 1 ? 0x55555555 : 0x00
  sendCommands(supervisionEncap(zwave.configurationV1.configurationSet(parameterNumber: 255, size: 4, scaledConfigurationValue: value)))
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

void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  logger("trace", "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}")
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off")
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
  logger("trace", "zwaveEvent(ConfigurationReport) - cmd: ${cmd.inspect()}")
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

void zwaveEvent(hubitat.zwave.commands.notificationv4.NotificationReport cmd) {
  logger("trace", "zwaveEvent(NotificationReport) - cmd: ${cmd.inspect()}")
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
          if (cmd.event) logger ("warn", "Unhandled power notifcation event: ${cmd.event}")
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
          if (cmd.event) logger ("warn", "Unhandled appliance notifcation event: ${cmd.event}")
      }
    default:
      if (cmd.event) logger ("warn", "Unhandled notifcation tpe: ${cmd.event}")
  }
}

void zwaveEvent(hubitat.zwave.commands.protectionv1.ProtectionReport cmd) {
  logger("trace", "zwaveEvent(ProtectionReport) - cmd: ${cmd.inspect()}")
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

