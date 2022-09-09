/**
 *  Qubino Smart Plug 16A ZMNHYDx
 *	Device Handler 
 *	Version 1.00
 *  Date: 10.9.2022
 *	Author: Rene Boer
 *  Copyright , none free to use
 *
 * |---------------------------- DEVICE HANDLER FOR QUBINO SMART PLUG 16A Z-WAVE DEVICE -------------------------------------------------------|  
 *	The handler supports all unsecure functions of the Qubino Smart Plug 16A device, except configurable inputs. Configuration parameters and
 *	association groups can be set in the device's preferences screen, but they are applied on the device only after
 *	pressing the 'Set configuration' and 'Set associations' buttons on the bottom of the details view. 
 *
 *	This device handler supports data values that are currently not implemented as capabilities, so custom attribute 
 *	states are used. Please use a SmartApp that supports custom attribute monitoring with this device in your rules.
 * |-----------------------------------------------------------------------------------------------------------------------------------------------|
 *
 *
 *	TO-DO:
 *  - Implement secure mode
 *
 *	CHANGELOG:
 *	0.01: First release
 */
metadata {
	definition (name: "Qubino Smart Plug 16A", namespace: "Goap", author: "Rene Boer") {
		capability "Actuator"
		capability "Switch"
		capability "PowerMeter"
        capability "EnergyMeter"
        capability "VoltageMeasurement"
        capability "CurrentMeter"
		capability "Sensor"
		capability "Configuration" //Needed for configure() function to set any specific configurations

		command "setConfiguration" //command to issue Configuration Set commands to the module according to user preferences
		command "setAssociation" //command to issue Association Set commands to the modules according to user preferences
		command "refreshPowerConsumption" //command to issue Meter Get requests for KWH measurements from the device, W are already shown as part of Pwer MEter capability
		command "resetPower" //command to issue Meter Reset commands to reset accumulated pwoer measurements
		
//		fingerprint  mfr:"0159", prod:"0002", model:"0054", inClusters:"0x5E,0x25,0x85,0x59,0x55,0x86,0x72,0x5A,0x70,0x32,0x71,0x73,0x9F,0x6C,0x7A"
		fingerprint  mfr:"0159", prod:"0002", model:"0054"
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
				input name: "param11", type: "number", range: "0..32536", required: false,
					title: "11. Automatic turning off output after set time in seconds. 0 = Disabled."
							
				input name: "param12", type: "number", range: "0..32536", required: false,
					title: "12. Automatic turning on output after set time in seconds. 0 = Disabled."

				input name: "param30", type: "enum", required: false,
					options: ["0" : "0 - Switch saves its state before power failure (it returns to the last position saved before a power failure)",
							  "1" : "1 - Switch does not save the state after a power failure, it returns to 'off' position"],
					title: "30. Saving the state of the device after a power failure."
				
				input name: "param40", type: "number", range: "0..100", required: false,
					title: "40. Watt Power Consumption Reporting Threshold."
							
				input name: "param42", type: "number", range: "0..32767", required: false,
					title: "42. Power reporting in Watts by time interval."

				input name: "param50", type: "number", range: "0..4000", required: false,
					title: "50. Down value."

				input name: "param51", type: "number", range: "0..4000", required: false,
					title: "51. Up value."

				input name: "param52", type: "enum", required: false,
					options: ["0" : "0 – function inactive",
							  "1" : "1 - turn the associated devices on, once the power drops below Down value",
							  "2" : "2 - turn the associated devices off, once the power drops below Down value",
							  "3" : "3 - turn the associated devices on, once the power rises above Up value",
							  "4" : "4 - turn the associated devices off, once the power rises above Up value",
							  "5" : "5 - 1 and 4 combined",
							  "6" : "6 - 2 and 3 combined"],
					title: "52. Action in case of exceeding defined power values."

				input name: "param70", type: "number", range: "0..4000", required: false,
					title: "70. Overload safety switch. 0 = not active."

				input name: "param71", type: "number", range: "0..4000", required: false,
					title: "71. Power threshold. 0 = not active."

				input name: "param72", type: "number", range: "0..125", required: false,
					title: "72. Time interval."

				input name: "param73", type: "enum", required: false,
					options: ["0" : "0 - function disabled",
							  "1" : "1 - turn OFF relay once the notification Program completed is sent"],
					title: "73. Turn Smart Plug OFF."

				input name: "param74", type: "enum", required: false,
					options: ["0" : "0 - LED is disabled",
							  "1" : "1 - LED is enabled"],
					title: "74. Enable/disable LED."
			
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
						   "Basic on/off."
						   
				input name: "assocGroup3", type: "text", required: false,
					title: "Association group 3: \n" +
						   "Plug Threshold"
						   
                input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
                input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true

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
	if (logEnable) log.debug "Qubino Smart Plug 16A: configure()"
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
	if (logEnable) log.debug "Qubino Smart Plug 16A: on()"
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
    ], 1000)  
}
/**
 * Switch capability command handler for OFF state. It issues a Switch Multilevel Set command with value 0x00 and instantaneous dimming duration.
 * This command is followed by a Switch Multilevel Get command, that updates the actual state of the dimmer.
 *		
 * @param void
 * @return void.
*/
def off() {
	if (logEnable) log.debug "Qubino Smart Plug 16A: off()"
    delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
    ], 1000)
}

/**
 * Refresh Power Consumption command handler for updating the cumulative consumption fields in kWh. It will issue a Meter Get command with scale parameter set to kWh.
 *		
 * @param void.
 * @return void.
*/
def refreshPowerConsumption() {
	if (logEnable) log.debug "Qubino Smart Plug 16A: refreshPowerConsumption()"
	delayBetween([
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format(),
		zwave.meterV2.meterGet(scale: 4).format(),
		zwave.meterV2.meterGet(scale: 5).format()
    ], 1000)
}
/**
 * Reset Power Consumption command handler for resetting the cumulative consumption fields in kWh. It will issue a Meter Reset command followed by Meter Get commands for active and accumulated power.
 *		
 * @param void.
 * @return void.
*/
def resetPower() {
	if (logEnable) log.debug "Qubino Smart Plug 16A: resetPower()"
	zwave.meterV2.meterReset()
	delayBetween([
		zwave.meterV2.meterReset().format(),
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format(),
		zwave.meterV2.meterGet(scale: 4).format(),
		zwave.meterV2.meterGet(scale: 5).format()
    ], 1000)
}

/**
 * setAssociations command handler that sets user selected association groups. In case no node id is insetred the group is instead cleared.
 * Lifeline association hidden from user influence by design.
 *
 * @param void
 * @return List of Association commands that will be executed in sequence with 500 ms delay inbetween.
*/

def setAssociation() {
	if (logEnable) log.debug "Qubino Smart Plug 16A: setAssociation()"
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
	if (logEnable) log.debug "Qubino Smart Plug 16A: setConfiguration()"
	def configSequence = []
	if(settings.param11 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 11, size: 2, scaledConfigurationValue: settings.param11.toInteger()).format()
	}
	if(settings.param12 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 12, size: 2, scaledConfigurationValue: settings.param12.toInteger()).format()
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
	if(settings.param50 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 50, size: 2, scaledConfigurationValue: settings.param50.toInteger()).format()
	}
	if(settings.param51 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 51, size: 2, scaledConfigurationValue: settings.param51.toInteger()).format()
	}
	if(settings.param52 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 52, size: 1, scaledConfigurationValue: settings.param52.toInteger()).format()
	}
	if(settings.param70 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 70, size: 2, scaledConfigurationValue: settings.param70.toInteger()).format()
	}
	if(settings.param71 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 71, size: 2, scaledConfigurationValue: settings.param71.toInteger()).format()
	}
	if(settings.param72 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 72, size: 1, scaledConfigurationValue: settings.param72.toInteger()).format()
	}
	if(settings.param73 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 73, size: 1, scaledConfigurationValue: settings.param73.toInteger()).format()
	}	
	if(settings.param74 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 74, size: 1, scaledConfigurationValue: settings.param74.toInteger()).format()
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
	if (logEnable) log.debug "Qubino Smart Plug 16A: Parsing '${description}'"
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
 * Event handler for received Switch Multilevel Report frames.
 *
 * @param void
 * @return List of events to update the ON / OFF and analogue control elements with received values.
*/
def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd){
	if (logEnable) log.debug "Qubino Smart Plug 16A: firing switch multilevel event"
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
	if (logEnable) log.debug "Qubino Smart Plug 16A: firing meter report event"
	def result = []
	if (cmd.meterType == 1) {
		switch(cmd.scale){
			case 0:
				log.debug("energy report is ${cmd.scaledMeterValue} kWh")
				result << createEvent(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"${device.displayName} consumed ${cmd.scaledMeterValue} kWh")
				break;
			case 1:
				log.debug("energy report is ${cmd.scaledMeterValue} kVah")
				result << createEvent(name:"energy", value: cmd.scaledMeterValue, unit:"kVah", descriptionText:"${device.displayName} consumed ${cmd.scaledMeterValue} kVah")
				break;
			case 2:
				log.debug("power report is ${cmd.scaledMeterValue} W")
				result << createEvent(name:"power", value: cmd.scaledMeterValue, unit:"W", descriptionText:"${device.displayName} consumes ${cmd.scaledMeterValue} W")
				break;
			case 4:
				log.debug("voltage report is ${cmd.scaledMeterValue} V")
				result << createEvent(name:"voltage", value: cmd.scaledMeterValue, unit:"V", descriptionText:"${device.displayName} level ${cmd.scaledMeterValue} V")
				break;
			case 5:
				log.debug("amperage report is ${cmd.scaledMeterValue} A")
				result << createEvent(name:"amperage", value: cmd.scaledMeterValue, unit:"A", descriptionText:"${device.displayName} consumes ${cmd.scaledMeterValue} A")
				break;
			default:
				log.warn("Unsupported scale. Skipped cmd: ${cmd}")
		}
	}
	else {
		log.warn("Unsupported meter type. Skipped cmd: ${cmd}")
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
	if (logEnable) log.debug "Qubino Smart Plug 16A: firing switch binary report event"
    createEvent(name:"switch", value: cmd.value ? "on" : "off")
}
/**
 * Event handler for received Configuration Report frames. Used for debugging purposes. 
 *
 * @param cmd Configuration Report received from device.
 * @return void.
*/
def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd){
	if (logEnable) log.debug "Qubino Smart Plug 16A: firing configuration report event"
	if (logEnable) log.debug cmd.configurationValue
}
/**
 * Event handler for received Basic Report frames.
 *
 * @param cmd Basic Report received from device.
 * @return void
*/
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd){
	if (logEnable) log.debug "Qubino Smart Plug 16A: firing basic report event"
	if (logEnable) log.debug cmd
}
/**
 * Event handler for received MultiChannelEndPointReport commands. Used to distinguish when the device is in singlechannel or multichannel configuration. 
 *
 * @param cmd communication frame.
 * @return commands to set up a MC Lifeline association.
*/
def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd){
	if (logEnable) log.debug "Qubino Smart Plug 16A: firing MultiChannelEndPointReport"
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
	if (logEnable) log.debug "Qubino Smart Plug 16A: firing MC Encapsulation event"
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
	if (logEnable) log.debug "Qubino Smart Plug 16A: firing MC sensor multilevel event"
	def result = []
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
       log.debug "Qubino Smart Plug 16A: firing MC switch multilevel event"
	   log.debug cmd
	   log.debug command
    }    
	def result = []
	result << createEvent(name:"switch", value: cmd.value ? "on" : "off")
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
        log.debug "Qubino Smart Plug 16A: firing MC Meter Report event"
	    log.debug command
	    log.debug cmd
    }
	def result = []
	if (cmd.meterType == 1) {
		switch(cmd.scale){
			case 0:
				log.debug("energy report is ${cmd.scaledMeterValue} kWh")
				result << createEvent(name:"energy", value: cmd.scaledMeterValue, unit:"kWh", descriptionText:"${device.displayName} consumed ${cmd.scaledMeterValue} kWh")
				break;
			case 1:
				log.debug("energy report is ${cmd.scaledMeterValue} kVah")
				result << createEvent(name:"energy", value: cmd.scaledMeterValue, unit:"kVah", descriptionText:"${device.displayName} consumed ${cmd.scaledMeterValue} kVah")
				break;
			case 2:
				log.debug("power report is ${cmd.scaledMeterValue} W")
				result << createEvent(name:"power", value: cmd.scaledMeterValue, unit:"W", descriptionText:"${device.displayName} consumes ${cmd.scaledMeterValue} W")
				break;
			case 4:
				log.debug("voltage report is ${cmd.scaledMeterValue} V")
				result << createEvent(name:"voltage", value: cmd.scaledMeterValue, unit:"V", descriptionText:"${device.displayName} level ${cmd.scaledMeterValue} V")
				break;
			case 5:
				log.debug("amperage report is ${cmd.scaledMeterValue} A")
				result << createEvent(name:"amperage", value: cmd.scaledMeterValue, unit:"A", descriptionText:"${device.displayName} consumes ${cmd.scaledMeterValue} A")
				break;
			default:
				log.warn("Unsupported scale. Skipped cmd: ${cmd}")
		}
	}
	else {
		log.warn("Unsupported MeterType. Skipped cmd: ${cmd}")
    }    
	return result
}
