<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/2.0"
                xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
                xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/2.0"
                xmlns:mrd="http://standards.iso.org/iso/19115/-3/mrd/1.0"
                version="2.0"
                exclude-result-prefixes="#all">

  <!--
  TODO: Problem with the xsl with {% %}, this xsl is replacing some of the host name to something else
  -->

  <xsl:param name="pot"/>
  <xsl:param name="thredds"/>
  <xsl:param name="geowebcache"/>
  <xsl:param name="geoserver"/>
  <xsl:param name="processes"/>

  <!-- default action is to copy -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <!--  Point of truth -->
  <!-- iso-19115 -->
  <xsl:template match="mdb:metadataLinkage/cit:CI_OnlineResource/cit:linkage/gco:CharacterString">
    <xsl:copy>
      <xsl:value-of select="if (not($pot = '')) then replace(text(), '//(.+?)/', concat('//',string($pot),'/')) else text()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Thredds -->
  <!-- iso-19115 -->
  <xsl:template match="/mdb:MD_Metadata/mdb:distributionInfo/mrd:MD_Distribution/mrd:transferOptions/mrd:MD_DigitalTransferOptions/mrd:onLine/cit:CI_OnlineResource/cit:linkage/gco:CharacterString[matches(text(), '//thredds(.*?)\.aodn\.org\.au/')]">
    <xsl:choose>
      <xsl:when test="not($thredds = '')">
        <xsl:value-of select="replace(text(), '//thredds(.*?)\.aodn\.org\.au/', concat('//',string($thredds),'/'))"/>
      </xsl:when>
      <xsl:when test="not('{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Thredds', {}).get('root_url', '') }}' = '')">
        <xsl:copy><xsl:value-of select="replace(., 'https?://thredds(.*?)\.aodn\.org\.au/', '{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Thredds', {}).get('root_url', '') }}/')"/></xsl:copy>
      </xsl:when>
      <xsl:when test="not('{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Thredds', '') }}' = '')" >
        <xsl:copy><xsl:value-of select="replace(., 'https?://thredds(.*?)\.aodn\.org\.au/', '{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Thredds', '') }}/')"/></xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="text()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Geoserver -->
  <!-- iso-19115 -->
  <xsl:template match="/mdb:MD_Metadata/mdb:distributionInfo/mrd:MD_Distribution/mrd:transferOptions/mrd:MD_DigitalTransferOptions/mrd:onLine/cit:CI_OnlineResource/cit:linkage/gco:CharacterString[matches(text(), '//geoserver(.*?)\.aodn\.org\.au/')]">
    <xsl:choose>
      <xsl:when test="not($geoserver = '')">
        <xsl:copy><xsl:value-of select="replace(text(), '//geoserver(.*?)\.aodn\.org\.au/', concat('//',string($geoserver),'/'))"/></xsl:copy>
      </xsl:when>
      <xsl:when test="not('{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geoserver', {}).get('root_url', '') }}' = '')">
        <xsl:copy><xsl:value-of select="replace(., 'https?://geoserver(.*?)\.aodn\.org\.au/', '{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geoserver', {}).get('root_url', '') }}/')"/></xsl:copy>
      </xsl:when>
      <xsl:when test="not('{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geoserver', '') }}' = '')" >
        <xsl:copy><xsl:value-of select="replace(., 'https?://geoserver(.*?)\.aodn\.org\.au/', '{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geoserver', '') }}/')"/></xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="text()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- GeoWebcache -->
  <!-- iso-19115 -->
  {% if  config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geowebcache') or config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geowebcache') -%}
  <xsl:template match="cit:CI_OnlineResource/cit:linkage/gco:CharacterString[../../cit:protocol/*/text()='OGC:WMS-1.1.1-http-get-map']">
    <xsl:choose>
      <xsl:when test="not($geowebcache = '')">
        <xsl:copy><xsl:value-of select="replace(., concat('//geoserver(.*?)\.aodn\.org\.au/','geoserver'), concat($geowebcache, '/geowebcache/service'))"/></xsl:copy>
      </xsl:when>
      <xsl:when test="not('{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geowebcache', {}).get('root_url', '') }}' = '')">
        <xsl:copy><xsl:value-of select="replace(., concat('https?://geoserver(.*?)\.aodn\.org\.au/', 'geoserver'), '{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geowebcache', {}).get('root_url', '') }}/geowebcache/service')"/></xsl:copy>
      </xsl:when>
      <xsl:when test="not('{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geowebcache', '') }}' = '')" >
        <xsl:copy><xsl:value-of select="replace(., concat('https?://geoserver(.*?)\.aodn\.org\.au/', 'geoserver'), '{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('Geowebcache', '') }}/geowebcache/service')"/></xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="text()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  {%- endif %}

  <!-- Processes -->
  <!-- iso-19115 -->
  <xsl:template match="/mdb:MD_Metadata/mdb:distributionInfo/mrd:MD_Distribution/mrd:transferOptions/mrd:MD_DigitalTransferOptions/mrd:onLine/cit:CI_OnlineResource/cit:linkage/gco:CharacterString[matches(text(), '//processes(.*?)\.aodn\.org\.au/')]">
    <xsl:choose>
      <xsl:when test="not($processes = '')">
        <xsl:value-of select="replace(text(), '//processes(.*?)\.aodn\.org\.au/', concat('//',string($processes),'/'))"/>
      </xsl:when>
      <xsl:when test="not('{{ config.get('stack_manifest', {}).get('Resources', {}).get('awswps', {}).get('endpoint', '') }}' = '')">
        <xsl:copy><xsl:value-of select="replace(., '//processes(.*?)\.aodn\.org\.au/', '//{{ config.get('stack_manifest', {}).get('Resources', {}).get('awswps', {}).get('endpoint', '') }}/')"/></xsl:copy>
      </xsl:when>
      <xsl:when test="not('{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('awswps', '') }}' = '')">
        <xsl:copy><xsl:value-of select="replace(., '//processes(.*?)\.aodn\.org\.au/', '//{{ config.get('stack_manifest', {}).get('ElasticBeanstalkResources', {}).get('Geonetwork3', {}).get('profile_inputs', {}).get('awswps', '') }}/')"/></xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="text()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
