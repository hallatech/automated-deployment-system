package com.spindrift.autodeploy.common

class AuditManager extends AutoDeployBase
{
	String auditHeaders
	String auditFilename

	public AuditManager(String auditHeaders, String auditFilename) {
		this.auditHeaders=auditHeaders
		this.auditFilename=auditFilename
	}

	/**
	Write an audit map to a CSV file as supplied by the auditFilename
	The beans.xml file can be used to configure the filename as well as the header names used within the file.
	*/
	public void write(Map auditMap)
		{
			File auditFile=new File(auditFilename)
			
			if(!auditFile.exists())
			{
				String headerLine=''
				auditHeaders.tokenize(',').each()
				{
					headerLine+=it+','	
				}
				
				auditFile.write(headerLine)
			}
			
			String auditLine=''
			
			auditHeaders.tokenize(',').each()
			{
				def value=auditMap.get(it)
				
				if(value == null)
				{value = ''}
				
				auditLine = auditLine + value + ','	
			}
			
			auditFile.append("\n${auditLine}")
		}
}
