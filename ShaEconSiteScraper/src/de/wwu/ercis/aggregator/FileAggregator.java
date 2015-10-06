package de.wwu.ercis.aggregator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import au.com.bytecode.opencsv.CSVWriter;

public class FileAggregator {

	private final static boolean DEBUG = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.wwu.ercis.scraper.SiteScraperInterface#init()
	 */

	/**
	 * Apply xpath magic to each file in dir
	 * 
	 * @param file
	 */
	@SuppressWarnings("unused")
	public static void extract(File file) {

		String filename = null;
		List<String> header = new ArrayList<String>();
		List<String> values = new ArrayList<String>();

		// System.out.println("Extracting data from file <" + file.getName() +
		// ">.");

		try {

			// build document from file
			SAXBuilder sax = new SAXBuilder();

			Namespace namespace = Namespace.getNamespace("awis",
					"http://awis.amazonaws.com/doc/2005-07-11");

			Document doc = sax.build(file);

			XPathFactory xpathfactory = XPathFactory.instance();

			XPathExpression<Element> rankPath = xpathfactory.compile(
					"//awis:Rank", Filters.element(), null, namespace);
			XPathExpression<Element> urlPath = xpathfactory.compile(
					"//awis:DataUrl", Filters.element(), null, namespace);
			XPathExpression<Element> countriesPath = xpathfactory.compile(
					"//awis:TrafficData", Filters.element(), null, namespace);
			XPathExpression<Element> monthPath = xpathfactory
					.compile(
							"//awis:UsageStatistics/awis:UsageStatistic[1]/awis:TimeRange/awis:Months",
							Filters.element(), null, namespace);
			XPathExpression<Element> CountriesPath = xpathfactory
					.compile(
							"//awis:Alexa/awis:TrafficData/awis:RankByCountry/child::*",
							Filters.element(), null, namespace);
			XPathExpression<Element> pageViewsPath = xpathfactory
					.compile(
							"//awis:Alexa/awis:TrafficData/awis:RankByCountry/awis:Country[1]/awis:Contribution/awis:PageViews",
							Filters.element(), null, namespace);

			String tmp = "//awis:Alexa/awis:TrafficData/awis:UsageStatistics/awis:UsageStatistic[1]/awis:PageViews/awis:PerMillion/awis:Value";
			// String contributionPageView =
			// "//awis:Alexa/awis:TrafficData/awis:RankByCountry/awis:Country/awis:Contribution/awis:PageViews";
			// String contributionPageView =
			// "//awis:Alexa/awis:TrafficData/awis:RankByCountry/awis:Country/awis:Contribution/child::*";
			String contributionPageView = "//awis:Alexa/awis:TrafficData/awis:RankByCountry/awis:Country/awis:Contribution/child::*";

			XPathExpression<Element> testPath = xpathfactory.compile(tmp,
					Filters.element(), null, namespace);
			XPathExpression<Element> contPath = xpathfactory.compile(
					contributionPageView, Filters.element(), null, namespace);

			// FINISHED
			Element rankResult = rankPath.evaluateFirst(doc);
			Element resultUrl = urlPath.evaluateFirst(doc);
			Element resultMonth = monthPath.evaluateFirst(doc);
			List<Element> resultsCountries = CountriesPath.evaluate(doc);

			// ---- TEST -----
			Element testres = pageViewsPath.evaluateFirst(doc);
			Element resultsPageViews = testPath.evaluateFirst(doc);
			List<Element> resultsCont = contPath.evaluate(doc);

			// ---- OUTPUT -----
			// System.out.println("-------");

			// System.out.println("TEST:\t" + testres.getText());

			// System.out.println("-------");
			header.add("URL");
			header.add("PAGEVIEWS");
			header.add("MONTH");
			header.add("RANK");

			filename = resultUrl.getText();
			values.add(resultUrl.getText());
			values.add(resultsPageViews.getValue());
			values.add(resultMonth.getText());
			values.add(rankResult.getText());

			// System.out.println("URL:\t" + resultUrl.getText());
			// System.out.println("VIEWS:\t" + resultsPageViews.getValue());
			// System.out.println("MONTH:\t" + resultMonth.getText());
			// System.out.println("RANK:\t" + rankResult.getText());

			List<String> finalCont = new ArrayList<String>();

			// copy over the page views to array
			for (Iterator iterator = resultsCont.iterator(); iterator.hasNext();) {

				Element element = (Element) iterator.next();

				if (element.getName() == "PageViews") {
					finalCont.add(element.getValue());
				}
			}

			if (finalCont.size() == resultsCountries.size()) {

				for (int index = 0; index < resultsCountries.size(); index++) {

					Element element = (Element) resultsCountries.get(index);

					List<Attribute> attributes = element.getAttributes();

					for (int i = 0; i < attributes.size(); i++) {

						Attribute attribute = (Attribute) attributes.get(i);

						System.out.println(attribute.getValue() + " "
								+ finalCont.get(index));
						// add contents and headers
						header.add(attribute.getValue());
						values.add(finalCont.get(index));

					}
				}
			}

		} catch (Exception e) {
			System.out.println("Something went wrong.");
			System.out.println(e.getMessage());
			// e.printStackTrace();
			// TODO: handle exception
		}

//		System.out.println("HEADER");
//		System.out.println(header);
//		System.out.println("VALUES");
//		System.out.println(values);

		
		
		String[] headerTmp = new String[header.size()];
		headerTmp = header.toArray(headerTmp);

		String[] valuesTmp = new String[values.size()];
		valuesTmp = values.toArray(valuesTmp);

// 		DEBUG
//		for (String s : headerTmp)
//			System.out.println(s);
//
//		for (String s : valuesTmp)
//			System.out.println(s);

		// write file
		CSVWriter writer;
		try {
			
			filename = "/Users/mo/Desktop/AwisResultsOut/out_"+filename;
			writer = new CSVWriter(new FileWriter(filename.concat(".csv")), ';');

			// writer header and values
			writer.writeNext(headerTmp);
			writer.writeNext(valuesTmp);
			
			// close
			writer.flush();
			writer.close();
			
			System.out.println("Stream closed.");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * List all files contained in folder
	 * 
	 * @param folder
	 */
	public static void listFilesForFolder(final File folder) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {

				if (!fileEntry.getName().contains("DS_St")) {
					System.out.println(fileEntry.getName());
					// call extract
					extract(fileEntry);
				}

			}
		}
	}

	/**
	 * MAIN
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		final File folder = new File("/Users/sbaar/Desktop/JBE_Alexa_Data");
		listFilesForFolder(folder);
	}

}
