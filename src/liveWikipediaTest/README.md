# Live Wikipedia Integration Tests

This source set contains integration tests that call the real Wikipedia API and page endpoints. They exist in their own Gradle source set so that they:

- are **not executed** during `./gradlew build`, `./gradlew check`, or `./gradlew test`, and
- can be run on demand to verify that `ExternalSiteService.searchWikipedia()` still finds real content for a curated list of taxa.

## Why a separate source set?

External APIs can change, rate-limit, or be temporarily unavailable. We do not want routine development builds, pull-request checks, or releases to fail because of a Wikipedia outage or a page edit. The tests therefore live outside the normal `src/integration-test` directory and are wired to a dedicated Gradle task.

## Test data

`src/liveWikipediaTest/resources/wikipedia-live-tests.csv` is a plain CSV file with the columns:

| Column | Description |
| --- | --- |
| `scientificName` | The scientific name to search for. |
| `kingdom` | The ALA kingdom to supply to `searchWikipedia` (may be empty). |
| `expectedHtmlContent` | A substring expected to appear in the returned HTML. The check is case-insensitive. May be empty if `commonName` is provided. |
| `commonName` | An optional additional fragment to check for in the returned HTML. Used when `expectedHtmlContent` is empty. |

Only stable, broad terms such as *species* or *genus* or geographic names should be used in `expectedHtmlContent`, because exact page titles can change in Wikipedia.

The snippet filter (`wikipedia.snippetPattern`) is configured in `application.yml`. It is used to exclude irrelevant Wikipedia pages (e.g., disambiguation pages, bands, airports) from the search results before fetching page HTML. It includes taxonomic rank terms plus broader context terms such as *australia* and *endemic* that commonly appear in taxon article snippets.

## Running the tests locally

```shell
./gradlew liveWikipediaTest
```

This enables the `wikipedia.live.tests` system property and runs only the tests under `src/liveWikipediaTest/groovy`.

## When these tests run

A GitHub Actions workflow (`.github/workflows/live-wikipedia-check.yml`) runs them on a weekly cron schedule and on manual dispatch. The workflow uses `continue-on-error: true` and writes a GitHub job summary, so it warns the maintainers but does not block other builds if Wikipedia changes.

## Known limitations

The current live test suite deliberately includes cases such as `Moggridgea rainbowi` whose Wikipedia search result snippets do not contain a bare rank keyword (e.g., *species*). This exposes how restrictive the snippet filter in `ExternalSiteService.searchWikipediaCandidates()` can be: a page that is clearly a taxon article may still be rejected because the search snippet omits the rank term. When such a test fails, review whether the service filters have become too restrictive before removing or changing the test data.

## Adding new taxa

Add a new line to `src/liveWikipediaTest/resources/wikipedia-live-tests.csv` and run `./gradlew liveWikipediaTest` before committing.
