package com.spindrift.autodeploy.atg

import org.gradle.api.*;
import org.gradle.api.plugins.*;
import com.spindrift.autodeploy.atg.parser.ApplicationsXmlParser;
import com.spindrift.autodeploy.atg.parser.EnvXmlParser;
import com.spindrift.autodeploy.atg.parser.ParameterParser;
import org.gradle.api.logging.Logging;
import groovy.util.slurpersupport.GPathResult;
import groovy.util.slurpersupport.NodeChild;

class JBossManager extends AtgBase
{	
	/**
	 * Create slots as defined in the environments.xml. These slots are created under the
	 * "environment" and "servergroup" passed in as parameters.
	 */
	void createSlot(String environment, String serverGroup, String server, String slot,Project project)
	{
		logger.info(Logging.QUIET, "Creating slot ${serverGroup}.${server}.${slot}");
		def serverFolder = "${project.ReleaseFolder}/${project.ReleaseID}/${environment}/${serverGroup}/${server}/Jboss/jboss-as/server";
		def slotFolder = "${serverFolder}/${slot}";

		project.ant.mkdir(dir:slotFolder);
		project.ant.copy(todir:slotFolder)
		{
			def fromDir="${serverFolder}/${project.atg.templateSlot}";
			fileset(dir:fromDir);
			logger.info(Logging.QUIET, "\nCopied files from: ${fromDir} to: ${slotFolder}");
		}
	}
	

	/**
	 * Control jboss slots with start, stop or restart command
	 */
	void controlJboss(Project pProject)
	{
		logger.info(Logging.QUIET, "Control Jboss");
		def command = pProject.Command;
		def xpApp = new ApplicationsXmlParser(pProject.atg.applicationXml);
		List<Map<String, String>> lstSlotStartupOrder = xpApp.getSlotStartupOrder(pProject);
		for(Map<String, String> entry : lstSlotStartupOrder)
		{
			logger.info("Entry:: name=${entry.name}, slot=${entry.slot}, type=${entry.type}, app=${entry.app}");
		}
		
		return;
		
		/** TODO: Need to be able to restart page server instances in parallel. */
		/* Therefore need to have already grouped entries in list :( */
		def envXmlParser = new EnvXmlParser(pProject.atg.environmentXml);
		Thread[] workerThreads = new Thread[startServerNames.size()];
		
		def i=0;
		startServerNames.each() { serverName ->
			workerThreads[i] = new JBossControlThread(command, serverName, pProject)
			workerThreads[i++].start()
		}
		
		try
		{
			(0..workerThreads.size()-1).each{ j->
				workerThreads[j].join()
			}
		}
		catch(InterruptedException ignore)
		{}
		
		(0..workerThreads.size()-1).each{ k->
			if (workerThreads[k].execResult != "0")
			{
				assert false: "Error while running jboss control command on server ${workerThreads[k].serverName}  : ${workerThreads[k].execError}"
			}
		}
	}

		
	void deleteFromSlot(String environment, String serverGroup, String server, String slot,Project project)
	{
//		ApplicationsXmlParser applicationsXmlParser = new ApplicationsXmlParser(project.atg.applicationXml)
//		logger.info(Logging.QUIET, "Deleting from slot ${serverGroup}.${server}.${slot}")
//		def serverFolder = "${project.ReleaseFolder}/${project.ReleaseID}/${environment}/${serverGroup}/${server}/Jboss/jboss-as/server"
//		def slotFolder = "${serverFolder}/${slot}"
//		def file = "slotFolder"
//
//		//project.ant.mkdir(dir:slotFolder)
//		project.ant.delete()
//		project.ant.copy(todir:slotFolder) {
//			def fromDir="${serverFolder}/${project.atg.templateSlot}"
//			fileset(dir:fromDir)
//			logger.info(Logging.QUIET, "\nCopied files from: ${fromDir} to: ${slotFolder}")
//		}
	}
}
