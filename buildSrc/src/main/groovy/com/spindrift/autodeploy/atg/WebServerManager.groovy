package com.spindrift.autodeploy.atg

import org.gradle.api.*;
import org.gradle.api.plugins.*;
import org.gradle.api.logging.Logging;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Map;
import com.spindrift.autodeploy.atg.parser.EnvXmlParser;
import com.spindrift.autodeploy.atg.parser.JbossConfigXmlParser;
import com.spindrift.autodeploy.common.BeanManager;
import com.spindrift.autodeploy.atg.parser.ApplicationsXmlParser;

class WebServerManager extends AtgBase
{
	ApplicationsXmlParser applicationsXmlParser;
	EnvXmlParser envXmlParser;
	JbossConfigXmlParser bindingsParser;

	/**
	 * Configure the worker.properties file specified by the 'propertiesFile' and write
	 * it to 'destination'
	 */
	void configureWorkers(Project project, String environment, String source, String destination, String appType) {
		File template=new File("${source}/workers.properties.minimal.template");
		new File("${destination}").mkdirs()
		File output= new File("${destination}/workers.properties")

		output.write(template.getText())

		String workers=""
		String workerBalancers=""
		String workerType=""
		String serverGroupBalancers=""
		
		envXmlParser = new EnvXmlParser(project.atg.environmentXml)
		applicationsXmlParser = new ApplicationsXmlParser(project.atg.applicationXml)
		bindingsParser = new JbossConfigXmlParser();
		
		/**
		 *   Add Additional Applications by slotType
		 */
		def additionalApps = project.AdditionalWorkerApps.tokenize(',')
		additionalApps.each {
			def additionalApp=it
			workers="${additionalApp}"
			workerType="worker.${additionalApp}.type=lb"

			output=writeHeader(output,workers,workerType)
			
			String[] appsForEnv = envXmlParser.getAppsForEnvironment(environment)
			appsForEnv.each {
				def serverGroup=it
				
				def serverNames = envXmlParser.getServerNames(environment,serverGroup)
				def serverAppType= applicationsXmlParser.getAppType(serverGroup);
	
				if ((appType == "Both") || (serverAppType == appType)) {
					serverNames=envXmlParser.getServerNames(environment,serverGroup)
					serverNames.each {
						serverNames = it
		
						String[] slots=envXmlParser.getSlotsForServer(environment, serverGroup, serverNames)
						
						slots.each {
							def slot=it
							serverGroupBalancers="worker.${additionalApp}.balance_workers=${serverNames}_${slot}"
							
							if(!envXmlParser.isSlotType(environment,serverGroup,serverNames,slot,additionalApp))
							{return}
		
							output=writeSlot(project,output,environment,serverGroup,serverNames,slot,serverGroupBalancers);
						}
					}
				}
			}
		}

		/**
		*   Add Standard Applications
		*/
		String[] appsForEnv = envXmlParser.getAppsForEnvironment(environment)
		appsForEnv.each {
			def serverGroup=it
			
			def serverNames = envXmlParser.getServerNames(environment,serverGroup)
			def serverAppType= applicationsXmlParser.getAppType(serverGroup);

			if ((appType == "Both") || (serverAppType == appType)) {
				
				workers="${serverGroup}"
				workerType="worker.${serverGroup}.type=lb"
	
				output=writeHeader(output,workers,workerType)
				
				serverNames=envXmlParser.getServerNames(environment,serverGroup)
			
				serverNames.each {
					serverNames = it
	
					String[] slots=envXmlParser.getSlotsForServer(environment, serverGroup, serverNames)
					
					slots.each {
						def slot=it
						serverGroupBalancers="worker.${serverGroup}.balance_workers=${serverNames}_${slot}"
						
						if(!envXmlParser.isSlotPageServer(environment,serverGroup,serverNames,slot))
						{return}
	
						output=writeSlot(project,output,environment,serverGroup,serverNames,slot,serverGroupBalancers)
					}
				}
			}
		}
	  }
	
	private File writeHeader(File output, String workers, String workerType) {
		output.append("\n\n\n###########################################################")
		output.append("\n# Workers list and Load balancing")
		output.append("\n###########################################################")
		output.append("\nworker.list=${workers}")
		output.append("\n${workerType}")
		output.append("\n")
		
		return(output)
	}
	
	private File writeSlot(Project project, File output, String environment, String serverGroup, String serverNames, String slot, String serverGroupBalancers)
	{
		Map jbossSlotBindings = bindingsParser.getBindings(new File(project.atg.jbossConfigSubstitutionXml), environment, slot, null, null)
		
		output.append("\n\n###########################################################")
		output.append("\n# Worker configuration for ${serverNames}_${slot}")
		output.append("\n###########################################################")

		output.append("\n${serverGroupBalancers}")
		output.append("\nworker.${serverNames}_${slot}.reference=worker.template")
		output.append("\nworker.${serverNames}_${slot}.host=${serverNames}")
		output.append("\nworker.${serverNames}_${slot}.port=${jbossSlotBindings.get("spindrift.jboss.webdeployer.server.ajp.connector.port")}")
		def partition = envXmlParser.getPartitionName(environment, serverGroup, serverNames, slot)
		output.append("\nworker.${serverNames}_${slot}.domain=${partition}")
		
		return(output)
	}
}