package tourGuide.service;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tourGuide.model.user.User;
import tourGuide.model.user.UserLocation;
import tourGuide.model.user.UserReward;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * UserService contains our collection of users, and performs associated tasks for main TourGuide application
 *
 * Currently, to allow concurrent access we use a ConcurrentMap to store Users.
 * In a real system we would adapt this class to interface with a database or similar storage solution instead
 */
@Service
public class UserService {
    private Logger logger = LoggerFactory.getLogger(UserService.class);
    private ConcurrentMap<String, User> usersByName;

    public UserService() {
        final int CAPACITY = 100;
        usersByName = new ConcurrentHashMap<>(CAPACITY);
    }

    /**
     * Add a user to the collection
     *
     * @param user User object to be added
     * @return boolean true if successful, false if user already exists with this username
     */
    public boolean addUser(User user){
        if(!usersByName.containsKey(user.getUserName())) {
            usersByName.put(user.getUserName(), user);
            return true;
        }
        return false;
    }

    /**
     * Add a VisitedLocation to a stored User
     *
     * @param visitedLocation VisitedLocation object to be added to user
     * @param userName name of user
     * @return boolean true if successful, false if user not found
     */
    public boolean addToVisitedLocations(VisitedLocation visitedLocation, String userName) {
        User user = usersByName.get(userName);
        if (user != null) {
            user.addToVisitedLocations(visitedLocation);
            return true;
        }
        logger.debug("addToVisitedLocations: user not found with name " + userName + " returning false");
        return false;
    }

    /**
     * Get all users' current locations
     *
     * @return List of UserLocation objects
     */
    public List<UserLocation> getAllCurrentLocations() {
        List<UserLocation> userLocations = new ArrayList<>();
        usersByName.forEach((k,v)-> {
            userLocations.add(new UserLocation(v.getUserId(), v.getLastVisitedLocation()));
        });
        return userLocations;
    }

    /**
     * Generate and add a UserReward to a stored User
     *
     * @param userName user's userName
     * @param visitedLocation the location user was tracked at
     * @param attraction the attraction the reward is for
     * @param rewardPoints the points to be added
     * @return boolean true if successful, false if user not found
     */
    public boolean addUserReward(String userName, VisitedLocation visitedLocation, Attraction attraction, int rewardPoints) {
        User user = getUserByUsername(userName);
        if (user != null) {
            user.addUserReward(new UserReward(visitedLocation, attraction, rewardPoints));
            return true;
        }
        logger.debug("addUserReward: user not found with name " + userName + " returning false");
        return false;
    }

    /**
     * Get all users currently stored in system
     *
     * @return List of User objects
     */
    public List<User> getAllUsers() {
        return usersByName.values().stream().collect(Collectors.toList());
    }

    /**
     * Get user from system by userName
     * Returns null if user does not exist
     *
     * @param userName user's userName
     * @return User object
     */
    public User getUserByUsername(String userName) {
        return usersByName.get(userName);
    }

    /**
     * Get count of all users currently stored in system
     *
     * @return count of all users currently stored in system
     */
    public int getUserCount() {
        return usersByName.size();
    }
}