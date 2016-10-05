package com.spindrift.autodeploy.atg.deployment

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.atg.AtgBase
import com.spindrift.autodeploy.atg.parser.ParameterParser

class RSyncDeployManager extends AtgBase {

	void deployRelease(Project project) {
		logger.info(Logging.QUIET, "\nStart of RSync Env Deployment.. $project.DeployReleaseId")
		logger.info(Logging.QUIET, "============================================================")
		def serverNames = ParameterParser.getServerNames(project)
		def syncDirServerMap = [:]
		
		new File("$project.ReleaseFolder/$project.DeployReleaseId/$project.Environment").eachDir(){
			def appDir = it
			if (appDir.name != "WebserverConfig"){
				new File(appDir.path).eachDir(){
					if (serverNames.contains(it.name)){ 
						syncDirServerMap.put(it.path + "/", it.name)
					}
				}
			}
		}
		deployDirToServers(syncDirServerMap, project)
		
		logger.info(Logging.QUIET, "\nEnd of RSync Deployment to  $project.DeployReleaseId")
		logger.info(Logging.QUIET, "============================================================")

	}
	
	/**
	deploys the directory and machine map with rsync
	*/
	private void deployDirToServers(Map syncDirServerMap, Project project){
		Thread[] workerThreads = new Thread[syncDirServerMap.size()]
		def i=0;
		syncDirServerMap.each() { syncDir, server -> 
			workerThreads[i] = new RSyncDeployThread(syncDir, server, project)
			workerThreads[i++].start()
		}
		try { 
			(0..workerThreads.size()-1).each{ j->
				workerThreads[j].join()
			}
		}catch (InterruptedException ignore) {}

		(0..workerThreads.size()-1).each{ k->
			if (workerThreads[k].rsyncExecResult != "0" || workerThreads[k].scriptExecResult != "0"){ 
				assert false: "Error while running rsync or post deploy script for server ${workerThreads[k].serverName}  : ${workerThreads[k].execErrors}"
			}
                }

	}
}
