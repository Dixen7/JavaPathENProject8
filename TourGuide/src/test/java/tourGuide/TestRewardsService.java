package tourGuide;

import static org.junit.Assert.*;

import java.util.*;

import gpsUtil.location.Location;
import org.junit.Ignore;
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
	private GpsUtil gpsUtil;

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

		rewardsService.calculateRewardsReturn(user);

		User updatedUser = userService.getUserByUsername(user.getUserName());

		List<UserReward> userRewards = updatedUser.getUserRewards();
		tourGuideService.tracker.stopTracking();
		assertTrue(userRewards.size() == 1);
	}

	@Test
	public void isWithinAttractionProximity() {
		GpsService gpsService = new GpsService(new GpsUtil());
		UserService userService = new UserService();
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);
		Attraction attraction = gpsService.getAttractions().get(0);
		Location attractionLocation = new Location(attraction.latitude, attraction.longitude);
		assertTrue(rewardsService.isWithinAttractionProximity(attraction, attractionLocation));
	}

	@Test
	public void nearAllAttractionggs() throws ConcurrentModificationException {
		GpsService gpsService = new GpsService(gpsUtil);
		UserService userService = new UserService();
		TripService tripService = new TripService(new TripPricer());
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);

		InternalTestHelper.setInternalUserNumber(1);
		TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, userService, tripService);

		Attraction attractionResponse = new Attraction("Disneyland", "Anaheim", "CA", 33.817595D, -117.922008D);
		List<Attraction> attractionResponseList = new ArrayList<>();
		attractionResponseList.add(attractionResponse);
		Mockito.when(gpsUtil.getAttractions()).thenReturn(attractionResponseList);

		rewardsService.calculateRewards(tourGuideService.getAllUsers().get(0));
		List<UserReward> userRewards = tourGuideService.getUserRewards(tourGuideService.getAllUsers().get(0).getUserName());

		assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
	}

}
