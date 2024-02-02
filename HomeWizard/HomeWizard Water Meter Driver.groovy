/**
 *
 *  File: HomeWizeard Water Meter Driver.groovy
 *  Platform: Hubitat
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

import hubitat.scheduling.AsyncResponse

metadata {
  definition (name: "HomeWizard Water Meter Driver", namespace: "reneboer", author: "Rene Boer", importUrl: "https://raw.githubusercontent.com/reneboer/Hubitat/main/HomeWizard/HomeWizard%20Water%20Meter%20Driver.groovy") {
    capability "Initialize"
    capability "Polling"
    capability "Sensor"
        
    attribute "currentFlowRate", "number"  // The actual water flow reported in l/m
    attribute "totalWaterUsage", "number"  // The total water usage reported since device installation in M3.
  }
}

preferences {
  input("ip", "text", title: "HomeWizard Water Meter", description: "IP Address (in form of 192.168.1.45)", required: true)
  input "pollInterval", "enum", title: "Poll Interval:", required: false, defaultValue: "10 Seconds", options: ["10 Seconds", "15 Seconds", "20 Seconds", "30 Seconds", "1 Minute", "2 Minutes", "5 Minutes"]
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
  if (ip) {
    initialize_poll()
  }
  if (logEnable) runIn(1800,logsOff)
}

void initialize() {
  logger "info", "initialize()"
  state.version = version()
    
  if (!ip) {
    logger ("warn", "Water Meter IP Address not configured yet.")
  } else {
    unschedule(pollDevice)
    try {
      httpGet("http://${ip}/api") { resp -> 
        if (resp.success) {
          def respValues = resp.getData()
          logger ("debug", "Initialize response: ${respValues}")
          state.api_version = respValues.api_version
          state.product_type = respValues.product_type
          state.serial = respValues.serial
          state.firmware_version = respValues.firmware_version
          initialize_poll()
        } else {
          logger ("warn", "Failed to get device details.")
        }  
	    }
    } catch(Exception e) {
      logger ("error", "error occured calling httpget ${e}")
    }
  }  
}

void initialize_poll() {
  logger ("info", "${pollInterval} refresh set.")

	if (pollInterval == "10 Seconds") {
	    schedule("0/10 * * * * ? *", pollDevice)
	} else if (pollInterval == "15 Seconds") {
	    schedule("0/15 * * * * ? *", pollDevice)
	} else if (pollInterval == "20 Seconds") {
	    schedule("0/20 * * * * ? *", pollDevice)
	} else if (pollInterval == "30 Seconds") {
	    schedule("0/30 * * * * ? *", pollDevice)
	} else if (pollInterval == "1 Minute") {
	    schedule("0 0/1 * * * ? *", pollDevice)
	} else if (pollInterval == "2 Minutes") {
	    schedule("0 0/2 * * * ? *", pollDevice)
	} else if (pollInterval == "5 Minutes") {
	    schedule("0 0/5 * * * ? *", pollDevice)
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
    Map params = [uri: "http://${ip}/api/${state.api_version}/data", timeout: 15]
    asynchttpGet(parse, params)
  } catch(Exception e) {
    logger ("error", "error occured calling httpget ${e}")
  }
}

void parse(AsyncResponse resp, Map data) {
  if (resp?.status == 200) {
    Map respValues = resp.getJson()
    logger ("debug", "Poll response: ${respValues}")
    sendEventWrapper(name: "currentFlowRate", value: respValues.active_liter_lpm, unit:"l/m")
    sendEventWrapper(name: "totalWaterUsage", value: respValues.total_liter_m3, unit:"m3")
  } else {
    logger ("warn", "Failed to parse reponse. Result ${resp.status}") 
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
