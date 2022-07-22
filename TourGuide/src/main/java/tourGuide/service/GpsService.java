package tourGuide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GpsService interfaces with GpsUtil to perform associated tasks for main TourGuide application
 */
@Service
public class GpsService {
    private Logger logger = LoggerFactory.getLogger(GpsService.class);
    private final GpsUtil gpsUtil;

    public GpsService(GpsUtil gpsUtil) {
        this.gpsUtil = gpsUtil;
    }

    /**
     * Get User Location
     *
     * Queries GpsUtil to get location of user with provided UUID
     *
     * @param userId UUID of user to be located
     * @return current location of user
     */
    public VisitedLocation getUserLocation(UUID userId) {
        return gpsUtil.getUserLocation(userId);
    }

    /**
     * Get Attractions
     *
     * Queries GpsUtil to get all available attractions currently available
     *
     * @return list of Attraction objects for all available attractions
     */
    public List<Attraction> getAttractions() {
        return new ArrayList<>(gpsUtil.getAttractions());
    }

}