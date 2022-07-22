package tourGuide.model.user;

import gpsUtil.location.VisitedLocation;

import java.util.UUID;

public class UserLocation {

    public String userId;
    double longitude;
    double latitude;

    public UserLocation(UUID userId, double longitude, double latitude) {
        this.userId = userId.toString();
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public UserLocation(UUID userId, VisitedLocation location) {
        this.userId = userId.toString();
        this.longitude = location.location.longitude;
        this.latitude = location.location.latitude;
    }
}
