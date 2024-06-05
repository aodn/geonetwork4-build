<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:gco="http://www.isotc211.org/2005/gco"
                xmlns:gmd="http://www.isotc211.org/2005/gmd"
                version="2.0">

    <!-- url substitutions to be performed -->

    <xsl:variable name="urlSubstitutions">
        <substitution match="https?://geoserver-portal.aodn.org.au(:443)?" replaceWith="http://geoserver-systest.aodn.org.au"/>
        <substitution match="https?://thredds.aodn.org.au(:443)?" replaceWith="http://thredds-systest.aodn.org.au"/>
    </xsl:variable>

    <xsl:variable name="urlSubstitutionSelector" select="string-join($urlSubstitutions/substitution/@match, '|')"/>

    <!-- default action is to copy -->

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Convert point of truth links only to avoid accidental edits -->

    <xsl:template match="gmd:URL[../../gmd:protocol/*/text()='WWW:LINK-1.0-http--metadata-URL']">
        <gmd:URL><xsl:value-of select="replace(.,'catalogue-imos', 'catalogue-systest')"/></gmd:URL>
    </xsl:template>

    <!-- substitute production service endpoints with systest service endpoints -->

    <xsl:template match="gmd:URL[matches(., $urlSubstitutionSelector)]">
        <xsl:variable name="url" select="."/>

        <xsl:for-each select="$urlSubstitutions/substitution">
            <xsl:if test="matches(string($url), string(@match))">
                <gmd:URL><xsl:value-of select="replace($url, string(@match), string(@replaceWith))"/></gmd:URL>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="gmd:URL[../../gmd:protocol/*/text()='OGC:WPS--gogoduck']">
        <gmd:URL><xsl:value-of select="replace(.,'https?://processes.aodn.org.au', 'https://processes-systest.aodn.org.au')"/></gmd:URL>
    </xsl:template>

    <xsl:template match="gmd:URL[../../gmd:protocol/*/text()='OGC:WMS-1.1.1-http-get-map']">
        <gmd:URL><xsl:value-of select="replace(.,'https?://tilecache.aodn.org.au(:443)?', 'https://tilecache-systest.aodn.org.au')"/></gmd:URL>
    </xsl:template>

    <xsl:template match="gmd:URL[../../gmd:protocol/*/text()='OGC:WPS--netcdf-subset-service']">
        <gmd:URL><xsl:value-of select="replace(.,'https?://geoserver-wps.aodn.org.au(:443)?', 'http://geoserver-wps-systest.aodn.org.au')"/></gmd:URL>
    </xsl:template>
</xsl:stylesheet>
