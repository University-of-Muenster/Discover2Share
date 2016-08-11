package de.wwu.ercis.ranker;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Encoder;

/**
 * Makes a request to the Alexa Web Information Service UrlInfo action.
 */
public class AwisRequestor {

	private static final String ACTION_NAME = "UrlInfo";
	private static final String RESPONSE_GROUP_NAME = "Rank,RankByCountry,RankByCity,UsageStats,OwnedDomains,SiteData,Keywords,ContactInfo";
	private static final String SERVICE_HOST = "awis.amazonaws.com";
	private static final String AWS_BASE_URL = "http://" + SERVICE_HOST + "/?";
	private static final String HASH_ALGORITHM = "HmacSHA256";

	private static final String DATEFORMAT_AWS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private String accessKeyId;
	private String secretAccessKey;
	private String site;

	public AwisRequestor(String accessKeyId, String secretAccessKey, String site) {
		this.accessKeyId = accessKeyId;
		this.secretAccessKey = secretAccessKey;
		this.site = site;
	}

	/**
	 * Generates a timestamp for use with AWS request signing
	 *
	 * @param date
	 *            current date
	 * @return timestamp
	 */
	protected static String getTimestampFromLocalTime(Date date) {
		SimpleDateFormat format = new SimpleDateFormat(DATEFORMAT_AWS);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		return format.format(date);
	}

	/**
	 * Computes RFC 2104-compliant HMAC signature.
	 *
	 * @param data
	 *            The data to be signed.
	 * @return The base64-encoded RFC 2104-compliant HMAC signature.
	 * @throws java.security.SignatureException
	 *             when signature generation fails
	 */
	protected String generateSignature(String data)
			throws java.security.SignatureException {
		String result;
		try {
			// get a hash key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(
					secretAccessKey.getBytes(), HASH_ALGORITHM);

			// get a hasher instance and initialize with the signing key
			Mac mac = Mac.getInstance(HASH_ALGORITHM);
			mac.init(signingKey);

			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(data.getBytes());

			// base64-encode the hmac
			// result = Encoding.EncodeBase64(rawHmac);
			result = new BASE64Encoder().encode(rawHmac);

		} catch (Exception e) {
			throw new SignatureException("Failed to generate HMAC : "
					+ e.getMessage());
		}
		return result;
	}

	/**
	 * Makes a request to the specified Url and return the results as a String
	 *
	 * @param requestUrl
	 *            url to make request to
	 * @return the XML document as a String
	 * @throws IOException
	 */
	public static String makeRequest(String requestUrl) throws IOException {
		URL url = new URL(requestUrl);
		System.out.println(requestUrl);
		URLConnection conn = url.openConnection();
		InputStream in = conn.getInputStream();

		// Read the response
		StringBuffer sb = new StringBuffer();
		int c;
		int lastChar = 0;
		while ((c = in.read()) != -1) {
			if (c == '<' && (lastChar == '>'))
				sb.append('\n');
			sb.append((char) c);
			lastChar = c;
		}
		in.close();

		return sb.toString();
	}

	/**
	 * Builds the query string
	 */
	protected String buildQuery(boolean historical, String date) throws UnsupportedEncodingException {
		String timestamp = getTimestampFromLocalTime(Calendar.getInstance()
				.getTime());

		Map<String, String> queryParams = new TreeMap<String, String>();
		if (!historical) queryParams.put("Action", ACTION_NAME); // for URL info
		else queryParams.put("Action", "TrafficHistory"); // for historical data
		if (!historical) queryParams.put("ResponseGroup", RESPONSE_GROUP_NAME);
		else queryParams.put("ResponseGroup", "History"); // for historical data
		queryParams.put("AWSAccessKeyId", accessKeyId);
		queryParams.put("Timestamp", timestamp);
		queryParams.put("Url", site);
		queryParams.put("SignatureVersion", "2");
		queryParams.put("SignatureMethod", HASH_ALGORITHM);
		// for historical data
		if (historical) {
			queryParams.put("Start", date);
		}
		
		String query = "";
		boolean first = true;
		for (String name : queryParams.keySet()) {
			if (first)
				first = false;
			else
				query += "&";

			query += name + "="
					+ URLEncoder.encode(queryParams.get(name), "UTF-8");
		}

		return query;
	}

	/**
	 * Makes a request to the Alexa Web Information Service UrlInfo action
	 */
	public static void requestData(String accessKey, String secretKey, String site, boolean historical, String date, String outputFolder) throws Exception {

		AwisRequestor urlInfo = new AwisRequestor(accessKey, secretKey, site);

		String query = urlInfo.buildQuery(historical, date);

		String toSign = "GET\n" + SERVICE_HOST + "\n/\n" + query;

		System.out.println("String to sign:\n" + toSign + "\n");

		String signature = urlInfo.generateSignature(toSign);

		String uri = AWS_BASE_URL + query + "&Signature="
				+ URLEncoder.encode(signature, "UTF-8");

		System.out.println("Making request to:\n");
		System.out.println(uri + "\n");

		// Make the Request

		String xmlResponse = makeRequest(uri);

		// Print out the XML Response

		//System.out.println("Response:\n");
		//System.out.println(xmlResponse);

		// write response to file

		String tempName = site;
		tempName = tempName.replaceAll("\\/", "");
		tempName = tempName.replaceAll("\\\\", "");
		tempName = tempName.replaceAll("https", "");
		tempName = tempName.replaceAll("http", "");
		tempName = tempName.replaceAll("www.", "");
		tempName = tempName.replaceAll(":", "");
		tempName = outputFolder + "/" +tempName;
		
		System.out.println(tempName);

		Writer out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(tempName + "_awis.xml"), "UTF-8"));
		try {
			out.write(xmlResponse);
		} finally {
			out.close();
		}

	}
}
