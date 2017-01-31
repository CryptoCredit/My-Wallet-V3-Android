package piuk.blockchain.android.ui.directory;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import info.blockchain.wallet.util.WebUtil;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class SuggestMerchantActivity extends BaseAuthActivity implements OnMapReadyCallback {

    private static final String SUGGEST_MERCHANT_URL = "https://merchant-directory.blockchain.info/api/suggest_merchant.php";
    TextView commandSave;
    DecimalFormat df = new DecimalFormat("#0.00");
    ReverseGeocodingTask reverseGeocodingTask;
    UpdateLastLocationThread updateLastLocationThread;
    private LocationManager locationManager = null;
    private LocationListener locationListener = null;
    private Location currLocation = null;
    private boolean gps_enabled = false;
    private ProgressBar progress = null;
    private FrameLayout mapView = null;
    private LinearLayout mapContainer = null;
    private LinearLayout confirmLayout = null;
    private GoogleMap map = null;
    private EditText edName = null;
    private EditText edDescription = null;
    private EditText edStreetAddress = null;
    private EditText edCity = null;
    private EditText edPostal = null;
    private EditText edTelephone = null;
    private EditText edWeb = null;
    private double selectedY;
    private double selectedX;
    private double strULat = 0.0;
    private double strULon = 0.0;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_suggest_merchant);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setSupportActionBar(toolbar);

        setTitle(R.string.suggest_merchant);

        edName = (EditText) findViewById(R.id.merchant_name);
        edDescription = (EditText) findViewById(R.id.description);
        edStreetAddress = (EditText) findViewById(R.id.street_address);
        edCity = (EditText) findViewById(R.id.city);
        edPostal = (EditText) findViewById(R.id.zip);
        edTelephone = (EditText) findViewById(R.id.telephone);
        edWeb = (EditText) findViewById(R.id.web);

        mapContainer = (LinearLayout) findViewById(R.id.map_container);
        mapView = (FrameLayout) findViewById(R.id.map_layout);
        confirmLayout = (LinearLayout) findViewById(R.id.confirm_layout);

        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setVisibility(View.GONE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();

        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

        map.getUiSettings().setZoomControlsEnabled(true);

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            gps_enabled = false;
        }

        if (gps_enabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

        map.setOnMapClickListener(point -> {
            map.clear();
            MarkerOptions marker = new MarkerOptions().position(new LatLng(point.latitude, point.longitude));
            map.addMarker(marker);

            populateAddressViews(point);
        });

        final List<String> categories = new ArrayList<>();
        categories.add(getString(R.string.merchant_cat_hint));
        categories.add(getString(R.string.merchant_cat1));
        categories.add(getString(R.string.merchant_cat2));
        categories.add(getString(R.string.merchant_cat3));
        categories.add(getString(R.string.merchant_cat4));
        categories.add(getString(R.string.merchant_cat5));

        final Spinner spCategory = (Spinner) findViewById(R.id.merchant_category_spinner);
        ArrayAdapter<String> categorySpinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, categories);
        categorySpinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spCategory.setAdapter(categorySpinnerArrayAdapter);

        commandSave = (TextView) findViewById(R.id.command_save);
        commandSave.setOnClickListener(v -> {

            if (commandSave.getText().toString().equals(getResources().getString(R.string.save))) {
                confirmLayout.setVisibility(View.GONE);

                final StringBuilder args = new StringBuilder();
                args.append("{");
                args.append("\"NAME\":\"").append(edName.getText());
                args.append("\",\"DESCRIPTION\":\"").append(edDescription.getText());
                args.append("\",\"STREET_ADDRESS\":\"").append(edStreetAddress.getText());
                args.append("\",\"CITY\":\"").append(edCity.getText());
                args.append("\",\"ZIP\":\"").append(edPostal.getText());
                args.append("\",\"TELEPHONE\":\"").append(edTelephone.getText());
                args.append("\",\"WEB\":\"").append(edWeb.getText());
                args.append("\",\"LATITUDE\":").append(df.format(selectedY));
                args.append(",\"LONGITUDE\":").append(df.format(selectedX));
                args.append(",\"CATEGORY\":").append(Integer.toString(spCategory.getSelectedItemPosition()));
                args.append(",\"SOURCE\":\"Android\"");
                args.append("}");

                new Thread(() -> {

                    Looper.prepare();

                    String res = null;
                    try {
                        res = WebUtil.getInstance().postURLJson(SUGGEST_MERCHANT_URL, args.toString());
                    } catch (Exception e) {
                        onShowToast(e.getMessage(), ToastCustom.TYPE_ERROR);
                    }

                    if (res != null && res.contains("\"result\":1")) {
                        onShowToast(R.string.ok_writing_merchant, ToastCustom.TYPE_OK);
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        onShowToast(R.string.error_writing_merchant, ToastCustom.TYPE_ERROR);
                    }

                    Looper.loop();

                }).start();

                finish();
            } else {
                confirmLayout.setVisibility(View.VISIBLE);
                commandSave.setVisibility(View.VISIBLE);
                commandSave.setText(getResources().getString(R.string.save));
                mapView.setVisibility(View.GONE);
                edDescription.requestFocus();
            }
        });

        mapView.setVisibility(View.GONE);
        confirmLayout.setVisibility(View.GONE);
        commandSave.setVisibility(View.GONE);

        currLocation = new Location(LocationManager.NETWORK_PROVIDER);
        Location lastKnownByGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastKnownByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownByGps == null && lastKnownByNetwork == null) {
            currLocation.setLatitude(0.0);
            currLocation.setLongitude(0.0);
        } else if (lastKnownByGps != null && lastKnownByNetwork == null) {
            currLocation = lastKnownByGps;
        } else if (lastKnownByGps == null) {
            currLocation = lastKnownByNetwork;
        } else {
            currLocation = (lastKnownByGps.getAccuracy() <= lastKnownByNetwork.getAccuracy()) ? lastKnownByGps : lastKnownByNetwork;
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            strULat = extras.getDouble("ULAT");
            strULon = extras.getDouble("ULON");

            currLocation.setLongitude(strULon);
            currLocation.setLatitude(strULat);
        }
    }

    public void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        onShowToast(getString(message), toastType);
    }

    public void onShowToast(String message, @ToastCustom.ToastType String toastType) {
        runOnUiThread(() -> ToastCustom.makeText(this, message, ToastCustom.LENGTH_SHORT, toastType));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }

    public void manualClicked(View view) {

        if (reverseGeocodingTask != null) reverseGeocodingTask.cancel(true);

        edDescription.requestFocus();
        edDescription.setText("");
        edStreetAddress.setText("");
        edPostal.setText("");
        edCity.setText("");
        edTelephone.setText("");
        edWeb.setText("");

        confirmLayout.setVisibility(View.VISIBLE);
        commandSave.setVisibility(View.VISIBLE);
        commandSave.setText(getResources().getString(R.string.save));
        mapView.setVisibility(View.GONE);
    }

    public void autoClicked(View view) {

        if (reverseGeocodingTask != null) reverseGeocodingTask.cancel(true);

        mapView.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        confirmLayout.setVisibility(View.GONE);
        mapContainer.setVisibility(View.GONE);

        if (currLocation != null) {

            zoomToLocation(currLocation.getLongitude(), currLocation.getLatitude());
            commandSave.setVisibility(View.VISIBLE);
            commandSave.setText(getResources().getString(R.string.next));
        } else {
            if (gps_enabled) {

                if (updateLastLocationThread != null) updateLastLocationThread.cancel(true);
                updateLastLocationThread = new UpdateLastLocationThread(this);
                updateLastLocationThread.execute();
            } else {

//				try {
//					gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//				} catch(Exception e) {
//					gps_enabled = false;
//				}
//
//				if(gps_enabled) {
//					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//				}

            }
        }
    }

    private void zoomToLocation(double x, double y) {
        if (map != null) {
            mapView.setVisibility(View.VISIBLE);
            mapContainer.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            map.clear();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(y, x), 13));
            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(y, x)).zoom(17).build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            map.addMarker(new MarkerOptions().position(new LatLng(y, x)));

            populateAddressViews(new LatLng(y, x));
        }
    }

    private void populateAddressViews(LatLng latLng) {

        selectedY = latLng.latitude;
        selectedX = latLng.longitude;

        try {
            reverseGeocodingTask = new ReverseGeocodingTask(SuggestMerchantActivity.this);
            reverseGeocodingTask.execute(latLng);

        } catch (Exception e) {
            Log.e("", "", e);
            onShowToast(R.string.address_lookup_fail, ToastCustom.TYPE_ERROR);
        }
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                currLocation = location;
            }

            if (progress != null && progress.isShown()) {
                progress.setVisibility(View.GONE);
            }

        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    class UpdateLastLocationThread extends AsyncTask<Void, Void, Void> {

        public HashMap<String, String> result = new HashMap<>();

        Context mContext;

        public UpdateLastLocationThread(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {

            currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (currLocation != null) {

                runOnUiThread(() -> {
                    zoomToLocation(currLocation.getLongitude(), currLocation.getLatitude());
                    commandSave.setVisibility(View.VISIBLE);
                    commandSave.setText(getResources().getString(R.string.next));
                });
            }

            return null;
        }
    }

    class ReverseGeocodingTask extends AsyncTask<LatLng, Void, Void> {

        Context mContext;

        public ReverseGeocodingTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(LatLng... params) {

            runOnUiThread(() -> progress.setVisibility(View.VISIBLE));

            Geocoder gc = new Geocoder(mContext, Locale.getDefault());

            try {
                List<Address> addrList = gc.getFromLocation(params[0].latitude, params[0].longitude, 1);

                if (addrList.size() > 0) {
                    final Address address = addrList.get(0);

                    runOnUiThread(() -> {
                        if (address.getMaxAddressLineIndex() > 0 && address.getAddressLine(0) != null) {
                            edStreetAddress.setText(address.getAddressLine(0));
                            onShowToast(address.getAddressLine(0), ToastCustom.TYPE_GENERAL);
                        }
                        if (address.getPostalCode() != null)
                            edPostal.setText(address.getPostalCode());
                        if (address.getLocality() != null)
                            edCity.setText(address.getLocality());

                        progress.setVisibility(View.GONE);
                    });
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            return null;
        }
    }
}