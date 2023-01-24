<section class="tab-pane fade" id="ausTraits">
    <div id="download-button-area" class="pull-right btn-group btn-group-vertical">
        <a href="${ausTraitsDownloadUrl}"
           id="downloadRecords" class="btn btn-default"><i class="glyphicon glyphicon-arrow-down"></i>
            <g:message code="aus.traits.download.records"/>
        </a>
        <a class="btn btn-default" style="text-align:left;" target="_blank"
           href="${grailsApplication.config.ausTraits.definitionsURL}">
            <g:message code="aus.traits.definitions"/>
        </a>
    </div>

    <div id="traitsRecords" class="result-list">
        <div id="traits-description"></div>
        <h2><g:message code="aus.traits.categorical.heading"/></h2>
        <span><i><g:message code="aus.traits.categorical.conflict_disclaimer"/></i></span>
        <table id="categorical-traits" class="table name-table  table-responsive">
            <thead>
            <tr>
                <th><g:message code="aus.traits.categorical.name"/></th>
                <th><g:message code="aus.traits.categorical.value"/></th>
                <th><g:message code="aus.traits.categorical.definition"/></th>
            </tr>
            </thead>
            <tbody></tbody>
        </table>

        <h2><g:message code="aus.traits.numeric.heading"/></h2>
        <table id="numeric-traits" class="table name-table  table-responsive">
            <thead>
            <tr>
                <th><g:message code="aus.traits.numeric.name"/></th>
                <th><g:message code="aus.traits.numeric.min"/></th>
                <th><g:message code="aus.traits.numeric.mean"/></th>
                <th><g:message code="aus.traits.numeric.max"/></th>
                <th><g:message code="aus.traits.numeric.unit"/></th>
                <th><g:message code="aus.traits.numeric.definition"/></th>
            </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>

    <div class="panel panel-default">
        <div class="panel-footer" style="border-radius:3px;border-top:none;">
            <p class="source">Source: <a href="${grailsApplication.config.ausTraits.sourceURL}" class="sourceText">Zenodo</a></p>
            <p class="rights">Rights holder: <span class="rightsText">AusTraits</span></p>
            <p class="provider">Provided by: <a href="${grailsApplication.config.ausTraits.homeURL}" class="providedBy">AusTraits</a></p>
        </div>
    </div>

</section>