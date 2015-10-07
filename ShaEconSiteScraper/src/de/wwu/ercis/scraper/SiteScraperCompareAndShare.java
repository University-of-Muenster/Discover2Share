package de.wwu.ercis.scraper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class SiteScraperCompareAndShare implements SiteScraperInterface {

	private final static Logger LOGGER = Logger.getLogger(SiteScraperCompareAndShare.class.getName()); 
	
	/** Name of file containing scraping results */
	private String fileName = "result-compare-n-share.csv";

	public CSVWriter writer;

	public static String baseUrl = null;

	public static int pagination = 0;

	/** Constructor */
	public SiteScraperCompareAndShare(String baseUrl) {

		super();

		LOGGER.log(Level.SEVERE, "Init");
		LOGGER.setLevel(Level.SEVERE);
		
		SiteScraperCompareAndShare.baseUrl = baseUrl;

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

		SiteScraperInterface scraper = new SiteScraperCompareAndShare(
				"http://www.compareandshare.com/sharing-economy-directory/params/pageNo/$$/");

		/*
		 * Max pages 392
		 * 06.10.2015
		 */
		scraper.scrape(392);

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

		System.out.println("Currently scraping page " + pagination + " .");

		// Init new driver and get page
		WebDriver driver = new HtmlUnitDriver();
		driver.get(page);

		/*
		 * At first, retrieve a list of links for every listed entry
		 */			
		List<WebElement> entriesLink = driver.findElements(By.className("text-slate"));
		List<String> scrapeList = new ArrayList<String>();

		System.out.println("We have found " + entriesLink.size()
				+ " links on page " + pagination + ".");

		Iterator<WebElement> iter1 = entriesLink.iterator();

		while (iter1.hasNext()) {
			// need to check, if page exists
			WebElement current = iter1.next();
			scrapeList.add(current.getAttribute("href"));
		}

		/*
		 * Init data strucutre for results
		 */
		List<String> entriesName = new ArrayList<String>();
		List<String> entriesUrl = new ArrayList<String>();
		List<String> entriesCategory = new ArrayList<String>();
		List<String> entriesSubCategory = new ArrayList<String>();
		List<String> entriesPayment = new ArrayList<String>();
		List<String> entriesImpact = new ArrayList<String>();
		List<String> entriesTypeSharing = new ArrayList<String>();
		List<String> entriesBusinessModel = new ArrayList<String>();
		List<String> entriesLocation = new ArrayList<String>();
		List<String> entriesFounded = new ArrayList<String>();
		List<String> entriesResource = new ArrayList<String>();
		List<String> entriesDelivery = new ArrayList<String>();
		List<String> entriesInnovation = new ArrayList<String>();
		List<String> entriesDescription = new ArrayList<String>();
		List<String> entriesTags = new ArrayList<String>();

		/*
		 * Now visit all links and scrape contents
		 */
		Iterator<String> iter = scrapeList.iterator();

		int count = 1;
		while (iter.hasNext()) {

			// get next link from list
			String currentLink = iter.next();

			System.out.println("\nCurrently scraping (" + count + "/"
					+ scrapeList.size() + ") : " + currentLink + ".");

			// init new driver to visit page
			WebDriver goDriver = new HtmlUnitDriver();

			// hit new page
			goDriver.get(currentLink);
			
			/*
			 * Get Category
			 */
			try {
				
				String pathCategory = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Category:')]/following::span[1]/a";

				WebDriverWait wait = new WebDriverWait(goDriver,10);
				WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(pathCategory)));
				
				String resultCat = element.getText();
				
				if (!resultCat.isEmpty()) {
					entriesCategory.add(resultCat);
					System.out.println("Category: " + resultCat);
				} else {
					entriesCategory.add("N/A");
					System.out.println("Category: N/A");
				}
			} catch (Exception e) {
				System.out.println(e);
				System.out.println(currentLink + " failed.");
				System.out.println("Category: N/A");
				entriesCategory.add("N/A");
			}

			/*
			 * Get foundation date
			 */
			try {
				String xpath = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Founded:')]/following::span[1]";
				String result = goDriver.findElement(By.xpath(xpath)).getText();

				if (!result.isEmpty()) {
					entriesFounded.add(result);
					System.out.println("Founded: " + result);
				} else {
					entriesFounded.add("N/A");
					System.out.println("Founded: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Founded: N/A");
				entriesFounded.add("N/A");
			}

			/*
			 * Get description
			 */
			try {
				String xpath = "//p[@itemprop='description']";
				String result = goDriver.findElement(By.xpath(xpath)).getText();

				if (!result.isEmpty()) {
					entriesDescription.add(result);
					System.out.println("Description: " + result);
				} else {
					entriesDescription.add("N/A");
					System.out.println("Description: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Description: N/A");
				entriesDescription.add("N/A");
			}

			/*
			 * Get innovation date
			 */
			try {
				String xpath = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Innovation:')]/following::span[1]";
				String result = goDriver.findElement(By.xpath(xpath)).getText();

				if (!result.isEmpty()) {
					entriesInnovation.add(result);
					System.out.println("Innovation: " + result);
				} else {
					entriesInnovation.add("N/A");
					System.out.println("Innovation: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Innovation: N/A");
				entriesInnovation.add("N/A");
			}

			/*
			 * Get resource type
			 */
			try {
				String xpath = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Resource Type:')]/following::span[1]";
				String result = goDriver.findElement(By.xpath(xpath)).getText();

				if (!result.isEmpty()) {
					entriesResource.add(result);
					System.out.println("Resource Type: " + result);
				} else {
					entriesResource.add("N/A");
					System.out.println("Resource Type: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Resource Type: N/A");
				entriesResource.add("N/A");
			}

			/*
			 * Get delivery model
			 */
			try {
				String xpath = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Delivery Model:')]/following::span[1]";
				String result = goDriver.findElement(By.xpath(xpath)).getText();

				if (!result.isEmpty()) {
					entriesDelivery.add(result);
					System.out.println("Resource Type: " + result);
				} else {
					entriesDelivery.add("N/A");
					System.out.println("Delivery Model: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Delivery Model: N/A");
				entriesDelivery.add("N/A");
			}

			/*
			 * Get Location
			 */
			try {
				String pathLocation = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Location:')]/following::span[1]";
				String resultLocation = goDriver.findElement(
						By.xpath(pathLocation)).getText();

				if (!resultLocation.isEmpty()) {
					entriesLocation.add(resultLocation);
					System.out.println("Location: " + resultLocation);
				} else {
					entriesLocation.add("N/A");
					System.out.println("Location: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Location: N/A");
				entriesLocation.add("N/A");
			}

			/*
			 * Get Sub-Category
			 */
			try {
				String pathSubCategory = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Sub Category:')]/following::span[1]";
				String resultSubCat = goDriver.findElement(
						By.xpath(pathSubCategory)).getText();

				if (!resultSubCat.isEmpty()) {
					entriesSubCategory.add(resultSubCat);
					System.out.println("Sub-Category: " + resultSubCat);
				} else {
					entriesSubCategory.add("N/A");
					System.out.println("Sub-Category: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Sub-Category: N/A");
				entriesSubCategory.add("N/A");

			}

			/*
			 * Get Payment Type
			 */
			try {
				String pathPayment = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Payment Type:')]/following::span[1]";
				String resultPayment = goDriver.findElement(
						By.xpath(pathPayment)).getText();

				if (!resultPayment.isEmpty()) {
					entriesPayment.add(resultPayment);
					System.out.println("Payment Type: " + resultPayment);
				} else {
					entriesPayment.add("N/A");
					System.out.println("Payment Type: N/A");
				}

			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Payment Type: N/A");
				entriesPayment.add("N/A");
			}

			/*
			 * Get URL for webiste
			 */

			try {
				String pathUrl = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//a[@id='websiteCLicks']";
				String resultUrl = goDriver.findElement(By.xpath(pathUrl))
						.getAttribute("href");

				if (!resultUrl.isEmpty()) {
					entriesUrl.add(resultUrl);
					System.out.println("URL: " + resultUrl);
				} else {
					entriesUrl.add("N/A");
					System.out.println("URL: N/A");
				}

			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("URL: N/A");
				entriesUrl.add("N/A");
			}

			/*
			 * Get Impact
			 */
			try {
				String pathImpact = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Impacts:')]/following::span[1]";
				String resultImpact = goDriver
						.findElement(By.xpath(pathImpact)).getText();

				if (!resultImpact.isEmpty()) {
					entriesImpact.add(resultImpact);
					System.out.println("Impact: " + resultImpact);
				} else {
					entriesImpact.add("N/A");
					System.out.println("Impact: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Impact: N/A");
				entriesImpact.add("N/A");
			}

			/*
			 * Get Type of Sharing
			 */

			try {
				String pathTypeSharing = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Type of Sharing:')]/following::span[1]";
				String resultTypeSharing = goDriver.findElement(
						By.xpath(pathTypeSharing)).getText();

				if (!resultTypeSharing.isEmpty()) {
					entriesTypeSharing.add(resultTypeSharing);
					System.out.println("Type of Sharing: " + resultTypeSharing);
				} else {
					entriesTypeSharing.add("N/A");
					System.out.println("Type of Sharing: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Type of Sharing: N/A");
				entriesTypeSharing.add("N/A");
			}
			
			/*
			 * Get Tags
			 */
			try {
				
				String xpath = "//a[starts-with(@href,'/sharing-economy-directory/listing/tags/')]";
				
				List<WebElement> results = goDriver.findElements(
						By.xpath(xpath));
				
				Iterator<WebElement> iterator = results.iterator();

				String result = "";
				
				while(iterator.hasNext()){
					WebElement current = iterator.next();
					String tmp = current.getText();
					result += " "+tmp;
				}
								

				if (!result.isEmpty()) {
					entriesTags.add(result);
					System.out.println("Tags: " + result);
				} else {
					entriesTags.add("N/A");
					System.out.println("Tags: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Tags: N/A");
				entriesTags.add("N/A");
			}
			
			/*
			 * Get Business Model
			 */

			try {
				String pathBusinessModel = "//div[contains(concat(' ', @class, ' '), ' directoryLinks ')]//span/descendant::text()[starts-with(.,'Business Model:')]/following::span[1]";
				String resultBusinessModel = goDriver.findElement(
						By.xpath(pathBusinessModel)).getText();

				if (!resultBusinessModel.isEmpty()) {
					entriesBusinessModel.add(resultBusinessModel);
					System.out.println("Business Model: " + resultBusinessModel);
				} else {
					entriesBusinessModel.add("N/A");
					System.out.println("Business Model: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Business Model: N/A");
				entriesBusinessModel.add("N/A");
			}

			/*
			 * Get Name
			 */
			try {
				String pathName = "//h2[@itemprop='name']";
				String resultName = goDriver.findElement(By.xpath(pathName))
						.getText();

				if (!resultName.isEmpty()) {
					entriesName.add(resultName);
					System.out.println("Name: " + resultName);
				} else {
					entriesName.add("N/A");
					System.out.println("Name: N/A");
				}
			} catch (Exception e) {
				System.out.println(currentLink + " failed.");
				System.out.println("Name: N/A");
				entriesName.add("N/A");
			}

			System.out.println("\tList Compare:            "
					+ "\n\tCategories:\t"
					+ entriesCategory.size()
					+ "\n\tSub-Categories:\t"
					+ entriesSubCategory.size()
					+ "\n\tBusiness Model:\t"
					+ entriesBusinessModel.size()
					+ "\n\tImpact:\t\t"
					+ entriesImpact.size()
					+ "\n\tWebsite:\t"
					+ entriesUrl.size()
					+ "\n\tPaymeny:\t"
					+ entriesPayment.size()
					+ "\n\tType Sharing:\t"
					+ entriesTypeSharing.size()
					+ "\n\tNames:\t\t"
					+ entriesName.size()
					+ "\n\tLocation:\t"
					+ entriesLocation.size()
					+ "\n\tFounded:\t"
					+ entriesFounded.size()
					+ "\n\tResource:\t"
					+ entriesResource.size()
					+ "\n\tDelivery:\t"
					+ entriesDelivery.size()
					+ "\n\tTags:\t\t"
					+ entriesTags.size()
					+ "\n\tInnovation:\t"
					+ entriesInnovation.size()
					+ "\n\tDescription\t"
					+ entriesDescription.size());
			/*
			 * Now aggregate all retrieved information and write to file:
			 * entriesName entriesUrl entriesCategory entriesSubCategory
			 * entriesPayment entriesImpact entriesTypeSharing
			 * entriesBusinessModel entriesLocation entriesFounded
			 * entriesResource entriesDelivery entriesInnovation
			 * entriesDescription
			 */
			count++;

			goDriver.quit();

		}
		for (int index = 0; index < entriesUrl.size(); index++) {

			// write to csv
			String[] currentLine = { entriesName.get(index),
					entriesUrl.get(index), entriesCategory.get(index),
					entriesSubCategory.get(index), entriesPayment.get(index),
					entriesImpact.get(index), entriesTypeSharing.get(index),
					entriesBusinessModel.get(index),
					entriesLocation.get(index), entriesFounded.get(index),
					entriesResource.get(index), entriesDelivery.get(index),
					entriesInnovation.get(index), entriesTags.get(index), entriesDescription.get(index) };

			writer.writeNext(currentLine);
		}

		driver.quit();
		LOGGER.log(Level.SEVERE, "Finished scraping page "+pagination);
		return "Success";
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

			String[] header = { "Name", "Website", "Category", "Sub Category",
					"Payment Type", "Impacts", "Type of Sharing",
					"Business Model", "Location", "Founded", "Resource Type",
					"Delivery Model", "Innovation", "Tags", "Description" };

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
			LOGGER.severe("Done");

		} catch (IOException e) {
			System.out.println("Awwwww.");
			e.printStackTrace();
		}
	}

}