#!/usr/bin/env node
/* eslint-disable no-console */
const puppeteer = require('puppeteer');

async function run() {
  const baseUrl = process.env.BASE_URL || 'http://localhost:80';
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();

  async function goto(path) {
    const url = new URL(path, baseUrl).toString();
    await page.goto(url, { waitUntil: 'networkidle2' });
  }

  // Dashboard
  await goto('/dashboard');
  await page.waitForSelector('h2.cbp-card__title, .cbp-dashboard-header__title', { timeout: 10000 });

  // Skip Quick Add in smoke; covered in unit flows

  // Countries: search/filter and export
  await goto('/countries');
  await page.waitForSelector('.cbp-page-header__title, .cbp-table-container', { timeout: 10000 });
  await page.type('#search-countries', 'United');
  await new Promise(r => setTimeout(r, 600));
  // Reset filters if visible
  const resetVisible = await page.$('.cbp-button--outline.cbp-button--small');
  if (resetVisible) {
    await resetVisible.click();
  }
  // Export All
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button')).filter(b => /Export Data|Export/i.test(b.textContent || ''));
    (btns[0] || document.querySelector('button')).click();
  });
  await new Promise(r => setTimeout(r, 500));

  // Change Requests: open and filter
  await goto('/change-requests');
  await page.waitForSelector('.cbp-page-header__title, .cbp-table-container', { timeout: 10000 });
  // Select a status filter if present
  const statusSelect = await page.$('#status-filter');
  if (statusSelect) await page.select('#status-filter', 'PENDING');
  await new Promise(r => setTimeout(r, 300));

  // Activity Log: ensure route resolves
  await goto('/activity-log');
  // Settings and Help stubs
  await goto('/settings');
  await goto('/help/user-guide');

  await browser.close();
  console.log('Smoke OK');
}

run().catch((e) => {
  console.error(e);
  process.exit(1);
});
