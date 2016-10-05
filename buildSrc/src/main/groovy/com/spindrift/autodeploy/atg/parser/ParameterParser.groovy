package com.spindrift.autodeploy.atg.parser

import java.util.List;
import org.gradle.api.*;
import com.spindrift.autodeploy.atg.parser.ApplicationsXmlParser;
import com.spindrift.autodeploy.atg.parser.EnvXmlParser;

class ParameterParser
{

	/**
	 * Get the list of environments specified on the command line.
	 * 
	 * @param pProject
	 * @return List - The list of environments in the project.
	 */
	public static List<String> getEnvironments(Project pProject)
	{
		List<String> lstReturn = new ArrayList<String>();

		def projectProperties = pProject.getProperties();
		String env = projectProperties.get("Environment");
		lstReturn.add(env);
		
		return lstReturn;
	}
	
	
	/**
	 * Temporary measure until multiple environments are supported.
	 * @deprecated
	 * @param pProject
	 * @return String - First environment in list.
	 */
	public static String getEnvironment(Project pProject)
	{
		String sReturn = null;

		List<String> lstEnvironments = getEnvironments(pProject);
		if(!lstEnvironments.isEmpty())
		{
			sReturn = lstEnvironments.get(0);
		}
		
		return sReturn;
	}
	
	
	/**
	 * Get the list of applications specified on the command line.
	 * 
	 * @param pProject
	 * @return List - The list of applications in the project.
	 */
	public static List<String> getApplications(Project pProject)
	{
		List<String> lstReturn = new ArrayList<String>();

		List<String> lstEnvironments = getEnvironments(pProject);		
		if(lstEnvironments.isEmpty())
		{
			assert false: "Must specify Env property";
		}

		def projectProperties = pProject.getProperties();
		String sApplications = projectProperties.get("ServerGroups");
		def envXmlParser = new EnvXmlParser(pProject.atg.environmentXml);
		for(String sEnv: lstEnvironments)
		{
			if(sApplications != null)
			{
				List<String> lstEnvApplications = envXmlParser.getAppsForEnvironment(sEnv);
				for(String sApp: sApplications.tokenize(","))
				{
					if(lstEnvApplications.contains(sApp)) lstReturn.add(sApp);
				}
			}
		}

		return lstReturn;
	}
	

	/**
	 * Get the list of server names specified on the command line.
	 * 
	 * @param project
	 * @return
	 */
	public static List<String> getServerNames(Project project)
	{
		List<String> lstReturn = new ArrayList<String>();

		def projectProperties = project.getProperties();
		String env = projectProperties.get("Environment");
		String serverGroups = projectProperties.get("ServerGroups");
		String servers = projectProperties.get("Servers");
		
		def envXmlParser = new EnvXmlParser(project.atg.environmentXml);
		
		if(servers != null)
		{
			if((env == null) || (serverGroups == null))
			{
				assert false: "Must specify Env and ServerGroups property with Servers property";
			}
			
			def serverNames=[];
			serverGroups.tokenize(",").each
			{
				serverNames.addAll(envXmlParser.getServerNames(env, it));
			}
			
			servers.tokenize(",").each
			{
				if(!serverNames.contains(it))
				{
					assert false: "Server $it doesn't belong to Env $env and ServerGroups $serverGroups";
				}
				
				lstReturn.add(it);
			}
		}
		else if(serverGroups != null)
		{
			if(env==null)
			{
				assert false: "Must specify Env property with ServersGroups property";
			}
			
			def serverGroupNames = envXmlParser.getServerGroupNames(env);
			serverGroups.tokenize(",").each
			{
				if(!serverGroupNames.toList().contains(it))
				{
					assert false: "Server $it doesn't belong to Env $env";
				}
				
				lstReturn.addAll(envXmlParser.getServerNames(env, it));
			}
		}
		else if(env != null)
		{
			def serverGroupNames = envXmlParser.getServerGroupNames(env);
			serverGroupNames.each
			{
				lstReturn.addAll(envXmlParser.getServerNames(env, it));
			}
			
		}
		
		return lstReturn;
	}
	
}
