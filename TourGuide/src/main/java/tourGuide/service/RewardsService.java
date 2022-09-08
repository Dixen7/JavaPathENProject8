package tourGuide.service;

import java.util.*;
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
	private int threadPoolSize = 50;
	private ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

	public RewardsService(GpsService gpsService, RewardCentral rewardCentral, UserService userService) {
		this.gpsService = gpsService;
		this.rewardsCentral = rewardCentral;
		this.userService = userService;
	}

	public ExecutorService getExecutor() {
		return executorService;
	}

	public void setProximityBuffer(int proximityBuffer) {
		logger.debug("setProximityBuffer: updating proximity buffer to " + proximityBuffer);
		this.proximityBuffer = proximityBuffer;
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	public boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

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
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsService.getAttractions();

		userLocations.forEach(visitedLocation -> {
			attractions.forEach(a -> {
				if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(a.attractionName)).count() == 0) {
					if(nearAttraction(visitedLocation, a)) {
						user.addUserReward(new UserReward(visitedLocation, a, getRewardPoints(a, user.getUserId())));
					}
				}
			});
		});
	}
}