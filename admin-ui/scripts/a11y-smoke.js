#!/usr/bin/env node
/* eslint-disable no-console */
const { AxePuppeteer } = require('@axe-core/puppeteer');
const puppeteer = require('puppeteer');

(async () => {
  const baseUrl = process.env.BASE_URL || 'http://localhost:4200';
  const routes = ['/', '/dashboard', '/change-requests'];
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  let hasViolations = false;

  for (const route of routes) {
    const url = new URL(route, baseUrl).toString();
    await page.goto(url, { waitUntil: 'networkidle2' });
    // Give UI a moment to settle
    await page.waitForTimeout(500);

    const results = await new AxePuppeteer(page).withTags(['wcag2a', 'wcag2aa']).analyze();
    if (results.violations.length) {
      hasViolations = true;
      console.log(`\nAccessibility violations on ${url}:`);
      for (const v of results.violations) {
        console.log(`- [${v.impact}] ${v.id}: ${v.help}`);
        console.log(`  Help: ${v.helpUrl}`);
        v.nodes.slice(0, 5).forEach((n, i) => {
          console.log(`  Node ${i + 1}: ${n.html}`);
          console.log(`  Target: ${n.target.join(' ')}`);
        });
      }
    } else {
      console.log(`No accessibility violations on ${url}.`);
    }
  }

  await browser.close();
  if (hasViolations) process.exit(1);
  process.exit(0);
})().catch((err) => {
  console.error(err);
  process.exit(2);
});

