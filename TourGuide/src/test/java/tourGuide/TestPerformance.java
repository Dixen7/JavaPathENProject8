package tourGuide;

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.*;

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

//	@Test
//	public void highVolumeGetRewards() {
//		GpsService gpsService = new GpsService(new GpsUtil());
//		UserService userService = new UserService();
//		TripService tripService = new TripService(new TripPricer());
//		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);
//
//		// Users should be incremented up to 100,000, and test finishes within 20 minutes
//		InternalTestHelper.setInternalUserNumber(NUMBER_OF_TEST_USERS);
//		StopWatch stopWatch = new StopWatch();
//		stopWatch.start();
//		TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, userService, tripService);
//
//		Attraction attraction = gpsService.getAttractions().get(0);
//		List<User> allUsers = new ArrayList<>();
//		allUsers = tourGuideService.getAllUsers();
//
//
//		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));
//
//		allUsers.forEach(u -> rewardsService.calculateRewards(u));
//
//		ThreadPoolExecutor executor = (ThreadPoolExecutor) rewardsService.getExecutor();
//		while (executor.getActiveCount() > 0 ) {
//			try {
//				TimeUnit.SECONDS.sleep(5);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		for(User user : allUsers) {
//			assertTrue(user.getUserRewards().size() > 0);
//		}
//		stopWatch.stop();
//		tourGuideService.tracker.stopTracking();
//
//		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
//		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
//	}

	@Test
	public void highVolumeGetRewards() {
		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		InternalTestHelper.setInternalUserNumber(NUMBER_OF_TEST_USERS);
		//Create a stopWatch and start it
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		GpsService gpsService = new GpsService(new GpsUtil());
		UserService userService = new UserService();
		TripService tripService = new TripService(new TripPricer());
		RewardsService rewardsService = new RewardsService(gpsService, new RewardCentral(), userService);
		TourGuideService tourGuideService = new TourGuideService(gpsService, rewardsService, userService, tripService);

		tourGuideService.tracker.stopTracking();

		//Create a list of UserModel containing all users
		List<User> allUsers;
		allUsers = tourGuideService.getAllUsers();

		Attraction attraction = gpsService.getAttractions().get(0);

		//Create an executor service with a thread pool of certain amount of threads
		try {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) rewardsService.getExecutor();
			//Execute the code as per in the method "trackUserLocation" in TourGuideService
			for (User user: allUsers) {
				System.out.println(user);

				Runnable runnable = () -> {
					user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
					//tourGuideService.trackUserLocation(user);
					CopyOnWriteArrayList<VisitedLocation> userLocations = new CopyOnWriteArrayList<>();
					List<Attraction> attractions = new CopyOnWriteArrayList<>();

					userLocations.addAll(user.getVisitedLocations());
					attractions.addAll(gpsService.getAttractions());

					userLocations.forEach(v -> {
						attractions.forEach(a -> {
							UUID userId = user.getUserId();
							if (user.getUserRewards().stream().filter(r ->
									r.attraction.attractionName.equals(a.attractionName)).count() == 0) {
								if (rewardsService.nearAttraction(v, a)) {
									user.addUserReward(new UserReward(v, a,
											rewardsService.getRewardValue(a, userId)));
								}
							}
						});
					});
					assertTrue(user.getUserRewards().size() > 0);
				};
				executor.execute(runnable);
			}
			executor.shutdown();
			executor.awaitTermination(15, TimeUnit.MINUTES);

		}
		catch (InterruptedException interruptedException) {
		}

		stopWatch.stop();

		//Asserting part that the time is as performant as wanted
		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
}
