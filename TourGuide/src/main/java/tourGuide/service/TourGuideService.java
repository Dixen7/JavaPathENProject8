package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.NearbyAttraction;
import tourGuide.model.user.User;
import tourGuide.model.user.UserLocation;
import tourGuide.model.user.UserReward;
import tourGuide.tracker.Tracker;
import tripPricer.Provider;

/**
 * TourGuideService performs operations for TourGuideController, as well as performing services for Tracker thread
 */
@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsService gpsService;
	private final RewardsService rewardsService;
	private final UserService userService;
	private final TripService tripService;
	public final Tracker tracker;
	boolean testMode = true;
	@Value("${thread.pool.size}")
	private int threadPoolSize = 500;
	private ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);


	public TourGuideService(GpsService gpsService, RewardsService rewardsService, UserService userService, TripService tripService) {
		this.gpsService = gpsService;
		this.rewardsService = rewardsService;
		this.userService = userService;
		this.tripService = tripService;

		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	/**
	 * Get all current UserRewards for provided user
	 *
	 * @param userName user's userName
	 * @return list of UserReward objects for provided user
	 */
	public List<UserReward> getUserRewards(String userName) {
		User user = getUserByUsername(userName);
		if (user == null) {
			logger.debug("getUserRewards: user not found with name " + userName + " returning null");
			return null;
		}
		return user.getUserRewards();
	}

	/**
	 * Get current location of provided user
	 *
	 * @param userName user's userName
	 * @return current location of user
	 */
	public VisitedLocation getUserLocation(String userName) {
		User user = getUserByUsername(userName);
		if (user==null) {
			logger.debug("getUserLocation: user not found with name " + userName + " returning null");
			return null;
		}
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
				user.getLastVisitedLocation() :
				trackUserLocation(user);
		return visitedLocation;
	}

	/**
	 * Get User object for provided user name
	 *
	 * @param userName user's userName
	 * @return user object
	 */
	public User getUserByUsername(String userName) {
		return userService.getUserByUsername(userName);
	}

	/**
	 * Get User objects for all users currently in system
	 *
	 * @return list of User objects for all users
	 */
	public List<User> getAllUsers() {
		return userService.getAllUsers();
	}

	/**
	 * Get number of users currently stored in system
	 *
	 * @return number of users currently stored, as integer
	 */
	public int getUserCount() {
		return userService.getUserCount();
	}

	/**
	 * Add a user to the system
	 *
	 * @param user User object to be added
	 */
	public void addUser(User user) {
		userService.addUser(user);
	}

	/**
	 * Get trip deals for a provided user from the TripService
	 *
	 * @param userName user's userName
	 * @return List of Provider objects for trip deals suggested by TripService
	 */
	public List<Provider> getTripDeals(String userName) {
		User user = getUserByUsername(userName);
		if (user == null) {
			logger.debug("getTripDeals: user not found with name " + userName + " returning null");
			return null;
		}
		return tripService.getTripDeals(getUserByUsername(userName));
	}

	/**
	 * Track user's current location
	 *
	 * Gets users location from GpsService
	 * Then adds this location to user's visited locations
	 * Then asks reward service to calculate user's rewards to update with new location
	 *
	 * @param user User to be tracked
	 * @return VisitedLocation of user's current location
	 */
	public VisitedLocation trackUserLocation(User user) {

		VisitedLocation visitedLocation = gpsService.getUserLocation(user.getUserId());

		CompletableFuture.supplyAsync(()-> {
					return userService.addToVisitedLocations(visitedLocation, user.getUserName());
				}, executorService)
				.thenAccept(n -> {rewardsService.calculateRewards(user);});

		return visitedLocation;
	}

	/**
	 * Track all users' current location
	 *
	 * For each user:
	 * Gets users location from GpsService
	 * Then adds this location to user's visited locations
	 *
	 */
	public void trackAllUserLocations() {
		List<User> allUsers = userService.getAllUsers();

		ArrayList<CompletableFuture> futures = new ArrayList<>();

		logger.debug("trackAllUserLocations: Creating futures for " + allUsers.size() + " user(s)");
		allUsers.forEach((n)-> {
			futures.add(
					CompletableFuture.supplyAsync(()-> userService.addToVisitedLocations(gpsService.getUserLocation(n.getUserId()), n.getUserName()), executorService)
			);
		});
		logger.debug("trackAllUserLocations: Futures created: " + futures.size() + ". Getting futures...");
		futures.forEach((n)-> {
			try {
				n.get();
			} catch (InterruptedException e) {
				logger.error("Track All Users InterruptedException: " + e);
			} catch (ExecutionException e) {
				logger.error("Track All Users ExecutionException: " + e);
			}
		});
		logger.debug("trackAllUserLocations: Done!");

	}

	/**
	 * Track all users' current location, then check if any new rewards are triggered
	 *
	 * For each user:
	 * Gets users location from GpsService
	 * Then adds this location to user's visited locations
	 * Then asks reward service to calculate user's rewards to update with new location
	 *
	 */
	public void trackAllUserLocationsAndProcess() {


		List<User> allUsers = userService.getAllUsers();

		ArrayList<CompletableFuture> futures = new ArrayList<>();

		logger.debug("trackAllUserLocationsAndProcess: Creating futures for " + allUsers.size() + " user(s)");
		allUsers.forEach((n)-> {
			futures.add(
					CompletableFuture.supplyAsync(()-> {
								return userService.addToVisitedLocations(gpsService.getUserLocation(n.getUserId()), n.getUserName());
							}, executorService)
							.thenAccept(y -> {rewardsService.calculateRewards(n);})
			);
		});
		logger.debug("trackAllUserLocationsAndProcess: Futures created: " + futures.size() + ". Getting futures...");
		futures.forEach((n)-> {
			try {
				n.get();
			} catch (InterruptedException e) {
				logger.error("Track All Users And Process InterruptedException: " + e);
			} catch (ExecutionException e) {
				logger.error("Track All Users And Process ExecutionException: " + e);
			}
		});
		logger.debug("Done!");

	}

	/**
	 * Process any pending rewards for all users
	 *
	 * For each user:
	 * Asks reward service to calculate user's rewards to update with any new visited locations
	 *
	 */
	public void processAllUserRewards() {


		List<User> allUsers = userService.getAllUsers();

		ArrayList<CompletableFuture> futures = new ArrayList<>();

		logger.debug("processAllUserRewards: Creating threads for " + allUsers.size() + " user(s)");
		allUsers.forEach((n)-> {
			futures.add(
					CompletableFuture.supplyAsync(()-> {
						return rewardsService.calculateRewardsReturn(n);
					}, executorService)
			);
		});
		logger.debug("processAllUserRewards: Futures created: " + futures.size() + ". Getting futures...");
		futures.forEach((n)-> {
			try {
				n.get();
			} catch (InterruptedException e) {
				logger.error("Process All User Rewards InterruptedException: " + e);
			} catch (ExecutionException e) {
				logger.error("Process All User Rewards ExecutionException: " + e);
			}
		});
		logger.debug("processAllUserRewards: Done!");

	}

	/**
	 * Get all users' current locations from UserService
	 *
	 * @return List of all users' current locations as UserLocation objects
	 */
	public List<UserLocation> getAllCurrentLocations() {
		return userService.getAllCurrentLocations();
	}

	/**
	 * Get five nearest attractions for provided location
	 *
	 * Gets all attractions from GpsService, and sorts by distance from provided location.
	 * Then returns the nearest five (or as many as possible, if GpsService has less than 5 total).
	 *
	 * @param visitedLocation location to be used
	 * @return List of five NearbyAttraction objects
	 */
	public List<NearbyAttraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<Attraction> attractionsList;
		Map<Double, Attraction> attractionsMap = new HashMap<>();

		//Create Map of Distance/Location, place into TreeMap to sort by distance
		gpsService.getAttractions().forEach((n)-> {
			attractionsMap.put(getDistance(n, visitedLocation.location), n);
		});
		TreeMap<Double, Attraction> sortedAttractionMap = new TreeMap<>(attractionsMap);

		//Create ArrayList containing closest 5 attractions
		if (sortedAttractionMap.size() >= 5) {
			attractionsList = new ArrayList<>(sortedAttractionMap.values()).subList(0,5);
		}
		else {
			logger.debug("getNearByAttractions: gpsService provided less than 5 attractions");
			attractionsList = new ArrayList<>(sortedAttractionMap.values()).subList(0,sortedAttractionMap.size());
		}

		//Create list of output entities containing only desired data
		List<NearbyAttraction> output = new ArrayList<>();
		attractionsList.forEach((n)-> {output.add(new NearbyAttraction(n.attractionName,
				n.latitude, n.longitude, visitedLocation.location.latitude, visitedLocation.location.longitude,
				getDistance(n, visitedLocation.location), rewardsService.getRewardValue(n, visitedLocation.userId)));

		});
		return output;
	}

	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
	private final int defaultProximityBuffer = 10;
	private final int proximityBuffer = defaultProximityBuffer;
	private final int attractionProximityRange = 200;

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	private double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				executorService.shutdown();tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 *
	 * Methods Below: For Internal Testing
	 *
	 **********************************************************************************/
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			userService.addUser(user);
		});

		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return (leftLimit + new Random().nextDouble() * (rightLimit - leftLimit));
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}