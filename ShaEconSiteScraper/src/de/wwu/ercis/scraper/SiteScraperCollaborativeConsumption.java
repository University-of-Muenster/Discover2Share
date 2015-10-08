package de.wwu.ercis.scraper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * {@link SiteScraperMeshingIt} is used to scrape content from web sites.
 * 
 * @author mo
 */
public class SiteScraperCollaborativeConsumption implements
		SiteScraperInterface {

	/** Name of file containing scraping results */
	private String fileName = "result-cc.csv";

	public CSVWriter writer;

	public static String baseUrl = null;

	public static int pagination = 0;

	/** Constructor */
	public SiteScraperCollaborativeConsumption(String baseUrl) {

		super();

		SiteScraperCollaborativeConsumption.baseUrl = baseUrl;

		if (initFile() == true) {
			System.out.println("Scraper initialized.");
		} else {
			System.out.println("Error initializing scraper!");
			System.exit(0);
		}
	}

	/**
	 * MAIN()
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

		SiteScraperInterface scraper = new SiteScraperCollaborativeConsumption(
				"http://www.collaborativeconsumption.com/directory/page/$$/");

		/*
		 * Max pages 48
		 * 06.10.2015
		 */
		scraper.scrape(48);

		scraper.finalize();
	}

	public void scrape(int maxPage) {

		/*
		 * We have {{Max pages}} pages on meshing.it, so iterate over! FIXME: Hardcoded
		 * pagination maxiumum
		 */
		for (int currentPage = 1; currentPage <= maxPage; currentPage++) {

			System.out.println("We are on page " + currentPage + ".");
			String currentUrl = baseUrl.replace("$$",
					String.valueOf(currentPage));
			System.out.println("Fetch: " + currentUrl);

			pagination = currentPage;
			this.getElements(currentUrl);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.wwu.ercis.scraper.SiteScraperInterface#getElements()
	 */
	@Override
	public String getElements(String page) {

		System.out.println("Scraping page " + pagination);

		// fetch page
		WebDriver driver = new HtmlUnitDriver();
		driver.get(page);

		/*
		 * DEBUG to print page source
		 */
		// System.out.println("\n"+ driver.getPageSource().toString() + "\n");

		/*
		 * At first, retrieve a list of links for every listed entry
		 */
		String link = "//div[@class='container services']/div[@class='row']/div[@class='span12']/section[@class='row-fluid']/div[@class='span2']/div[1]/div[@class='post-entry']/h3/a";
		List<WebElement> entriesLink = driver.findElements(By.xpath(link));

		System.out.println("We have found " + entriesLink.size()
				+ " links on page " + pagination + ".");

		List<String> entriesName = new ArrayList<String>();
		List<String> entriesUrl = new ArrayList<String>();
		List<String> entriesYear = new ArrayList<String>();
		List<String> entriesLocation = new ArrayList<String>();
		List<String> entriesInvest = new ArrayList<String>();
		List<String> entriesDescription = new ArrayList<String>();

		/*
		 * Now visit all links from and extract the URL of the respective
		 * listing * TODO: surround try-catch, because some sites dont have a
		 * link. See: http://meshing.it/companies/1592-977-Media-LLC
		 */
		Iterator<WebElement> iter = entriesLink.iterator();

		// iterate over all links in list
		while (iter.hasNext()) {

			// need to check, if page exists

			System.out.println("\nList of Years: " + entriesYear.size()
					+ "\nList of URLs: " + entriesUrl.size()
					+ "\nList of Locations: " + entriesLocation.size()
					+ "\nList of Investments: " + entriesInvest.size());

			WebElement current = iter.next();

			String link2follow = current.getAttribute("href");

			System.out.println("Currently scraping  " + link2follow + ".");

			// init new driver to visit page
			WebDriver goDriver = new HtmlUnitDriver();

			// hit new page
			goDriver.get(link2follow);

			/*
			 * If page was not found
			 */
			if (goDriver.getTitle().startsWith("Page Not Found")) {

				// add N/A for page not found
				entriesName.add("N/A");
				entriesLocation.add("N/A");
				entriesYear.add("N/A");
				entriesInvest.add("N/A");
				entriesUrl.add("N/A");
				entriesDescription.add("N/A");

			} else {

				/*
				 * Page is available now extract specific information from
				 * repsective page.
				 */

				/*
				 * Get description
				 */
				try {
					String xpath = "//div[@class='post']/*";
					
					// before scraping, wait until DOM is fully loaded
					WebDriverWait wait = new WebDriverWait(goDriver,5);
					WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
					
					String result = element.getText();

					if (!result.isEmpty()) {
						System.out.println("Description: " + result);
						entriesDescription.add(result);
					} else {
						System.out.println("Description: N/A");
						entriesDescription.add("N/A");
					}

				} catch (Exception e) {
					System.out.println("Description: N/A");
					entriesDescription.add("N/A");
				}

				/*
				 * Get names
				 */
				try {
					String xpath = "//h2[@class='directory-title']";
					String result = goDriver.findElement(By.xpath(xpath)).getText();	

					if (!result.isEmpty()) {
						System.out.println("Name: " + result);
						entriesName.add(result);
					} else {
						System.out.println("Name: N/A");
						entriesName.add("N/A");
					}

				} catch (Exception e) {
					System.out.println("Name: N/A");
					entriesName.add("N/A");
				}

				/*
				 * Get locations
				 */
				try {
					// check if 'Location' exists
					String check = goDriver.findElement(By.xpath("//p[@class='info']/strong[contains(.,'Location')]")).getText();
					if (!check.isEmpty()) {
						String xpath = "//p[@class='info']";
						String temp = goDriver.findElement(By.xpath(xpath)).getText();
						String result0 = temp.replaceAll("(Location:\\s)(.+)(\\n)*(.*\\n*)*", "$2");
						String result = result0.replaceAll(",","");
						
						if (!result.isEmpty()) {
							System.out.println("Location: " + result);
							entriesLocation.add(result);
						} else {
							System.out.println("Location: N/A");
							entriesLocation.add("N/A");
						}
					} else {
						System.out.println("Location: N/A");
						entriesLocation.add("N/A");
					}
				} catch (Exception e) {
					System.out.println("Location: N/A");
					entriesLocation.add("N/A");
				}

				/*
				 * Get Year
				 */
				try {
					//check if 'Year of Origin' exists
					String check1 = goDriver.findElement(By.xpath("//p[@class='info']/strong[contains(.,'Year of Origin')]")).getText();
					
					String xpath = "//p[@class='info']";
					String temp = goDriver.findElement(By.xpath(xpath)).getText();
					String result = temp.replaceAll("(.+\\n)*(Year of Origin:\\s)(.+)(\\n)*(.*\\n*)*$", "$3");
					if (!result.isEmpty()) {
						System.out.println("Year: " + result);
						entriesYear.add(result);
					} else {
						System.out.println("Year: N/A");
						entriesYear.add("N/A");
					}

				} catch (Exception e) {
					System.out.println("Year: N/A");
					entriesYear.add("N/A");
				}

				/*
				 * Get Invest
				 */
				try {
					//check if 'Investment' exists
					String check1 = goDriver.findElement(By.xpath("//p[@class='info']/strong[contains(.,'Investment')]")).getText();
					
					String xpath = "//p[@class='info']";
					String temp = goDriver.findElement(By.xpath(xpath)).getText();
					String result = temp.replaceAll("(.+\\n)*(Investment:\\s)(.+)(\\n)*(.*\\n*)*$", "$3");
					if (!result.isEmpty()) {
						System.out.println("Invest: " + result);
						entriesInvest.add(result);
					} else {
						System.out.println("Invest: N/A");
						entriesInvest.add("N/A");
					}
					
				} catch (Exception e) {
					System.out.println("Invest: N/A");
					entriesInvest.add("N/A");
				}

				/*
				 * Try to find URL
				 */
				try {

					String linkText = "Visit Website";
					String result = goDriver.findElement(By.linkText(linkText)).getAttribute("href");

					if (!result.isEmpty()) {
						System.out.println("URL: " + result);
						entriesUrl.add(result);
					} else {
						System.out.println("URL: N/A");
						entriesUrl.add("N/A");
					}

				} catch (Exception e) {
					System.out.println("URL: N/A");
					entriesUrl.add("N/A");
				}
			}
			
			goDriver.quit();

		}

		/*
		 * Now aggregate all retrieved information
		 */

		System.out.println("\nList of Years: " + entriesYear.size()
				+ "\nList of URLs: " + entriesUrl.size()
				+ "\nList of Locations: " + entriesLocation.size()
				+ "\nList of Investments: " + entriesInvest.size()
				+ "\nList of Descriptions: " + entriesDescription.size());

		for (int index = 0; index < entriesUrl.size(); index++) {

			// print to console
			System.out.println("\nName:\t" + entriesName.get(index)
					+ "\nYear:\t" + entriesYear.get(index) + "\nWebsite:\t"
					+ entriesUrl.get(index) + "\nLocation:\t"
					+ entriesLocation.get(index) + "\nDescription:\t"
							+ entriesDescription.get(index)+"\n");

			// write to csv
			String[] currentLine = { entriesName.get(index),
					entriesUrl.get(index), entriesYear.get(index),
					entriesLocation.get(index), entriesInvest.get(index), entriesDescription.get(index) };

			writer.writeNext(currentLine);

			driver.quit();

			// Return
		}
		return link;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.wwu.ercis.scraper.SiteScraperInterface#init()
	 */
	@Override
	public boolean initFile() {
		try {

			writer = new CSVWriter(new FileWriter(fileName), ';');

			String[] header = { "name", "url", "year", "location", "invest", "description" };

			// writer header
			writer.writeNext(header);

			return true;

		} catch (Exception e) {
			System.out.println("Awwwww.");
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.wwu.ercis.scraper.SiteScraperInterface#finalize()
	 */
	@Override
	public void finalize() {

		try {
			writer.close();
			System.out.println("Stream closed.");

		} catch (IOException e) {
			System.out.println("Awwwww.");
			e.printStackTrace();
		}
	}

}