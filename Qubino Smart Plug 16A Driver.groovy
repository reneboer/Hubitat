/**
 *  Qubino Smart Plug 16A ZMNHYDx
 *  Device Handler
 *  Version 0.11
 *  Date: 23.12.2022
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
 *  0.01: First release
 *  0.02: Added default for preferences, removed debug messages when debug is off.
 *  0.03: Added power & amperage refresh after switch off.
 *  0.04: Added html formatted attribute to display W, V, A and KWh in one tile
 *  0.05: Fixed refreshPowerConsumption. Only updating tile on power value change to reduce events.
 *  0.06: Added finger print and associations.
 *  0.10: Added secure mode support. Some rewrites.
 *  0.11: Added High/Low states
 */
metadata {
    definition(name: 'Qubino Smart Plug 16A', namespace: 'Goap', author: 'Rene Boer') {
        capability 'Actuator'
        capability 'Switch'
        capability 'PowerMeter'
        capability 'EnergyMeter'
        capability 'VoltageMeasurement'
        capability 'CurrentMeter'
        capability 'Sensor'
        capability 'Configuration' //Needed for configure() function to set any specific configurations
        attribute 'htmlTile', 'string'  // To display all readings in one tile.
        attribute  'currentH', 'number'
        attribute  'currentL', 'number'
        // attribute  'energyDuration', 'number'
        attribute  'powerH', 'number'
        attribute  'powerL', 'number'
        attribute  'voltageH', 'number'
        attribute  'voltageL', 'number'

        command 'setConfiguration' //command to issue Configuration Set commands to the module according to user preferences
        command 'setAssociation' //command to issue Association Set commands to the modules according to user preferences
        command 'refreshPowerConsumption' //command to issue Meter Get requests for KWH measurements from the device, W are already shown as part of Pwer MEter capability
        command 'resetPower' //command to issue Meter Reset commands to reset accumulated pwoer measurements

        fingerprint mfr:'0159', prod:'0002', model:'0054'
        fingerprint mfr:'0159', prod:'0002', deviceId:'0054', inClusters:'0x5E,0x25,0x85,0x59,0x55,0x86,0x72,0x5A,0x70,0x32,0x71,0x73,0x9F,0x6C,0x7A'
    }

    preferences {
        section {
            image(name: 'educationalcontent', multiple: true, images: [
                'https://raw.githubusercontent.com/reneboer/Hubitat/qubino-smart-plug.jpg'
                ])
        }
        section {
/**
*            --------    CONFIGURATION PARAMETER SECTION    --------
*/
            input(
                type: 'paragraph',
                element: 'paragraph',
                title: 'CONFIGURATION PARAMETERS:',
                description: 'Configuration parameter settings.'
            )
            input name: 'param11', type: 'number', range: '0..32536', required: false, defaultValue: 0,
                title: '11. Automatic turning off output after set time in seconds. 0 = Disabled.'

            input name: 'param12', type: 'number', range: '0..32536', required: false, defaultValue: 0,
                title: '12. Automatic turning on output after set time in seconds. 0 = Disabled.'

            input name: 'param30', type: 'enum', required: false, defaultValue: 0,
                options: ['0' : '0 - Switch saves its state before power failure (it returns to the last position saved before a power failure)',
                          '1' : "1 - Switch does not save the state after a power failure, it returns to 'off' position"],
                title: '30. Saving the state of the device after a power failure.'

            input name: 'param40', type: 'number', range: '0..100', required: false, defaultValue: 20,
                title: '40. Watt Power Consumption Reporting Threshold.'

            input name: 'param42', type: 'number', range: '0..32767', required: false, defaultValue: 0,
                title: '42. Power reporting in Watts by time interval.'

            input name: 'param50', type: 'number', range: '0..4000', required: false, defaultValue: 30,
                title: '50. Down value.'

            input name: 'param51', type: 'number', range: '0..4000', required: false, defaultValue: 50,
                title: '51. Up value.'

            input name: 'param52', type: 'enum', required: false, defaultValue: 6,
                options: ['0' : '0 – function inactive',
                          '1' : '1 - turn the associated devices on, once the power drops below Down value',
                          '2' : '2 - turn the associated devices off, once the power drops below Down value',
                          '3' : '3 - turn the associated devices on, once the power rises above Up value',
                          '4' : '4 - turn the associated devices off, once the power rises above Up value',
                          '5' : '5 - 1 and 4 combined',
                          '6' : '6 - 2 and 3 combined'],
                title: '52. Action in case of exceeding defined power values.'

            input name: 'param70', type: 'number', range: '0..4000', required: false, defaultValue: 0,
                title: '70. Overload safety switch. 0 = not active.'

            input name: 'param71', type: 'number', range: '0..4000', required: false, defaultValue: 0,
                title: '71. Power threshold. 0 = not active.'

            input name: 'param72', type: 'number', range: '0..125', required: false, defaultValue: 1,
                title: '72. Program completed notification Time interval.'

            input name: 'param73', type: 'enum', required: false, defaultValue: 0,
                options: ['0' : '0 - function disabled',
                          '1' : '1 - turn OFF relay once the notification Program completed is sent'],
                title: '73. Turn Smart Plug OFF.'

            input name: 'param74', type: 'enum', required: false, defaultValue: 1,
                options: ['0' : '0 - LED is disabled',
                          '1' : '1 - LED is enabled'],
                title: '74. Enable/disable LED.'
        }
        section {
/**
*            --------    ASSOCIATION GROUP SECTION    --------
*/
            input(
                type: 'paragraph',
                element: 'paragraph',
                title: 'ASSOCIATION GROUPS:',
                description: 'Association group settings. Up to 16 nodes per group.\n' +
                       'NOTE: Insert the node Id value of the devices you wish to associate this group with. Multiple nodeIds can also be set at once by separating individual values by a comma (2,3,...).'
            )
            input name: 'assocGroup2', type: 'text', required: false,
                title: 'Association group 2: \n' +
                       'Basic on/off.'

            input name: 'assocGroup3', type: 'text', required: false,
                title: 'Association group 3: \n' +
                       'Plug Threshold'
        }
        section {
            input(
                type: 'paragraph',
                element: 'paragraph',
                title: 'Debug settings:',
                description: 'Driver debug settings. '
            )
            input name: 'logEnable', type: 'bool', title: 'Enable debug logging', defaultValue: true
            input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', defaultValue: false
        }
    }
}
/*
* Supported command classes
*/
private static getCommandClassVersions() {
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

/**
*    --------    HELPER METHODS SECTION    --------
*/
/**
 * Converts a list of String type node id values to Integer type.
 *
 * @param stringList - a list of String type node id values.
 * @return stringList - a list of Integer type node id values.
*/
def convertStringListToIntegerList(stringList) {
    if (logEnable) log.debug stringList
    if (stringList != null) {
        for (int i = 0; i < stringList.size(); i++) {
            stringList[i] = stringList[i].toInteger()
        }
    }
    return stringList
}
/*
*    --------    HANDLE COMMANDS SECTION    --------
*/
/**
 * Configuration capability command handler.
 *
 * @param void
 * @return List of commands that will be executed in sequence with 500 ms delay inbetween.
*/
def configure() {
    logDebug 'configure()'
    sendToDevice([
        zwave.associationV1.associationRemove(groupingIdentifier:1),
        zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId),
        zwave.multiChannelV3.multiChannelEndPointGet()
    ], 1000)
}

/**
 * Switch capability command handler for ON state.
 * This command is followed by a Switch Binary Get command, that updates the actual state of the switch.
 *
 * @param void
 * @return void.
*/
def on() {
    logDebug 'on()'
    sendToDevice([
        zwave.basicV1.basicSet(value: 0xFF),
        zwave.switchBinaryV1.switchBinaryGet()
    ])
}
/**
 * Switch capability command handler for OFF state.
 * This command is followed by a Switch Binary Get command, that updates the actual state of the switch.
 *
 * @param void
 * @return void.
*/
def off() {
    logDebug 'off()'
    sendToDevice([
        zwave.basicV1.basicSet(value: 0x00),
        zwave.switchBinaryV1.switchBinaryGet()
    ])
}

/**
 * Refresh Power Consumption command handler for updating the cumulative consumption fields in kWh. It will issue a Meter Get command with scale parameter set to kWh.
 *
 * @param void.
 * @return void.
*/
def refreshPowerConsumption() {
    logDebug 'refreshPowerConsumption()'
    sendToDevice([
        zwave.meterV4.meterGet(scale: 0),
        zwave.meterV4.meterGet(scale: 4),
        zwave.meterV4.meterGet(scale: 5),
        zwave.meterV4.meterGet(scale: 2)
    ])
}
/**
 * Reset Power Consumption command handler for resetting the cumulative consumption fields in kWh. It will issue a Meter Reset command followed by Meter Get commands for active and accumulated power.
 *
 * @param void.
 * @return void.
*/
def resetPower() {
    logDebug 'resetPower()'
    sendToDevice([
        zwave.meterV2.meterReset(),
        zwave.meterV4.meterGet(scale: 0),
        zwave.meterV4.meterGet(scale: 4),
        zwave.meterV4.meterGet(scale: 5),
        zwave.meterV4.meterGet(scale: 2)
    ])
}

/**
 * setAssociations command handler that sets user selected association groups. In case no node id is insetred the group is instead cleared.
 * Lifeline association hidden from user influence by design.
 *
 * @param void
 * @return List of Association commands that will be executed in sequence with 500 ms delay inbetween.
*/

def setAssociation() {
    logDebug 'setAssociation()'
    def assocSet = []
    def associationGroups = 3
    for (int i = 2; i <= associationGroups; i++) {
        if (settings."assocGroup${i}" != null) {
            logDebug "associationSet(groupingIdentifier:${i})"
            def groupparsed = settings."assocGroup${i}".tokenize(',')
            if (groupparsed == null) {
                assocSet << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:settings."assocGroup${i}").format()
            } else {
                groupparsed = convertStringListToIntegerList(groupparsed)
                assocSet << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:groupparsed).format()
            }
        } else {
            logDebug "associationRemove(groupingIdentifier:${i})"
            assocSet << zwave.associationV2.associationRemove(groupingIdentifier:i).format()
        }
    }
    if (assocSet.size() > 0) {
        return sendToDevice(assocSet)
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
    logDebug 'setConfiguration()'
    def configSequence = []
    if (settings.param11 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 11, size: 2, scaledConfigurationValue: settings.param11.toInteger()).format()
    }
    if (settings.param12 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 12, size: 2, scaledConfigurationValue: settings.param12.toInteger()).format()
    }
    if (settings.param30 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 30, size: 1, scaledConfigurationValue: settings.param30.toInteger()).format()
    }
    if (settings.param40 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 40, size: 1, scaledConfigurationValue: settings.param40.toInteger()).format()
    }
    if (settings.param42 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 42, size: 2, scaledConfigurationValue: settings.param42.toInteger()).format()
    }
    if (settings.param50 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 50, size: 2, scaledConfigurationValue: settings.param50.toInteger()).format()
    }
    if (settings.param51 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 51, size: 2, scaledConfigurationValue: settings.param51.toInteger()).format()
    }
    if (settings.param52 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 52, size: 1, scaledConfigurationValue: settings.param52.toInteger()).format()
    }
    if (settings.param70 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 70, size: 2, scaledConfigurationValue: settings.param70.toInteger()).format()
    }
    if (settings.param71 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 71, size: 2, scaledConfigurationValue: settings.param71.toInteger()).format()
    }
    if (settings.param72 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 72, size: 1, scaledConfigurationValue: settings.param72.toInteger()).format()
    }
    if (settings.param73 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 73, size: 1, scaledConfigurationValue: settings.param73.toInteger()).format()
    }
    if (settings.param74 != null) {
        configSequence << zwave.configurationV1.configurationSet(parameterNumber: 74, size: 1, scaledConfigurationValue: settings.param74.toInteger()).format()
    }
    if (configSequence.size() > 0) {
        return sendToDevice(configSequence)
    }
}

/*
*    --------    EVENT PARSER SECTION    --------
*/
/**
 * parse function takes care of parsing received bytes and passing them on to event methods.
 *
 * @param description String type value of the received bytes.
 * @return Parsed result of the received bytes.
*/
def parse(String description) {
    logDebug "Parsing '${description}'"
    def result = null
    def cmd = zwave.parse(description, commandClassVersions)
    if (cmd) {
        result = zwaveEvent(cmd)
        logDebug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        logDebug "Non-parsed event: ${description}"
    }
    return result
}
/**
 * Catch Event Not Otherwise Handled!
*/
def zwaveEvent(hubitat.zwave.Command cmd, ep = null) {
    //    com.hubitat.app.DeviceWrapper targetDevice = getTargetDeviceByEndPoint(ep)
    log.trace "Received Z-Wave Message ${cmd} that is not handled by this driver. Endpoint: ${ep}. Message class: ${cmd.class}."
}
/**
 * Event handler for received Switch Multilevel Report frames.
 *
 * @param void
 * @return List of events to update the ON / OFF and analogue control elements with received values.
*/
def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
    logDebug "firing switch multilevel event ($cmd)"
    def result = []
    result << createEvent(name:'switch', value: cmd.value ? 'on' : 'off')
    result << createEvent(name:'level', value: cmd.value, unit:'%', descriptionText:"${device.displayName} dimmed to ${cmd.value==255 ? 100 : cmd.value}%")
    return result
}
/**
 * Event handler for received Meter Report frames. Used for displaying W and kWh measurements.
 *
 * @param void
 * @return Power consumption event for W data or kwhConsumption event for kWh data.
*/
def zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd) {
    logDebug "firing meter report event ($cmd)"
    updateReports(cmd)
}

/**
 * Event handler for received Switch Binary Report frames. Used for ON / OFF events.
 *
 * @param cmd Switch Binary Report received from device
 * @return Switch Event with on or off value.
*/
def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    logDebug "firing switch binary report event ($cmd)"
    createEvent(name:'switch', value: cmd.value ? 'on' : 'off')
}
/**
 * Event handler for received Configuration Report frames. Used for debugging purposes.
 *
 * @param cmd Configuration Report received from device.
 * @return void.
*/
def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
    logDebug "firing configuration report event ($cmd.configurationValue)"
}
/**
 * Event handler for received Basic Report frames.
 *
 * @param cmd Basic Report received from device.
 * @return void
*/
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logDebug "firing basic report event ($cmd)"
}
/**
 * Event handler for received Multi Channel Encapsulated commands.
 *
 * @param cmd encapsulated communication frame
 * @return parsed event.
*/
def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    logDebug 'firing MC Encapsulation event'
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
def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap command) {
    logDebug 'firing MC sensor multilevel event'
    return null
}
/**
 * Event handler for received MC Encapsulated Switch Multilevel Report frames.
 *
 * @param cmd communication frame, command mc encapsulated communication frame; needed to distinguish sources
 * @return List of events to update the ON / OFF and analogue control elements with received values.
*/
def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap command) {
    logDebug "firing MC switch multilevel event ($cmd), ($command)"
    def result = []
    result << createEvent(name:'switch', value: cmd.value ? 'on' : 'off')
    return result
}
/**
 * Event handler for received MC Encapsulated Meter Report frames.
 *
 * @param cmd communication frame, command mc encapsulated communication frame; needed to distinguish sources
 * @return List of events to update power control elements with received values.
*/
def zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd, hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap command) {
    logDebug("firing MC Meter Report event ($cmd), ($command)")
    updateReports(cmd)
}

private void updateTile(  ) {
    String val

    // Create special compound/html tile
    //    val = "<B>Power : </B> ${state.power} </BR><B>Amperage : </B> ${state.amperage}  </BR><B>Energy : </B> ${state.energy}  </BR><B>Voltage : </B> ${state.voltage}"
    val = '<B>Power : </B> ' + device.currentValue('power') + ' W</BR><B>Amperage : </B> ' + device.currentValue('amperage').toString() + ' A</BR><B>Energy : </B> ' + device.currentValue('energy').toString() + ' KWh</BR><B>Voltage : </B> ' + device.currentValue('voltage').toString() + ' V'
    if (device.currentValue( 'htmlTile' ).toString() != val) {
        sendEvent( name: 'htmlTile', value: val )
    }
}

/*
 * Process meters report
*/
def updateReports(cmd) {
    def result = []
    String name
    String unit
    String label
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
                runIn(1, 'updateTile')
                break
            case 3:
                name = 'frequency'
                unit = 'Hz'
                label = 'level'
                break
            case 4:
                name = 'voltage'
                unit = 'V'
                label = 'level'
                break
            case 5:
                name = 'amperage'
                unit = 'A'
                label = 'consumes'
                break
            default:
                log.warn("Unsupported scale. Skipped cmd: ${cmd}")
        }
    }
    else {
        log.warn("Unsupported MeterType. Skipped cmd: ${cmd}")
    }
    if (name) {
        logDebug("${name} report is ${val} ${unit}")
        if (device.currentValue("$name") != val) {
            result << createEvent(name:name, value: val, unit:unit, descriptionText:"${device.displayName} ${label} ${val} ${unit}")
            if (name != "energy"){
                result += createMeterHistoryEvents(name, label, val, unit, true)
                result += createMeterHistoryEvents(name, label, val, unit, false)
            }
        }
    }
    return result
}

private createMeterHistoryEvents(String mainName, String label, mainVal, String unit, Boolean lowEvent) {
    String name = "${mainName}${lowEvent ? 'L' : 'H'}"
    def val = device.currentValue("${name}")

    def result = []
    if ((val == null) || (lowEvent && (mainVal < val)) || (!lowEvent && (mainVal > val))) {
        result << createEvent(name:name, value: val, unit:unit, descriptionText:"${device.displayName} ${label} ${mainVal} ${unit}")
        logDebug("${name} report is ${val} ${unit ?: ''}")
    }
    return result
}

/*
 * Send zwave commands to device. Need to see why secure does not work.
*/
def sendToDevice(hubitat.zwave.Command cmd) {
    if (getDataValue('zwaveSecurePairingComplete') == 'true') {
        logDebug "sendToDevice secured ($cmd)"
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        logDebug "sendToDevice($cmd)"
        cmd.format()
    }
}

def sendToDevice(List<hubitat.zwave.Command> commands, delay=200) {
    logDebug "sendToDevice($commands)"
    delayBetween(commands.collect { sendToDevice(it) }, delay)
}

// Write to log if enabled
private logDebug(msg) {
    if (logEnable) log.debug "Qubino Smart Plug 16A: $msg"
}
