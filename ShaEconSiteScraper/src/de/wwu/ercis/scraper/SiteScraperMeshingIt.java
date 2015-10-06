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
public class SiteScraperMeshingIt implements SiteScraperInterface {

	/** Name of file containing scraping results */
	private String fileName = "result-mesh.csv";

	public CSVWriter writer;

	public static String baseUrl = null;

	public static int pagination = 0;

	/** Constructor */
	public SiteScraperMeshingIt(String baseUrl) {

		super();

		SiteScraperMeshingIt.baseUrl = baseUrl;

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

		SiteScraperInterface scraper = new SiteScraperMeshingIt(
				"http://meshing.it/mesh_directories/browse_a_to_z?page=$$");

		/*
		 * Max pages 382
		 * 06.10.2015
		 */
		scraper.scrape(382);

		scraper.finalize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.wwu.ercis.scraper.SiteScraperInterface#getElements()
	 */
	@Override
	public String getElements(String page) {

		WebDriver driver = new HtmlUnitDriver();

		driver.get(page);

		/*
		 * At first, retrieve a list of links for every listed entry
		 */
		String link = "//li[@itemtype='http://schema.org/Organization']/div[@class='cat-content-right alr']/h2/a";
		List<WebElement> entriesLink = driver.findElements(By.xpath(link));

		System.out.println("We have found " + entriesLink.size()
				+ " links on page " + pagination + ".");

		List<String> entriesUrl = new ArrayList<String>();
		List<String> entriesName = new ArrayList<String>();
		List<String> entriesCategory = new ArrayList<String>();
		List<String> entriesLocation = new ArrayList<String>();
		List<String> entriesDescription = new ArrayList<String>();

		/*
		 * Now visit all links from and extract the URL of the respective
		 * listing * TODO: surround try-catch, because some sites dont have a
		 * link. See: http://meshing.it/companies/1592-977-Media-LLC
		 */

		Iterator<WebElement> i = entriesLink.iterator();
		String url = "//a[@itemprop='url']";

		// iterate over all links in list
		while (i.hasNext()) {

			System.out.println("\nList of URLs: " + entriesUrl.size()
					+ "\nList of Locations: " + entriesLocation.size()
					+ "\nList of Categories: " + entriesCategory.size()
					+ "\nList of Description: " + entriesDescription.size());

			WebElement current = i.next();

			String link2follow = current.getAttribute("href");

			System.out.println("Currently scraping  " + link2follow + ".");

			WebDriver goDriver = new HtmlUnitDriver();

			goDriver.get(link2follow);

			/*
			 * If no URL could be found, add N/A.
			 */
			try {
				WebElement currentUrl = goDriver.findElement(By.xpath(url));
				System.out.println("URL: " + currentUrl.getAttribute("href"));
				entriesUrl.add(currentUrl.getAttribute("href"));
			} catch (Exception e) {
				entriesUrl.add("N/A");
			}

			try {
				WebElement tmp = goDriver.findElement(By.xpath("//address"));
				System.out.println("\n" + tmp.getText() + "\n");
			} catch (Exception e) {
				System.out.println("Couldn't get text of address...");
			}

			/*
			 * get Description
			 */
			try {

				String selector = "//p[@itemprop='description']/text()";

				String data = (goDriver.getPageSource().toString());

				data = data.replace("<![CDATA[", "");
				data = data.replace("//<![CDATA[", "");
				data = data.replace("//]]>", "");
				data = data.replace("]]>", "");

				data = data.replace("&", "&amp;");

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

				// result = result.replaceAll(",", "");
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
				e.getLocalizedMessage();
				e.printStackTrace();
				System.out.println("Description: N/A");
				entriesDescription.add("N/A");
			}

			/*
			 * get Location
			 */
			try {

				String selector = "//p[@itemprop='address']/strong/text()";

				String data = (goDriver.getPageSource().toString());

				data = data.replace("<![CDATA[", "");
				data = data.replace("//<![CDATA[", "");
				data = data.replace("//]]>", "");
				data = data.replace("]]>", "");

				data = data.replace("&", "&amp;");

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

				// 2nd try
				if (result.isEmpty()) {

					System.out.println("Retry to get location!");

					String ex = "//a[@href='javascript:;']/text()[1]";
					XPath xpath2 = xpathFactory.newXPath();

					XPathExpression expr2 = xpath2.compile(ex);
					result = xpath2.evaluate(ex, doc);

					result = result.trim();
					result = result.replace("\n", "").replace("\r", "");

					System.out.println("\t1. Retry location: " + result);

					if (result.isEmpty()) {

						String kk = "html/body/div[1]/div[3]/div/div[3]/div[2]/ul/li/a/text()";

						XPath xpath3 = xpathFactory.newXPath();

						XPathExpression expr3 = xpath3.compile(ex);
						result = xpath3.evaluate(ex, doc);
						result = result.trim();
						result = result.replace("\n", "").replace("\r", "");

						System.out.println("\t\t2. Retry location: " + result);
					}

					if (result.isEmpty()) {

						String gg = "//a[@class='blue-over']/span[@class='light-grey']/following-sibling::text()";

						XPath xpath4 = xpathFactory.newXPath();

						XPathExpression expr4 = xpath4.compile(ex);
						result = xpath4.evaluate(ex, doc);
						result = result.trim();
						result = result.replace("\n", "").replace("\r", "");

						System.out
								.println("\t\t\t3. Retry location: " + result);
					}
				}

				result = result.trim();
				result = result.replace("\n", "").replace("\r", "");

				if (!result.isEmpty()) {
					System.out.println("Location: " + result);
					entriesLocation.add(result);
				} else {
					System.out.println("Location: N/A");
					entriesLocation.add("N/A");
				}

			} catch (Exception e) {
				e.getLocalizedMessage();
				e.printStackTrace();
				System.out.println("Name: N/A");
				entriesLocation.add("N/A");
			}

			/*
			 * get Name
			 */
			try {

				String selector = "//h1[@itemprop='name']/text()";

				String data = (goDriver.getPageSource().toString());

				data = data.replace("<![CDATA[", "");
				data = data.replace("//<![CDATA[", "");
				data = data.replace("//]]>", "");
				data = data.replace("]]>", "");

				data = data.replace("&", "&amp;");

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

				// result = result.replaceAll(",", "");
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
				e.getLocalizedMessage();
				e.printStackTrace();
				System.out.println("Name: N/A");
				entriesName.add("N/A");
			}

			/*
			 * Get category
			 */
			try {

				String selector = "//ul[@id='profile-category']/li/a/text()";
				String data = (goDriver.getPageSource().toString());

				data = data.replace("<![CDATA[", "");
				data = data.replace("//<![CDATA[", "");
				data = data.replace("//]]>", "");
				data = data.replace("]]>", "");

				data = data.replace("&", "&amp;");

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

				// result = result.replaceAll(",", "");
				result = result.trim();
				result = result.replace("\n", "").replace("\r", "");

				if (!result.isEmpty()) {
					System.out.println("Category: " + result);
					entriesCategory.add(result);
				} else {
					System.out.println("Category: N/A");
					entriesCategory.add("N/A");
				}

			} catch (Exception e) {
				e.getLocalizedMessage();
				e.printStackTrace();
				System.out.println("Category: N/A");
				entriesCategory.add("N/A");
			}
		}

		/*
		 * Now aggregate all retrieved information
		 */

		System.out.println("\nList of Categories: " + entriesCategory.size()
				+ "\nList of URLs: " + entriesUrl.size()
				+ "\nList of Locations: " + entriesLocation.size()
				+ "\nList of Descriptions: " + entriesDescription.size());

		for (int index = 0; index < entriesUrl.size(); index++) {

			// print to console
			System.out.println("\nName:\t" + entriesName.get(index)
					+ "\nURL:\t" + entriesUrl.get(index) + "\nCatergory:\t"
					+ entriesCategory.get(index) + "\nLocation:\t"
					+ entriesLocation.get(index) + "\nDescription:\t"
					+ entriesDescription.get(index));

			// write to csv
			String[] currentLine = { entriesName.get(index),
					entriesUrl.get(index), entriesCategory.get(index),
					entriesLocation.get(index), entriesDescription.get(index) };

			writer.writeNext(currentLine);
		}

		driver.quit();

		// Return
		return "";
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

			String[] header = { "name", "url", "category", "location",
					"description" };

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

	@Override
	public void scrape(int maxPage) {

		for (int currentPage = 1; currentPage <= maxPage; currentPage++) {

			System.out.println("We are on page " + currentPage + ".");
			String currentUrl = baseUrl.replace("$$",
					String.valueOf(currentPage));
			System.out.println("Fetch: " + currentUrl);

			pagination = currentPage;
			this.getElements(currentUrl);
		}
	}

}