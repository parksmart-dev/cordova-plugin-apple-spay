/********* AppleSpayPlugin.m Cordova Plugin Implementation *******/
@import Stripe;
#import <PassKit/PassKit.h>
#import <Cordova/CDV.h>

@interface AppleSpayPlugin : CDVPlugin <STPApplePayContextDelegate> {
  PKMerchantCapability merchantCapabilities;
  NSArray<NSString *>* supportedPaymentNetworks;
}

- (void)makePaymentRequest:(CDVInvokedUrlCommand*)command;
- (void)canMakePayments:(CDVInvokedUrlCommand*)command;
- (void)completeLastTransaction:(CDVInvokedUrlCommand*)command;
@end

@implementation AppleSpayPlugin

// - (void)coolMethod:(CDVInvokedUrlCommand*)command
// {
//     CDVPluginResult* pluginResult = nil;
//     NSString* echo = [command.arguments objectAtIndex:0];

//     if (echo != nil && [echo length] > 0) {
//         pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:echo];
//     } else {
//         pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
//     }

//     [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
// }

- (void)pluginInitialize
{
    // Set these to the payment cards accepted.
    // They will nearly always be the same.
    supportedPaymentNetworks = @[PKPaymentNetworkVisa, PKPaymentNetworkMasterCard, PKPaymentNetworkAmex];

    // Set the capabilities that your merchant supports
    // Adyen for example, only supports the 3DS one.
    merchantCapabilities = PKMerchantCapability3DS;// PKMerchantCapabilityEMV;

    NSString * stripePublishableKey = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"StripeTestPublishableKey"];
    NSLog(@"Stripe stripePublishableKey == %@", stripePublishableKey);
    NSString * appleMerchantIdentifier = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"AppleMerchantIdentifier"];
    NSLog(@"ApplePay appleMerchantIdentifier == %@", appleMerchantIdentifier);
    [[STPPaymentConfiguration sharedConfiguration] setPublishableKey:stripePublishableKey];
    [[STPPaymentConfiguration sharedConfiguration] setAppleMerchantIdentifier:appleMerchantIdentifier];
}

- (void)canMakePayments:(CDVInvokedUrlCommand*)command
{
    if([StripeAPI deviceSupportsApplePay]) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: @"This device can make payments and has a supported card"];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
                return;
    } else {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"This device cannot make payments."];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            return;
    }
}

@end
