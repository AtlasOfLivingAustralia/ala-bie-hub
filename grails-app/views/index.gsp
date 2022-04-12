%{--
  - Copyright (C) 2022 Atlas of Living Australia
  - All Rights Reserved.
  -
  - The contents of this file are subject to the Mozilla Public
  - License Version 1.1 (the "License"); you may not use this file
  - except in compliance with the License. You may obtain a copy of
  - the License at http://www.mozilla.org/MPL/
  -
  - Software distributed under the License is distributed on an "AS
  - IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
  - implied. See the License for the specific language governing
  - rights and limitations under the License.
  --}%

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="breadcrumb" content="${ message(code: 'label.search') }"/>
    <meta name="layout" content="ala-main"/>
    <title><g:message code="page.index.title" args="${[ grailsApplication.config.skin.orgNameLong ]}"/></title>
    <asset:script type="text/javascript">
        // global var to pass GSP vars into JS file
        SEARCH_CONF = {
            bieWebServiceUrl: "${grailsApplication.config.bie.index.url}"
        }
    </asset:script>
    <asset:javascript src="autocomplete-configuration.js"/>
</head>
<body class="page-search">

<section class="container">

    <header class="pg-header">
        <h1><g:message code="page.index.heading" args="${[ grailsApplication.config.skin.orgNameLong ]}"/></h1>
    </header>

    <div class="section">
        <div class="row">
            <div class="col-lg-8">
                <form id="search-inpage" action="search" method="get" name="search-form">
                    <div class="input-group">
                        <input id="search" class="form-control ac_input general-search" name="q" type="text" placeholder="${ message(code: 'label.searchAtlas') }" autocomplete="on">
                        <span class="input-group-btn">
                            <input type="submit" class="form-control btn btn-primary" alt="${ message(code: 'label.search') }" value="${ message(code: 'label.search') }">
                        </span>
                    </div>
                </form>
            </div>
        </div>
    </div>
</section><!--end .inner-->
</body>
</html>