package com.sslsupport.plugin;


import android.os.Bundle;
import android.content.Context;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.apache.cordova.PluginResult;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;


//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//import okhttp3.HttpUrl;
//import okhttp3.FormBody;
//import okhttp3.RequestBody;
//import okhttp3.CertificatePinner;

import okhttp3.*;
import com.thomasbouvier.persistentcookiejar.*; //for persistentcookiejar
import com.thomasbouvier.persistentcookiejar.cache.*;
import com.thomasbouvier.persistentcookiejar.persistence.*;

import java.io.IOException;
import java.io.BufferedInputStream;

import java.net.URI;
import android.content.SharedPreferences;

//import java.util.Map;
//import java.util.HashMap;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.*;
import java.util.concurrent.TimeUnit;


import java.io.InputStream;

import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;


//import com.wallethub.plugin.OkHttpCertPin;

/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaPluginSslSupport extends CordovaPlugin {

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override public long contentLength() {
            return responseBody.contentLength();
        }

        @NonNull
        @Override public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }

    public WebSettings settings;

    //Context context=this.cordova.getActivity().getApplicationContext();
    //ClearableCookieJar cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));
    //private PersistentCookieJar persistentCookieJar;
     CallbackContext PUBLIC_CALLBACKS = null;

    ClearableCookieJar cookieJar;

    Activity activity;
    ApplicationInfo appliInfo = null;

    Boolean SSL_PINNING_STATUS = false; //flag to know whether the certificates have been pinned or not
    Boolean SSL_PINNING_STOP = false; //force stop pinning, and remove cert pinner
    Boolean OKHTTPCLIENT_INIT = false;

    String newurl = "";

    public CertificatePinner getPinnedHashes() {
        CertificatePinner.Builder builder = new CertificatePinner.Builder();
        Activity activity = this.cordova.getActivity();
            try {
                appliInfo = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
            } catch (NameNotFoundException e) {
                Log.e("SSLpinning", "NameNotFoundException+" + e.getMessage());
            }
            Bundle bundle = appliInfo.metaData;
            try {

                    for (String key : bundle.keySet()) {
                        String val = bundle.get(key).toString();
                        String [] vals = val.split(",");
                        for(int i=0;i<vals.length;i++){
                            builder.add(key, "sha256/" + vals[i]);
                            Log.i("SSLpinning", key + "=" + "sha256/" + vals[i]);
                        }
                    }
            }
            catch (IllegalArgumentException e) {
          //do something
                    Log.e("SSLpinning", "IllegalArgumentException+" + e.getMessage());
            }
            catch (Exception e) {
                //do something
                Log.e("SSLpinning", "Exception+" +e.getMessage());
            }

        return builder.build();
    }

    OkHttpClient client = getOkHttpClient(); //new OkHttpClient();
    //OkHttpClient client = new OkHttpClient.Builder().build();
    OkHttpClient httpclient = getOkHttpClient();

    ArrayList<String> domainlist = new ArrayList<>(); //saves all secure domains


    public OkHttpClient getOkHttpClient() {

        try {
            return new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            //do something
            Log.e("SSLpinning", "getclientException+" + e.getMessage());
        }

        return new OkHttpClient();
    }

//######################Main Execute function
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    PUBLIC_CALLBACKS = callbackContext;


    JSONObject retObj = new JSONObject();
    
        if(action.equals("get") || action.equals("post") || action.equals("download")) {
        try{
            try{
                this.getpostMethod(action, args, callbackContext);
                
            }catch (NullPointerException e)
            {
                                retObj.put("data", "");
                                retObj.put("httperrorcode", 0);
                                retObj.put("errorcode", -3);
                                retObj.put("errorinfo",e.getMessage());
                                callbackContext.error(retObj);
                                
                return false;
            } catch(Exception e) {
                                retObj.put("data", "");
                                retObj.put("httperrorcode", 0);
                                retObj.put("errorcode", -2);
                                retObj.put("errorinfo",e.getMessage());
                                callbackContext.error(retObj);
                 return false;
            }
            }catch(JSONException e){
                    callbackContext.error("JSONerror="+e.getMessage());
            }
            return true;
        }
        else if(action.equals("getCookies")) {
            try{
                this.getCookies(args, callbackContext);
                
            } catch (Exception e)
            {
                callbackContext.error(e.getMessage());
                return false;
            }
            return true;
        }
        else if(action.equals("enableSSLPinning")) {
            try{
                this.enableSSLPinning(args, callbackContext);
                
            } catch (Exception e)
            {
                callbackContext.error(e.getMessage());
                return false;
            }
            return true;
        }
        else if(action.equals("cancelRequest")) {
            try{
                String urlkey = args.getString(0);
                OkHttpUtils.cancelCallWithTag(client, urlkey);
                OkHttpUtils.cancelCallWithTag(httpclient, urlkey);
                callbackContext.success("done");
                
            } catch (Exception e)
            {
                callbackContext.error(e.getMessage());
                return false;
            }
            return true;
        }
        
        
        return false;
    }

//#######################Initialize

 public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
     // try{

     //            settings = ((WebView) webView.getEngine().getView()).getSettings();

     //        }catch (Exception error){

     //            settings = null;

     //        }

    activity = cordova.getActivity();  
    Context context=activity.getApplicationContext();
    
      try{
                settings = ((WebView) webView.getEngine().getView()).getSettings();
            }catch (Exception error){
                //settings = null;
                //if the above fails then use the one below
                settings = new WebView(activity.getApplicationContext()).getSettings(); 
            }
    

    if(!OKHTTPCLIENT_INIT){
    Log.i("SSLpinning", "OKHTTPCLIENT_INIT: Done");
    
       cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));
          httpclient = httpclient.newBuilder()
                .cookieJar(cookieJar)
                .build();      
           client = client.newBuilder()
                .cookieJar(cookieJar)
                .build();                

        OKHTTPCLIENT_INIT = true;
        
    }
}


//########### Get all cookies function
private void getCookies(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String domainName = "all";
    if (args.length() > 0) {
        domainName = args.getString(0);
    }
    
    boolean addthiscookie;
    String cookiedomain;

JSONObject jsonCookies = new JSONObject();

    //for all persistent cookies
    try{

        List<Cookie> cookies = loadAllPersistentCookies();
         for (int i = 0; i < cookies.size(); i++) {
                    Cookie cookie = cookies.get(i); 
                    cookiedomain = cookie.domain();
                    if(domainName.equals("all"))
                    {
                        addthiscookie = true;
                    }
                    else{
                        if(cookiedomain.contains(domainName)) {
                           addthiscookie = true;
                        }
                        else{
                            addthiscookie = false;
                        }
                    }
                    
                    if(addthiscookie)
                    {   
                    JSONObject jsonCookie = new JSONObject();
                    jsonCookie.put("name", cookie.name());
                    jsonCookie.put("value", cookie.value());
                    jsonCookie.put("domain", cookie.domain());
                    jsonCookie.put("path", cookie.path());

                    jsonCookies.put(cookie.name(), jsonCookie);
                    }
                }


        callbackContext.success(jsonCookies);
    }catch(JSONException je){
                Log.e("CookieError", je.getMessage());
                callbackContext.error(je.getMessage());
    }
            
        
}
//###################################################### Helper function
private List<Cookie> loadAllPersistentCookies() {
activity = this.cordova.getActivity();  
    Context context=activity.getApplicationContext();
    
SharedPreferences sharedPreferences = context.getSharedPreferences("CookiePersistence", Context.MODE_PRIVATE);
       // List<Cookie> cookies = new ArrayList<>(sharedPreferences.getAll().size());
        List cookies = new ArrayList(sharedPreferences.getAll().size()); 

        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            String serializedCookie = (String) entry.getValue();
            Cookie cookie = new SerializableCookie().decode(serializedCookie);
            if (cookie != null) {
                cookies.add(cookie);
            }
        }
        return cookies;
    }
//########### Enable or disable SSL function
private void enableSSLPinning(JSONArray args, CallbackContext callbackContext) throws JSONException {

boolean sslstatus = args.getBoolean(0);
//permanently enable the sslpinning as  default: true

            try{
                        if(!sslstatus)
                        {
                                client = client.newBuilder().certificatePinner(CertificatePinner.DEFAULT).build();
                                SSL_PINNING_STATUS = false;  
                                SSL_PINNING_STOP = true;
                                callbackContext.success("SSL Pinning disabled");
                        }
                        else
                        {
                            //client = client.newBuilder().certificatePinner(getPinnedHashes()).build();
                             try{
                                    doSSLpinning(args, callbackContext);
                                } catch (Exception e)
                                {
                                    callbackContext.error(e.getMessage());
                                }
                            SSL_PINNING_STATUS = true;    
                            SSL_PINNING_STOP = false;
                            callbackContext.success("SSL Pinning enabled");
                        }
           
            }catch(Exception e){
                        Log.e("SSLflagError", e.getMessage());
                        callbackContext.error(e.getMessage());
            }
            
        
}
//########### Enable SSL pinning with Trust Manager, Helper function
private void doSSLpinning(JSONArray args, final CallbackContext callbackContext){

    Activity activity = this.cordova.getActivity(); 
    Context context = activity.getApplicationContext();
    CertificatePinner.Builder builder = new CertificatePinner.Builder();

    try {            
            String[] fileNames = context.getAssets().list("certificates");
            for(String name:fileNames){ 
                 //Log.e("SSLpinning", "getPEM: " + name);
                 if (name.endsWith(".pem")){
                        String domainname= name.replaceAll(".pem$", "");
                            InputStream certInputStream = context.getAssets().open("certificates/"  + name);
                            BufferedInputStream bis = new BufferedInputStream(certInputStream);
                            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                            Log.i("SSLpinning", "FOUND-PEM: " + name +  " for domain: " + domainname);
                            while (bis.available() > 0) {
                                Certificate cert = certificateFactory.generateCertificate(bis);
                                builder.add(domainname, doGenerate256(domainname, cert));
                                domainlist.add(domainname);
                            }
                    }
                    else if (name.endsWith(".cer")){
                        String domainname= name.replaceAll(".cer$", "");
                            InputStream certInputStream = context.getAssets().open("certificates/"  + name);
                            BufferedInputStream bis = new BufferedInputStream(certInputStream);
                            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                            Log.i("SSLpinning", "FOUND-CER: " + name +  " for domain: " + domainname);
                            while (bis.available() > 0) {
                                Certificate cert = certificateFactory.generateCertificate(bis);
                                builder.add(domainname, doGenerate256(domainname, cert));
                                domainlist.add(domainname);
                            }
                    }
                    else{
                        Log.i("SSLpinning", "getPEMdomain: not pem");
                    }
            }            

            client = client.newBuilder()
                .certificatePinner(builder.build())
                .build();
        } catch (Exception e) {
            Log.e("SSLpinning", "getsslconteXTException+" +e.getMessage());
        }

}
//#####################
private String doGenerate256(String domainname, Certificate certificate){

    if (!(certificate instanceof X509Certificate)) {
      throw new IllegalArgumentException("Certificate pinning requires X509 certificates");
    }
    String sha =  "sha256/" + sha256((X509Certificate) certificate).base64();
    Log.i("SSLpinning", "SHAgenerated!! : " +sha + " for domain: " + domainname);
    return sha;
}

private ByteString sha256(X509Certificate x509Certificate) {
    return ByteString.of(x509Certificate.getPublicKey().getEncoded()).sha256();
  }
//########### get/post function, the 1st variable is "get" or "post"
private void getpostMethod(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

    //url : "https://www.google.co.in/search",
    //data : {q: "check+data", oq: "check+data"},
    //headers : {}, urlkey : "google"

    String url = args.getString(0);
    String urlkey;
    JSONObject qdata = new JSONObject();
    JSONObject headers = new JSONObject();
    String dest = "";
    boolean securedomain;
    boolean isjson = false;
    OkHttpClient useClient;

    final JSONObject retObj = new JSONObject();
    Headers.Builder headersBuilder = new Headers.Builder();
    final RequestBody formBody;


    Request request;


    if(!SSL_PINNING_STATUS && !SSL_PINNING_STOP)
    {
        try {
            doSSLpinning(args, callbackContext);
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }

        SSL_PINNING_STATUS = true;
    }

    try {
        headersBuilder.set("User-Agent", settings.getUserAgentString()); //for user agent
    } catch (Exception e) {
        retObj.put("data", "");
        retObj.put("httperrorcode", 0);
        retObj.put("errorcode", -1);
        retObj.put("errorinfo",e.getMessage());
        callbackContext.error(retObj);
    }

    try {

        if (!action.equals("download")) qdata = args.getJSONObject(1);
        else dest = args.getString(1);

        headers = args.getJSONObject(2);

        try  //lets iterate the headers object sent by the requesting function
        {


            Iterator<?> keys = headers.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = headers.getString(key);
                if (key.toLowerCase().contains("content-type") && value.toLowerCase().contains("json")) {
                    isjson = true;
                }
                headersBuilder.set(key, value);
            }
        } catch (Exception e) {
            //xx.toString();
            retObj.put("data", "");
            retObj.put("httperrorcode", 0);
            retObj.put("errorcode", -1);
            retObj.put("errorinfo", e.getMessage());
            callbackContext.error(retObj);
        }

    } catch (JSONException e) {
        Log.e("DataObjecterror", e.getMessage());
        retObj.put("data", "");
        retObj.put("httperrorcode", 0);
        retObj.put("errorcode", 0);
        retObj.put("errorinfo", e.getMessage());
        callbackContext.error(retObj);
    }


    if (url != null && url.length() > 0) {
        try {

            urlkey = args.getString(3);

            String domainname = Objects.requireNonNull(HttpUrl.parse(url)).host();
            String wildcarddomainname = Objects.requireNonNull(HttpUrl.parse(url)).host();

            String[] arr = domainname.split("\\.");
            if (arr.length == 2)//just a common case of wildcard domain supporting the root as well
            {
                wildcarddomainname = "*." + arr[0] + '.' + arr[1];
            } else if (arr.length > 2) {
                wildcarddomainname = "*";
                for (int i = 1; i < arr.length; i++) {
                    wildcarddomainname += "." + arr[i];
                }
            }
            Log.i("SSLpinning", "WILDCARDDomain: " + wildcarddomainname);

            if (domainlist.contains(domainname)) {
                securedomain = true;
                Log.i("SSLpinning", "ParsedDomain: " + domainname + " Type: Secure : " + urlkey);
            } else if (domainlist.contains(wildcarddomainname)) {
                securedomain = true;
                Log.i("SSLpinning", "ParsedWildCardDomain: " + domainname + " Type: Secure : " + urlkey);
            } else {
                securedomain = false;
                Log.i("SSLpinning", "ParsedDomain: " + domainname + " Type: Not Secure : " + urlkey);
            }
// POST
            if (action.equals("post")) {
                if (isjson) {
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    formBody = RequestBody.create(JSON, qdata.toString());
                } else {
                    //## adding the post parameters
                    FormBody.Builder formBuilder = new FormBody.Builder();
                    Iterator<?> keys = qdata.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        formBuilder.add(key, qdata.getString(key));
                    }
                    formBody = formBuilder.build();
                    //## post parameters done
                }
                Log.i("SSLpinning", "POSTasJSON: " + isjson);

                newurl = url;
                request = new Request.Builder().url(newurl).headers(headersBuilder.build()).post(formBody).tag(urlkey).build();

            } else {

                //## adding the query parameters
                HttpUrl.Builder httpBuider = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
                Iterator<?> keys = qdata.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    httpBuider.addQueryParameter(key, qdata.getString(key));
                }

                newurl = httpBuider.build().toString();
                request = new Request.Builder().url(newurl).headers(headersBuilder.build()).tag(urlkey).build();
            }

            if (securedomain) {
                useClient = client;
            } else {
                useClient = httpclient;
            }
            // for a download request add progress listener
            if(action.equals("download")) {
                final ProgressListener progressListener = new ProgressListener() {
                    @Override
                    public void update(long bytesRead, long contentLength, boolean done) {
                        final JSONObject retObj = new JSONObject();
                        try {
                            if (!done) {
                                if (contentLength != -1) {
                                    double progress = ((100 * (double) bytesRead) / (double) contentLength) / 100;

                                    retObj.put("progress", progress);
                                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, retObj);
                                    pluginResult.setKeepCallback(true);

                                    callbackContext.sendPluginResult(pluginResult);
                                }
                            }
                        } catch (Exception e) {
                            String err = (e == null || e.getMessage()==null) ? "Donwload Progress Failed": e.getMessage();
                            Log.e("SSLDWNError",err);
                        }
                    }
                };
                Log.i("SSLpinning", "create download client");
                useClient = useClient.newBuilder()
                        .addNetworkInterceptor(new Interceptor() {
                            @NonNull
                            @Override public Response intercept(@NonNull Chain chain) throws IOException {
                                Response originalResponse = chain.proceed(chain.request());
                                return originalResponse.newBuilder()
                                        .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                                        .build();
                            }
                        }).build();
            }

            OkHttpUtils.cancelCallWithTag(useClient, urlkey);

            String finalDest =
                    dest != null && dest.length() > 1 && !dest.equals("null") ? dest :
                            cordova.getContext().getFilesDir().toString() + "/" + URLUtil.guessFileName(request.url().toString(),null,null);

            useClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    //e.printStackTrace();
                    try {
                        Log.e("SSLpINErroR", e.getMessage());
                        String errStr = e.getMessage();
                        String err = Objects.requireNonNull(errStr).toLowerCase();
                        JSONObject erritems = new JSONObject();

                        erritems.put("err-1202", "CertPathValidatorException");
                        erritems.put("err-1205", "Certificate pinning failure");
                        //erritems.put("err-1003", "No address associated with hostname");
                        erritems.put("err-1001", "failed to connect");
                        erritems.put("err-10", "No address associated with hostname"); //no internet
                        //erritems.put("err-10", "failed to connect"); //no internet
                        erritems.put("err-999", "Canceled"); //navigation cancelled by the app by triggering the unique key method

                        retObj.put("data", "");
                        retObj.put("httperrorcode", 0);
                        retObj.put("errorcode", -1);

                        int myNum;


                        Iterator<String> iter = erritems.keys();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            try {
                                String val = erritems.get(key).toString();
                                if (err.contains(val.toLowerCase())) {
                                    try {
                                        myNum = Integer.parseInt(key.replaceAll("err", ""));
                                        retObj.put("errorcode", myNum);
                                    } catch (NumberFormatException nfe) {
                                        System.out.println("Could not parse " + nfe);
                                    }
                                    //retObj.put("errorcode", key.replaceAll("err", ""));
                                    break;
                                }
                            } catch (JSONException ee) {
                                // Something went wrong!
                            }
                        }

                        retObj.put("errorinfo", errStr);
                    } catch (JSONException je) {
                        Log.e("Data object error", je.getMessage());
                    } catch (Exception fe) {
                        Log.e("onFailure SSL error", fe.getMessage());
                    }

                    callbackContext.error(retObj);
                    //callbackContext.error(e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull final Response response) {
                    try {

                        JSONObject jsonHeaders = new JSONObject();

                        Headers responseHeaders = response.headers();
                        for (int i = 0; i < responseHeaders.size(); i++) {
                            jsonHeaders.put(responseHeaders.name(i), responseHeaders.value(i));
                        }

                        retObj.put("headers", jsonHeaders);

                        if (response.isSuccessful()) {
                            if(action.equals("download")) {
                                File file = new File(new URI(finalDest));
                                file.createNewFile();
                                BufferedSink sink = Okio.buffer(Okio.sink(file));
                                // you can access body of response
                                sink.writeAll(Objects.requireNonNull(response.body()).source());
                                sink.close();

                                Log.i("SSLpinning", "File downloaded to: "+ finalDest);

                                retObj.put("url", finalDest);

                            } else {
                                retObj.put("data", Objects.requireNonNull(response.body()).string());
                            }

                            retObj.put("status", response.code());
                            callbackContext.success(retObj);

                        } else {
                            retObj.put("data", Objects.requireNonNull(response.body()).string());

                            retObj.put("httperrorcode", response.code());

                            if (response.code() >= 400 && response.code() <= 600) {
                                retObj.put("errorcode", -1011);
                            } else {
                                retObj.put("errorcode", response.code());
                            }

                            retObj.put("errorinfo", response.toString());

                            callbackContext.error(retObj);
                        }
                    } catch (JSONException e) {
                        Log.e("DataobjectError", e.getMessage());
                        callbackContext.error("DataObjectErrN" + e.getMessage());

                    } catch (Exception e) {
                        String err = ( e == null || e.getMessage()==null ) ? "Donwload Failed" : e.getMessage();
                        Log.e("SSLDWNGETPOSError", err);

                        try {
                            retObj.put("data", "");
                            retObj.put("httperrorcode", 0);
                            retObj.put("errorcode", 1203);
                            retObj.put("errorinfo", err);
                        } catch (JSONException ex) { }

                        callbackContext.error(retObj);
                    }

                }
            });


        } catch (Exception e) {
            //do something
            Log.e("SSLError", e.getMessage());
            retObj.put("data", "");
            retObj.put("httperrorcode", 0);
            retObj.put("errorcode", 1203);
            retObj.put("errorinfo", e.getMessage());
            callbackContext.error(retObj);
            //callbackContext.error(e.getMessage());
        }
    } else {
        retObj.put("data", "");
        retObj.put("httperrorcode", 0);
        retObj.put("errorcode", -1);
        retObj.put("errorinfo", "Incorrect url parameter");
        callbackContext.error(retObj);
        //callbackContext.error("Some problems with the url parameter");
    }
}

//###############

}

// end of class method

//#############################