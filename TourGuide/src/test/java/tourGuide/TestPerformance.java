package tourGuide;

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.*;

import gpsUtil.location.Location;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.user.UserReward;
import tourGuide.service.*;
import tourGuide.model.user.User;
import tripPricer.TripPricer;

public class TestPerformance {

	@Before
	public void setUp() {
		Locale.setDefault(new Locale("en", "US"));
	}

	/*
	 * A note on performance improvements:
	 *
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *
	 *     		InternalTestHelper.setInternalUserNumber(100000);
	 *
	 *
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent.
	 *
	 *     These are performance metrics that we are trying to hit:
	 *
	 *     highVolumeTrackLocation: 100,000 users within 15 minutes:
	 *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 *
	 *     highVolumeGetRewards: 100,000 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */


	// Users should be incremented up to 100,000, and test finishes within 20 minutes
	private static final int NUMBER_OF_TEST_USERS = 100000;

	@Test
	public void highVolumeTrackLocation() {
		GpsService gpsService = new GpsService(new GpsUtil());
		UserService userService = new UserService();
		TripService tripService = new TripService(new TripPricer());
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);

		InternalTestHelper.setInternalUserNumber(NUMBER_OF_TEST_USERS);
		TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, userService, tripService);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		tourGuideService.trackAllUserLocations();
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

	@Test
	public void highVolumeTrackLocationAndProcessConc() {
		GpsService gpsService = new GpsService(new GpsUtil());
		UserService userService = new UserService();
		TripService tripService = new TripService(new TripPricer());
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);

		InternalTestHelper.setInternalUserNumber(NUMBER_OF_TEST_USERS);
		TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, userService, tripService);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		tourGuideService.trackAllUserLocationsAndProcess();
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

	@Test
	public void highVolumeGetRewards() {

		InternalTestHelper.setInternalUserNumber(NUMBER_OF_TEST_USERS);

		GpsService gpsService = new GpsService(new GpsUtil());
		UserService userService = new UserService();
		TripService tripService = new TripService(new TripPricer());
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);
		TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, userService, tripService);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();


		Attraction attraction = gpsService.getAttractions().get(0);
		Location attractionLocation = new Location(attraction.latitude, attraction.longitude);
		List<User> allUsers = new ArrayList<>();
		allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(u -> {
			u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attractionLocation, new Date()));
		});

		tourGuideService.calculateReward(allUsers);

		for(User user : allUsers) {
			assertTrue(user.getUserRewards().size() > 0);
		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
}
