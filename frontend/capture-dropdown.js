const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  // Set viewport
  await page.setViewport({ width: 1280, height: 800 });
  
  // Navigate to the page
  await page.goto('http://localhost', { waitUntil: 'networkidle2' });
  
  // Wait for the user menu button to be available
  await page.waitForSelector('.user-menu button', { visible: true });
  
  // Click the user menu button
  await page.click('.user-menu button');
  
  // Wait for dropdown to appear
  await page.waitForSelector('.user-dropdown', { visible: true });
  
  // Wait a bit for animation
  await new Promise(resolve => setTimeout(resolve, 500));
  
  // Take screenshot
  await page.screenshot({ path: 'user-dropdown-open.png', fullPage: true });
  
  console.log('Screenshot saved as user-dropdown-open.png');
  
  await browser.close();
})();