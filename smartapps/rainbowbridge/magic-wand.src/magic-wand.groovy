/**
 *	Magic Wand
 *
 *	Author: Shiblee Sadik
 *	Date: 2018-7-1
 */
definition(
    name: "Magic Wand",
    namespace: "rainbowbridge",
    author: "Shiblee Sadik",
    description: "Control anything with morse code. A single button used to generate the morse code. Code can be assigned to control a device or trigger a routine",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png",
    pausable: true
)

preferences {
	page(name: "selectButton", content:"selectButton")
    page(name: "configureSwitches", content:"configureSwitches")
}

def selectButton() {
	return dynamicPage(name: "selectButton", title: "Configure Button and Devices", nextPage: "configureSwitches") {
    	def actions = location.helloHome?.getPhrases()*.label
		section ("Configure Device") {
			input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true
            input "switches", "capability.switch", title: "Switches", multiple: true, required: false
            input "locks", "capability.lock", title: "Locks", multiple: true, required: false
            input "alarms", "capability.alarm", title: "Alarms", multiple: true, required: false
            if (actions) {
            	input "routines", "enum", title: "Routines", options: actions, multiple: true, required: false
            }
		}
        section ("Configure Symbols") {
			input "pushed", "text", title: "Pushed", defaultValue: ".", required: true
            input "held", "text", title: "Held", defaultValue: "-", required: true
		}
        section ("Configure time") {
        	input "delay", "number", title: "Delay in sec", defaultValue: 3, required: true
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
		}
	}
}

/* Called after the button is configured */
def configureSwitches() {
    dynamicPage(name: "configureSwitches", title: "Setup code for switches", install:true, uninstall:true) {
    	if (switches != null) {
            section ("Configure Switches") {
                for (def i=0; i<switches.size(); i++) {
                    input "switchMorseCode_${i}", "text", title: switches[i].displayName, required: true
                }
            }
        }
        if (locks != null) {
            section ("Configure Locks") {
                for (def i=0; i<locks.size(); i++) {
                    input "lockMorseCode_${i}", "text", title: locks[i].displayName, required: true
                }
            }
        }
        if (alarms != null) {
            section ("Configure Alarms") {
                for (def i=0; i<alarms.size(); i++) {
                    input "alarmMorseCode_${i}", "text", title: alarms[i].displayName, required: true
                }
            }
        }
        if (routines != null) {
        	section ("Configure Routines") {
                for (def i=0; i<routines.size(); i++) {
                    input "routineMorseCode_${i}", "text", title: routines[i], required: true
                }
            }
        }
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	state.code = ""
	subscribe(buttonDevice, "button", buttonEvent)
    // subscribe(location, "routineExecuted", routineChanged)
}

def routineChanged(evt) {
    log.debug "routineChanged: $evt"
    log.debug "evt name: ${evt.name}"
    log.debug "evt value: ${evt.value}"
    log.debug "evt displayName: ${evt.displayName}"
    log.debug "evt descriptionText: ${evt.descriptionText}"
}

def configured() {
	return buttonDevice || buttonConfigured()
}

def buttonEvent(evt){
    def value = evt.value
    state.code = state.code + settings["$value"]
    runIn(delay, "executebutton")
    log.debug "buttonEvent: $evt.name = $evt.value ($evt.data) : " + state.code
}

def executebutton() {
	def s = find(state.code)
    def r = findRoutine(state.code)
    if (s != null) {
    	toggle(s)
    } else if (r != null) {
    	executeRoutine(r)
    } else {
    	sendPush("No action found for ${buttonDevice.displayName} with ${state.code}")
    }
    state.code = ""
}

def find(code) {
	if (switches != null) {
        for (def i=0; i<switches.size(); i++) {
            if (settings["switchMorseCode_${i}"] == code) {
                return switches[i]
            }
        }
    }
    if (locks != null) {
        for (def i=0; i<locks.size(); i++) {
            if (settings["lockMorseCode_${i}"] == code) {
                return locks[i]
            }
        }
    }
    if (alarms != null) {
        for (def i=0; i<alarms.size(); i++) {
            if (settings["alarmMorseCode_${i}"] == code) {
                return alarms[i]
            }
        }
    }
    return null
}

def findRoutine(code) {
	if (routines != null) {
        for (def i=0; i<routines.size(); i++) {
            if (settings["routineMorseCode_${i}"] == code) {
                return routines[i]
            }
        }
    }
    return null
}

def executeRoutine(routine) {
	location.helloHome?.execute(routine)
}

def toggle(device) {
	log.debug "Executing on device $device.displayName"
	if (device.currentValue('switch').contains('on')) {
		device.off()
	}
	else if (device.currentValue('switch').contains('off')) {
		device.on()
	}
	else if (device.currentValue('lock').contains('locked')) {
		device.unlock()
	}
	else if (device.currentValue('lock').contains('unlocked')) {
		device.lock()
	}
	else if (device.currentValue('alarm').contains('off')) {
        device.siren()
    }
	else {
		device.on()
	}
}