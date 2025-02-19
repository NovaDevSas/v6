var BeaconPlugin = {
    startMonitoring: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "BeaconPlugin", "startMonitoring", []);
    },
    stopMonitoring: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "BeaconPlugin", "stopMonitoring", []);
    }
};

module.exports = BeaconPlugin;
