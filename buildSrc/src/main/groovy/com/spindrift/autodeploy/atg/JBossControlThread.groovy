package com.spindrift.autodeploy.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.atg.AtgBase
import groovy.transform.Synchronized

class JBossControlThread extends Thread {
	String command
	String serverName
	Project project
	String execError
	String execResult
	public JBossControlThread(String command, String serverName, Project project){
		this.command=command
		this.serverName = serverName
		this.project = project
	}
	/**
	The actual control command call is run within a thread
	*/
	public void run() {
		println "\n $name is exectuting command jboss $command on server $serverName"
		def commandArgs = "$project.UserAccount@$serverName  \'/etc/init.d/jboss $command\'"
		def controlError = "controlError" + serverName
		def controlResult = "result" + serverName
		AntBuilder antBuilder = getAnt(project)
		antBuilder.exec(dir:".", executable:"ssh", failonerror:"false", errorproperty:controlError, resultproperty:controlResult){
			arg(line:commandArgs)
		}
		execError = antBuilder.properties.get(controlError)
		execResult = antBuilder.properties.get(controlResult)
	}
	@Synchronized
	private AntBuilder getAnt(Project project) { 
		return project.ant
	}
}

