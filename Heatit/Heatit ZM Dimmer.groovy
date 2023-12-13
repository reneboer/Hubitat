/**
 *  Heatit ZM Dimmer Z-Wave 800 Driver for Hubitat
 *  Date: 13.12.2023
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
 *    V1.1 : Fixed paramter 14 with size of 2. Added button functions for S1 and S2 when in scene controller mode.
 *    V1.2 : Added supervisedEncap to handle S2 retransmissions properly. powerHigh/Low reset when running resetPower.
 *    V1.3 : Small change for powerHigh/Low. Added DoubleTapableButton capability. Minor fixes.
 *    V1.4 : Current device parameters will be populated on install or updated on refresh.
 *    V1.5 : Fix for bool type parameters.
 */
import groovy.transform.Field

@Field String VERSION = "1.5"

metadata {
  definition (name: "Heatit ZM Dimmer", namespace: "reneboer", author: "Rene Boer", importUrl: "https://github.com/reneboer/Hubitat/blob/main/Heatit/Heatit%20ZM%20Dimmer.groovy") {
    capability "Actuator"
    capability "Switch"
    capability "Switch Level"
    capability "Power Meter"
    capability "EnergyMeter"
    capability "ChangeLevel"
    capability "PushableButton"
    capability "HoldableButton"
    capability "ReleasableButton"
    capability "DoubleTapableButton"
    capability "Configuration"
    capability "Refresh"

    attribute "powerHigh", "number"
    attribute "powerLow", "number"
    attribute "overloadProtection", "number"

    command "resetPower" //command to issue Meter Reset commands to reset accumulated power measurements

    fingerprint mfr: "411", prod: "33", deviceId: "8449", inClusters: "0x5E, 0x55, 0x98, 0x9F, 0x6C", secureInClusters: "0x86, 0x26, 0x32, 0x5B, 0x70, 0x71, 0x8E, 0x87, 0x85, 0x59, 0x72, 0x5A, 0x73, 0x7A", deviceJoinName: "Heatit ZM Dimmer"
    fingerprint mfr: "411", prod: "33", deviceId: "8449", inClusters: "0x5E, 0x26, 0x32, 0x70, 0x5B, 0x8E, 0x87, 0x85, 0x59, 0x55, 0x71, 0x86, 0x72, 0x5A, 0x73, 0x98, 0x9F, 0x6C, 0x7A", deviceJoinName: "Heatit ZM Dimmer"
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
  7:  [input: [name: "configParam7", type: "enum", title: "S2 functionality", description: "S2 switch functionality.", defaultValue: 0, required: true, options:[[0:"Default Dimming"], [1:"Scene Controller"], [2:"Scene Controller and Dimming"], [3:"Disabled"]]], parameterSize: 1],
  8:  [input: [name: "configParam8", type: "enum", title: "Dimming duration", description:"Define how long it takes to dim when using the external switch.", required: true, defaultValue: 50, options: [[0:"Instantly"], [5:"0.5s"], [10:"1s"], [20:"2s"], [30:"3s"], [40:"4s"], [50:"5s"], [60:"6s"], [70:"7s"], [80:"8s"], [90:"9s"], [100:"10s"]]], parameterSize: 1],
  9:  [input: [name: "configParam9", type: "enum", title: "Dimmer Curve", description: "Choose if the dimmer uses Linear or Logarythmic dimming.", defaultValue: 0, required: true, options:[[0:"Liniar dimming"], [1:"Logarithmic dimming"]]], parameterSize: 1],
  10: [input: [name: "configParam10",type: "enum", title: "Load dimming mode", description: "Choose the dimming type.", defaultValue: 0, required: true, options:[[0:"Trailing edge"], [1:"Leading edge"]]], parameterSize: 1],
  11: [input: [name: "configParam11",type: "number", title: "Maximum dim level", range: "2..99", description:"Highest dim level of the dimmer.<br/>2-99%", required: true, defaultValue: 90], parameterSize: 1],
  12: [input: [name: "configParam12",type: "number", title: "Minmum dim level", range: "1..98", description:"Lowest dim level of the dimmer.<br/>1-98%", required: true, defaultValue: 15], parameterSize: 1],
  13: [input: [name: "configParam13",type: "number", title: "Meter report threshold", range: "0..250", description:"Threshold for device to send meter report in W.<br/>0 Disabled, 1-250W", required: true, defaultValue: 10], parameterSize: 1],
  14: [input: [name: "configParam14",type: "number", title: "Meter report interval", range: "30..65535", description:"Time interval between consecutive meter reports in seconds.<br/>30-65535s", required: true, defaultValue: 810], parameterSize: 2],
  15: [input: [name: "configParam15",type: "bool", title: "ON/OFF Functionality", description: "Set to true for non-dimmable loads.", defaultValue: false, required: true], parameterSize: 1]
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
    secureCmd(zwave.meterV5.meterGet(scale: 0x00)),
    secureCmd(zwave.meterV5.meterGet(scale: 0x02)),
    secureCmd(zwave.switchMultilevelV4.switchMultilevelGet()),
    secureCmd(zwave.notificationV8.notificationGet(notificationType: hubitat.zwave.commands.notificationv8.NotificationGet.NOTIFICATION_TYPE_POWER_MANAGEMENT))
  ]
  configParams.each { param, data ->
    cmds.add(secureCmd(zwave.configurationV4.configurationGet(parameterNumber: param.toInteger())))
  }
  sendCommands(cmds, 500)
}

void configure() {
  logger("debug", "configure()")

  List<hubitat.zwave.Command> cmds=[]
  cmds.add(supervisionEncap(zwave.associationV2.associationRemove(groupingIdentifier:1)))
  cmds.add(supervisionEncap(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId)))
  configParams.each { param, data ->
    if (settings[data.input.name] != null) {
      cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name]))
    }
  }
  if (!device.getDataValue("MSR")) {
    cmds.add(secureCmd(zwave.versionV3.versionGet()))
    cmds.add(secureCmd(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
  }
  runIn (cmds.size() * 2, refresh)
  sendCommands(cmds, 1000)
}

private List<String> configCmd(parameterNumber, size, Boolean boolConfigurationValue) {
  return [
    supervisionEncap(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: boolConfigurationValue ? 1 : 0))//,
//    secureCmd(zwave.configurationV4.configurationGet(parameterNumber: parameterNumber.toInteger()))
  ]
}

private List<String> configCmd(parameterNumber, size, value) {
  List<Integer> confValue = []
  
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
     supervisionEncap(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), configurationValue: confValue))//, 
//     secureCmd(zwave.configurationV4.configurationGet(parameterNumber: parameterNumber.toInteger()))
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

void setLevel(level) {
  if(level > 99) level = 99
  logger("debug", "setLevel(value: ${level})")
  sendCommands(supervisionEncap(zwave.switchMultilevelV4.switchMultilevelSet(value: level, dimmingDuration: 0x00)))
}

void setLevel(level, duration) {
  if(level > 99) level = 99
  if (duration > 100) duration = 100
  if (duration < 1) duration = 1
  logger("debug", "setLevel(value: ${level}, dimmingDuration: ${duration})")
  sendCommands(supervisionEncap(zwave.switchMultilevelV4.switchMultilevelSet(value: level, dimmingDuration: duration)))
}

void startLevelChange(direction) {
  Boolean upDownVal = direction == "down" ? true : false
  Short dimmingDuration = Math.round((settings."param8"!=null? settings."param8":50) / 10)
  Short startLevel = device.currentValue("level")
  logger("debug", "startLevelChange(upDown: ${direction}, startLevel: ${startLevel}, dimmingDuration: ${dimmingDuration})")
  sendCommands(supervisionEncap(zwave.switchMultilevelV4.switchMultilevelStartLevelChange(ignoreStartLevel: true, startLevel: startLevel, upDown: upDownVal, dimmingDuration: dimmingDuration)))
}

void stopLevelChange() {
  logger("debug", "stopLevelChange()")
  sendCommands(supervisionEncap(zwave.switchMultilevelV4.switchMultilevelStopLevelChange()))
}

void resetPower() {
  logger("debug", "resetPower()")
  runIn (10, refresh)
  sendCommands(supervisionEncap(zwave.meterV2.meterReset()))
  device.deleteCurrentState("powerHigh")
  device.deleteCurrentState("powerLow")
}

void delayHold(button){
  sendButtonEvent("held", button, "physical")
}

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

void sendButtonEvent(action, button, type){
  if (button == 1 || button == 2) {
    sendEventWrapper(name:action, value:button, descriptionText:"button ${button} was ${action} [${type}]", isStateChange:true, type:type)
  } else {
    logger("warn", "button number must be one or two.")
  }	  
}

/**
* Incomming zwave event handlers.
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

// Handle zwave events not expected
void zwaveEvent(hubitat.zwave.Command cmd) {
  logger("warn", "zwaveEvent(Command) - Unspecified - cmd: ${cmd.inspect()}")
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelStopLevelChange cmd) {
  logger("trace", "zwaveEvent(SwitchMultilevelStopLevelChange) - cmd: ${cmd.inspect()}")
  //not needed. do nothing.
}

//CentralSceneNotification(keyAttributes:0, sceneNumber:1, sequenceNumber:0, slowRefresh:true)
void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd) {
  logger("trace", "zwaveEvent(CentralSceneNotification) - cmd: ${cmd.inspect()}")
  Integer button = cmd.sceneNumber
  Integer key = cmd.keyAttributes
  String action
  switch (key){
    case 0: //pushed
      action = "pushed"
      break
    case 1:	//released, only after 2
      state."${button}" = 0
      action = "released"
      break
    case 2:	//holding
      if (state."${button}" == 0){
        state."${button}" = 1
        runInMillis(200,delayHold,[data:button])
      }
      break
    case 3:	//double tap, 4 is tripple tap
      action = "doubleTapped"
      break
	default:
      logger("warn", "zwaveEvent(CentralSceneNotification) - skipped. Unknown button action.")
  }
  if (action) sendButtonEvent(action, button, "physical")
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelReport cmd){
  logger("trace", "zwaveEvent(SwitchMultilevelReport) - cmd: ${cmd.inspect()}")
  sendEventWrapper(name:"switch", value: cmd.value ? "on" : "off")
  sendEventWrapper(name:"level", value: cmd.value, unit:"%", descriptionText:"dimmed to ${cmd.value==255 ? 100 : cmd.value}%")
}

void zwaveEvent(hubitat.zwave.commands.meterv5.MeterReport cmd) {
  logger("trace", "zwaveEvent(MeterReport) - cmd: ${cmd.inspect()}")
  switch(cmd.scale){
    case 0x00:
      sendEventWrapper(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"consumed ${cmd.scaledMeterValue} kWh")
      break;
    case 0x02:
	  def val = cmd.scaledMeterValue
      sendEventWrapper(name:"power", value: val, unit:"W", descriptionText:"consumes ${cmd.scaledMeterValue} W")
      // Update powerHigh/Low values when wihtin expected ranges
      if (val >= 0 && val <= 300) {
        def valLow = device.currentValue("powerLow")
        def valHigh = device.currentValue("powerHigh")
        if (val > valHigh || valHigh == null) sendEventWrapper(name:"powerHigh", value: val)
        if (val < valLow || valLow == null) sendEventWrapper(name:"powerLow", value: val)
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
        if (cmd.event) logger ("warn", "Unhandled power notifcation event: ${cmd.event}")
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

