package com.spindrift.autodeploy.atg

import java.util.List
import org.gradle.api.*
import org.gradle.api.plugins.*
import com.spindrift.autodeploy.common.BeanManager
import com.spindrift.autodeploy.atg.parser.EnvXmlParser

class EnvironmentNavigator extends AtgBase 
{
	/**
	Invoke the closure for every slot in every server 
	in every serverGroup and every environment defined in gradle.properties
	*/
	void eachSlot(Project project, Closure c)
	{
		def env = project.envToBuild
		project.appsToBuild.each() {			
			def app = it
			def envXmlParser = new EnvXmlParser(project.atg.environmentXml)
			def servers = envXmlParser.getServerNames(env, app)
			servers.each {
				def server = it
				def slots = envXmlParser.getSlotsForServer(env, app, server)
				slots.each {
						def slot=it
						c.call(env, app, server, slot, project)
				}
			}				
		}
	}
	
	/**
	Invoke the closure for every slot in every server 
	in every serverGroup and every environment defined in gradle.properties
	*/
	void eachServer(Project project, Closure c) {
		def env = project.envToBuild
		project.appsToBuild.each() {			
			def app = it
			def envXmlParser = new EnvXmlParser(project.atg.environmentXml)
			def servers = envXmlParser.getServerNames(env, app)
			servers.each {
				def server = it
				c.call(env, app, server, project)
			}				
		}
	}
	
	/**
	Invoke the closure for the first server in the specified serverGroup.
	for every environment defined in gradle.properties
	*/
	void firstServerInApp(String application,Project project, Closure c) {
		def env = project.envToBuild
		project.appsToBuild.each() {			
			def app = it
			if (app != application) return
			
			def envXmlParser = new EnvXmlParser(project.atg.environmentXml)
			def servers = envXmlParser.getServerNames(env, app)
			c.call(env, app, servers[0], project)			
		}
	}	

}
