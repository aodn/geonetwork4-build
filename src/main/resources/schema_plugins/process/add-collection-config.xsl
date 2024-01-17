<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:gmd="http://www.isotc211.org/2005/gmd"
                xmlns:gco="http://www.isotc211.org/2005/gco"
                xmlns:gmx="http://www.isotc211.org/2005/gmx"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="2.0">

    <!--
        You need to register this in the schema_plugins/<plugin_id>/suggest.xml, otherwise it will not be called
        the suggest.xml didn't registered this, so why we need it in the cloud-deploy-config
    -->
    <xsl:output indent="yes"/>

    <!-- Parameters passed to the stylesheet -->

    <xsl:param name="config-dir"/>     <!-- location of collection-config files (url or local directory path) -->

    <!-- Read portal config for metadata -->

    <xsl:variable name="metadata-uuid" select="//gmd:fileIdentifier/*/text()"/>
    <xsl:variable name="collection-config-url" select="concat($config-dir, '/', $metadata-uuid, '.xml')"/>

    <xsl:variable name="collection-config" select="document($collection-config-url)/collection-config"/>
    <xsl:variable name="geonetwork-url" select="concat(environment-variable('GEONETWORK_PROTOCOL'),'://',environment-variable('GEONETWORK_HOST'))"/>

    <!-- Abort if we don't have any portal config so record isn't harvested -->
    <xsl:message>Processing <xsl:value-of select="$metadata-uuid"/> with add-collection-config.xsl</xsl:message>

    <xsl:template match="/">
        <xsl:if test="not($collection-config)">
            <xsl:message terminate="yes">Error: no portal config loaded for <xsl:value-of select="$metadata-uuid"/></xsl:message>
        </xsl:if>
        <xsl:copy>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Default rule - copy, applying templates to attributes/child nodes -->

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Add parameters/platforms as keywords in data identification section -->

    <xsl:template match="gmd:MD_DataIdentification" >
        <xsl:copy>
            <xsl:apply-templates select="gmd:citation"/>
            <xsl:apply-templates select="gmd:abstract"/>
            <xsl:apply-templates select="gmd:purpose"/>
            <xsl:apply-templates select="gmd:credit"/>
            <xsl:apply-templates select="gmd:status"/>
            <xsl:apply-templates select="gmd:pointOfContact"/>
            <xsl:apply-templates select="gmd:resourceMaintenance"/>
            <xsl:apply-templates select="gmd:graphicOverview"/>
            <xsl:apply-templates select="gmd:resourceFormat"/>
            <xsl:apply-templates select="gmd:descriptiveKeywords"/>

            <xsl:call-template name="add-descriptive-keywords">
                <xsl:with-param name="thesaurus-title">AODN Discovery Parameter Vocabulary</xsl:with-param>
                <xsl:with-param name="thesaurus-uri">http://vocab.aodn.org.au/def/discovery_parameter/1</xsl:with-param>
                <xsl:with-param name="vocab-terms" select="$collection-config/parameter"/>
                <xsl:with-param name="vocab-uri" select="concat($geonetwork-url, '/geonetwork/srv/eng/thesaurus.download?ref=external.theme.aodn_aodn-discovery-parameter-vocabulary')"/>
                <xsl:with-param name="vocab-name">geonetwork.thesaurus.external.theme.aodn_aodn-discovery-parameter-vocabulary</xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="add-descriptive-keywords">
                <xsl:with-param name="thesaurus-title">AODN Platform Vocabulary</xsl:with-param>
                <xsl:with-param name="thesaurus-uri">http://vocab.aodn.org.au/def/platform/1</xsl:with-param>
                <xsl:with-param name="vocab-terms" select="$collection-config/platform"/>
                <xsl:with-param name="vocab-uri" select="concat($geonetwork-url, '/geonetwork/srv/eng/thesaurus.download?ref=external.theme.aodn_aodn-platform-vocabulary')" />
                <xsl:with-param name="vocab-name">geonetwork.thesaurus.external.theme.aodn_aodn-platform-vocabulary</xsl:with-param>
            </xsl:call-template>

            <xsl:apply-templates select="gmd:resourceSpecificUsage"/>
            <xsl:apply-templates select="gmd:resourceConstraints"/>
            <xsl:apply-templates select="gmd:aggregationInfo"/>
            <xsl:apply-templates select="gmd:spatialRepresentationType"/>
            <xsl:apply-templates select="gmd:spatialResolution"/>
            <xsl:apply-templates select="gmd:language"/>
            <xsl:apply-templates select="gmd:characterSet"/>
            <xsl:apply-templates select="gmd:topicCategory"/>
            <xsl:apply-templates select="gmd:environmentDescription"/>
            <xsl:apply-templates select="gmd:extent"/>
            <xsl:apply-templates select="gmd:supplementalInformation"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="add-descriptive-keywords">
        <xsl:param name="thesaurus-title"/>
        <xsl:param name="thesaurus-uri"/>
        <xsl:param name="vocab-terms"/>
        <xsl:param name="vocab-uri"/>
        <xsl:param name="vocab-name"/>

        <xsl:if test="$vocab-terms">
            <gmd:descriptiveKeywords>
                <gmd:MD_Keywords>
                    <xsl:for-each select="$vocab-terms">
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="{uri/text()}"><xsl:value-of select="label/text()"/></gmx:Anchor>
                        </gmd:keyword>
                    </xsl:for-each>
                    <gmd:thesaurusName>
                        <gmd:CI_Citation>
                            <gmd:title>
                                <gmx:Anchor xlink:href="{$thesaurus-uri}"><xsl:value-of select="$thesaurus-title"/></gmx:Anchor>
                            </gmd:title>
                            <xsl:if test="$vocab-uri">
                                <gmd:identifier>
                                    <gmd:MD_Identifier>
                                        <gmd:code>
                                            <gmx:Anchor xlink:href="{$vocab-uri}"><xsl:value-of select="$vocab-name"/></gmx:Anchor>
                                        </gmd:code>
                                    </gmd:MD_Identifier>
                                </gmd:identifier>
                            </xsl:if>
                        </gmd:CI_Citation>
                    </gmd:thesaurusName>
                </gmd:MD_Keywords>
            </gmd:descriptiveKeywords>
        </xsl:if>
    </xsl:template>

    <!-- Add services as online resources in a transferOptions element in the MD_Distribution section -->

    <xsl:template match="gmd:MD_Distribution">
        <xsl:copy>
            <xsl:apply-templates select="gmd:distributionFormat"/>
            <xsl:apply-templates select="gmd:distributor"/>
            <xsl:apply-templates select="gmd:transferOptions"/>

            <xsl:call-template name="add-transfer-options">
                <xsl:with-param name="services" select="$collection-config/service"/>
            </xsl:call-template>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="add-transfer-options">
        <xsl:param name="services"/>

        <xsl:if test="$services">
            <gmd:transferOptions>
                <gmd:MD_DigitalTransferOptions>
                    <xsl:for-each select="$services">
                        <gmd:onLine>
                            <gmd:CI_OnlineResource>
                                <gmd:linkage>
                                    <gmd:URL><xsl:value-of select="url/text()"/></gmd:URL>
                                </gmd:linkage>
                                <xsl:if test="name">
                                    <gmd:name>
                                        <gco:CharacterString><xsl:value-of select="name/text()"/></gco:CharacterString>
                                    </gmd:name>
                                </xsl:if>
                                <gmd:protocol>
                                    <gco:CharacterString><xsl:value-of select="protocol/text()"/></gco:CharacterString>
                                </gmd:protocol>
                                <xsl:if test="description">
                                    <gmd:description>
                                        <gco:CharacterString><xsl:value-of select="description/text()"/></gco:CharacterString>
                                    </gmd:description>
                                </xsl:if>
                            </gmd:CI_OnlineResource>
                        </gmd:onLine>
                    </xsl:for-each>
                </gmd:MD_DigitalTransferOptions>
            </gmd:transferOptions>
        </xsl:if>
    </xsl:template>

    <!-- Add Organisations as Cited Responsible Parties - for demo only - the metadata should be fixed! -->

    <xsl:template match="gmd:MD_DataIdentification/*/gmd:CI_Citation">
        <xsl:copy>
            <xsl:apply-templates select="gmd:title"/>
            <xsl:apply-templates select="gmd:alternateTitle"/>
            <xsl:apply-templates select="gmd:date"/>
            <xsl:apply-templates select="gmd:edition"/>
            <xsl:apply-templates select="gmd:editionDate"/>
            <xsl:apply-templates select="gmd:identifier"/>
            <xsl:apply-templates select="gmd:citedResponsibleParty"/>

            <xsl:call-template name="add-cited-reponsible-parties"/>

            <xsl:apply-templates select="gmd:presentationForm"/>
            <xsl:apply-templates select="gmd:series"/>
            <xsl:apply-templates select="gmd:otherCitationDetails"/>
            <xsl:apply-templates select="gmd:collectiveTitle"/>
            <xsl:apply-templates select="gmd:ISBN"/>
            <xsl:apply-templates select="gmd:ISSN"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="add-cited-reponsible-parties">
        <xsl:for-each select="$collection-config/organisation/text()">
            <gmd:citedResponsibleParty>
                <gmd:CI_ResponsibleParty>
                    <gmd:organisationName>
                        <gco:CharacterString><xsl:value-of select="."/></gco:CharacterString>
                    </gmd:organisationName>
                    <gmd:role gco:nilReason="missing"/>
                </gmd:CI_ResponsibleParty>
            </gmd:citedResponsibleParty>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
