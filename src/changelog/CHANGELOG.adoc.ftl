<#--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<#function isCommitter apacheId>
  <#switch apacheId>
    <#case "ckozak">
    <#case "ggregory">
    <#case "grobmeier">
    <#case "mattsicker">
    <#case "pkarwasz">
    <#case "rgoers">
    <#case "rpopma">
    <#case "vy">
      <#return true>
    <#default>
      <#return false>
  </#switch>
</#function>
<#function getGithubId apacheId>
  <#switch apacheId>
    <#case "ckozak">
      <#return "carterkozak">
    <#case "ggregory">
      <#return "garydgregory">
    <#case "mattsicker">
      <#return "jvz">
    <#case "pkarwasz">
      <#return "ppkarwasz">
    <#case "rpopma">
      <#return "remkop">
    <#default>
      <#return apacheId>
  </#switch>
</#function>
<#if entriesByType?size gt 0>= ${release.version}<#if release.date?has_content> (${release.date})</#if>
<#list entriesByType as entryType, entries>

== ${entryType?capitalize}

<#list entries as entry>
<@compress single_line=true>
  * <#list entry.issues as issue>
    ${issue.link}[${issue.id}]${issue?has_next?string(", ", ":")}
    </#list>
  ${entry.description.text?ensure_ends_with(".")}
  <#assign first = true>
  <#assign committer = "">
  <#list entry.authors as author>
    <#if isCommitter(author.id!"")>
      <#assign committer = author.id>
    <#else>
      ${first?string("Thanks to ", ", ")}
      <#if author.name?has_content>
        <#if author.id?has_content>
          https://github.com/${author.id}[${author.name}].
        <#else>
          ${author.name}.
        </#if>
      <#else>
        https://github.com/${author.id}[${author.id}].
      </#if>
    </#if>
  </#list>
  <#if committer?has_content>
    (https://github.com/${getGithubId(committer)}[${committer}])
  </#if>
</@compress>

</#list>
</#list>
</#if>
