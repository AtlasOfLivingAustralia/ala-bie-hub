<section class="tab-pane fade" id="ausTraits">

    <div id="austraits-logo">
        <img src="${resource(dir: "images", file: "austraits_logo.png")}" height="100" alt="Austraits Logo"/>
        <p> <g:message code="aus.traits.whatis"/></p>
    </div>

    <div id="austraits-summary" class="collapse" aria-expanded="false">
        <p> <g:message code="aus.traits.summary.caption"/></p>
    </div>
    <a  id="austraits-summary-toggle" style="cursor: pointer" onclick="toggleTraitsSummary()">See More</a>

    <hr>

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
                <th class="centered-cell"><g:message code="aus.traits.categorical.definition"/></th>
            </tr>
            </thead>
            <tbody></tbody>
        </table>

        <h2><g:message code="aus.traits.numeric.heading"/></h2>
        <table id="numeric-traits" class="table name-table  table-responsive">
            <thead>
            <tr>
                <th><g:message code="aus.traits.numeric.name"/></th>
                <th class="centered-cell"><g:message code="aus.traits.numeric.min"/></th>
                <th class="centered-cell"><g:message code="aus.traits.numeric.mean"/></th>
                <th class="centered-cell"><g:message code="aus.traits.numeric.max"/></th>
                <th class="centered-cell"><g:message code="aus.traits.numeric.unit"/></th>
                <th class="centered-cell"><g:message code="aus.traits.numeric.definition"/></th>
            </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>

    <hr>
    <div class="panel panel-default">
        <div class="panel-footer" style="border-radius:3px;border-top:none;">
            <p class="source">Source: <a href="${grailsApplication.config.ausTraits.sourceURL}" class="sourceText">Zenodo</a></p>
            <p class="rights">Rights holder: <span class="rightsText">AusTraits</span></p>
            <p class="provider">Provided by: <a href="${grailsApplication.config.ausTraits.homeURL}" class="providedBy">AusTraits</a></p>
            <h4>How to cite AustTraits data</h4>
            <p>
                <span> Falster, Gallagher et al (2021) AusTraits, a curated plant trait database for the Australian flora. Scientific Data 8: 254, <a href="https://doi.org/10.1038/s41597-021-01006-6" target="_blank" class="providedBy">https://doi.org/10.1038/s41597-021-01006-6</a>
                </span>  - followed by the ALA url and access date<br>
                For more information about citing information on the ALA, see - <a target="_blank" href="${grailsApplication.config.alaCitingURL}"> Citing the ALA</a>
            </p>
        </div>

    </div>



</section>