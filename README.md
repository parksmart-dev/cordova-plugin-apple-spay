# Cordova Apple Pay and Google Pay integration

This plugin is built as unified method for obtaining payment from Apple/Google pay to Stripe

Plugin supports iOS 11+, android 10+.

## Installation

```
cordova plugin add cordova-plugin-apple-spay --variable STRIPE_LIVE_PUBLISHABLE_KEY=your_key --variable STRIPE_TEST_PUBLISHABLE_KEY=your_key --variable APPLE_MERCHANT_IDENTIFIER=your_merchant_id
```

For Android, register and fill all required forms at https://pay.google.com/business/console. Add following to
config.xml:

```
<config-file parent="/manifest/application" target="AndroidManifest.xml">
    <meta-data
            android:name="com.google.android.gms.wallet.api.enabled"
            android:value="true" />
</config-file>
```

For iOS, you have to have valid developer account with merchant set up and ApplePay capability and a merchant id
configured in your Xcode project. Merchant id can be obtained
from https://developer.apple.com/account/resources/identifiers/list/merchant. Do configuration manually or using
config.xml:

```
<platform name="ios">

  <config-file target="*-Debug.plist" parent="com.apple.developer.in-app-payments">
    <array>
      <string>developer merchant ID here</string>
    </array>
  </config-file>

  <config-file target="*-Release.plist" parent="com.apple.developer.in-app-payments">
    <array>
      <string>production merchant ID here</string>
    </array>
  </config-file>
</platform>
```

## Usage

`window.AppleSpayPlugin.makePaymentRequest()` create payment request

```
request example:
{
        items: [
          {
            label: "UTEC USA LLC",
            amount: 33,
          },
        ],
        amount: 33,
        countryCode: "UA",
        currencyCode: "USD",
        billingAddressRequirement: "none",
        shippingAddressRequirement: "none",
        clientSecret: this.paymentIntentApple.clientSecret,
}
All parameters in request object are required.
```

`window.AppleSpayPlugin.canMakePayments()` return boolean true/false
