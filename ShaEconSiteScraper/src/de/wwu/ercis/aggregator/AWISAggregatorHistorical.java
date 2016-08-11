package de.wwu.ercis.aggregator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.wwu.ercis.ranker.AwisRequestor;

import org.jdom2.Namespace;

public class AWISAggregatorHistorical {	
	
	
	public static void main(String[] args) throws IOException, JDOMException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		File inputPath;
		File outputFile;
		/**do {
			System.out.println("Correct path of .xml input folder required:");
			String pathToXML = br.readLine();
			inputPath = new File(pathToXML);
		} 
		while (!inputPath.exists() || !inputPath.isDirectory()); // repeat while no valid folder was provided
		
		do {
			System.out.println("Correct path and filename of .csv output folder required:");
			String output = br.readLine();
			outputFile = new File(output);
		} while (!outputFile.exists() & outputFile.isDirectory()); // repeat while no valid folder and file location was provided
		
		System.out.println("For historical data, no split over countries exists,  \n so we need to extrapolate existing splits that we have. \n "
				+ "Enter at least one file for extrapolation. \n "
				+ "This file must have the first column for URLs, the second for total and the rest for countries");
		ArrayList<File> extrapolationFiles = new ArrayList<File>();
		String oneMore;
		do {
			File extrapolationFile;
			do {
				System.out.println("Correct path and filename of .csv input file required:");
				String extrapolation = br.readLine();
				extrapolationFile = new File(extrapolation);
			} while (!extrapolationFile.exists() | extrapolationFile.isDirectory());
			extrapolationFiles.add(extrapolationFile);
			System.out.println("Add another file? [y/n]:");
			oneMore = br.readLine();
		} while (oneMore.equals("y")); */
		
		// For texting only
		ArrayList<File> extrapolationFiles = new ArrayList<File>();
		extrapolationFiles.add(new File("ITER1_Extrapolation.csv"));
		extrapolationFiles.add(new File("ITER3_Extrapolation.csv"));
		inputPath = new File("./ITER2_AWIS_results_historical");
		outputFile = new File("ITER2_AWIS_results_historical_aggregated.csv");
		
		// extrapolate contributions of countries
		HashMap<String,HashMap<String, Double>> countryContributions = extrapolateContributions(extrapolationFiles);
		
		parseResults(inputPath, outputFile, countryContributions);
	}
	
	public static void parseResults(File input, File output, HashMap<String,HashMap<String, Double>> countryContributions) throws JDOMException, IOException {
		// save the results for all platforms
		ArrayList<PageResult> pageResults = new ArrayList<PageResult>();	
		// figure out for which countries data actually exists
		ArrayList<String> containedCountryCodes = new ArrayList<String>();
		int countURLUnavailable = 0;
		int countPageViewsUnavailable = 0;		
		
		// parse from xml
		int counter = 0;
		for (File file : input.listFiles()) {
			//System.out.println("Extracting data from file <" + file.getName() +">.");
			counter++;
			System.out.println("Parsing file " + counter + "/" + input.listFiles().length);
			
			// store results for this page
			PageResult result = new PageResult();
			
			SAXBuilder sax = new SAXBuilder();
			Namespace namespace = Namespace.getNamespace("awis", "http://awis.amazonaws.com/doc/2005-07-11");
			Document doc = sax.build(file);
			XPathFactory xpathfactory = XPathFactory.instance();
			
			// get URL
			XPathExpression<Element> urlPath = xpathfactory.compile(
					"//awis:Alexa/awis:TrafficHistory/awis:Site", Filters.element(), null, namespace);
			Element url = urlPath.evaluateFirst(doc);
			if (url == null) countURLUnavailable++;
			else {
				String urlString = url.getText();
				
				// this URL is formed different from the URL of URLinfo data (see other aggregator)
				// remove http://www. or https://www. if exists
				if (urlString.contains("www.")) urlString = urlString.substring(urlString.indexOf(".")+1, urlString.length());
				urlString = urlString.replace("/", "");
				result.setUrl(urlString);
			}
			
			// get traffic data for all queried 31 days
			XPathExpression<Element> trafficData = xpathfactory.compile(
					"//awis:Alexa/awis:TrafficHistory/awis:HistoricalData/awis:Data/awis:PageViews/awis:PerMillion", 
					Filters.element(), null, namespace);
			
			List<Element> allTrafficData = trafficData.evaluate(doc);
			if (allTrafficData.size() == 0) {
				countPageViewsUnavailable++;
			}
			else {
				double average = 0;
				for (Element e : allTrafficData) {
					String text = e.getText().replace(",", "");
					if (text.equals("")) {
						countPageViewsUnavailable++;
						continue;
					}
					average = average + Double.parseDouble(text);
				}
				average = average / allTrafficData.size();
				result.setPageViews(average+"");
			}
			pageResults.add(result);
			
		}

        System.out.println("Number of missing URLs: " + countURLUnavailable);
        System.out.println("Number of missing page views: " + countPageViewsUnavailable);
        
		pageResults = extrapolateCountryContributions(countryContributions, pageResults);
		
		// due to rounding, the absolute number of the countries might differ quite a lot from the overall total views
		// normalize in order to have fitting values
		pageResults = normalizePageResults(pageResults);
		
		// check out which countries are actually included
		for (PageResult page : pageResults) {
			for (String key : page.getCountryContributions().keySet()) {
				boolean contained = false;
				for (String s : containedCountryCodes) {
					if (key.equals(s)) {
						contained = true;
						continue;
					}
				}
				if (!contained) containedCountryCodes.add(key);
			}
		}
		
		FileWriter writer = new FileWriter(output);
		// output into csv
		ArrayList<String> inputFirstLine = new ArrayList<String>();
		inputFirstLine.add("URL");
		inputFirstLine.add("TotalViews");
		for (String countryCode : containedCountryCodes) inputFirstLine.add(countryCode);
		CustomWriter.writeLine(writer, inputFirstLine);
		counter = 0;
		for (PageResult result : pageResults) {
			counter++;
			System.out.println("Printing to output platform " + counter + "/" + input.listFiles().length);
			ArrayList<String> pageInput = new ArrayList<String>();
			if (result.getUrl() == null) pageInput.add("NA");
			else pageInput.add(result.getUrl());
			if (result.getPageViews() == null) pageInput.add("NA");
			else pageInput.add(result.getPageViews());
			for (String countryCode : containedCountryCodes) {
				String countryContribution;
				if (result.containsCountryContribution(countryCode) && result.getPageViews() != null) {
					countryContribution = result.getCountryContribution(countryCode);
				}
				else countryContribution = "0";
				pageInput.add(countryContribution);
			}
			CustomWriter.writeLine(writer, pageInput);
		}
		writer.flush();
        writer.close();

	}
	
	private static HashMap<String,HashMap<String,Double>> extrapolateContributions(ArrayList<File> extrapolationFiles) throws IOException {
		System.out.println("Extracting data from " + extrapolationFiles.size() + " files.");
		
		// For each URL, for each country get all contribution margins
		HashMap<String, HashMap<String, ArrayList<Double>>> preliminaryContributions = new HashMap<String, HashMap<String, ArrayList<Double>>>();
		int counter = 0;
		int urlCounter = 0;
		for (File file : extrapolationFiles) {
			counter++;
			System.out.println("Extrapolating data from file [" + counter+ "/" + extrapolationFiles.size() + "].");
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			// get country codes from first line
			String firstLine = br.readLine();
			String[] countryCodes = firstLine.split(","); // remember to ignore the first two elements
			
			while ((line = br.readLine()) != null) {
				String[] cells = line.split(",");
				String url = cells[0];
				Double totalViews;
				// if no data on total views available, continue (in this case, the double parsing simply fails since there should be an NA in the cell
				try {
					totalViews = Double.parseDouble(cells[1]);
				}
				catch (Exception e) {
					continue;
				}
				
				// if total views is zero, continue
				if (totalViews == 0) continue;
				
				// check if this URL occurred in another csv already
				String key = null;
				for (String s : preliminaryContributions.keySet()) {
					if (s.equals(url)) key = s;
				}
				
				HashMap<String, ArrayList<Double>> contributions;
				// if it occurred already, there exists a HashMap entry already
				if (key != null) contributions = preliminaryContributions.get(key);
				
				// if not, make new HashMap entry and increase count of urls
				else {
					contributions = new HashMap<String, ArrayList<Double>>();
					urlCounter++;
				}
				
				// for every country entry
				for (int i = 2; i < cells.length; i++) {
					// check if a country entry already exists
					String countryKey = null;
					for (String s : contributions.keySet()) {
						if (s.equals(countryCodes[i])) countryKey = s;
					}
					
					ArrayList<Double> contributionsForOneCountry;
					// if country already occured
					if (countryKey != null) contributionsForOneCountry = contributions.get(countryKey);
					else contributionsForOneCountry = new ArrayList<Double>();
					
					// add double contribution
					Double cont = Double.parseDouble(cells[i]);
					cont = cont / totalViews;
					contributionsForOneCountry.add(cont);
					
					// add updated array list to hash map
					if (countryKey != null) contributions.put(countryKey, contributionsForOneCountry);
					else contributions.put(countryCodes[i], contributionsForOneCountry);
				}
				
				// add contributions for this platform to the overall hash map
				if (key != null) preliminaryContributions.put(key, contributions);
				else preliminaryContributions.put(url, contributions);
			}
			br.close();
		}
		
		// aggregate percentage values
		HashMap<String, HashMap<String, Double>> finalContributions = new HashMap<String, HashMap<String, Double>>();
		for (String key1 : preliminaryContributions.keySet()) {
			HashMap<String, Double> finalContributionsForOneURL = new HashMap<String, Double>();
			for (String key2 : preliminaryContributions.get(key1).keySet()) {
				ArrayList<Double> allPercentageValues = preliminaryContributions.get(key1).get(key2);
				Double average = new Double(0);
				for (Double d : allPercentageValues) {
					average = average + d;
				}
				average = average / allPercentageValues.size();
				finalContributionsForOneURL.put(key2, average);
			}
			finalContributions.put(key1, finalContributionsForOneURL);
		}
		
		// testing
		/**
		for (String key1 : finalContributions.keySet()) {
			for (String key2 : finalContributions.get(key1).keySet()) {
				System.out.println("Found an average of " + finalContributions.get(key1).get(key2) + 
						"% for URL " + key1 + " for country " + key2);
			}
		} */
		
		System.out.println("Number of distinct URLs found with entries for page views: " +urlCounter);
		
		return finalContributions;
	}
	
	private static ArrayList<PageResult> extrapolateCountryContributions(
			HashMap<String, HashMap<String, Double>> extrapolationBasis, ArrayList<PageResult> pageResults) {
		
		// for every previously parsed page result
		int countNoExtrapolationData = 0;
		for (PageResult result : pageResults) {
			// check if page result has any traffic data
			if (result.getPageViews() == null) continue;
			
			// check if page result occurs in extrapolation data
			String key = null;
			for (String s : extrapolationBasis.keySet()) {
				if (s.equals(result.getUrl())) key = s;
			}
			
			// if no entry in the extrapolation data exists, continue
			if (key == null) {
				countNoExtrapolationData++;
				continue;
			}
			
			// get data for current url
			HashMap<String, Double> dataForOneSite = extrapolationBasis.get(key);
			HashMap<String, String> dataForOneSiteResult = new HashMap<String, String>();
			
			// multiply percentage value with absolute page views
			for (String key2 : dataForOneSite.keySet()) {
				double value = dataForOneSite.get(key2);
				if (value > 0) {
					value = value * Double.parseDouble(result.getPageViews());
					dataForOneSiteResult.put(key2, value+"");	
				}
			}
			result.putAllCountryContributions(dataForOneSiteResult);
		}
		System.out.println("Sites with no data for extrapolation: [" + countNoExtrapolationData + "/" + pageResults.size() + "]");
		return pageResults;
	}
	
	private static ArrayList<PageResult> normalizePageResults(ArrayList<PageResult> pageResults) {
		for (PageResult page : pageResults) {
			if (page.getPageViews() == null) continue;
			double target = Double.parseDouble(page.getPageViews());
			double is = 0;
			for (String s : page.getCountryContributions().keySet()) {
				is += Double.parseDouble(page.getCountryContributions().get(s));
			}
			double factor = is/target;
			for (String s : page.getCountryContributions().keySet()) {
				double temp = Double.parseDouble(page.getCountryContributions().get(s));
				temp = temp / factor;
				page.getCountryContributions().put(s, temp+"");
			}
		}
		return pageResults;
	}
	
	private static class PageResult {
		private String url;
		private String pageViews;
		private HashMap<String,String> countryContributions = new HashMap<String,String>();
		public String getUrl() {
			return url;
		}
		public HashMap<String,String> getCountryContributions() {
			return countryContributions;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getPageViews() {
			return pageViews;
		}
		public void setPageViews(String pageViews) {
			this.pageViews = pageViews;
		}
		public void putCountryContribution(String key, String contribution) {
			countryContributions.put(key, contribution);
		}
		public boolean containsCountryContribution(String key) {
			return countryContributions.containsKey(key);
		}
		public String getCountryContribution(String key) {
			return countryContributions.get(key);
		}
		public void putAllCountryContributions(HashMap<String,String> countryContributions) {
			this.countryContributions = countryContributions;
		}
	}
	
}
