package tourGuide.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.model.user.UserReward;
import tourGuide.service.TourGuideService;
import tourGuide.model.user.User;
import tourGuide.service.UserService;
import tripPricer.Provider;

/**
 * RestController for TourGuide endpoints
 *
 */
@RestController
public class TourGuideController {

    private Logger logger = LoggerFactory.getLogger(TourGuideController.class);

    @Autowired
    TourGuideService tourGuideService;

    @Autowired
    UserService userService;

    /**
     * Mapping for root url
     *
     * Takes no parameters, simply returns welcome message
     *
     * @return welcome message String
     */
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    /**
     * Mapping for Get
     *
     * Returns:
     * User location if user exists, in format: {"longitude":xxx,"latitude":yyy}
     * "User Not Found [userName]" if user is not in system
     *
     * @param userName name of user
     * @return Json string of user's current location
     */
    @RequestMapping("/getLocation")
    public String getLocation(@RequestParam String userName) {
        logger.info("getLocation: endpoint called.");
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(userName);
        if (visitedLocation == null) {
            logger.info("getLocation: requested user not found.");
            return JsonStream.serialize("User Not Found [" + userName + "]");
        }
        logger.info("getLocation: returning user location.");
        return JsonStream.serialize(visitedLocation.location);
    }

    /**
     * Mapping for Get
     *
     * Returns:
     * Closest five tourist attractions for the user, regardless of distance.
     * User will always receive atleast five attractions (unless the system contains less than 5 total attractions)
     * Response includes:
     *  -Name of attraction
     *  -Attraction's lat/long
     *  -User's lat/long
     *  -Distance in miles between user's location and attraction
     *  -Reward points value for this attraction/user combination
     *  Returns "User Not Found [userName]" if user is not in system
     *
     * @param userName name of user
     * @return Json string of user's closest five attractions.
     */
    @RequestMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) {
        logger.info("getNearbyAttractions: endpoint called.");
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(userName);
        if (visitedLocation == null) {
            logger.info("getNearbyAttractions: requested user not found.");
            return JsonStream.serialize("User Not Found [" + userName + "]");
        }
        logger.info("getNearbyAttractions: returning nearby attractions.");
        return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation));
    }

    /**
     * Mapping for Get
     *
     * Returns:
     * Json object of user's current rewards, in the form of a List of UserReward objects
     * "User Not Found [userName]" if user is not in system
     *
     * @param userName name of user
     * @return Json string of user's closest five attractions.
     */
    @RequestMapping("/getRewards")
    public String getRewards(@RequestParam String userName) {
        logger.info("getRewards: endpoint called.");
        List<UserReward> userRewards = tourGuideService.getUserRewards(userName);
        if (userRewards == null) {
            logger.info("getRewards: requested user not found.");
            return JsonStream.serialize("User Not Found [" + userName + "]");
        }
        logger.info("getRewards: returning user rewards.");
        return JsonStream.serialize(userRewards);
    }

    /**
     * Mapping for Get
     *
     * Returns:
     * Json object of all users' current locations, in the format:
     * {"userId":idString,"longitude":xxx,"latitude":yyy},{"userId":idString,"longitude":xxx,"latitude":yyy}]
     * If no users are stored, an empty list is returned
     *
     * @return Json string of all users' current locations.
     */
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
        logger.info("getAllCurrentLocations: endpoint called. Returning all current locations");
        return JsonStream.serialize(tourGuideService.getAllCurrentLocations());
    }

    /**
     * Mapping for Get
     *
     * Returns:
     * Json object containing 5 trip deals for user. Includes the attraction name, price, and tripId.
     * "User Not Found [userName]" if user is not in system
     *
     * @param userName user's userName
     * @return Json string of all users' current locations.
     */
    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
        logger.info("getTripDeals: endpoint called.");
        List<Provider> providers = tourGuideService.getTripDeals(userName);
        if (providers == null) {
            logger.info("getTripDeals: requested user not found.");
            return JsonStream.serialize("User Not Found [" + userName + "]");
        }
        logger.info("getTripDeals: returning trip deals for user.");
        return JsonStream.serialize(providers);
    }

}