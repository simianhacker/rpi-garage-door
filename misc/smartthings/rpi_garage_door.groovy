/* RPI Garage Door
 *
 *  Copyright 2015 Chris Cowan
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
 */

import groovy.json.JsonSlurper

metadata {
  definition (name: "RPI Garage Door", namespace: "simianhacker", author: "Chris Cowan") {
    capability "Contact Sensor"
    capability "Momentary"
    command "push"
    command "refresh"
  }

  simulator {
    // TODO: define status and reply messages here
    status "open": "{ \"status\": \"open\" }"
    status "closed": "{ \"status\": \"closed\" }"
  }

  tiles {
    // TODO: define your main and details tiles here
    standardTile("contact", "device.contact", width: 2, height: 2) {
      state("open", label:'${name}', icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e", next: "closed")
      state("closed", label:'${name}', icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", next: "open")
    }
    standardTile("open_close", "device.contact", inactiveLabel: false, decoration: "flat", canChangeIcon: true) {
      state "open", action:"push", icon: "st.doors.garage.garage-closing", label: "Close", displayName: "Close"
      state "closed", action:"push", icon: "st.doors.garage.garage-opening", label: "Open", displayName: "Open"
    }
    standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
      state "default", action:"refresh", icon: "st.secondary.refresh-icon" , label: "Refresh", displayName: "Refresh"
    }
    main "contact"
    details(["contact", "open_close", "refresh"])
  }
}

def push() {
  log.debug "Sending post to ${getHostAddress()}"
  request("/", "POST")
}

def refresh() {
  log.debug "Sending post to ${getHostAddress()}"
  request("/", "GET")
}

def request(path, method, body = "") {
  log.debug "Sending ${method} to ${getHostAddress()}${path}"
  try {
    def hubAction = new physicalgraph.device.HubAction(
      method: method,
      path: path,
      body: body,
      headers: [ HOST: getHostAddress() ]
    )
    hubAction
  } catch (e) {
    log.debug "Hit Exception $e on $hubAction"
  }

}

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
  if (description == 'updated') {
    return
  }
  def descMap = parseDescriptionAsMap(description)
  if (descMap.containsKey("body")) {
    def body = parseBase64Json(descMap["body"])
    if (body.containsKey("status")) {
      sendEvent(name: "contact", value: body.status)
    }
  }
}

def parse(LinkedHashMap map) {
  sendEvent(name: map.name, value: map.value)
}

def parseBase64Json(String input) {
  def sluper = new JsonSlurper();
  sluper.parseText(new String(input.decodeBase64()))
}

def parseDescriptionAsMap(String description) {
  description.split(',').inject([:]) { map, param ->
    def nameAndValue = param.split(":")
    map += [(nameAndValue[0].trim()) : (nameAndValue[1].trim())]
  }
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() )
  log.debug hexport
  return hexport
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	log.debug("Convert hex to ip: $hex")
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
  def parts = device.deviceNetworkId.split(":")
  log.debug device.deviceNetworkId
  def ip = convertHexToIP(parts[0])
  def port = convertHexToInt(parts[1])
  return ip + ":" + port
}



