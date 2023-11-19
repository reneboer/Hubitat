/**
 *  ZVIDAR Z-TRV-V01 Thermostat Radiotor Valve Driver for Hubitat
 *  Date: 16.11.2023
 *	Author: Rene Boer
 *  Copyright (C) Rene Boer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  This TRV is sold under several brands like Remotec and has a z-wave 800 chip.
 *
 *	TO-DO:
 *
 *	CHANGELOG:
 *    V1.1 : Numerous small fixes. Removed running Configuration from update. User needsto press Config seperately.
 *    V1.2 : Added supervisedEncap to handle S2 retransmissions properly.
 *
 */

import groovy.transform.Field

@Field String VERSION = "1.2"

metadata {
  definition (name: "ZVIDAR Z-TRV-V01", namespace: "reneboer", author: "Rene Boer", importUrl: "https://github.com/reneboer/Hubitat/blob/main/ZVIDAR/Z-TRV-V01.groovy") {
    capability "Actuator"
    capability "Sensor"
    capability "Thermostat"
    capability "TemperatureMeasurement"
    capability "Battery"
    capability "Refresh"
    capability "Configuration"

	attribute "minHeatingSetpoint", "number" //google alexa compatability // should be part of setpoint to test without
    attribute "maxHeatingSetpoint", "number" //google alexa compatability // should be part of heating setpoint to test without
    attribute "thermostatTemperatureSetpoint", "String"    //google alexa compatability 
    attribute "valve", "String"
    attribute "lastBatteryReportReceivedAt", "String"

    fingerprint mfr: "1114", prod: "1024", deviceId: "1281", inClusters: "0x5E, 0x55, 0x98, 0x9F, 0x6C, 0x22", secureInClusters: "0x86, 0x85, 0x8E, 0x59, 0x72, 0x5A, 0x80, 0x70, 0x26, 0x31, 0x40, 0x43, 0x7A, 0x73, 0x87", deviceJoinName: "Remotec Thermostat"
    fingerprint mfr: "1114", prod: "1024", deviceId: "1281", inClusters: "0x5E, 0x55, 0x98, 0x9F, 0x6C, 0x22, 0x86, 0x85, 0x8E, 0x59, 0x72, 0x5A, 0x80, 0x70, 0x26, 0x31, 0x40, 0x43, 0x7A, 0x73, 0x87", deviceJoinName: "Remotec Thermostat"
  }

  preferences {
    configParams.each { input it.value.input }
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
  }
}

@Field static Map CMD_CLASS_VERS = [
  0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO_V2 (Insecure)
  0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE_V2 (Insecure)
  0x98: 1, // COMMAND_CLASS_SECURITY_V1 (Insecure)
  0x9F: 1, // COMMAND_CLASS_SECURITY_2_V1 (Insecure)
  0x6C: 1, // COMMAND_CLASS_SUPERVISION_V1 (Insecure)
  0x22: 1, // COMMAND_CLASS_APPLICATION_STATUS_V1 (Insecure)
  0x20: 2, // COMMAND_CLASS_BASIC_V2 (Secure)
  0x86: 3, // COMMAND_CLASS_VERSION_V3 (Secure)
  0x85: 2, // COMMAND_CLASS_ASSOCIATION_V2 (Secure)
  0x59: 3, // COMMAND_CLASS_ASSOCIATION_GRP_INFO_V3 (Secure)
  0x8E: 3, // COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3 (Secure)
  0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2 (Secure)
  0x80: 1, // COMMAND_CLASS_BATTERY_V1 (Secure)
  0x70: 4, // COMMAND_CLASS_CONFIGURATION_V4 (Secure)
  0x26: 4, // COMMAND_CLASS_SWITCH_MULTILEVEL_V4 (Secure)
  0x31: 11,// COMMAND_CLASS_SENSOR_MULTILEVEL_V11 (Secure)
  0x40: 3, // COMMAND_CLASS_THERMOSTAT_MODE_V3 (Secure)
  0x43: 3, // COMMAND_CLASS_THERMOSTAT_SETPOINT_V3 (Secure)
  0x7A: 5, // COMMAND_CLASS_FIRMWARE_UPDATE_MD (Insecure)
  0x73: 1, // COMMAND_CLASS_POWERLEVEL_V1 (Secure)
  0x87: 3 // COMMAND_CLASS_INDICATOR_V3 (Secure)
]
@Field static List<String> SUPPORTED_THERMOSTAT_MODES=["off", "heat"]
@Field static Map configParams = [
  1: [input: [name: "configParam1", type: "bool", title: "Open window detect", description: "When the measured temp drops the value will close", defaultValue: false, required: true], parameterSize: 1],
  2: [input: [name: "configParam2", type: "bool", title: "Anti-freezing", description:"Opens value when measured temp drops below 5°C", defaultValue: false, required: true], parameterSize: 1],
  3: [input: [name: "configParam3", type: "enum", title: "Temperature Offset", description:"Set value is added or subtracted to actual measured value by sensor. -6 to 6°C", defaultValue: 0, options:[0xFA:"-6°C",0xFB:"-5°C",0xFC:"-4°C",0xFD:"-3°C",0xFE:"-2°C",0xFF:"-1°C",0:"0°C",1:"+1°C",2:"+2°C",3:"+3°C",4:"+4°C",5:"+5°C",6:"+6°C"], required: true], parameterSize: 1],
  4: [input: [name: "configParam4", type: "bool", title: "Away mode", description:"", defaultValue: false, required: true], parameterSize: 1],
  5: [input: [name: "configParam5", type: "bool", title: "Anti-scale", description:"Opens value every two weeks to avoid issues", defaultValue: false, required: true], parameterSize: 1],
  6: [input: [name: "configParam6", type: "enum", title: "Valve opening report threshold", description: "Valve opening level change threshold.<br/>0 Disable, 1-99%", defaultValue: 0, options:[[0:"Disable"], [1:"1%"], [5:"5%"], [10:"10%"], [15:"15%"], [20:"20%"], [25:"25%"], [30:"30%"], [50:"50%"], [75:"75%"], [90:"90%"]], required: true], parameterSize: 1],
  7: [input: [name: "configParam7", type: "enum", title: "Temperature auto report interval", description: "Time interval when to send the temperature report.<br/>0 Disable, 1s-74h", defaultValue: 0, options: [[0:"Disable"], [60:"1m"], [120:"2m"], [180:"3m"], [240:"4m"], [300:"5m"],[600:"10m"], [900:"15m"], [1800:"30m"], [3600:"1h"], [7200:"2h"], [10800:"3h"], [14400:"4h"], [21600:"6h"], [28800:"8h"], [43200:"12h"], [86400:"24h"], [129600:"36h"], [259200:"72h"]], required: true], parameterSize: 4],
  8: [input: [name: "configParam8", type: "enum", title: "Temperature change report threshold", description: "", defaultValue: 5, options: [[0:"Disable"], [1:"0.1°C"], [5:"0.5°C"], [10:"1°C"], [20:"2°C"], [30:"3°C"], [40:"4°C"], [50:"5°C"], [60:"6°C"], [80:"8°C"], [100:"10°C"]], required: true], parameterSize:1],
  9: [input: [name: "configParam9", type: "enum", title: "Battery auto report interval", description: "Time interval when to send the battery level report.<br/>0 Disable, 1s-74h", defaultValue: 0, options: [[0:"Disable"], [3600:"1h"], [7200:"2h"], [14400:"4h"], [28800:"8h"], [43200:"12h"], [86400:"24h"], [129600:"36h"], [259200:"72h"]], required: true], parameterSize:4],
  10: [input: [name: "configParam10", type: "enum", title: "Battery change report threshold", description: "", defaultValue: 5, options: [[0:"Disable"], [1:"1%"], [5:"5%"], [10:"10%"], [15:"15%"], [20:"20%"], [25:"25%"], [30:"30%"], [50:"50%"]], required: true], parameterSize:1],
  11: [input: [name: "configParam11", type: "bool", title: "Enable child lock", description: "", defaultValue: false, required: true], parameterSize:1]
]

void logsOff(){
  log.warn "debug logging disabled..."
  device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void installed() {
  log.info "installed(${VERSION})"
}

void updated() {
  log.debug "updated()"
  log.warn "debug logging is: ${logEnable == true}"
  log.warn "description logging is: ${txtEnable == true}"
  unschedule()
  if (logEnable) runIn(86400, logsOff)
//  runIn (5, configure)
}

void refresh() {
  logger "info", "refresh()"
  List<String> cmds=[]
  cmds.add(secureCmd(zwave.thermostatSetpointV3.thermostatSetpointGet(setpointType: hubitat.zwave.commands.thermostatsetpointv3.ThermostatSetpointSet.SETPOINT_TYPE_HEATING_1)))
  cmds.add(secureCmd(zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: hubitat.zwave.commands.sensormultilevelv11.SensorMultilevelGet.SENSOR_TYPE_TEMPERATURE_VERSION_1)))
  cmds.add(secureCmd(zwave.switchMultilevelV4.switchMultilevelGet()))
  cmds.add(secureCmd(zwave.thermostatModeV2.thermostatModeGet()))
  cmds.add(secureCmd(zwave.batteryV1.batteryGet()))
  sendCommands(cmds)
}

void configure() {
  logger("debug", "configure()")
  setDeviceLimits()
  sendEventWrapper(name: "supportedThermostatFanModes", value: "[off]")
  sendEventWrapper(name: "thermostatFanMode", value: "off")
  sendEventWrapper(name: "supportedThermostatModes", value: "[off, heat]" )
  List<String> cmds=[]
  cmds.add(supervisionEncap(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId)))
  configParams.each { param, data ->
    if (settings[data.input.name] != null) {
      cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name]))
    }
  }
  if (!device.getDataValue("MSR")) {
    cmds.add(secureCmd(zwave.versionV2.versionGet()))
    cmds.add(secureCmd(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
  }
  runIn (cmds.size(), refresh)
  sendCommands(cmds)
}

List<String> configCmd(parameterNumber, size, Boolean boolConfigurationValue) {
  int intval=boolConfigurationValue ? 1 : 0
  return [
	supervisionEncap(zwave.configurationV1.configurationSet(parameterNumber:  parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: intval)),
    secureCmd(zwave.configurationV4.configurationGet(parameterNumber: parameterNumber.toInteger()))
  ]
}

List<String> configCmd(parameterNumber, size, scaledConfigurationValue) {
  // Works only for Size 1 & 4 values!
  List<String> cmds = []
  int intval=scaledConfigurationValue.toInteger()
  if (size==1) {
    if (intval < 0) intval = 256 + intval
    cmds.add(supervisionEncap(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), configurationValue: [intval])))
  } else {
    cmds.add(supervisionEncap(zwave.configurationV4.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: intval)))
  }
  cmds.add(secureCmd(zwave.configurationV4.configurationGet(parameterNumber: parameterNumber.toInteger())))
  return cmds
}

// Parse z-wave commands and verison the device supports.
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

// Multilevel report reports measured temp only
void zwaveEvent(hubitat.zwave.commands.sensormultilevelv11.SensorMultilevelReport cmd) {
  logger("trace", "zwaveEvent(SensorMultilevelReport) - cmd: ${cmd.inspect()}")
  switch (cmd.sensorType) {
    case hubitat.zwave.commands.sensormultilevelv11.SensorMultilevelReport.SENSOR_TYPE_TEMPERATURE_VERSION_1:
      String cmdScale = cmd.scale == 1 ? "F" : "C"
      Double radiatorTemperature = Double.parseDouble(convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)).round(1) 
      sendEventWrapper(name: "temperature", value: radiatorTemperature, unit: "°" + getTemperatureScale(), descriptionText: "Temperature is ${radiatorTemperature} ${"°" + getTemperatureScale()}")
    break
    default:
      logger("warn", "zwaveEvent(SensorMultilevelReport) - Unknown sensorType - cmd: ${cmd.inspect()}")
      return
    break;
  }
}

// Report on battery level received. Warn on low batt.
void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
  logger("trace", "zwaveEvent(BatteryReport) - cmd: ${cmd.inspect()}")
  Map map = [ name: "battery", unit: "%" ]
  if (cmd.batteryLevel == 0xFF) {
    map.value = 1
    map.descriptionText = "Has a low battery"
  } else {
    map.value = cmd.batteryLevel
    map.descriptionText = "Battery is ${cmd.batteryLevel} ${map.unit}"
  }
  sendEventWrapper(map)
  sendEventWrapper(name: "lastBatteryReportReceivedAt", value: String.format('%tF %<tH:%<tM', now()))
}

// On association report, check we have one with hub. If not, set it.
void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
  logger("trace", "zwaveEvent(AssociationReport) - cmd: ${cmd.inspect()}")
  if (cmd.nodeId.any { it == zwaveHubNodeId }) {
    logger("info", "Is associated in group ${cmd.groupingIdentifier}")
  } else if (cmd.groupingIdentifier == 1) {
    logger("info", "Associating in group ${cmd.groupingIdentifier}")
    sendCommands(supervisionEncap(zwave.associationV2.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId)))
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

// Handle S2 Supervision or device may think communication failed. No multi channel support.
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

// Handle zwave events not expected
void zwaveEvent(hubitat.zwave.Command cmd) {
  logger("warn", "zwaveEvent(Command) - Unspecified - cmd: ${cmd.inspect()}")
//  sendEventWrapper(descriptionText: "${device.displayName}: ${cmd}")
}

// Report on supported modes. Expecting on Off and Heat
void zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) { 
  logger("trace", "zwaveEvent(ThermostatModeSupportedReport) - cmd: ${cmd.inspect()}")
  List<String> supportedModes = []
   
  if(cmd.off) { supportedModes << "off" }
  if(cmd.heat) { supportedModes << "heat" }
  if(cmd.cool) { supportedModes << "cool" }
  if(cmd.auto) { supportedModes << "auto" }
  if(cmd.auxiliaryemergencyHeat) { supportedModes << "emergency heat" } //boost
  if(cmd.energySaveHeat) { supportedModes << "cool"}
  if(supportedModes == [ ] ) { supportedModes = SUPPORTED_THERMOSTAT_MODES}
  sendEventWrapper(name: "supportedThermostatModes", value: supportedModes, descriptionText: "Supported Thermostat Modes ${supportedModes.toString()}")
}

// Capture reported manufacturer info.
void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logger("trace", "zwaveEvent(ManufacturerSpecificReport) - cmd: ${cmd.inspect()}")

  if (cmd.manufacturerName) { device.updateDataValue("manufacturer", cmd.manufacturerName) }
  if (cmd.productTypeId) { device.updateDataValue("productTypeId", cmd.productTypeId.toString()) }
  if (cmd.productId) { device.updateDataValue("deviceId", cmd.productId.toString()) }
//  if (cmd.manufacturerId){ device.updateDataValue("manufacturerId", cmd.manufacturerId.toString()) }

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  device.updateDataValue("MSR", msr)
}

// No actions on config report.
void zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
  logger("trace", "zwaveEvent(ConfigurationReport) - cmd: ${cmd.inspect()}")
}

// Report on value percent open. If open then we are heating.
void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelReport cmd){
  logger("trace", "zwaveEvent(SwitchMultilevelReport) - cmd: ${cmd.inspect()}")
  sendEventWrapper(name: "valve", value: cmd.value, unit: "%", descriptionText: "Valve open ${cmd.value}%")
  if (cmd.value > 0) {
    sendEventWrapper(name: "thermostatOperatingState", value: "heating")
  } else{
    sendEventWrapper(name: "thermostatOperatingState", value: "idle")
  }
}

// Basic for Frost Protection mode
// Always reports zero (on). The Thermostat Mode is the value that follows a BasicSet command
void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd){
  logger("trace", "zwaveEvent(BasicReport) - cmd: ${cmd.inspect()}")
  // 0xFF = normall mode, 0x00 Frost Protection
  String nv = cmd.value == 0x00 ? "on" : "off"
  sendEventWrapper(name: "frostProtection", value: nv, descriptionText: "Frost Protection is '${nv}'")
}

// Thermostat Mode report
void zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
  logger("trace", "zwaveEvent(ThermostatModeReport) - cmd: ${cmd.inspect()}")
  Map map = [name: "thermostatMode"]

  switch (cmd?.mode) {
    case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_OFF:
      map.value = "off"
      map.descriptionText = "Thermostat Mode is off"
    break
    case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_HEAT:
      map.value = "heat"
      map.descriptionText = "Thermostat Mode is heat"
    break
    default:
      logger("warn", "zwaveEvent(ThermostatModeReport) - Unknown mode - cmd: ${cmd.inspect()}")
    break;
  }
  sendEventWrapper(map)
}

// Setpoint report. Update multiple states.
void zwaveEvent(hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd) {
  logger("trace", "zwaveEvent(ThermostatSetpointReport) - cmd: ${cmd.inspect()}")
  String cmdScale = cmd.scale == 1 ? "F" : "C"
  Double radiatorTemperature = Double.parseDouble(convertTemperatureIfNeeded(cmd.scaledValue, cmdScale, cmd.precision)).round(1) //reported setpoint
  switch (cmd.setpointType) {
    case hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport.SETPOINT_TYPE_HEATING_1:
      Map map = [unit : "°" + getTemperatureScale(), value: radiatorTemperature]
	  map.name = "thermostatSetpoint"
      sendEventWrapper(map)
	  map.name = "thermostatTemperatureSetpoint"
      sendEventWrapper(map)
	  map.name = "heatingSetpoint"
      sendEventWrapper(map)
    break;
    default:
      logger("warn", "Unknown setpointType ${cmd.setpointType}")
  }
}


// Versions report
void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
  logger("trace", "zwaveEvent(VersionReport) - cmd: ${cmd.inspect()}")
  device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
  device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
  device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

// Handle invalid request response
void zwaveEvent(hubitat.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd) {
  logger("trace", "zwaveEvent(ApplicationRejectedRequest) - cmd: ${cmd.inspect()}")
  logger("warn", "Rejected the last request")
}

// Not sure we will get one
void zwaveEvent(hubitat.zwave.commands.applicationstatusv1.ApplicationBusy cmd) {
  logger("trace", "zwaveEvent(ApplicationBusy) - cmd: ${cmd.inspect()}")
  logger("warn", "Is busy")
}

// Not sure we will get one
void zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
  logger("trace", "zwaveEvent(VersionCommandClassReport) - cmd: ${cmd.inspect()}")
}

// Not sure we will get one
void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
  logger("trace", "zwaveEvent(DeviceSpecificReport) - cmd: ${cmd.inspect()}")
}

// Not sure we will get one
void zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  logger("trace", "zwaveEvent(FirmwareMdReport) - cmd: ${cmd.inspect()}")
}

// Not sure we will get one
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

// Not sure we will get one
void zwaveEvent(hubitat.zwave.commands.securityv1.SecuritySchemeReport cmd) {
  logger("trace", "zwaveEvent(SecuritySchemeReport) - cmd: ${cmd.inspect()}")
}

// Not sure we will get one
void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
  logger("trace", "zwaveEvent(SecurityCommandsSupportedReport) - cmd: ${cmd.inspect()}")
}

// Not sure we will get one
void zwaveEvent(hubitat.zwave.commands.securityv1.NetworkKeyVerify cmd) {
  logger("trace", "zwaveEvent(NetworkKeyVerify) - cmd: ${cmd.inspect()}")
  logger("info", "Secure inclusion was successful")
}

/**
* Device command functions.
*/
// When setting the setpoint, assume we want frost protection only off.
List<String> setHeatingSetpoint(Double degrees) {
  logger("debug", "setHeatingSetpoint() - degrees: ${degrees}")
  sendCommands([
    supervisionEncap(zwave.thermostatSetpointV3.thermostatSetpointSet(setpointType: hubitat.zwave.commands.thermostatsetpointv3.ThermostatSetpointSet.SETPOINT_TYPE_HEATING_1, scale:0, precision: 1, scaledValue: degrees)),
    supervisionEncap(zwave.thermostatModeV3.thermostatModeSet(mode: hubitat.zwave.commands.thermostatmodev3.ThermostatModeSet.MODE_HEAT)),
    secureCmd(zwave.thermostatSetpointV3.thermostatSetpointGet(setpointType: hubitat.zwave.commands.thermostatsetpointv3.ThermostatSetpointSet.SETPOINT_TYPE_HEATING_1)),
    secureCmd(zwave.thermostatModeV3.thermostatModeGet())
  ])
}

List<String> on() {
  logger("debug", "Command on()")
  setThermostatMode(hubitat.zwave.commands.thermostatmodev3.ThermostatModeSet.MODE_HEAT)
}

List<String> heat() {
  logger("debug", "Command heat()")
  setThermostatMode(hubitat.zwave.commands.thermostatmodev3.ThermostatModeSet.MODE_HEAT)
}

List<String> off() {
  logger("debug", "Command off()")
  setThermostatMode(hubitat.zwave.commands.thermostatmodev3.ThermostatModeSet.MODE_OFF)
}

// Mode 0x1F is to put TRV in direct Valve control mode. Not supported in this driver.
List<String> setThermostatMode(Short mode) {
  logger("info", "setThermostatMode ${mode}")
  sendCommands([
      supervisionEncap(zwave.thermostatModeV3.thermostatModeSet(mode: mode)),
      secureCmd(zwave.thermostatModeV3.thermostatModeGet())
    ])
}

void emergencyHeat() {
  logger("info", "emergencyHeat() - Unsupported command for TRV")
}

void auto() {
  logger("info", "auto() - Unsupported command for TRV")
}
 
void cool() {
  logger("info", "cool() - Unsupported command for TRV")
}

void fanAuto() {
  logger("info", "fanAuto() - Unsupported command for TRV")
}

void fanCirculate() {
  logger("info", "fanCirculate() - Unsupported command for TRV")
}

void fanOn() {
  logger("info", "fanOn() - Unsupported command for TRV")
}

void setCoolingSetpoint(Double degrees) {
  logger("info", "setCoolingSetpoint() - Unsupported command for TRV")
}

void setThermostatFanMode(String mode) {
  logger("info", "setThermostatFanMode() - Unsupported command for TRV")
}

private void setDeviceLimits() {
  if (location.temperatureScale=="F") {
    sendEventWrapper(name:"minHeatingSetpoint", value: 41, unit: "F")
    sendEventWrapper(name:"maxHeatingSetpoint", value: 86, unit: "F")
  } else {
    sendEventWrapper(name:"minHeatingSetpoint", value: 5, unit: "C")
    sendEventWrapper(name:"maxHeatingSetpoint", value: 30, unit: "C")
  }
}

/*
* return the Double value of an attribute
*/
private currentDouble(attributeName) {
	if(device.currentValue(attributeName)) {
		return device.currentValue(attributeName).doubleValue()
	}
	else {
		return 0d
	}
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

