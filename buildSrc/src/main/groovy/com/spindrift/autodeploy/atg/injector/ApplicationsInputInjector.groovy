package com.spindrift.autodeploy.atg.injector

import org.gradle.api.*
import org.gradle.api.plugins.*
import com.spindrift.autodeploy.atg.parser.EnvXmlParser
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.atg.parser.ApplicationsXmlParser
import groovy.util.slurpersupport.GPathResult
import com.spindrift.autodeploy.atg.AtgBase

class ApplicationsInputInjector extends AtgBase	{
	/**
	Copy build inputs to the server folder.
	*/
	public void injectInputToServer(String environment,String serverGroup,String serverName, Project project) {
		def releaseFolder = "${project.ReleaseFolder}/${project.ReleaseID}/"
		def applicationsXml = new XmlSlurper().parse(new File(project.atg.applicationXml))
		def sourcePath="${project.AtgEarFiles}/${serverGroup}/server"
		def destinationPath="${releaseFolder}/${environment}/${serverGroup}/${serverName}/Jboss/jboss-as"

		def applicationXml = applicationsXml.application.find{it.@name.text().equals("All")}
		def items=applicationXml.buildinput.item
		items.each() {
			def item=it
			def serverItems = item.findAll{item.destination.@receiver.text().equals("server")}
			injectInput(serverItems,sourcePath,destinationPath,project)
		}

		items=applicationXml.buildinput.deleteFromServer.serverItem
		removeItems(items,destinationPath,project)
		
		applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
		items=applicationXml.buildinput.item
		items.each() {
			def item=it
			def serverItems = item.findAll{item.destination.@receiver.text().equals("server")}
			injectInput(serverItems,sourcePath,destinationPath,project)
		}

		items=applicationXml.buildinput.deleteFromServer.serverItem
		removeItems(items,destinationPath,project)
	}
	
	/**
	Copy build inputs to the slot folder.
	*/
	public void injectInputToSlot(String environment,String serverGroup,String serverName,String slot,Project project) {
		def releaseFolder = "${project.ReleaseFolder}/${project.ReleaseID}/"
		def applicationsXml = new XmlSlurper().parse(new File(project.atg.applicationXml))
		def sourcePath="${project.AtgEarFiles}/${serverGroup}/server/slot"
		def destinationPath="${releaseFolder}${environment}/${serverGroup}/${serverName}/Jboss/jboss-as/server/${slot}"

		def envXmlParser = new EnvXmlParser(project.atg.environmentXml)
		def slotType = envXmlParser.getSlotType(environment,serverGroup,serverName,slot)

		// Add to All slots		
		def applicationXml = applicationsXml.application.find{it.@name.text().equals("All")}
		def items=applicationXml.buildinput.item
		items.each() {
			def item=it
			def slotItems = item.findAll{item.destination.@receiver.text().equals("slot") && ((item.@slotType.text() == "") || (item.@slotType.text().equals(slotType)))}
			injectInput(slotItems,sourcePath,destinationPath,project)
		}

		// Delete from All slots
		items=applicationXml.buildinput.deleteFromSlot.slotItem
		items.each() {
			def item=it
			def slotItems = item.findAll{item.@slotType.text() == "" || item.@slotType.text().equals(slotType)}
			removeItems(slotItems,destinationPath,project)
		}

		// Add to Specific Application slot
        applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
		items=applicationXml.buildinput.item
		items.each() {
			def item=it
			def slotItems = item.findAll{item.destination.@receiver.text().equals("slot") && ((item.@slotType.text() == "") || (item.@slotType.text().equals(slotType)))}
			injectInput(slotItems,sourcePath,destinationPath,project)
		}
		
		// Delete from Specific Application slot
        items=applicationXml.buildinput.deleteFromSlot.findAll{it.@slotType.text() == "" || it.@slotType.text().equals(slotType)}
		items.each() {
		  removeItems(it.slotItem,destinationPath,project)
		}
	}

        /**
        Copy build inputs to the ATG-Data folder.
        */
	public void injectInputToAtgData(String environment,String serverGroup,String serverName,String slot,Project project) {
    	def releaseFolder = "${project.ReleaseFolder}/${project.ReleaseID}/"
        def applicationsXml = new XmlSlurper().parse(new File(project.atg.applicationXml))
        def destinationPath="${releaseFolder}${environment}/${serverGroup}/${serverName}/ATG-Data/servers/${slot}"

		def envXmlParser = new EnvXmlParser(project.atg.environmentXml)
		def slotType = envXmlParser.getSlotType(environment,serverGroup,serverName,slot)

        def applicationXml = applicationsXml.application.find{it.@name.text().equals("All")}
        def items=applicationXml.buildinput.deleteFromAtgData.AtgDataItem
        removeItemsBySlotType(items,destinationPath,project,slotType)
				
        applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
        items=applicationXml.buildinput.deleteFromAtgData.AtgDataItem
        removeItemsBySlotType(items,destinationPath,project,slotType)
	}

	/**
	Remove application items from the release folders.
	*/
	public void removeItemsBySlotType(GPathResult items, String destinationPath, Project project, String slotType) {
		items.each() {
			def item=it
				
			logger.debug(Logging.QUIET, "applications.xml AtgData - type = ${item.@type.text()}")
			logger.debug(Logging.QUIET, "applications.xml AtgData - AppSlotType:${item.@slotType.text()} EnvSlotType:${slotType}")
			
			if(item.@type.text().equals("file") && item.@slotType.text().equals(slotType)) {
				project.ant.delete(file:"${destinationPath}/${item.text()}")
				logger.info(Logging.QUIET, "applications.xml Removed item ${destinationPath}/${item.text()}")
			}
			else if (item.@type.text().equals("folder") && item.@slotType.text().equals(slotType)) {
				project.ant.delete(dir:"${destinationPath}/${item.text()}")
				logger.info(Logging.QUIET, "applications.xml Removed item ${destinationPath}/${item.text()}")
			}
			
		}
	}
		
				
	/** 
	Remove application items from the release folders.
	*/
	public void removeItems(GPathResult items, String destinationPath, Project project) {
		items.each() {
			def item=it

			if(item.@type.text().equals("file")) {
				project.ant.delete(file:"${destinationPath}/${item.text()}")
			}
			else if (item.@type.text().equals("folder")) {
				project.ant.delete(dir:"${destinationPath}/${item.text()}")
			}
			logger.info(Logging.QUIET, "applications.xml Removed item ${destinationPath}/${item.text()}")
		}
	}

	/**
	Copy build input artefacts to the release folders.
	*/
	public void injectInput(GPathResult items, String sourcePath, String destinationPath, Project project) {
		items.each() {
			def item=it
		
			if(item.source.@type.text().equals("file")) {
				if(item.destination.@type.text().equals("file")) {
					project.ant.copy(file:"${sourcePath}/${item.source.text()}",toFile:"${destinationPath}/${item.destination.text()}",overwrite:true)
				}
				else if (item.destination.@type.text().equals("folder")) {
					project.ant.copy(file:"${sourcePath}/${item.source.text()}",toDir:"${destinationPath}/${item.destination.text()}",overwrite:true)
				}
			}
			else if (item.source.@type.text().equals("folder")) {
				project.ant.copy(toDir:"${destinationPath}/${item.destination.text()}",overwrite:true) {
					fileset(dir:"${sourcePath}/${item.source.text()}")
				}
			}
			logger.info(Logging.QUIET, "applications.xml Copied ${sourcePath}/${item.source.text()} to ${destinationPath}/${item.destination.text()}")
		}
	}
}
