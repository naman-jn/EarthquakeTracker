package com.njain.earthquaketracker;

import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.njain.earthquaketracker.model.EarthQuake;
import com.njain.earthquaketracker.ui.CustomInfoWindow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private RequestQueue queue;
    private AlertDialog dialog;
    private AlertDialog.Builder dialogBuilder;
    String url="https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_week.geojson";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        queue=Volley.newRequestQueue(this);

        getEarthQuakes();



    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);


        // Add a marker in Sydney and move the camera
//        LatLng guwahati = new LatLng(26.144516666666664, 91.73623500000001);
//        LatLng clockTower=new LatLng(12.8229350,80.0448384);
//        LatLng indCenter= new LatLng(21.1458,79.0882);
//        mMap.addMarker(new MarkerOptions().position(guwahati).title("Marker in Guwahati")
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
//                .alpha(0.7f));
//        mMap.addMarker(new MarkerOptions().position(clockTower).title("Marker in Clock Tower"));
//
//
//       // mMap.moveCamera(CameraUpdateFactory.newLatLng(guwahati));
//       mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(indCenter,4));
//       // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clockTower,11));
//        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);


    }

    public void getEarthQuakes() {
        final EarthQuake earthQuake = new EarthQuake();

        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray features=response.getJSONArray("features");
                    for(int i=0;i<30;i++){

                        JSONObject properties= features.getJSONObject(i).getJSONObject("properties");

                        JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");


                        JSONArray coordinates = geometry.getJSONArray("coordinates");

                        double lon = coordinates.getDouble(0);
                        double lat = coordinates.getDouble(1);

                        earthQuake.setPlace(properties.getString("place"));
                        earthQuake.setType(properties.getString("type"));
                        earthQuake.setTime(properties.getLong("time"));
                        earthQuake.setLat(lat);
                        earthQuake.setLon(lon);
                        earthQuake.setMagnitude(properties.getDouble("mag"));
                        earthQuake.setDetailLink(properties.getString("detail"));

                        java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
                        String formattedDate =  dateFormat.format(new Date(Long.valueOf(properties.getLong("time")))
                                .getTime());
                        MarkerOptions markerOptions = new MarkerOptions();

                        //markerOptions.icon(iconColors[Constants.randomInt(iconColors.length, 0)]);
                        markerOptions.title(earthQuake.getPlace());
                        markerOptions.position(new LatLng(earthQuake.getLat(), earthQuake.getLon()));
                        markerOptions.snippet("Magnitude: "
                                + earthQuake.getMagnitude() +
                                "\n" + "Date: " + formattedDate);

                        Marker marker = mMap.addMarker(markerOptions);
                        marker.setTag(earthQuake.getDetailLink());

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 1));

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(jsonObjectRequest);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        getQuakeDetails(marker.getTag().toString());
    }

    private void getQuakeDetails(String url) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String detailsUrl = "";

                try {
                    JSONObject properties = response.getJSONObject("properties");
                    JSONObject products = properties.getJSONObject("products");
                    JSONArray geoserve = products.getJSONArray("geoserve");
                    //https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/ci38608250.geojson

                    for (int i = 0; i < geoserve.length(); i++) {
                        JSONObject geoserveObj = geoserve.getJSONObject(i);

                        JSONObject contentObj = geoserveObj.getJSONObject("contents");
                        JSONObject geoJsonObj = contentObj.getJSONObject("geoserve.json");

                        detailsUrl = geoJsonObj.getString("url");


                    }
                    Log.d("URL: ", detailsUrl);

                    getMoreDetails(detailsUrl);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);
    }
    private void getMoreDetails(String url) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                View view = getLayoutInflater().inflate(R.layout.popup, null);

                Button dismissButton = (Button) view.findViewById(R.id.dismissPop);
                Button dismissButtonTop = (Button) view.findViewById(R.id.desmissPopTop);
                TextView popList = (TextView) view.findViewById(R.id.popList);
                WebView htmlPop = (WebView) view.findViewById(R.id.htmlWebview);

                StringBuilder stringBuilder = new StringBuilder();


                try {

                    if (response.has("tectonicSummary") && response.getString("tectonicSummary") != null) {

                        JSONObject tectonic = response.getJSONObject("tectonicSummary");

                        if (tectonic.has("text") && tectonic.getString("text") != null) {

                            String text = tectonic.getString("text");

                            htmlPop.loadDataWithBaseURL(null, text, "text/html", "UTF-8", null);

                            Log.d("HTML", text);

                        }

                    }


                    JSONArray cities = response.getJSONArray("cities");

                    for (int i = 0; i < cities.length(); i++) {
                        JSONObject citiesObj = cities.getJSONObject(i);

                        stringBuilder.append("City: " + citiesObj.getString("name")
                                + "\n" + "Distance: " + citiesObj.getString("distance")
                                + "\n" + "Population: "
                                + citiesObj.getString("population"));

                        stringBuilder.append("\n\n");

                    }

                    popList.setText(stringBuilder);

                    dismissButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dismissButtonTop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialogBuilder.setView(view);
                    dialog = dialogBuilder.create();
                    dialog.show();




                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(jsonObjectRequest);

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}