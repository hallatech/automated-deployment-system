package com.spindrift.autodeploy.common

import org.gradle.api.logging.Logging

class TfsManager extends AutoDeployBase
{
	String server
	String user
	String password
	String domain
	String workspace
	String workfolder
	String tfsExecutable
    private String getLoginString() {
        return("-login:" + user + "@" + domain + ',' + password)
	}

	public void initialise(String tfsExecutable, String user, String password, String domain, String server) {
		setTfsExecutable(tfsExecutable)
		setUser(user)
		setPassword(password)
		setDomain(domain)
		setServer(server)
	}

	public void getLatest() {
		//tf get -recursive -force -all -login:${TFS_USER}@${TFS_DOMAIN},${TFS_PASSWORD}
        success=executeTfsCommand("get", "-recursive -force -all", getLoginString())
        assert success, "TFS command failed."

    }

	public void getLabel(String label) {
       //tf get -version:${LABEL} -force -all -login:${TFS_USER}@${TFS_DOMAIN},${TFS_PASSWORD}
		success=executeTfsCommand("get", "-recursive -force -all", getLoginString())
        assert success, "TFS command failed."
	}
 
	private Boolean executeTfsCommand(String tfsCommand, String options, String login) {
		def command="tf " + tfsCommand + " " + options + " " + login
		
		logger.info(Logging.QUIET, "Executing command " + command)
		def process=command.execute()
		
		process.waitFor()
		
		//Capture TFS output and write to command window
		println "stderr: ${process.err.text}"
		println "stdout: ${process.in.text}" 
				
		//Check return code. 
		assert process.exitValue()==0, 'The TFS task exited with errors.'
		
		return true
	}
}
