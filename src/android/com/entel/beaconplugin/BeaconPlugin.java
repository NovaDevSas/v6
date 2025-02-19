@Override
public boolean execute(String action, JSONArray args, CallbackContext callback) {
    if ("startMonitoring".equals(action)) {
        this.callbackContext = callback;
        startMonitoring();
        return true;
    } else if ("isAvailable".equals(action)) {
        callback.success("true");
        return true;
    }
    return false;
}

private void startMonitoring() {
    beaconManager = BeaconManager.getInstanceForApplication(cordova.getActivity());
    beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
    beaconManager.bind(this);
}

@Override
public void onBeaconServiceConnect() {
    try {
        beaconManager.addRangeNotifier((beacons, region) -> {
            if (!beacons.isEmpty()) {
                Beacon beacon = beacons.iterator().next();
                JSONObject result = new JSONObject();
                result.put("major", beacon.getId2().toInt());
                result.put("minor", beacon.getId3().toInt());
                callbackContext.success(result);
            }
        });
        beaconManager.startRangingBeaconsInRegion(new Region("all-beacons", null, null, null));
    } catch (Exception e) {
        callbackContext.error(e.getMessage());
    }
}
