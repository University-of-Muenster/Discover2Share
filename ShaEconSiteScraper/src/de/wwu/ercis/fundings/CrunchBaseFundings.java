package de.wwu.ercis.fundings;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class CrunchBaseFundings {
	
	/**
	 * API Key
	 */
	private final static String API_KEY = "6814d5c1a203934740e2652cd8c3ea1b";
	
	/**
	 * 
	 */
	public static CSVWriter writer;

	/**
	 * Cotains all URLs to be checked at CrunchBase
	 */
	static List<String> sites = new ArrayList<String>();

	/**
	 * The Placeholder to be replaced by a specific company URL
	 */
	static String placeHolder = "$$$";

	/**
	 * The URL is a template to query CrunchBase API for specific companies
	 */
	static String baseUrl = "https://api.crunchbase.com/v/3/organizations?domain_name=" + placeHolder + "&user_key=" + API_KEY;

	/**
	 * The path of the file containing all URLs
	 */
	private String pathToFile = "urls.csv";

	private String resultFile = "crunchbase_fundings.csv";
	
	/**
	 * Constructor
	 */
	public CrunchBaseFundings() {
		super();

		System.out.println("Initializing CrunchBase Fundings...");

		readInUrlsFromCsv();

		if (initFile() == true) {
			System.out.println("CrunchBase initialized.");
		} else {
			System.out.println("Error initializing scraper!");
			System.exit(0);
		}
	}
	
	public void readInUrlsFromCsv() {

		try {
			CSVReader reader = new CSVReader(new FileReader(pathToFile), ',');

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {

				// convert URL to CrunchBase domain_name
				String domain_name = nextLine[0].replaceFirst("^(http://|http://www\\.|www\\.)","");
				domain_name = domain_name.replaceFirst("^(http://|http://www\\.|www\\.)","");
				
				sites.add(domain_name);

				System.out.println("Added <" + nextLine[0]
						+ "> to sites.");
			}
			// finished reading in
			reader.close();
		} catch (Exception e) {
			System.out.println("Could not read in file...");
			e.printStackTrace();
		}
		System.out.println("Read in " + sites.size() + " sites from CSV from <"
				+ pathToFile + ">.");
	}
	
	public boolean initFile() {
		try {

			writer = new CSVWriter(new FileWriter(resultFile), ';');
			
			String[] header = { "url", "cb_homepage_url", "cb_name", "cb_short_description", "cb_founded_on", "cb_is_closed", 
					"cb_num_employees_min", "cb_num_employees_max", "cb_total_funding_usd", 
					"cb_funding_rounds", "cb_number_of_investments"};

			// writer header
			writer.writeNext(header);

			return true;

		} catch (Exception e) {
			System.out.println("Nope. :-/");
			return false;
		}
	}
	
	
	/** CrunchBase API **/
	
	/**
	 * Opens a connection to CrunchBase and fetches the organization
	 */
	public void getOrganizations(String page, String url) {

		try {
			// Init new driver and get page
			WebDriver driver = new HtmlUnitDriver();
			driver.get(page);

			String json = "";
			json = (driver.getPageSource().toString());
			Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
			
			Integer total_items = JsonPath.read(document, "$.data.paging.total_items");
			
			// only one item found
			if (total_items == 1) {
				System.out.println("--> 1 ITEM FOUND");
				// get permalink from CB
				String permalink = JsonPath.read(document, "$.data.items[0].properties.permalink");
				// get properties and fundings
				getPropertiesAndFundings(permalink, url);
			}
			// more than one item found
			else if (total_items > 1) {
				
			}
			// no item found
			else {
				getPropertiesAndFundings("N/A", url);
			}
		
		} catch (Exception e) {
			System.out.println("Ewww");
			e.printStackTrace();
		}
	}
	
	/**
	 * Opens a connection to CrunchBase and fetches the properties & fundings
	 */
	public void getPropertiesAndFundings(String permalink, String url) {
		String result_homepage_url = "N/A";
		String result_name = "N/A";
		String result_short_description = "N/A";
		String result_founded_on = "N/A";
		String result_is_closed = "N/A";
		String result_num_employees_min = "N/A";
		String result_num_employees_max = "N/A";
		String result_total_funding_usd = "N/A";
		String result_funding_rounds = "N/A";
		String result_number_of_investments = "N/A";
		
		if (permalink != "N/A") {
			try {
				// Init new driver and get page
				WebDriver driver = new HtmlUnitDriver();
				
				// get JSON from permalink
				String page_permalink = "https://api.crunchbase.com/v/3/organizations/"+ permalink +"?user_key=" + API_KEY;
				driver.get(page_permalink);
				String json = (driver.getPageSource().toString());
				Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
				
				String homepage_url = JsonPath.read(document, "$.data.properties.homepage_url");
				String name = JsonPath.read(document, "$.data.properties.name");
				String short_description = JsonPath.read(document, "$.data.properties.short_description");
				String founded_on = JsonPath.read(document, "$.data.properties.founded_on").toString();
				String is_closed = JsonPath.read(document, "$.data.properties.is_closed").toString();
				String num_employees_min = JsonPath.read(document, "$.data.properties.num_employees_min").toString();
				String num_employees_max = JsonPath.read(document, "$.data.properties.num_employees_max").toString();
				String total_funding_usd = JsonPath.read(document, "$.data.properties.total_funding_usd").toString();
				String funding_rounds = JsonPath.read(document, "$.data.relationships.funding_rounds.paging.total_items").toString();
				String number_of_investments = JsonPath.read(document, "$.data.properties.number_of_investments").toString();
				
				// save data
				result_homepage_url = homepage_url;
				result_name = name;
				result_short_description = short_description;
				result_founded_on = founded_on;
				result_is_closed = is_closed;
				result_num_employees_min = num_employees_min;
				result_num_employees_max = num_employees_max;
				result_total_funding_usd = total_funding_usd;
				result_funding_rounds = funding_rounds;
				result_number_of_investments = number_of_investments;

			
			} catch (Exception e) {
				System.out.println("Ewww");
				e.printStackTrace();
			}	
		}

		// create a new line for the csv
		String[] currentLine = { url, result_homepage_url, result_name, result_short_description, result_founded_on,
				result_is_closed, result_num_employees_min, result_num_employees_max, result_total_funding_usd,
				result_funding_rounds, result_number_of_investments };

		// write line to CSV
		writer.writeNext(currentLine);
	}
	
	
	
	/** Main **/
	
	public void finalize() {

		try {
			writer.close();
			System.out.println("Stream closed.");
			System.out.println("Done");

		} catch (IOException e) {
			System.out.println("Nope. :-/");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		CrunchBaseFundings cbf = new CrunchBaseFundings();

		/*
		 * iterate over all entries from csv and fetch data from CrunchBase
		 */

		for (int index = 0; index < sites.size(); index++) {

			String currentQuery = baseUrl
					.replace(placeHolder, sites.get(index));

			System.out.println("Index "+index+"/"+sites.size()+":");
			System.out.println(currentQuery);

			cbf.getOrganizations(currentQuery, sites.get(index));
		}
		// finish
		cbf.finalize();
	}
	

}
