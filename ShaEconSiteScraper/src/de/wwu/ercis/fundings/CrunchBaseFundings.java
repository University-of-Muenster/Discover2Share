package de.wwu.ercis.fundings;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

			String[] header = { "domain_name", "fundings" };

			// writer header
			writer.writeNext(header);

			return true;

		} catch (Exception e) {
			System.out.println("Nope. :-/");
			return false;
		}
	}
	
	
	/** CrunchBase API **/
	
	
	
	
	
	
	
	
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

			//cbf.getRank(currentQuery, sites.get(index));
		}
		// finish
		cbf.finalize();
	}
	

}
