/*
    Basic Z-Wave Association tool

    2022-12-16
        - First version

    usage:
        -replace existing driver with this driver
        -set your paremeters
        -replace this driver with previous driver

    WARNING!
        --Setting device associations is an advanced feature, randomly poking values to a device
          can lead to unexpected results which may result in needing to perform a factory reset
          or possibly bricking the device or even causing a message storm when device associations point back to each other.
        --Refer to the device documentation for the correct group identifiers and values for your specific device
*/

import groovy.transform.Field

metadata {
    definition(name: 'Basic Z-Wave Association tool', namespace: 'hubitat', author: 'Rene') {
        command 'getAssociationReport'
        command 'addAssociation', [[name:'groupingIdentifier', type:'NUMBER', description:'Group Number (2..5)', constraints:['NUMBER']], [name:'nodeId', type:'STRING', description:'Device List (Decimal, comma seperated)']]
        command 'removeAssociation', [[name:'groupingIdentifier', type:'NUMBER', description:'Group Number (2..5)', constraints:['NUMBER']], [name:'nodeId', type:'STRING', description:'Device List (Decimal, comma seperated)']]
        command 'clearAssociations', [[name:'groupingIdentifier', type:'NUMBER', description:'Group Number (2..5)', constraints:['NUMBER']]]
    }
}

@Field Map zwLibType = [
    0:'N/A', 1:'Static Controller', 2:'Controller', 3:'Enhanced Slave', 4:'Slave', 5:'Installer',
    6:'Routing Slave', 7:'Bridge Controller', 8:'Device Under Test (DUT)', 9:'N/A', 10:'AV Remote', 11:'AV Device'
]

// Build-In callback Methods code
void installed() {
    if (getDataValue('zwWakeupInterval') != null) {
        state.cmdQueue = []
        log.info('Device needs to be woken up')
    } else {
        log.info('Device is always awake')
    }
}

void configure() { }

void updated() { }

def parse(String description) {
    hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
    if (cmd) {
        zwaveEvent(cmd)
    } else {
        log.warn "Non-parsed event ${description}"
    }
}

//Z-Wave responses code
/*
 * Set the reported associations as device data value
*/
void zwaveEvent(hubitat.zwave.commands.associationv1.AssociationReport cmd) {
    log.info "AssociationReport- groupingIdentifier:${cmd.groupingIdentifier}, maxNodesSupported:${cmd.maxNodesSupported}, nodes:${cmd.nodeId}"
    String noteName = "associationGroup${cmd.groupingIdentifier}"
    if (cmd.nodeId == null || cmd.nodeId == '' || cmd.nodeId == []) {
        removeDataValue(noteName)
    } else {
        updateDataValue(noteName, "${cmd.nodeId}")
    }
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    hubitat.zwave.Command encapCmd = cmd.encapsulatedCommand()
    if (encapCmd) {
        return zwaveEvent(encapCmd)
    } else {
        log.warn "Unable to extract encapsulated cmd from ${cmd}"
    }
}

// Device woke up, see if we have commands to send
def zwaveEvent(hubitat.zwave.commands.wakeupv1.WakeUpNotification cmd) {
    log.trace "cmd: $cmd"
    log.info "Device ${device.displayName} woke up. There are ${state.cmdQueue.size()} commands to be send."

    List<String> request = state.cmdQueue
    state.cmdQueue = []

    if (request != []) {
        request << 'delay 5000'
    } else {
        log.debug 'No commands pending to send'
    }
    request << secureCmd(zwave.wakeUpV1.wakeUpNoMoreInformation())
    response(request)
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "skip: ${cmd}"
}

//cmds
List<String> getAssociationReport() {
    List<String> commandsQue = []
    2.upto(5, {
        commandsQue.add(secureCmd(zwave.associationV1.associationGet(groupingIdentifier: it)))
    })
    log.trace 'getAssociationReport command(s) sent...'
    return sendQueueCommands(commandsQue)
}

/**
 * addAssociation command handler that adds node(s) to user selected association group.
 * Lifeline association hidden from user influence by design.
*/
List<String> addAssociation(BigDecimal groupingIdentifier = null, String nodeId = null) {
    if (groupingIdentifier == null || nodeId == null) {
        log.warn 'incomplete parameter list supplied...'
        log.info 'syntax: addAssociation(groupingIdentifier,nodeId)'
    } else {
        if (checkGroupingIdentifier(groupingIdentifier) == true) {
            List<String> valueparsed = nodeId.tokenize(',')
            List<String> commandsQue = []
            log.info "Setting Associations for group $groupingIdentifier, device(s) $nodeId"
            if (valueparsed == null) {
                commandsQue << secureCmd(zwave.associationV2.associationSet(groupingIdentifier:groupingIdentifier, nodeId:nodeId))
            } else {
                valueparsed = convertStringListToIntegerList(valueparsed)
                commandsQue << secureCmd(zwave.associationV2.associationSet(groupingIdentifier:groupingIdentifier, nodeId:valueparsed))
            }
            commandsQue << secureCmd(zwave.associationV1.associationGet(groupingIdentifier:groupingIdentifier))
            return sendQueueCommands(commandsQue)
        } else {
            log.warn 'incorrect parameter list supplied...'
            log.info 'groupingIdentifier must be a number between 2 and 5'
        }
    }
}

/**
 * removeAssociation command handler that removes node(s) from user selected association group.
 * Lifeline association hidden from user influence by design.
*/
List<String> removeAssociation(BigDecimal groupingIdentifier = null, String nodeId = null) {
    if (groupingIdentifier == null || nodeId == null) {
        log.warn 'incomplete parameter list supplied...'
        log.info 'syntax: removeAssociation(groupingIdentifier,nodeId)'
    } else {
        if (checkGroupingIdentifier(groupingIdentifier) == true) {
            List<String> valueparsed = nodeId.tokenize(',')
            List<String> commandsQue = []
            log.info "Setting Associations for group $groupingIdentifier, device(s) $nodeId"
            if (valueparsed == null) {
                commandsQue << secureCmd(zwave.associationV2.associationRemove(groupingIdentifier:groupingIdentifier, nodeId:nodeId))
            } else {
                valueparsed = convertStringListToIntegerList(valueparsed)
                commandsQue << secureCmd(zwave.associationV2.associationRemove(groupingIdentifier:groupingIdentifier, nodeId:valueparsed))
            }
            commandsQue << secureCmd(zwave.associationV1.associationGet(groupingIdentifier:groupingIdentifier))
            return sendQueueCommands(commandsQue)
        } else {
            log.warn 'incorrect parameter list supplied...'
            log.info 'groupingIdentifier must be a number between 2 and 5'
        }
    }
}

/**
 * clearAssociation command handler that removes all nodes from user selected association group.
 * Lifeline association hidden from user influence by design.
*/
List<String> clearAssociations(BigDecimal groupingIdentifier = null) {
    if (groupingIdentifier == null) {
        log.warn 'incomplete parameter list supplied...'
        log.info 'syntax: clearAssociations(groupingIdentifier)'
    } else {
        if (checkGroupingIdentifier(groupingIdentifier) == true) {
            List<String> commandsQue = []
            log.info "Clearing Association for group $groupingIdentifier"
            commandsQue << secureCmd(zwave.associationV2.associationRemove(groupingIdentifier:groupingIdentifier))
            commandsQue << secureCmd(zwave.associationV1.associationGet(groupingIdentifier:groupingIdentifier))
            return sendQueueCommands(commandsQue)
        } else {
            log.warn 'incorrect parameter list supplied...'
            log.info 'groupingIdentifier must be a number between 2 and 5'
        }
    }
}

/*
 * Helper functions
 */
List<Integer> convertStringListToIntegerList(List<String> stringList) {
    List<Integer> intList = []
    if (stringList != null) {
        for (int i = 0; i < stringList.size(); i++) {
            intList[i] = stringList[i].toInteger()
        }
    }
    return intList
}

// Check for valid groupingIdentifier value
Boolean checkGroupingIdentifier(BigDecimal gId) {
    return (gId > 1 && gId <= 5)
}
Boolean checkGroupingIdentifier(String gIdString) {
    return gIdString?.isBigDecimal() ? checkGroupingIdentifier(gIdString as BigDecimal) : false
}

List<String> sendQueueCommands(List<String> cmds) {
    if (getDataValue("zwWakeupInterval") != null) {
        state.cmdQueue = cmds
        return []
    } else {
        return delayBetween(commandsQue, 500)
    }
}

/*
 * Send z-wave commands with secure support.
 */
String secure(String cmd) {
    return zwaveSecureEncap(cmd)
}

String secure(hubitat.zwave.Command cmd) {
    return zwaveSecureEncap(cmd)
}

String secureCmd(cmd) {
    log.info "Sending command : $cmd)"
    if (getDataValue('zwaveSecurePairingComplete') == 'true' && getDataValue('S2') == null) {
        return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
  } else {
        return secure(cmd)
    }
}

private static Map getCommandClassVersions() {
    [
        0x84: 1, // Wakeup Notification
        0x85: 2  // Association
    ]
}

