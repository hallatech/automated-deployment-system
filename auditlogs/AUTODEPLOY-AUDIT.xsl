<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
  <html>
  <body>
  <h2>SpindriftAD - Audit Logs.</h2>
    <table border="1">
      <tr bgcolor="GREY" style="color:WHITE">
	    <TH>DATE</TH>
	    <TH>ENVIRONMENT</TH>
	    <TH>APPLICATION</TH>
	    <TH>RFC</TH>
	    <TH>SPINDRIFTAD_VERSION</TH>
	    <TH>ATG_CONFIG_TFS_PATH</TH>
      <TH>ATG_SIT_CONFIG_PATH</TH>
      <TH>RELEASE_OUTPUT</TH>
	    <TH>RELEASE_INPUT</TH>
	    <TH>ACTION</TH>
      </tr>
      <xsl:for-each select="AUDIT/ENTRY">
      <tr>
      	<xsl:for-each select="KEY">
        <td><xsl:value-of select="@VALUE"/></td>
     	</xsl:for-each>
      </tr>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>
