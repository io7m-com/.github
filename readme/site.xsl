<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:c="urn:com.io7m.catalog:1"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                exclude-result-prefixes="#all"
                xmlns="http://www.w3.org/1999/xhtml"
                version="2.0">

  <xsl:output method="text"/>

  <xsl:template match="c:Works">
    <xsl:text>| |Project|Description|&#x000a;</xsl:text>
    <xsl:text>|-|-|-|&#x000a;</xsl:text>

    <xsl:for-each select=".//c:Work">
      <xsl:sort select="@Name"/>
      <xsl:if test="c:GroupAssignment[@Name='com.io7m.core']">
        <xsl:variable name="Description" select="dc:description"/>
        <xsl:variable name="SCM" select="c:Meta[@Name='scm_source']"/>
        <xsl:value-of select="concat('|![',@Name,'](',@Name,'.png)|[',@Name,'](',$SCM,')|',$Description,'|')"/><xsl:text>&#x000a;</xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>&#x000a;</xsl:text>
  </xsl:template>

  <xsl:template match="c:Works" mode="shell">
    <xsl:text>#!/bin/sh&#x000a;</xsl:text>
    <xsl:text>&#x000a;</xsl:text>

    <xsl:for-each select=".//c:Work">
      <xsl:sort select="@Name"/>
      <xsl:if test="c:GroupAssignment[@Name='com.io7m.core']">
        <xsl:value-of select="concat('cp ${BRANDING_HOME}/output/',@Name,'/icon32.png ',@Name,'.png')"/><xsl:text>&#x000a;</xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="c:Catalog">
    <xsl:result-document href="table.md"
                         indent="no"
                         method="text">
      <xsl:apply-templates select="c:Works"/>
    </xsl:result-document>

    <xsl:result-document href="table-icon-copy.sh"
                         indent="no"
                         method="text">
      <xsl:apply-templates select="c:Works" mode="shell"/>
    </xsl:result-document>
  </xsl:template>

</xsl:stylesheet>