package com.spindrift.autodeploy.atg.deployment

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.atg.AtgBase
import groovy.transform.Synchronized


class RSyncDeployThread extends Thread {
	String syncDir
	String serverName
	Project project
	String execErrors
	String rsyncExecResult
	String scriptExecResult
	public RSyncDeployThread(String syncDir, String serverName, Project project){
		this.syncDir=syncDir
		this.serverName = serverName
		this.project = project
	}
	/**
	The actual rsync call is run within a thread
	*/
	public void run() {
		println " $name rsync started with local sync dir: $syncDir and remote: $serverName:$project.DEPLOYHOME"
		def excludeFromFile = project.ADROOT + "/exclude.txt"
		AntBuilder antBuilder = getAnt(project)
		def rsyncArgs = "--exclude-from=${excludeFromFile} --recursive --perms --checksum --delete --executability ${syncDir} $project.UserAccount@${serverName}:$project.DEPLOYHOME"
		def rsyncerror = "rsyncerror" + serverName
		def rsyncResult = "rsyncResult" + serverName 
		antBuilder.exec(dir:".", executable:"rsync", failonerror:"false", errorproperty:rsyncerror ,resultproperty:rsyncResult){
			arg(line:rsyncArgs)
		}
		rsyncExecResult =  antBuilder.properties.get(rsyncResult)
		//Run the post deployment script after the Rsync
		println "$name running post deployment script : $project.UserAccount@${serverName} 'sh $project.DEPLOYHOME/scripts/sh/postDeployment.sh'"
		def commandArgs = "$project.UserAccount@${serverName} 'sh $project.DEPLOYHOME/scripts/sh/postDeployment.sh'"
		def scripterror = "scripterror" + serverName
		def scriptResult = "scriptResult" + serverName
		project.ant.exec(dir:".", executable:"ssh", failonerror:"false", errorproperty:scripterror, resultproperty:scriptResult){
			arg(line:commandArgs)
		}
		scriptExecResult =  antBuilder.properties.get(scriptResult)
		execErrors = antBuilder.properties.get(rsyncerror) +  antBuilder.properties.get(scripterror)
	}

        @Synchronized
        private AntBuilder getAnt(Project project) {
                return project.ant
        }
}
