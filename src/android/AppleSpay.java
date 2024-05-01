package com.awkoy.cordova.applespay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.ApiResultCallback;
import com.stripe.android.GooglePayConfig;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class AppleSpay extends CordovaPlugin {
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 53;
    private PaymentsClient paymentsClient;
    private Stripe stripe;
    private String initKey;
    private String connectAccountId;
    private CallbackContext callbackContext;
    private String clientSecret;

    @Override
    protected void pluginInitialize() {
        
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        switch (action) 
        {
            case "canMakePayments":
                canMakePayments(args, callbackContext);
                break;

            case "makePaymentRequest":
                makePaymentRequest(args, callbackContext);
                break;

            case "manualInit":
                manualInit(args, callbackContext);
                break;

            default:
                return false;
        }

        return true;
    }

    @NonNull
    private IsReadyToPayRequest createIsReadyToPayRequest() throws JSONException {
        final JSONArray allowedAuthMethods = new JSONArray();
        allowedAuthMethods.put("PAN_ONLY");
        allowedAuthMethods.put("CRYPTOGRAM_3DS");

        final JSONArray allowedCardNetworks = new JSONArray();
        allowedCardNetworks.put("AMEX");
        allowedCardNetworks.put("DISCOVER");
        allowedCardNetworks.put("MASTERCARD");
        allowedCardNetworks.put("VISA");

        final JSONObject cardParameters = new JSONObject();
        cardParameters.put("allowedAuthMethods", allowedAuthMethods);
        cardParameters.put("allowedCardNetworks", allowedCardNetworks);

        final JSONObject cardPaymentMethod = new JSONObject();
        cardPaymentMethod.put("type", "CARD");
        cardPaymentMethod.put("parameters", cardParameters);

        final JSONArray allowedPaymentMethods = new JSONArray();
        allowedPaymentMethods.put(cardPaymentMethod);

        final JSONObject isReadyToPayRequestJson = new JSONObject();
        isReadyToPayRequestJson.put("apiVersion", 2);
        isReadyToPayRequestJson.put("apiVersionMinor", 0);
        isReadyToPayRequestJson.put("allowedPaymentMethods", allowedPaymentMethods);

        return IsReadyToPayRequest.fromJson(isReadyToPayRequestJson.toString());
    }

    private void manualInit(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject argss = args.getJSONObject(0);
        Activity activity = cordova.getActivity();
        cordova.setActivityResultCallback(this);

        this.callbackContext = callbackContext;

        try {
            this.initKey = getParam(argss, "stripeInitKey");
            this.connectAccountId = getParam(argss, "connectAccountId");

            Log.d("onCreate plugin", this.initKey);

            Context context = activity.getApplicationContext();

            PaymentConfiguration.init(context, this.initKey);

            paymentsClient = Wallet.getPaymentsClient(
                    activity,
                    new Wallet.WalletOptions.Builder()
                            .setEnvironment(this.initKey == null || this.initKey.contains("test") ? WalletConstants.ENVIRONMENT_TEST : WalletConstants.ENVIRONMENT_PRODUCTION)
                            .build()
            );

            stripe = new Stripe(context, this.initKey, this.connectAccountId);

            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));

        } catch (JSONException e) {
            callbackContext.error(e.getMessage());
        }
    }

    private void makePaymentRequest(JSONArray args, CallbackContext callbackContext) throws JSONException 
    {
        JSONObject argss = args.getJSONObject(0);
        Activity activity = cordova.getActivity();
        cordova.setActivityResultCallback(this);

        this.callbackContext = callbackContext;

        try {
            String price = getParam(argss, "amount");
            String currencyCode = getParam(argss, "currencyCode");
            String countryCode = getParam(argss, "countryCode");
            this.clientSecret = getParam(argss, "clientSecret");

            final JSONObject tokenizationSpec = new GooglePayConfig(this.initKey, this.connectAccountId).getTokenizationSpecification();

            final JSONObject cardPaymentMethod = new JSONObject()
                    .put("type", "CARD")
                    .put(
                            "parameters",
                            new JSONObject()
                                    .put("allowedAuthMethods", new JSONArray()
                                            .put("PAN_ONLY")
                                            .put("CRYPTOGRAM_3DS"))
                                    .put("allowedCardNetworks",
                                            new JSONArray()
                                                    .put("AMEX")
                                                    .put("DISCOVER")
                                                    .put("MASTERCARD")
                                                    .put("VISA"))

                                    // require billing address
                                    .put("billingAddressRequired", false)
                    )
                    .put("tokenizationSpecification", tokenizationSpec);

            // create PaymentDataRequest
            final JSONObject paymentDataRequest = new JSONObject()
                    .put("apiVersion", 2)
                    .put("apiVersionMinor", 0)
                    .put("allowedPaymentMethods",
                            new JSONArray().put(cardPaymentMethod))
                    .put("transactionInfo", new JSONObject()
                            .put("totalPrice", price)
                            .put("totalPriceStatus", "FINAL")
                            .put("currencyCode", currencyCode)
                            .put("countryCode", countryCode)
                    )
                    .put("merchantInfo", new JSONObject()
                            .put("merchantName", "ParkSmart"))
                    .put("emailRequired", false);

            String requestJson = paymentDataRequest.toString();

            PaymentDataRequest request = PaymentDataRequest.fromJson(requestJson);

            // Since loadPaymentData may show the UI asking the user to select a payment method, we use
            // AutoResolveHelper to wait for the user interacting with it. Once completed,
            // onActivityResult will be called with the result.
            if (request != null) 
            {
                AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(request), activity, LOAD_PAYMENT_DATA_REQUEST_CODE);
            }

        } catch (JSONException e) {
            callbackContext.error(e.getMessage());
        }
    }

    private void canMakePayments(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final IsReadyToPayRequest request = createIsReadyToPayRequest();
        paymentsClient.isReadyToPay(request)
            .addOnCompleteListener(
                new OnCompleteListener<Boolean>() {
                    public void onComplete(Task<Boolean> task) {
                        boolean result = task.isSuccessful();
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                    }
                }
            );
    }

    private String getParam(JSONObject args, String name) throws JSONException {
        String param = args.getString(name);

        if (param == null || param.length() == 0) {
            throw new JSONException(String.format("%s is required", name));
        }

        return param;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        // value passed in AutoResolveHelper
        if (requestCode != LOAD_PAYMENT_DATA_REQUEST_CODE) {
            return;
        }

        switch (resultCode) 
        {
            case Activity.RESULT_OK: 
                onGooglePayResult(data);
                break;
            
            case Activity.RESULT_CANCELED:
                callbackContext.error("Payment cancelled");
                break;
            
            case AutoResolveHelper.RESULT_ERROR: 
                // Log the status for debugging
                // Generally there is no need to show an error to
                // the user as the Google Payment API will do that
                Status status = AutoResolveHelper.getStatusFromIntent(data);
                callbackContext.error(status.getStatusMessage());
                break;
            
            default: 
                // Do nothing.
                break;
        }
    }

    private void onGooglePayResult(@NonNull Intent data) 
    {
        PaymentData paymentData = PaymentData.getFromIntent(data);

        if (paymentData == null) 
        {
            callbackContext.error("Error with paymentData");
            return;
        }

        try 
        {
            JSONObject jsonObj = new JSONObject(paymentData.toJson());

            PaymentMethodCreateParams paymentMethodCreateParams = PaymentMethodCreateParams.createFromGooglePay(jsonObj);

            stripe.createPaymentMethod(
                    paymentMethodCreateParams,
                    new ApiResultCallback<PaymentMethod>() {
                        @Override
                        public void onSuccess(@NonNull PaymentMethod result) 
                        {
                            callbackContext.success(result.id);
                        }

                        @Override
                        public void onError(@NonNull Exception e) 
                        {
                            callbackContext.error("Error2 occurred while attempting to pay with GooglePay. Error #" + e.toString());
                        }
                    }
            );
        } catch (JSONException e) 
        {
            //Log.i("DRIVER", e.toString());
            //webView.loadUrl("javascript:console.log('Error');");
            callbackContext.error("JSON error");
        }
    }
}