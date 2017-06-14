package se.zinokader.spotiq.constant;

public class ApplicationConstants {

    private ApplicationConstants() {}

    /* Request codes */
    public static final int LOGIN_INTENT_REQUEST_CODE = 5473;
    public static final int SEARCH_INTENT_REQUEST_CODE = 3125;

    /* Time and timing-related */
    public static final int SHORT_VIBRATION_DURATION = 30;
    public static final int SHORT_ACTION_DELAY = 1;
    public static final int MEDIUM_ACTION_DELAY = 2;

    /* Image-related */
    public static final String ALBUM_ART_PLACEHOLDER_URL = "https://www.zinokader.se/img/spotiq-album-placeholder.png";
    public static final String PROFILE_IMAGE_PLACEHOLDER_URL = "https://www.zinokader.se/img/spotiq-profile-placeholder.png";
    public static final int DEFAULT_TRACKLIST_CROP_WIDTH = 600;
    public static final int DEFAULT_TRACKLIST_CROP_HEIGHT = 200;
    public static final int DEFAULT_TRACKLIST_BLUR_RADIUS = 100;

    /* Activity & Lifecycle-related */
    //Party-related
    public static final String PARTY_NAME_EXTRA = "party_name_extra";
    public static final int DEFAULT_DELAY_MS = 500;
    public static final int TAB_TRACKLIST_INDEX = 0;
    public static final int TAB_PARTY_MEMBERS_INDEX = 1;

    /* Search-related */
    public static final int DEFAULT_DEBOUNCE_MS = 400;

}
