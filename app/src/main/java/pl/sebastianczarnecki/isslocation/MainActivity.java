package pl.sebastianczarnecki.isslocation;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,MapboxMap.OnMapClickListener {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private Marker marker;

    public Double latitudefromjson;
    public Double longitudefromjson;

    public Double jLatitude;
    public Double jLongitude;

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String USGS_REQUEST_URL = "http://api.open-notify.org/iss-now";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        LocationAsyncTask task = new LocationAsyncTask();
        task.execute();
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;

        Toast.makeText(
                MainActivity.this,
                getString(R.string.tap_on_map_instruction),
                Toast.LENGTH_LONG
        ).show();

        // Create an Icon object for the marker to use
        Icon icon = IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.sokol);

        marker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(38.889462878011365, -77.03525304794312))
                .icon(icon));

        mapboxMap.addOnMapClickListener(this);
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {

        // Toast instructing user to tap on the map
        Toast.makeText(
                MainActivity.this,
                getString(R.string.tap_on_map_instruction),
                Toast.LENGTH_LONG
        ).show();

        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(Double.valueOf(jLatitude), Double.valueOf(jLongitude))) // Sets the new camera position
                .zoom(5) // Sets the zoom
                .bearing(180) // Rotate the camera
                .tilt(30) // Set the camera tilt
                .build(); // Creates a CameraPosition from the builder

        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 4000);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapboxMap != null) {
        }
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public void updateUI(Event location) {

        marker.setPosition(new LatLng(location.latitude, location.longitude));

        jLatitude = Double.valueOf(location.latitude);
        jLongitude = Double.valueOf(location.longitude);

        TextView dateTextView = (TextView) findViewById(R.id.date);
        String total2 = String.valueOf(jLatitude.toString());
        dateTextView.setText(total2);

        // Create the Handler object (on the main thread by default)
        Handler handler = new Handler();
        // Define the code block to be executed
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                new LocationAsyncTask().execute();
                // Do something here on the main thread
                Log.d("Handlers", "Called on main thread");
                // Repeat this the same runnable code block again another 2 seconds
                // 'this' is referencing the Runnable object
                handler.postDelayed(this, 1);
            }
        };
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
    }

    public class LocationAsyncTask extends AsyncTask<URL, Void, Event> {

        @Override
        protected Event doInBackground(URL... urls) {
            // Create URL object
            URL url = createUrl(USGS_REQUEST_URL);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem making the HTTP request.", e);
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object
            Event location = extractFeatureFromJson(jsonResponse);

            // Return the {@link Event} object as the result fo the {@link LocationAsyncTask}
            return location;

        }

        /**
         * Update the screen with the given location (which was the result of the
         * {@link LocationAsyncTask}).
         */
        @Override
        protected void onPostExecute(Event location) {
            if (location == null) {
                return;
            }
            updateUI(location);
        }

        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";

            // If the URL is null, then return early.
            if (url == null) {
                return jsonResponse;
            }

            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();

                // If the request was successful (response code 200),
                // then read the input stream and parse the response.
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }


        public Event extractFeatureFromJson(String locationJSON) {
            // If the JSON string is empty or null, then return early.
            if (TextUtils.isEmpty(locationJSON)) {
                return null;
            }

            try {
                JSONObject baseJsonResponse = new JSONObject(locationJSON);
                JSONObject jsonObject = baseJsonResponse.getJSONObject("iss_position");
                // If there are results in the features array

                latitudefromjson = jsonObject.getDouble("latitude");
                longitudefromjson = jsonObject.getDouble("longitude");

                // Create a new {@link Event} object
                return new Event(latitudefromjson, longitudefromjson);

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
            }
            return null;
        }
    }
}