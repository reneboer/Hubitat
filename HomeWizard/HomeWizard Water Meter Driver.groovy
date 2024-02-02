/**
 *
 *  File: HomeWizeard Water Meter Driver.groovy
 *  Platform: Hubitat
 *
 *  https://raw.githubusercontent.com
 *
 *  Requirements:
 *     1) HomeWizard Water Meter with local API enabled.
 *        Set DHCP Reservation to prevent IP address from changing.
 *
 *  Copyright 2024 Rene Boer 
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
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2024-02-02  Rene Boer      Alpha release
 */

def version() {"v0.1.20240202"}

import hubitat.helper.InterfaceUtils
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
  definition (name: "Logitech Harmony Hub Parent", namespace: "ogiewon", author: "Dan Ogorchock", importUrl: "https://raw.githubusercontent.com/ogiewon/Hubitat/master/Drivers/logitech-harmony-hub-parent.src/logitech-harmony-hub-parent.groovy") {
    capability "Initialize"
    capability "Polling"
    capability "Sensor"
        
    attribute "currentFlowRate", "number"  // The actual water flow reported in l/m
    attribute "totalWaterUsage", "number"  // The total water usage reported since device installation in M3.
  }
}

preferences {
  input("ip", "text", title: "HomeWizard Water Meter", description: "IP Address (in form of 192.168.1.45)", required: true)
  input "pollInterval", "enum", title: "Poll Interval:", required: false, defaultValue: "5 Seconds", options: ["2 Seconds", "5 Seconds", "10 Seconds", "15 Seconds", "30 Seconds", "1 Minute", "2 Minutes", "5 Minutes"]
  input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true   
  input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
}

void logsOff(){
  log.warn "debug logging disabled..."
  device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void installed() {
  logger "info", "installed()"
  updated()
}

void updated() {
  logger "info", "updated()"
  state.version = version()
  unschedule()
  if (logEnable) runIn(1800,logsOff)
}

void initialize() {
  logger "info", "initialize()"
  state.version = version()
    
  if (!ip) {
    logger ("warn", "Water Meter IP Address not configured yet.")
  } else {
    unschedule(pollDevice)
    String url = "http://${ip}/api"
    try {
      httpGet(url) { resp -> 
        if (resp.success) {
          logger ("debug", resp.getData())
          def respValues = new JsonSlurper().parseText(resp.data.toString().trim())
          state.api_version = respValues.api_version
          state.product_type = respValues.product_type
          state.serial = respValues.serial
          state.firmware_version = respValues.firmware_version
          initialize_poll()
        } else {
          logger ("warn", "")
        }  
	    }
    } catch(Exception e) {
      logger ("error", "error occured calling httpget ${e}")
    }
  }  
}

void initialize_poll() {
  logger ("info", "${device.label} ${pollInterval} refresh called")

	if (pollInterval == "2 Seconds") {
	    schedule("2 * * * * ? *", pollDevice)
	} else if (pollInterval == "5 Seconds") {
      schedule("5 * * * * ? *", pollDevice)
	} else if (pollInterval == "10 Seconds") {
	    schedule("10 * * * * ? *", pollDevice)
	} else if (pollInterval == "15 Seconds") {
	    schedule("15 * * * * ? *", pollDevice)
	} else if (pollInterval == "30 Seconds") {
	    schedule("30 * * * * ? *", pollDevice)
	} else if (pollInterval == "1 Minute") {
	    schedule("0 1 * * * ? *", pollDevice)
	} else if (pollInterval == "2 Minutes") {
	    schedule("0 2 * * * ? *", pollDevice)
	} else if (pollInterval == "5 Minutes") {
	    schedule("0 5 * * * ? *", pollDevice)
	} else {
    logger ("warn", "unknown polling interval.")
  }  
}

void poll() {
  logger "info", "poll()"
  pollDevice()
}

void pollDevice() {
  logger "info", "pollDevice()"
  String url = "http://${ip}/api/${state.api_version}/data"
  try {
    httpGet(url) { resp -> 
      logger ("debug", resp.getData())
      def respValues = new JsonSlurper().parseText(resp.data.toString().trim())
      sendEventWrapper(name: "currentFlowRate", value: respValues.active_liter_lpm, unit:"l/m")
      sendEventWrapper(name: "totalWaterUsage", value: respValues.total_liter_m3, unit:"m3")
	  }
  } catch(Exception e) {
    logger ("error", "error occured calling httpget ${e}")
  }
}

void parse(String description) {
  logger "info", "void(${description})"
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
