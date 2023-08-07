/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

function showSpeciesPage(traitsTabSet) {
    //console.log("Starting show species page");

    //load content
    loadOverviewImages();
    loadMap();
    loadGalleries();
    loadExpertDistroMap();
    loadExternalSources();
    loadSpeciesLists();
    loadDataProviders();
    loadIndigenousData();
    //
    ////setup controls
    addAlerts();
    loadBhl();
    if (traitsTabSet && SHOW_CONF.kingdom == 'Plantae') {
        loadAusTraits();
    }
}

function loadSpeciesLists() {

    //console.log('### loadSpeciesLists #### ' + SHOW_CONF.speciesListServiceUrl + '/ws/species/' + SHOW_CONF.guid);
    $.getJSON(SHOW_CONF.speciesListServiceUrl + '/ws/species/' + SHOW_CONF.guid + '?isBIE=true', function (data) {
        for (var i = 0; i < data.length; i++) {
            var specieslist = data[i];
            var maxListFields = 20;

            if (specieslist.list.isBIE) {
                var $description = $('#descriptionTemplate').clone();
                $description.css({'display': 'block'});
                $description.attr('id', '#specieslist-block-' + specieslist.dataResourceUid);
                $description.addClass('species-list-block');
                $description.find(".title").html(specieslist.list.listName);

                if (specieslist.kvpValues.length > 0) {
                    var content = "<table class='table specieslist-table'>";
                    $.each(specieslist.kvpValues, function (idx, kvpValue) {
                        if (idx >= maxListFields) {
                            return false;
                        }
                        var value = kvpValue.value;
                        if (kvpValue.vocabValue) {
                            value = kvpValue.vocabValue;
                        }
                        content += "<tr><td>" + (kvpValue.key + "</td><td>" + value + "</td></tr>");
                    });
                    content += "</table>";
                    $description.find(".content").html(content);
                } else {
                    $description.find(".content").html("A species list provided by " + specieslist.list.listName);
                }

                $description.find(".source").css({'display': 'none'});
                $description.find(".rights").css({'display': 'none'});

                $description.find(".providedBy").attr('href', SHOW_CONF.speciesListUrl + '/speciesListItem/list/' + specieslist.dataResourceUid);
                $description.find(".providedBy").html(specieslist.list.listName);

                $description.appendTo('#listContent');
            }
        }
    });
}

function addAlerts() {
    // alerts button
    $("#alertsButton").click(function (e) {
        e.preventDefault();
        var query = "Species: " + SHOW_CONF.scientificName;
        var searchString = "?q=lsid:" + SHOW_CONF.guid;
        var url = SHOW_CONF.alertsUrl + "/webservice/createBiocacheNewRecordsAlert?";
        url += "queryDisplayName=" + encodeURIComponent(query);
        url += "&baseUrlForWS=" + encodeURIComponent(SHOW_CONF.biocacheServiceUrl);
        url += "&baseUrlForUI=" + encodeURIComponent(SHOW_CONF.biocacheUrl);
        url += "&webserviceQuery=%2Foccurrences%2Fsearch" + encodeURIComponent(searchString);
        url += "&uiQuery=%2Foccurrences%2Fsearch" + encodeURIComponent(searchString);
        url += "&resourceName=" + encodeURIComponent("Atlas");
        window.location.href = url;
    });
}

function loadMap() {

    if (SHOW_CONF.map != null) {
        return;
    }

    //add an occurrence layer for this taxon
    var taxonLayer = L.tileLayer.wms(SHOW_CONF.biocacheServiceUrl + "/mapping/wms/reflect?q=lsid:" +
        SHOW_CONF.guid + (SHOW_CONF.qualityProfile ? "&qualityProfile=" + SHOW_CONF.qualityProfile : "")
        + "&qc=" + SHOW_CONF.mapQueryContext + SHOW_CONF.additionalMapFilter
        , {
            layers: 'ALA:occurrences',
            format: 'image/png',
            transparent: true,
            attribution: SHOW_CONF.mapAttribution,
            bgcolor: "0x000000",
            outline: SHOW_CONF.mapOutline,
            ENV: SHOW_CONF.mapEnvOptions
        });

    var speciesLayers = new L.LayerGroup();
    taxonLayer.addTo(speciesLayers);

    SHOW_CONF.map = L.map('leafletMap', {
        center: [SHOW_CONF.defaultDecimalLatitude, SHOW_CONF.defaultDecimalLongitude],
        zoom: SHOW_CONF.defaultZoomLevel,
        layers: [speciesLayers],
        scrollWheelZoom: false
    });

    var defaultBaseLayer = L.tileLayer(SHOW_CONF.defaultMapUrl, {
        attribution: SHOW_CONF.defaultMapAttr,
        subdomains: SHOW_CONF.defaultMapDomain,
        mapid: SHOW_CONF.defaultMapId,
        token: SHOW_CONF.defaultMapToken
    });

    defaultBaseLayer.addTo(SHOW_CONF.map);
    L.control.scale({imperial: false, position: 'bottomright'}).addTo(SHOW_CONF.map);

    var baseLayers = {
        "Base layer": defaultBaseLayer
    };

    var sciName = SHOW_CONF.scientificName;

    var overlays = {};
    overlays[sciName] = taxonLayer;

    L.control.layers(baseLayers, overlays).addTo(SHOW_CONF.map);

    //SHOW_CONF.map.on('click', onMapClick);
    SHOW_CONF.map.invalidateSize(false);

    updateOccurrenceCount();
    fitMapToBounds();
}

/**
 * Update the total records count for the occurrence map in heading text
 */
function updateOccurrenceCount() {
    $.getJSON(SHOW_CONF.biocacheServiceUrl + '/occurrences/search?q=lsid:' + SHOW_CONF.guid + "&qualityProfile=" + SHOW_CONF.qualityProfile + "&fq=" + SHOW_CONF.mapQueryContext, function (data) {
        if (data) {
            if (data.totalRecords > 0) {
                $('.occurrenceRecordCount').html(data.totalRecords.toLocaleString());
            } else {
                // hide charts if no records
                $("#recordBreakdowns").html("<h3>" + jQuery.i18n.prop("no.records.found") + "</h3>");
            }
        }
    });
}

function fitMapToBounds() {
    var jsonUrl = SHOW_CONF.biocacheServiceUrl + "/mapping/bounds?q=lsid:" + SHOW_CONF.guid;
    $.getJSON(jsonUrl, function (data) {
        if (data.length == 4 && data[0] != 0 && data[1] != 0) {
            //console.log("data", data);
            var sw = L.latLng(data[1], data[0]);
            var ne = L.latLng(data[3], data[2]);
            //console.log("sw", sw.toString());
            var dataBounds = L.latLngBounds(sw, ne);
            //var centre = dataBounds.getCenter();
            var mapBounds = SHOW_CONF.map.getBounds();

            if (!mapBounds.contains(dataBounds) && !mapBounds.intersects(dataBounds)) {
                SHOW_CONF.map.fitBounds(dataBounds);
                if (SHOW_CONF.map.getZoom() > 3) {
                    SHOW_CONF.map.setZoom(3);
                }
            }

            SHOW_CONF.map.invalidateSize(true);
        }
    });
}

//function onMapClick(e) {
//    $.ajax({
//        url: SHOW_CONF.biocacheServiceUrl + "/occurrences/info",
//        jsonp: "callback",
//        dataType: "jsonp",
//        data: {
//            q: SHOW_CONF.scientificName,
//            zoom: "6",
//            lat: e.latlng.lat,
//            lon: e.latlng.lng,
//            radius: 20,
//            format: "json"
//        },
//        success: function (response) {
//            var popup = L.popup()
//                .setLatLng(e.latlng)
//                .setContent("Occurrences at this point: " + response.count)
//                .openOn(SHOW_CONF.map);
//        }
//    });
//}

function loadAusTraits() {
    $.ajax({url: SHOW_CONF.ausTraitsSummaryUrl}).done(function (data) {
        // handle if traits  controller returns an error
        if (data.error) {
            $("#traitsRecords").html("<p style='font-size: small'>" + jQuery.i18n.prop("no.traits.connection") + " You can find more infomation on AusTraits   <a target='_blank' href='" + SHOW_CONF.ausTraitsHomeUrl + "'>here</a>. </p>");
            $("#download-button-area").hide()
            $(".panel-footer").hide();
        } else if (data.numeric_traits && data.categorical_traits) {
            $.each(data.categorical_traits, function (idx, traitValue) {
                var tableRow = "<tr><td>";
                tableRow += traitValue.trait_name + "</td><td>"
                tableRow += traitValue.trait_values + "</td><td class='centered-cell'>"
                tableRow += "<a target='_blank' href=" + traitValue.definition + "> <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-box-arrow-up-right\" viewBox=\"0 0 16 16\">\n" +
                    "  <path fill-rule=\"evenodd\" d=\"M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5z\"/>\n" +
                    "  <path fill-rule=\"evenodd\" d=\"M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0v-5z\"/>\n" +
                    "</svg></a></td></tr>";

                $('#categorical-traits tbody').append(tableRow);
            });

            $.each(data.numeric_traits, function (idx, traitValue) {
                console.log(traitValue.min, traitValue.mean, traitValue.max)
                var tableRow = "<tr><td>";
                tableRow += traitValue.trait_name + "</td><td class='centered-cell'>"
                tableRow += (traitValue.min || " - ") + "</td><td class='centered-cell'>"
                tableRow += (traitValue.mean || " - ") + "</td><td class='centered-cell'>"
                tableRow += (traitValue.max || " - ") + "</td><td class='centered-cell'>"
                tableRow += traitValue.unit + "</td><td class='centered-cell'>"
                tableRow += "<a target='_blank' href=" + traitValue.definition + "> <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-box-arrow-up-right\" viewBox=\"0 0 16 16\">\n" +
                    "  <path fill-rule=\"evenodd\" d=\"M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5z\"/>\n" +
                    "  <path fill-rule=\"evenodd\" d=\"M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0v-5z\"/>\n" +
                    "</svg></a></td></tr>"
                $('#numeric-traits tbody').append(tableRow);
            });

        } else {
            $("#traitsRecords").html("<h3>" + jQuery.i18n.prop("no.traits.found") + "</h3>");
            $("#download-button-area").hide()
            $(".panel-footer").hide();
        }
        // apply table cell styling after content is loaded.
        $(".centered-cell").css({"text-align": "center"})

    }).error(function (jqXHR, textStatus, errorThrown) {
        console.warn("error " + textStatus);
        console.warn("incoming Text " + jqXHR.responseText);
    });

    $.ajax({url: SHOW_CONF.ausTraitsCountUrl}).done(function (data) {
        if (data[0] && data[0]["summary"] && data[0]["AusTraits"] && data[0]["taxon"]) {
            let htmlContent = "<span> There are " + data[0]["summary"] + " available for <span class='scientific-name rank-" + SHOW_CONF.rankString + "'><span class='name'>" + data[0]["taxon"] + "</span></span>  with data for " + data[0]["AusTraits"] +
                " further traits in the <a  target='_blank' href='" + SHOW_CONF.ausTraitsHomeUrl + "'>AusTraits</a> database. These are accessible via the download CSV button or alternatively the entire database can be accessed at  <a target='_blank' href='" + SHOW_CONF.ausTraitsSourceUrl + "'>" + SHOW_CONF.ausTraitsSourceUrl + "</a>. </span>"
            $('#traits-description').html(htmlContent);
        }
    }).error(function (jqXHR, textStatus, errorThrown) {
        console.warn("error " + textStatus);
        console.warn("incoming Text " + jqXHR.responseText);
    });
}

/**
 * Toggle the Austraits summary section and update the toggle action text accordingly
 */
function toggleTraitsSummary() {
    const summary = $('#austraits-summary');
    const summaryToggle = $('#austraits-summary-toggle');
    const expanded = summary.attr('aria-expanded')
    if (expanded === "true") {
        summary.collapse('hide');
        summaryToggle.text("See More")
    } else {
        summary.collapse('show');
        summaryToggle.text("See Less")
    }
}

function loadDataProviders() {

    var url = SHOW_CONF.biocacheServiceUrl +
        '/occurrences/search?q=lsid:' +
        SHOW_CONF.guid +
        '&pageSize=0&flimit=-1';

    if (SHOW_CONF.mapQueryContext) {
        url = url + '&fq=' + SHOW_CONF.mapQueryContext;
    }

    url = url + '&facet=on&facets=data_resource_uid';

    var uiUrl = SHOW_CONF.biocacheUrl +
        '/occurrences/search?q=lsid:' +
        SHOW_CONF.guid;

    $.getJSON(url, function (data) {

        if (data.totalRecords > 0) {

            var datasetCount = data.facetResults[0].fieldResult.length;

            //exclude the "Unknown facet value"
            if (data.facetResults[0].fieldResult[datasetCount - 1].label == "Unknown") {
                datasetCount = datasetCount - 1;
            }

            if (datasetCount == 1) {
                $('.datasetLabel').html("dataset has");
            }

            $('.datasetCount').html(datasetCount);
            $.each(data.facetResults[0].fieldResult, function (idx, facetValue) {
                if (facetValue.count > 0) {

                    var uid = facetValue.fq.replace(/data_resource_uid:/, '').replace(/[\\"]*/, '').replace(/[\\"]/, '');
                    var dataResourceUrl = SHOW_CONF.collectoryUrl + "/public/show/" + uid;
                    var tableRow = "<tr><td><a href='" + dataResourceUrl + "'><span class='data-provider-name'>" + facetValue.label + "</span></a>";

                    $.ajax({
                        url: SHOW_CONF.collectoryServiceUrl + "/ws/dataResource/" + uid,
                        dataType: 'json',
                        async: false,
                        success: function (collectoryData) {
                            if (collectoryData.provider) {
                                tableRow += "<br/><small><a href='" + SHOW_CONF.collectoryUrl + '/public/show/' + uid + "'>" + collectoryData.provider.name + "</a></small>";
                            }
                            tableRow += "</td>";
                            tableRow += "<td>" + collectoryData.licenseType + "</td>";

                            var queryUrl = uiUrl + "&fq=" + facetValue.fq;
                            tableRow += "</td><td><a href='" + queryUrl + "'><span class='record-count'>" + facetValue.count + "</span></a></td>"
                            tableRow += "</tr>";
                            $('#data-providers-list tbody').append(tableRow);
                        }
                    });
                }
            });
        }
    });
}

function loadIndigenousData() {

    if (!SHOW_CONF.profileServiceUrl || SHOW_CONF.profileServiceUrl == "") {
        return;
    }

    var url = SHOW_CONF.profileServiceUrl + "/api/v1/profiles?summary=true&tags=IEK&guids=" + SHOW_CONF.guid;
    $.getJSON(url, function (data) {
        if (data.total > 0) {
            $("#indigenous-info-tab").parent().removeClass("hide");

            $.each(data.profiles, function (index, profile) {
                var panel = $('#indigenous-profile-summary-template').clone();
                panel.removeClass("hide");
                panel.attr("id", profile.id);

                var logo = profile.collection.logo || SHOW_CONF.noImage100Url;
                panel.find(".collection-logo").append("<img src='" + logo + "' alt='" + profile.collection.title + " logo'>");
                panel.find(".collection-logo-caption").append(profile.collection.title);


                panel.find(".profile-name").append(profile.name);
                panel.find(".collection-name").append("(" + profile.collection.title + ")");
                var otherNames = "";
                var summary = "";
                $.each(profile.attributes, function (index, attribute) {
                    if (attribute.name) {
                        otherNames += attribute.text;
                        if (index < profile.attributes.length - 2) {
                            otherNames += ", ";
                        }
                    }
                    if (attribute.summary) {
                        summary = attribute.text;
                    }
                });
                panel.find(".other-names").append(otherNames);
                panel.find(".summary-text").append(summary);
                panel.find(".profile-link").append("<a href='" + profile.url + "' title='Click to view the whole profile' target='_blank'>View the full profile</a>");

                if (profile.thumbnailUrl) {
                    panel.find(".main-image").removeClass("hide");

                    panel.find(".image-embedded").append("<img src='" + profile.thumbnailUrl + "' alt='" + profile.collection.title + " main image'>");
                }

                if (profile.mainVideo) {
                    panel.find(".main-video").removeClass("hide");
                    panel.find(".video-name").append(profile.mainVideo.name);
                    panel.find(".video-attribution").append(profile.mainVideo.attribution);
                    panel.find(".video-license").append(profile.mainVideo.license);
                    panel.find(".video-embedded").append(profile.mainVideo.embeddedVideo);
                }

                if (profile.mainAudio) {
                    panel.find(".main-audio").removeClass("hide");
                    panel.find(".audio-name").append(profile.mainAudio.name);
                    panel.find(".audio-attribution").append(profile.mainAudio.attribution);
                    panel.find(".audio-license").append(profile.mainAudio.license);
                    panel.find(".audio-embedded").append(profile.mainAudio.embeddedAudio);
                }

                panel.appendTo("#indigenous-info");
            });
        }
    });
}

function showWikipediaData(data) {
    var node = $(data)
    node.find('[role="note"]').remove()

    // show wikipedia data
    var dataLength = $(data).length
    var tested = false
    var valid = true
    node.each(function (idx, item) {
        // include SECTIONS
        if (item.tagName == "SECTION") {
            var $description = $('#descriptionTemplate').clone();

            // redirect if required
            var redirect = $(item).find('link[rel="mw:PageProp/redirect"]')
            if (redirect.length > 0) {
                var redirectItem = redirect[0].href.replace(/^.*\//, "")
                var url = "/externalSite/wikipedia?name=" + encodeURI(redirectItem)
                $.ajax({url: url}).done(function (data) {
                    showWikipediaData(data)
                });
                return
            }

            // basic test for validity
            if (!tested) {
                var uppercaseData = node.text().toUpperCase()
                tested = true
                valid = false
                if (SHOW_CONF.family) {
                    valid = uppercaseData.indexOf(SHOW_CONF.family.toUpperCase()) > 0
                }
                if (!valid && SHOW_CONF.order) {
                    valid = uppercaseData.indexOf(SHOW_CONF.order.toUpperCase()) > 0
                }
                if (!valid && SHOW_CONF.class) {
                    valid = uppercaseData.indexOf(SHOW_CONF.class.toUpperCase()) > 0
                }
                if (!valid && SHOW_CONF.phylum) {
                    valid = uppercaseData.indexOf(SHOW_CONF.phylum.toUpperCase()) > 0
                }
                if (!valid) {
                    return
                }
            }

            // identify the title
            var title
            if (item.childNodes[0].tagName.match(/H[0-9]/)) {
                title = item.childNodes[0].innerHTML
                item.removeChild(item.childNodes[0])
            } else {
                title = jQuery.i18n.prop("description.title.default")
            }
            $description.find(".title").html(title)

            // remove items
            $(item).find('.infobox').remove()
            $(item).find('.hatnote').remove()
            $(item).find('.infobox.biota').remove()
            $(item).find('.mw-editsection').remove()
            $(item).find('.navbar').remove()
            $(item).find('.reference').remove()
            $(item).find('.error').remove()
            $(item).find('.box-Unreferenced_section').remove()
            $(item).find('.portalbox').remove()

            // fix relative links
            $description.find(".content").html(item.innerHTML.replaceAll('href="./', "href=\"" + "https://wikipedia.org/wiki/"))

            // show this description
            $description.css({'display': 'block'});

            // set the source of this description
            var sourceHtml = "<a href='https://wikipedia.org/wiki/" + encodeURI(SHOW_CONF.scientificName) + "' target='wikipedia'>Wikipedia</a>&nbsp;" + jQuery.i18n.prop("wikipedia.licence.comment")
            $description.find(".sourceText").html(sourceHtml);

            // hide unused properties of this description
            var rights = "<a href='https://creativecommons.org/licenses/by-sa/4.0/'>" + jQuery.i18n.prop("wikipedia.licence.label") + "</a>"
            $description.find(".rights").html(rights);
            $description.find(".provider").css({'display': 'none'});

            // add to the page
            $description.appendTo('#descriptiveContent');
        }
    })
}

function loadExternalSources() {
    // load Wikipedia content
    if (SHOW_CONF.wikiUrl != 'hide') {
        var name = SHOW_CONF.scientificName
        if (SHOW_CONF.wikiUrl.match("^http.*")) {
            name = SHOW_CONF.wikiUrl.replace(/^.*\//, "")
        }
        var url = "/externalSite/wikipedia?name=" + encodeURI(name)
        $.ajax({url: url}).done(function (data) {
            showWikipediaData(data)
        });
    }

    //load Genbank content
    $.ajax({url: SHOW_CONF.genbankUrl}).done(function (data) {
        if (data.total) {
            $('.genbankResultCount').html('<a href="' + data.resultsUrl + '">View all results - ' + data.total + '</a>');
            if (data.results) {
                $.each(data.results, function (idx, result) {
                    var $genbank = $('#genbankTemplate').clone();
                    $genbank.removeClass('hide');
                    $genbank.find('.externalLink').attr('href', result.link);
                    $genbank.find('.externalLink').html(result.title);
                    $genbank.find('.description').html(result.description);
                    $genbank.find('.furtherDescription').html(result.furtherDescription);
                    $('.genbank-results').append($genbank);
                });
            }
        }
    });

    //load sound content
    $.ajax({url: SHOW_CONF.soundUrl}).done(function (data) {
        if (data.sounds) {
            var soundsDiv = "<div class='panel panel-default '><div class='panel-heading'>";
            soundsDiv += '<h3 class="panel-title">Sounds</h3></div><div class="panel-body">';
            soundsDiv += '<audio src="' + data.sounds[0].alternativeFormats['audio/mpeg'] + '" preload="auto" />';
            audiojs.events.ready(function () {
                var as = audiojs.createAll();
            });
            var source = "";

            if (data.processed.attribution.collectionName) {
                var attrUrl = "";
                var attrUrlPrefix = SHOW_CONF.collectoryUrl + "/public/show/";
                if (data.raw.attribution.dataResourceUid) {
                    attrUrl = attrUrlPrefix + data.raw.attribution.dataResourceUid;
                } else if (data.processed.attribution.collectionUid) {
                    attrUrl = attrUrlPrefix + data.processed.attribution.collectionUid;
                }

                if (data.raw.attribution.dataResourceUid == "dr341") {
                    // hard-coded copyright as most sounds are from ANWC and are missing attribution data fields
                    source += "&copy; " + data.processed.attribution.collectionName + " " + data.processed.event.year + "<br>";
                }

                if (attrUrl) {
                    source += "Source: <a href='" + attrUrl + "' target='biocache'>" + data.processed.attribution.collectionName + "</a>";
                } else {
                    source += data.processed.attribution.collectionName;
                }


            } else {
                source += "Source: " + data.processed.attribution.dataResourceName
            }

            soundsDiv += '</div><div class="panel-footer"><p>' + source + '<br>';
            soundsDiv += '<a href="' + SHOW_CONF.biocacheUrl + '/occurrence/' + data.raw.rowKey + '">View more details of this audio</a></p>';
            soundsDiv += '</div></div>';
            $('#sounds').append(soundsDiv);
        }
    }).fail(function (jqXHR, textStatus, errorThrown) {
        console.warn("AUDIO Error", errorThrown, textStatus);
    });
}

/**
 * Trigger loading of the 3 gallery sections
 */
function loadGalleries() {
    //console.log('loading galleries');
    $('#gallerySpinner').show();
    loadGalleryType('type', 0)
    loadGalleryType('specimen', 0)
    loadGalleryType('other', 0)
    loadGalleryType('uncertain', 0)
}

var entityMap = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': '&quot;',
    "'": '&#39;',
    "/": '&#x2F;'
};

function escapeHtml(string) {
    return String(string).replace(/[&<>"'\/]/g, function (s) {
        return entityMap[s];
    });
}

/**
 * Load overview images on the species page. This is separate from the main galleries.
 */
function loadOverviewImages() {
    var countPreferredImages = 0; // Could get a race condition where no main image gets loaded due callbacks

    if (SHOW_CONF.preferredImageId) {
        var imageIds = SHOW_CONF.preferredImageId.split(',')
        $.each(imageIds, function(idx, imageId) {
            var prefUrl = SHOW_CONF.biocacheServiceUrl +
                '/occurrences/search?q=images:' + imageId +
                '&fq=-assertion_user_id:*&im=true&facet=off&pageSize=1&start=0';
            $.ajax({
                url: prefUrl,
                dataType: 'json',
                async: false,
                success: function (data) {
                    var record = {
                        'uuid': null,
                        'image': imageId,
                        'scientificName': SHOW_CONF.scientificName,
                        'largeImageUrl': SHOW_CONF.imageServiceBaseUrl + "/image/" + imageId + "/large"
                    }
                    if (data && data.totalRecords > 0) {
                        record.uuid = data.occurrences[0].uuid;
                    }

                    if (countPreferredImages == 0) {
                        addOverviewImage(record)
                    } else {
                        addOverviewThumb(record, countPreferredImages)
                    }
                    countPreferredImages = countPreferredImages + 1
                }
            })
        })
    }

    var url = SHOW_CONF.biocacheServiceUrl +
        '/occurrences/search?q=lsid:' +
        SHOW_CONF.guid +
        '&fq=multimedia:"Image"&fq=geospatial_kosher:true&fq=-user_assertions:50001&fq=-user_assertions:50005&im=true&facet=off&pageSize=5&start=0';

    $.getJSON(url, function (data) {
        if (data && data.totalRecords > 0) {
            addOverviewImages(data.occurrences, countPreferredImages);
        }
    }).always(function () {
        $('#gallerySpinner').hide();
    });
}

function addOverviewImages(imagesArray, countPreferredImages) {
    var addedImages = countPreferredImages
    for (j = 0; j < imagesArray.length && addedImages < 5; j++) {
        // load images
        if (imagesArray.length > j && (SHOW_CONF.preferredImageId || SHOW_CONF.preferredImageId.indexOf(imagesArray[j].image) < 0) &&
            SHOW_CONF.hiddenImages.indexOf(imagesArray[j].image) < 0) {
            if (addedImages == 0) {
                // first image is the overview image
                addOverviewImage(imagesArray[j])
            } else {
                // additional images are thumbnails
                addOverviewThumb(imagesArray[j], addedImages)
            }
            addedImages = addedImages + 1
        }
    }
}

function addOverviewImage(overviewImageRecord) {
    $('#noOverviewImages').addClass('hide');
    $('.main-img').removeClass('hide');
    $('.thumb-row').removeClass('hide');
    var $categoryTmpl = $('#overviewImages');
    $categoryTmpl.removeClass('hide');

    var $mainOverviewImage = $('.mainOverviewImage');
    $mainOverviewImage.attr('src', overviewImageRecord.largeImageUrl);
    $mainOverviewImage.parent().attr('href', overviewImageRecord.largeImageUrl);
    $mainOverviewImage.parent().attr('data-title', getImageTitleFromOccurrence(overviewImageRecord));
    $mainOverviewImage.parent().attr('data-footer', getImageFooterFromOccurrence(overviewImageRecord));
    $mainOverviewImage.parent().attr('data-image-id', overviewImageRecord.image);
    $mainOverviewImage.parent().attr('data-record-url', SHOW_CONF.biocacheUrl + '/occurrences/' + overviewImageRecord.uuid);

    $mainOverviewImage.parent().parent().find('.hero-button').attr('onclick', 'event.stopImmediatePropagation(); heroImage("' + overviewImageRecord.image + '");')

    $('.mainOverviewImageInfo').html(getImageTitleFromOccurrence(overviewImageRecord));
}

function addOverviewThumb(record, i) {

    if (i < 4) {
        var $thumb = generateOverviewThumb(record, i);
        $('#overview-thumbs').append($thumb);
    } else {
        $('#more-photo-thumb-link').attr('style', 'background-image:url(' + record.smallImageUrl + ')');
    }
}

function generateOverviewThumb(occurrence, id) {
    var $taxonSummaryThumb = $('#taxon-summary-thumb-template').clone();
    var $taxonSummaryThumbLink = $taxonSummaryThumb.find('a');
    $taxonSummaryThumb.removeClass('hide');
    $taxonSummaryThumb.attr('id', 'taxon-summary-thumb-' + id);
    $taxonSummaryThumb.attr('style', 'background-image:url(' + occurrence.smallImageUrl + ')');
    $taxonSummaryThumbLink.attr('data-title', getImageTitleFromOccurrence(occurrence));
    $taxonSummaryThumbLink.attr('data-footer', getImageFooterFromOccurrence(occurrence));
    $taxonSummaryThumbLink.attr('href', occurrence.largeImageUrl);
    $taxonSummaryThumbLink.attr('data-image-id', occurrence.image);
    $taxonSummaryThumbLink.attr('data-record-url', SHOW_CONF.biocacheUrl + '/occurrences/' + occurrence.uuid);
    $taxonSummaryThumb.find('.hero-button').attr('onclick', 'event.stopImmediatePropagation(); heroImage("' + occurrence.image + '");')
    return $taxonSummaryThumb;
}

function editWikipediaURL() {
    var defaultWiki = SHOW_CONF.wikiUrl

    var url = prompt(jQuery.i18n.prop("edit.wiki.url"), defaultWiki);

    if (url != defaultWiki && url != null && url != undefined) {
        var url = '/externalSite/setUrl?guid=' + encodeURIComponent(SHOW_CONF.guid) + '&url=' + encodeURIComponent(url) + '&name=' + encodeURIComponent(SHOW_CONF.scientificName)
        $.getJSON(url, function (data) {
        })
        SHOW_CONF.wikiUrl = url
    }
}

function heroImage(imageId) {
    // determine existing order
    var originalOrder = 0   // not an existing preferred image
    var imageIds = SHOW_CONF.preferredImageId.split(',')
    if (SHOW_CONF.preferredImageId == '') {
        imageIds = []
    }
    for (var i=0;i<imageIds.length;i++) {
        if (imageIds[i] == imageId) {
            originalOrder = i + 1
        }
    }

    // is it an excluded imageId?
    if (SHOW_CONF.hiddenImages.indexOf(imageId) >= 0) {
        originalOrder = -1
    }

    var order = parseInt(prompt(jQuery.i18n.prop("confirm.hero.image"), originalOrder))

    // Insert imageId at the position 1 to 5. Remove the image if `order` == 0. Add to exclusion list if -1.
    if (order != originalOrder && order != null && order != undefined) {
        // insert or remove from preferred images
        var newImageIds = []
        if (imageIds.length == 0 && order > 0) {
            newImageIds.push(imageId)
        } else {
            for (var i = 0; i < imageIds.length; i++) {
                if (order <= 0 && imageIds[i] != imageId) {
                    // add image if it is not being removed
                    newImageIds.push(imageIds[i])
                } else if (order > 0 && order - 1 == i) {
                    // insert image at requested position
                    newImageIds.push(imageId)
                    if (newImageIds.length < 5) {
                        newImageIds.push(imageIds[i])
                    }
                } else if (order > 0 && imageId != imageIds[i]) {
                    // insert image if it is not being moved
                    newImageIds.push(imageIds[i])
                }
            }
        }

        // insert or remove from excluded images
        var hiddenImages = SHOW_CONF.hiddenImages.split(',')
        if (SHOW_CONF.hiddenImages == '') {
            hiddenImages = []
        }
        var newHiddenImages = []
        if (order == -1) {
            // add to hidden images
            newHiddenImages = [imageId]
        }
        for (var i = 0; i < hiddenImages.length; i++) {
            // copy all hidden images but exclude imageId if it no longer hidden
            if (order < 0 || hiddenImages[i] != imageId) {
                newHiddenImages.push(hiddenImages[i])
            }
        }

        var url = '/externalSite/setImages?guid=' + encodeURIComponent(SHOW_CONF.guid) + '&prefer=' +
            encodeURIComponent(newImageIds.join(',')) + '&name=' + encodeURIComponent(SHOW_CONF.scientificName) +
            '&hide=' + encodeURIComponent(newHiddenImages.join(','))
        $.getJSON(url, function (data) {
        })

        SHOW_CONF.preferredImageId = newImageIds.join(',')
        SHOW_CONF.hiddenImages = newHiddenImages.join(',')
    }
}

/**
 * AJAX loading of gallery images from biocache-service
 *
 * @param category
 * @param start
 */
function loadGalleryType(category, start) {

    var imageCategoryParams = {
        type: '&fq=type_status:*',
        specimen: '&fq=basis_of_record:PreservedSpecimen&fq=-type_status:*',
        other: '&fq=-type_status:*&fq=-basis_of_record:PreservedSpecimen&fq=-identification_qualifier_s:"Uncertain"&fq=geospatial_kosher:true&fq=-user_assertions:50001&fq=-user_assertions:50005',
        uncertain: '&fq=-type_status:*&fq=-basis_of_record:PreservedSpecimen&fq=identification_qualifier_s:"Uncertain"'
    };

    var pageSize = 20;

    if (start > 0) {
        $('.loadMore.' + category + ' button').addClass('disabled');
        $('.loadMore.' + category + ' img').removeClass('hide');
    }

    //TODO a toggle between LSID based searches and names searches
    var url = SHOW_CONF.biocacheServiceUrl +
        '/occurrences/search?q=lsid:' +
        SHOW_CONF.guid +
        (SHOW_CONF.qualityProfile ? "&qualityProfile=" + SHOW_CONF.qualityProfile : "") + '&fq=multimedia:"Image"&pageSize=' + pageSize +
        '&facet=off&start=' + start + imageCategoryParams[category] + '&im=true';

    $.getJSON(url, function (data) {

        if (data && data.totalRecords > 0) {
            var br = "<br>";
            var $categoryTmpl = $('#cat_' + category);
            $categoryTmpl.removeClass('hide');

            $.each(data.occurrences, function (i, el) {
                // clone template div & populate with metadata
                var $taxonThumb = $('#taxon-thumb-template').clone();
                $taxonThumb.removeClass('hide');
                if (SHOW_CONF.hiddenImages.indexOf(el.image) >= 0) {
                    $taxonThumb.addClass('hiddenImage');
                    if (!SHOW_CONF.showHiddenImages) {
                        $taxonThumb.hide();
                    }
                }
                $taxonThumb.attr('id', 'thumb_' + category + i);
                // $taxonThumb.attr('href', el.largeImageUrl);
                $taxonThumb.find('img').attr('src', el.smallImageUrl);
                // turned off 'onerror' below as IE11 hides all images
                //$taxonThumb.find('img').attr('onerror',"$(this).parent().hide();"); // hide broken images

                // brief metadata
                var briefHtml = getImageTitleFromOccurrence(el);
                $taxonThumb.find('.caption-brief').html(briefHtml);
                $taxonThumb.attr('data-title', briefHtml);
                $taxonThumb.find('.caption-detail').html(briefHtml);

                $taxonThumb.find('.hero-button').attr('onclick', 'event.stopImmediatePropagation(); heroImage("' + el.image + '");')

                // write to DOM
                $taxonThumb.attr('data-footer', getImageFooterFromOccurrence(el));
                $taxonThumb.attr('data-image-id', el.image);
                $taxonThumb.attr('data-record-url', SHOW_CONF.biocacheUrl + '/occurrences/' + el.uuid);
                $categoryTmpl.find('.taxon-gallery').append($taxonThumb);
            });

            $('.loadMore.' + category).remove(); // remove 'load more images' button that was just clicked

            if (data.totalRecords > (start + pageSize)) {
                // add new 'load more images' button if required
                var spinnerLink = $('img#gallerySpinner').attr('src');
                var btn = '<div class="loadMore ' + category + '"><br><button class="btn btn-default" onCLick="loadGalleryType(\'' + category + '\','
                    + (start + pageSize) + ');">Load more images <img src="' + spinnerLink + '" class="hide"/></button></div>';
                $categoryTmpl.find('.taxon-gallery').append(btn);
            }
        } else {
            $('#cat_nonavailable').addClass('show');
        }
    }).fail(function (jqxhr, textStatus, error) {
        alert('Error loading gallery: ' + textStatus + ', ' + error);
    }).always(function () {
        $('#gallerySpinner').hide();
    });
}

function getImageTitleFromOccurrence(el) {
    var br = "<br/>";
    var briefHtml = "";
    //include sci name when genus or higher taxon
    if (SHOW_CONF.taxonRankID < 7000) {
        briefHtml += (el.raw_scientificName === undefined ? el.scientificName : el.raw_scientificName); //raw scientific name can be null, e.g. if taxon GUIDS were submitted
    }

    if (el.typeStatus) {
        if (briefHtml.length > 0) briefHtml += br;
        briefHtml += el.typeStatus;
    }

    if (el.institutionName) {
        if (briefHtml.length > 0) briefHtml += br;
        briefHtml += ((el.typeStatus) ? ' | ' : br) + el.institutionName;
    }

    if (el.imageMetadata && el.imageMetadata.length > 0 && el.imageMetadata[0].creator != null) {
        if (briefHtml.length > 0) briefHtml += br;
        briefHtml += "Photographer: " + el.imageMetadata[0].creator;
    } else if (el.imageMetadata && el.imageMetadata.length > 0 && el.imageMetadata[0].rightsHolder != null) {
        if (briefHtml.length > 0) briefHtml += br;
        briefHtml += "Rights holder: " + el.imageMetadata[0].rightsHolder;
    } else if (el.collector) {
        if (briefHtml.length > 0) briefHtml += br;
        briefHtml += "Supplied by: " + el.collector;
    }

    return briefHtml;
}

function getImageFooterFromOccurrence(el) {
    var br = "<br/>";
    var detailHtml = (el.raw_scientificName === undefined ? el.scientificName : el.raw_scientificName); //raw scientific name can be null, e.g. if taxon GUIDS were submitted
    if (el.typeStatus) detailHtml += br + 'Type: ' + el.typeStatus;
    if (el.collector) detailHtml += br + 'By: ' + el.collector;
    if (el.eventDate) detailHtml += br + 'Date: ' + moment(el.eventDate).format('YYYY-MM-DD');
    if (el.institutionName && el.institutionName !== undefined) {
        detailHtml += br + "Supplied by: " + el.institutionName;
    } else if (el.dataResourceName && el.dataResourceName !== undefined) {
        detailHtml += br + "Supplied by: " + el.dataResourceName;
    }
    if (el.imageMetadata && el.imageMetadata.length > 0 && el.imageMetadata[0].rightsHolder != null) {
        detailHtml += br + "Rights holder: " + el.imageMetadata[0].rightsHolder;
    }

    // write to DOM
    if (el.uuid) {
        detailHtml += '<div class="recordLink"><a href="' + SHOW_CONF.biocacheUrl + '/occurrences/' + el.uuid + '">View details of this record</a>' +
            '<br><br>If this image is incorrectly<br>identified please flag an<br>issue on the <a href=' + SHOW_CONF.biocacheUrl +
            '/occurrences/' + el.uuid + '>record.<br></div>';
    }
    return detailHtml;
}

function loadBhl() {
    loadBhl(0, 10, false);
}

/**
 * BHL search to populate literature tab
 *
 * @param start
 * @param rows
 * @param scroll
 */
function loadBhl(start, rows, scroll) {
    if (!start) {
        start = 0;
    }
    if (!rows) {
        rows = 10;
    }
    var source = SHOW_CONF.bhlUrl;
    var taxonName = SHOW_CONF.scientificName;
    var synonyms = SHOW_CONF.synonyms;
    var i;
    var query = ""; // = taxonName.split(/\s+/).join(" AND ") + synonyms;
    if (taxonName) {
        query = query + "s=" + encodeURIComponent(taxonName);
    }
    if (synonyms) {
        for (i = 0; i < synonyms.length; i++) {
            if (synonyms[i] == taxonName) {
                continue;
            }
            if (query.length > 0)
                query = query + "&";
            query = query + "s=" + encodeURIComponent(synonyms[i]);
        }
    }

    if (!query) {
        return cancelSearch("No names were found to search BHL");
    }

    var url = source + "?" + query + '&start=' + start + "&rows=" + rows;
    var buf = "";
    $("#status-box").css("display", "block");
    $("#bhl-results-list").html("");

    $("#bhl-results-list").load(url);
} // end doSearch

function cancelSearch(msg) {
    $("#status-box").css("display", "none");
    $("#solr-results").html(msg);
    return true;
}

let distributions = []
var distributionsIdx = 0

function loadExpertDistroMap() {
    var url = SHOW_CONF.layersServiceUrl + "/distribution/lsids/" + SHOW_CONF.guid;
    $.getJSON(url, function (data) {
        if (data) {
            $.each(data, function (idx, distribution) {
                var record = {
                    url: distribution.imageUrl || distribution.image_url,
                    name: distribution.area_name,
                    dr: distribution.data_resource_uid
                }

                if (record.dr) {
                    $.getJSON(SHOW_CONF.collectoryUrl + "/ws/dataResource/" + record.dr, function (collectoryData) {
                        record.providerName = collectoryData.name
                        distributions.push(record)

                        showDistribution()
                    })
                }
            })
            $('#expertDistroCount').text(' (' + data.length + ')')
        }
    })
}

function nextDistribution() {
    if (distributionsIdx < distributions.length) {
        distributionsIdx = distributionsIdx + 1
    }

    showDistribution()
}

function prevDistribution() {
    if (distributionsIdx > 0) {
        distributionsIdx = distributionsIdx - 1
    }

    showDistribution()
}

function showDistribution() {
    $("#expertDistroDiv img").attr("src", distributions[distributionsIdx].url);
    $("#dataResourceAreaName").text((distributionsIdx + 1) + ": " + distributions[distributionsIdx].name)
    if (distributions[distributionsIdx].dr) {
        var attr = $('<a>').attr('href', SHOW_CONF.collectoryUrl + '/public/show/' + distributions[distributionsIdx].dr).text(distributions[distributionsIdx].providerName)
        $("#expertDistroDiv #dataResource").html(attr);
    }

    $("#expertDistroDiv").show();

    if (distributionsIdx > 0) {
        $("#expertDistroPrev").prop("disabled", false);
    } else {
        $("#expertDistroPrev").prop("disabled", true);
    }

    if (distributionsIdx < distributions.length - 1) {
        $("#expertDistroNext").prop("disabled", false);
    } else {
        $("#expertDistroNext").prop("disabled", true);
    }
}

function expandImageGallery(btn) {
    if (!$(btn).hasClass('.expand-image-gallery')) {
        $(btn).parent().find('.collapse-image-gallery').removeClass('btn-primary');
        $(btn).addClass('btn-primary');

        $(btn).parents('.image-section').find('.taxon-gallery').slideDown(400)
    }
}

function collapseImageGallery(btn) {
    if (!$(btn).hasClass('.collapse-image-gallery')) {
        $(btn).parent().find('.expand-image-gallery').removeClass('btn-primary');
        $(btn).addClass('btn-primary');

        $(btn).parents('.image-section').find('.taxon-gallery').slideUp(400)
    }
}
