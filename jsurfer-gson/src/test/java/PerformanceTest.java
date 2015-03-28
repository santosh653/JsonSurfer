import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.junit.Test;
import org.leo.json.GsonSurfer;
import org.leo.json.JsonSimpleSurfer;
import org.leo.json.parse.GsonProvider;
import org.leo.json.parse.JsonPathListener;
import org.leo.json.parse.JsonSimpleProvider;
import org.leo.json.parse.ParsingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.leo.json.BuilderFactory.context;
import static org.leo.json.BuilderFactory.root;
import static org.leo.json.parse.SurfingContext.Builder;


public class PerformanceTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(PerformanceTest.class);

    @Test
    public void testLargeJsonJPSimple() throws Exception {
        // use json-simple parser
        JsonSimpleSurfer surfer = new JsonSimpleSurfer();
        // transform json into json-simple model i.e.org.json.simple.JSONObject or org.json.simple.JSONArray
        Builder builder = context().withJsonProvider(new JsonSimpleProvider());
        final AtomicLong counter = new AtomicLong();
        JsonPathListener printListener = new JsonPathListener() {

            @Override
            public void onValue(Object value, ParsingContext context) {
                counter.incrementAndGet();
                LOGGER.trace("value: {}", value);
            }
        };
        builder.bind(root().child("builders").anyChild().child("properties"), printListener).skipOverlappedPath();
        long start = System.currentTimeMillis();
        surfer.surf(new InputStreamReader(Resources.getResource("allthethings.json").openStream()), builder.build());
        LOGGER.info("jsurfer-simple processes {} value in {} millisecond", counter.get(), System.currentTimeMillis()
                - start);

    }

    @Test
    public void testLargeJsonJPGson() throws Exception {
        // use gson parser
        GsonSurfer surfer = new GsonSurfer();
        // transform json into gson model i.e.com.google.gson.JsonElement
        Builder builder = context().withJsonProvider(new GsonProvider());
        final AtomicLong counter = new AtomicLong();
        JsonPathListener printListener = new JsonPathListener() {
            @Override
            public void onValue(Object value, ParsingContext context) {
                counter.incrementAndGet();
                LOGGER.trace("value: {}", value);
            }
        };
        // equivalent to $.builders.*.properties
        builder.bind(root().child("builders").anyChild().child("properties"), printListener).skipOverlappedPath();
        long start = System.currentTimeMillis();
        surfer.surf(new InputStreamReader(Resources.getResource("allthethings.json").openStream()), builder.build());
        LOGGER.info("jsurfer-gson without deep scan processes {} value in {} millisecond", counter.get(), System.currentTimeMillis() - start);
    }

    @Test
    public void testLargeJsonJPGsonWithScan() throws Exception {
        GsonSurfer surfer = new GsonSurfer();
//        for (int i=0;i < 1000;i++) {
        Builder handler = context().withJsonProvider(new GsonProvider());
        final AtomicLong counter = new AtomicLong();
        JsonPathListener printListener = new JsonPathListener() {
            @Override
            public void onValue(Object value, ParsingContext context) {
                counter.incrementAndGet();
                LOGGER.trace("value: {}", value);
            }
        };
        handler.bind(root().child("builders").scan().child("properties").scan().child("product"), printListener);
        long start = System.currentTimeMillis();
        surfer.surf(new InputStreamReader(Resources.getResource("allthethings.json").openStream()), handler.build());
        LOGGER.info("jsurfer-gson with deep scan processes {} value in {} millisecond", counter.get(), System.currentTimeMillis() - start);
//        }
    }

    @Test
    public void testLargeJsonRawGson() throws Exception {
        final AtomicLong counter = new AtomicLong();
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new InputStreamReader(Resources.getResource("allthethings.json").openStream(), "UTF-8"));
        long start = System.currentTimeMillis();
        reader.beginObject();
        reader.nextName();
        // $.builders
        reader.beginObject();
        while (reader.hasNext()) {
            reader.nextName();
            Map element = gson.fromJson(reader, Map.class);
            Object value = ((Map) element.get("properties")).get("branch");
            counter.incrementAndGet();
            LOGGER.trace("JsonPath: {} value: {}", reader.getPath(), value);
        }
        LOGGER.info("Gson processes {} value in {} millisecond", counter.get(), System.currentTimeMillis() - start);
    }

}