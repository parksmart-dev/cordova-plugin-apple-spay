package cordova-plugin-apple-spay;

import android.util.Log;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

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
    private String publicToken;

    @Override
    protected void pluginInitialize() {
        Log.d("pluginInitialize plugin");
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.publicToken = this.preferences.getString("DOMAIN_URI_PREFIX", "");
        Log.d("onCreate plugin", this.publicToken);

        PaymentConfiguration.init(this, PUBLISHABLE_KEY);

        paymentsClient = Wallet.getPaymentsClient(
            this,
            new Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()
        );

        // stripe = new Stripe(this, pk_test_nXYwkeGYwZPZnGPqiC3Qq0Oz00jnJZzjr8);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("canMakePayments")) {
            this.canMakePayments(args, callbackContext);
            return true;
        }
        return false;
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
}
