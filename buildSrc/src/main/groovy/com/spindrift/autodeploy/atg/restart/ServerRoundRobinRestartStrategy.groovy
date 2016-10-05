package com.spindrift.autodeploy.atg.restart;

import java.util.List;
import java.util.Map;

import com.spindrift.autodeploy.common.AutoDeployBase;

/**
 * This restart strategy tries to distribute the restarts across servers.
 * 
 * @author kevin
 *
 */
public class ServerRoundRobinRestartStrategy extends AutoDeployBase implements RestartStrategy 
{
	/* (non-Javadoc)
	 * @see com.spindrift.autodeploy.atg.restart.RestartStrategy#sort(java.util.List)
	 */
	@Override
	public void sort(List<Map<String, String>> pRestarts)
	{
		getLogger().info("Sorting list to round-robin");
		getLogger().debug("Starting sort(${pRestarts})");

		List<Map<String, String>> lstTemp = new ArrayList<Map<String, String>>();
		int iOldSize = -1;
		while(lstTemp.size() != iOldSize)
		{
			iOldSize = lstTemp.size();
			
			String sLastServer = "";
			String sLastType = "";
			if(lstTemp.size() > 0)
			{
				sLastServer = lstTemp.get(lstTemp.size() - 1).get("name");
				sLastType = lstTemp.get(lstTemp.size() - 1).get("type");
			}
			
			for(Map<String, String> entry: pRestarts)
			{
				if(!lstTemp.contains(entry))
				{
					String sServer = entry.get("name");
					String sType = entry.get("type");
					String sSlot = entry.get("slot");
					
					if(!(
						sServer.equals(sLastServer)
						&& sType.equals(sLastType)
						))
					{
						getLogger().trace("Added: ${sServer}.${sSlot} (${sSlot})");
						lstTemp.add(entry);
						sLastServer = sServer;
						sLastType = sType;
					}
				}
			}
			
			// Add any remaining
			for(Map<String, String> entry: pRestarts)
			{
				if(!lstTemp.contains(entry))
				{
					String sServer = entry.get("name");
					String sType = entry.get("type");
					String sSlot = entry.get("slot");
					getLogger().trace("Added: ${sServer}.${sSlot} (${sSlot})");
					lstTemp.add(entry);
				}
			}
		}
		
		pRestarts.clear();
		pRestarts.addAll(lstTemp);
		getLogger().debug("> sort(...):: order now: ${pRestarts}");
	}

}
