<!DOCTYPE html>
<html>
	<head>
		<title><g:if env="development">Grails Runtime Exception</g:if><g:else>Error</g:else></title>
		<meta name="layout" content="${grailsApplication.config.skin.layout}">
		<g:if env="development"><asset:stylesheet src="errors.css"/></g:if>
	</head>
	<body>
	<h2>An error has occurred</h2>
	<ul class="errors">
			<li>${message}</li>
			<g:if test="${exception}">
				<li><g:renderException exception="${exception}" /></li>
			</g:if>
		</ul>
	<p>If this problem persists, please send an email to <a href="mailto:${grailsApplication.config.supportEmail ?: 'support@ala.org.au'}?subject=Reporting error on page: ${request.serverName}${request.forwardURI}">${grailsApplication.config.supportEmail ?: 'support@ala.org.au'}</a> and include the URL to this page.</p>
	</body>
</html>
