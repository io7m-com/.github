<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:si="urn:com.io7m.site:1.0"
                exclude-result-prefixes="#all"
                xmlns="http://www.w3.org/1999/xhtml"
                version="2.0">

  <xsl:output method="text"/>

  <xsl:template match="si:Software">
    <xsl:text>| |Project|Description|&#x000a;</xsl:text>
    <xsl:text>|-|-|-|&#x000a;</xsl:text>
    <xsl:for-each select="si:Package[@status='ACTIVE']">
      <xsl:sort select="@name"/>
      <xsl:if test="not(@external)">
        <xsl:value-of select="concat('|![',@name,'](profile/',@name,'.png)|[',@name,'](',@site,')|',@description,'|')"/><xsl:text>&#x000a;</xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>&#x000a;</xsl:text>
  </xsl:template>

  <xsl:template match="si:Software" mode="shell">
    <xsl:text>#!/bin/sh&#x000a;</xsl:text>
    <xsl:text>&#x000a;</xsl:text>

    <xsl:for-each select="si:Package[@status='ACTIVE']">
      <xsl:sort select="@name"/>
      <xsl:if test="not(@external)">
        <xsl:value-of select="concat('cp ${BRANDING_HOME}/output/',@name,'/icon32.png ',@name,'.png')"/><xsl:text>&#x000a;</xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="si:Software" mode="checklist">
    <xsl:for-each select="si:Package[@status='ACTIVE']">
      <xsl:sort select="@name"/>
      <xsl:if test="not(@external)">
        <xsl:value-of select="concat('- [ ] ',@name)"/><xsl:text>&#x000a;</xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="si:Site">
    <xsl:result-document href="table.md"
                         indent="no"
                         method="text">
      <xsl:apply-templates select="si:Software"/>
    </xsl:result-document>

    <xsl:result-document href="table-icon-copy.sh"
                         indent="no"
                         method="text">
      <xsl:apply-templates select="si:Software" mode="shell"/>
    </xsl:result-document>

    <xsl:result-document href="checklist.txt"
                         indent="no"
                         method="text">
      <xsl:apply-templates select="si:Software" mode="checklist"/>
    </xsl:result-document>
  </xsl:template>

</xsl:stylesheet>