var BeaconPlugin = {
    startMonitoring: function(uuid, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "BeaconPlugin", "startMonitoring", [uuid || ""]);
    },
    stopMonitoring: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "BeaconPlugin", "stopMonitoring", []);
    }
};

module.exports = BeaconPlugin;
