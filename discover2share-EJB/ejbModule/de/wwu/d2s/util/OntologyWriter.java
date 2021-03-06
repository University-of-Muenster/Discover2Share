package de.wwu.d2s.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import de.wwu.d2s.ejb.OntologyServiceBean;
import de.wwu.d2s.jpa.GeoUnit;
import de.wwu.d2s.jpa.Platform;
import de.wwu.d2s.jpa.ResourceType;

/**
 * Transforms P2P SCC platform objects into instances of the Discover2Share ontology.
 */
public class OntologyWriter {

	private Logger log;

	private final String ENDPOINT = "http://localhost:3030/d2s-ont/query";
	private final String COUNTRIESJSON = "http://localhost:8080/discover2share-Web/resources/js/countries.json";

	// ontology namespaces
	private final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	private final String D2S = "http://www.discover2share.net/d2s-ont/";
	private final String DBPP = "http://dbpedia.org/property/";
	private final String DBPR = "http://dbpedia.org/resource/";
	private final String DBPO = "http://dbpedia.org/ontology/";
	private final String DCT = "http://purl.org/dc/terms/";
	private final String LGD = "http://linkedgeodata.org/ontology/";
	private final String SKOS = "http://www.w3.org/2004/02/skos/core#";
	private final String TIME = "http://www.w3.org/2006/time#";
	private final String WORDNET = "http://wordnet-rdf.princeton.edu/wn31/";
	private final String OWL = "http://www.w3.org/2002/07/owl#";

	// the central element for output
	private OntModel ontologyModel;

	// current platform object and RDF resource centrally available for all methods
	private Resource platformResource;
	private Platform platform;

	// Maps to avoid unnecessary AJAX calls to geonames.org
	private Map<String, String> countryIndex = new HashMap<String, String>();

	// properties available centrally for all methods and platforms to only require one instantiation
	private Property rdfType;
	private Property rdfsLabel;
	private Property dbppUrl;
	private Property hasResourceType;
	private Property promotes;
	private Property hasPattern;
	private Property hasTemporality;
	private Property hasMarketMediation;
	private Property accessedObjectHasType;
	private Property hasResourceOwner;
	private Property minServiceDuration;
	private Property maxServiceDuration;
	private Property hasConsumerInvolvement;
	private Property hasMoneyFlow;
	private Property hasMarketIntegration;
	private Property marketsAre;
	private Property hasScope;
	private Property launchYear;
	private Property launchedIn;
	private Property operatorResidesIn;
	private Property locationCity;
	private Property locationCountry;
	private Property hasTrustContribution;
	private Property hasApp;
	private Property owlSameAs;
	private Property dbppLanguage;
	private Property rdfsComment;
	private Property rdfsSubClassOf;
	private Property rdfsSeeAlso;

	// resources available centrally for all methods and platforms to only require one instantiation
	private Resource yearClass;
	private Resource cityClass;
	private Resource resourceTypeClass;
	private Resource p2pSccPlatformClass;
	private Resource once;
	private Resource often;
	private Resource socialConsumerism;
	private Resource economicConsumerism;
	private Resource environmentalConsumerism;
	private Resource noConsumerism;
	private Resource deferredPattern;
	private Resource immediatePattern;
	private Resource recurrentPattern;
	private Resource notForProfit;
	private Resource indirectProfit;
	private Resource profitFromUserData;
	private Resource profitFromAdvertisement;
	private Resource profitFromBoth;
	private Resource profitFromPeerConsumers;
	private Resource profitFromPeerProviders;
	private Resource perTransaction;
	private Resource perListing;
	private Resource membershipFee;
	private Resource mixedObjectType;
	private Resource experientialObjectType;
	private Resource functionalObjectType;
	private Resource privateResourceOwner;
	private Resource businessResourceOwner;
	private Resource privateAndBusinessResourceOwner;
	private Resource minutes;
	private Resource hours;
	private Resource days;
	private Resource weeks;
	private Resource months;
	private Resource years;
	private Resource fullService;
	private Resource selfService;
	private Resource inBetween;
	private Resource c2c;
	private Resource c2b2c;
	private Resource free;
	private Resource marketIntegrationClass;
	private Resource integrated;
	private Resource separated;
	private Resource neighbourhoodWide;
	private Resource cityWide;
	private Resource stateWide;
	private Resource countryWide;
	private Resource regionWide;
	private Resource global;
	private Resource androidApp;
	private Resource iOSApp;
	private Resource windowsPhoneApp;
	private Resource providerRatings;
	private Resource providerAndConsumerRatings;
	private Resource referral;
	private Resource vouching;
	private Resource valueAddedServices;

	/**
	 * Instantiates the new ontology model and most resources and properties
	 */
	public OntologyWriter() {
		log = Logger.getLogger(this.getClass().getName()); // instantiate logger

		buildCountryIndex(); // transform json array into map with country codes as keys

		ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		// define namespace prefixes to be used in output
		ontologyModel.setNsPrefix("dbpp", DBPP);
		ontologyModel.setNsPrefix("dbpr", DBPR);
		ontologyModel.setNsPrefix("dbpo", DBPO);
		ontologyModel.setNsPrefix("dct", DCT);
		ontologyModel.setNsPrefix("lgd", LGD);
		ontologyModel.setNsPrefix("skos", SKOS);
		ontologyModel.setNsPrefix("time", TIME);
		ontologyModel.setNsPrefix("wordnet", WORDNET);

		// create properties once in the beginning to use for every platform
		rdfType = ontologyModel.createProperty(RDF + "type");
		rdfsLabel = ontologyModel.createProperty(RDFS + "label");
		dbppUrl = ontologyModel.createProperty(DBPP + "url");
		hasResourceType = ontologyModel.createProperty(D2S + "has_resource_type");
		promotes = ontologyModel.createProperty(D2S + "promotes");
		hasPattern = ontologyModel.createProperty(D2S + "has_p2p_scc_pattern");
		hasTemporality = ontologyModel.createProperty(D2S + "has_temporality");
		hasMarketMediation = ontologyModel.createProperty(D2S + "has_market_mediation");
		accessedObjectHasType = ontologyModel.createProperty(D2S + "accessed_object_has_type");
		hasResourceOwner = ontologyModel.createProperty(D2S + "has_resource_owner");
		minServiceDuration = ontologyModel.createProperty(D2S + "min_service_duration");
		maxServiceDuration = ontologyModel.createProperty(D2S + "max_service_duration");
		hasConsumerInvolvement = ontologyModel.createProperty(D2S + "has_consumer_involvement");
		hasMoneyFlow = ontologyModel.createProperty(D2S + "has_money_flow");
		hasMarketIntegration = ontologyModel.createProperty(D2S + "has_market_integration");
		marketsAre = ontologyModel.createProperty(D2S + "markets_are");
		hasScope = ontologyModel.createProperty(D2S + "has_scope");
		launchYear = ontologyModel.createProperty(DBPP + "launchYear");
		launchedIn = ontologyModel.createProperty(D2S + "launched_in");
		operatorResidesIn = ontologyModel.createProperty(D2S + "operator_resides_in");
		locationCity = ontologyModel.createProperty(DBPP + "locationCity");
		locationCountry = ontologyModel.createProperty(DBPP + "locationCountry");
		hasApp = ontologyModel.createProperty(D2S + "has_app");
		hasTrustContribution = ontologyModel.createProperty(D2S + "has_trust_contribution");
		owlSameAs = ontologyModel.createProperty(OWL + "sameAs");
		dbppLanguage = ontologyModel.createProperty(DBPP + "language");
		rdfsComment = ontologyModel.createProperty(RDFS + "comment");
		rdfsSubClassOf = ontologyModel.createProperty(RDFS + "subClassOf");
		rdfsSeeAlso = ontologyModel.createProperty(RDFS + "seeAlso");

		// create resources once in the beginning to use for every platform
		yearClass = ontologyModel.createResource(D2S + "Year");
		cityClass = ontologyModel.createResource(D2S + "City");

		resourceTypeClass = ontologyModel.createResource(D2S + "Resource_Type");

		p2pSccPlatformClass = ontologyModel.createResource(D2S + "P2P_SCC_Platform");
		once = ontologyModel.createResource(D2S + "Once");
		often = ontologyModel.createResource(D2S + "Often");

		socialConsumerism = ontologyModel.createResource(D2S + "Social");
		economicConsumerism = ontologyModel.createResource(D2S + "Economic");
		environmentalConsumerism = ontologyModel.createResource(D2S + "Environmental");
		noConsumerism = ontologyModel.createResource(D2S + "None");

		deferredPattern = ontologyModel.createResource(D2S + "Deferred");
		immediatePattern = ontologyModel.createResource(D2S + "Immediate");
		recurrentPattern = ontologyModel.createResource(D2S + "Recurrent");

		notForProfit = ontologyModel.createResource(D2S + "Not-for-profit");
		indirectProfit = ontologyModel.createResource(D2S + "Indirect_Profit");
		profitFromUserData = ontologyModel.createResource(D2S + "Profit_from_user_data");
		profitFromAdvertisement = ontologyModel.createResource(D2S + "Profit_from_advertisement");
		profitFromBoth = ontologyModel.createResource(D2S + "Profit_from_both");
		profitFromPeerConsumers = ontologyModel.createResource(D2S + "Profit_from_peer_consumers");
		profitFromPeerProviders = ontologyModel.createResource(D2S + "Profit_from_peer_providers");
		perTransaction = ontologyModel.createResource(D2S + "Per_transaction");
		perListing = ontologyModel.createResource(D2S + "Per_listing");
		membershipFee = ontologyModel.createResource(D2S + "Membership_fee");

		mixedObjectType = ontologyModel.createResource(D2S + "Mixed");
		experientialObjectType = ontologyModel.createResource(D2S + "Experiential");
		functionalObjectType = ontologyModel.createResource(D2S + "Functional");

		privateResourceOwner = ontologyModel.createResource(D2S + "Private");
		businessResourceOwner = ontologyModel.createResource(D2S + "Business");
		privateAndBusinessResourceOwner = ontologyModel.createResource(D2S + "Private_and_business");

		minutes = ontologyModel.createResource(TIME + "unitMinute");
		hours = ontologyModel.createResource(TIME + "unitHour");
		days = ontologyModel.createResource(TIME + "unitDay");
		weeks = ontologyModel.createResource(TIME + "unitWeek");
		months = ontologyModel.createResource(TIME + "unitMonth");
		years = ontologyModel.createResource(TIME + "unitYear");

		fullService = ontologyModel.createResource(D2S + "Full-service");
		selfService = ontologyModel.createResource(D2S + "Self-service");
		inBetween = ontologyModel.createResource(D2S + "In-Between");

		c2c = ontologyModel.createResource(D2S + "C2C");
		c2b2c = ontologyModel.createResource(D2S + "C2B2C");
		free = ontologyModel.createResource(D2S + "Free");

		marketIntegrationClass = ontologyModel.createResource(D2S + "Market_Integration");
		integrated = ontologyModel.createResource(D2S + "Integrated");
		separated = ontologyModel.createResource(D2S + "Separated");
		neighbourhoodWide = ontologyModel.createResource(D2S + "Neighbourhood-wide");
		cityWide = ontologyModel.createResource(D2S + "City-wide");
		stateWide = ontologyModel.createResource(D2S + "State-wide");
		countryWide = ontologyModel.createResource(D2S + "Country-wide");
		regionWide = ontologyModel.createResource(D2S + "Region-wide");
		global = ontologyModel.createResource(D2S + "Global");

		androidApp = ontologyModel.createResource(D2S + "Android_app");
		iOSApp = ontologyModel.createResource(D2S + "iOS_app");
		windowsPhoneApp = ontologyModel.createResource(D2S + "Windows_Phone_app");

		providerRatings = ontologyModel.createResource(D2S + "Provider_ratings");
		providerAndConsumerRatings = ontologyModel.createResource(D2S + "Provider_and_consumer_ratings");
		referral = ontologyModel.createResource(D2S + "Referral");
		vouching = ontologyModel.createResource(D2S + "Vouching");
		valueAddedServices = ontologyModel.createResource(D2S + "Value-added_services");
	}

	/**
	 * Retrieve JSON from discover2share server and transform it into a map with country codes as keys and a map as value. Latter map contains all attributes of
	 * a country object with their names as keys and values as values.
	 */
	private void buildCountryIndex() {
		try {
			// read JSON from discover2share server
			JSONObject json = JsonReader.readJsonFromUrl(COUNTRIESJSON);
			JSONArray a = json.getJSONArray("countries"); // retrieved object contains an array at the key "countries"
			for (int i = 0; i < a.length(); i++) { // for each country
				JSONObject o = a.getJSONObject(i);
				countryIndex.put(o.getString("countryId"), o.getString("resourceName")); // put an entry into the map
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calls all necessary steps required to construct the D2S representation of a platform. With a new URI for the platform.
	 */
	public OntModel constructPlatform(Platform platform) {
		return constructPlatform(platform, null);
	}

	/**
	 * Calls all necessary steps required to construct the D2S representation of a platform. With the given URI.
	 */
	public OntModel constructPlatform(Platform platform, String uri) {
		this.platform = platform;

		initializePlatform(uri); // create platform resource with basic properties

		// add all properties defining the dimension values
		resourceTypeDimension(platform.getResourceTypes());
		sustainableConsumerismDimension(platform.getConsumerisms());
		patternDimension(platform.getPattern(), platform.getTemporality());
		marketMediationDimension(platform.getMarketMediations());
		typeOfAccessedObjectDimension(platform.getTypeOfAccessedObject());
		resourceOwnerDimension(platform.getResourceOwner());
		minServiceDurationDimension(platform.getServiceDurationMin());
		maxServiceDurationDimension(platform.getServiceDurationMax());
		consumerInvolvementDimension(platform.getConsumerInvolvement());
		moneyFlowDimension(platform.getMoneyFlow());
		marketIntegrationDimension(platform.getOffering(), platform.getGeographicScope());
		launchYearDimension();
		locationDimension(platform.getLaunchCity(), platform.getLaunchCountry(), launchedIn);
		locationDimension(platform.getResidenceCity(), platform.getResidenceCountry(), operatorResidesIn);
		smartphoneAppDimension(platform.getApps());
		trustContributionDimension(platform.getTrustContributions());
		languageDimension(platform.getLanguages());

		return ontologyModel;
	}

	/**
	 * Creates the ontology Resource of type d2s:P2P_SCC_Platform representing the platform with basic properties
	 */
	private void initializePlatform(String uri) {
		// Create new platform instance
		if (uri == null) { // if no specific uri is provided to use
			platformResource = ontologyModel.createResource(D2S + "platform_" + getNewPlatformURI()); // get a new unused one
		} else {
			platformResource = ontologyModel.createResource(D2S + uri); // use the given uri
		}
		// Set type
		platformResource.addProperty(rdfType, p2pSccPlatformClass);
		// add rdfs:label property
		platformResource.addProperty(rdfsLabel, platform.getLabel());
		// add dbpp:url property
		platformResource.addProperty(dbppUrl, platform.getUrl(), XSDDatatype.XSDanyURI);
		// add comment/description
		if (platform.getDescription() != null && !platform.getDescription().isEmpty())
			platformResource.addProperty(rdfsComment, platform.getDescription());
	}

	// retrieve all resource types currently used in the ontology
	private List<Map<String, String>> resourceTypeIndex = new OntologyServiceBean().getAllResourceTypes();

	/**
	 * Creates a link between the platform resource an its value for the resource type dimension.
	 * 
	 * @param set
	 *            Platform's resource type as defined in the excel table.
	 */
	private void resourceTypeDimension(Set<ResourceType> set) {
		if (set == null || set.isEmpty())
			return;

		for (ResourceType resourceType : set) { // for each of the platform's resource types
			String resourceTypeLabel = resourceType.getLabel().toLowerCase();
			if (resourceTypeLabel.isEmpty()) // don't process resource types without labels
				continue;

			Resource match = null;
			for (Map<String, String> rt : resourceTypeIndex){ // compare with each resource type already in the ontology
				if (rt.get("name").toLowerCase().equals(resourceTypeLabel)) { // if names are equal
					match = platformResource.addProperty(hasResourceType, ontologyModel.createResource(D2S + rt.get("resourceName"))); // add that resource type
					break;
				}
			}
			
			if (match == null) { // if it's a new Resource Type
				Resource newResourceType = ontologyModel.createResource(D2S
						+ resourceType.getLabel().replace(" ", "_").replace("[", "%5B").replace("]", "%5D").replace("/", "_").replace(",", "_")
								.replace("\"", "")); // create resource, replace invalid characters
				newResourceType.addProperty(rdfsSubClassOf, resourceTypeClass); // is of type d2s:Resource_Type
				newResourceType.addProperty(rdfsLabel, resourceType.getLabel()); // add label
				if (resourceType.getExternals() != null) { // if external concepts are provided
					for (ResourceType external : resourceType.getExternals()) { // for each of them
						if (external.getResource() != null && !external.getResource().isEmpty()) {
							Resource newExternal = ontologyModel.createResource(external.getResource()); // create a resource
							newResourceType.addProperty(rdfsSeeAlso, newExternal); // connect via rdfs:seeAlso
						}
					}
				}
				platformResource.addProperty(hasResourceType, newResourceType); // add that new resource type
			}
		}
	}

	String[] consumerisms = { "none", "economic", "environmental", "social" };

	/**
	 * Creates a link between the platform resource an its values for the sustainable consumerism dimension.
	 * 
	 * @param set
	 */
	private void sustainableConsumerismDimension(Set<String> set) {
		if (set == null || set.isEmpty())
			return;

		for (String consumerism : set) { // compare with all possible values
			consumerism = consumerism.toLowerCase();
			if (consumerism.equals(consumerisms[0]))
				platformResource.addProperty(promotes, noConsumerism); // add property link if matched
			else if (consumerism.equals(consumerisms[1]))
				platformResource.addProperty(promotes, economicConsumerism);
			else if (consumerism.equals(consumerisms[2]))
				platformResource.addProperty(promotes, environmentalConsumerism);
			else if (consumerism.equals(consumerisms[3]))
				platformResource.addProperty(promotes, socialConsumerism);
		}
	}

	private String[] patternValues = { "deferred", "immediate", "recurrent" };
	private String[] temporalityValues = { "once", "often" };

	/**
	 * Creates a link between the platform resource an its value for the P2P SCC pattern dimension.
	 * 
	 * @param pattern
	 *            Platform's pattern as defined in the excel table.
	 */
	private void patternDimension(String pattern, String temporality) {
		if (pattern == null || pattern.isEmpty())
			return;
		pattern = pattern.toLowerCase();

		Resource patternResource = null;
		if (pattern.equals(patternValues[0])) { // when matching
			// create a new anonymous instance of type deferred to be able to add temporality to it later on
			patternResource = ontologyModel.createResource();
			patternResource.addProperty(rdfType, deferredPattern); // of type d2s:Deferred
			platformResource.addProperty(hasPattern, patternResource); // add that new resource as a property to the platform resource
		} else if (pattern.equals(patternValues[1])) {
			patternResource = ontologyModel.createResource();
			patternResource.addProperty(rdfType, immediatePattern);
			platformResource.addProperty(hasPattern, patternResource);
		} else if (pattern.equals(patternValues[2])) {
			patternResource = ontologyModel.createResource();
			patternResource.addProperty(rdfType, recurrentPattern);
			platformResource.addProperty(hasPattern, patternResource);
		}

		if (temporality != null) { // if temporality value is provided
			temporality = temporality.toLowerCase();
			if (temporality.equals(temporalityValues[0])) // match against possible values
				patternResource.addProperty(hasTemporality, once); // add property link if match
			else if (temporality.equals(temporalityValues[1]))
				patternResource.addProperty(hasTemporality, often);
		}
	}

	private String[] mediationValues = { "not-for-profit", "profit from both", "profit from both peer consumers and peer providers", "indirect profit",
			"profit from peer consumers", "profit from peer providers", "profit from user data", "profit from advertisement", "per transaction", "per listing",
			"membership fee" };

	/**
	 * Creates a link between the platform resource an its value for the market mediation dimension.
	 * 
	 * @param set
	 *            Platform's market mediation as defined in the excel table.
	 */
	private void marketMediationDimension(Set<String> set) {
		if (set == null || set.isEmpty())
			return;

		for (String mediation : set) {
			mediation = mediation.toLowerCase();
			if (mediation.equals(mediationValues[0]))
				platformResource.addProperty(hasMarketMediation, notForProfit);
			else if (mediation.equals(mediationValues[1]) || mediation.equals(mediationValues[2]))
				platformResource.addProperty(hasMarketMediation, profitFromBoth);
			else if (mediation.equals(mediationValues[2]))
				platformResource.addProperty(hasMarketMediation, indirectProfit);
			else if (mediation.equals(mediationValues[3]))
				platformResource.addProperty(hasMarketMediation, profitFromPeerConsumers);
			else if (mediation.equals(mediationValues[4]))
				platformResource.addProperty(hasMarketMediation, profitFromPeerProviders);
			else if (mediation.equals(mediationValues[5]))
				platformResource.addProperty(hasMarketMediation, profitFromUserData);
			else if (mediation.equals(mediationValues[6]))
				platformResource.addProperty(hasMarketMediation, profitFromAdvertisement);
			else if (mediation.equals(mediationValues[7]))
				platformResource.addProperty(hasMarketMediation, perTransaction);
			else if (mediation.equals(mediationValues[8]))
				platformResource.addProperty(hasMarketMediation, perListing);
			else if (mediation.equals(mediationValues[9]))
				platformResource.addProperty(hasMarketMediation, membershipFee);
		}
	}

	private String[] objectTypeValues = { "mixed", "functional", "experiential" };

	/**
	 * Creates a link between the platform resource an its value for the type of accessed object dimension.
	 */
	private void typeOfAccessedObjectDimension(String value) {
		if (value == null || value.isEmpty())
			return;
		value = value.toLowerCase();

		if (value.equals(objectTypeValues[0]))
			platformResource.addProperty(accessedObjectHasType, mixedObjectType);
		else if (value.equals(objectTypeValues[1]))
			platformResource.addProperty(accessedObjectHasType, functionalObjectType);
		else if (value.equals(objectTypeValues[2]))
			platformResource.addProperty(accessedObjectHasType, experientialObjectType);
	}

	private String[] resourceOwnerValues = { "private", "private and business", "business" };

	/**
	 * Creates a link between the platform resource an its value for the resource owner dimension.
	 * 
	 * @param value
	 *            Platform's resource type as defined in the excel table.
	 */
	private void resourceOwnerDimension(String value) {
		if (value == null || value.isEmpty())
			return;
		value = value.toLowerCase();

		if (value.equals(resourceOwnerValues[0]))
			platformResource.addProperty(hasResourceOwner, privateResourceOwner);
		else if (value.equals(resourceOwnerValues[1]))
			platformResource.addProperty(hasResourceOwner, privateAndBusinessResourceOwner);
		else if (value.equals(resourceOwnerValues[2])) {
			platformResource.addProperty(hasResourceOwner, businessResourceOwner);
		}
	}

	private String[] serviceDurationValues = { "minutes", "hours", "days", "weeks", "months", "years" };

	/**
	 * Creates a link between the platform resource an its value for the min service duration.
	 * 
	 * @param value
	 *            Platform's min service duration as defined in the excel table.
	 */
	private void minServiceDurationDimension(String value) {
		if (value == null || value.isEmpty())
			return;
		value = value.toLowerCase();

		if (value.equals(serviceDurationValues[0]))
			platformResource.addProperty(minServiceDuration, minutes);
		else if (value.equals(serviceDurationValues[1]))
			platformResource.addProperty(minServiceDuration, hours);
		else if (value.equals(serviceDurationValues[2]))
			platformResource.addProperty(minServiceDuration, days);
		else if (value.equals(serviceDurationValues[3]))
			platformResource.addProperty(minServiceDuration, weeks);
		else if (value.equals(serviceDurationValues[4]))
			platformResource.addProperty(minServiceDuration, months);
		else if (value.equals(serviceDurationValues[5]))
			platformResource.addProperty(minServiceDuration, years);
	}

	/**
	 * Creates a link between the platform resource an its value for the max service duration.
	 * 
	 * @param value
	 *            Platform's max service duration as defined in the excel table.
	 */
	private void maxServiceDurationDimension(String value) {
		if (value == null || value.isEmpty())
			return;
		value = value.toLowerCase();

		if (value.equals(serviceDurationValues[0]))
			platformResource.addProperty(maxServiceDuration, minutes);
		else if (value.equals(serviceDurationValues[1]))
			platformResource.addProperty(maxServiceDuration, hours);
		else if (value.equals(serviceDurationValues[2]))
			platformResource.addProperty(maxServiceDuration, days);
		else if (value.equals(serviceDurationValues[3]))
			platformResource.addProperty(maxServiceDuration, weeks);
		else if (value.equals(serviceDurationValues[4]))
			platformResource.addProperty(maxServiceDuration, months);
		else if (value.equals(serviceDurationValues[5]))
			platformResource.addProperty(maxServiceDuration, years);
	}

	private String[] consumerInvolvementValues = { "full-service", "self-service", "in-between" };

	/**
	 * Creates a link between the platform resource an its value for the consumer involvement dimension.
	 * 
	 * @param value
	 *            Platform's consumer involvement as defined in the excel table.
	 */
	private void consumerInvolvementDimension(String value) {
		if (value == null || value.isEmpty())
			return;
		value = value.toLowerCase();

		if (value.equals(consumerInvolvementValues[0]))
			platformResource.addProperty(hasConsumerInvolvement, fullService);
		else if (value.equals(consumerInvolvementValues[1]))
			platformResource.addProperty(hasConsumerInvolvement, selfService);
		else if (value.equals(consumerInvolvementValues[2]))
			platformResource.addProperty(hasConsumerInvolvement, inBetween);
	}

	private String[] moneyFlowValues = { "c2c", "consumer to consumer", "c2b2c", "consumer to business to consumer", "free" };

	/**
	 * Creates a link between the platform resource an its value for the money flow dimension.
	 * 
	 * @param value
	 *            Platform's money flow as defined in the excel table.
	 */
	private void moneyFlowDimension(String value) {
		if (value == null || value.isEmpty())
			return;
		value = value.toLowerCase();

		if (value.equals(moneyFlowValues[0]) || value.equals(moneyFlowValues[1]))
			platformResource.addProperty(hasMoneyFlow, c2c);
		else if (value.equals(moneyFlowValues[2]) || value.equals(moneyFlowValues[3]))
			platformResource.addProperty(hasMoneyFlow, c2b2c);
		else if (value.equals(moneyFlowValues[4]))
			platformResource.addProperty(hasMoneyFlow, free);
	}

	/**
	 * Creates a link between the platform resource and a new market integration instance. This instance is then connected to the respective market offering and
	 * geographic scope.
	 */
	private void marketIntegrationDimension(String offering, String geographicScope) {
		if ((offering == null || offering.isEmpty()) && (geographicScope == null || geographicScope.isEmpty()))
			return;

		Resource marketIntegration = ontologyModel.createResource(); // create anonymous instance
		marketIntegration.addProperty(rdfType, marketIntegrationClass); // of type d2s:Market_Integration
		platformResource.addProperty(hasMarketIntegration, marketIntegration); // add that instance as a property to the platform resource

		marketOffering(marketIntegration, offering); // add market offering resource
		geographicScope(marketIntegration, geographicScope); // and market integration resource as properties to the anonymous instance 
	}

	private String[] marketOfferingValues = { "integrated", "separated" };

	/**
	 * Creates a link between a platform's market integration instance and its value for the market offering.
	 * 
	 * @param marketIntegration
	 *            Instance created for the current platform.
	 * @param value
	 *            Market offering value as defined in the excel table.
	 */
	private void marketOffering(Resource marketIntegration, String value) {
		if (value == null || value.isEmpty())
			return;
		value = value.toLowerCase();

		if (value.equals(marketOfferingValues[0]))
			marketIntegration.addProperty(marketsAre, integrated);
		else if (value.equals(marketOfferingValues[1]))
			marketIntegration.addProperty(marketsAre, separated);
	}

	private String[] geographicScopeValues = { "neighbourhood-wide", "city-wide", "state-wide", "country-wide", "region-wide", "global" };

	/**
	 * Creates a link between a platform's market integration instance and its value for the geographic scope.
	 * 
	 * @param marketIntegration
	 *            Instance created for the current platform.
	 * @param value
	 *            Geographic scope as defined in the excel table.
	 */
	private void geographicScope(Resource marketIntegration, String value) {
		if (value == null || value.isEmpty())
			return;
		value = value.toLowerCase();

		if (value.equals(geographicScopeValues[0]))
			marketIntegration.addProperty(hasScope, neighbourhoodWide);
		else if (value.equals(geographicScopeValues[1]))
			marketIntegration.addProperty(hasScope, cityWide);
		else if (value.equals(geographicScopeValues[2]))
			marketIntegration.addProperty(hasScope, stateWide);
		else if (value.equals(geographicScopeValues[3]))
			marketIntegration.addProperty(hasScope, countryWide);
		else if (value.equals(geographicScopeValues[4]))
			marketIntegration.addProperty(hasScope, regionWide);
		else if (value.equals(geographicScopeValues[5]))
			marketIntegration.addProperty(hasScope, global);
	}

	/**
	 * Creates a link between the platform and the resource representing its launch year.
	 */
	private void launchYearDimension() {
		if (platform.getYearLaunch() == null || platform.getYearLaunch().isEmpty())
			return;

		String yearString = platform.getYearLaunch().replace("http://www.discover2share.net/d2s-ont/", ""); // remove URI base
		try {
			String sparqlQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " 
									+ "PREFIX d2s: <http://www.discover2share.net/d2s-ont/> "
									+ "ask  {d2s:" + yearString + " rdf:type d2s:Year}"; // query to ask if this year already exists in the ontology
			Query query = QueryFactory.create(sparqlQuery);
			QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query); // ask endpoint

			int year = Integer.parseInt(yearString);
			Resource launchYearResource;
			if (!qexec.execAsk()) { // when no instance for this year has been created before
				launchYearResource = ontologyModel.createResource(D2S + year); // create new resource
				launchYearResource.addProperty(rdfType, yearClass); // of type Year
				// create Resource for DBpedia equivalent
				Resource launchYearResourceDBpedia = ontologyModel.createResource(DBPR + year);
				launchYearResource.addProperty(owlSameAs, launchYearResourceDBpedia); // connect to DBpedia equivalent
			} else
				// if instance for this year was already created
				launchYearResource = ontologyModel.createResource(D2S + year); // create only for usage here

			platformResource.addProperty(launchYear, launchYearResource); // connect platform and year instance
		} catch (NumberFormatException e) {
			log.warn("Year launch is not a proper number. Is: '" + yearString + "'");
		} catch (Exception e) {
			log.warn("Error querying the Ontology for an existing resource with the URI d2s:" + yearString);
		}
	}

	/**
	 * Connects the platform either to an anonymous class representing its foundation or residence place depending on the given property. The anonymous class
	 * bundles city and country resources of that respective place.
	 * 
	 * @param city
	 *            City as defined in the excel table.
	 * @param country
	 *            Country code as defined in the excel table.
	 * @param property
	 *            Property to use when connecting place and platform.
	 */
	private void locationDimension(GeoUnit city, GeoUnit country, Property property) {
		if (city == null && country == null)
			return; // stop if neither city nor country are provided

		Resource countryResource = null;
		Resource cityResource = null;

		if (city != null && city.getLabel() != null) { // if a city was chosen
			// query to find the given city in the ontology with its respective country
			String sparqlQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " 
									+ "PREFIX d2s: <http://www.discover2share.net/d2s-ont/> "
									+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " 
									+ "PREFIX dbpp: <http://dbpedia.org/property/> " 
									+ "SELECT ?city ?country {"
									+ " ?city rdf:type d2s:City." + " ?city rdfs:label \"" + city.getLabel() + "\"." 
									+ " ?city dbpp:locationCountry ?country." + "}";

			Query query = QueryFactory.create(sparqlQuery);
			QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query);
			ResultSet result = qexec.execSelect();

			if (result.hasNext()) { // if the city was found in the ontology
				QuerySolution first = result.next();
				cityResource = first.getResource("city"); // extract resources from the result
				countryResource = first.getResource("country");
			} else { // not found in ontology
				try {
					// Create proxy instance of d2s:City. Remove illegal characters for resource name.
					cityResource = ontologyModel.createResource(D2S
							+ city.getLabel().replace(" ", "_").replace("[", "%5B").replace("]", "%5D").replace("/", "_").replace(",", "_").replace("\"", ""));
					cityResource.addProperty(rdfType, cityClass); // of type d2s:City
					cityResource.addProperty(rdfsLabel, city.getLabel()); // Add city name as label
					if (city.getGeonames() != null) { // if the geonames equivalent is provided
						// Create resource for the found geonames concept
						Resource cityGeonames = ontologyModel.createResource(city.getGeonames());
						cityResource.addProperty(owlSameAs, cityGeonames); // link the two equivalents

						// make another request to geonames to retrieve extended info on the city concept
						JSONObject cityInfo = null;
						int tryCounter = 0; // counter to keep track of query attempts to geonames
						while(cityInfo == null && tryCounter < 3) { // attempt geonames at least twice if it fails
							try { // GeoNames API query often fails on the first attempt
								cityInfo = JsonReader.readJsonFromUrl("http://api.geonames.org/getJSON?username=discover2share&geonameId="
										+ city.getGeonames().replace("http://www.geonames.org/", ""));
							} catch (Exception e) {
								log.warn("Querying GeoNames API failed: " + e.getMessage());
								tryCounter++;
							}
						}
						// if the extended info contains a proper link to the respective wikipedia article
						if (cityInfo != null && cityInfo.has("wikipediaURL")) {
							// Create resource for DBpedia concept belonging to the wikipedia article
							String dbpediaId = cityInfo.getString("wikipediaURL").replace("en.wikipedia.org/wiki", "");
							Resource dbpedia = ontologyModel.createResource("http://dbpedia.org/resource" + dbpediaId);
							// Connect the two equivalents
							cityResource.addProperty(owlSameAs, dbpedia);
						}
					}
				} catch (JSONException e1) {
					e1.printStackTrace();
				}

				if (country != null && country.getGeonames() != null) { // if the country and its geonames equivalent are provided
					// retrieve its proper resource name from the index
					String countryResourceName = countryIndex.get(country.getGeonames().replace("http://www.geonames.org/", ""));
					// create a resource for it as well
					countryResource = ontologyModel.createResource(D2S + countryResourceName);
					cityResource.addProperty(locationCountry, countryResource); // link the two
				}
			}
		} else if (country != null && country.getGeonames() != null) { // if no city but a country was chosen
			// retrieve its proper resource name from the index
			String countryResourceName = countryIndex.get(country.getGeonames().replace("http://www.geonames.org/", ""));
			// create a resource object for it
			countryResource = ontologyModel.createResource(D2S + countryResourceName);
		}

		if (countryResource != null) { // if a resource was created for the country
			Resource locationResource = ontologyModel.createResource(); // create a new anonymous resource
			platformResource.addProperty(property, locationResource); // connect platform and anonymous resource
			// connect anonymous resource and country resource
			locationResource.addProperty(locationCountry, countryResource);

			if (cityResource != null) // if a resource was created for the city as well
				// connect anonymous resource and city resource
				locationResource.addProperty(locationCity, cityResource);
		}
	}

	private String[] smartphoneApps = { "android app", "ios app", "windows phone app" };

	/**
	 * Connects the platform to its values from the Smartphone App dimension
	 */
	private void smartphoneAppDimension(Set<String> set) {
		if (set == null || set.isEmpty())
			return;

		for (String app : set) {
			app = app.toLowerCase();
			if (app.equals(smartphoneApps[0]))
				platformResource.addProperty(hasApp, androidApp);
			else if (app.equals(smartphoneApps[1]))
				platformResource.addProperty(hasApp, iOSApp);
			else if (app.equals(smartphoneApps[2]))
				platformResource.addProperty(hasApp, windowsPhoneApp);
		}
	}

	private String[] trustContributions = { "provider ratings", "provider and consumer ratings", "referral", "vouching", "value-added services" };

	/**
	 * Connects the platform to its values from the trust contribution dimension
	 * @param set 
	 * 			Set of trust contribution values
	 */
	private void trustContributionDimension(Set<String> set) {
		if (set == null || set.isEmpty())
			return;

		for (String trustContribution : set) {
			trustContribution = trustContribution.toLowerCase();
			if (trustContribution.equals(trustContributions[0]))
				platformResource.addProperty(hasTrustContribution, providerRatings);
			else if (trustContribution.equals(trustContributions[1]))
				platformResource.addProperty(hasTrustContribution, providerAndConsumerRatings);
			else if (trustContribution.equals(trustContributions[2]))
				platformResource.addProperty(hasTrustContribution, referral);
			else if (trustContribution.equals(trustContributions[3]))
				platformResource.addProperty(hasTrustContribution, vouching);
			else if (trustContribution.equals(trustContributions[4]))
				platformResource.addProperty(hasTrustContribution, valueAddedServices);
		}
	}

	/**
	 * Connects the platform to its values from the language dimension
	 * @param set 
	 * 			Set of trust language URIs
	 */
	private void languageDimension(Set<String> set) {
		if (set == null || set.isEmpty())
			return;

		for (String language : set) { // for each of the platforms languages
			// connect platform resource to the language's resource as property
			// languages in the set need to be the full URI's!
			platformResource.addProperty(dbppLanguage, ontologyModel.createResource(language));
		}
	}

	/**
	 * Determines an unused platform number in the ontology by adding 1 to the highest in use.
	 * 
	 * @return A new, unused platform number
	 */
	private String getNewPlatformURI() {
		// query that determines the highest platform number used in the ontology + 1
		String sparqlQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " 
								+ "PREFIX d2s: <http://www.discover2share.net/d2s-ont/> "
								+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " 
								+ "SELECT (?nr + 1 as ?new) {" 
								+ "  ?platform rdf:type d2s:P2P_SCC_Platform."
								+ "  BIND(xsd:integer(substr(xsd:string(?platform),48)) as ?nr)" 
								+ "} order by desc (?nr) limit 1";
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query);
		ResultSet result = qexec.execSelect();
		if (result.hasNext()) { // if a new number could be determined
			return result.next().getLiteral("new").getLexicalForm(); // return it as string
		}
		return null;
	}
}
