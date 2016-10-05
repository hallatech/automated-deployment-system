package com.spindrift.autodeploy.atg

import com.spindrift.autodeploy.common.BeanManager
import java.util.Date
import org.gradle.api.*
import com.spindrift.autodeploy.atg.parser.ParameterParser

class AtgAuditor extends AtgBase 
{
	/**
	Add context information regarding the current build to the auditMap object 
	and then use AuditManager to write them to a store
	*/
	void auditBuild(Project project)
	{
		def auditManager=BeanManager.getBean("auditManager")
		Map auditMap=BeanManager.getBean("auditMap")

		project.AtgApplications.tokenize(',').each()
		{
			auditMap.put("DATE",new Date().format('dd/MM/yyy HH:mm'))
			auditMap.put("ENVIRONMENT",project.Environment)
			auditMap.put("APPLICATION",it)
			auditMap.put("RFC",project.RFC)
			auditMap.put("AUTODEPLOY_VERSION",project.VERSION)
			auditMap.put("ATG_CONFIG_TFS_PATH",project.AtgTemplates)
			auditMap.put("ATG_SIT_CONFIG_PATH",project.AtgExampleConfig)
			auditMap.put("RELEASE_OUTPUT","${project.ReleaseFolder}/${project.ReleaseID}")
			auditMap.put("RELEASE_INPUT",project.AtgEarFiles)
			auditMap.put("ACTION","BUILD")

			auditManager.write(auditMap)	
		}
		def converter=new com.spindrift.autodeploy.common.Csv2Xml()
		converter.convert(auditManager.getAuditFilename(),auditManager.getAuditFilename()+'.XML')		
	}
	
	/**
	Add context information regarding the currentdeployment to the auditMap object 
	and then use AuditManager to write them to a store
	*/	
	void auditDeploy(Project project)
	{
		def auditManager=BeanManager.getBean("auditManager")
		Map auditMap=BeanManager.getBean("auditMap")
		def serverNames = ParameterParser.getServerNames(project)
		
		serverNames.each()
		{
			auditMap.put("DATE",new Date().format('dd/MM/yyy HH:mm'))
			auditMap.put("ENVIRONMENT",project.Environment)
			auditMap.put("APPLICATION",it)
			auditMap.put("RFC",'-')			
			auditMap.put("AUTODEPLOY_VERSION",project.VERSION)
			auditMap.put("ATG_CONFIG_TFS_PATH",'-')
			auditMap.put("ATG_SIT_CONFIG_PATH",'-')								
			auditMap.put("RELEASE_OUTPUT","${project.ReleaseFolder}/${project.DeployReleaseId}")
			auditMap.put("RELEASE_INPUT",'-')			
			auditMap.put("ACTION","DEPLOY")

			
			auditManager.write(auditMap)	
		}
		def converter=new com.spindrift.autodeploy.common.Csv2Xml()
		converter.convert(auditManager.getAuditFilename(),auditManager.getAuditFilename()+'.XML')		
	}
}
