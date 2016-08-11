package de.wwu.ercis.ranker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * This funny little class invokes the {@link AwisRequestor} class with customized
 * args
 */
public class AlexaConnector {
	

	//ITERATION 1 20140801"
	//ITERATION 2 20151101
	//ITERATION 3 20160501

	public static void main(String[] args) throws IOException {

		BufferedReader br1 = new BufferedReader(new InputStreamReader(System.in));
		File urls;
		
		do {
			System.out.println("Correct path and filename of .csv url input file required:");
			String urlFileString = br1.readLine();
			urls = new File(urlFileString);
		} 
		while (!urls.exists() & !urls.isDirectory()); // repeat while no valid file was provided

		String outputFolder; 
		File folder;
		do {
			System.out.println("Correct output folder required:");
			outputFolder = br1.readLine();
			folder = new File(outputFolder);
		} 
		while (!folder.exists() || !folder.isDirectory()); // repeat while no valid folder was provided
		
		String historicalData;
		do {
			System.out.println("Historical data? [y/n]");
			historicalData = br1.readLine();
		} 
		while (!historicalData.equals("y") && !historicalData.equals("n"));
		boolean historical = (historicalData.equals("y")) ? true : false;
		
		String date = null;
		if (historical) {
			do {
				System.out.println("Enter the date for the historical snapshot [yyyymmdd]:");
				date = br1.readLine();
			} while (!checkDateConformity(date));
		}
		
		
		BufferedReader br2 = new BufferedReader(new FileReader(urls));
		String line;
		while ((line = br2.readLine()) != null) {
			// compose args for 
			
			// AKIAIICLE7RCKPKMMUPA 7Ym3vyutzxh/5rdqCAefDcQC0v/PhCJqtAeG5Fwp airbnb.com
			String accessKey = "AKIAIZ5MCTSM55SS4KNA";
			String secretKey = "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";
			String site = line;
			// invoke main of UrlInfo
			try {
				AwisRequestor.requestData(accessKey, secretKey, site, historical, date, outputFolder);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}			
		}
	}
	
	private static boolean checkDateConformity(String dateString) {
		SimpleDateFormat spdf = new SimpleDateFormat("yyyymmdd");
		try {
			Calendar requested = spdf.getCalendar();
			requested.setTime(spdf.parse(dateString));
			// date must be within the previous 4 years
			Calendar current = Calendar.getInstance();
			int diff = current.get(Calendar.YEAR) - requested.get(Calendar.YEAR);
			if (diff > 4) {
				System.out.println("Date must not be more than 4 years ago.");
				return false;
			}
			else if (diff < 0) {
				System.out.println("Date must be in the past.");
				return false;
			}
			else return true;
		} catch (ParseException e) {
			return false;
		}
		
	}
}
