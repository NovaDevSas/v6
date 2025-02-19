package com.entel.beaconplugin;

import com.altbeacon.beacon.Beacon;
import com.altbeacon.beacon.BeaconConsumer;
import com.altbeacon.beacon.BeaconManager;
import com.altbeacon.beacon.BeaconParser;
import com.altbeacon.beacon.Region;
import com.altbeacon.beacon.RangeNotifier;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;

public class BeaconPlugin extends CordovaPlugin implements BeaconConsumer {

    private BeaconManager beaconManager;
    private CallbackContext callbackContext;
    private static final String BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) {
        this.callbackContext = callback;
        if ("startMonitoring".equals(action)) {
            startMonitoring();
            return true;
        } else if ("stopMonitoring".equals(action)) {
            stopMonitoring();
            callback.success("Monitoring stopped");
            return true;
        }
        return false;
    }

    private void startMonitoring() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                beaconManager = BeaconManager.getInstanceForApplication(cordova.getActivity());
                beaconManager.getBeaconParsers().clear();
                beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_LAYOUT));
                beaconManager.bind(BeaconPlugin.this);
            }
        });
    }

    private void stopMonitoring() {
        if (beaconManager != null) {
            beaconManager.unbind(this);
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (!beacons.isEmpty()) {
                    // Se toma el primer beacon detectado
                    Beacon beacon = beacons.iterator().next();
                    try {
                        JSONObject beaconData = new JSONObject();
                        beaconData.put("uuid", beacon.getId1().toString());
                        beaconData.put("major", beacon.getId2().toInt());
                        beaconData.put("minor", beacon.getId3().toInt());
                        // Se envían los datos al JavaScript
                        callbackContext.success(beaconData);
                    } catch (Exception e) {
                        callbackContext.error("Error: " + e.getMessage());
                    }
                }
            }
        });

        try {
            Region region = new Region("all-beacons", null, null, null);
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (Exception e) {
            callbackContext.error("Error starting ranging: " + e.getMessage());
        }
    }
}
