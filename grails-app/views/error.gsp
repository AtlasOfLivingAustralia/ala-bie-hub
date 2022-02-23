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
	<p>If this problem persists, please send an email to <a href="mailto:support@ala.org.au?subject=Error page for: ${grailsApplication.config.serverName}${request.forwardURI}">support@ala.org.au</a>.</p>
	</body>
</html>
