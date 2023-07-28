<ul>
    <li><a href="${jsonLink}">JSON (data interchange format)</a></li>
    <li><a href="http://www.gbif.org/species/search?q=${tc?.taxonConcept?.nameString}">GBIF</a></li>
    <li><a href="http://www.biodiversitylibrary.org/search?searchTerm=${tc?.taxonConcept?.nameString}#/names">Biodiversity Heritage Library</a></li>
    <li><a href="${grailsApplication.config.literature.trove.url}/result?q=%22${synonyms?.join('%22+OR+%22')}%22" target="_trove"><g:message code="bhl.title.trove"/></a></li>
</ul>
