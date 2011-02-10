<%@ page errorPage="../../ErrorPage.jsp" %>
<jsp:include page="../../AdminHeader.jsp" />

<jsp:useBean id="solrserver" scope="session" class="fr.paris.lutece.plugins.solrserver.web.SolrserverJspBean" />
<% 
solrserver.init( request, fr.paris.lutece.plugins.solrserver.web.SolrserverJspBean.RIGHT_MANAGE_SOLRSERVER);
%>
<%= solrserver.getForm( request ) %>


<%@ include file="../../AdminFooter.jsp" %>