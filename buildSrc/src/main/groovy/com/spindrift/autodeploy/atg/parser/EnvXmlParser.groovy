package com.spindrift.autodeploy.atg.parser

import org.gradle.api.*
import org.gradle.api.plugins.*
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.atg.AtgBase

class EnvXmlParser extends AtgBase	{

    // The environment XML File Object.  File to be read is in the gradle.properties file
    def xmlConfig
    
    public EnvXmlParser(String environmentXml)
	{
		xmlConfig = new XmlSlurper().parse(new File(environmentXml))
	}
	
	/* 
	A helper method to find a ServerGroup GPathResult in the environment.xml
	*/		
	GPathResult findServerGroup(String environment, String serverGroup) {
		// find the first instance of the xml node matching the Environment passed into this method  and assign it to the Env variable 
		def envGPath = xmlConfig.environment.find{ it.@name.text().equals(environment) } 
		
		// Check that a valid environment was found in the xml
		assert envGPath.size() != 0, 'Error: Environment supplied is not valid:'+environment

		//within the Env variable find the first instance of the Servergroup 	
		def servGroupGPath = envGPath.servergroup.find{ it.@application.text().equals(serverGroup) }
		
		// Check that a valid Servergroup was found in the xml	
		assert servGroupGPath.size() != 0, 'Error: servergroup supplied is not valid:'+serverGroup
			
		return(servGroupGPath)
	}
	
	
	/* 
	A helper method to find a Server GPathResult in the environment.xml
	*/	
	GPathResult findServer(String environment, String serverGroup, String serverName) {
		def servGroupGPath = findServerGroup(environment, serverGroup)

		//within the Server Group find the first instance of the specified serverGroup. 	
		def serverGPath = servGroupGPath.server.find{ it.@name.text().equals(serverName) }
		
		// Check that a valid server was found in the xml	
		assert serverGPath.size() != 0, 'Error: Server Name supplied is not valid:'+serverName
		return(serverGPath)
	}
	
	/* 
	The getServers method  expects a Environment and a Servergroup. Give this  it will readthe environment
	xml file, defined in grade.properties and return the list of servers found within these Nodes as a array.
    Eg.     getServers(	NFT, LiveStoreFront)
	*/
	String[] getServers(String environment, String serverGroup) {
		
		// defin ethe serverList array that will be returned from this method
		def serverList = []
		
		def servGroupGPath = findServerGroup(environment, serverGroup)
								
		servGroupGPath.server.each {			
			def folderPath = environment + '/' + serverGroup + '/' + it.@name.text()
			serverList.add(folderPath)			
		}	

		return(serverList)					
	}		
	
	/* 
	Get a list of server names for the given environment and serverGroup
	
	param: environment - An environment specified in environment.xml.
	param: serverGroup - A serverGroup specified in environment.xml.	
	*/
	String[] getServerNames(String environment, String serverGroup) {
		
		// defin ethe serverList array that will be returned from this method
		def serverNames = []
		
		def servGroupGPath = findServerGroup(environment, serverGroup)
								
		servGroupGPath.server.each{
			serverNames.add(it.@name.text())	
		}	
		
		return(serverNames)
	}		
	
	/*
	 * Get a list of server group names (applications) for the given environment.
	 *
	 * @param: environment - An environment specified in environment.xml.
	*/
	String[] getServerGroupNames(String environment)
	{
		def serverGroupNames = [];
		def envGPath = xmlConfig.environment.find{ it.@name.text().equals(environment) };
		
		assert envGPath.size() != 0, 'Error: Environment supplied is not valid:'+environment;

		envGPath.servergroup.each{
			serverGroupNames.add(it.@application.text());
		}
		
		return(serverGroupNames);
	}
	
	/**
	This method returns an array of strings that holds slotnames for the given server.
	
	param: environment - An environment specified in environment.xml.
	param: serverGroup - A serverGroup specified in environment.xml.
	param: serverName - A server specified in environment.xml.
	*/
	String[] getSlotsForServer(String environment, String serverGroup, String serverName) 
	{
		
		// define the serverList array that will be returned from this method
		def slotList = []

		def serverGPath = findServer(environment, serverGroup, serverName)
		
		serverGPath.slot.each
		{
			slotList.add(it.@name.text())
		}			
		
		return(slotList)					
	}		
	
	/**
	For the given slot return the text from the required jvm_opts element in the environments xml.
	*/	
	String getJavaOptsForSlot(String environment, String serverGroup, String serverName, String slotName) {
		
		def serverGPath = findServer(environment, serverGroup, serverName)
	
		def slotGPath = serverGPath.slot.find{ it.@name.text().equals(slotName) }
		
		// Check that a valid slot was found in the xml	
		assert slotGPath.size() != 0, 'Error: Slot Name supplied is not valid:'+slotName
	
		return(slotGPath.jvm_opts.text())
	}

	/**
	For the given slot, derive the partition name using clustername as well domain name..
	*/	
	String getPartitionName(String environment, String serverGroup, String serverName, String slotName) 
	{
		def servGroupGPath = findServerGroup(environment, serverGroup)
		def serverGPath = findServer(environment, serverGroup, serverName)
		def slotGPath = serverGPath.slot.find{ it.@name.text().equals(slotName) }
		return "${servGroupGPath.@jbossCluster.text()}_${slotGPath.@jbossDomain.text()}"
	}
	
	/**
	For the given slot, return the clustername.
	*/	
	String getClusterName(String environment, String serverGroup)
	{
			def servGroupGPath = findServerGroup(environment, serverGroup)
			return servGroupGPath.@jbossCluster.text()
	}	

	/**
	 * For the given slot, return the cluster musticast address
	 */
	String getJbossClusterMCastAddress(String environment, String serverGroup, String serverName, String slotName)
	{
		def serverGPath = findServer(environment, serverGroup, serverName)
		def slotGPath = serverGPath.slot.find{ it.@name.text().equals(slotName) }
		return slotGPath.@jbossClusterMCastAddr.text()
	}
	
	
	/**
	Return the jboss domain name for the slot specified by the parameters
	*/
	String getJbossDomainNameForSlot(String environment, String serverGroup, String serverName, String slotName)	
	{
		def serverGPath = findServer(environment, serverGroup, serverName)
	
		def slotGPath = serverGPath.slot.find{ it.@name.text().equals(slotName) }

		

		// Check that a valid slot was found in the xml	
		assert slotGPath.size() != 0, 'Error: Slot Name supplied is not valid:'+slotName
	
		return slotGPath.@jbossDomain.text()

	}	

	/**
	Return the logFileRetentionDays name for the slot specified by the parameters
	*/
	String getlogFileRetentionDaysForSlot(String environment, String serverGroup, String serverName, String slotName)	
	{
		def serverGPath = findServer(environment, serverGroup, serverName)
	
		def slotGPath = serverGPath.slot.find{ it.@name.text().contains(slotName) }
		
		// Check that a valid slot was found in the xml	
		assert slotGPath.size() != 0, 'Error: Slot Name supplied is not valid:'+slotName
	
		return slotGPath.@logFileRetentionDays.text()
	}		
	
	
	/**
	Return the type attribute for the slot specified by the parameters
	*/
	String getSlotType(String environment, String serverGroup, String serverName, String slotName)	
	{
		def serverGPath = findServer(environment, serverGroup, serverName)
	
		def slotGPath = serverGPath.slot.find{ it.@name.text().equals(slotName) }
		
		// Check that a valid slot was found in the xml	
		assert slotGPath.size() != 0, 'Error: Slot Name supplied is not valid:'+slotName
	
		return slotGPath.@type.text()
	}
	
	/**
	Indicate if a slot is a page server or not
	*/
	boolean isSlotPageServer(String environment, String serverGroup, String serverName, String slotName)	
	{
		def serverGPath = findServer(environment, serverGroup, serverName)
	
		def slotGPath = serverGPath.slot.find{ it.@name.text().equals(slotName) }

		assert slotGPath.size() != 0, 'Error: Slot Name supplied is not valid:'+slotName

		return slotGPath.@type.text().toUpperCase().equals('PS')

	}
	
	/**
	Indicate if a slot is of a given type
	*/
	boolean isSlotType(String environment, String serverGroup, String serverName, String slotName, String slotType)
	{
		def serverGPath = findServer(environment, serverGroup, serverName)
	
		def slotGPath = serverGPath.slot.find{ it.@name.text().equals(slotName) }

		assert slotGPath.size() != 0, 'Error: Slot Name supplied is not valid:'+slotName

		return slotGPath.@type.text().equals(slotType)
	}
		
	/**
	Return all apps(servergroups) for an environment
	*/
	String[] getAppsForEnvironment(String environment)	
	{
		def appsList = []
		def environmentNode = xmlConfig.environment.find{ it.@name.text().equals(environment) } 
		
		// Check that a valid environment was found in the xml
		assert environmentNode.size() != 0, 'Error:  environment name supplied is not valid:'+environment
		
		environmentNode.servergroup.each
		{
			appsList.add(it.@application.text())
		}			
		return(appsList)		
	}

}

