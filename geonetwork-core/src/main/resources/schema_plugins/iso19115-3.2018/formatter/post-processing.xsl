<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>
    <xsl:param name='xml' select=''/>
    <xsl:template match="//section[@class='gn-md-side-providedby']">
        <section class="gn-md-side-providedby-custom">
            <h2>
                <i class="fa fa-fw fa-cog"></i>
                <span>Custom Provided By Section</span>
            </h2>
            <img class="gn-source-logo"
                 alt="Custom Logo"
                 src="custom-logo.png" />
        </section>
    </xsl:template>
</xsl:stylesheet>