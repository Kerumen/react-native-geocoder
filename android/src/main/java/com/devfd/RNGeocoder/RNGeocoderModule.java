package com.devfd.RNGeocoder;

import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.ArrayList;
import java.util.List;

public class RNGeocoderModule extends ReactContextBaseJavaModule {

    private AddressResultReceiver resultReceiver;

    private Promise currentPromise;

    public RNGeocoderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        resultReceiver = new AddressResultReceiver(new Handler(Looper.getMainLooper()));
    }

    @Override
    public String getName() {
        return "RNGeocoder";
    }

    @ReactMethod
    public void geocodeAddress(String addressName, Promise promise) {
        currentPromise = promise;
        startIntentService(addressName);
    }

    @ReactMethod
    public void geocodePosition(ReadableMap position, Promise promise) {
        currentPromise = promise;
        startIntentService(position);
    }

    private void startIntentService(ReadableMap position) {
        Location location = new Location("");
        location.setLatitude(position.getDouble("lat"));
        location.setLongitude(position.getDouble("lng"));
        Intent intent = new Intent(getCurrentActivity(), GeocoderIntentService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        getReactApplicationContext().startService(intent);
    }

    private void startIntentService(String address) {
        Intent intent = new Intent(getCurrentActivity(), GeocoderIntentService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.ADDRESS_DATA_EXTRA, address);
        getReactApplicationContext().startService(intent);
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (currentPromise != null) {
                if (resultCode == Constants.SUCCESS_RESULT) {
                    ArrayList<Address> addresses = resultData.getParcelableArrayList(Constants.RESULT_DATA_KEY);
                    currentPromise.resolve(transform(addresses));
                    currentPromise = null;
                } else if (resultCode == Constants.FAILURE_RESULT) {
                    String error = resultData.getString(Constants.ERROR_DATA_KEY);
                    currentPromise.reject("GEOCODER_ERROR", error);
                    currentPromise = null;
                }
            }
        }
    }

    private WritableArray transform(List<Address> addresses) {
        WritableArray results = new WritableNativeArray();

        for (Address address: addresses) {
            WritableMap result = new WritableNativeMap();

            WritableMap position = new WritableNativeMap();
            position.putDouble("lat", address.getLatitude());
            position.putDouble("lng", address.getLongitude());
            result.putMap("position", position);

            final String feature_name = address.getFeatureName();
            if (feature_name != null && !feature_name.equals(address.getSubThoroughfare()) &&
                    !feature_name.equals(address.getThoroughfare()) &&
                    !feature_name.equals(address.getLocality())) {

                result.putString("feature", feature_name);
            }
            else {
                result.putString("feature", null);
            }

            result.putString("locality", address.getLocality());
            result.putString("adminArea", address.getAdminArea());
            result.putString("country", address.getCountryName());
            result.putString("countryCode", address.getCountryCode());
            result.putString("locale", address.getLocale().toString());
            result.putString("postalCode", address.getPostalCode());
            result.putString("subAdminArea", address.getSubAdminArea());
            result.putString("subLocality", address.getSubLocality());
            result.putString("streetNumber", address.getSubThoroughfare());
            result.putString("streetName", address.getThoroughfare());

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(address.getAddressLine(i));
            }

            result.putString("formattedAddress", sb.toString());

            results.pushMap(result);
        }

        return results;
    }
}
