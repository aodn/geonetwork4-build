<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
                xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/2.0"
                xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                version="2.0"
                exclude-result-prefixes="#all">

    <!--
      TODO: Need to check where it is using.imos_po.json
imos_po_gn3-12.json
imos_sample.json
imos_sample_gn3-12.json
prod_portal_catalogue.json
systest_portal_catalogue.json
    -->
    <!-- url substitutions to be performed -->
    <xsl:variable name="urlSubstitutions">

    <!-- For stack configurations eg dev stacks -->
    <substitution match="https?://processes.aodn.org.au" replaceWith="https://processes-systest.aodn.org.au"/>
    {% if config.get('stack_manifest', {}).get('Resources', {}).get('databucket', {}).get('endpoint') -%}
    <substitution match="https?://imos-data.s3-ap-southeast-2.amazonaws.com" replaceWith="https://{{ config.get('stack_manifest', {}).get('Resources', {}).get('databucket', {}).get('endpoint') }}"/>
    {% endif -%}
    {% if config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('AodnPortal', {}).get('endpoint') -%}
    <substitution match="https?://portal.aodn.org.au" replaceWith="https://{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('AodnPortal', {}).get('endpoint') }}"/>
    {% endif -%}
    {% if config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Thredds', {}).get('endpoint') -%}
    <substitution match="https?://thredds.aodn.org.au" replaceWith="http://{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Thredds', {}).get('endpoint') }}"/>
    {% endif -%}
    {% if config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geoserver', {}).get('endpoint') -%}
    <substitution match="https?://geoserver.123.aodn.org.au" replaceWith="http://{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geoserver', {}).get('endpoint') }}"/>
    <substitution match="https?://geoserver-portal.aodn.org.au" replaceWith="http://{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geoserver', {}).get('endpoint') }}"/>
    <substitution match="https?://tilecache.aodn.org.au/geowebcache/service/wms" replaceWith="http://{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geoserver', {}).get('endpoint') }}/geoserver/wms"/>
    {% endif -%}

    <!-- Geonetwork3 not in stack eg systest -->
    {%if config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Catalogue') -%}
    <substitution match="https?://catalogue-imos.aodn.org.au(:443)?" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Catalogue') }}"/>
    <substitution match="https?://catalogue-123.aodn.org.au(:443)?" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Catalogue') }}"/>
    {% elif config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('endpoint') -%}
    <substitution match="https?://catalogue-imos.aodn.org.au(:443)?" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('root_url') }}"/>
    <substitution match="https?://catalogue-123.aodn.org.au(:443)?" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('root_url') }}"/>
    {% endif -%}
    {% if config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('AodnPortal') -%}
    <substitution match="https?://portal.aodn.org.au" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('AodnPortal') }}"/>
    {% endif -%}
    {% if config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geoserver') -%}
    <substitution match="https?://geoserver.123.aodn.org.au" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geoserver') }}"/>
    <substitution match="https?://geoserver-portal.aodn.org.au" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geoserver') }}"/>
    {% endif -%}
    {% if config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Thredds') -%}
    <substitution match="https?://thredds.aodn.org.au(:443)?" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Thredds') }}"/>
    {% endif -%}
    {% if config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geowebcache') -%}
    <substitution match="https?://tilecache.aodn.org.au(:443)?" replaceWith="{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geowebcache') }}"/>
    {% endif -%}

  </xsl:variable>
  
  <xsl:variable name="urlSubstitutionSelector" select="string-join($urlSubstitutions/substitution/@match, '|')"/>

  <!-- default action is to copy -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- substitute production service endpoints with service endpoints from stack -->
  <xsl:template match="cit:CI_OnlineResource[cit:protocol/gco:CharacterString/not(text()='WWW:DOWNLOAD-1.0-http--downloadother')]/cit:linkage/gco:CharacterString[matches(., $urlSubstitutionSelector)]">
    <xsl:copy>
      <xsl:apply-templates mode="substitute" select="text()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@xlink:href[matches(., $urlSubstitutionSelector)]">
    <xsl:attribute name="xlink:href">
      <xsl:apply-templates mode="substitute" select="."/>
    </xsl:attribute>
  </xsl:template>

  <xsl:template mode="substitute" match="@*|text()">
    <xsl:variable name="url" select="."/>
    <xsl:for-each select="$urlSubstitutions/substitution">
      <xsl:if test="matches($url, string(@match))">
        <xsl:value-of select="replace($url, string(@match), string(@replaceWith))"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="mri:abstract/gco:CharacterString[matches(., $urlSubstitutionSelector)]">
    <xsl:variable name="abstractText" select="text()"/>
    <xsl:copy>
      <xsl:for-each select="$urlSubstitutions/substitution">
        <xsl:if test="matches($abstractText, string(@match))">
          <xsl:value-of select="replace($abstractText, string(@match), string(@replaceWith))"/>
        </xsl:if>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
