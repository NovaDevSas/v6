package com.entel.beaconplugin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import com.altbeacon.beacon.Beacon;
import com.altbeacon.beacon.BeaconManager;
import com.altbeacon.beacon.BeaconParser;
import com.altbeacon.beacon.Region;
import com.altbeacon.beacon.RangeNotifier;
import com.altbeacon.beacon.MonitorNotifier;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BeaconPlugin extends CordovaPlugin {

    private BeaconManager beaconManager;
    private CallbackContext callbackContext;
    private static final String BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private String pendingAction;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) {
        this.callbackContext = callback;
        
        if ("startMonitoring".equals(action)) {
            pendingAction = "startMonitoring";
            requestPermissions();
            return true;
        } else if ("stopMonitoring".equals(action)) {
            stopMonitoring();
            callback.success("Monitoring stopped");
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        
        boolean hasAllPermissions = true;
        for (String permission : permissions) {
            if (!cordova.hasPermission(permission)) {
                hasAllPermissions = false;
                break;
            }
        }
        
        if (hasAllPermissions) {
            executeAction();
        } else {
            cordova.requestPermissions(this, PERMISSION_REQUEST_CODE, 
                permissions.toArray(new String[0]));
        }
    }
    
    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                executeAction();
            } else {
                callbackContext.error("Required permissions not granted");
            }
        }
    }
    
    private void executeAction() {
        if ("startMonitoring".equals(pendingAction)) {
            startMonitoring();
        }
    }

    private void startMonitoring() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    beaconManager = BeaconManager.getInstanceForApplication(cordova.getActivity().getApplicationContext());
                    beaconManager.getBeaconParsers().clear();
                    beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_LAYOUT));
                    
                    // Add monitor notifier for region entry/exit events
                    beaconManager.addMonitorNotifier(new MonitorNotifier() {
                        @Override
                        public void didEnterRegion(Region region) {
                            try {
                                JSONObject regionData = new JSONObject();
                                regionData.put("event", "enter");
                                regionData.put("identifier", region.getUniqueId());
                                
                                PluginResult result = new PluginResult(PluginResult.Status.OK, regionData);
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                            } catch (Exception e) {
                                // Handle exception
                            }
                        }

                        @Override
                        public void didExitRegion(Region region) {
                            try {
                                JSONObject regionData = new JSONObject();
                                regionData.put("event", "exit");
                                regionData.put("identifier", region.getUniqueId());
                                
                                PluginResult result = new PluginResult(PluginResult.Status.OK, regionData);
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                            } catch (Exception e) {
                                // Handle exception
                            }
                        }

                        @Override
                        public void didDetermineStateForRegion(int state, Region region) {
                            // Optional: handle state changes
                        }
                    });
                    
                    // Fix the RangeNotifier implementation
                    beaconManager.addRangeNotifier(new RangeNotifier() {
                        @Override
                        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                            if (beacons != null && !beacons.isEmpty()) {
                                // Se toma el primer beacon detectado
                                Beacon beacon = beacons.iterator().next();
                                try {
                                    JSONObject beaconData = new JSONObject();
                                    beaconData.put("uuid", beacon.getId1().toString());
                                    beaconData.put("major", beacon.getId2().toInt());
                                    beaconData.put("minor", beacon.getId3().toInt());
                                    beaconData.put("rssi", beacon.getRssi());
                                    beaconData.put("proximity", getProximityString(beacon.getDistance()));
                                    beaconData.put("accuracy", beacon.getDistance());
                                    // Se envían los datos al JavaScript
                                    PluginResult result = new PluginResult(PluginResult.Status.OK, beaconData);
                                    result.setKeepCallback(true);
                                    callbackContext.sendPluginResult(result);
                                } catch (Exception e) {
                                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error: " + e.getMessage());
                                    result.setKeepCallback(true);
                                    callbackContext.sendPluginResult(result);
                                }
                            }
                        }
                    });

                    Region region = new Region("all-beacons", null, null, null);
                    beaconManager.startRangingBeacons(region);
                    beaconManager.startMonitoring(region);
                } catch (Exception e) {
                    callbackContext.error("Error starting ranging: " + e.getMessage());
                }
            }
        });
    }

    // Helper method to convert distance to proximity string (like iOS)
    private String getProximityString(double distance) {
        if (distance < 0.5) return "immediate";
        else if (distance < 3.0) return "near";
        else return "far";
    }

    private void stopMonitoring() {
        if (beaconManager != null) {
            try {
                Region region = new Region("all-beacons", null, null, null);
                beaconManager.stopRangingBeacons(region);
                beaconManager.stopMonitoring(region);
            } catch (Exception e) {
                // Handle any exceptions
            }
        }
    }
}
