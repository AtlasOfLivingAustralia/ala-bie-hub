import { test, expect } from '@playwright/test';
// import exp from 'constants';

// const baseUrl = 'https://bie-test.ala.org.au';
const searchUrl = '/search?q=Acacia&rows=20';

// Needed for BIE WAF on GH actions servers
test.use({ userAgent: 'GH Actions Bot 1.0' });

test('Acacia Mill - names check', async ({ page }) => {
  // Search for Acacia 
  await page.goto(searchUrl);

  // Click the genus page result
  await page.locator('a[href="/species/Acacia"]').nth(1).click();
  await page.waitForSelector('h1 .accepted-name', { timeout: 30000 })
  await expect(page.locator('h1 .accepted-name')).toContainText('Acacia Mill.');
  await page.waitForSelector('.language-name', { timeout: 30000 });
  await expect(page.locator('.language-name').nth(0)).toContainText('Wudjari');
  const errorsList = page.locator('ul.errors'); // error page should not be present
  await expect(errorsList).toHaveCount(0);
});

test('Acacia Mill - API URL', async ({ page }) => {
  // Navigate via search results so the test is resilient to taxon ID changes.
  await page.goto(searchUrl);
  await page.locator('a[href="/species/Acacia"]').nth(1).click();
  await page.waitForSelector('h1 .accepted-name', { timeout: 30000 });

  await page.getByRole('button', { name: 'API' }).click();
  const textInput = page.locator('#al4rcode');
  const value = await textInput.inputValue();
  await expect(value).toMatch(/https:\/\/bie-ws-test\.ala\.org\.au\/ws\/species\/https:\/\/id\.biodiversity\.org\.au\/taxon\/apni\/\d+/);
});

test('Acacia Mill - hero images', async ({ page }) => {
  // Navigate via search results so the test is resilient to taxon ID changes.
  await page.goto(searchUrl);
  await page.locator('a[href="/species/Acacia"]').nth(1).click();
  await page.waitForSelector('h1 .accepted-name', { timeout: 30000 });

  // Overview images depend on external occurrence/image data. If none are
  // available for the current taxon, skip the rest of this test.
  const hasImages = await page.locator('.thumb-row:not(.hide)').isVisible({ timeout: 30000 }).catch(() => false);
  test.skip(!hasImages, 'No overview images available for this taxon in the current environment');

  await page.waitForFunction(() => {
    const thumbs = document.querySelectorAll('.taxon-summary-thumb');
    return Array.from(thumbs).some(thumb => {
      const bg = window.getComputedStyle(thumb).backgroundImage;
      return bg && bg.includes('image/proxyImageThumbnail');
    });
  }, { timeout: 30000 });

  const thumbCount = await page.locator('.thumb-row:not(.hide) .taxon-summary-thumb').count();
  await expect(thumbCount).toBeGreaterThanOrEqual(2);
});

test('Acacia Mill - Wikipedia content', async ({ page }) => {
  // Navigate via search results so the test is resilient to taxon ID changes.
  await page.goto(searchUrl);
  await page.locator('a[href="/species/Acacia"]').nth(1).click();
  await page.waitForSelector('h1 .accepted-name', { timeout: 30000 });

  await page.waitForSelector('.panel-description', { timeout: 30000 });
  const expectedTexts = ['Description', 'Taxonomy', 'Ecology', 'Uses', 'References'];
  let matchedCount = 0;
  for (const text of expectedTexts) {
    if (await page.getByText(text, { exact: true }).isVisible().catch(() => false)) {
      matchedCount++;
    }
  }
  await expect(matchedCount).toBeGreaterThanOrEqual(3);
});
