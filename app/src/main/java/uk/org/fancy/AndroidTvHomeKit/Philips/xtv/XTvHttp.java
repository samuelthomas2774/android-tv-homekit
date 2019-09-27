package uk.org.fancy.AndroidTvHomeKit.Philips.xtv;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.LinkedList;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import android.util.Log;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;

public class XTvHttp {
    private final static String TAG = "HomeKit:XTvHttp";
    final static String baseUrl = "http://127.0.0.1:1925/6/";
    final static String secureBaseUrl = "https://127.0.0.1:1926/6/";
    private String username;
    private String password;
    protected final AsyncHttpClient http = new DefaultAsyncHttpClient(
        new DefaultAsyncHttpClientConfig.Builder()
            .setUseInsecureTrustManager(true)
            .build()
    );
    public final XTvAmbilightHttp ambilight = new XTvAmbilightHttp(this);

    public XTvHttp(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public CompletableFuture<Response> get(String url) {
        String pathname;
        try {
            pathname = (new URL(url)).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        String authorizationHeader = XTvUtil.generateAuthorizationHeader(username, password, "GET", pathname);

        Log.d(TAG, "Sending GET " + url + " (authorization: " + authorizationHeader + ")");

        CompletableFuture<Response> whenResponse = http
            .prepareGet(url)
            .addHeader("Authorization", authorizationHeader)
            .execute()
            .toCompletableFuture();

        return whenResponse;
    }

    public CompletableFuture<Response> post(String url, JsonObject object) {
        StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(object);
        return post(url, "application/json", writer.toString());
    }

    public CompletableFuture<Response> post(String url, String mimeType, String body) {
        String pathname;
        try {
            pathname = (new URL(url)).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        String authorizationHeader = XTvUtil.generateAuthorizationHeader(username, password, "POST", pathname);

        Log.d(TAG, "Sending POST " + url + " (authorization: " + authorizationHeader + ") " + body);

        CompletableFuture<Response> whenResponse = http
            .preparePost(url)
            .addHeader("Authorization", authorizationHeader)
            .addHeader("Content-Type", mimeType)
            .setBody(body)
            .execute()
            .toCompletableFuture();

        return whenResponse;
    }

    public class System {
        public final String menulanguage;
        public final String name;
        public final String country;
        public final String serialNumber;
        public final String softwareVersion;
        public final String model;
        public final String deviceId;
        public final String nettvVersion;
        public final String apiVersion;
        public final String osType;

        public System(Response response) {
            String body = response.getResponseBody();
            Log.d(TAG, "Got /system response " + body);
            JsonObject system = Json.createReader(new StringReader(body)).readObject();

            menulanguage = system.getString("menulanguage");
            name = system.getString("name");
            country = system.getString("country");
            serialNumber = XTvUtil.decrypt(system.getString("serialnumber_encrypted"));
            softwareVersion = XTvUtil.decrypt(system.getString("softwareversion_encrypted"));
            model = XTvUtil.decrypt(system.getString("model_encrypted"));
            deviceId = XTvUtil.decrypt(system.getString("deviceid_encrypted"));
            nettvVersion = system.getString("nettvversion");
            JsonObject apiVersionObject = system.getJsonObject("api_version");
            apiVersion = Integer.toString(apiVersionObject.getInt("Major")) + "." +
                Integer.toString(apiVersionObject.getInt("Minor")) + "." +
                Integer.toString(apiVersionObject.getInt("Patch"));
            osType = system.getString("os_type");
        }
    }

    public CompletableFuture<System> getSystem() {
        return this.get(baseUrl + "system").thenApply(response -> new System(response));
    }

    public enum PowerState {
        ON,
        STANDBY,
        STANDBY_WAKE_LOCK,
    }

    public CompletableFuture<PowerState> getPowerState() {
        return this.get(secureBaseUrl + "powerstate").thenApply(response -> {
            String body = response.getResponseBody();
            Log.d(TAG, "Got /powerstate response " + body);
            JsonObject powerstate = Json.createReader(new StringReader(body)).readObject();

            switch (powerstate.getString("powerstate")) {
                case "On": return PowerState.ON;
                default: case "Standby": return PowerState.STANDBY;
                case "StandbyKeep": return PowerState.STANDBY_WAKE_LOCK;
            }
        });
    }

    public CompletableFuture<Object> setPowerState(PowerState powerState) {
        return this.post(secureBaseUrl + "powerstate", Json.createObjectBuilder()
            .add("powerstate", powerState == PowerState.ON ? "On" :
                powerState == PowerState.STANDBY_WAKE_LOCK ? "StandbyKeep" :
                "Standby"
            ).build()
        ).thenApply(response -> {
            Log.d(TAG, "POST /powerstate response " + response.getResponseBody());
            return null;
        });
    }

    public class Application {
        public final String label;
        public final ApplicationIntent intent;
        public final int order;
        public final String id;
        public final ApplicationType type;

        public Application(String id, ApplicationType type, String label, int order, ApplicationIntent intent) {
            this.label = label;
            this.intent = intent;
            this.order = order;
            this.id = id;
            this.type = type;
        }

        public Application(JsonObject application) {
            label = application.getString("label");
            JsonObject intentObject = application.getJsonObject("intent");
            JsonObject componentObject = intentObject.getJsonObject("component");
            intent = new ApplicationIntent(
                componentObject.getString("packageName"),
                componentObject.getString("className"),
                intentObject.getString("action")
            );
            order = application.getInt("order");
            id = application.getString("id");
            type = application.getString("type") == "game" ? ApplicationType.GAME : ApplicationType.APP;
        }
    }

    public class ApplicationIntent {
        public final Activity activity;
        public final String action;

        public ApplicationIntent(String packageName, String className, String action) {
            this.activity = new Activity(packageName, className);
            this.action = action;
        }
    }

    public class Activity {
        public final String packageName;
        public final String className;

        public Activity(String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
        }

        public Activity(JsonObject activity) {
            this.packageName = activity.getString("packageName");
            this.className = activity.getString("className") == "NA" ? null : activity.getString("className");
        }
    }

    public enum ApplicationType {
        APP,
        GAME,
    }

    public CompletableFuture<List<Application>> getApplications() {
        return this.get(secureBaseUrl + "applications").thenApply(response -> {
            String body = response.getResponseBody();
            Log.d(TAG, "Got /applications response " + body);

            JsonObject applicationsObject = Json.createReader(new StringReader(body)).readObject();
            List<Application> applications = new LinkedList<Application>();

            for (JsonValue application: applicationsObject.getJsonArray("applications")) {
                if (!(application instanceof JsonObject)) continue;

                applications.add(new Application((JsonObject) application));
            }

            return applications;
        });
    }

    public CompletableFuture<Activity> getCurrentActivity() {
        return this.get(secureBaseUrl + "activities/current").thenApply(response -> {
            String body = response.getResponseBody();
            Log.d(TAG, "Got /activities/current response " + body);

            JsonObject activity = Json.createReader(new StringReader(body)).readObject();

            return new Activity(activity.getJsonObject("component"));
        });
    }

    public CompletableFuture<Object> launch(ApplicationIntent intent) {
        return this.post(secureBaseUrl + "activities/launch", Json.createObjectBuilder()
            .add("intent", Json.createObjectBuilder()
                .add("component", Json.createObjectBuilder()
                    .add("packageName", intent.activity.packageName)
                    .add("className", intent.activity.className))
                .add("action", intent.action)
            ).build()
        ).thenApply(response -> {
            Log.d(TAG, "POST /activities/launch response " + response.getResponseBody());
            return null;
        });
    }

    public class AudioSettings {
        public final int MINIMUM_VOLUME;
        public final int MAXIMUM_VOLUME;
        public final boolean mute;
        public final int volume;

        public AudioSettings(int min, int max, boolean mute, int volume) {
            this.MINIMUM_VOLUME = min;
            this.MAXIMUM_VOLUME = min;
            this.mute = mute;
            this.volume = volume;
        }

        public AudioSettings(JsonObject audio) {
            MINIMUM_VOLUME = audio.getInt("min");
            MAXIMUM_VOLUME = audio.getInt("max");
            mute = audio.getBoolean("muted");
            volume = audio.getInt("current");
        }
    }

    public CompletableFuture<AudioSettings> getAudioSettings() {
        return this.get(secureBaseUrl + "audio/volume").thenApply(response -> {
            String body = response.getResponseBody();
            return new AudioSettings(Json.createReader(new StringReader(body)).readObject());
        });
    }

    public CompletableFuture<Object> setAudioSettings(boolean mute, int volume) {
        return this.post(secureBaseUrl + "audio/volume", Json.createObjectBuilder()
            .add("muted", mute)
            .add("current", volume)
            .build()
        ).thenApply(response -> {
            Log.d(TAG, "POST /audio/volume response " + response.getResponseBody());
            return null;
        });
    }

    public CompletableFuture<Boolean> getMute() {
        return getAudioSettings().thenApply(audio -> audio.mute);
    }

    public CompletableFuture<Object> setMute(boolean mute) {
        if (mute) {
            return setAudioSettings(mute, 0);
        } else {
            return getAudioSettings().thenApply(audio -> {
                return setAudioSettings(mute, audio.volume);
            });
        }
    }

    public CompletableFuture<Integer> getVolume() {
        return getAudioSettings().thenApply(audio -> audio.volume);
    }

    public CompletableFuture<Object> setVolume(int volume) {
        return setAudioSettings(false, volume);
    }

    public class RemoteKey {
        public static final String STANDBY = "Standby";
        public static final String CURSOR_UP = "CursorUp";
        public static final String CURSOR_DOWN = "CursorDown";
        public static final String CURSOR_LEFT = "CursorLeft";
        public static final String CURSOR_RIGHT = "CursorRight";
        public static final String CONFIRM = "Confirm";
        public static final String BACK = "Back";
        public static final String EXIT = "Exit";
        public static final String TV = "WatchTV";
        public static final String HOME = "Home";
        public static final String SOURCE = "Source";
        public static final String LIST = "List";
        public static final String FIND = "Find";
        public static final String OPTIONS = "Options";
        public static final String ADJUST = "Adjust";
        public static final String COLOUR_RED = "RedColour";
        public static final String COLOUR_GREEN = "GreenColour";
        public static final String COLOUR_YELLOW = "YellowColour";
        public static final String COLOUR_BLUE = "BlueColour";
        public static final String PLAY = "Play";
        public static final String PLAY_PAUSE = "PlayPause";
        public static final String PAUSE = "Pause";
        public static final String FAST_FORWARD = "FastForward";
        public static final String STOP = "Stop";
        public static final String REWIND = "Rewind";
        public static final String RECORD = "Record";
        public static final String CHANNEL_UP = "ChannelStepUp";
        public static final String CHANNEL_DOWN = "ChannelStepDown";
        public static final String DIGIT_0 = "Digit0";
        public static final String DIGIT_1 = "Digit1";
        public static final String DIGIT_2 = "Digit2";
        public static final String DIGIT_3 = "Digit3";
        public static final String DIGIT_4 = "Digit4";
        public static final String DIGIT_5 = "Digit5";
        public static final String DIGIT_6 = "Digit6";
        public static final String DIGIT_7 = "Digit7";
        public static final String DIGIT_8 = "Digit8";
        public static final String DIGIT_9 = "Digit9";
        public static final String DOT = "Dot";
        public static final String VOLUME_UP = "VolumeUp";
        public static final String VOLUME_DOWN = "VolumeDown";
        public static final String MUTE = "Mute";
        public static final String TELETEXT = "Teletext";
        public static final String SUBTITLES = "Subtitle";
        public static final String CLOSED_CAPTIONS = "ClosedCaption";
        public static final String TV_GUIDE = "TvGuide";
        public static final String INFO = "Info";
        public static final String AMBILIGHT = "AmbilightOnOff";
        public static final String VIEW_MODE = "Viewmode";
        public static final String _3D_FORMAT = "3dFormat";
        public static final String MULTIVIEW = "Multiview";
        public static final String PICTURE_STYLE = "PictureStyle";
        public static final String _3D_DEPTH = "3dDepth";
        public static final String SOUND_STYLE = "SoundStyle";
        public static final String SURROUND_MODE = "SurroundMode";
        public static final String HEADPHONES_VOLUME = "HeadphonesVolume";
        public static final String _2_PLAYER_GAMING = "2PlayerGaming";
        public static final String SETUP = "Setup";
        public static final String WHITE_COLOUR = "WhiteColour";
        public static final String POWER_ON = "PowerOn";
        public static final String POWER_OFF = "PowerOff";
        public static final String ONLINE = "Online";
        public static final String SMART_TV = "SmartTV";
        public static final String MENU = "PhilipsMenu";
    }

    public CompletableFuture<Object> keypress(String key) {
        return this.post(secureBaseUrl + "input/key", Json.createObjectBuilder()
            .add("key", key)
            .build()
        ).thenApply(response -> {
            Log.d(TAG, "POST /input/key response " + response.getResponseBody());
            return null;
        });
    }
}
