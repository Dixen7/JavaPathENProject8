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

@RestController
public class TourGuideController {

    private Logger logger = LoggerFactory.getLogger(TourGuideController.class);

    @Autowired
    TourGuideService tourGuideService;

    @Autowired
    UserService userService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
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
    
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
        logger.info("getAllCurrentLocations: endpoint called. Returning all current locations");
        return JsonStream.serialize(tourGuideService.getAllCurrentLocations());
    }
    
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