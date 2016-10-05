package com.spindrift.autodeploy.atg.restart;

import java.util.List;
import java.util.Map;

import com.spindrift.autodeploy.common.AutoDeployBase;

public class OneAtATimeRestartStrategy extends AutoDeployBase implements RestartStrategy {

	@Override
	public void sort(List<Map<String, String>> pSlotList) {
		getLogger().info("Leaving restart list in default order");
	}

}
