package com.spindrift.autodeploy.atg.restart;

import java.util.List;
import java.util.Map;

public interface RestartStrategy
{
	public abstract void sort(List<Map<String, String>> pSlotList);
}
