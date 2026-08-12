$(document).ready(function () {
    // autocomplete
    var bieBaseUrl = SEARCH_CONF.bieWebServiceUrl;
    var bieParams = { limit: 20 };
    var autoHints = SEARCH_CONF.autocompleteHints; // expects { fq: "kingdom:Plantae" }
    $.extend( bieParams, autoHints ); // merge autoHints into bieParams

    function getMatchingName(item) {
        if (item.scientificNameMatches && item.scientificNameMatches.length) {
            return item.name;
        } else if (item.commonNameMatches && item.commonNameMatches.length) {
            return item.commonName;
        } else {
            return item.name;
        }
    };

    function formatAutocompleteList(list) {
        var results = [];
        if (list && list.length){
            list.forEach(function (item) {
                var name = getMatchingName(item);
                results.push({label: name, value: name});
            })
        }

        return results;
    };

    // Use the local autocomplete proxy when the page origin differs from the
    // BIE web service origin. This avoids CORS errors when running on localhost
    // or any other host not explicitly allowed by the web service.
    function getAutocompleteUrl() {
        var serviceOrigin;
        try {
            serviceOrigin = new URL(bieBaseUrl).origin;
        } catch (e) {
            serviceOrigin = '';
        }
        if (window.location.origin !== serviceOrigin) {
            return '/search/auto.json';
        }
        return bieBaseUrl + '/search/auto';
    }

    $.ui.autocomplete({
        source: function (request, response) {
            bieParams.q = request.term;
            $.ajax( {
                url: getAutocompleteUrl(),
                dataType: "json",
                data: bieParams,
                success: function( data ) {
                    response( formatAutocompleteList(data.autoCompleteList) );
                }
            } );
        }
    }, $(":input#autocompleteResultPage, :input#search"));
});