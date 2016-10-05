package com.spindrift.autodeploy.atg

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.logging.Logging
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.core.io.FileSystemResource
import com.spindrift.autodeploy.atg.parser.ApplicationsXmlParser
import com.spindrift.autodeploy.common.BeanManager
import com.spindrift.autodeploy.atg.parser.EnvXmlParser

class Initializer extends AtgBase implements Plugin<Project>	
{
	void apply(Project project) { 
		File adRoot = project.file ('../../..')
		project.setProperty('ADROOT', adRoot.absolutePath)
		
		project.setProperty('ADHOME', project.ADROOT + project.ADHOME)
		project.setProperty('AtgHome', project.ADHOME + project.AtgHome)
		project.setProperty('AtgEarFiles', project.ADROOT + project.AtgEarFiles)
		project.setProperty('AtgExampleConfig', project.ADROOT + project.AtgExampleConfig)
		project.setProperty('AtgTemplates', project.AtgHome + project.AtgTemplates)
		project.setProperty('AtgConfigTemp', project.ADROOT + project.AtgConfigTemp)
		project.setProperty('ReleaseFolder', project.ADROOT + project.ReleaseFolder)
		
		
		def config = new ConfigSlurper().parse(new File(project.AtgHome + "/atg.properties").toURL())
		config.setProperty('environmentXml', project.AtgHome + config.environmentXml)
		config.setProperty('environmentXsd', project.AtgHome + config.environmentXsd)
		config.setProperty('applicationXml', project.AtgHome + config.applicationXml)
		config.setProperty('applicationXsd', project.AtgHome + config.applicationXsd)
		config.setProperty('atgConfigSubstitutionXml', project.AtgHome + config.atgConfigSubstitutionXml)
		config.setProperty('atgConfigSubstitutionXsd', project.AtgHome + config.atgConfigSubstitutionXsd)
		config.setProperty('jbossConfigSubstitutionXml', project.AtgHome + config.jbossConfigSubstitutionXml)
		config.setProperty('jbossConfigSubstitutionXsd', project.AtgHome + config.jbossConfigSubstitutionXsd)
		
		project.setProperty('atg',config)
		
		project.setProperty('factory',BeanManager.getFactory())		
	}	
	
	Boolean preBuildVerification(Project project) {
		//Verify that home folder exists.
		assert new File(project.AtgHome).exists(), "Required folder ${project.ADHOME} does not exist."
		
		//Verify that AtgHome folder exists.
		assert new File(project.AtgHome).exists(), "Required folder ${project.AtgHome} does not exist."

		//Check for environment XML and XSD
		assert new File(project.atg.environmentXsd).exists(), "Required file ${project.atg.environmentXsd} does not exist." 
		assert new File(project.atg.environmentXml).exists(), "Required file ${project.atg.environmentXml} does not exist." 

		//Check for application XML and XSD
		assert new File(project.atg.applicationXsd).exists(), "Required file ${project.atg.applicationXsd} does not exist." 
		assert new File(project.atg.applicationXml).exists(), "Required file ${project.atg.applicationXml} does not exist." 

		//Check that an RFC number has been passed in as a project property
		assert project.property('RFC') != null
		
		def environment=project.envToBuild
		def envXmlParser = new EnvXmlParser(project.atg.environmentXml)
		def applicationsXmlParser = BeanManager.getBean("applicationsXmlParser")
		
		project.appsToBuild.each() {
			def serverGroup=it
			def serverNames = envXmlParser.getServerNames(environment, serverGroup)	

			// This is the release folder under which slots are created.	
			//def releaseFolder = "${project.ReleaseFolder}/${project.ReleaseID}/"

			serverNames.each {
				def serverName=it

				//Retreive a list of slots from the environment.xml file.
				def slots = envXmlParser.getSlotsForServer(environment, serverGroup, it)

				slots.each {
					//Copy build inputs described in applications.xml
					def applicationsXml = new XmlSlurper().parse(project.atg.applicationXml)
					def applicationXml = applicationsXml.application.find{it.@name.text().equals(serverGroup)}
					def sourcePath="${project.AtgEarFiles}/${serverGroup}/server/slot"
					applicationXml.buildinput.item.each() {
						assert new File("${sourcePath}/${it.source.text()}").exists(), "An expected file or folder ${sourcePath}/${it.source.text()} does not exist"
					}
				}
			}
		}			
	
		// Verify that the storage folder for release deliverables exist.
		assert new File(project.ReleaseFolder).exists(), "Required folder ${project.ReleaseFolder} does not exist."
		
		// Verify that the JBoss Appserver Source files exist.
		def jbossSource = project.ADHOME + project.atg.jbossAppserverSource
		assert new File(jbossSource).exists(), "Required installer ${jbossSource} does not exist."
	
		logger.info(Logging.QUIET, "Pre-build verifications completed successfully.")
		return true;
	}

	Boolean preDeployVerification(Project project) {
		//Checks go here.
		logger.info(Logging.QUIET, "Pre-deploy verifications completed successfully.")
		return true;
	}
}

