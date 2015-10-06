package de.wwu.ercis.scraper;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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
		String link = "//div[@class='container services']/div[@class='row']/div[@class='span12']/section[@class='row-fluid']/div[@class='span2']/*/div[@class='thumb']/a";
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
					String selector = "//div[2]/div[3]/div/div[1]/div/article/div[2]/p/text()";
					String data = (goDriver.getPageSource().toString());

					data = data.replace("<![CDATA[", "");
					data = data.replace("//<![CDATA[", "");
					data = data.replace("//]]>", "");
					data = data.replace("]]>", "");

					InputSource source = new InputSource(new StringReader(data));

					DocumentBuilderFactory domFactory = DocumentBuilderFactory
							.newInstance();

					DocumentBuilder builder = domFactory.newDocumentBuilder();

					builder.isNamespaceAware();
					Document doc = builder.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();
					XPath xpath = xpathFactory.newXPath();
					XPathExpression expr = xpath.compile(selector);

					String result = xpath.evaluate(selector, doc);
					result = result.replaceAll(",", "");
					result = result.trim();
					result = result.replace("\n", "").replace("\r", "");

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
					// h2[@class='directory-title']
					String selector = "//h2[@class='directory-title']/text()";
					String data = (goDriver.getPageSource().toString());

					data = data.replace("<![CDATA[", "");
					data = data.replace("//<![CDATA[", "");
					data = data.replace("//]]>", "");
					data = data.replace("]]>", "");

					InputSource source = new InputSource(new StringReader(data));

					DocumentBuilderFactory domFactory = DocumentBuilderFactory
							.newInstance();

					DocumentBuilder builder = domFactory.newDocumentBuilder();

					builder.isNamespaceAware();
					Document doc = builder.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();
					XPath xpath = xpathFactory.newXPath();
					XPathExpression expr = xpath.compile(selector);

					String result = xpath.evaluate(selector, doc);
					result = result.replaceAll(",", "");
					result = result.trim();
					result = result.replace("\n", "").replace("\r", "");

					if (!result.isEmpty()) {
						System.out.println("Name: " + result);
						entriesName.add(result);
					} else {
						System.out.println("Name: N/A");
						entriesName.add("N/A");
					}

				} catch (Exception e) {
					// TODO: handle exception
				}

				/*
				 * Get locations
				 */
				try {

					String selector = "//p[@class='info']/strong[contains(.,'Location')]/following-sibling::text()[1]";
					String data = (goDriver.getPageSource().toString());

					data = data.replace("<![CDATA[", "");
					data = data.replace("//<![CDATA[", "");
					data = data.replace("//]]>", "");
					data = data.replace("]]>", "");

					InputSource source = new InputSource(new StringReader(data));

					DocumentBuilderFactory domFactory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder builder = domFactory.newDocumentBuilder();

					builder.isNamespaceAware();
					Document doc = builder.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();
					XPath xpath = xpathFactory.newXPath();
					XPathExpression expr = xpath.compile(selector);

					String result = xpath.evaluate(selector, doc);

					result = result.replaceAll(",", "");
					result = result.trim();
					result = result.replace("\n", "").replace("\r", "");

					if (!result.isEmpty()) {
						System.out.println("Location: " + result);
						entriesLocation.add(result);
					} else {
						System.out.println("Location: N/A " + result);
						entriesLocation.add("N/A");
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				/*
				 * Get Year
				 */
				try {

					String selector = "//p[@class='info']/strong[contains(.,'Year')]/following-sibling::text()[1]";
					String data = (goDriver.getPageSource().toString());

					data = data.replace("<![CDATA[", "");
					data = data.replace("//<![CDATA[", "");
					data = data.replace("//]]>", "");
					data = data.replace("]]>", "");

					InputSource source = new InputSource(new StringReader(data));

					DocumentBuilderFactory domFactory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder builder = domFactory.newDocumentBuilder();

					builder.isNamespaceAware();
					Document doc = builder.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();
					XPath xpath = xpathFactory.newXPath();

					XPathExpression expr = xpath.compile(selector);

					String result = xpath.evaluate(selector, doc);

					result = result.replaceAll(",", "");
					result = result.trim();
					result = result.replace("\n", "").replace("\r", "");

					if (!result.isEmpty()) {
						System.out.println("Year: " + result);
						entriesYear.add(result);
					} else {
						System.out.println("Year: N/A");
						entriesYear.add("N/A");
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				/*
				 * Get Invest
				 */
				try {

					String selector = "//p[@class='info']/strong[contains(.,'Invest')]/following-sibling::text()[1]";
					String data = (goDriver.getPageSource().toString());

					data = data.replace("<![CDATA[", "");
					data = data.replace("//<![CDATA[", "");
					data = data.replace("//]]>", "");
					data = data.replace("]]>", "");

					InputSource source = new InputSource(new StringReader(data));

					DocumentBuilderFactory domFactory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder builder = domFactory.newDocumentBuilder();

					builder.isNamespaceAware();
					Document doc = builder.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();
					XPath xpath = xpathFactory.newXPath();

					XPathExpression expr = xpath.compile(selector);

					String result = xpath.evaluate(selector, doc);

					result = result.replaceAll(",", "");
					result = result.trim();
					result = result.replace("\n", "").replace("\r", "");

					if (!result.isEmpty()) {
						System.out.println("Location: " + result);
						entriesInvest.add(result);
					} else {
						System.out.println("Location: N/A");
						entriesInvest.add("N/A");
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				/*
				 * Try to find URL
				 */
				try {

					String selector = "//p/a[@target='_blank']/@href";
					String data = (goDriver.getPageSource().toString());

					data = data.replace("<![CDATA[", "");
					data = data.replace("//<![CDATA[", "");
					data = data.replace("//]]>", "");
					data = data.replace("]]>", "");

					InputSource source = new InputSource(new StringReader(data));

					DocumentBuilderFactory domFactory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder builder = domFactory.newDocumentBuilder();

					builder.isNamespaceAware();
					Document doc = builder.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();
					XPath xpath = xpathFactory.newXPath();

					XPathExpression expr = xpath.compile(selector);

					String result = xpath.evaluate(selector, doc);

					result = result.replaceAll(",", "");
					result = result.trim();
					result = result.replace("\n", "").replace("\r", "");

					if (!result.isEmpty()) {
						System.out.println("URL: " + result);
						entriesUrl.add(result);
					} else {
						System.out.println("URL: N/A");
						entriesUrl.add("N/A");
					}

				} catch (Exception e) {
					entriesUrl.add("N/A");
					System.out.println("URL: N/A");
					e.printStackTrace();

				}
			}

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
					+ entriesUrl.get(index) + "\nCatergory:\t" + "Location:\t"
					+ entriesLocation.get(index) + "Description:\t"
							+ entriesDescription.get(index));

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