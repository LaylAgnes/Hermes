const { chromium } = require('playwright');
const axios = require('axios');

const START_URL = 'https://boards.greenhouse.io/nubank';
const API_URL = 'http://localhost:8080/api/jobs/import';

(async () => {

  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  await page.goto(START_URL, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('a[href*="/jobs/"]');

  const links = await page.$$eval('a[href*="/jobs/"]', as =>
    [...new Set(as.map(a => a.href))]
  );

  console.log("Vagas encontradas:", links.length);

  const jobs = [];

  for (const link of links.slice(0, 10)) { // limita pra teste

    const jobPage = await browser.newPage();
    await jobPage.goto(link, { waitUntil: 'domcontentloaded' });

    const title = await jobPage.locator('h1').innerText().catch(()=>null);

    const location = await jobPage.locator('[class*="location"]').first().innerText().catch(()=>null);

    const description = await jobPage.locator('#content, .content, main').innerText().catch(()=>null);

    jobs.push({
      url: link,
      title,
      location,
      description
    });

    console.log("Indexado:", title);

    await jobPage.close();
  }

  await axios.post(API_URL, { jobs });

  console.log("Enviado para API:", jobs.length);

  await browser.close();

})();