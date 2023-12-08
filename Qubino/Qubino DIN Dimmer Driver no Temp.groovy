/**
 *  Qubino DIN Dimmer no Temp
 *	Device Handler 
 *  Date: 7.12.2023
 *	Author: Kristjan Jam&scaron;ek (Kjamsek), Goap d.o.o.
 *  Post V1.0 updates: Rene Boer
 *  Copyright 2017 Kristjan Jam&scaron;ek
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
 * |---------------------------- DEVICE HANDLER FOR QUBINO DIN DIMMER Z-WAVE DEVICE -------------------------------------------------------|  
 *	The handler supports all unsecure functions of the Qubino DIN Dimmer device, except configurable inputs. Configuration parameters and
 *	association groups can be set in the device's preferences screen, but they are applied on the device only after
 *	pressing the 'Set configuration' and 'Set associations' buttons on the bottom of the details view. 
 *
 *	This device handler supports data values that are currently not implemented as capabilities, so custom attribute 
 *	states are used. Please use a SmartApp that supports custom attribute monitoring with this device in your rules.
 * |-----------------------------------------------------------------------------------------------------------------------------------------------|
 *
 *
 *	TO-DO:
 * Add powerHigh/Low values agains impossible readings.
 *
 *	CHANGELOG:
 *	0.99: Final release code cleanup and commenting
 *	1.00: Added comments to code for readability
 *  1.01: Removed Temp capability for better alexa intergration.
 *  1.02: Added loggin options. Shortened text for device parameters. Get the manual when updating.
 *  1.03: Added event handler for zwave.commands.switchmultilevelv4.SwitchMultilevelSet
 *  1.04: Removed ST specifics (tiles, simulation)
 *  1.05: Added default for preferences, removed debug messages when debug is off.
 *  1.06: Added power refresh after switch off.
 *  1.07: Added ChangeLevel support
 *  1.08: Changed on/off commands. Rewrite of some functions.
 *  1.1 : Added sure support. However, do not! include with S0 security as DIN dimmer will drop from network.
 *  1.2 : Rewrite now I know more about writing drivers. Removed setting associations.
 *  1.3 : Minor tweak in min/max power reporting
 *  1.4 : Current device parameters will be populated on install or updated on refresh.
 */
import groovy.transform.Field

@Field String VERSION = "1.4"

metadata {
  definition (name: "Qubino DIN Dimmer no Temp", namespace: "reneboer", author: "Rene Boer", importUrl: "https://github.com/reneboer/Hubitat/blob/main/Qubino/Qubino%20DIN%20Dimmer%20Driver%20no%20Temp.groovy") {
    capability "Actuator"
    capability "Switch"
    capability "Switch Level"
    capability "Power Meter"
    capability "EnergyMeter"
    capability "ChangeLevel"
    capability "Configuration"
    capability "Refresh"
    attribute "powerHigh", "number"
    attribute "powerLow", "number"
    //attribute "kwhConsumption", "number" //attribute used to store and display power consumption in KWH 

    command "resetPower" //command to issue Meter Reset commands to reset accumulated pwoer measurements
	
	fingerprint mfr:"0159", prod:"0001", deviceId:"0052", inClusters:"0x5E,0x86,0x5A,0x72,0x73,0x98,0x27,0x25,0x26,0x32,0x71,0x85,0x8E,0x59,0x70", outClusters:"0x26", deviceJoinName: "Qubino DIN Dimmer"
  }
  preferences {
    configParams.each { input it.value.input }
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
  }  
}

@Field static Map CMD_CLASS_VERS = [
  0x70: 2, // Configuration 
  0x59: 2, // Association Grp Info
  0x8E: 3, // Multi Channel Association
  0x85: 2, // Association 
  0x71: 5, // Notification 
  0x32: 4, // Meter
  0x26: 4, // Switch Multilevel
  0x25: 1, // Switch Binary
  0x27: 1, // Switch All
  0x98: 1, // COMMAND_CLASS_SECURITY_V1
  0x73: 1, // Powerlevel
  0x72: 2, // Manufacturer Specific
  0x5A: 1, // Device Reset Locally
  0x86: 2, // Version 
  0x5E: 2  // Zwaveplus Info
]
@Field static Map configParams = [
  1:  [input: [name: "configParam1", type: "enum", title: "Input 1 switch type", defaultValue: 0, required: true, options:[0 : "mono-stable switch type (push button)", 1 : "Bi-stable switch type"]], parameterSize: 1],
  5:  [input: [name: "configParam5", type: "enum", title: "Module function", defaultValue: 0, required: true, options:[0 : "Dimmer mode", 1 : "Switch mode"]], parameterSize: 1],
  10: [input: [name: "configParam10",type: "enum", title: "Activate / deactivate functions ALL ON / ALL OFF", defaultValue: 255, required: true, options:[0 : "ALL ON is not active, ALL OFF is not active", 1 : "ALL ON is not active, ALL OFF active", 2 : "ALL ON active, ALL OFF is not active", 255 : "ALL ON active, ALL OFF active"]], parameterSize: 2],
  11: [input: [name: "configParam11",type: "number", title: "Automatic turning off", range: "0..32536", description:"Time for the dimmer to turn off automatically after turning it on.<br/>0 Disabled, 1-32536", required: true, defaultValue: 0], parameterSize: 2],
  12: [input: [name: "configParam12",type: "number", title: "Automatic turning on", range: "0..32536", description:"Time for the dimmer to turn on automatically after turning it off.<br/>0 Disabled, 1-32536", required: true, defaultValue: 0], parameterSize: 2],
  21: [input: [name: "configParam21",type: "enum", title: "Enable/Disable Double click function", defaultValue: 0, required: true, options:[0 : "Double click disabled", 1 : "Double click enabled"]], parameterSize: 1],
  30: [input: [name: "configParam30",type: "enum", title: "Saving the state of the device after a power failure", defaultValue: 0, required: true, options:[0 : "Return to state prior to power failure", 1 : "Set off after power failure"]], parameterSize: 1],
  40: [input: [name: "configParam40",type: "number", title: "Power reporting on power change", range: "0..100", description:"How much power consumption needs to increase or decrease to be reported.<br/>0 Disabled, 1-100%", required: true, defaultValue: 5], parameterSize: 1],
  42: [input: [name: "configParam42",type: "number", title: "Power reporting by time interval", range: "0..32767", description:"The time interval with which power consumption in Watts is reported.<br/>0 Disabled, 30-32767s", required: true, defaultValue: 5], parameterSize: 2],
  60: [input: [name: "configParam60",type: "number", title: "Minimum dimming value", range: "1..98", description:"The minimum level may not be higher than the maximum level!.<br/>1-98%", required: true, defaultValue: 1], parameterSize: 1],
  61: [input: [name: "configParam61",type: "number", title: "Maxmum dimming value", range: "2..99", description:"The minimum level may not be higher than the maximum level!.<br/>2-99%", required: true, defaultValue: 99], parameterSize: 1],
  65: [input: [name: "configParam65",type: "number", title: "Dimming time (soft on/off)", range: "50..255", description:"Step size is 10 milliseconds. <br>50-255ms", required: true, defaultValue: 100], parameterSize: 2],
  66: [input: [name: "configParam66",type: "number", title: "Dimming time when key pressed", range: "1..255", description:"Time during which the Dimmer will move between the min. and max. dimming values during a continuous press.<br>1-255s", required: true, defaultValue: 3], parameterSize: 2],
  67: [input: [name: "configParam67",type: "enum", title: "Ignore start level", description:"If configured to use the start level, it should start the dimming process from the currently set dimming level.", defaultValue: 0, required: true, options:[0 : "Respect start level", 1 : "Ignore start level"]], parameterSize: 1],
  68: [input: [name: "configParam68",type: "number", title: "Dimming duration", range: "0..127", description:"Time during which the device will transition from the current value to the new target value.<br>0 According Dimming time parameter, 1-127s", required: true, defaultValue: 3], parameterSize: 1]
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
}

void refresh() {
  logger "info", "refresh()"
  List<hubitat.zwave.Command> cmds=[
    secureCmd(zwave.meterV4.meterGet(scale: 0x00)),
    secureCmd(zwave.meterV4.meterGet(scale: 0x02)),
    secureCmd(zwave.switchMultilevelV4.switchMultilevelGet()),
    secureCmd(zwave.notificationV8.notificationGet(notificationType: hubitat.zwave.commands.notificationv8.NotificationGet.NOTIFICATION_TYPE_POWER_MANAGEMENT))
  ]
  configParams.each { param, data ->
    cmds.add(secureCmd(zwave.configurationV2.configurationGet(parameterNumber: param.toInteger())))
  }
  sendCommands(cmds, 500)
}

void configure() {
  logger("debug", "configure()")

  List<hubitat.zwave.Command> cmds=[]
  cmds.add(secureCmd(zwave.associationV2.associationRemove(groupingIdentifier:1)))
  cmds.add(secureCmd(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId)))
  configParams.each { param, data ->
    if (settings[data.input.name] != null) {
      cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name]))
    }
  }
  cmds.add(secureCmd(zwave.configurationV1.configurationSet(parameterNumber: 120, size: 1, scaledConfigurationValue: 0))) // Disable temp reporting
  if (!device.getDataValue("MSR")) {
    cmds.add(secureCmd(zwave.versionV3.versionGet()))
    cmds.add(secureCmd(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
  }
  runIn (cmds.size() * 2, refresh)
  sendCommands(cmds, 1000)
}

private List<String> configCmd(parameterNumber, size, Boolean boolConfigurationValue) {
  return [
    secureCmd(zwave.configurationV2.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: boolConfigurationValue ? 1 : 0))//,
//    secureCmd(zwave.configurationV2.configurationGet(parameterNumber: parameterNumber.toInteger()))
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
     secureCmd(zwave.configurationV2.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), configurationValue: confValue))//, 
//     secureCmd(zwave.configurationV4.configurationGet(parameterNumber: parameterNumber.toInteger()))
  ]
}

void on() {
  logger("debug", "on()")
  sendCommands(secureCmd(zwave.basicV1.basicSet(value: 0xFF)))
}

void off() {
  logger("debug", "off()")
  sendCommands(secureCmd(zwave.basicV1.basicSet(value: 0x00)))
}

void setLevel(level) {
  if(level > 99) level = 99
  logger("debug", "setLevel(value: ${level})")
  sendCommands(secureCmd(zwave.switchMultilevelV4.switchMultilevelSet(value: level, dimmingDuration: 0x00)))
}

void setLevel(level, duration) {
  if(level > 99) level = 99
  logger("debug", "setLevel(${level}, ${duration})")
  sendCommands(secureCmd(zwave.switchMultilevelV3.switchMultilevelSet(value: level, dimmingDuration: duration)))
}

void startLevelChange(direction) {
  boolean upDownVal = direction == "down" ? true : false
  logger("debug", "startLevelChange(${direction})")
  sendCommands(secureCmd(zwave.switchMultilevelV4.switchMultilevelStartLevelChange(ignoreStartLevel: true, startLevel: device.currentValue("level"), upDown: upDownVal, dimmingDuration: settings."param66"!=null? settings."param66":3)))
}

void stopLevelChange() {
  logger("debug", "stopLevelChange()")
  sendCommands(secureCmd(zwave.switchMultilevelV4.switchMultilevelStopLevelChange()))
}

void resetPower() {
  logger("debug", "resetPower()")
  runIn (10, refresh)
  sendCommands(secureCmd(zwave.meterV2.meterReset()))
  device.deleteCurrentState("powerHigh")
  device.deleteCurrentState("powerLow")
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

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelReport cmd){
  logger("trace", "zwaveEvent(SwitchMultilevelReport) - cmd: ${cmd.inspect()}")
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off")
  sendEventWrapper(name:"level", value: cmd.value, unit:"%", descriptionText:"${device.displayName} dimmed to ${cmd.value==255 ? 100 : cmd.value}%")
}

void zwaveEvent(hubitat.zwave.commands.meterv4.MeterReport cmd) {
  logger("trace", "zwaveEvent(MeterReport) - cmd: ${cmd.inspect()}")
  switch(cmd.scale){
	case 0:
      logger("debug", "energy report is ${cmd.scaledMeterValue} kWh")
      sendEventWrapper(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"${device.displayName} consumed ${cmd.scaledMeterValue} kWh")
      break;
    case 2:
      logger("debug", "power report is ${cmd.scaledMeterValue} W")
      sendEventWrapper(name:"power", value: cmd.scaledMeterValue, unit:"W", descriptionText:"${device.displayName} consumes ${cmd.scaledMeterValue} W")
      // Update powerHigh/Low values when wihtin expected ranges
	    def val = cmd.scaledMeterValue
      if (val >= 0 && val <= 250) {
        def valLow = device.currentValue("powerLow")
        def valHigh = device.currentValue("powerHigh")
        if (val > valHigh || valHigh == null || valHigh == 0) sendEventWrapper(name:"powerHigh", value: val)
        if (val < valLow || valLow == null || valLow == 0) sendEventWrapper(name:"powerLow", value: val)
      }
      break;
    default:
      logger("warn", "zwaveEvent(MeterReport) -skipped cmd: ${cmd}")
  }    
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd) {
  logger("trace", "zwaveEvent(SwitchBinaryReport) - cmd: ${cmd.inspect()}")
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off")
}

void zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd){
  logger("trace", "zwaveEvent(ConfigurationReport) - cmd: ${cmd.inspect()}")
  def newVal = cmd.scaledConfigurationValue.toInteger()
  Map param = configParams[cmd.parameterNumber.toInteger()]
  if (param) {
    def curVal = device.getSetting(param.input.name)
    if (param.input.type == "bool") { curVal = curVal == "false" ? 0 : 1}
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

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd){
  logger("trace", "zwaveEvent(BasicReport) - cmd: ${cmd.inspect()}")
}

// Event handler for received MultiChannelEndPointReport commands. Used to distinguish when the device is in singlechannel or multichannel configuration. 
void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd){
  logger("trace", "zwaveEvent(MultiChannelEndPointReport) - cmd: ${cmd.inspect()}")
  if(cmd.endPoints > 0){
    state.isMcDevice = true;
    createChildDevices();
  }
  sendCommands(secureCmd(zwave.associationV1.associationRemove(groupingIdentifier:1).format()))
  sendCommands(secureCmd(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 1, nodeId: [0,zwaveHubNodeId,1])))
}

void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
  logger("trace", "zwaveEvent(MultiChannelCmdEncap) - cmd: ${cmd.inspect()}")
  def encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
  if (encapsulatedCommand) {
    logger("trace", "zwaveEvent(MultiChannelCmdEncap) - encapsulatedCommand: ${encapsulatedCommand}")
    zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
  } else {
    logger("warn", "zwaveEvent(MultiChannelCmdEncap) - Unable to extract MultiChannel command from: ${cmd.inspect()}")
  }
}

// We ignore temp reports.
void zwaveEvent(hubitat.zwave.commands.sensormultilevelv7.SensorMultilevelReport cmd, hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap command){
  logger("trace", "zwaveEvent(SensorMultilevelReport, MultiChannelCmdEncap) - cmd: ${cmd.inspect()} command: ${command.inspect()}")
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap command){
  logger("trace", "zwaveEvent(SwitchMultilevelReport, MultiChannelCmdEncap) - cmd: ${cmd.inspect()} command: ${command.inspect()}")
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off")
  sendEventWrapper(name:"level", value: cmd.value, unit:"%", descriptionText:"${device.displayName} dimmed to ${cmd.value==255 ? 100 : cmd.value}%")
}

void zwaveEvent(hubitat.zwave.commands.meterv4.MeterReport cmd, hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap command){
  logger("trace", "zwaveEvent(MeterReport, MultiChannelCmdEncap) - cmd: ${cmd.inspect()} command: ${command.inspect()}")
  switch(cmd.scale){
    case 0:
      sendEventWrapper(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"${device.displayName} consumed ${cmd.scaledMeterValue} kWh")
      break;
    case 2:
      sendEventWrapper(name:"power", value: cmd.scaledMeterValue, unit:"W", descriptionText:"${device.displayName} consumes ${cmd.scaledMeterValue} W")
      break;
  }
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelSet cmd){
  logger("trace", "zwaveEvent(SwitchMultilevelSet) - cmd: ${cmd.inspect()}")
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
  logger("trace", "zwaveEvent(VersionReport) - cmd: ${cmd.inspect()}")
  device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
  device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
  device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logger("trace", "zwaveEvent(ManufacturerSpecificReport) - cmd: ${cmd.inspect()}")
  if (cmd.manufacturerName) { device.updateDataValue("manufacturer", cmd.manufacturerName) }
  if (cmd.productTypeId) { device.updateDataValue("productTypeId", cmd.productTypeId.toString()) }
  if (cmd.productId) { device.updateDataValue("deviceId", cmd.productId.toString()) }
  device.updateDataValue("MSR", String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId))
}

// Set overloadProtection to 1 when detected.
void zwaveEvent(hubitat.zwave.commands.notificationv5.NotificationReport cmd) {
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
        if (cmd.event) logger ("warn", "Unhandled power notifcation event: ${cmd.event}")
        return
    }
    sendEventWrapper(map)
  } else {
    logger("warn", "Unhandled NotificationReport: ${cmd}")
  }
}

// Efficient sendEvent handling
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

// Always log error and warnings. Other levels only if logEnable is true.
private logger(String level, String msg) {
  if (level == "error" || level == "warn") {
    log."${level}" "${device.displayName} ${msg}"
  } else{
    if (logEnable) log."${level}" "${device.displayName} ${msg}"
  }
}

//These send commands to the device either a list or a single command
void sendCommands(List<String> cmds, Long delay=200) {
  logger("debug", "sendCommands Commands($commands), delay ($delay)")
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
  return zwaveSecureEncap(cmd.format())
}

