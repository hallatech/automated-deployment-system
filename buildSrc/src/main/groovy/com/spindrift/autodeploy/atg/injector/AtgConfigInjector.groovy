package com.spindrift.autodeploy.atg.injector

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.atg.parser.EnvXmlParser
import com.spindrift.autodeploy.common.BeanManager
import groovy.util.slurpersupport.GPathResult
import com.spindrift.autodeploy.atg.AtgBase

class AtgConfigInjector extends AtgBase {
	/**
	Delegates the ATG generation task to private methods
	*/
	void process(String environment,String application,String server,String slot,Project project) {
	
		logger.info(Logging.QUIET, "processATGConfig : Start.")	
		logger.info(Logging.QUIET, "============================================================")
		
		copyTemplatesToSlot(environment,application,server,slot,project) 		
		copyExampleConfigToSlot(environment,application,server,slot,project)

		def slotFolder = "${project.AtgConfigTemp}/${environment}/${server}/${slot}"
		def xmlConfig = new XmlSlurper().parse(new File(project.atg.atgConfigSubstitutionXml))
		
		doEnvSubstitutions (project, xmlConfig, application, environment, slotFolder)
		doSlotSubstitutions (project, xmlConfig,slot, slotFolder)
		
		def envXmlParser = new EnvXmlParser(project.atg.environmentXml)
		def slottype = envXmlParser.getSlotType(environment, application, server, slot)
		doSlotTypeSubstitutions (project, xmlConfig,slottype, slotFolder)
		
		doKeySubstitutions(environment,application,server,slot,project)
		
		logger.info(Logging.QUIET, "processATGConfig : End.")
		logger.info(Logging.QUIET, "------------------------------------------------------------")
		logger.info(Logging.QUIET, "\n")			
	}
	
	
	/**
	Copy ATG Config Templates. 
	Please note that this action will overwrite some of the ATG configuration files supplied by engineering.
	*/
	private void copyTemplatesToSlot(String environment, String application, String server, String slot, Project project) {
		def commonFolder = "${project.AtgTemplates}/ATG-Data/Common"
		def applicationFolder = "${project.AtgTemplates}/ATG-Data/$application"
		def slotFolder = "${project.AtgConfigTemp}/${environment}/${server}/${slot}"
				
		logger.info(Logging.QUIET, "\nCopying atg config templates to ${slotFolder}")
		logger.info(Logging.QUIET, "**********************************************") 

		project.ant.delete(dir:slotFolder)
		project.ant.mkdir(dir:slotFolder)

		project.ant.copy(todir:slotFolder,overwrite:false) {
			fileset(dir:commonFolder) 			
			fileset(dir:applicationFolder) 			
		}
		logger.info(Logging.QUIET, "Copied files from: ${commonFolder} and ${applicationFolder} to ${slotFolder}" )
	}
	
	/**
	Generate configuration data for each server and slot for the specified application.
	*/
	private void copyExampleConfigToSlot(String environment,String application, String server, String slot,Project project) {
		logger.info(Logging.QUIET, "\nCreating atg config folder for ${server}/${slot}")
		logger.info(Logging.QUIET, "**************************************************")
		logger.info(Logging.QUIET, "${project.AtgExampleConfig}")
		
		def sourceFolder = "${project.AtgExampleConfig}/${application}/localconfig"
		def configFolder = "${project.AtgConfigTemp}/${environment}/${server}/${slot}/localconfig"
		
		FileTree tree = project.fileTree(dir:sourceFolder)
		
		tree.visit {element ->
				File destinationFile = new File("${configFolder}/${element.relativePath}")	
				if (element.file.isFile() && destinationFile.exists()) { 
					assert false: "AtgConfigInjector->copyExampleConfigToSlot : File at ${element.relativePath} already exists in ${destinationFile}, cannot proceed.." 
				}
		}
		project.ant.copy(todir:configFolder,overwrite:false) {
			fileset(dir:sourceFolder) 			
		}
		logger.info(Logging.QUIET, "Copied files from: ${sourceFolder} to ${configFolder}" )
	}	

	/**
	Environment substution as specified by the envsubstitutions node in the atg-config-substitution.xml
	*/
	private void doEnvSubstitutions(Project project, GPathResult xmlConfig, String application, String environment, String slotFolder) {
		
		def applicationSubs = xmlConfig.envsubstitutions.find{it.@application.text().equals(application)}
		applicationSubs.substitution.each {
			def substype=it.@type.text()
			if (substype == "modify"){
				doSubstitutions(it, slotFolder, "env", environment)
			}
			if (substype == "delete"){
				def envToDeleteFrom = it.@env.text()
				if (envToDeleteFrom.contains(environment)) { 
					project.ant.delete(file:"${slotFolder}/${it.@filepath.text()}")
				}
			}
		}
	}
	
	/**
	Slot substution as specified by the slotsubstitutions node in the atg-config-substitution.xml
	*/
	private void doSlotSubstitutions(Project project, GPathResult xmlConfig, String slot, String slotFolder) {
	
		xmlConfig.slotsubstitutions.substitution.each {
			def substype=it.@type.text()
			if (substype == "modify"){
				doSubstitutions(it, slotFolder, "slot", slot)
			}
			//We are not expecting any substitution type of delete for slot substitution
		}
	}

	/**
	Slot Type substution as specified by the slottypesubstitutions node in the atg-config-substitution.xml
	*/
	private void doSlotTypeSubstitutions(Project project, GPathResult xmlConfig, String slotType, String slotFolder) {
		
		xmlConfig.slottypesubstitutions.substitution.each {
			def substype=it.@type.text()
			if (substype == "modify"){
				doSubstitutions(it, slotFolder, "slottype", slotType)
			}
			if (substype == "delete"){
				def slottypeToDeleteFrom = it.@slottype.text()
				if (slottypeToDeleteFrom.contains(slotType)) {
					project.ant.delete(file:"${slotFolder}/${it.@filepath.text()}")
				}
			}
		}
	}
	
	/**
	Helper method used by all substition type methods
	*/
	private void doSubstitutions(GPathResult substitutionNode, String slotFolder, String substitutePropertyType, String substitutePropertyValue) { 
		
		File file=new File("${slotFolder}/${substitutionNode.@filepath.text()}")
		
		if (!file.exists()) {
			logger.info(Logging.QUIET, "WARNING: An expected SIT configuration file does not exist :" + file.getPath() )
		}
		else {
			logger.info(Logging.QUIET, "Processing file for slot substitution :" + file.getPath() )
			def newFileText=new StringBuffer()
			def originalText=file.getText()
			file.eachLine {
				String line=it
				String newLine = replacePropertyValue(line,substitutionNode.property,substitutePropertyType,substitutePropertyValue)
				newFileText.append(newLine)
			}
			file.write(newFileText.toString())
			if(!newFileText.toString().equals(originalText)) {
				logger.info(Logging.QUIET, "Amended file: " + file.getPath() )
			}
		}
	}
	
	/**
	if a line is a valid key value pair where the key is in the given properties list, prepare a new line
	using the key and an slot specific value provided in the properties list.
	*/
	private String replacePropertyValue(String line, GPathResult property, String substitutePropertyType, String substitutePropertyValue) {
		
		if(line.startsWith('#')) return line + '\n'
		if(!line.contains('=')) return line + '\n'

		def tokens=line.trim().tokenize('=')
		
		def propertyInThisLine = property.find { it.@name.text().equals(tokens.get(0).trim())}
		
		if(propertyInThisLine.size()==0) return line + '\n'

		if (tokens.size()>2) {
			logger.error(Logging.QUIET, "Invalid configuration line found in ATG Config files ==>" + line )
			assert false
		}		
		
		def newPropertyValue
		switch (substitutePropertyType) {
			case "env":
				newPropertyValue = propertyInThisLine.env.find{it.@name.text().equals(substitutePropertyValue)}
				break
			case "slot":
				newPropertyValue = propertyInThisLine.slot.find{it.@name.text().equals(substitutePropertyValue)}
				break
			case "slottype":
				newPropertyValue = propertyInThisLine.slottype.find{it.@name.text().equals(substitutePropertyValue)}
				break
		}
		
		if(newPropertyValue.size()==0) return line + '\n'
		return propertyInThisLine.@name.text() + '=' + newPropertyValue.text() + '\n'
	}	
	
	/**
	Xml files substitution specified by the keysubstitution node in the atg-config-substitution.xml
	*/
	private void doKeySubstitutions(String environment,String application, String server, String slot, Project project) {
		def envXmlParser = new EnvXmlParser(project.atg.environmentXml)
		def bindingsParser = BeanManager.getBean("bindingsParser")
		
		logger.info(Logging.QUIET, "\nApplying key substitution for ${server}/${slot}")
		logger.info(Logging.QUIET, "*********************************************")
		
		Map envBindings = bindingsParser.getBindings(new File(project.atg.atgConfigSubstitutionXml),environment,application)
		logger.info(Logging.QUIET, "\nSubstitution map size : ${envBindings.size()}")
		
		def configFolder = "${project.AtgConfigTemp}/${environment}/${server}/${slot}/localconfig"
		def slotNumber=bindingsParser.getBindings(new File(project.atg.jbossConfigSubstitutionXml),slot,application).get("spindrift.jboss.slot.number")
	    def slotType = envXmlParser.getSlotType(environment, application,server,slot)
	
		new File(configFolder).eachFileRecurse {
			//Replace values in the file.
			if (!it.isFile()) return
			def text=it.getText()
			
			envBindings.each {
				text=text.replaceAll("#${it.key}#",it.value)
			}
			it.write(text)					
		}	
	}

	/**
	Copy ATG Configuration files from the working directory to ATG Data Folder under the release folder
	**/
	public void copyWorkingAtgConfigToRelease(String environment,String serverGroup,String serverName,String slot,Project project) {
		def releaseFolder = "${project.ReleaseFolder}/${project.ReleaseID}/"
		def sourceFolder = "${project.AtgConfigTemp}/${environment}/${serverName}/${slot}/localconfig"
		def destinationFolder = "${releaseFolder}/${environment}/${serverGroup}/${serverName}/ATG-Data/servers/${slot}/localconfig"

		assert new File(sourceFolder).exists(), 'An expected folder' + sourceFolder + ' does not exist'
		project.ant.copy(toDir:destinationFolder, overwrite:false) {
			fileset(dir:sourceFolder)
		}
		logger.info(Logging.QUIET, "Copied folder " + sourceFolder)
		logger.info(Logging.QUIET, "to " + destinationFolder)
	}
} 
