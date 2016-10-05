package com.spindrift.autodeploy.atg

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.atg.parser.EnvXmlParser
import com.spindrift.autodeploy.common.BeanManager

class ReleaseManager extends AtgBase {
	
	/**
	Generate a unique release number. This will be used to name the release folder.
	*/
	String generateReleaseId(Project project) {
		project.ant.buildnumber (file : "mybuild.number")
		def today= new Date().format('ddMM')
        def releaseNumber = 'R_' + project.ant.properties."build.number" + '_' + today

		File file = new File("latestBuildLocation.txt")
		// We will write the latest build number only if the environment is CI
		if (project.Environment=="CI") { 
			file.write(releaseNumber)
		}

		return releaseNumber
	}

	/**
	Lock a release to prevent deletion
	*/
    void lockRelease(Project project, String releaseNumber) {
		File file = new File("${project.ReleaseFolder}/${project.ReleaseID}/lock")
		file.write("Release Locked and will not be deleted")
    }

	/**
	Unlock a release to prevent deletion
	*/
	void unlockRelease(Project project, String releaseNumber) {
		File file = new File("${project.ReleaseFolder}/${project.ReleaseID}/lock")
		file.delete()
	}
	
    /** 
    Validate a Release Number for deployment
    */
	String validateReleaseId(Project project, String releaseNumber) {
	  if (releaseNumber.equals("latest")) {
		File file = new File("latestBuildLocation.txt")
		def latestReleaseNumber
		file.withReader { line->latestReleaseNumber = line.readLine().trim()}
		return latestReleaseNumber
	  }
	  return releaseNumber
	}
		
	/**
	Create a root folder for each server on the release path
	*/
	void createServerRootFolder(String environment, String serverGroup, String server, Project project) {
		def rootFolder = "${project.ReleaseFolder}/${project.ReleaseID}/${environment}/${serverGroup}/${server}"
		project.ant.mkdir(dir:rootFolder)
		logger.info(Logging.QUIET, "Folder created: ${rootFolder}" )
	}	


	/**
	Delete releases that older than the number days indicated by project property AGE.
	*/
	public void deleteReleases(Project project) {
		logger.info(Logging.QUIET, "deleteReleases : Start.")
		logger.info(Logging.QUIET, "============================================================")
		logger.info(Logging.QUIET, "Deleting all releases before ${new Date().minus(new Integer(project.Age).intValue())}")

		new File(project.ReleaseFolder).eachDir() {
			if(new Date().minus(new Date(it.lastModified())) > new Integer(project.Age).intValue()&& it.getName().startsWith('R')) {
				
				def lock = new File("${it}/lock")
				if (!lock.exists()) {
				   logger.info(Logging.QUIET, "Deleting ${it.getPath()}")
				   project.ant.delete(dir:it.getPath())
				} else {
				   logger.info(Logging.QUIET, "Release ${it.getPath()} is locked: Not deleting")	
				}
			}
		}
		logger.info(Logging.QUIET, "deleteReleases : End.")
		logger.info(Logging.QUIET, "------------------------------------------------------------")
		logger.info(Logging.QUIET, "\n")
	}		
}
