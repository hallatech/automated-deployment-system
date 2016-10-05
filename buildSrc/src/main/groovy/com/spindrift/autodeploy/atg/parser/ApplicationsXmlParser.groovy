package com.spindrift.autodeploy.atg.parser;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.*;
import groovy.util.slurpersupport.GPathResult;
import groovy.util.slurpersupport.NodeChild;
import com.spindrift.autodeploy.atg.AtgBase;
import com.spindrift.autodeploy.atg.restart.OneAtATimeRestartStrategy
import com.spindrift.autodeploy.atg.restart.RestartStrategy;

class ApplicationsXmlParser extends AtgBase
{
	public static final String SLOT_TYPE_OTHER = "OTHER";
	private static final String ALL = "All";
	private static final String RESTART_STRATEGY_PACKAGE = "com.spindrift.autodeploy.atg.restart";
	

    // The environment XML File Object.  File to be read is in the gradle.properties file
    def xmlConfig;
    
    public ApplicationsXmlParser(String applicationsXml)
	{
		xmlConfig = new XmlSlurper().parse(new File(applicationsXml));
	}

	/**
	 * Find the startup order of the slots.
	 *
	 * @param pProject
	 * @return List<Map<String, String>> - a list of Maps of form [name=<server name>, slot=<slot name>].
	 */
	public List<Map<String, String>> getSlotStartupOrder(Project pProject)
	{
		List<Map<String, String>> lstReturn = null;
		logger.debug("Starting getSlotStartupOrder(" + pProject + ")");
		
		def projectProperties = pProject.getProperties();
		String sEnv = ParameterParser.getEnvironment(pProject);
		def xpEnv = new EnvXmlParser(pProject.atg.environmentXml);
		List<String> lstApplications = getRelevantApplications(sEnv, ParameterParser.getApplications(pProject), xpEnv);
		List<String> lstServers = ParameterParser.getServerNames(pProject);
   
		lstReturn = getEnvSlotStartupOrder(sEnv, lstApplications, lstServers, xpEnv);
	   
		logger.debug("> getSlotStartupOrder(...) returns: " + lstReturn);
		return lstReturn;
	}

   
	/**
	 * Get Application type (frontend or back).
	*/
	String getAppType(String appName)
	{
		def app = xmlConfig.application.find{ it.@name.text().equals(appName) };
		assert app.size() != 0, 'Error:  application name supplied is not valid:'+appName;
		return(app.@type.text());
	}	

	
	/**
	 * Get slot-type startup order for specified application.
	 *
	 * @param pAppName - Application of interest.
	 * @return String[] - Ordered list of slot types always containing at least the entry 'OTHER'.
	 */
	private String[] getApplicationSlotTypeStartupOrder(String pAppName)
	{
		String[] saReturn = null;
		logger.trace("Starting getSlotTypeStartupOrder(" + pAppName + ")");
		
		def mapValues = [:] as Map;
		
		// Start with 'ALL' apps.
		getApplicationSlotTypeStartupOrder(mapValues, ALL);
		// Override with named app.
		getApplicationSlotTypeStartupOrder(mapValues, pAppName);
		
		int iSize = mapValues.size();
		if(iSize > 0)
		{
			saReturn = new String[iSize];
			int i = 0;
			
			// Retrieve the slot types from the map order of 'startup order'.
			// NOTE: Order is not guaranteed if the same 'startup order' is specified for multiple slot types.
			Integer[] intOrders = mapValues.values().toArray(new Integer[0]);
			Arrays.sort(intOrders);
			
			int iMin = intOrders[0].intValue();
			int iMax = intOrders[iSize - 1].intValue();
			for(int j = iMin; j <= iMax; j++)
			{
				if(mapValues.containsValue(new Integer(j)))
				{
					mapValues.each {
						if(((Integer)it.value).intValue() == j)
						{
							saReturn[i] = it.key.toString();
							i++;
						}
					}
				}
			}
		}
		else
		{
			saReturn = new String[1];
			saReturn[0] = SLOT_TYPE_OTHER;
		}
		
		logger.trace("> getSlotTypeStartupOrder(...) returns: " + saReturn);
		return saReturn;
	}
	
	
	/**
	 * Populate the given map with the slot-type startup order from the specified application.
	 * 
	 * @param pMap
	 * @param pAppName
	 */
	private void getApplicationSlotTypeStartupOrder(Map pMap, String pAppName)
	{
		logger.trace("Starting getSlotTypeStartupOrder(" + pMap + ", " + pAppName + ")");
		
		def app = xmlConfig.application.find{ it.@name.text().equals(pAppName) };
		if(app.size() > 0)
		{
			def startStop = app.startStop;
			if(startStop != null)
			{
				def startupOrder = app.startStop.slotTypeStartupOrder;
				if((startupOrder != null) && (startupOrder.size() > 0))
				{
					startupOrder.slot.each {
						String sKey = it.@type.text();
						Integer intValue = it.@order.toInteger();
						pMap.put(sKey, intValue);
					}
					
					pMap.put(SLOT_TYPE_OTHER, startupOrder.@default.text().toInteger());
				}
			}
		}

		logger.trace("> getSlotTypeStartupOrder(...): map now: " + pMap);
	}

	
	/**
	 * If list of applications not specified then get all for environment.
	 * 
	 * @param pEnv
	 * @param pApplications
	 * @param pEnvParser
	 * @return List - List of application names.
	 */
	private List<String> getRelevantApplications(String pEnv, List<String> pApplications, EnvXmlParser pEnvParser)
	{
		List lstReturn = new ArrayList();
		logger.trace("Starting getRelevantApplications(" + pEnv + ", " + pApplications + ", " + pEnvParser + ")");
		
		if(lstReturn.isEmpty())
		{
			lstReturn.addAll(pEnvParser.getAppsForEnvironment(pEnv));
		}

		logger.trace("> getRelevantApplications(...) returns: " + lstReturn);
		return lstReturn;
	}
   

	/**
	 * Get the slot startup order for the combination of arguments specified.
	 * 
	 * @param pEnv
	 * @param pApplications
	 * @param pServerNames
	 * @param pEnvParser
	 * @param pRestartStrategy
	 * @return List<Map<String, String>> - List of form [name=<server name>, slot=<slot name>, type=<slot type>, app=<application>].
	 */
	private List<Map<String, String>> getEnvSlotStartupOrder(String pEnv, List<String> pApplications, List<String> pServerNames, EnvXmlParser pEnvParser)
	{
		List<Map<String, String>> lstReturn = new ArrayList<Map<String, String>>();
		logger.trace("Starting getSlotStartupOrder(" + pEnv + ", " + pApplications + ", " + pServerNames + ", " + pEnvParser + ")");
		
		for(String sApplication : pApplications)
		{
			List<Map<String, String>> lstRestarts = getApplicationSlotStartupOrder(pEnv, sApplication, pServerNames, pEnvParser);
			for(Map<String, String> map : lstRestarts)
			{
				map.put("app", sApplication);
			}
			
			lstReturn.addAll(lstRestarts);
		}
		
		logger.trace("> getSlotStartupOrder(...) returns: " + lstReturn);
		return lstReturn;
	}


	/**
	 * Get the default slot startup order for the combination of arguments specified - sorted by slot type, and by order of definition in XML.
	 * 
	 * @param pEnv
	 * @param pApplications
	 * @param pServerNames
	 * @param pEnvParser
	 * @return List<Map<String, String>> - List of form [name=<server name>, slot=<slot name>, type=<slot type>].
	 */
	private List<Map<String, String>> getApplicationSlotStartupOrder(String pEnv, String pApplication, List<String> pServerNames, EnvXmlParser pEnvParser)
	{
		List<Map<String, String>> lstReturn = null;
		logger.trace("Starting getApplicationSlotStartupOrder(${pEnv}, ${pApplication}, ${pServerNames}, ${pEnvParser})");
		
		lstReturn = getApplicationDefaultSlotStartupOrder(pEnv, pApplication, pServerNames, pEnvParser);
		applyRestartStrategy(pApplication, lstReturn);

		logger.trace("> getApplicationSlotStartupOrder(...) returns: " + lstReturn);
		return lstReturn;
	}
	
	/**
	 * Get the default slot startup order for the combination of arguments specified - sorted by slot type, and by order of definition in XML.
	 * 
	 * @param pEnv
	 * @param pApplication
	 * @param pServerNames
	 * @param pEnvParser
	 * @return List<Map<String, String>> - List of form [name=<server name>, slot=<slot name>, type=<slot type>].
	 */
	private List<Map<String, String>> getApplicationDefaultSlotStartupOrder(String pEnv, String pApplication, List<String> pServerNames, EnvXmlParser pEnvParser)
	{
		List<Map<String, String>> lstReturn = new ArrayList();
		logger.trace("Starting getApplicationDefaultSlotStartupOrder(" + pEnv + ", " + pApplication + ", " + pServerNames + ", " + pEnvParser + ")");
		
		ArrayList lstSlotTypeStartupOrder = new ArrayList();
		lstSlotTypeStartupOrder.addAll(getApplicationSlotTypeStartupOrder(pApplication));
		def lstEnvServerNames = pEnvParser.getServerNames(pEnv, pApplication);
		int iOtherPosition = 0;
		lstSlotTypeStartupOrder.each
		{
			String sSlotType = it;
			if(sSlotType.equals(ApplicationsXmlParser.SLOT_TYPE_OTHER))
			{
				// Record position that 'OTHER' slots should be inserted at
				iOtherPosition = lstReturn.size();
			}
			else
			{
				lstEnvServerNames.each
				{
					String sServerName = it;
					if((pServerNames == null) || pServerNames.contains(sServerName))
					{
						def lstSlotNames = pEnvParser.getSlotsForServer(pEnv, pApplication, sServerName);
						lstSlotNames.each
						{
							String sSlotName = it;
							if(pEnvParser.isSlotType(pEnv, pApplication, sServerName, sSlotName, sSlotType))
							{
								Map map = new HashMap<String, String>(3);
								map.put("name", sServerName);
								map.put("slot", sSlotName);
								map.put("type", sSlotType);
								lstReturn.add(map);
							}
						}
					}
				}
			}
		}

		// Insert 'OTHER' slots at correct position in list
		lstEnvServerNames.each
		{
			String sServerName = it;
			if((pServerNames == null) || pServerNames.contains(sServerName))
			{
				def lstSlotNames = pEnvParser.getSlotsForServer(pEnv, pApplication, sServerName);
				lstSlotNames.each
				{
					String sSlotName = it;
					if(!lstSlotTypeStartupOrder.contains(pEnvParser.getSlotType(pEnv, pApplication, sServerName, sSlotName)))
					{
						Map map = new HashMap(3);
						map.put("name", sServerName);
						map.put("slot", sSlotName);
						map.put("type", ApplicationsXmlParser.SLOT_TYPE_OTHER);
						lstReturn.add(iOtherPosition, map);
						// Increment 'OTHER' position so as not to reverse list
						iOtherPosition++;
					}
				}
			}
		}

		logger.trace("> getApplicationDefaultSlotStartupOrder(...) returns: " + lstReturn);
		return lstReturn;
	}

	
	/**
	 * Use the supplied RestartStrategy to sort the restart list.
	 * 
	 * @param pApplication
	 * @param pRestartList
	 */
	private void applyRestartStrategy(final String pApplication, final List<Map<String, String>> pRestartList)
	{
		logger.trace("Starting applyRestartStrategy(${pApplication}, ${pRestartList})");
		
		RestartStrategy strategy = getApplicationRestartStrategy(pApplication);
		if(strategy != null)
		{
			strategy.sort(pRestartList);
		}
		
		logger.trace("> > applyRestartStrategy(...):: Order now: ${pRestartList}");
		logger.trace("> applyRestartStrategy(...) returns");
	}
	
	
	/**
	 * Return the RestartStrategy specified in the XML for the application.
	 * This will return a default instance of OneAtATimeRestartStrategy if none is specified.
	 * 
	 * @param pApplication
	 * @return RestartStrategy
	 */
	private RestartStrategy getApplicationRestartStrategy(final String pApplication)
	{
		RestartStrategy rsReturn = null;
		logger.trace("Starting getApplicationRestartStrategy(" + pApplication + ")");
		
		def sRestartStrategyName = getApplicationRestartStrategyName(pApplication);
		if((sRestartStrategyName == null) || (sRestartStrategyName.length() < 1))
		{
			sRestartStrategyName = getApplicationRestartStrategyName(ALL);
		}
		
		if(sRestartStrategyName != null)
		{
			logger.trace("> > getApplicationRestartStrategy(...) looking for class: " + sRestartStrategyName);
			Class<RestartStrategy> clsRestartStrategy = this.class.classLoader.loadClass(RESTART_STRATEGY_PACKAGE + '.' + sRestartStrategyName);
			if(clsRestartStrategy != null)
			{
				rsReturn = clsRestartStrategy.newInstance();
			}
		}

		if(rsReturn == null)
		{
			logger.trace("> > getApplicationRestartStrategy(...) using default");
			rsReturn = new OneAtATimeRestartStrategy();
		}
		
		logger.trace("> getApplicationRestartStrategy(...) returns: " + rsReturn);
		return rsReturn;
	}

	
	/**
	 * Get the name of the RestartStrategy specified for the application in the XML.
	 * 
	 * @return String
	 */
	private String getApplicationRestartStrategyName(final String pApplication)
	{
		String sReturn = null;
		logger.trace("Starting getApplicationRestartStrategyName(" + pApplication + ")");
		
		def app = xmlConfig.application.find{ it.@name.text().equals(pApplication) };
		if(app.size() > 0)
		{
			def startStop = app.startStop;
			if(startStop != null)
			{
				def elemRestartStrategy = app.startStop.restartStrategy;
				if(elemRestartStrategy != null)
				{
					sReturn = elemRestartStrategy.@name.text();
				}
			}
		}
		
		logger.trace("> getApplicationRestartStrategyName(...) returns: " + sReturn);
		return sReturn;
	}
	
}
