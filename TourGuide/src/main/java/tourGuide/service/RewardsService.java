package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.model.user.User;
import tourGuide.model.user.UserReward;

/**
 * RewardsService interfaces with RewardCentral and performs associated tasks for main TourGuide application
 */
@Service
public class RewardsService {
	private Logger logger = LoggerFactory.getLogger(RewardsService.class);

	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	private int defaultProximityBuffer = 100;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsService gpsService;
	private final RewardCentral rewardsCentral;
	private final UserService userService;

	@Value("${thread.pool.size}")
	private int threadPoolSize = 50;
	private ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

	public RewardsService(GpsService gpsService, RewardCentral rewardCentral, UserService userService) {
		this.gpsService = gpsService;
		this.rewardsCentral = rewardCentral;
		this.userService = userService;
	}

	public void setProximityBuffer(int proximityBuffer) {
		logger.debug("setProximityBuffer: updating proximity buffer to " + proximityBuffer);
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		logger.debug("setDefaultProximityBuffer: resetting proximity buffer to " + defaultProximityBuffer);
		proximityBuffer = defaultProximityBuffer;
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	//Check if a VisitedLocation is within range of an Attraction
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	//Get reward point value of an attraction for a provided UUID
	private int getRewardPoints(Attraction attraction, UUID userid) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, userid);
	}

	/**
	 * Get Reward Point value of an attraction for a user
	 *
	 * @param userid UUID of user
	 * @param attraction attraction to be checked
	 * @return reward value as integer
	 */
	public int getRewardValue(Attraction attraction, UUID userid) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, userid);
	}

	//Get distance between two locations
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

	/**
	 * Calculate rewards for a provided User
	 *
	 * Checks all user's VisitedLocations, compares each to list of Attractions from GpsService
	 * If a user has visited an attraction (ie visited location is in range of an attraction)
	 * Add reward to user for that attraction if they have not already received a reward for it
	 *
	 * @param user User object
	 */
	public void calculateRewards(User user) {

		ExecutorService executorService = Executors.newFixedThreadPool(200);
		executorService.execute(() -> {
			List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
			List<Attraction> attractions = new CopyOnWriteArrayList<>(gpsService.getAttractions());

			for(VisitedLocation visitedLocation : userLocations) {
				for(Attraction attraction : attractions) {
					if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
						if(nearAttraction(visitedLocation, attraction)) {
							user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user.getUserId())));
						}
					}
				}
			}
		});

		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}


	/**
	 * Calculate rewards for a provided User
	 *
	 * Checks all user's VisitedLocations, compares each to list of Attractions from GpsService
	 * If a user has visited an attraction (ie visited location is in range of an attraction)
	 * Add reward to user for that attraction if they have not already received a reward for it
	 *
	 * @param user User object
	 * @return userName
	 */
	public String calculateRewardsReturn(User user) {

		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsService.getAttractions();

		CopyOnWriteArrayList<CompletableFuture> futures = new CopyOnWriteArrayList<CompletableFuture>();

		for(VisitedLocation visitedLocation : userLocations) {
			for (Attraction attr : attractions) {
				futures.add(
						CompletableFuture.runAsync(()-> {
							if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attr.attractionName)).count() == 0) {

								if(nearAttraction(visitedLocation, attr)) {
									userService.addUserReward(user.getUserName(), visitedLocation, attr, getRewardPoints(attr, user.getUserId()));
								}
							}
						},executorService)
				);
			}
		}

		futures.forEach((n)-> {
			try {
				n.get();
			} catch (InterruptedException e) {
				logger.error("Calculate Rewards InterruptedException: " + e);
			} catch (ExecutionException e) {
				logger.error("Calculate Rewards ExecutionException: " + e);
			}
		});
		return user.getUserName();
	}
	public ExecutorService getExecutorService() {
		return executorService;
	}

}