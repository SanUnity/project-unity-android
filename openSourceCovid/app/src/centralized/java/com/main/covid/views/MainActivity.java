package com.main.covid.views;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.main.covid.R;
import com.main.covid.network.DataUploader;
import com.main.covid.network.NetworkStateReceiver;
import com.main.covid.receivers.ShareReceiver;
import com.main.covid.utils.BluetracerUtils;
import com.main.covid.utils.PermissionCompanion;
import com.main.covid.web.WebAppInterface;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import io.reactivex.disposables.Disposable;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.main.covid.BuildConfig.VERSION_NAME;


/**
 * Created by Rubén Izquierdo, Global Incubator
 */
public class MainActivity extends AppCompatActivity implements NetworkStateReceiver.NetworkStateReceiverListener {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 8888;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 9999;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATION = 7777;
    private static final int PERMISSION_REQUEST_CONTACTS = 6666;
    private static final int SHARE_CODE = 5050;

    private static final int REQ_CODE_GALLERY = 272;

    public static final String EXTRA_TITLE = "arg_title";
    public static final String EXTRA_URL = "arg_url";
    public static final String EXTRA_BASE_URL = "arg_base_url";
    public static final int REQUEST_BT_ENABLE = 2000;

    WebView webView;
    String url, baseUrl;
    private GeolocationPermissions.Callback mCallback;
    private String mOrigin;
    private final int LOCATION_PERM = 1000;
    private String userValue = "0";
    private Disposable disposableBTUploader;
    private NetworkStateReceiver networkStateReceiver;
    private boolean sharing = false;

    public static final String TAG = MainActivity.class.getSimpleName();
    private FirebaseAnalytics firebaseAnalytics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        lockScreen(true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
        updateFirebaseNoUserToken();

        startNetworkReceiver();

        initializeRunningServices();

        startBTService();

        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        } catch (Exception ex) {
            Log.e("FIREBASE", "FirebaseAnalytics was not initialized");
        }

        getUrlForWebView(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        getUrlForWebView(intent);

    }

    private void getUrlForWebView(Intent intent) {
        String urlNotification = intent.getStringExtra("urlNotification");

        if (urlNotification != null) {
            if (urlNotification.contains(getString(R.string.url)) ||
                    urlNotification.contains(getString(R.string.alternativeName1)) ||
                    urlNotification.contains(getString(R.string.alternativeName2))
            ) {
                initializeWebView(urlNotification);
            } else {
                Intent defaultBrowser = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER);
                defaultBrowser.setData(Uri.parse(urlNotification));
                startActivity(defaultBrowser);
            }
        } else {
            Uri data = intent.getData();
            String url;
            if (data != null) {
                url = data.toString()
                        .replace(getString(R.string.deeplinkUrl),
                                getString(R.string.url));
            } else {
                url = getString(R.string.url) + "?so=Android&v=" + VERSION_NAME;
            }
            initializeWebView(url);
        }
    }

    private void initializeWebView(String uri) {
        Uri dataURI = getIntent().getData();

        if (dataURI != null) {

            if (dataURI.getScheme().equals("https")) {
                url = uri;
                baseUrl = getString(R.string.base_url);
            } else {
                url = uri;
                baseUrl = getString(R.string.base_url);
            }

        } else if (getIntent().getStringExtra(EXTRA_URL) == null) {
            url = uri;
            baseUrl = getString(R.string.base_url);
        } else {
            url = uri;
            baseUrl = getIntent().getStringExtra(EXTRA_BASE_URL);
        }

        webView = findViewById(R.id.web_view);
        setupWebView();
        webView.loadUrl(url);
    }

    private void startNetworkReceiver() {
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void initializeRunningServices() {

        if (PermissionCompanion.INSTANCE.isBTServiceActivated()) {
            startBTService();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void lockScreen(Boolean portrait) {
        if (portrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    private void setupWebView() {
        //Cinfigura el webview
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.setWebChromeClient(new MyChromeClient());
        webView.setWebViewClient(new MyWebViewClient());
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.clearCache(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    }

    ValueCallback<Uri[]> filePathCallback;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERM:
                boolean isAllowed = grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (mCallback != null && mOrigin != null)
                    mCallback.invoke(mOrigin,
                            isAllowed,
                            false);
                mCallback = null;
                mOrigin = null;
                break;

            case REQUEST_BT_ENABLE:
                if (PermissionCompanion.INSTANCE.isBTServiceActivated())
                    startBTService();
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_GALLERY && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri imageUri = data.getData();
            Uri[] uris = new Uri[1];
            uris[0] = imageUri;
            filePathCallback.onReceiveValue(uris);

        } else if (requestCode == REQUEST_BT_ENABLE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.v("OK", "Bluetooth enabled");
            }
        } else if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATION) {
            if (resultCode == Activity.RESULT_OK &&
                    PermissionCompanion.INSTANCE.isBTServiceActivated()) {
                startBTService();
            }
        } else if (filePathCallback != null)
            filePathCallback.onReceiveValue(null);
        else if (requestCode == SHARE_CODE) {
            shareCallback(PermissionCompanion.INSTANCE.isShareSuccessful());
            PermissionCompanion.INSTANCE.resetShareSuccess();
        }
    }


    public void shareApp() {

        if (firebaseAnalytics != null) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.METHOD, "SHARE");
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle);
        }


        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.url_share) + "\n\n" + getString(R.string.text_share));
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    public void updateFirebaseToken() {

// LOGIN
        if (firebaseAnalytics != null) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.METHOD, "LOGIN");
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
        }


        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "getInstanceId failed", task.getException());
                        return;
                    }
                    String token = task.getResult().getToken();

                    DataUploader.INSTANCE.uploadIfNewDeviceTokenTrue(token);
                });
    }

    public void updateFirebaseNoUserToken() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "getInstanceId failed", task.getException());
                        return;
                    }
                    String token = task.getResult().getToken();

                    DataUploader.INSTANCE.uploadIfNewDeviceTokenTrueNotUser(token);
                });
    }

    @Override
    public void networkAvailable() {
        View networkM = findViewById(R.id.networkMessage);
        networkM.setVisibility(View.GONE);
    }

    @Override
    public void networkUnavailable() {
        View networkM = findViewById(R.id.networkMessage);
        networkM.setVisibility(View.VISIBLE);
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.contains("logout"))
                PermissionCompanion.INSTANCE.logout();
            super.onPageStarted(view, url, favicon);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
//        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack() && getResources().getBoolean(R.bool.backPressed)) {
//            webView.goBack();
//            return true;
//        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.disposableBTUploader != null)
            this.disposableBTUploader.dispose();
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
    }

    @Override
    public void onBackPressed() {

    }

    private class MyChromeClient extends WebChromeClient {


        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d("webconsole", consoleMessage.message());
            return true;
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

            MainActivity.this.filePathCallback = filePathCallback;

            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_PICK);
            startActivityForResult(Intent.createChooser(i, "Select Picture"), REQ_CODE_GALLERY);
            return true;

        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
//            callback.invoke(origin, true, false);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mCallback = callback;
                mOrigin = origin;
                requestLocationWebViewPermission();
            } else {
                callback.invoke(origin, true, false);
            }
        }

    }


    public void requestEnableBt() {
        if (!BluetracerUtils.INSTANCE.isBluetoothAvailable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
        }
    }

    public void startBTService() {
        requestLocationPermission();
        requestEnableBt();
        BluetracerUtils.INSTANCE.startBluetoothMonitoringService(this);
    }

    public void requestLocationWebViewPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERM);
    }

    public void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_DENIED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_COARSE_LOCATION);
    }

    public void getContacts() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_DENIED) {
            sharing = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST_CONTACTS);
        } else {
            HashMap<String, String> contactsMap = PermissionCompanion.INSTANCE.getContacts(this);
            String contactList = formatContacts(contactsMap);
            sendContacts(contactList);
        }
    }

    private String formatContacts(HashMap<String, String> contactsMap) {
        if (!contactsMap.isEmpty()) {
            JSONObject jsonObject = new JSONObject(contactsMap);
            return jsonObject.toString();
        } else
            return null;
    }

    public void share(@NotNull String message, @Nullable String image, @Nullable String url) {

        AsyncTask.execute(() -> {
            Uri uriImage = null;
            if (image != null) {
                Bitmap imageBitmap = getBitmapFromURL(image);
                uriImage = saveImageExternal(imageBitmap);
            }

            StringBuilder shareableMessage = new StringBuilder(message);
            if (url != null) {
                shareableMessage.append("\n\n");
                shareableMessage.append(url);
            }

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareableMessage.toString());
            sendIntent.setType("text/plain");


            if (uriImage != null) {
                sendIntent.putExtra(Intent.EXTRA_STREAM, uriImage);
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                sendIntent.setType("image/png");
            }

            PendingIntent pi = PendingIntent.getBroadcast(this, SHARE_CODE,
                    new Intent(this, ShareReceiver.class),
                    FLAG_UPDATE_CURRENT);

            PermissionCompanion.INSTANCE.resetShareSuccess();

            sharing = true;
            startActivityForResult(Intent.createChooser(sendIntent, null, pi.getIntentSender()), SHARE_CODE);
        });
    }

    public Bitmap getBitmapFromURL(String src) {
        try {
            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uri saveImageExternal(Bitmap image) {
        File imagesFolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.main.covid.fileprovider", file);

        } catch (IOException e) {
            Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;

    }

    private void sendContacts(String contacts) {
        if (contacts != null) {
            byte[] data = contacts.getBytes(StandardCharsets.UTF_8);
            String base64 = Base64.encodeToString(data, Base64.DEFAULT);
            runOnUiThread(() -> webView.loadUrl("javascript:setContacts('" + base64 + "')"));
        } else
            runOnUiThread(() -> webView.loadUrl("javascript:setContactsError()"));
    }

    public void shareCallback(boolean success) {
        int res = success ? 1 : 0;
        runOnUiThread(() -> webView.loadUrl("javascript:shareResult('" + res + "')"));
    }

    public void sendToken() {
        runOnUiThread(() -> webView.loadUrl("javascript:setToken('" + PermissionCompanion.INSTANCE.getApiToken() + "')"));
    }

    public void sendEmptyToken() {
        runOnUiThread(() -> webView.loadUrl("javascript:setToken('')"));
    }
}
