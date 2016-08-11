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

public class AWISAggregatorURL {	
	
	
	public static void main(String[] args) throws IOException, JDOMException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		File inputFile;
		File outputFile;
		do {
			System.out.println("Correct path of .xml input folder required:");
			String pathToXML = br.readLine();
			inputFile = new File(pathToXML);
		} 
		while (!inputFile.exists() & inputFile.isDirectory()); // repeat while no valid folder was provided
		
		do {
			System.out.println("Correct path and filename of .csv output folder required:");
			String output = br.readLine();
			outputFile = new File(output);
		} while (!outputFile.exists() & outputFile.isDirectory()); // repeat while no valid folder was provided */
		//inputFile = new File("./ITER3_AWIS_results");
		//outputFile = new File("./AWIS_result_parsed/AWIS_results.csv");
		parseResults(inputFile, outputFile);
	}
	
	public static void parseResults(File input, File output) throws JDOMException, IOException {
		// save the results for all platforms
		ArrayList<PageResult> pageResults = new ArrayList<PageResult>();		
		// get all existing country codes in the world from csv
		ArrayList<String> countryCodes = getCountryCodes();
		// figure out for which countries data actually exists
		ArrayList<String> containedCountryCodes = new ArrayList<String>();
		// Add "O" for other
		countryCodes.add("O");
		int countURLUnavailable = 0;
		int countPageViewsUnavailable = 0;
		int countCountryContributionUnavailable = 0;
		
		
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
					"//awis:DataUrl", Filters.element(), null, namespace);
			Element url = urlPath.evaluateFirst(doc);
			if (url == null) countURLUnavailable++;
			else {
				String urlString = url.getText();
				result.setUrl(urlString);
			}
			
			// get total traffic
			XPathExpression<Element> pageViewsPath = xpathfactory.compile(
					"//awis:Alexa/awis:TrafficData/awis:UsageStatistics/awis:UsageStatistic[1]/awis:PageViews/awis:PerMillion/awis:Value", 
					Filters.element(), null, namespace);
			Element pageViews = pageViewsPath.evaluateFirst(doc);
			if (pageViews == null) countPageViewsUnavailable++;
			else {
				String pageViewsValue = pageViews.getValue();
				result.setPageViews(pageViewsValue);
			}
			
			// get countries contribution
			// iterate over all existing countries
			boolean countryAvailable = false;
			for (String countryCode : countryCodes) {
				XPathExpression<Element> countriesContributionPath = xpathfactory.compile(
						"//awis:Alexa/awis:TrafficData/awis:RankByCountry/awis:Country[@Code='"+countryCode+"']/awis:Contribution/awis:PageViews",
						Filters.element(), null, namespace);
				List<Element> countryContributionsValue = countriesContributionPath.evaluate(doc);
				
				// if for this country an entry exists
				if (countryContributionsValue.size() > 0) {
					countryAvailable = true;
					result.putCountryContribution(countryCode, countryContributionsValue.get(0).getText());
					// update data on countries that exist in the results
					if (!containedCountryCodes.contains(countryCode)) containedCountryCodes.add(countryCode);
				}			
			}
			if (!countryAvailable) countCountryContributionUnavailable++;
			pageResults.add(result);
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
					countryContribution = parseAbsolute(result.getPageViews(), result.getCountryContribution(countryCode))+"";
				}
				else countryContribution = "0";
				pageInput.add(countryContribution);
			}
			CustomWriter.writeLine(writer, pageInput);
		}
		writer.flush();
        writer.close();

        System.out.println("Number of missing URLs: " + countURLUnavailable);
        System.out.println("Number of missing page views: " + countPageViewsUnavailable);
        System.out.println("Number of missing country contributions: " + countCountryContributionUnavailable);
	}
	
	private static ArrayList<String> getCountryCodes() {
		ArrayList<String> allCountryCodes = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("countrycodes.csv"));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line;
		try {
			while ((line = br.readLine()) != null) {
				allCountryCodes.add(line);		
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return allCountryCodes;
	}
	
	private static double parseAbsolute(String value, String percentage) {
		double doubleValue = Double.parseDouble(value);
		double percentageValue = Double.parseDouble(percentage.replace("%", ""));
		return doubleValue * (percentageValue/100);
	}
	
	private static class PageResult {
		private String url;
		private String pageViews;
		private HashMap<String,String> countryContributions = new HashMap<String,String>();
		public String getUrl() {
			return url;
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
	}
	
}
