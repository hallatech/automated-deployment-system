package com.spindrift.autodeploy.atg.parser;

import com.spindrift.autodeploy.atg.AtgBase

class JbossConfigXmlParser extends AtgBase
{
	private static final String ALL = "ALL";
	private static final String SPINDRIFT_DS_NAME = "spindrift.datasource.name";
	private static final String TEMPLATE = "template";

	
	/**
	 * Generate a map of the bindings for the combination of arguments given.
	 * 
	 * @param pBindingsXml - XML file containing the bindings/substitutions.
	 * @param pEnvironment - the environment ('ALL' will be used if not specified).
	 * @param pSlot - the slot ('ALL' will be used if not specified).
	 * @param pApplication - the application ('ALL' will be used if not specified).
	 * @param pDatasource - the datasource.
	 * @return Map
	 */
	Map getBindings(File pBindingsXml, String pEnvironment, String pSlot, String pApplication, String pDatasource)
	{
		def mapReturn = [:] as Map;

		def parsedXML = new XmlParser().parse(pBindingsXml);

		// Get ALL application mappings
		getBindingsForApplication(mapReturn, pEnvironment, pSlot, parsedXML.application.find{ it.@name == ALL }, pDatasource);
		// Overlay with application specific mappings
		getBindingsForApplication(mapReturn, pEnvironment, pSlot, parsedXML.application.find{ it.@name == pApplication}, pDatasource);
		
		// TODO: Use hash of hashes for both files and data-sources?
		
		//mapReturn.each { println("Binding: ${it.key} = ${it.value}"); }
		return mapReturn;
	}
	
	
	/**
	 * Get the names of the datasources applicable to the combination of arguments given.
	 * 
	 * @param pBindingsXml - XML file containing the bindings/substitutions.
	 * @param pApplication - the application ('ALL' will be used if not specified).
	 * @return
	 */
	Set getDatasourceNamesForApplication(File pBindingsXml, String pApplication)
	{
		// Using a set as will not create duplicate entries.
		def setReturn = [] as Set;
		//println("getDatasourceNamesForApplication(pBindingsXml, ${pApplication})");
		
		def parsedXML = new XmlSlurper().parse(pBindingsXml);

		// ALL application mappings
		def appXml = parsedXML.application.find{ it.@name == ALL };
		setReturn.addAll(appXml.datasource.@name.collect{ it.text() });
		
		if(pApplication != null)
		{
			// Specific application mappings
			appXml = parsedXML.application.find{ it.@name == pApplication };
			setReturn.addAll(appXml.datasource.@name.collect{ it.text() });
		}
		
		// Remove the 'ALL' datasource if it has been added.
		setReturn.remove(ALL);
		
		return setReturn;
	}
	
	
	/**
	 * Update the map of the bindings with the values for the combination of arguments given.
	 * 
	 * @param pMap - the map to add the additional bindings/substitutions to.
	 * @param pEnvironment - the environment ('ALL' will be used if not specified).
	 * @param pSlot - the slot ('ALL' will be used if not specified).
	 * @param pApplication - the application ('ALL' will be used if not specified).
	 * @param pDatasource - the datasource.
	 */
	private void getBindingsForApplication(Map pMap, String pEnvironment, String pSlot, Node pApplicationNode, String pDatasource)
	{
		if(pApplicationNode != null)
		{
			List<Node> lstElements = pApplicationNode.children();
			lstElements.each
			{
				if(it.name() == "key")
				{
					processEnvKey(it, pMap, pEnvironment);
				}
				else if(it.name() == "slotKey")
				{
					processSlotKey(it, pMap, pSlot);
				}
			}
		}
		
		if(pDatasource != null)
		{
			getDatasourceBindingsForApplication(pMap, pEnvironment, pSlot, pApplicationNode, pDatasource);
		}
	}
	
	
	/**
	 * Update the map of the bindings with the datasource values for the combination of arguments given.
	 * 
	 * @param pMap - the map to add the additional bindings/substitutions to.
	 * @param pEnvironment - the environment ('ALL' will be used if not specified).
	 * @param pSlot - the slot ('ALL' will be used if not specified).
	 * @param pApplication - the application ('ALL' will be used if not specified).
	 * @param pDatasource - the datasource ('ALL' will be used first, then the name supplied here)
	 */
	private void getDatasourceBindingsForApplication(Map pMap, String pEnvironment, String pSlot, Node pApplicationNode, String pDatasource)
	{
		if(pApplicationNode != null)
		{
			List<Node> lstElements = pApplicationNode.children();
			lstElements.each
			{
				if((it.name() == "datasource") && (it.@name == ALL))
				{
					processDatasource(it, pMap, pEnvironment, pSlot);
				}
			}

			lstElements.each
			{
				if((it.name() == "datasource") && (pDatasource != null) && (it.@name == pDatasource))
				{
					processDatasource(it, pMap, pEnvironment, pSlot);
				}
			}
		}
	}
	
	
	/**
	 * Process a (env) key XML node.
	 * 
	 * @param pNode - Node of the XML tree to process.
	 * @param pMap - the map to add the additional bindings/substitutions to.
	 * @param pEnvironment - the environment ('ALL' will be used if not specified).
	 */
	private void processEnvKey(Node pNode, Map pMap, String pEnvironment)
	{
		processKey(pNode, pMap, pEnvironment);
	}

	
	/**
	 * Process a slotKey XML node.
	 * 
	 * @param pNode - Node of the XML tree to process.
	 * @param pMap - the map to add the additional bindings/substitutions to.
	 * @param pSlot - the slot ('ALL' will be used if not specified).
	 */
	private void processSlotKey(Node pNode, Map pMap, String pSlot)
	{
		processKey(pNode, pMap, pSlot);
	}

	
	/**
	 * Process a (slot or env) key XML node.
	 * 
	 * @param pNode - Node of the XML tree to process.
	 * @param pMap - the map to add the additional bindings/substitutions to.
	 * @param pMatch - the environment or slot to use.
	 */
	private void processKey(Node pNode, Map pMap, String pMatch)
	{
		//println("processKey(${pNode.name()}:${pNode.@name}, pMap, ${pMatch})");
		Node node = pNode.find{ it.@name == ALL };
		String sValueAll = node ? node.text() : null;
		node = pNode.find{ it.@name == pMatch };
		String sValueEnv = node ? node.text() : null;
		if(((sValueAll != null) && (sValueAll.length() > 0)) || ((sValueEnv != null) && (sValueEnv.length() > 0)))
		{
			String sValue = ((sValueEnv != null) && (sValueEnv.length() > 0)) ? sValueEnv : sValueAll;
			//println("processKey() storing binding: ${pNode.@name}, ${sValue}");
			pMap.put(pNode.@name, sValue);
		}
	}

	
	/**
	 * Process a datasource XML node.
	 * 
	 * @param pNode - Node of the XML tree to process.
	 * @param pMap - the map to add the additional bindings/substitutions to.
	 * @param pEnvironment - the environment ('ALL' will be used if not specified).
	 * @param pSlot - the slot ('ALL' will be used if not specified).
	 */
	private void processDatasource(Node pNode, Map pMap, String pEnvironment, String pSlot)
	{
		//println("processDatasource(${pNode.name()}:${pNode.@name}, pMap, ${pEnvironment}, ${pSlot})");
		String sName = pNode.@name;

		pMap.put(SPINDRIFT_DS_NAME, sName);
		pMap.put(TEMPLATE, pNode.attribute(TEMPLATE));

		List<Node> lstKeys = pNode.children();
		lstKeys.each
		{
			if(it.name() == "key")
			{
				processKey(it, pMap, pEnvironment);
			}
			else
			{
				processKey(it, pMap, pSlot);
			}
		}
	}
		
}
