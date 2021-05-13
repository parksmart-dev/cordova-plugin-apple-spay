/********* AppleSpayPlugin.m Cordova Plugin Implementation *******/
#import <UIKit/UIKit.h>
#import <PassKit/PassKit.h>
#import <Cordova/CDV.h>
@import Stripe;

@interface AppleSpayPlugin : CDVPlugin<STPApplePayContextDelegate>
@property (nonatomic, retain) NSString *clientSecret;
@property (nonatomic, retain) NSString *appleMerchantIdentifier;
@property (nonatomic, retain) NSString *paymentCallbackId;
- (void)makePaymentRequest:(CDVInvokedUrlCommand*)command;
- (void)canMakePayments:(CDVInvokedUrlCommand*)command;
@end

@implementation AppleSpayPlugin
@synthesize clientSecret;
@synthesize appleMerchantIdentifier;
@synthesize paymentCallbackId;
- (void)pluginInitialize
{
    NSString * stripePublishableKey = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"StripeLivePublishableKey"];
    self.appleMerchantIdentifier = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"AppleMerchantIdentifier"];
    [StripeAPI setDefaultPublishableKey:stripePublishableKey];
    [[STPPaymentConfiguration sharedConfiguration] setAppleMerchantIdentifier:self.appleMerchantIdentifier];
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

- (NSString *)countryCodeFromArguments:(NSArray *)arguments
{
    NSString *countryCode = [[arguments objectAtIndex:0] objectForKey:@"countryCode"];
    return countryCode;
}

- (NSString *)currencyCodeFromArguments:(NSArray *)arguments
{
    NSString *currencyCode = [[arguments objectAtIndex:0] objectForKey:@"currencyCode"];
    return currencyCode;
}

- (NSString *)clientSecretFromArguments:(NSArray *)arguments
{
    NSString *clientSecret = [[arguments objectAtIndex:0] objectForKey:@"clientSecret"];
    return clientSecret;
}

- (NSArray *)itemsFromArguments:(NSArray *)arguments
{
    NSArray *itemDescriptions = [[arguments objectAtIndex:0] objectForKey:@"items"];

    NSMutableArray *items = [[NSMutableArray alloc] init];

    for (NSDictionary *item in itemDescriptions) {

        NSString *label = [item objectForKey:@"label"];

        NSDecimalNumber *amount = [NSDecimalNumber decimalNumberWithDecimal:[[item objectForKey:@"amount"] decimalValue]];

        PKPaymentSummaryItem *newItem = [PKPaymentSummaryItem summaryItemWithLabel:label amount:amount];

        [items addObject:newItem];
    }

    return items;
}

- (void)makePaymentRequest:(CDVInvokedUrlCommand*)command
{
    self.paymentCallbackId = command.callbackId;
    NSString * countryCode = [self countryCodeFromArguments:command.arguments];
    NSString * currencyCode = [self currencyCodeFromArguments:command.arguments];
    NSArray * paymentSummaryItems = [self itemsFromArguments:command.arguments];
    self.clientSecret = [self clientSecretFromArguments:command.arguments];
    PKPaymentRequest *paymentRequest = [StripeAPI paymentRequestWithMerchantIdentifier:self.appleMerchantIdentifier country:countryCode currency:currencyCode];
    
    paymentRequest.paymentSummaryItems = paymentSummaryItems;
    
    STPApplePayContext *applePayContext = [[STPApplePayContext alloc] initWithPaymentRequest:paymentRequest delegate:self];
    if (applePayContext) {
        // Present Apple Pay payment sheet
        [applePayContext presentApplePayOnViewController:self.viewController completion:nil];
    } else {
        CDVPluginResult* result;
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"ApplePay context error."];
        [self.commandDelegate sendPluginResult:result callbackId:self.paymentCallbackId];
        NSLog(@"ApplePay context error == %@", applePayContext);
    }
}

- (void)applePayContext:(STPApplePayContext *)context didCompleteWithStatus:(STPPaymentStatus)status error:(NSError *)error {
    CDVPluginResult* result;
    switch (status) {
        case STPPaymentStatusSuccess:
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: @"ApplePay context success."];
            [self.commandDelegate sendPluginResult:result callbackId:self.paymentCallbackId];
            break;

        case STPPaymentStatusError:
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"ApplePay context error."];
            [self.commandDelegate sendPluginResult:result callbackId:self.paymentCallbackId];
            break;

        case STPPaymentStatusUserCancellation:
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"ApplePay context cancell."];
            [self.commandDelegate sendPluginResult:result callbackId:self.paymentCallbackId];
            break;
    }
}

- (void)applePayContext:(STPApplePayContext * _Nonnull)context didCreatePaymentMethod:(STPPaymentMethod * _Nonnull)paymentMethod paymentInformation:(PKPayment * _Nonnull)paymentInformation completion:(void (^ _Nonnull)(NSString * _Nullable, NSError * _Nullable))completion {
    completion(clientSecret, nil);
}


@end
