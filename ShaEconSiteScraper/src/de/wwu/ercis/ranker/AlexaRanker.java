package de.wwu.ercis.ranker;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * 
 */
public class AlexaRanker {

	/**
	 * API Key (Insight package)
	 */
	private final String API_KEY = "";
	
	/**
	 * 
	 */
	public static CSVWriter writer;

	/**
	 * Cotains all URLs to be checked at Alexa
	 */
	static List<String> sites = new ArrayList<String>();

	/**
	 * The Placeholder to be replaced by a specific URL
	 */
	static String placeHolder = "$$$";

	/**
	 * The URL is a template to query Alexa for specific Sites
	 */
	static String baseUrl = "http://data.alexa.com/data?cli=10&dat=snbamz&url="
			+ placeHolder;

	/**
	 * The path of the file containing all URLs
	 */
	private String pathToFile = "urls.csv";

	private String resultFile = "alexa_rank.csv";

	/**
	 * The Logger
	 */
	private final static Logger LOGGER = Logger.getLogger(AlexaRanker.class
			.getName());

	/**
	 * Constructor
	 */
	public AlexaRanker() {
		super();

		LOGGER.setLevel(Level.FINEST);

		LOGGER.log(Level.FINE, "Initializing Alexa Ranker...");

		readInUrlsFromCsv();
		System.out.println(sites.size());

		if (initFile() == true) {
			LOGGER.fine("Ranker initialized.");
		} else {
			LOGGER.fine("Error initializing scraper!");
			System.exit(0);
		}

	}

	public void readInUrlsFromCsv() {

		try {
			CSVReader reader = new CSVReader(new FileReader(pathToFile), ',');

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {

				sites.add(nextLine[0]);

				LOGGER.log(Level.FINEST, "Added <" + nextLine[0]
						+ "> to sites.");
			}
			// finished reading in
			reader.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Could not read in file...");
			e.printStackTrace();
		}
		LOGGER.fine("Read in " + sites.size() + " sites from CSV from <"
				+ pathToFile + ">.");
	}

	/**
	 * Opens a connection to Alexa and fetches data
	 */
	public static void getRank(String page, String url) {

		String result = null;
		String result_country = null;
		String result_country_rank = null;
		String result_delta = null;
		String result_pop = null;
		

		try {
			// Init new driver and get page
			WebDriver driver = new HtmlUnitDriver();
			driver.get(page);

			// String selector = "//SD/REACH/@RANK";

			String data = (driver.getPageSource().toString());

			InputSource source = new InputSource(new StringReader(data));

			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();

			builder.isNamespaceAware();
			Document doc = builder.parse(source);

			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xpath = xpathFactory.newXPath();

			// XPathExpression expr = xpath.compile(selector);

			result = xpath.evaluate("//SD/REACH/@RANK", doc);

			/*
			 * define and evaluate different XPATH expressions
			 */
			result_country = xpath.evaluate("//SD/COUNTRY/@RANK", doc);
			result_country_rank = xpath.evaluate("//SD/COUNTRY/@NAME", doc);
			result_delta = xpath.evaluate("//SD/RANK/@DELTA", doc);
			result_pop = xpath.evaluate("//SD/POPULARITY/@TEXT", doc);

			// test, if results were found
			if (result_country.isEmpty() && result_country_rank.isEmpty()) {
				result_country = "N/A";
				result_country_rank = "N/A";
			}

			if (result.isEmpty()) {
				result = "N/A";
			}

			if (result_pop.isEmpty()) {
				result_pop = "N/A";
			}

			if (result_delta.isEmpty()) {
				result_delta = "N/A";
			}

		} catch (Exception e) {
			System.out.println("Ewww");

			result_delta = "N/A";
			result = "N/A";
			result_country = "N/A";
			result_country_rank = "N/A";
			result_pop = "N/A";

			e.printStackTrace();
		}

		// create a new line for the csv
		String[] currentLine = { url, result, result_delta, result_country,
				result_country_rank, result_pop };

		// DEBUG
		LOGGER.fine("\nURL:\t\t" + url + "\nRank:\t\t" + result
				+ "\nDelta:\t\t" + result_delta + "\nCountry:\t"
				+ result_country + "\nRank Country:\t" + result_country_rank
				+ "\nPop:\t\t" + result_pop + "\n");

		// write line to csv
		writer.writeNext(currentLine);

	}

	/**
	 * 
	 * @return
	 */
	public boolean initFile() {
		try {

			writer = new CSVWriter(new FileWriter(resultFile), ';');

			String[] header = { "url", "reach_rank", "delta", "country",
					"country_rank", "populartity" };

			// writer header
			writer.writeNext(header);

			return true;

		} catch (Exception e) {
			LOGGER.fine("Awwwww.");
			return false;
		}
	}

	/**
	 * 
	 */
	public void finalize() {

		try {
			writer.close();
			LOGGER.fine("Stream closed.");
			LOGGER.severe("Done");

		} catch (IOException e) {
			LOGGER.fine("Awwwww.");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		AlexaRanker ranker = new AlexaRanker();

		/*
		 * iterate over all entries from csv and fetch data from Alexa
		 */

		for (int index = 0; index < sites.size(); index++) {

			// for (int index = 0; index < 10; index++) {

			String currentQuery = baseUrl
					.replace(placeHolder, sites.get(index));

			LOGGER.fine(currentQuery);

			ranker.getRank(currentQuery, sites.get(index));
		}
		// finish
		ranker.finalize();
	}
}
