/**
 *  Heatit ZM Dimmer Z-Wave 800 Driver for Hubitat
 *  Date: 12.11.2023
 *	Author: Rene Boer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 * |---------------------------- DEVICE HANDLER FOR HEATIT ZM DIMMER Z-WAVE DEVICE -------------------------------------------------------|  
 *	The handler supports all unsecure and secure functions of the Qubino DIN Dimmer device, except configurable inputs. Configuration parameters and
 *	association groups can be set in the device's preferences screen, but they are applied on the device only after
 *	pressing the 'Set configuration' and 'Set associations' buttons on the bottom of the details view. 
 *
 *	This device handler supports data values that are currently not implemented as capabilities, so custom attribute 
 *	states are used. Please use a SmartApp that supports custom attribute monitoring with this device in your rules.
 * |-----------------------------------------------------------------------------------------------------------------------------------------------|
 *
 *	TO-DO:
 *
 *	CHANGELOG:
 */
import groovy.transform.Field

@Field String VERSION = "1.0"

metadata {
  definition (name: "Heatit ZM Dimmer", namespace: "reneboer", author: "Rene Boer", importUrl: "https://raw.githubusercontent.com/reneboer/Hubitat/master/Drivers/Heatit/Heatit ZM Dimmer.groovy") {
    capability "Actuator"
    capability "Switch"
    capability "Switch Level"
    capability "Power Meter"
    capability "EnergyMeter"
    capability "ChangeLevel"
    capability "Configuration" //Needed for configure() function to set any specific configurations
    capability "Refresh"       //Needed for refresh() function to set get current power and levels

    //attribute "kwhConsumption", "number" //attribute used to store and display power consumption in KWH 
    attribute "powerHigh", "number"
    attribute "powerLow", "number"
	attribute "overloadProtection", "number"

    command "resetPower" //command to issue Meter Reset commands to reset accumulated power measurements

    fingerprint mfr: "411", prod: "33", deviceId: "8449", inClusters: "0x5E, 0x55, 0x98, 0x9F, 0x6C", secureInClusters: "0x86, 0x26, 0x32, 0x5B, 0x70, 0x71, 0x8E, 0x87, 0x85, 0x59, 0x72, 0x5A, 0x73, 0x7A", deviceJoinName: "Heatit ZM Dimmer"
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
  0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY_V1
  0x7A: 5, // COMMAND_CLASS_FIRMWARE_UPDATE_MD_V5
  0x87: 3, // COMMAND_CLASS_INDICATOR_V3,
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
  0x8E: 3, // COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
  0x73: 1, // COMMAND_CLASS_POWER_LEVEL_V1
  0x98: 1, // COMMAND_CLASS_SECURITY_V1
  0x9F: 1, // COMMAND_CLASS_SECURITY_2_V1
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1
  0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE_V2
  0x86: 3, // COMMAND_CLASS_VERSION_V3
  0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO_V2
  0x22: 1, // COMMAND_CLASS_APPLICATION_STATUS_V1
  0x20: 2, // COMMAND_CLASS_BASIC_V2
  0x5B: 3, // COMMAND_CLASS_CENTRAL_SCENE_V3
  0x70: 4, // COMMAND_CLASS_CONFIGURATION_V4
  0x32: 5, // COMMAND_CLASS_METER_V5
  0x40: 8, // COMMAND_CLASS_NOTIFICATION_V8
  0x26: 4  // COMMAND_CLASS_SWITCH_MULTILEVEL_V4
]
@Field static Map configParams = [
  1:  [input: [name: "configParam1", type: "enum", title: "Power restore level", description: "The state the dimmer should return to once power is restored after a power failure.", defaultValue: 100, required: true, options:[[0:"off"], [100:"on"], [5:"5%"], [10:"10%"], [15:"15%"], [20:"20%"], [25:"25%"], [30:"30%"], [50:"50%"], [75:"75%"], [90:"90%"]]], parameterSize: 1],
  2:  [input: [name: "configParam2", type: "enum", title: "Switch ON level", description:"Defines the dimming level when restored from the OFF state.", defaultValue: 0, required: true, options:[[0:"off"], [100:"on"], [5:"5%"], [10:"10%"], [15:"15%"], [20:"20%"], [25:"25%"], [30:"30%"], [50:"50%"], [75:"75%"], [90:"90%"]]], parameterSize: 1],
  3:  [input: [name: "configParam3", type: "number", title: "Automatic turn OFF", range: "0..86400", description:"Time for the dimmer to turn off automatically after turning it on.<br/>0 Disabled, 1-86400s", required: true, defaultValue: 0], parameterSize: 4],
  4:  [input: [name: "configParam4", type: "number", title: "Automatic turn ON", range: "0..86400", description:"Time for the dimmer to turn on automatically after turning it off.<br/>0 Disabled, 1-86400s", required: true, defaultValue: 0], parameterSize: 4],
  5:  [input: [name: "configParam5", type: "number", title: "Turn off delay time", range: "0..60", description:"The time it takes before the dimmer turns off after turning it off.<br/>0 Disabled, 1-60s", required: true, defaultValue: 0], parameterSize: 1],
  6:  [input: [name: "configParam6", type: "enum", title: "S1 functionality", description: "S1 switch functionality.", defaultValue: 0, required: true, options:[[0:"Default Dimming"], [1:"Scene Controller"], [2:"Scene Controller and Dimming"], [3:"Disabled"]]], parameterSize: 1],
  7:  [input: [name: "configParam7", type: "enum", title: "S2 functionality", description: "S1 switch functionality.", defaultValue: 0, required: true, options:[[0:"Default Dimming"], [1:"Scene Controller"], [2:"Scene Controller and Dimming"], [3:"Disabled"]]], parameterSize: 1],
  8:  [input: [name: "configParam8", type: "enum", title: "Dimming duration", description:"Define how long it takes to dim when using the external switch.", required: true, defaultValue: 50, options: [[0:"Instantly"], [5:"0.5s"], [10:"1s"], [20:"2s"], [30:"3s"], [40:"4s"], [50:"5s"], [60:"6s"], [70:"7s"], [80:"8s"], [90:"9s"], [100:"10s"]]], parameterSize: 1],
  9:  [input: [name: "configParam9", type: "enum", title: "Dimmer Curve", description: "Choose if the dimmer uses Linear or Logarythmic dimming.", defaultValue: 0, required: true, options:[[0:"Liniar dimming"], [1:"Logarithmic dimming"]]], parameterSize: 1],
  10: [input: [name: "configParam10",type: "enum", title: "Load dimming mode", description: "Choose the dimming type.", defaultValue: 0, required: true, options:[[0:"Trailing edge"], [1:"Leading edge"]]], parameterSize: 1],
  11: [input: [name: "configParam11",type: "number", title: "Maximum dim level", range: "2..99", description:"Highest dim level of the dimmer.<br/>2-99%", required: true, defaultValue: 90], parameterSize: 1],
  12: [input: [name: "configParam12",type: "number", title: "Minmum dim level", range: "1..98", description:"Lowest dim level of the dimmer.<br/>1-98%", required: true, defaultValue: 15], parameterSize: 1],
  13: [input: [name: "configParam13",type: "number", title: "Meter report threshold", range: "0..250", description:"Threshold for device to send meter report in W.<br/>0 Disabled, 1-250W", required: true, defaultValue: 10], parameterSize: 1],
  14: [input: [name: "configParam14", type: "number", title: "Meter report interval", range: "30..65535", description:"Time interval between consecutive meter reports in seconds.<br/>30-65535s", required: true, defaultValue: 810], parameterSize: 2],
  15: [input: [name: "configParam15", type: "bool", title: "ON/OFF Functionality", description: "Set to true for non-dimmable loads.", defaultValue: false, required: true], parameterSize: 1]
]

void logsOff(){
//  log.warn "debug logging disabled..."
//  device.updateSetting("logEnable",[value:"false",type:"bool"])
}


void installed() {
  log.info "installed(${VERSION})"
  device.updateDataValue("powerLow", -1)
  device.updateDataValue("powerHigh", -1)
}

void updated() {
  log.debug "updated()"
  log.warn "debug logging is: ${logEnable == true}"
  log.warn "description logging is: ${txtEnable == true}"
  unschedule()
  if (logEnable) runIn(86400, logsOff)
  runIn (5, configure)
}

List<hubitat.zwave.Command> refresh() {
  logger "info", "refresh()"
  sendListToDevice([
    zwave.meterV5.meterGet(scale: 0x00),
    zwave.meterV5.meterGet(scale: 0x02),
    zwave.switchMultilevelV4.switchMultilevelGet(),
    zwave.notificationV8.notificationGet(notificationType: hubitat.zwave.commands.notificationv8.NotificationGet.NOTIFICATION_TYPE_POWER_MANAGEMENT)
  ], 500)
}

/**
 * Configuration capability command handler.
*/
List<String> configure() {
  logger("debug", "configure()")

  List<hubitat.zwave.Command> cmds=[]
  cmds.add(zwave.associationV2.associationRemove(groupingIdentifier:1))
  cmds.add(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId))
  configParams.each { param, data ->
    if (settings[data.input.name] != null) {
      cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name]))
    }
  }
  if (!device.getDataValue("MSR")) {
    cmds.add(zwave.versionV2.versionGet())
    cmds.add(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
  }
  runIn (cmds.size() * 2, refresh)
  sendListToDevice(cmds, 500)
}

List<String> configCmd(parameterNumber, size, Boolean boolConfigurationValue) {
  return [
    zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: boolConfigurationValue ? 1 : 0),
    zwave.configurationV4.configurationGet(parameterNumber: parameterNumber.toInteger())
  ]
}

List<String> configCmd(parameterNumber, size, scaledConfigurationValue) {
  List<hubitat.zwave.Command> cmds = []
  int intval=scaledConfigurationValue.toInteger()
  if (size==1) {
    if (intval < 0) intval = 256 + intval
    cmds.add(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), configurationValue: [intval]))
  } else {
    cmds.add(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: intval))
  }
  cmds.add(zwave.configurationV4.configurationGet(parameterNumber: parameterNumber.toInteger()))
  return cmds
}

String on() {
  logger("debug", "on()")
  sendToDevice(zwave.basicV1.basicSet(value: 0xFF))
}
String off() {
  logger("debug", "off()")
  sendToDevice(zwave.basicV1.basicSet(value: 0x00))
}

String setLevel(level) {
  if(level > 99) level = 99
  logger("debug", "setLevel(value: ${level})")
  sendToDevice(zwave.switchMultilevelV4.switchMultilevelSet(value: level, dimmingDuration: 0x00))
}

String setLevel(level, duration) {
  if(level > 99) level = 99
  logger("debug", "setLevel(value: ${level}, dimmingDuration: ${duration})")
  sendToDevice(zwave.switchMultilevelV4.switchMultilevelSet(value: level, dimmingDuration: duration))
}

String startLevelChange(direction) {
  Boolean upDownVal = direction == "down" ? true : false
  Short dimmingDuration = Math.round((settings."param8"!=null? settings."param8":50) / 10)
  Short startLevel = device.currentValue("level")
  logger("debug", "startLevelChange(upDown: ${direction}, startLevel: ${startLevel}, dimmingDuration: ${dimmingDuration})")
  sendToDevice(zwave.switchMultilevelV4.switchMultilevelStartLevelChange(ignoreStartLevel: true, startLevel: startLevel, upDown: upDownVal, dimmingDuration: dimmingDuration))
}

String stopLevelChange() {
  logger("debug", "stopLevelChange()")
  sendToDevice(zwave.switchMultilevelV4.switchMultilevelStopLevelChange())
}

String resetPower() {
  logger("debug", "resetPower()")
  runIn (10, refresh)
  sendToDevice(zwave.meterV2.meterReset())
}

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

// Handle zwave events not expected
void zwaveEvent(hubitat.zwave.Command cmd) {
  logger("warn", "zwaveEvent(Command) - Unspecified - cmd: ${cmd.inspect()}")
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelReport cmd){
  logger("trace", "zwaveEvent(SwitchMultilevelReport) - cmd: ${cmd.inspect()}")
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off")
  sendEventWrapper(name:"level", value: cmd.value, unit:"%", descriptionText:"dimmed to ${cmd.value==255 ? 100 : cmd.value}%")
}

void zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd) {
  logger("trace", "zwaveEvent(MeterReport) - cmd: ${cmd.inspect()}")
  switch(cmd.scale){
    case 0x00:
      sendEventWrapper(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"consumed ${cmd.scaledMeterValue} kWh")
      break;
    case 0x02:
	  def val = cmd.scaledMeterValue
      sendEventWrapper(name:"power", value: val, unit:"W", descriptionText:"consumes ${cmd.scaledMeterValue} W")
      // Update powerHigh/Low values when within expected ranges
      if (val >= 0 && val <= 300) {
        def valLow = device.currentValue("powerLow")
        def valHigh = device.currentValue("powerHigh")
        if (val > valHigh || valHigh == -1) sendEventWrapper(name:"powerHigh", value: val)
        if (val < valLow || valLow == -1) sendEventWrapper(name:"powerLow", value: val)
      }
      break;
    default:
      logger("warn", "zwaveEvent(MeterReport) - skipped: ${cmd.inspect()}")
    }    
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  logger("trace", "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}")
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off")
}

void zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd){
  logger("trace", "zwaveEvent(ConfigurationReport) - cmd: ${cmd.inspect()}. No action.")
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd){
  logger("trace", "zwaveEvent(BasicReport) - cmd: ${cmd.inspect()}. No action.")
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelSet cmd){
  logger("trace", "zwaveEvent(SwitchMultilevelSet) - cmd: ${cmd.inspect()}. No action.")
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logger("trace", "zwaveEvent(ManufacturerSpecificReport) - cmd: ${cmd.inspect()}")
  if (cmd.manufacturerName) { device.updateDataValue("manufacturer", cmd.manufacturerName) }
  if (cmd.productTypeId) { device.updateDataValue("productTypeId", cmd.productTypeId.toString()) }
  if (cmd.productId) { device.updateDataValue("deviceId", cmd.productId.toString()) }
  device.updateDataValue("MSR", String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId))
}

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
  logger("trace", "zwaveEvent(VersionReport) - cmd: ${cmd.inspect()}")
  device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
  device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
  device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

// Set overloadProtection to 1 when detected.
void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
  logger("trace", "zwaveEvent(NotificationReport) - cmd: ${cmd.inspect()}")
  Map map = [name: "overloadProtection"]
  if (cmd.notificationType ==  8) {
    // Power management notification
    switch (cmd.event) {
      case 0:
        map.descriptionText = "normal operation"
        map.value = 0
        break
      case 8:
        map.descriptionText = "power overload detected"
		map.value = 1
        break
      default:
        if (cmd.event) log.warn "Unhandled power notifcation event: ${cmd.event}"
        return
    }
    sendEventWrapper(map)
  } else {
    logger("warn", "Unhandled NotificationReport: ${cmd}")
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
  sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0).format()), hubitat.device.Protocol.ZWAVE))
}

/**
* Wrapper for sendEvent to limit duplicate events and support logging
*/
private void sendEventWrapper(Map prop) {
  String cv = device.currentValue(prop.name)
  Boolean isStateChange = (cv?.toString() != prop.value?.toString()) ? true : false
  if (isStateChange) sendEvent(prop)
  if (prop?.descriptionText) {
    if (txtEnable && isStateChange) {
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

/**
 * Send z-wave commands with secure support.
 * Can be string, command or command list
 */
String sendToDevice(String cmd) {
    logger("debug", "sendToDevice String($cmd)")
    return zwaveSecureEncap(cmd.format())
}

String sendToDevice(hubitat.zwave.Command cmd) {
    logger("debug", "sendToDevice Command($cmd)")
    return zwaveSecureEncap(cmd.format())
}

List<String> sendListToDevice(List<hubitat.zwave.Command> commands, Long delay=100) {
    logger("debug", "sendListToDevice Commands($commands), delay ($delay)")
    delayBetween(commands.collect{ sendToDevice(it) }, delay)
}
