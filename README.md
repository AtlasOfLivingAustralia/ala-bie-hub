# ala-bie-hub
###    [![Build Status](https://app.travis-ci.com/AtlasOfLivingAustralia/ala-bie-hub.svg?branch=develop)](https://app.travis-ci.com/AtlasOfLivingAustralia/ala-bie-hub)

This is the BIE (Biodiversity Information Explorer) front end - merge of old ala-bie and bie-plugin. 

THE BIE handles general search duties and serves taxon/species pages.
For more details on the architecture see [BIE index](http://github.com/AtlasOfLivingAustralia/bie-index)

### Languages

The bie-plugin uses ISO-639 language codes, particularly ISO-639-3, drawn from http://www.sil.org/iso639-3/ and the AIATSIS codes, drawn from https://aiatsis.gov.au/

### Blacklisted External Information

It is possible to blacklist sources of external information
that is either incorrect or not relevant.
Blacklisting is performed by pattern matching and can be configured by URLs that give a specific blacklist.
Blacklists are configured as a map of possible blacklists against the information in a document.
For example:

```yaml
external:
  blacklist: file:///data/ala-bie/config/blacklist.json
```

An example blacklist file can be found [here](src/test/resources/test-blacklist.json).
It contains metadata descibing the intent of the blacklist and
a list of entries that will cause the blacklist to trigger.

Each blacklist entry can trigger on some combination of:

* **source** The URL of the original source of the data.
* **name** The supplied name of taxon.
* **title** The title of the article

Currently, the blacklist is only used with the Encyclopedia of Life external source.


### Common Names Pull

It is possible to "pull" common names with special status into their own section and have them displayed in a special way.
To do this, use the following configuration settings:

* `vernacularName.pull.active` Set to true for a pull display (*false* by default)
* `vernacularName.pull.categories` A comma-separated list of status values that will cause the names to be pulled (*empty* by default)
* `vernacularName.pull.label` The label for the pull section in the names tab (*Special Common Names* by default)
* `vernacularName.pull.labelDetail` The detail to put into the label title (empty by default)
* `vernacularName.pull.showHeader` List these names in the header, just below the preferred common name (*false* by default)
* `vernacularName.pull.showLanguage` Show the language of the common name (*false* by default)

### Change log
See the [releases page](https://github.com/AtlasOfLivingAustralia/ala-bie-hub/releases) for this repository.
