package com.spindrift.autodeploy.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import com.spindrift.autodeploy.atg.parser.EnvXmlParser
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.common.BindingsParser
import com.spindrift.autodeploy.common.BeanManager
import com.spindrift.autodeploy.common.TemplateEngine

class ScriptManager extends AtgBase
{
	TemplateEngine templateEngine;

	/**
	 * The jboss start stop script that are to be deployed into the target servers.
	 */
	void generateJbossStartStopScript(String pEnvironment, String pServerGroup, String pServer, Project pProject)
	{
		logger.info(Logging.QUIET, "\nAdding start stop script for " + pServer);
		logger.info(Logging.QUIET, "*********************************************************");

		// This is the release folder under which slots are created.
		def releaseFolder = "${pProject.ReleaseFolder}/${pProject.ReleaseID}/";

		def destination = new File("${releaseFolder}${pEnvironment}/${pServerGroup}/${pServer}/scripts/sh");
		if (!destination.exists())
		{
			new File("${destination}").mkdirs();
		}
	   
		def envXmlParser = new EnvXmlParser(pProject.atg.environmentXml);
		def lstSlots = envXmlParser.getSlotsForServer(pEnvironment, pServerGroup, pServer);

		def sStartSlotCommands = "";
		def sStopSlotCommands = "CMD_STOP=\"\\\${JBOSS_HOME}/bin/shutdown.sh\"\n";
		def iJnpPort = 1099
		def iPortBindingSet = 0
		def sSlotList = "";
		lstSlots.each
		{
			def sSlot=it
			sSlotList += sSlot + ' ';

			String sJbossStartCommand = "CMD_START_${sSlot}=\"".toUpperCase();
			sJbossStartCommand += "rm -Rf \\\${JBOSS_HOME}/server/SLOT_NAME/tmp;";
			sJbossStartCommand += "\\\${JAVA_HOME}/bin/java JAVA_OPTS_FROM_ENVIRONMENT_XML -Djava.endorsed.dirs=\\\${JBOSS_HOME}/lib/endorsed -Djava.net.preferIPv4Stack=true -classpath \\\${JAVA_HOME}/lib/tools.jar:\\\${JBOSS_HOME}/bin/run.jar org.jboss.Main -g JBOSS_CLUSTER_NAME -u JBOSS_CLUSTER_MCAST_ADDRESS -c SLOT_NAME -b 0.0.0.0 -Djboss.service.binding.set=PORT_BINDING -Djboss.messaging.ServerPeerID=SERVER_PEER_ID -Datg.dynamo.data-dir=${pProject.DEPLOYHOME}/ATG-Data -Datg.dynamo.server.name=SLOT_NAME > \\\${JBOSS_HOME}/server/SLOT_NAME/log/console.log 2>&1 &"
			sJbossStartCommand += "\"";
			
			iPortBindingSet++
			sJbossStartCommand = sJbossStartCommand.replaceAll(~/SERVER_PEER_ID/, Integer.toString(generatePeerId(pServer, sSlot)))
			sJbossStartCommand = sJbossStartCommand.replaceAll(~/PORT_BINDING/, "ports-0" + Integer.toString(iPortBindingSet))
			sJbossStartCommand = sJbossStartCommand.replaceAll(~/JBOSS_CLUSTER_NAME/, envXmlParser.getPartitionName(pEnvironment, pServerGroup, pServer, sSlot))
			sJbossStartCommand = sJbossStartCommand.replaceAll(~/JBOSS_CLUSTER_MCAST_ADDRESS/, envXmlParser.getJbossClusterMCastAddress(pEnvironment, pServerGroup, pServer, sSlot))
			sJbossStartCommand = sJbossStartCommand.replaceAll(~/JAVA_OPTS_FROM_ENVIRONMENT_XML/, envXmlParser.getJavaOptsForSlot(pEnvironment, pServerGroup, pServer, sSlot))
			sJbossStartCommand = sJbossStartCommand.replaceAll(~/SLOT_NAME/, "${sSlot}")
			
			sStartSlotCommands += sJbossStartCommand;
			sStartSlotCommands += "\n";
			
			
			// stop command is defined here so that the jnp port can be replaced everytime dynamically.
			String sJbossStopCommand = "CMD_STOPARGS_${sSlot}=\"".toUpperCase();
			sJbossStopCommand += "-S -s jnp://localhost:JNP_PORT --user=\${JBOSS_STOP_USER} --password=\${JBOSS_STOP_PASSWORD}";
			sJbossStopCommand += "\"";

			iJnpPort += 100
			sJbossStopCommand = sJbossStopCommand.replaceAll(~/JNP_PORT/, Integer.toString(iJnpPort))
			sStopSlotCommands += sJbossStopCommand
			sStopSlotCommands += "\n"
		}
		
		def bindingsParser = BeanManager.getBean("jbossBindingsParser");
		Map bindings = bindingsParser.getBindings(new File (pProject.atg.jbossConfigSubstitutionXml), pEnvironment, "ALL", "ALL", null);
		bindings.put("spindrift.script.jboss.jbossUser", "jboss");
		bindings.put("spindrift.script.jboss.availableSlots", sSlotList.trim());
		bindings.put("spindrift.script.jboss.slotStartCommands", java.util.regex.Matcher.quoteReplacement(sStartSlotCommands));
		bindings.put("spindrift.script.jboss.slotStopCommands", java.util.regex.Matcher.quoteReplacement(sStopSlotCommands));
		bindings.put("spindrift.script.postDeployment.DeployHome", pProject.DEPLOYHOME)
		
		def sourceFile = new File("${pProject.AtgHome}/templates/scripts/jboss");
		def destinationFile = new File("${destination}/jboss");
		templateEngine.makeTemplate(bindings, sourceFile, destinationFile);
		logger.info(Logging.QUIET, "Applied bindings to ${destinationFile} using template ${sourceFile}");
			   
		logger.info(Logging.QUIET, "Created Jboss start stop script");
   }

   
	/**
	 * Generate a numeric clustering peer id - each node in a cluster must have a unique (numeric) id between 0 and 1023.
	 */
	private int generatePeerId(final String pServer, final String pSlot)
	{
		int iReturn = -1;
		
		if((pServer != null) && (pSlot != null))
		{
			char c1 = pServer.charAt(pServer.length() - 1);
			char c2 = pSlot.charAt(pSlot.length() - 1);
			iReturn = Integer.parseInt("" + c1 + c2);
		}
		
		return iReturn;
	}
	
	
	/**
	 * The jboss logs prune script
	 */
	void generatePruneJbossLogsScript(String pEnvironment, String pServerGroup, String pServer, Project pProject)
	{
		logger.info(Logging.QUIET, "\nAdding Jboss prune logs script for: " + pServer)
		logger.info(Logging.QUIET, "*********************************************************")
		
		// This is the release folder under which slots are created.
		def releaseFolder = "${pProject.ReleaseFolder}/${pProject.ReleaseID}/"

		def destination = new File("${releaseFolder}${pEnvironment}/${pServerGroup}/${pServer}/scripts/sh")
		if (!destination.exists())
		{
			new File("${destination}").mkdirs()
		}
		
		File output= new File("${destination}/pruneJbossLogs.sh")
		
		def envXmlParser = new EnvXmlParser(pProject.atg.environmentXml)
		def slots = envXmlParser.getSlotsForServer(pEnvironment, pServerGroup, pServer)

		output.append("#! /bin/sh")
		output.append("\n")

		slots.each
		{
			def slot = it
			def daysToRetain = envXmlParser.getlogFileRetentionDaysForSlot(pEnvironment, pServerGroup, pServer, slot)
			output.append("\n\necho \"Prune jboss logs for ${slot}\"")
			output.append("\nfind ${pProject.DEPLOYHOME}/Jboss/jboss-as/server/${slot}/log/ -mtime +${daysToRetain} -exec rm {} \\;")
		}
		
		logger.info(Logging.QUIET, "Created Jboss prune log script")
	}

		
	/**
	 * The post deployment script that changes mainly environment variables
	 */
	void generatePostDeploymentScript(String pEnvironment, String pServerGroup, String pServer, Project pProject)
	{
		logger.info(Logging.QUIET, "\nAdding post deployment scripts for: " + pServer)
		logger.info(Logging.QUIET, "*********************************************************")
		
		Map bindings = [:]
		bindings.put("spindrift.script.postDeployment.DeployHome", pProject.DEPLOYHOME)
		
		def sourceFile = new File("${pProject.AtgHome}/templates/scripts/postDeployment.sh")
		def destinationFile = new File("${pProject.ReleaseFolder}/${pProject.ReleaseID}/${pEnvironment}/${pServerGroup}/${pServer}/scripts/sh/postDeployment.sh")
		templateEngine.makeTemplate(bindings, sourceFile, destinationFile)
		logger.info(Logging.QUIET, "Applied bindings to ${destinationFile} using template ${sourceFile}")
		logger.info(Logging.QUIET, "Created post deployment script")
	}
	
	
	/**
	 * Link the smoke tests run for an environment, to those in the release that has just been deployed.
	 */
	void linkSmokeTests(final String pEnvironment, final String pReleaseId, Project pProject)
	{
		logger.info(Logging.QUIET, "\nLinking smoke tests for env:" + pEnvironment + " to release:" + pReleaseId);
		
		def scriptsDir = new File("${pProject.ReleaseFolder}/${pReleaseId}/${pProject.releaseSmokeTestsFolder}");

		def alCommands = ["rm -f ${pProject.smokeTestRoot}/${pEnvironment}", "ln -s ${scriptsDir} ${pProject.smokeTestRoot}/${pEnvironment}"];
		executeCommandSequence(alCommands, false, true);
		
		logger.info(Logging.QUIET, "Finished linking smoke tests");
	}
	
	
	/**
	 * Execute the specified series of shell commands.
	 *
	 * @param pCommands - the list of commands.
	 * @param pContinueOnError - whether to ignore errors and continue.
	 * @param pLogOutput - whether to log the output.
	 */
	private void executeCommandSequence(final ArrayList<String> pCommands, final boolean pContinueOnError, final boolean pLogOutput)
	{
		def sbOut = new StringBuffer();
		def sbErr = new StringBuffer();
		
		Process p;
		for(String sCmd : pCommands)
		{
			p = sCmd.execute();
			p.waitForOrKill(30000);
			p.consumeProcessOutput(sbOut, sbErr);
			if(!pContinueOnError && p.exitValue()) break;
		}
		
		if(pLogOutput)
		{
			if(sbOut.length() > 0) logger.info(Logging.QUIET, "out:\n" + sbOut);
			if(sbErr.length() > 0) logger.info(Logging.QUIET, "err:\n" + sbErr);
		}
	}
}
