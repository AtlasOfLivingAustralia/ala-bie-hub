import { test, expect } from '@playwright/test';

const fs = require('fs');
const searchUrl = '/search';

// Needed for BIE WAF on GH actions servers
test.use({ userAgent: 'GH Actions Bot 1.0' });

test('BIE home page has title', async ({ page }) => {
  await page.goto('/');

  // Expect a title "to contain" a substring.
  await expect(page).toHaveTitle(/Search | Atlas of Living Australia/);
});

test('autocomplete suggestion test', async ({ page }) => {
  await page.goto('/');

  // Type text into the search input.
  await page.fill('#search', 'acacia');

  // Wait for the autocomplete suggestions to appear.
  await page.waitForSelector('.ui-autocomplete .ui-menu-item');

  // Check that the correct suggestion is displayed with an exact string match.
  const suggestion = await page.locator('.ui-menu-item-wrapper').first().innerText();
  await expect(suggestion).toBe('Acacia');

});

test('search button test', async ({ page }) => {
  await page.goto('/');

  // Type text into the search input.
  await page.fill('#search', 'Acacia');

  // Click the get started link.
  await page.getByRole('button', { name: 'Search', exact: true }).click();

  // Expects page to have a heading with the name of Installation.
  await expect(page.getByRole('heading', { name: 'Search for Acacia' })).toBeVisible();

});

test('Acacia search results', async ({ page }) => {
  await page.goto(searchUrl + '?q=Acacia&rows=20');

  // Check Facets section
  const facetsIdx = page.locator('#facet-idxtype');
  await expect(facetsIdx).toContainText('Species');
  await expect(facetsIdx).toContainText('Common Name');
  await expect(facetsIdx).toContainText('Support article');

  const facetsRank = page.locator('#facet-rank');
  await expect(facetsRank).toContainText('Variety');
  await expect(facetsRank).toContainText('Subspecies');
  await expect(facetsRank).toContainText('Form');

  const facetsAusCons = page.locator('#facet-conservationStatusAUS_s');
  await expect(facetsAusCons).toContainText('Vulnerable');
  await expect(facetsAusCons).toContainText('Endangered');
  await expect(facetsAusCons).toContainText('Extinct');

  // Check for thumbnail images on right side - in .result-thumbnail
  await expect(page.locator('.result-thumbnail img').first()).toBeAttached();
});

test('Acacia download test', async ({ page, browserName }) => {
  test.skip(browserName === 'webkit', 'Doesn\'t work for WebKit on Linux');
  // Search for Acacia 
  await page.goto(searchUrl + '?q=Acacia&rows=20');

  // Enable async handling of downloads
  const downloadPromise = page.waitForEvent('download');

  // Perform the action that initiates the download
  await page.getByRole('link', { name: ' Download' }).click(); // Unicode character for download icon

  // Wait for download to begin
  const download = await downloadPromise;

  // Wait for the download process to complete and get the downloaded file path
  await download.saveAs('/tmp/' + download.suggestedFilename());
  const filePath = await download.path();

  // Read file contents directly without saving permanently
  const fileContent = await fs.promises.readFile(filePath, 'utf-8');

  // Verify file contents
  expect(fileContent).toContain('taxonID');
  expect(fileContent).toContain('scientificName');
  expect(fileContent).toContain('APNI');
  expect(fileContent).toContain('Acacia dealbata');

  // Check file is roughly the expected size (over 3000 lines)
  const lineCount = fileContent.split('\n').length;
  expect(lineCount).toBeGreaterThan(3000);
});

