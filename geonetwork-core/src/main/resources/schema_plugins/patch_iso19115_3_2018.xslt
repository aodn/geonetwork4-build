<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="sidePanel">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <xsl:if test="not(directive[@data-gn-onlinesrc-list])">
                <xsl:text>&#10;    </xsl:text>
                <directive data-gn-onlinesrc-list="" data-types="parent|onlinesrc|sibling|associated|source|dataset|service|fcats"/>
            </xsl:if>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>