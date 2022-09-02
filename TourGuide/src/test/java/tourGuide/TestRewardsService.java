package tourGuide;

import static org.junit.Assert.*;

import java.util.*;


import gpsUtil.location.Location;

import org.junit.Before;

import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.*;
import tourGuide.model.user.User;
import tourGuide.model.user.UserReward;
import tripPricer.TripPricer;

@RunWith(MockitoJUnitRunner.class)
public class TestRewardsService {

	@Mock
	private GpsUtil gpsUtilMock;

	@Before
	public void setUp() {
		Locale.setDefault(new Locale("en", "US"));
	}

	@Test
	public void userGetRewards() {
		GpsUtil gpsUtil = new GpsUtil();
		GpsService gpsService = new GpsService(gpsUtil);
		UserService userService = new UserService();
		TripService tripService = new TripService(new TripPricer());
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);

		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, userService, tripService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		Attraction attraction = gpsUtil.getAttractions().get(5);
		user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
		tourGuideService.addUser(user);

		rewardsService.calculateRewards(user);

		User updatedUser = userService.getUserByUsername(user.getUserName());

		List<UserReward> userRewards = updatedUser.getUserRewards();
		tourGuideService.tracker.stopTracking();

		System.out.println("Size: " + userRewards.size());
		assertTrue(userRewards.size() == 1);
	}

	@Test
	public void isWithinAttractionProximity() {
		GpsService gpsService = new GpsService(new GpsUtil());
		UserService userService = new UserService();
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);
		Attraction attraction = gpsService.getAttractions().get(0);
		assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}

	@Test
	public void nearAllAttractions() throws ConcurrentModificationException {
		GpsService gpsService = new GpsService(gpsUtilMock);
		UserService userService = new UserService();
		TripService tripService = new TripService(new TripPricer());
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);

		InternalTestHelper.setInternalUserNumber(1);
		TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, userService, tripService);

		rewardsService.calculateRewards(tourGuideService.getAllUsers().get(0));
		List<UserReward> userRewards = tourGuideService.getUserRewards(tourGuideService.getAllUsers().get(0).getUserName());
		tourGuideService.tracker.stopTracking();

		assertEquals(gpsService.getAttractions().size(), userRewards.size());
	}

}
