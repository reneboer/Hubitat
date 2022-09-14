/**
 *  Qubino DIN Dimmer no Temp
 *	Device Handler 
 *	Version 1.06
 *  Date: 14.9.2022
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
 *	- Implement Multichannel Association Command Class to add MC Association functionality and support configurable inputs.
 *  - Implement secure mode
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
 */
metadata {
	definition (name: "Qubino DIN Dimmer no Temp", namespace: "Goap", author: "Kristjan Jam&scaron;ek") {
		capability "Actuator"
		capability "Switch"
		capability "Switch Level"
		capability "Power Meter"
        capability "EnergyMeter"
		
		capability "Relay Switch"	// - Tagging capability
		capability "Light"			// - Tagging capability
		capability "Sensor"			// - Tagging capability for configurable inputs
		
		capability "Configuration" //Needed for configure() function to set any specific configurations
		// capability "Temperature Measurement" //This capability is valid for devices with temperature sensors connected
		
		//attribute "kwhConsumption", "number" //attribute used to store and display power consumption in KWH 

		command "setConfiguration" //command to issue Configuration Set commands to the module according to user preferences
		command "setAssociation" //command to issue Association Set commands to the modules according to user preferences
		command "refreshPowerConsumption" //command to issue Meter Get requests for KWH measurements from the device, W are already shown as part of Pwer MEter capability
		command "resetPower" //command to issue Meter Reset commands to reset accumulated pwoer measurements
		
        fingerprint mfr:"0159", prod:"0001", model:"0052"  //Manufacturer Information value for Qubino DIN Dimmer
	}


	preferences {
/**
*			--------	CONFIGURATION PARAMETER SECTION	--------
*/
                input (
					type: "paragraph",
					element: "paragraph",
					title: "CONFIGURATION PARAMETERS:",
					description: "Configuration parameter settings."
				)
				input name: "param1", type: "enum", required: false, defaultValue: 0,
					options: ["0" : "0 - mono-stable switch type (push button)",
							  "1" : "1 - Bi-stable switch type"],
					title: "1. Input 1 switch type."
				
				input name: "param5", type: "enum", required: false, defaultValue: 0,
					options: ["0" : "0 - Dimmer mode",
							  "1" : "1 - Switch mode"],
					title: "5. Module function."
						   
				input name: "param10", type: "enum", required: false, defaultValue: 255,
					options: ["0" : "0 - ALL ON is not active, ALL OFF is not active",
							  "1" : "1 - ALL ON is not active, ALL OFF active",
							  "2" : "2 - ALL ON active, ALL OFF is not active",
							  "255" : "255 - ALL ON active, ALL OFF active"],
					title: "10. Activate / deactivate functions ALL ON / ALL OFF."
				
				input name: "param11", type: "number", range: "0..32536", required: false, defaultValue: 0,
					title: "11. Automatic turning off output after set time in seconds."
							
				input name: "param12", type: "number", range: "0..32536", required: false, defaultValue: 0,
					title: "12. Automatic turning on output after set time in seconds."

				input name: "param21", type: "enum", required: false, defaultValue: 0,
					options: ["0" : "0 - Double click disabled",
							  "1" : "1 - Double click enabled"],
					title: "21. Enable/Disable Double click function."
							
				input name: "param30", type: "enum", required: false, defaultValue: 0,
					options: ["0" : "0 - DIN Dimmer module saves its state before power failure (it returns to the last position saved before a power failure)",
							  "1" : "1 - DIN Dimmer module does not save the state after a power failure, it returns to 'off' position"],
					title: "30. Saving the state of the device after a power failure."
				
				input name: "param40", type: "number", range: "0..100", required: false, defaultValue: 5,
					title: "40. Power reporting in Watts on power change."
							
				input name: "param42", type: "number", range: "0..32767", required: false, defaultValue: 0,
					title: "42. Power reporting in Watts by time interval."

				input name: "param60", type: "number", range: "1..98", required: false, defaultValue: 1,
					title: "60. Minimum dimming value."

				input name: "param61", type: "number", range: "1..99", required: false, defaultValue: 99,
					title: "61. Maximum dimming value."

				input name: "param65", type: "number", range: "50..255", required: false, defaultValue: 100,
					title: "65. Dimming time (soft on/off) in mili seconds."

				input name: "param66", type: "number", range: "1..255", required: false, defaultValue: 3,
					title: "66. Dimming time when key pressed in seconds."

				input name: "param67", type: "enum", required: false, defaultValue: 0,
					options: ["0" : "0 - Respect start level",
							  "1" : "1 - Ignore start level"],
					title: "67. Ignore start level."

				input name: "param68", type: "number", range: "0..127", required: false, defaultValue: 0,
					title: "68. Dimming duration in seconds."

				input name: "param110", type: "number", range: "1..32536", required: false, defaultValue: 32356,
					title: "110. Temperature sensor offset settings."

				input name: "param120", type: "number", range: "0..127", required: false, defaultValue: 5,
					title: "120. Digital temperature sensor reporting."
			
/**
*			--------	ASSOCIATION GROUP SECTION	--------
*/
				input (
					type: "paragraph",
					element: "paragraph",
					title: "ASSOCIATION GROUPS:",
					description: "Association group settings. Up to 16 nodes per group.\n" +
						   "NOTE: Insert the node Id value of the devices you wish to associate this group with. Multiple nodeIds can also be set at once by separating individual values by a comma (2,3,...)."
				)
				input name: "assocGroup2", type: "text", required: false,
					title: "Association group 2: \n" +
						   "Basic on/off (triggered at change of the input I1 and reflecting state of the output Q)."
						   
				input name: "assocGroup3", type: "text", required: false,
					title: "Association group 3: \n" +
						   "Start level change/stop level change (triggered at change of the input I1 state and reflecting its state)."
						   
				input name: "assocGroup4", type: "text", required: false,
					title: "Association group 4: \n" +
						   "Multilevel set (triggered at changes of state/value of the DIN Dimmer)."
						   
				input name: "assocGroup5", type: "text", required: false,
					title: "Association group 5: \n" +
						   "Multilevel Sensor Report (triggered at the change od temperature sensor values)."
						   
				input (
					type: "paragraph",
					element: "paragraph",
					title: "Debug settings:",
					description: "Driver debug settings. "
				)
                input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
                input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false

	}
}
/**
*	--------	HELPER METHODS SECTION	--------
*/
/**
 * Converts a list of String type node id values to Integer type.
 *
 * @param stringList - a list of String type node id values.
 * @return stringList - a list of Integer type node id values.
*/
def convertStringListToIntegerList(stringList){
	if (logEnable) log.debug stringList
	if(stringList != null){
		for(int i=0;i<stringList.size();i++){
			stringList[i] = stringList[i].toInteger()
		}
	}
	return stringList
}
/*
*	--------	HANDLE COMMANDS SECTION	--------
*/
/**
 * Configuration capability command handler.
 *
 * @param void
 * @return List of commands that will be executed in sequence with 500 ms delay inbetween.
*/
def configure() {
	if (logEnable) log.debug "Qubino DIN Dimmer: configure()"
	def cmds = []
	cmds << zwave.associationV1.associationRemove(groupingIdentifier:1).format()
	cmds << zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format()
	cmds << zwave.multiChannelV3.multiChannelEndPointGet().format()
	return response(delayBetween(cmds, 1000))
	
}

/**
 * Switch capability command handler for ON state. It issues a Switch Multilevel Set command with value 0xFF and instantaneous dimming duration.
 * This command is followed by a Switch Multilevel Get command, that updates the actual state of the dimmer.
 *		
 * @param void
 * @return void.
*/
def on() {
        delayBetween([
				zwave.switchMultilevelV3.switchMultilevelSet(value: 0xFF, dimmingDuration: 0x00).format(),
				zwave.switchMultilevelV1.switchMultilevelGet().format()
        ], 500)  
}
/**
 * Switch capability command handler for OFF state. It issues a Switch Multilevel Set command with value 0x00 and instantaneous dimming duration.
 * This command is followed by a Switch Multilevel Get command, that updates the actual state of the dimmer.
 *		
 * @param void
 * @return void.
*/
def off() {
        delayBetween([
				zwave.switchMultilevelV3.switchMultilevelSet(value: 0x00, dimmingDuration: 0x00).format(),
				zwave.switchMultilevelV1.switchMultilevelGet().format(),
        		zwave.meterV2.meterGet(scale: 2).format()
        ], 500)
}
/**
 * Switch Level capability command handler for a positive dimming state. It issues a Switch Multilevel Set command with value contained in the parameter value and instantaneous dimming duration.
 * This command is followed by a Switch Multilevel Get command, that updates the actual state of the dimmer. We need to limit the max valueto 99% by Z-Wave protocol definitions.
 *		
 * @param level The desired value of the dimmer we are trying to set.
 * @return void.
*/
def setLevel(level) {
//	if(level > 99) level = 99
	Integer newValue = Math.max(Math.min(level, 99),0)
    delayBetween([
		zwave.switchMultilevelV3.switchMultilevelSet(value: newValue, dimmingDuration: 0x00).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], 500)
}

/**
 * Refresh Power Consumption command handler for updating the cumulative consumption fields in kWh. It will issue a Meter Get command with scale parameter set to kWh.
 *		
 * @param void.
 * @return void.
*/
def refreshPowerConsumption() {
	if (logEnable) log.debug "Qubino DIN Dimmer: refreshPowerConsumption()"
	delayBetween([
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format()
    ], 500)
}
/**
 * Reset Power Consumption command handler for resetting the cumulative consumption fields in kWh. It will issue a Meter Reset command followed by Meter Get commands for active and accumulated power.
 *		
 * @param void.
 * @return void.
*/
def resetPower() {
	if (logEnable) log.debug "Qubino DIN Dimmer: resetPower()"
	zwave.meterV2.meterReset()
	delayBetween([
		zwave.meterV2.meterReset(),
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format()
    ], 500)
}

/**
 * setAssociations command handler that sets user selected association groups. In case no node id is insetred the group is instead cleared.
 * Lifeline association hidden from user influence by design.
 *
 * @param void
 * @return List of Association commands that will be executed in sequence with 500 ms delay inbetween.
*/

def setAssociation() {
	if (logEnable) log.debug "Qubino DIN Dimmer: setAssociation()"
	def assocSet = []
	if(settings.assocGroup2 != null){
		def group2parsed = settings.assocGroup2.tokenize(",")
		if(group2parsed == null){
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:assocGroup2).format()
		}else{
			group2parsed = convertStringListToIntegerList(group2parsed)
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:group2parsed).format()
		}
	}else{
		assocSet << zwave.associationV2.associationRemove(groupingIdentifier:2).format()
	}
	if(settings.assocGroup3 != null){
		def group3parsed = settings.assocGroup3.tokenize(",")
		if(group3parsed == null){
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:assocGroup3).format()
		}else{
			group3parsed = convertStringListToIntegerList(group3parsed)
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:group3parsed).format()
		}
	}else{
		assocSet << zwave.associationV2.associationRemove(groupingIdentifier:3).format()
	}
	if(settings.assocGroup4 != null){
		def group4parsed = settings.assocGroup4.tokenize(",")
		if(group4parsed == null){
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:4, nodeId:assocGroup4).format()
		}else{
			group4parsed = convertStringListToIntegerList(group4parsed)
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:4, nodeId:group4parsed).format()
		}
	}else{
		assocSet << zwave.associationV2.associationRemove(groupingIdentifier:4).format()
	}
	if(settings.assocGroup5 != null){
		def group5parsed = settings.assocGroup5.tokenize(",")
		if(group5parsed == null){
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:5, nodeId:assocGroup5).format()
		}else{
			group5parsed = convertStringListToIntegerList(group5parsed)
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:5, nodeId:group5parsed).format()
		}
	}else{
		assocSet << zwave.associationV2.associationRemove(groupingIdentifier:5).format()
	}
	if(assocSet.size() > 0){
		return delayBetween(assocSet, 500)
	}
}

/**
 * setConfigurationParams command handler that sets user selected configuration parameters on the device. 
 * In case no value is set for a specific parameter the method skips setting that parameter.
 * Secure mode setting hidden from user influence by design.
 *
 * @param void
 * @return List of Configuration Set commands that will be executed in sequence with 500 ms delay inbetween.
*/

def setConfiguration() {
	if (logEnable) log.debug "Qubino DIN Dimmer: setConfiguration()"
	def configSequence = []
	if(settings.param1 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 1, size: 1, scaledConfigurationValue: settings.param1.toInteger()).format()
	}
	if(settings.param5 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 5, size: 1, scaledConfigurationValue: settings.param5.toInteger()).format()
	}
	if(settings.param10 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 10, size: 2, scaledConfigurationValue: settings.param10.toInteger()).format()
	}
	if(settings.param11 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 11, size: 2, scaledConfigurationValue: settings.param11.toInteger()).format()
	}
	if(settings.param12 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 12, size: 2, scaledConfigurationValue: settings.param12.toInteger()).format()
	}
	if(settings.param21 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 21, size: 1, scaledConfigurationValue: settings.param21.toInteger()).format()
	}
	if(settings.param30 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 30, size: 1, scaledConfigurationValue: settings.param30.toInteger()).format()
	}
	if(settings.param40 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 40, size: 1, scaledConfigurationValue: settings.param40.toInteger()).format()
	}
	if(settings.param42 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 42, size: 2, scaledConfigurationValue: settings.param42.toInteger()).format()
	}
	if(settings.param60 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 60, size: 1, scaledConfigurationValue: settings.param60.toInteger()).format()
	}
	if(settings.param61 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 61, size: 1, scaledConfigurationValue: settings.param61.toInteger()).format()
	}
	if(settings.param65 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 65, size: 2, scaledConfigurationValue: settings.param65.toInteger()).format()
	}
	if(settings.param66 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 66, size: 2, scaledConfigurationValue: settings.param66.toInteger()).format()
	}
	if(settings.param67 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 67, size: 1, scaledConfigurationValue: settings.param67.toInteger()).format()
	}
	if(settings.param68 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 68, size: 1, scaledConfigurationValue: settings.param68.toInteger()).format()
	}
	if(settings.param110 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 110, size: 2, scaledConfigurationValue: settings.param110.toInteger()).format()
	}	
	if(settings.param120 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 120, size: 1, scaledConfigurationValue: settings.param120.toInteger()).format()
	}	
	if(configSequence.size() > 0){
		return delayBetween(configSequence, 500)
	}
}

/*
*	--------	EVENT PARSER SECTION	--------
*/
/**
 * parse function takes care of parsing received bytes and passing them on to event methods.
 *
 * @param description String type value of the received bytes.
 * @return Parsed result of the received bytes.
*/
def parse(String description) {
	if (logEnable) log.debug "Qubino DIN Dimmer: Parsing '${description}'"
	def result = null
    def cmd = zwave.parse(description)
    if (cmd) {
		result = zwaveEvent(cmd)
        if (logEnable) log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
		if (logEnable) log.debug "Non-parsed event: ${description}"
    }
    return result
}
/**
 * Catch Event Not Otherwise Handled! 
*/
def zwaveEvent(hubitat.zwave.Command cmd, ep = null) {
	com.hubitat.app.DeviceWrapper targetDevice = getTargetDeviceByEndPoint(ep)
    if (logEnable) log.info "Device ${targetDevice.displayName}: Received Z-Wave Message ${cmd} that is not handled by this driver. Endpoint: ${ep}. Message class: ${cmd.class}."
}
/**
 * Event handler for received Sensor Multilevel Report frames. These are for the temperature sensor connected to TS connector.
 *
 * @param void
 * @return Event that updates the temperature values with received values.
*/
//def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd){
//	log.debug "Qubino DIN Dimmer: SensorMultilevelReport handler fired"
//	def resultEvents = []
//	resultEvents << createEvent(name:"temperature", value: convertDegrees(location.temperatureScale,cmd), unit:"°"+location.temperatureScale, descriptionText: "Temperature: "+convertDegrees(location.temperatureScale,cmd)+"°"+location.temperatureScale)
//	return resultEvents
//    return null
//}
/**
 * Event handler for received Switch Multilevel Report frames.
 *
 * @param void
 * @return List of events to update the ON / OFF and analogue control elements with received values.
*/
def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd){
	if (logEnable) log.debug "Qubino DIN Dimmer: firing switch multilevel event"
	def result = []
	result << createEvent(name:"switch", value: cmd.value ? "on" : "off")
	result << createEvent(name:"level", value: cmd.value, unit:"%", descriptionText:"${device.displayName} dimmed to ${cmd.value==255 ? 100 : cmd.value}%")
	return result
}
/**
 * Event handler for received Meter Report frames. Used for displaying W and kWh measurements.
 *
 * @param void
 * @return Power consumption event for W data or kwhConsumption event for kWh data.
*/
def zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd) {
	if (logEnable) log.debug "Qubino DIN Dimmer: firing meter report event"
	def result = []
	switch(cmd.scale){
		case 0:
            if (logEnable) log.debug("energy report is ${cmd.scaledMeterValue} kWh")
			result << createEvent(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"${device.displayName} consumed ${cmd.scaledMeterValue} kWh")
            break;
		case 2:
            if (logEnable) log.debug("power report is ${cmd.scaledMeterValue} W")
			result << createEvent(name:"power", value: cmd.scaledMeterValue, unit:"W", descriptionText:"${device.displayName} consumes ${cmd.scaledMeterValue} W")
			break;
        default:
			log.warn("skipped cmd: ${cmd}")
    }    
	return result
}

/**
 * Event handler for received Switch Binary Report frames. Used for ON / OFF events.
 *
 * @param cmd Switch Binary Report received from device
 * @return Switch Event with on or off value.
*/
def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	if (logEnable) log.debug "Qubino DIN Dimmer: firing switch binary report event"
    createEvent(name:"switch", value: cmd.value ? "on" : "off")
}
/**
 * Event handler for received Configuration Report frames. Used for debugging purposes. 
 *
 * @param cmd Configuration Report received from device.
 * @return void.
*/
def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd){
	if (logEnable) log.debug "Qubino DIN Dimmer: firing configuration report event"
	if (logEnable) log.debug cmd.configurationValue
}
/**
 * Event handler for received Basic Report frames.
 *
 * @param cmd Basic Report received from device.
 * @return void
*/
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd){
	if (logEnable) log.debug "Qubino DIN Dimmer: firing basic report event"
	if (logEnable) log.debug cmd
}
/**
 * Event handler for received MultiChannelEndPointReport commands. Used to distinguish when the device is in singlechannel or multichannel configuration. 
 *
 * @param cmd communication frame.
 * @return commands to set up a MC Lifeline association.
*/
def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd){
	if (logEnable) log.debug "Qubino DIN Dimmer: firing MultiChannelEndPointReport"
	if(cmd.endPoints > 0){
		state.isMcDevice = true;
		createChildDevices();
	}
	def cmds = []
	cmds << response(zwave.associationV1.associationRemove(groupingIdentifier:1).format())
	cmds << response(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 1, nodeId: [0,zwaveHubNodeId,1]).format())
	return cmds
}
/**
 * Event handler for received Multi Channel Encapsulated commands.
 *
 * @param cmd encapsulated communication frame
 * @return parsed event.
*/
def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd){
	if (logEnable) log.debug "Qubino DIN Dimmer: firing MC Encapsulation event"
	def encapsulatedCommand = cmd.encapsulatedCommand()
	if (encapsulatedCommand) {
			return zwaveEvent(encapsulatedCommand, cmd)
	}
}
/**
 * Event handler for received MC Encapsulated Sensor Multilevel Report frames.
 *
 * @param cmd communication frame, command mc encapsulated communication frame; needed to distinguish sources
 * @return List of events to update the temperature control elements with received values.
*/
def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap command){
	if (logEnable) log.debug "Qubino DIN Dimmer: firing MC sensor multilevel event"
	def result = []
//	result << createEvent(name:"temperature", value: convertDegrees(location.temperatureScale,cmd), unit:"°"+location.temperatureScale, descriptionText: "Temperature: "+convertDegrees(location.temperatureScale,cmd)+"°"+location.temperatureScale, isStateChange: true)
//	return result
    return null
}
/**
 * Event handler for received MC Encapsulated Switch Multilevel Report frames.
 *
 * @param cmd communication frame, command mc encapsulated communication frame; needed to distinguish sources
 * @return List of events to update the ON / OFF and analogue control elements with received values.
*/
def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap command){
    if (logEnable) {
       log.debug "Qubino DIN Dimmer: firing MC switch multilevel event"
	   log.debug cmd
	   log.debug command
    }    
	def result = []
	result << createEvent(name:"switch", value: cmd.value ? "on" : "off")
	result << createEvent(name:"level", value: cmd.value, unit:"%", descriptionText:"${device.displayName} dimmed to ${cmd.value==255 ? 100 : cmd.value}%")
	return result
}
/**
 * Event handler for received MC Encapsulated Meter Report frames.
 *
 * @param cmd communication frame, command mc encapsulated communication frame; needed to distinguish sources
 * @return List of events to update power control elements with received values.
*/
def zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd, hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap command){
    if (logEnable) {
        log.debug "Qubino DIN Dimmer: firing MC Meter Report event"
	    log.debug command
	    log.debug cmd
    }
	def result = []
	switch(cmd.scale){
		case 0:
			result << createEvent(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"${device.displayName} consumed ${cmd.scaledMeterValue} kWh")
			break;
		case 2:
			result << createEvent(name:"power", value: cmd.scaledMeterValue, unit:"W", descriptionText:"${device.displayName} consumes ${cmd.scaledMeterValue} W")
			break;
	}
	return result
}
/**
 * Event handler for received Dimming Duration commands.
 *
 * @param cmd encapsulated communication frame
 * @return parsed event.
*/
def zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelSet cmd){
    if (logEnable) {
        log.debug "Qubino DIN Dimmer: firing Dimming Duration event"
	    log.debug cmd
    }
//	def encapsulatedCommand = cmd.encapsulatedCommand()
//	if (encapsulatedCommand) {
//			return zwaveEvent(encapsulatedCommand, cmd)
//	}
}