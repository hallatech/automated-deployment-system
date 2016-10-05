package com.spindrift.autodeploy.common

import java.io.InputStream
import java.io.OutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

class Csv2Xml extends AutoDeployBase
{
	void convert(String source, String destination)
	{
		boolean justStarted=true
	
		def headers
		def values

		StringBuffer xml=new StringBuffer('<AUDIT>')

		new File(source).eachLine()
		{

			if(justStarted)
			{
				justStarted=false
				headers=it.tokenize(",")
			}
			else
			{
				values=it.tokenize(",")
				xml.append("<ENTRY>")
				for (def i=0;i<headers.size();i++)
				{
					xml.append("<KEY NAME=\"${headers.get(i)}\" VALUE=\"${values.get(i)}\"/>")
				}
				xml.append("</ENTRY>")
			}
		}
		xml.append("</AUDIT>")
		
		def stringWriter = new StringWriter() 
		def node = new XmlParser().parseText(xml.toString()); 
		new XmlNodePrinter(new PrintWriter(stringWriter)).print(node) 
		File dest = new File(destination)
		dest.write('<?xml-stylesheet type="text/xsl" href="AUTODEPLOY-AUDIT.xsl"?>')
		dest.append(stringWriter.toString())
		
	}
}
