var BeaconPlugin = {
    startMonitoring: function (successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            "BeaconPlugin",
            "startMonitoring",
            []
        );
    }
};