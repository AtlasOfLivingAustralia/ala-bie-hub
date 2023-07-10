<g:set var="orgNameLong" value="${grailsApplication.config.skin.orgNameLong}"/>
<g:set var="orgNameShort" value="${grailsApplication.config.skin.orgNameShort}"/>
<g:applyLayout name="ala-main">
    <head>
        <title><g:layoutTitle/></title>
        <meta name="breadcrumb" content="${pageProperty(name: 'meta.breadcrumb', default: pageProperty(name: 'title').split('\\|')[0].decodeHTML())}"/>
        <meta name="breadcrumbParent" content="${pageProperty(name: 'meta.breadcrumbParent', default: "${createLink(uri: '/')},${message(code: 'page.index.heading', args: [orgNameShort])}")}"/>

        <script type="text/javascript">
            var BIE_VARS = { "autocompleteUrl" : "${grailsApplication.config.bie.index.url}/search/auto"}
        </script>
        <g:layoutHead/>
    </head>
    <body>
        <g:layoutBody />
    </body>
</g:applyLayout>
