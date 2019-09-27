package uk.org.fancy.AndroidTvHomeKit.Philips.xtv;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArray;
import javax.json.JsonValue;
import javax.json.JsonString;
import android.util.Log;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;

public class XTvAmbilightHttp {
    private final static String TAG = "HomeKit:XTvAmbilightHttp";
    final static String baseUrl = "http://127.0.0.1:1925/6/ambilight/";
    private final XTvHttp xtvhttp;

    public XTvAmbilightHttp(XTvHttp xtvhttp) {
        this.xtvhttp = xtvhttp;
    }

    public CompletableFuture<Response> get(String url) {
        String pathname;
        try {
            pathname = (new URL(url)).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Log.d(TAG, "Sending GET " + url);

        CompletableFuture<Response> whenResponse = xtvhttp.http
            .prepareGet(url)
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

        Log.d(TAG, "Sending POST " + url + " " + body);

        CompletableFuture<Response> whenResponse = xtvhttp.http
            .preparePost(url)
            .addHeader("Content-Type", mimeType)
            .setBody(body)
            .execute()
            .toCompletableFuture();

        return whenResponse;
    }

    public class Topology {
        public final int layers;
        public final int left;
        public final int top;
        public final int bottom;
        public final int right;

        public Topology(int layers, int left, int top, int bottom, int right) {
            this.layers = layers;
            this.left = left;
            this.top = top;
            this.bottom = bottom;
            this.right = right;
        }

        public Topology(JsonObject topology) {
            layers = topology.getInt("layers");
            left = topology.getInt("left");
            top = topology.getInt("top");
            bottom = topology.getInt("bottom");
            right = topology.getInt("right");
        }
    }

    public CompletableFuture<Topology> getTopology() {
        return this.get(baseUrl + "topology").thenApply(response -> {
            String body = response.getResponseBody();
            return new Topology(Json.createReader(new StringReader(body)).readObject());
        });
    }

    public class SupportedStyle {
        public final String name;
        public final List<String> algorithms;
        public final int maxTuning;
        public final int maxSpeed;

        public SupportedStyle(JsonObject supportedStyle) {
            name = supportedStyle.getString("styleName");
            maxTuning = supportedStyle.getInt("maxTuning");
            maxSpeed = supportedStyle.getInt("top");
            
            JsonArray algorithmsArray = supportedStyle.getJsonArray("algorithms");

            if (algorithmsArray != null) {
                List<String> algorithms = new LinkedList<String>();

                for (JsonValue algorithm: algorithmsArray) {
                    if (!(algorithm instanceof JsonString)) continue;

                    algorithms.add(algorithm.toString());
                }

                this.algorithms = Collections.unmodifiableList(algorithms);
            } else {
                algorithms = null;
            }
        }
    }

    public CompletableFuture<List<SupportedStyle>> getSupportedStyles() {
        return this.get(baseUrl + "supportedstyles").thenApply(response -> {
            String body = response.getResponseBody();
            JsonArray supportedStylesArray = Json.createReader(new StringReader(body)).readObject()
                .getJsonArray("supportedStyles");

            List<SupportedStyle> supportedStyles = new LinkedList<SupportedStyle>();

            for (JsonValue supportedStyle: supportedStylesArray) {
                if (!(supportedStyle instanceof JsonObject)) continue;

                supportedStyles.add(new SupportedStyle((JsonObject) supportedStyle));
            }
            
            return Collections.unmodifiableList(supportedStyles);
        });
    }

    public enum EnabledState {
        ON,
        OFF,
    }

    public CompletableFuture<EnabledState> getEnabledState() {
        return this.get(baseUrl + "power").thenApply(response -> {
            String body = response.getResponseBody();
            Log.d(TAG, "Got /power response " + body);
            JsonObject powerstate = Json.createReader(new StringReader(body)).readObject();

            switch (powerstate.getString("power")) {
                case "On": return EnabledState.ON;
                default: case "Off": return EnabledState.OFF;
            }
        });
    }

    public CompletableFuture<Object> setEnabledState(EnabledState enabled) {
        return this.post(baseUrl + "power", Json.createObjectBuilder()
            .add("power", enabled == EnabledState.ON ? "On" : "Off")
            .build()
        ).thenApply(response -> {
            Log.d(TAG, "POST /power response " + response.getResponseBody());
            return null;
        });
    }

    public static class ProcessedColour {
        public final int red;
        public final int green;
        public final int blue;

        public ProcessedColour(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public ProcessedColour(JsonObject processedColour) {
            red = processedColour.getInt("r");
            green = processedColour.getInt("g");
            blue = processedColour.getInt("b");
        }
    }

    public class ProcessedColoursLayer {
        public final int id;
        public final List<ProcessedColour> left;
        public final List<ProcessedColour> top;
        public final List<ProcessedColour> bottom;
        public final List<ProcessedColour> right;

        public ProcessedColoursLayer(int id, JsonObject processedColoursLayer) {
            this.id = id;
            left = toProcessedColourList(processedColoursLayer.getJsonObject("left"));
            top = toProcessedColourList(processedColoursLayer.getJsonObject("top"));
            bottom = toProcessedColourList(processedColoursLayer.getJsonObject("bottom"));
            right = toProcessedColourList(processedColoursLayer.getJsonObject("right"));
        }
    }

    private static List<ProcessedColour> toProcessedColourList(JsonObject processedColoursLayerColours) {
        if (processedColoursLayerColours == null) return null;

        List<ProcessedColour> list = new LinkedList<ProcessedColour>();

        for (JsonValue colour: processedColoursLayerColours.values()) {
            if (!(colour instanceof JsonObject)) continue;

            list.add(new ProcessedColour((JsonObject) colour));
        }

        return Collections.unmodifiableList(list);
    }

    public CompletableFuture<List<ProcessedColoursLayer>> getProcessedColours() {
        return this.get(baseUrl + "processed").thenApply(response -> {
            String body = response.getResponseBody();
            JsonObject processedColoursObject = Json.createReader(new StringReader(body)).readObject();

            List<ProcessedColoursLayer> processedColours = new LinkedList<ProcessedColoursLayer>();

            for (String key: processedColoursObject.keySet()) {
                if (key.substring(0, 5) != "layer") continue;
                JsonValue layer = processedColoursObject.get(key);
                if (!(layer instanceof JsonObject)) continue;

                processedColours.add(new ProcessedColoursLayer(Integer.parseInt(key.substring(5)), (JsonObject) layer));
            }
            
            return Collections.unmodifiableList(processedColours);
        });
    }

    public CompletableFuture<List<ProcessedColoursLayer>> getMeasuredColours() {
        return this.get(baseUrl + "measured").thenApply(response -> {
            String body = response.getResponseBody();
            JsonObject processedColoursObject = Json.createReader(new StringReader(body)).readObject();

            List<ProcessedColoursLayer> processedColours = new LinkedList<ProcessedColoursLayer>();

            for (String key: processedColoursObject.keySet()) {
                if (key.substring(0, 5) != "layer") continue;
                JsonValue layer = processedColoursObject.get(key);
                if (!(layer instanceof JsonObject)) continue;

                processedColours.add(new ProcessedColoursLayer(Integer.parseInt(key.substring(5)), (JsonObject) layer));
            }
            
            return Collections.unmodifiableList(processedColours);
        });
    }

    public CompletableFuture<List<ProcessedColoursLayer>> getCachedColours() {
        return this.get(baseUrl + "cached").thenApply(response -> {
            String body = response.getResponseBody();
            JsonObject processedColoursObject = Json.createReader(new StringReader(body)).readObject();

            List<ProcessedColoursLayer> processedColours = new LinkedList<ProcessedColoursLayer>();

            for (String key: processedColoursObject.keySet()) {
                if (key.substring(0, 5) != "layer") continue;
                JsonValue layer = processedColoursObject.get(key);
                if (!(layer instanceof JsonObject)) continue;

                processedColours.add(new ProcessedColoursLayer(Integer.parseInt(key.substring(5)), (JsonObject) layer));
            }
            
            return Collections.unmodifiableList(processedColours);
        });
    }

    // TODO: set cached colours

    public static class Colour {
        public final int hue; // 0-255
        public final int saturation; // 0-255
        public final int brightness; // 0-255

        public Colour(int hue, int saturation, int brightness) {
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;
        }

        public Colour(JsonObject colour) {
            hue = colour.getInt("hue");
            saturation = colour.getInt("saturation");
            brightness = colour.getInt("brightness");
        }

        public JsonObject toJsonObject() {
            return Json.createObjectBuilder()
                .add("hue", hue)
                .add("saturation", saturation)
                .add("brightness", brightness)
                .build();
        }
    }

    public class LoungeConfiguration {
        public final Colour colour;
        public final Colour delta;
        public final int speed;
        public final LoungeMode mode;

        public LoungeConfiguration(Colour colour, Colour delta, int speed, LoungeMode mode) {
            this.colour = colour;
            this.delta = delta;
            this.speed = speed;
            this.mode = mode;
        }

        public LoungeConfiguration(JsonObject configuration) {
            colour = new Colour(configuration.getJsonObject("color"));
            delta = new Colour(configuration.getJsonObject("colordelta"));
            speed = configuration.getInt("speed");

            switch (configuration.getString("mode")) {
                case "Default": mode = LoungeMode.DEFAULT; break;
                default: case "Automatic": mode = LoungeMode.AUTOMATIC; break;
            }
        }
    }

    public enum LoungeMode {
        DEFAULT, // Default
        AUTOMATIC, // Automatic
    }

    // TODO: lounge mode

    public static class Configuration {
        public final Style style;
        public final VideoAlgorithm videoAlgorithm;
        public final AudioAlgorithm audioAlgorithm;
        public final ColourPreset colour;
        public final Colour manualColour;
        public final Colour manualColourDelta;
        public final int speed;
        public final boolean isExpert;

        public Configuration(Style style) {
            this.style = style;
            videoAlgorithm = null;
            audioAlgorithm = null;
            colour = null;
            manualColour = null;
            manualColourDelta = null;
            speed = 0;
            isExpert = false;
        }

        public Configuration(VideoAlgorithm algorithm) {
            style = Style.FOLLOW_VIDEO;
            videoAlgorithm = algorithm;
            audioAlgorithm = null;
            colour = null;
            manualColour = null;
            manualColourDelta = null;
            speed = 0;
            isExpert = false;
        }

        public Configuration(AudioAlgorithm algorithm) {
            style = Style.FOLLOW_AUDIO;
            videoAlgorithm = null;
            audioAlgorithm = algorithm;
            colour = null;
            manualColour = null;
            manualColourDelta = null;
            speed = 0;
            isExpert = false;
        }

        public Configuration(ColourPreset algorithm) {
            style = Style.FOLLOW_COLOUR;
            videoAlgorithm = null;
            audioAlgorithm = null;
            colour = algorithm;
            manualColour = null;
            manualColourDelta = null;
            speed = 0;
            isExpert = false;
        }

        public Configuration(Colour colour, Colour delta, int speed) {
            style = Style.FOLLOW_COLOUR;
            videoAlgorithm = null;
            audioAlgorithm = null;
            this.colour = null;
            manualColour = colour;
            manualColourDelta = delta;
            this.speed = speed;
            isExpert = true;
        }
        public Configuration(Colour colour, Colour delta) {
            this(colour, delta, 1);
        }
        public Configuration(Colour colour) {
            this(colour, new Colour(0, 0, 0), 1);
        }

        public Configuration(JsonObject configuration) {
            switch (configuration.getString("styleName")) {
                case "OFF": style = Style.OFF; break;
                case "FOLLOW_VIDEO": style = Style.FOLLOW_VIDEO; break;
                case "FOLLOW_AUDIO": style = Style.FOLLOW_AUDIO; break;
                case "FOLLOW_COLOR": style = Style.FOLLOW_COLOUR; break;
                default: style = Style.UNKNOWN; break;
            }

            isExpert = configuration.getBoolean("isExpert");

            if (style == Style.FOLLOW_VIDEO) {
                switch (configuration.getString("menuSetting")) {
                    case "STANDARD": videoAlgorithm = VideoAlgorithm.STANDARD; break;
                    case "NATURAL": videoAlgorithm = VideoAlgorithm.NATURAL; break;
                    case "IMMERSIVE": videoAlgorithm = VideoAlgorithm.IMMERSIVE; break;
                    case "VIVID": videoAlgorithm = VideoAlgorithm.VIVID; break;
                    case "GAME": videoAlgorithm = VideoAlgorithm.GAME; break;
                    case "COMFORT": videoAlgorithm = VideoAlgorithm.COMFORT; break;
                    case "RELAX": videoAlgorithm = VideoAlgorithm.RELAX; break;
                    default: videoAlgorithm = null; break;
                }
            } else videoAlgorithm = null;

            if (style == Style.FOLLOW_AUDIO) {
                switch (configuration.getString("menuSetting")) {
                    case "ENERGY_ADAPTIVE_BRIGHTNESS": audioAlgorithm = AudioAlgorithm.ENERGY_ADAPTIVE_BRIGHTNESS; break;
                    case "ENERGY_ADAPTIVE_COLORS": audioAlgorithm = AudioAlgorithm.ENERGY_ADAPTIVE_COLORS; break;
                    case "VU_METER": audioAlgorithm = AudioAlgorithm.VU_METER; break;
                    case "SPECTRUM_ANALYSER": audioAlgorithm = AudioAlgorithm.SPECTRUM_ANALYSER; break;
                    case "KNIGHT_RIDER_ALTERNATING": audioAlgorithm = AudioAlgorithm.KNIGHT_RIDER_ALTERNATING; break;
                    case "RANDOM_PIXEL_FLASH": audioAlgorithm = AudioAlgorithm.RANDOM_PIXEL_FLASH; break;
                    case "MODE_RANDOM": audioAlgorithm = AudioAlgorithm.MODE_RANDOM; break;
                    default: audioAlgorithm = null; break;
                }
            } else audioAlgorithm = null;

            if (style == Style.FOLLOW_COLOUR && !isExpert) {
                switch (configuration.getString("menuSetting")) {
                    case "HOT_LAVA": colour = ColourPreset.HOT_LAVA; break;
                    case "DEEP_WATER": colour = ColourPreset.DEEP_WATER; break;
                    case "FRESH_NATURE": colour = ColourPreset.FRESH_NATURE; break;
                    case "ISF": colour = ColourPreset.ISF; break;
                    case "PTA_LOUNGE": colour = ColourPreset.PTA_LOUNGE; break;
                    default: colour = null; break;
                }
            } else colour = null;

            if (style == Style.FOLLOW_COLOUR && isExpert) {
                JsonObject colours = configuration.getJsonObject("colorSettings");
                manualColour = new Colour(colours.getJsonObject("color"));
                manualColourDelta = new Colour(colours.getJsonObject("colorDelta"));
                speed = colours.getInt("speed");
            } else {
                manualColour = null;
                manualColourDelta = null;
                speed = 0;
            }
        }

        public JsonObject toJsonObject() {
            if (style == Style.FOLLOW_VIDEO) return Json.createObjectBuilder()
                .add("styleName", "FOLLOW_VIDEO")
                .add("menuSetting", videoAlgorithm == VideoAlgorithm.STANDARD ? "STANDARD" :
                    videoAlgorithm == VideoAlgorithm.NATURAL ? "NATURAL" :
                    videoAlgorithm == VideoAlgorithm.IMMERSIVE ? "IMMERSIVE" :
                    videoAlgorithm == VideoAlgorithm.VIVID ? "VIVID" :
                    videoAlgorithm == VideoAlgorithm.GAME ? "GAME" :
                    videoAlgorithm == VideoAlgorithm.COMFORT ? "COMFORT" :
                    videoAlgorithm == VideoAlgorithm.RELAX ? "RELAX" : null)
                .build();

            if (style == Style.FOLLOW_AUDIO) return Json.createObjectBuilder()
                .add("styleName", "FOLLOW_AUDIO")
                .add("menuSetting",
                    audioAlgorithm == AudioAlgorithm.ENERGY_ADAPTIVE_BRIGHTNESS ? "ENERGY_ADAPTIVE_BRIGHTNESS" :
                    audioAlgorithm == AudioAlgorithm.ENERGY_ADAPTIVE_COLORS ? "ENERGY_ADAPTIVE_COLORS" :
                    audioAlgorithm == AudioAlgorithm.VU_METER ? "VU_METER" :
                    audioAlgorithm == AudioAlgorithm.SPECTRUM_ANALYSER ? "SPECTRUM_ANALYSER" :
                    audioAlgorithm == AudioAlgorithm.KNIGHT_RIDER_ALTERNATING ? "KNIGHT_RIDER_ALTERNATING" :
                    audioAlgorithm == AudioAlgorithm.RANDOM_PIXEL_FLASH ? "RANDOM_PIXEL_FLASH" :
                    audioAlgorithm == AudioAlgorithm.MODE_RANDOM ? "MODE_RANDOM" : null)
                .build();

            if (style == Style.FOLLOW_COLOUR && !isExpert) return Json.createObjectBuilder()
                .add("styleName", "FOLLOW_COLOR")
                .add("menuSetting", colour == ColourPreset.HOT_LAVA ? "HOT_LAVA" :
                    colour == ColourPreset.DEEP_WATER ? "DEEP_WATER" :
                    colour == ColourPreset.FRESH_NATURE ? "FRESH_NATURE" :
                    colour == ColourPreset.ISF ? "ISF" :
                    colour == ColourPreset.PTA_LOUNGE ? "PTA_LOUNGE" : null)
                .build();

            if (style == Style.FOLLOW_COLOUR && isExpert) return Json.createObjectBuilder()
                .add("styleName", "FOLLOW_COLOR")
                .add("isExpert", true)
                .add("colorSettings", Json.createObjectBuilder()
                    .add("color", manualColour.toJsonObject())
                    .add("colorDelta", manualColourDelta.toJsonObject())
                    .add("speed", speed)
                    .build())
                .build();

            return Json.createObjectBuilder().add("styleName", "OFF").build();
        }
    }

    public enum Style {
        OFF,
        FOLLOW_VIDEO,
        FOLLOW_AUDIO,
        FOLLOW_COLOUR,
        UNKNOWN,
    }

    public enum VideoAlgorithm {
        STANDARD, // Standard
        NATURAL, // Natural
        IMMERSIVE, // Immersive/Football
        VIVID, // Vivid
        GAME, // Game
        COMFORT, // Comfort
        RELAX, // Relax
    }

    public enum AudioAlgorithm {
        ENERGY_ADAPTIVE_BRIGHTNESS, // Lumina
        ENERGY_ADAPTIVE_COLORS, // Colora
        VU_METER, // Retro
        SPECTRUM_ANALYSER, // Spectrum
        KNIGHT_RIDER_ALTERNATING, // Scanner
        RANDOM_PIXEL_FLASH, // Rhythm
        MODE_RANDOM, // Party
    }

    public enum ColourPreset {
        HOT_LAVA, // Hot Lava/Hot lava
        DEEP_WATER, // Deep Water/Deep water
        FRESH_NATURE, // Fresh Nature/Fresh nature
        ISF, // Warm White
        PTA_LOUNGE, // Cool White/Cool white
    }

    public CompletableFuture<Configuration> getConfiguration() {
        return this.get(baseUrl + "currentconfiguration").thenApply(response -> {
            String body = response.getResponseBody();
            return new Configuration(Json.createReader(new StringReader(body)).readObject());
        });
    }

    public CompletableFuture<Object> setConfiguration(Configuration configuration) {
        StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(configuration.toJsonObject());
        Log.i(TAG, "Setting configuration: " + writer.toString());

        return this.post(baseUrl + "currentconfiguration", configuration.toJsonObject()).thenApply(response -> {
            Log.d(TAG, "POST /currentconfiguration response " + response.getResponseBody());
            return null;
        });
    }

    public CompletableFuture<Object> setConfiguration(Style style) {
        return setConfiguration(new Configuration(style));
    }

    public CompletableFuture<Object> setConfiguration(VideoAlgorithm algorithm) {
        return setConfiguration(new Configuration(algorithm));
    }

    public CompletableFuture<Object> setConfiguration(AudioAlgorithm algorithm) {
        return setConfiguration(new Configuration(algorithm));
    }

    public CompletableFuture<Object> setConfiguration(ColourPreset algorithm) {
        return setConfiguration(new Configuration(algorithm));
    }

    public CompletableFuture<Object> setConfiguration(Colour colour, Colour delta, int speed) {
        return setConfiguration(new Configuration(colour, delta, speed));
    }
    public CompletableFuture<Object> setConfiguration(Colour colour, Colour delta) {
        return setConfiguration(new Configuration(colour, delta));
    }
    public CompletableFuture<Object> setConfiguration(Colour colour) {
        return setConfiguration(new Configuration(colour));
    }
}
