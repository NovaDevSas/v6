#import <CoreLocation/CoreLocation.h>
#import <Cordova/CDV.h>

@interface BeaconPlugin : CDVPlugin <CLLocationManagerDelegate>
@property (nonatomic, strong) CLLocationManager *locationManager;
@property (nonatomic, strong) CDVInvokedUrlCommand *callbackCommand;
@end

@implementation BeaconPlugin

- (void)startMonitoring:(CDVInvokedUrlCommand *)command {
    self.callbackCommand = command;
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;
    [self.locationManager requestAlwaysAuthorization];
    CLBeaconRegion *region = [[CLBeaconRegion alloc] initWithProximityUUID:[[NSUUID alloc] initWithUUIDString:@"8C3889DC-3338-FF37-0E19-28BB83B37217"] identifier:@"all-beacons"];
    [self.locationManager startRangingBeaconsInRegion:region];
    NSLog(@"Started monitoring beacons");
}

- (void)locationManager:(CLLocationManager *)manager didRangeBeacons:(NSArray<CLBeacon *> *)beacons inRegion:(CLBeaconRegion *)region {
    if (beacons.count > 0) {
        CLBeacon *beacon = beacons.firstObject;
        NSDictionary *result = @{
            @"major": @(beacon.major.integerValue),
            @"minor": @(beacon.minor.integerValue)
        };
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result] callbackId:self.callbackCommand.callbackId];
        NSLog(@"Ranged beacon: %@", result);
    }
}

- (void)isAvailable:(CDVInvokedUrlCommand *)command {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end