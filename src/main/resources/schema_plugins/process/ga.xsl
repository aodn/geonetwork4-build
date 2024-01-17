<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:gmd="http://www.isotc211.org/2005/gmd"
                xmlns:gco="http://www.isotc211.org/2005/gco"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="2.0">

    <!-- Include code to add collection config -->

    <xsl:include href="add-collection-config.xsl"/>

    <!-- Fix incorrectly mapped MD_LegalConstraints/reference element -->

    <xsl:template match="gmd:MD_LegalConstraints/gmd:reference">
        <!-- not valid in iso19139 - map to otherConstraint -->
        <xsl:element name="gmd:otherConstraints">
            <xsl:element name="gco:CharacterString">
                <xsl:value-of select="gmd:CI_Citation/gmd:title/*/text()"/>
            </xsl:element>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
