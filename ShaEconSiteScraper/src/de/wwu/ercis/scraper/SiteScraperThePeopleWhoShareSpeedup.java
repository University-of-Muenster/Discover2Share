package de.wwu.ercis.scraper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * {@link SiteScraperThePeopleWhoShareSpeedup} is used to scrape content from web sites.
 * @author Marcus
 *
 */
public class SiteScraperThePeopleWhoShareSpeedup implements SiteScraperInterface{

	/** Name of file containing scraping results */
	private String fileName = "result-TPWS.csv";

	public CSVWriter writer;

	public static String baseUrl = null;

	public static int pagination = 0;
	
	private static final int NUMBER_OF_THREADS = 10;
	
	/** Constructor */
	public SiteScraperThePeopleWhoShareSpeedup(String baseUrl) {

		super();

		SiteScraperThePeopleWhoShareSpeedup.baseUrl = baseUrl;

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

		SiteScraperInterface scraper = new SiteScraperThePeopleWhoShareSpeedup(
				"http://www.thepeoplewhoshare.com/sharing-economy-guide/search-results/params/pageNo/$$/");

		scraper.scrape(10);

		scraper.finalize();
	}
	
	public void scrape(int maxPage) {

		ArrayList<String[]> returnValue = new ArrayList<String[]>();
		for (int currentPage = 1; currentPage <= maxPage; currentPage = currentPage + NUMBER_OF_THREADS) {
			System.out.println("We are on page " + currentPage + ".");
			ArrayList<ThePeopleWhoShareThread> threads = new ArrayList<ThePeopleWhoShareThread>();

			ExecutorService es = Executors.newFixedThreadPool(NUMBER_OF_THREADS);	
			for (int i = 0; i < NUMBER_OF_THREADS; i++) {
				String currentUrl = baseUrl.replace("$$",
						String.valueOf(currentPage+i));
				System.out.println("Fetch: " + currentUrl);
				threads.add(new ThePeopleWhoShareThread(currentUrl));
			}
			
			try {
				for (Future<String> result: es.invokeAll(threads)) {
					if (result.equals("done")) break;
					returnValue.add(result.get().split("\n"));
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		writer.writeAll(returnValue);
	}
	
	@Override
	public String getElements(String page) {return null;};
	
	@Override
	public boolean initFile() {
		try {

			writer = new CSVWriter(new FileWriter(fileName), ';');

			String[] header = { "name", "url", "country", "location", "description" };

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
