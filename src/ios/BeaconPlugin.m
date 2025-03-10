#import <CoreLocation/CoreLocation.h>
#import <Cordova/CDV.h>

@interface BeaconPlugin : CDVPlugin <CLLocationManagerDelegate>
@property (nonatomic, strong) CLLocationManager *locationManager;
@property (nonatomic, strong) CLBeaconRegion *beaconRegion;
@property (nonatomic, strong) CDVInvokedUrlCommand *callbackCommand;
@end

@implementation BeaconPlugin

- (void)startMonitoring:(CDVInvokedUrlCommand *)command {
    self.callbackCommand = command;
    
    // Inicializa el Location Manager y solicita permisos
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;
    
    // Request appropriate authorization based on iOS version
    if ([self.locationManager respondsToSelector:@selector(requestAlwaysAuthorization)]) {
        [self.locationManager requestAlwaysAuthorization];
    }
    
    // Define la región de beacons (utilizando el UUID de ejemplo)
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:@"8C3889DC-3338-FF37-0E19-28BB83B37217"];
    self.beaconRegion = [[CLBeaconRegion alloc] initWithProximityUUID:uuid identifier:@"all-beacons"];
    self.beaconRegion.notifyEntryStateOnDisplay = YES;
    
    // Start monitoring after a short delay to ensure permissions are processed
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 1 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
        [self.locationManager startRangingBeaconsInRegion:self.beaconRegion];
    });
}

- (void)stopMonitoring:(CDVInvokedUrlCommand *)command {
    if (self.beaconRegion) {
        [self.locationManager stopRangingBeaconsInRegion:self.beaconRegion];
    }
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Monitoring stopped"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

// Handle authorization status changes
- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status {
    if (status == kCLAuthorizationStatusAuthorizedAlways || 
        status == kCLAuthorizationStatusAuthorizedWhenInUse) {
        // Start ranging if authorized
        [self.locationManager startRangingBeaconsInRegion:self.beaconRegion];
    } else if (status == kCLAuthorizationStatusDenied || 
               status == kCLAuthorizationStatusRestricted) {
        // Send error if permissions denied
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                    messageAsString:@"Location permission denied"];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackCommand.callbackId];
    }
}

// Método del delegate que se ejecuta al detectar beacons
- (void)locationManager:(CLLocationManager *)manager didRangeBeacons:(NSArray *)beacons inRegion:(CLBeaconRegion *)region {
    if (beacons.count > 0) {
        // Se toma el primer beacon detectado
        CLBeacon *beacon = [beacons firstObject];
        NSDictionary *beaconData = @{
            @"uuid": beacon.proximityUUID.UUIDString,
            @"major": beacon.major,
            @"minor": beacon.minor
        };
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:beaconData];
        // Mantiene el callback activo para múltiples actualizaciones
        result.keepCallback = @(YES);
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackCommand.callbackId];
    }
}

@end
