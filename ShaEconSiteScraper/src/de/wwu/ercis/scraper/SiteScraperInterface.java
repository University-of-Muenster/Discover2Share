package de.wwu.ercis.scraper;

public interface SiteScraperInterface {

	/**
	 * 
	 * @return TODO: fix xpath expression
	 */
	public abstract String getElements(String page);

	/**
	 * Initialize the CSV file reporter
	 * 
	 * @return
	 */
	public abstract boolean initFile();

	/**
	 * Close the stream.
	 */
	public abstract void finalize();

	public abstract void scrape(int i);
	
}