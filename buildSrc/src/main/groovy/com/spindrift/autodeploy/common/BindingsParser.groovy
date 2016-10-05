package com.spindrift.autodeploy.common

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import groovy.util.slurpersupport.GPathResult

class BindingsParser extends AutoDeployBase
{
	private static final String ALL = "ALL";
	
	Map getBindings(File bindingsXML, String configName, String appName)
	{
		Map allSlotSubs, allEnvSubs
		
		def parsedXML = new XmlSlurper().parse(bindingsXML)
		def slotSubstitionsForAllXML = parsedXML.keysubstitutions.slotsubstitutions.find{ it.@application.text().equals(ALL) }
		allSlotSubs = getSlotSubstitutions(slotSubstitionsForAllXML, configName)
		
		def slotSubstitionsForAppXML = parsedXML.keysubstitutions.slotsubstitutions.find{ it.@application.text().equals(appName) }
		Map appSlotSubs = getSlotSubstitutions(slotSubstitionsForAppXML, configName)
		
		//Replace the value in allSlotSubs with appSlotSubs
		
		appSlotSubs.each{ allSlotSubs.put(it.key, it.value) }
		
		
		
		def envSubstitionsForAllXML = parsedXML.keysubstitutions.envsubstitutions.find{ it.@application.text().equals(ALL) }
		allEnvSubs = getEnvSubstitutions(envSubstitionsForAllXML, configName)
		def envSubstitionsForAppXML = parsedXML.keysubstitutions.envsubstitutions.find{ it.@application.text().equals(appName) }
		Map appEnvSubs = getEnvSubstitutions(envSubstitionsForAppXML, configName)
		
		appEnvSubs.each{ allEnvSubs.put(it.key, it.value) }
		
		allSlotSubs.putAll(allEnvSubs)
		
		return allSlotSubs
	}

	/**
	Return a Map of bindings defined in the specified xml file for the specified config element.
	*/
	private Map getSlotSubstitutions(GPathResult parsedXML, String configName) 
	{
		def config = parsedXML.slot.find{ it.@name.text().equals(configName) } 
		
		def bindingsMap=[:]
		
		config.key.each
		{
			bindingsMap.put(it.@name.text(),it.text())	
		}
	
		return(bindingsMap)					
	}
	
	private Map getEnvSubstitutions(GPathResult parsedXML, String configName) 
	{
		def bindingsMap=[:]
		
		parsedXML.key.each
		{
			def value = it.env.find{ it.@name.text().equals(configName) }
			bindingsMap.put(it.@name.text(),value.text())
		}

		return(bindingsMap)					
	}	
}
