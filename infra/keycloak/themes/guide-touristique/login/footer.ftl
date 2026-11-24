<#macro content>
  <#if client?? && client.baseUrl?has_content>
    <a class="brand-return" href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a>
  </#if>
</#macro>
