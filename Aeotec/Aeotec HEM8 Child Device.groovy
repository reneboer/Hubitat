/**
 *  Aeotec HEM-8 Child Device
 *
 *  Copyright 2020 Rene Boer
 *
 */
metadata {
	definition (
        name: "Aeotec Home Energy Meter 8 Child Device", 
        namespace: "reneboer", 
        author: "Rene Boer",
    ) 
    {
		capability "EnergyMeter"
		capability "PowerMeter"
		capability "VoltageMeasurement"
        capability "CurrentMeter"
		capability "Refresh"
		
		command "resetEnergy"
	}
}

def refresh() { parent.refresh(device.deviceNetworkId[-1].toInteger()) }
def resetEnergy() { parent.resetEnergy(device.deviceNetworkId[-1].toInteger()) }