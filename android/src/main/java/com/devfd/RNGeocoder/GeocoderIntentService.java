package com.devfd.RNGeocoder;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GeocoderIntentService extends IntentService {

    protected ResultReceiver receiver;

    public static final String TAG = GeocoderIntentService.class.getName();

    public GeocoderIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Geocoder geocoder = new Geocoder(this);

        if (!Geocoder.isPresent()) {
            deliverErrorToReceiver(Constants.FAILURE_RESULT, "Geocoder not available for this platform");
            return;
        }

        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);
        String address = intent.getStringExtra(Constants.ADDRESS_DATA_EXTRA);
        receiver = intent.getParcelableExtra(Constants.RECEIVER);

        try {
            List<Address> addresses = null;
            if (location != null) {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 20);
            } else if (address != null) {
                addresses = geocoder.getFromLocationName(address, 20);
            }
            deliverResultToReceiver(Constants.SUCCESS_RESULT, addresses);
        } catch (IOException e) {
            deliverErrorToReceiver(Constants.FAILURE_RESULT, "Geocoder service not available");
        }
    }

    private void deliverResultToReceiver(int resultCode, List<Address> addresses) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.RESULT_DATA_KEY, new ArrayList<>(addresses));
        receiver.send(resultCode, bundle);
    }

    private void deliverErrorToReceiver(int resultCode, String error) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ERROR_DATA_KEY, error);
        receiver.send(resultCode, bundle);
    }
}
