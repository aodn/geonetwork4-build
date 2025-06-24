<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="2.0">
    <xsl:output method="html" indent="yes"/>
    <!-- Identity template to copy all content, important to preserve the html tags -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    <!-- Template to match the <section class="abc"> -->
    <xsl:template match="//section[@class='gn-md-side-providedby']">
        <section class="gn-md-side-providedby">
            <h2>
                <i class="fa fa-fw fa-cog"></i>
                <span>Custom Provided By Section</span>
            </h2>
            <img class="gn-source-logo"
                 alt="Custom Logo"
                 src="custom-logo.png"></img>
        </section>
    </xsl:template>
</xsl:stylesheet>
