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
 * {@link SiteScraperThePeopleWhoShare} is used to scrape content from web sites.
 * @author Marcus
 * FOR SOME REASON, THE WEBSITE DISCONNECTS SELENIUM EVERY ONCE IN A WHILE
 * THEREFORE, SCRAPING NEEDS TO BE RESTARTED MANUALLY AT LATER PAGE COUNTS
 * UNTIL ALL PAGES ARE SCRAPED
 */
public class SiteScraperThePeopleWhoShare implements SiteScraperInterface{

	/** Name of file containing scraping results */
	private String fileName = "result-TPWS.csv";

	public CSVWriter writer;

	public static String baseUrl = null;

	public static int pagination = 0;
	
	//lot of printlns required?
	private static final boolean VERBOSE = false;
	
	/** Constructor */
	public SiteScraperThePeopleWhoShare(String baseUrl) {

		super();

		SiteScraperThePeopleWhoShare.baseUrl = baseUrl;

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

		SiteScraperInterface scraper = new SiteScraperThePeopleWhoShare(
				"http://www.thepeoplewhoshare.com/sharing-economy-guide/search-results/params/pageNo/$$/");

		scraper.scrape(9999);

		scraper.finalize();
	}
	
	public void scrape(int maxPage) {

		//TODO have to manually assign pages as it is not possible to scrape all in one run
		for (int currentPage = 589; currentPage <= maxPage; currentPage++) {

			System.out.println("We are on page " + currentPage + ".");
			String currentUrl = baseUrl.replace("$$",
					String.valueOf(currentPage));
			System.out.println("Fetch: " + currentUrl);

			pagination = currentPage;
			String returnValue = this.getElements(currentUrl);
			if (returnValue.equals("done")) break;
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
		//String link = "//div[@class='container services']/div[@class='row']/div[@class='span12']/section[@class='row-fluid']/div[@class='span2']/div[1]/div[@class='post-entry']/h3/a";
		//iterate through all 9 results of a page
		List<WebElement> entriesLink = new ArrayList<WebElement>();
		//add here just for having a return value
		String link = "//*[@id='resultsPanel']/div[1]/div[1]/article/div[1]/a";
		for (int i = 1; i < 10; i++) {
			link = "//*[@id='resultsPanel']/div[1]/div["+i+"]/article/div[1]/a";
			entriesLink.addAll(driver.findElements(By.xpath(link)));
		}
		
		//if we reached the end of existing entries
		//somehow dirty workaround with a return value
		if (entriesLink.size() == 0) {
			return "done";
		}
		System.out.println("We have found " + entriesLink.size()
				+ " links on page " + pagination + ".");

		List<String> entriesName = new ArrayList<String>();
		List<String> entriesUrl = new ArrayList<String>();
		List<String> entriesCountry = new ArrayList<String>();
		List<String> entriesLocation = new ArrayList<String>();
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

			WebElement current = iter.next();

			String link2follow = current.getAttribute("href");

			System.out.println("Currently scraping  " + link2follow + ".");

			// init new driver to visit page
			WebDriver goDriver = new HtmlUnitDriver();

			// hit new page
			// added try catch block because some links throw java.net.SocketTimeoutException: Read timed out
			// TODO fix this instead of building a workaround
			try {
				goDriver.get(link2follow);
			}
			catch (Exception e){
				System.out.println("Website " + link2follow + " could not be caught");
			}
			
			//catch exceptions where goDriver is null (probably caused by invalid or currently unavailable links
			if (goDriver.getTitle() == null) continue;

			/*
			 * If page was not found
			 */
			if (goDriver.getTitle().startsWith("Page Not Found")) {

				// add N/A for page not found
				entriesName.add("N/A");
				entriesCountry.add("N/A");
				entriesLocation.add("N/A");
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
					String xpath = "html/body/section[2]/div/div[2]/div/p";
					
					// before scraping, wait until DOM is fully loaded
					WebDriverWait wait = new WebDriverWait(goDriver,5);
					WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
					
					String result = element.getText();
					if (VERBOSE) System.out.println(result);
					if (!result.isEmpty()) {
						if (VERBOSE) System.out.println("Description: " + result);
						entriesDescription.add(result);
					} else {
						if (VERBOSE) System.out.println("Description: N/A");
						entriesDescription.add("N/A");
					}

				} catch (Exception e) {
					if (VERBOSE) System.out.println("Description: N/A");
					entriesDescription.add("N/A");
				}

				/*
				 * Get names
				 */
				try {
					String xpath = "//*[@id='resultsPanel']/div/div[1]/article/div[1]/a";
					String result = goDriver.findElement(By.xpath(xpath)).getText();	

					if (!result.isEmpty()) {
						if (VERBOSE) System.out.println("Name: " + result);
						entriesName.add(result);
					} else {
						if (VERBOSE) System.out.println("Name: N/A");
						entriesName.add("N/A");
					}

				} catch (Exception e) {
					if (VERBOSE) System.out.println("Name: N/A");
					entriesName.add("N/A");
				}

				/*
				 * Get city
				 */
				try {
					String xpath = "html/body/section[2]/div/div[3]/div/div[2]/div[1]";
					WebElement element = goDriver.findElement(By.xpath(xpath));
					String location = element.getText();
					String[] locationDetails = location.split("\n");
					//find country
					String country = "N/A";
					for (int i = 0; i < locationDetails.length; i++) { 
						String s = locationDetails[i];
						if (s.contains("Country:")) {
							country = s.substring(9); //cut out "Country: "
							locationDetails[i] = ""; //remove country entry from array
							break;
						}
					}
					entriesCountry.add(country);
					
					//find other details and aggregate, since city cannot be extracted automatically
					//skip first element since it is just "Headquarters"
					String otherLocationDetails = "";
					for (int i = 1; i < locationDetails.length; i++) {
						otherLocationDetails += locationDetails[i] + " ";
					}
					otherLocationDetails = otherLocationDetails.trim();
					entriesLocation.add(otherLocationDetails);

				} catch (Exception e) {
					if (VERBOSE) System.out.println("Location: N/A");
					entriesLocation.add("N/A");
				}

				/*
				 * Try to find URL
				 */
				try {
					String xpath = "html/body/section[2]/div/div[3]/div/div[2]/div[2]/ul/li[1]/a";
					WebElement element = goDriver.findElement(By.xpath(xpath));					
					
					String result = element.getText();

					if (!result.isEmpty()) {
						if (VERBOSE) System.out.println("URL: " + result);
						entriesUrl.add(result);
					} else {
						if (VERBOSE) System.out.println("URL: N/A");
						entriesUrl.add("N/A");
					}

				} catch (Exception e) {
					if (VERBOSE) System.out.println("URL: N/A");
					entriesUrl.add("N/A");
				}
			}
			
			goDriver.quit();

		}

		/*
		 * Now aggregate all retrieved information
		 */

		if (VERBOSE) System.out.println("\nList of Years: " 
				+ "\nList of URLs: " + entriesUrl.size()
				+ "\nList of Locations: " + entriesLocation.size()
				+ "\nList of Investments: " 
				+ "\nList of Descriptions: " + entriesDescription.size());

		for (int index = 0; index < entriesUrl.size(); index++) {

			// print to console
			if (VERBOSE) System.out.println("\nName:\t" + entriesName.get(index)
					+ "\nYear:\t"  + "\nWebsite:\t"
					+ entriesUrl.get(index) + "\nLocation:\t"
					+ entriesLocation.get(index) + "\nDescription:\t"
							+ entriesDescription.get(index)+"\n");

			// write to csv
			String[] currentLine = { entriesName.get(index),
					entriesUrl.get(index), entriesLocation.get(index), entriesDescription.get(index) };

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
