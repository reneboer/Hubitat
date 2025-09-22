/*
* Copyright 2025 Rene Boer for HEM8 3 clamp version and redesign.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*
* Inspired by the Aeotec 2 clamp driver at https://aeotec.freshdesk.com/support/solutions/articles/6000278077-setup-home-energy-meter-8-2-clamp-with-hubitat
*/

/*
*
* RELEASE HISTORY:
*     - 1.0: Initial Release
*				3 clamp version.
*				Added finger print for automatic driver assignment on inclusion.
*        Added child devices for Consumption and optionally for Production.
*				Only include measurements for kVarh, kVar, kVah and Power Factor when configured. Note that unsolicitated reports from HEM for these measurements will be handled.
*				Added all device parameters for configuration.
*				Use of CurrentMeter capability that reports amperage not current, so renamed all as appropriate.
*
*/

import groovy.transform.Field
@Field String VERSION = "1.0"

//
// Driver Definition
//
metadata {
    definition(
        name: 'Aeotec Home Energy Meter 8 (3 clamp)',
        namespace: 'reneboer',
        author: 'Rene Boer',
        importUrl: "https://raw.githubusercontent.com/reneboer/Hubitat/refs/heads/main/Aeotec/Aeotec%20Home%20Energy%20Meter%208%20(3%20clamp).groovy"
    ) {
        capability 'Refresh'
//        capability 'Initialize'

        capability 'EnergyMeter'
        capability 'PowerMeter'
        capability 'CurrentMeter'
        capability 'VoltageMeasurement'

        command 'resetEnergy'

        //Total parent special meter values
        attribute "kVar", "string"
        attribute "kVarh", "string"
        attribute "powerFactor", "string"
        
        //clamp 1 Consumption
        attribute "energy-Endpoint-1", "string"
        attribute "power-Endpoint-1", "string"
        attribute "voltage-Endpoint-1", "string"
        attribute "amperage-Endpoint-1", "string"
        attribute "kVar-Endpoint-1", "string"
        attribute "kVarh-Endpoint-1", "string"
        attribute "PF-Endpoint-1", "string"
        
        //clamp 1 Production
        attribute "energy-Endpoint-2", "string"
        attribute "power-Endpoint-2", "string"
        attribute "voltage-Endpoint-2", "string"
        attribute "amperage-Endpoint-2", "string"
        attribute "kVar-Endpoint-2", "string"
        attribute "kVarh-Endpoint-2", "string"
        attribute "powerFactor-Endpoint-2", "string"
        
        //clamp 2 Consumption
        attribute "energy-Endpoint-3", "string"
        attribute "power-Endpoint-3", "string"
        attribute "voltage-Endpoint-3", "string"
        attribute "amperage-Endpoint-3", "string"
        attribute "kVar-Endpoint-3", "string"
        attribute "kVarh-Endpoint-3", "string"
        attribute "powerFactor-Endpoint-3", "string"
        
        //clamp 2 Production
        attribute "energy-Endpoint-4", "string"
        attribute "power-Endpoint-4", "string"
        attribute "voltage-Endpoint-4", "string"
        attribute "amperage-Endpoint-4", "string"
        attribute "kVar-Endpoint-4", "string"
        attribute "kVarh-Endpoint-4", "string"
        attribute "powerFactor-Endpoint-4", "string"
        
        //clamp 3 Consumption
        attribute "energy-Endpoint-5", "string"
        attribute "power-Endpoint-5", "string"
        attribute "voltage-Endpoint-5", "string"
        attribute "amperage-Endpoint-5", "string"
        attribute "kVar-Endpoint-5", "string"
        attribute "kVarh-Endpoint-5", "string"
        attribute "powerFactor-Endpoint-5", "string"
        
        //clamp 3 Production
        attribute "energy-Endpoint-6", "string"
        attribute "power-Endpoint-6", "string"
        attribute "voltage-Endpoint-6", "string"
        attribute "amperage-Endpoint-6", "string"
        attribute "kVar-Endpoint-6", "string"
        attribute "kVarh-Endpoint-6", "string"
        attribute "powerFactor-Endpoint-6", "string"

        //Total Consumption
        attribute "energy-Endpoint-7", "string"
        attribute "power-Endpoint-7", "string"
        attribute "voltage-Endpoint-7", "string"
        attribute "amperage-Endpoint-7", "string"
        attribute "kVar-Endpoint-7", "string"
        attribute "kVarh-Endpoint-7", "string"
        attribute "powerFactor-Endpoint-7", "string"
        
        //Total Production
        attribute "energy-Endpoint-8", "string"
        attribute "power-Endpoint-8", "string"
        attribute "voltage-Endpoint-8", "string"
        attribute "amperage-Endpoint-8", "string"
        attribute "kVar-Endpoint-8", "string"
        attribute "kVarh-Endpoint-8", "string"
        attribute "powerFactor-Endpoint-8", "string"
        
        //Endpoint 9 Total Consumption
        attribute "energy-Endpoint-9", "string" //if EP5 > EP6, then EP7=EP5-EP6
        attribute "power-Endpoint-9", "string" //if deltaEP5 > deltaEP6, then EP7=EP7+(deltaEP5-deltaEP6) Else EP7=EP7 previous value
        
        //Endpoint 10 Total Production
        attribute "energy-Endpoint-10", "string" //if EP5 < EP6, then EP8=EP6-EP5
        attribute "power-Endpoint-10", "string" //if deltaEP5 < deltaEP6, then EP8=EP8+(deltaEP6-deltaEP5) Else EP8=EP8 previous value

        fingerprint mfr: "0371", prod: "0003", deviceId: "0034", inClusters: "0x5E,0x98,0x9F,0x55,0x22,0x85,0x59,0x8E,0x70,0x5A,0x7A,0x87,0x72,0x32,0x60,0x73,0x6C,0x86", deviceJoinName: "Aeotec HEM 8"
        fingerprint mfr: "0371", prod: "0003", deviceId: "0034", inClusters: "0x5E,0x98,0x9F,0x55,0x22,0x6C", secureInClusters: "0x85,0x59,0x8E,0x70,0x5A,0x7A,0x87,0x72,0x32,0x60,0x73,0x86", deviceJoinName: "Aeotec HEM 8"
    }

    preferences {
      	input name: 'parentReportType', type: 'enum', title: 'Parent Report Type', defaultValue: 'C', options: ['C': "Consumption (EP7)", 'P': "Production (EP8)"], required: true, description: "Update parent values with Consumption or Production values"
      	input name: 'includeProduction', type: 'enum', title: 'Include production end points', defaultValue: '0', options: ['0': "No", '1': "Yes"], required: true, description: "Include reporting and child devices for reporting end points."
      	input name: 'includeSpecials', type: 'enum', title: 'Include special reports', defaultValue: '0', options: ['0': "No", '1': "Yes"], required: true, description: "Include reporting for kVarh, kVar, kVah and Power Factor on Refresh."

  		parameterMap.each {
        	input (
          		name: it.key,
          		title: "${it.num}. ${it.title}",
          		type: it.type,
          		options: it.options,
          		range: (it.min != null && it.max != null) ? "${it.min}..${it.max}" : null,
          		defaultValue: it.def,
                description: (it.description != null) ? "${it.description}" : null,
          		required: false
        	)
      	}
      	input name: 'logEnable', type: 'bool', title: 'Enable Debug Logging', defaultValue: false, required: true
        input name: 'txtEnable', type: 'bool', title: 'Enable description Text logging', defaultValue: false, required: true
    }
}
//
// STATIC DATA
//
// You need the table below to calculate values to use
/*@Field def reportOptions = [
    0:       'None',
    1:       'kWh Total', 
    2:       'W Total',
    4:       'V Total',
    8:       'A Total', 
    16:      'kVarh Total', 
    32:      'kVar Total', 
    64:      'kVah Total', 
    128:     'powerFactor Total', 
    256:       'kWh Clamp 3', 
    512:       'W Clamp 3',
    1024:      'V Clamp 3',
    2048:      'A Clamp 3',
    4096:      'kVarh Clamp 3',
    8192:      'kVar Clamp 3',
    16384:     'kVah Clamp 3',
    32768:     'PF Clamp 3',
    65536:     'kWh Clamp 2',
    131072:    'W Clamp 2',
    262144:    'V Clamp 2',
    524288:    'A Clamp 2',
    1048576:   'kVarh Clamp 2',
    2097152:   'kVar Clamp 2',
    4194304:   'kVah Clamp 2',
    8388608:   'PF Clamp 2',
    16777216:  'kWh Clamp 1',
    33554432:  'W Clamp 1',
    67108864:  'V Clamp 1',
    134217728: 'A Clamp 1'
    268435456: 'kVarh Clamp 1',
    536870912: 'kVar Clamp 1',
    1073741824:'kVah Clamp 1',
    2147483648:'PF Clamp 1'

]*/
// report type definitions by meter report scale, scale 3 does not return a value (Aeotec Meter 8 is running firmware version: 1.7).
@Field static reportTypes = [
    0: [type: "energy", unit: "kWh", special: false, description: "consumed"],
    1: [type: "kVAh", unit: "kVAh", special: true, description: "consumed"],
    2: [type: "power", unit: "W", special: false, description: "consumes"],
    3: [type: "cycles", unit: "Hz", special: true, description: "level"],
    4: [type: "voltage", unit: "V", special: false, description: "level"],
    5: [type: "amperage", unit: "A", special: false, description: "consumes"],
    6: [type: "powerFactor", unit: "cos(φ)", special: true, description: "level"],
    7: [hasScale2: true, 0: [type: "kvar", unit: "kvar", special: true, description: "consumes"], 1: [type: "kvarh", unit: "kvarh", special: true, description: "consumed"]]
]
// List of end points
@Field static endPointTypes = [
    1: [type: 'C', clamp: 1, scales: [0,1,2,4,5,6,7]], 
    2: [type: 'P', clamp: 1, scales: [0,1,2,4,5,6,7]], 
    3: [type: 'C', clamp: 2, scales: [0,1,2,4,5,6,7]], 
    4: [type: 'P', clamp: 2, scales: [0,1,2,4,5,6,7]], 
    5: [type: 'C', clamp: 3, scales: [0,1,2,4,5,6,7]], 
    6: [type: 'P', clamp: 3, scales: [0,1,2,4,5,6,7]], 
    7: [type: 'C', clamp: 0, scales: [0,1,2,4,5,6,7], parent: true], // Total Consumption, can be used for parent values
    8: [type: 'P', clamp: 0, scales: [0,1,2,4,5,6,7], parent: true], // Total Production, can be used for parent values
    9: [type: 'C', clamp: 0, scales: [0,2]], // Total calculated consumption minus production
    10: [type: 'P', clamp: 0, scales: [0,2]], // Total calculated consumption minus production
]
    
@Field static commandClasses = [
    0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO V2COMMAND_CLASS_VERSION V2
    0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC V2
    0x86: 3, // COMMAND_CLASS_VERSION_V3
    0x32: 5, // COMMAND_CLASS_METER V5/6?
    0x56: 1, // COMMAND_CLASS_CRC_16_ENCAP V1
    0x60: 4, // COMMAND_CLASS_MULTI_CHANNEL V4
    0x8E: 3, // COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
    0x70: 4, // COMMAND_CLASS_CONFIGURATION V4
    0x59: 1, // COMMAND_CLASS_ASSOCIATION_GRP_INFO V1
    0x85: 2, // COMMAND_CLASS_ASSOCIATION V2
    0x7A: 2, // COMMAND_CLASS_FIRMWARE_UPDATE_MD  V2
    0x73: 1, // COMMAND_CLASS_POWERLEVEL V1
    0x98: 1, // COMMAND_CLASS_SECURITY V1
    //0xEF: 1, // COMMAND_CLASS_MARK V1
    0x5A: 1 // COMMAND_CLASS_DEVICE_RESET_LOCALLY V1
]

@Field static parameterMap = [
	[key: "reportingThreshold", title: "Reporting Threshold", type: "enum", options: [0: "0 - disable", 1: "1 - enable"], num: 3, size: 1, def: 1, min: 0, max: 1, description: "Enable selective reporting only when power change reaches a certain threshold."],
	[key: "thresholdTotalConsumption", title: "Total Consumption threshold", type: "number", num: 4, size: 2, def: 50, min: 5, max: 60000, description: "Threshold change in Consumption wattage to induce an automatic report (Whole HEM)."], 
	[key: "thresholdClamp1Consumption", title: "Clamp 1 Consumption threshold", type: "number", num: 5, size: 2, def: 50, min: 5, max: 60000, description: "Threshold change in Consumption wattage to induce an automatic report (Clamp 1)."], 
	[key: "thresholdClamp2Consumption", title: "Clamp 2 Consumption threshold", type: "number", num: 6, size: 2, def: 50, min: 5, max: 60000, description: "Threshold change in Consumption wattage to induce an automatic report (Clamp 2)"], 
	[key: "thresholdClamp3Consumption", title: "Clamp 3 Consumption threshold", type: "number", num: 7, size: 2, def: 50, min: 5, max: 60000, description: "Threshold change in Consumption wattage to induce an automatic report (Clamp 3)"], 
	[key: "thresholdTotalProduction", title: "Total Production threshold", type: "number", num: 8, size: 2, def: 50, min: 5, max: 60000, description: "Threshold change in Production wattage to induce an automatic report (Whole HEM)."], 
	[key: "thresholdClamp1Production", title: "Clamp 1 Production threshold", type: "number", num: 9, size: 2, def: 50, min: 5, max: 60000, description: "Threshold change in Production wattage to induce an automatic report (Clamp 1)."], 
	[key: "thresholdClamp2Production", title: "Clamp 2 Production threshold", type: "number", num: 10, size: 2, def: 50, min: 5, max: 60000, description: "Threshold change in Production wattage to induce an automatic report (Clamp 2)."], 
	[key: "thresholdClamp3Production", title: "Clamp 3 Production threshold", type: "number", num: 11, size: 2, def: 50, min: 5, max: 60000, description: "Threshold change in Production wattage to induce an automatic report (Clamp 3)."], 
	[key: "percentageTotalConsumption", title: "Total Consumption percentage", type: "number", num: 12, size: 1, def: 20, min: 1, max: 100, description: "Percentage change in Consumption wattage to induce an automatic report (Whole HEM)."], 
	[key: "percentageClamp1Consumption", title: "Clamp 1 Consumption percentage", type: "number", num: 13, size: 1, def: 20, min: 1, max: 100, description: "Percentage change in Consumption wattage to induce an automatic report (Clamp 1)."], 
	[key: "percentageClamp2Consumption", title: "Clamp 2 Consumption percentage", type: "number", num: 14, size: 1, def: 20, min: 1, max: 100, description: "Percentage change in Consumption wattage to induce an automatic report (Clamp 2)."], 
	[key: "percentageClamp3Consumption", title: "Clamp 3 Consumption percentage", type: "number", num: 15, size: 1, def: 20, min: 1, max: 100, description: "Percentage change in Consumption wattage to induce an automatic report (Clamp 3)."],
	[key: "percentageTotalProduction", title: "Total Production percentage", type: "number", num: 16, size: 1, def: 20, min: 1, max: 100, description: "Percentage change in Production wattage to induce an automatic report (Whole HEM)."], 
	[key: "percentageClamp1Production", title: "Clamp 1 Production percentage", type: "number", num: 17, size: 1, def: 20, min: 1, max: 100, description: "Percentage change in Production wattage to induce an automatic report (Clamp 1)."], 
	[key: "percentageClamp2Production", title: "Clamp 2 Production percentage", type: "number", num: 18, size: 1, def: 20, min: 1, max: 100, description: "Percentage change in Production wattage to induce an automatic report (Clamp 2)."], 
	[key: "percentageClamp3Production", title: "Clamp 3 Production percentage", type: "number", num: 19, size: 1, def: 20, min: 1, max: 100, description: "Percentage change in Production wattage to induce an automatic report (Clamp 3)."],
	[key: "group1ReportValuesConsumption", title: "Group 1 Reports Consumption", type: "number", num: 101, size: 4, def: 50529027, min: 0, max: 4294967295, description: "Configure which report needs to be sent in Consumption Report group 1"],
	[key: "group2ReportValuesConsumption", title: "Group 2 Reports Consumption", type: "number", num: 102, size: 4, def: 202116108, min: 0, max: 4294967295, description: "Configure which report needs to be sent in Consumption Report group 2"],
	[key: "group3ReportValuesConsumption", title: "Group 3 Reports Consumption", type: "number", num: 103, size: 4, def: 4042322160, min: 0, max: 4294967295, description: "Configure which report needs to be sent in Consumption Report group 3"],
	[key: "group1ReportValuesProduction", title: "Group 1 Reports Production", type: "number", num: 104, size: 4, def: 50529027, min: 0, max: 4294967295, description: "Configure which report needs to be sent in Production Report group 1"],
	[key: "group2ReportValuesProduction", title: "Group 2 Reports Production", type: "number", num: 105, size: 4, def: 202116108, min: 0, max: 4294967295, description: "Configure which report needs to be sent in Production Report group 2"],
	[key: "group3ReportValuesProduction", title: "Group 3 Reports Production", type: "number", num: 106, size: 4, def: 4042322160, min: 0, max: 4294967295, description: "Configure which report needs to be sent in Production Report group 3"],
	[key: "group1ReportIntervalConsumption", title: "Group 1 Consumption time interval", type: "number", num: 111, size: 4, def: 3600, min: 1, max: 4294967295, description: "Set the interval time of sending report in Consumption Report group 1"],
	[key: "group2ReportIntervalConsumption", title: "Group 2 Consumption time interval", type: "number", num: 112, size: 4, def: 7200, min: 1, max: 4294967295, description: "Set the interval time of sending report in Consumption Report group 2"],
	[key: "group3ReportIntervalConsumption", title: "Group 3 Consumption time interval", type: "number", num: 113, size: 4, def: 7200, min: 1, max: 4294967295, description: "Set the interval time of sending report in Consumption Report group 3"],
	[key: "group1ReportIntervalProduction", title: "Group 1 Production time interval", type: "number", num: 114, size: 4, def: 3600, min: 1, max: 4294967295, description: "Set the interval time of sending report in Production Report group 1"],
	[key: "group2ReportIntervalProduction", title: "Group 2 Production time interval", type: "number", num: 115, size: 4, def: 7200, min: 1, max: 4294967295, description: "Set the interval time of sending report in Production Report group 2"],
	[key: "group3ReportIntervalProduction", title: "Group 3 Production time interval", type: "number", num: 116, size: 4, def: 7200, min: 1, max: 4294967295, description: "Set the interval time of sending report in Production Report group 3"]
]

// Handle device installation
void installed() {
    logInfo "installed(${VERSION})"
  	runIn (10, 'getConfig')  // Get current device config after installed.
  	runIn (15, 'refresh')  // Get the measurements from the device.
//    runIn (15, 'createChildDevices')  // Will create with wrong names if done before device name is entered by user.
}
// Handle device removal
def uninstalled() {
	logDebug "${device.label} uninstalled()"
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
// Handle preferences saved and update device configuration accordingly.
void updated() {
    logDebug "Entering updated()"
  	logWarn "debug logging is: ${logEnable == true}"
  	logWarn "description logging is: ${txtEnable == true}"
    unschedule()
    createChildDevices()
   	List<hubitat.zwave.Command> commands = []
    for ( param in parameterMap ) {
      	if ( this["$param.key"] != null && (state."$param.key".toString() != this["$param.key"].toString() )) {
          	commands << zwave.configurationV4.configurationSet(scaledConfigurationValue: this["$param.key"].toInteger(), parameterNumber: param.num, size: param.size)
      	}
    }
    runCommandsWithInterstitialDelay(commands, 300)
    runIn(10, 'getConfig')
  	if (logEnable) runIn(3600, 'logsOff')
}
// Disable debug after enabled in settings
private void logsOff(){
  	logWarn "debug logging disabled..."
  	device.updateSetting("logEnable",[value:"false",type:"bool"])
}
// For testing child devices
void initialize() {
    logDebug "Entering initialize()"
    state.clear()
    removeChildDevices(getChildDevices())
}
// Handle Refresh command
void refresh(Integer ep = null) {
    logDebug "Entering refresh()"
    runCommandsWithInterstitialDelay refreshAllDataCommands
}
// Reset energy and update measurements.
void resetEnergy(Integer ep = null) {
    logDebug "Entering resetEnergy()"
    List<hubitat.zwave.Command> commands = [
        zwave.meterV4.meterReset()
    ]
    commands.addAll refreshAllDataCommands 
    runCommandsWithInterstitialDelay commands
}
// Request parameters from device.
private void getConfig() {
    logDebug "Entering getConfig()"
   	List<hubitat.zwave.Command> commands = []
    for ( param in parameterMap ) {
      if ( this["$param.key"] != null && state."$param.key".toString() != this["$param.key"].toString() ) {
          commands << zwave.configurationV4.configurationGet(parameterNumber: param.num)
      } 
    }
    commands << zwave.versionV3.versionGet()
    if (!device.getDataValue("MSR")) commands << zwave.manufacturerSpecificV2.manufacturerSpecificGet()
    runCommandsWithInterstitialDelay commands
}
// Return all required meterGet commands per the settings.
private List<hubitat.zwave.Command> getRefreshAllDataCommands(){
    List<hubitat.zwave.Command> cmds = []
    // Loop over endpoint types and see if production types should be included or not.
    endPointTypes.each { ep, data ->
    	if (data.type == 'C' || (data.type == 'P' && includeProduction == '1')) {
          	// Loop over scales and see if scale should be included or not.
          	data.scales.each { 
	            def scale = reportTypes[it]
    	        if (scale) {
                  	if (scale.special == false || includeSpecials == '1') {
                      	cmds.add(zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: ep).encapsulate(zwave.meterV5.meterGet(scale: it)))
              		}
            	}
          	}
    	}
  	}
    return cmds
}
// Wrapper for sendEvent to limit duplicate events and support logging
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
// Create the child devices if not yet exsisting
// We look only for endpoints 1 to 6 for Consumption and Produciton and not 7-10 for Totals
private void createChildDevices() {
    (1..6).each() {
        Map epd = endPointTypes[it]
       	def cd = getChildDevice("${device.deviceNetworkId}-${it}")
        String action = 'N'
        if (epd?.type == 'C' && !cd) action = 'A'
        if (epd?.type == 'P' && includeProduction == '1' && !cd) action = 'A'
        if (epd?.type == 'P' && includeProduction == '0' && cd) action = 'D'
     	if (action == 'A') {
           	logDebug "Adding child device for endpoint $it"
           	addChildDevice(
               	"reneboer",
                "Aeotec Home Energy Meter 8 Child Device", 
                "${device.deviceNetworkId}-${it}", 
                [isComponent: true, name: "${device.displayName} (Clamp ${epd.clamp} ${epd?.type == 'C' ? 'Consumption' : 'Production'})"]
            )
        } else if (action == 'D') {
            logDebug "Deleting child device for endpoint $it"
            deleteChildDevice("${device.deviceNetworkId}-${it}")
        } 
    }
}

// ZWave command parser
void parse(String description) {
    logDebug "Entering parse()"
    logDebug "description: ${description.inspect()}"
    hubitat.zwave.Command cmd = zwave.parse(description, commandClasses)
    if (cmd) {
        zwaveEvent(cmd)
    } else {
        logErr("Command not able to be parsed: $description")
    }
}
// Catch unhandled zwave event
void zwaveEvent(hubitat.zwave.Command cmd) {
  	logErr "Unhandled zwaveEvent: $cmd"
}
//COMMAND_CLASS_ZWAVEPLUS_INFO V2
void zwaveEvent(hubitat.zwave.commands.zwaveplusinfov2.ZwaveplusInfoReport cmd) {
    logDebug 'zwaveEvent(hubitat.zwave.commands.zwaveplusinfov2.ZwaveplusInfoReport cmd)'
}
//COMMAND_CLASS_VERSION V3
void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
    logDebug "zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd)"
  	device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
  	device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
  	device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
  	if (cmd.firmwareTargets > 0) {
    	cmd.targetVersions.each { target ->
      		device.updateDataValue("firmware${target.target}Version", "${target.version}.${target.subVersion}")
    	}
  	}
}
//COMMAND_CLASS_MANUFACTURER_SPECIFIC V2
void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    logDebug 'zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd)'
    if (cmd.manufacturerName) { device.updateDataValue("manufacturer", cmd.manufacturerName) }
  	if (cmd.productTypeId) { device.updateDataValue("productTypeId", cmd.productTypeId.toString()) }
  	if (cmd.productId) { device.updateDataValue("deviceId", cmd.productId.toString()) }
  	device.updateDataValue("MSR", String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId))
}
//COMMAND_CLASS_METER V5 (V6 not yet supported in Hubitat
void zwaveEvent(hubitat.zwave.commands.meterv5.MeterReport cmd, int endpoint = 0) {
    logDebug "zwaveEvent(hubitat.zwave.commands.meterv5.MeterReport cmd, int endpoint = $endpoint, scale = $cmd.scale)"

    String source = endpoint == 0 ? '' : "-Endpoint-$endpoint"
    Map measurement = reportTypes[cmd.scale as Integer]
    if (measurement?.hasScale2) {
      	measurement = reportTypes[cmd.scale as Integer][cmd.scale2 as Integer]
    }
    if (measurement) {
      	sendEventWrapper(name: "$measurement.type$source", value: cmd.scaledMeterValue.toFloat(), unit: measurement.unit, descriptionText:"endpoint: ${endpoint} ${measurement.description} ${cmd.scaledMeterValue} ${measurement.unit}")
      
		// Update parent with totals value
      	if (endpoint > 0) {
        	Map epd = endPointTypes[endpoint]
        	if (epd?.parent && epd?.type == parentReportType) {
        		sendEventWrapper(name: measurement.type, value: cmd.scaledMeterValue.toFloat(), unit: unit, descriptionText:"${measurement.description} ${cmd.scaledMeterValue} ${measurement.unit}") 
        	}
      		// Update child with value
        	def cd = getChildDevice("${device.deviceNetworkId}-${endpoint}")
      		if (cd) {
          		logDebug "Updating child device ${device.deviceNetworkId}-${endpoint}, scale ${cmd.scale}"
          		cd.sendEvent([name: measurement.type, value: cmd.scaledMeterValue, unit: measurement.unit])
        	}
      	}
    } else {
      	logWarn "Scale not implemented. ${cmd.scale}, ${cmd.scale2}: ${cmd.scaledMeterValue}"
    }
}
//COMMAND_CLASS_CRC_16_ENCAP V1
// CRC16 handled by Hubitat C-7

//COMMAND_CLASS_MULTI_CHANNEL V4
void zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd) {
    logDebug "Entering zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd)"
    zwaveEvent cmd.encapsulatedCommand(), cmd.sourceEndPoint.toInteger()
}
//COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
void zwaveEvent(hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport cmd) {
    logDebug 'hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport'

    if (!state.multiChannelAssociations) state.multiChannelAssociations = []
    if (!state.multiChannelAssociationsMultiChannelNodeIDs) state.multiChannelAssociationsMultiChannelNodeIDs = []
    state.multiChannelAssociations[cmd.groupingIdentifier] = cmd.nodeId
    state.multiChannelAssociationsMultiChannelNodeIDs[cmd.groupingIdentifier] = cmd.multiChannelNodeIds
}
//COMMAND_CLASS_CONFIGURATION V1
void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    logInfo "$device.displayName has value '$cmd.scaledConfigurationValue' for property with ID '$cmd.parameterNumber'"
}
//COMMAND_CLASS_ASSOCIATION_GRP_INFO V1
    //We don't need to support this one. 
//COMMAND_CLASS_ASSOCIATION V2
void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    logDebug 'hubitat.zwave.commands.associationv2.AssociationReport'
    if (!state.associatedNodes) {
        state.associatedNodes = []
    }
    state.associatedNodes[cmd.groupingIdentifier] = cmd.nodeId
}
//COMMAND_CLASS_FIRMWARE_UPDATE_MD  V2
    //No Need for us to do anything with this CC
//COMMAND_CLASS_POWERLEVEL V1
    //Not going to handle this here, info is available on Z-Wave Settings page
//COMMAND_CLASS_SECURITY V1
    // Security handled by Hubitat on C-7
//COMMAND_CLASS_DEVICE_RESET_LOCALLY V1
void zwaveEvent(hubitat.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
    logErr "WARNING: $device.displayName sent a DeviceResetLocallyNotification!"
}
//COMMAND_CLASS_MULTI_CHANNEL V4
void zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelEndPointReport cmd) {
    logDebug "MultiChannelEndPointReport: dynamic: ${cmd.dynamic} endPoints: ${cmd.endPoints} identical: ${cmd.identical}"
    createChildDevices()
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