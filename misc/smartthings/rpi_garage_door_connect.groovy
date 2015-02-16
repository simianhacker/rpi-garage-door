/**
 *  RPI Garage Door (connect)
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
definition(
    name: "RPI Garage Door (connect)",
    namespace: "simianhacker",
    author: "Chris Cowan",
    description: "Connect a Raspberry Pi",
    category: "My Apps",
    iconUrl: "https://dl.dropboxusercontent.com/u/41596401/RPI.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/41596401/RPI.png",
    iconX3Url: "https://dl.dropboxusercontent.com/u/41596401/RPI.png")


preferences {
  section("Raspberry Pi") {
    input("ip", "string", title:"IP Address", description: "192.168.1.68", required: true, displayDuringSetup: true)
    input("port", "string", title:"Port", description: "3000", defaultValue: 3000 , required: true, displayDuringSetup: true)
    input "hub", "hub", title: "Select Hub", displayDuringSetup: true, required: true
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def initialize() {
  addRPI()
}

def addRPI() {
  def dni = convertIpPortToDeviceNetworkId(ip, port)
  def d = getChildDevice(dni);
  if (!d) {
    d = addChildDevice("simianhacker", "RPI Garage Door", dni, hub.id, [:])
    d.setDeviceNetworkId(dni);
    log.debug("${d.displayName} created with id ${d.id}")
  } else {
    log.debug("device ${dni} already created")
  }
  subscribeToDoorEvents()
}

def subscribeToDoorEvents() {
  log.debug "Subscribe to the door events here"
  subscribe(location, null, lanResponseHandler, [filterEvents: false])
  try {
    subscribeAction("/subscribe")
  } catch(e) {
    log.debug "Hit Exception $e subscribe"
  }
}

mappings {
  path('/contactEvent') {
    action: [
      POST: "updateContactEvent"
    ]
  }
}

def lanResponseHandler(evt) {
	log.debug "I got back ${evt.name}"
  def descMap = parseDescriptionAsMap(evt.name)
  if (descMap.containsKey("body")) {
    def body = parseBase64Json(descMap["body"])
    if (body.containsKey("status")) {
      sendEvent(name: "contact", value: body.status)
    }
  }
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

def updateContactEvent() {
  def dni = convertIpPortToDeviceNetworkId(ip, port)
  def status = request.JSON?.status
  def device = getChildDevice(dni);
  if (!device) {
    httpError(404, "Device not found.")
  } else {
    sendEvent(device, name: "contact", value: status)
    '{ "status": "Ok"}'
  }
}

private subscribeAction(path, callbackPath="") {
    def dni = convertIpPortToDeviceNetworkId(ip, port)
    def device = getChildDevice(dni);
    def address = device.hub.localIP + ":" + device.hub.localSrvPortTCP
    def parts = device.deviceNetworkId.split(":")
    def ip = convertHexToIP(parts[0])
    def port = convertHexToInt(parts[1])
    def host = ip + ":" + port

    def result = new physicalgraph.device.HubAction("""POST /subscribe HTTP/1.1\r\nHOST:$host\r\nx-callback:http://$address/contactEvent\r\n\r\n""", physicalgraph.device.Protocol.LAN)
    sendHubCommand(result);
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

private convertIpPortToDeviceNetworkId(ip, port) {
  def hostHex = convertIPtoHex(ip)
  def portHex = convertPortToHex(port)
  return "${hostHex}:${portHex}"
}
