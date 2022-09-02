package tourGuide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import tourGuide.service.RewardsService;
import tripPricer.TripPricer;

/**
 * TourGuideModule is a configuration class to enable the use of the external libraries used.
 * In production system these would be replaced with classes used to contact external APIs.
 */
@Configuration
public class TourGuideModule {

	@Bean
	public GpsUtil getGpsUtil() {
		return new GpsUtil();
	}

	@Bean
	public TripPricer getTripPricer() {
		return new TripPricer();
	}

	@Bean
	public RewardCentral getRewardCentral() {
		return new RewardCentral();
	}
	
}
