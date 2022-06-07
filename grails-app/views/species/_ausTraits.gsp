<section class="tab-pane fade" id="ausTraits">
    <div id="download-button-area" class="pull-right">
        <a href="${ausTraitsDownloadUrl}"
           id="downloadRecords" class="btn btn-default"><i class="fa fa-download"></i>
            <g:message code="aus.traits.download.records"/>
        </a>
    </div>

    <div id="traitsRecords" class="result-list">
        <h2><g:message code="aus.traits.categorical.heading"/></h2>
        <table id="categorical-traits" class="table name-table  table-responsive">
            <thead>
            <tr>
                <th><g:message code="aus.traits.categorical.name"/></th>
                <th><g:message code="aus.traits.categorical.value"/></th>
            </tr>
            </thead>
            <tbody></tbody>
        </table>

        <h2><g:message code="aus.traits.numeric.heading"/></h2>
        <table id="numeric-traits" class="table name-table  table-responsive">
            <thead>
            <tr>
                <th><g:message code="aus.traits.numeric.name"/></th>
                <th><g:message code="aus.traits.numeric.mean.type"/></th>
                <th><g:message code="aus.traits.numeric.mean"/></th>
                <th><g:message code="aus.traits.numeric.unit"/></th>
                <th><g:message code="aus.traits.numeric.nsites"/></th>
                <th><g:message code="aus.traits.numeric.ndatasets"/></th>
            </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>

    <div class="panel panel-default">
        <div class="panel-footer">
            <p class="source">Source: <a href="${grailsApplication.config.ausTraits.sourceURL}" class="sourceText">Zenodo</a></p>
            <p class="rights">Rights holder: <span class="rightsText">AusTraits</span></p>
            <p class="provider">Provided by: <a href="${grailsApplication.config.ausTraits.homeURL}" class="providedBy">AusTraits</a></p>
        </div>
    </div>

</section>