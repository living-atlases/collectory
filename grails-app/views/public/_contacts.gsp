<g:if test="${it?.size() > 0}">
  <section class="public-metadata">
    <h4><g:message code="public.show.contacts.contact" default="Contact"/></h4>
    <g:each in="${it}" var="cf">
      <g:set var="primaryName" value="${cf?.contact?.buildName()}"/>
      <div class="contact">
        <p>
            <span class="contact-name contact-name-highlight">${primaryName}</span><br/>
            <g:if test="${cf?.role}"><span class="contact-role">${cf?.role}</span><br/></g:if>
            <g:if test="${cf?.contact?.positionName && cf?.contact?.positionName != primaryName}"><span class="contact-position">${cf?.contact?.positionName}</span><br/></g:if>
            <g:if test="${cf?.contact?.organizationName && cf?.contact?.organizationName != primaryName}"><span class="contact-organization">${cf?.contact?.organizationName}</span><br/></g:if>
            <g:if test="${cf?.contact?.phone}"><span class="contact-phone"><g:message code="public.show.contacts.phone" default="Phone"/>: ${cf?.contact?.phone}</span><br/></g:if>
            <g:if test="${cf?.contact?.fax}"><g:message code="public.show.contacts.phone" default="phone"/>: ${cf?.contact?.fax}<br/></g:if>
            <g:if test="${cf?.contact?.userId}"><span class="contact-id">ID: <a href="${cf?.contact?.userId}" class="user_id_link" target="_blank">${cf?.contact?.userId}</a></span><br/></g:if>
            <cl:emailLink email="${cf?.contact?.email}"><g:message code="public.show.contacts.email" default="email this contact"/> </cl:emailLink>
        </p>
      </div>
    </g:each>
  </section>
</g:if>
