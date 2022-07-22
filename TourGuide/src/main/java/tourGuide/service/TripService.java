package tourGuide.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tourGuide.model.user.User;
import tripPricer.Provider;
import tripPricer.TripPricer;

import java.util.List;

/**
 * TripService interfaces with TripPricer and performs associated tasks for main TourGuide application
 */
@Service
public class TripService {

    private Logger logger = LoggerFactory.getLogger(TripService.class);
    private final TripPricer tripPricer;

    @Value("${tripPricer.api.key}")
    private static final String tripPricerApiKey = "test-server-api-key";

    public TripService(TripPricer tripPricer) {
        this.tripPricer = tripPricer;
    }

    /**
     * Get trip deals for a provided User
     * Queries TripPricer with various user details (including preferences such as number of adults/children)
     * TripPricer responds with 5 trip deals for user based on provided requirements
     *
     * @param user user for whom deals are to be found
     * @return suggested deals as a List of Provider objects
     */
    public List<Provider> getTripDeals(User user) {
        int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
                user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
        user.setTripDeals(providers);
        return providers;
    }

}