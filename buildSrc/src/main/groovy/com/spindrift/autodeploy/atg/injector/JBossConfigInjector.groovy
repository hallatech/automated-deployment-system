package com.spindrift.autodeploy.atg.injector

import java.io.File;
import java.util.Map;

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.logging.Logging
import com.spindrift.autodeploy.atg.parser.EnvXmlParser
import com.spindrift.autodeploy.atg.parser.JbossConfigXmlParser
import com.spindrift.autodeploy.common.BeanManager
import groovy.util.slurpersupport.GPathResult
import com.spindrift.autodeploy.atg.AtgBase

class JBossConfigInjector extends AtgBase 
{	
	private static final String DEFAULT_DS_TEMPLATE_FILE = "oracle-ds.xml";
	private static final String DS_TEMPLATE_FILE_SUFFIX = "_template-ds.xml";
	
	
	/**
	 * Copies the generated config into the build.
	 */
	public void injectJBossConfigToRelease(String pEnvironment, String pServerGroup, String pServerName, String pSlot, Project pProject)
	{
		def releaseFolder = "${pProject.ReleaseFolder}/${pProject.ReleaseID}/";
		def sourceFolder = "${pProject.AtgConfigTemp}/${pEnvironment}/${pServerName}/${pSlot}/jboss-server";
		def destinationFolder = "${releaseFolder}/${pEnvironment}/${pServerGroup}/${pServerName}/Jboss/jboss-as/server/${pSlot}";
		assert new File(sourceFolder).exists(), 'An expected folder ' + sourceFolder + ' does not exist';
		//We must overwrite here in order to overwrite existing files
		pProject.ant.copy(toDir:destinationFolder, overwrite:true)
		{
			fileset(dir:sourceFolder);
		}
		
		logger.info(Logging.QUIET, "Copied folder " + sourceFolder);
		logger.info(Logging.QUIET, "to " + destinationFolder);
	}
	

	/**
	 * Delegates the ATG generation task to private methods.
	 */
	void process(String pEnvironment, String pApplication, String pServer, String pSlot, Project pProject)
	{
		logger.info(Logging.QUIET, "processJbossConfig : Start.");
		logger.info(Logging.QUIET, "============================================================");
		
		copyTemplatesToSlot(pEnvironment, pApplication, pServer, pSlot, pProject);
		doXmlSubstitution(pEnvironment, pApplication, pServer, pSlot, pProject);
		createDatasourceDefinitions(pEnvironment, pApplication, pServer, pSlot, pProject);
		copyBinariesToSlot(pEnvironment, pApplication, pServer, pSlot, pProject);

		logger.info(Logging.QUIET, "processATGConfig : End.");
		logger.info(Logging.QUIET, "------------------------------------------------------------");
		logger.info(Logging.QUIET, "\n");
	}

	
	/**
	 * Copy ATG config templates that will need to be substituted.
	 */
	private void copyTemplatesToSlot(String pEnvironment, String pApplication, String pServer, String pSlot, Project pProject)
	{
		def sourceFolder = "${pProject.AtgHome}/templates/${pProject.JbossTemplates}";
    	def destinationFolder = "${pProject.AtgConfigTemp}/${pEnvironment}/${pServer}/${pSlot}/jboss-server";
				
		pProject.ant.delete(dir:destinationFolder);
        pProject.ant.mkdir(dir:destinationFolder);

        pProject.ant.copy(todir:destinationFolder, overwrite:false)
		{
            fileset(dir:"${sourceFolder}", excludes:"lib/ojdbc6.jar");
        }

		logger.info(Logging.QUIET, "Copied files from: ${sourceFolder} to ${destinationFolder}");
	}

		
	/**
	 * Substitute values in in XML files using the data froma jboss-config-substitution.xml.
	 */
	private void doXmlSubstitution(String pEnvironment, String pApplication, String pServer, String pSlot, Project pProject)
	{
		def envXmlParser = new EnvXmlParser(pProject.atg.environmentXml);
		def bindingsParser = BeanManager.getBean("jbossBindingsParser");
		
		logger.info(Logging.QUIET, "\nApplying bindings for ${pServer}/${pSlot}");
		logger.info(Logging.QUIET, "*********************************************");

		Map bindings = bindingsParser.getBindings(new File (pProject.atg.jbossConfigSubstitutionXml), pEnvironment, pSlot, pApplication, null);
		
		//Calculate and add the jvmRoute value for the given slot
		bindings.put("spindrift.jboss.deploy.jbossweb.server.jvmRoute",  getJvmRouteValue(pEnvironment, pApplication, pServer, pSlot, pProject, envXmlParser));
	
		logger.info(Logging.QUIET, "\nApplying ${bindings.size()} binding(s)");
		
		def configFolder = "${pProject.AtgConfigTemp}/${pEnvironment}/${pServer}/${pSlot}/jboss-server";
	
		new File(configFolder).eachFileRecurse
		{
			//Replace values in the file.
			if (!it.isFile()) return;
			def text=it.getText();
			bindings.each
			{
				text = text.replaceAll("#${it.key}#", it.value);
			}
			
			it.write(text);
		}
	}
	

	/**
	 * The jvmRoute is required for Apache mod_jk to route to correct node.
	 */
	private String getJvmRouteValue(String pEnvironment, String pApplication, String pServer, String pSlot, Project pProject, EnvXmlParser pEnvXmlParser)
	{ 
		def jvmRouteValue = "";
		def slotType = pEnvXmlParser.getSlotType(pEnvironment, pApplication, pServer, pSlot);

		if(slotType == "PS")
		{
			jvmRouteValue = "jvmRoute=\"" + pServer + "_" + pSlot + "\"";
		}
		else
		{
			def additionalApps = pProject.AdditionalWorkerApps.tokenize(',');
			additionalApps.each
			{
				def additionalApp = it;
				if(slotType == additionalApp)
				{
					jvmRouteValue = "jvmRoute=\"" + pServer + "_" + pSlot + "\"";
				}
			}
		}
		
		return jvmRouteValue;
	}

		
	/**
	 * The datasource definitions are created from templates where placeholders are substituted with values from jboss-config-substitution.xml.
	 */
	private void createDatasourceDefinitions(String pEnvironment, String pApplication, String pServer, String pSlot, Project pProject)
	{
		def envXmlParser = new EnvXmlParser(pProject.atg.environmentXml);
		def bindingsParser = BeanManager.getBean("jbossBindingsParser");
		
		logger.info(Logging.QUIET, "\nApplying datasource substitutions for ${pServer}/${pSlot}");
		logger.info(Logging.QUIET, "***********************************************************");
		
		File fleSubstitutions = new File(pProject.atg.jbossConfigSubstitutionXml);
		def setDatasources = bindingsParser.getDatasourceNamesForApplication(fleSubstitutions, pApplication);
		setDatasources.each
		{
			String sDatasource = it;
			Map mapDatasourceSubstitutions = bindingsParser.getBindings(fleSubstitutions, pEnvironment, pSlot, pApplication, sDatasource);
			
			String sTemplate = mapDatasourceSubstitutions.get("template");
			if((sTemplate == null) || (sTemplate == ""))
			{
				sTemplate = DEFAULT_DS_TEMPLATE_FILE;
				mapDatasourceSubstitutions.put("template", DEFAULT_DS_TEMPLATE_FILE);
			}
			
			logger.info(Logging.QUIET, "\nCreating datasource ${sDatasource} from template ${sTemplate}");
			def sourceFile = "${pProject.AtgConfigTemp}/${pEnvironment}/${pServer}/${pSlot}/jboss-server/deploy/${sTemplate}";
			def destnFile = "${pProject.AtgConfigTemp}/${pEnvironment}/${pServer}/${pSlot}/jboss-server/deploy/${sDatasource}-ds.xml";
			pProject.ant.copy(file:sourceFile, toFile:destnFile);
						
			logger.info(Logging.QUIET, "Applying ${mapDatasourceSubstitutions.size()} binding(s)");
			def dsFile = new File(destnFile);
			def dsText = dsFile.getText();
			mapDatasourceSubstitutions.each
			{
				dsText = dsText.replaceAll("#${it.key}#", it.value);
			}
			
			dsFile.write(dsText);
		}
		
		// Remove the templates from the destination folder
		new File("${pProject.AtgHome}/templates/${pProject.JbossTemplates}/deploy").eachFile
		{
			if(it.isFile())
			{
				String sName = it.name;
				if(sName.equals(DEFAULT_DS_TEMPLATE_FILE) || sName.endsWith(DS_TEMPLATE_FILE_SUFFIX))
				{
					pProject.ant.delete(file:"${pProject.AtgConfigTemp}/${pEnvironment}/${pServer}/${pSlot}/jboss-server/deploy/${sName}");
				}
			}
		}
	}
	

 	/**
     * Binaries should be copied separately after substitution, otherwise they get corrupted.
     */
    private void copyBinariesToSlot(String pEnvironment, String pApplication, String pServer, String pSlot, Project pProject)
	{
		logger.info(Logging.QUIET, "\nCopying binary files for ${pServer}/${pSlot}");
		logger.info(Logging.QUIET, "*********************************************");

        def sourceFolder = "${pProject.AtgHome}/templates/${pProject.JbossTemplates}/lib";
        def destinationFolder = "${pProject.AtgConfigTemp}/${pEnvironment}/${pServer}/${pSlot}/jboss-server/lib";

        pProject.ant.delete(dir:destinationFolder);
        pProject.ant.mkdir(dir:destinationFolder);

        pProject.ant.copy(todir:destinationFolder, overwrite:false)
		{
            fileset(dir:"${sourceFolder}");
        }

        logger.info(Logging.QUIET, "\nCopied files from: ${sourceFolder} to ${destinationFolder}");
    }

}
