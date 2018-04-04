package com.wallethub.plugin;

import com.wallethub.plugin.OkHttpUtils;

import android.os.Bundle;
import android.content.Context;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import android.webkit.WebSettings;
import android.webkit.WebView;

import org.apache.cordova.LOG;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
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
import com.franmontiel.persistentcookiejar.*; //for persistentcookiejar
import com.franmontiel.persistentcookiejar.cache.*; 
import com.franmontiel.persistentcookiejar.persistence.*; 

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;


import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import static java.net.CookiePolicy.ACCEPT_ORIGINAL_SERVER;
import android.content.SharedPreferences;

//import java.util.Map;
//import java.util.HashMap;
import android.util.Log;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okio.ByteString;


//import com.wallethub.plugin.OkHttpCertPin;

/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaPluginSslSupport extends CordovaPlugin {

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
CookieManager cookieManager = new CookieManager(null, ACCEPT_ORIGINAL_SERVER);
//cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

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
                        Log.e("SSLpinning", key + "=" + "sha256/" + vals[i]);
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

ArrayList<String> domainlist = new ArrayList<String>(); //saves all secure domains


public OkHttpClient getOkHttpClient()  {        

try{


OkHttpClient okHttpClient = new OkHttpClient.Builder()
    //.certificatePinner(certPinner)
    //.certificatePinner(getPinnedHashes()) // we will do it during the get or post calls, since activity is not available
    //.cookieJar(new JavaNetCookieJar(cookieManager))
    //.cookieJar(cookieJar)
    .connectTimeout(10,TimeUnit.SECONDS)
    .writeTimeout(10,TimeUnit.SECONDS)
    .readTimeout(30,TimeUnit.SECONDS)
    .build();
    return okHttpClient;
    }
catch (Exception e) {
        //do something
        Log.e("SSLpinning", "getclientException+" +e.getMessage());
    }
    
    return new OkHttpClient();
}

//######################Main Execute function
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    PUBLIC_CALLBACKS = callbackContext;


    JSONObject retObj = new JSONObject();
    
        if(action.equals("get") || action.equals("post")) {
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
                
            }catch (NullPointerException e)
            {
                callbackContext.error(e.getMessage());
                return false;
            } catch(Exception e) {
                callbackContext.error(e.getMessage());
                 return false;
            }
            return true;
        }
        else if(action.equals("enableSSLPinning")) {
            try{
                this.enableSSLPinning(args, callbackContext);
                
            }catch (NullPointerException e)
            {
                callbackContext.error(e.getMessage());
                return false;
            } catch(Exception e) {
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
                
            }catch (NullPointerException e)
            {
                callbackContext.error(e.getMessage());
                return false;
            } catch(Exception e) {
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
    

    if(OKHTTPCLIENT_INIT == false){
    Log.e("SSLpinning", "OKHTTPCLIENT_INIT: Done");
    
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
    
    Boolean addthiscookie = true;
    String cookiedomain = "";

JSONObject jsonCookies = new JSONObject();

    //for all persistent cookies
    try{

        List<Cookie> cookies = loadAllPersistentCookies();
         for (int i = 0; i < cookies.size(); i++) {
                    Cookie cookie = cookies.get(i); 
                    cookiedomain = cookie.domain().toString();
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
                    jsonCookie.put("name", cookie.name().toString());
                    jsonCookie.put("value", cookie.value().toString());
                    jsonCookie.put("domain", cookie.domain().toString());
                    jsonCookie.put("path", cookie.path().toString());

                    jsonCookies.put(cookie.name().toString(), jsonCookie);
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
                                    doSSLpinningTrustManager(args, callbackContext);
                                }catch (NullPointerException e)
                                {
                                    callbackContext.error(e.getMessage());
                                } catch(Exception e) {
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
private void doSSLpinningTrustManager(JSONArray args, final CallbackContext callbackContext){

    Activity activity = this.cordova.getActivity(); 
    Context context = activity.getApplicationContext();
    CertificatePinner.Builder builder = new CertificatePinner.Builder();

    try {

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            
            String[] fileNames = context.getAssets().list("certificates");
            for(String name:fileNames){ 
                 //Log.e("SSLpinning", "getPEM: " + name);
                 if (name.endsWith(".pem")){
                        String domainname= name.replaceAll(".pem$", "");
                            InputStream certInputStream = context.getAssets().open("certificates/"  + name);
                            BufferedInputStream bis = new BufferedInputStream(certInputStream);
                            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                            Log.e("SSLpinning", "FOUND-PEM: " + name +  " for domain: " + domainname);
                            while (bis.available() > 0) {
                                Certificate cert = certificateFactory.generateCertificate(bis);
                                keyStore.setCertificateEntry(domainname, cert);
                                domainlist.add(domainname);
                            }
                    }
                    else if (name.endsWith(".cer")){
                        String domainname= name.replaceAll(".cer$", "");
                            InputStream certInputStream = context.getAssets().open("certificates/"  + name);
                            BufferedInputStream bis = new BufferedInputStream(certInputStream);
                            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                            Log.e("SSLpinning", "FOUND-CER: " + name +  " for domain: " + domainname);
                            while (bis.available() > 0) {
                                Certificate cert = certificateFactory.generateCertificate(bis);
                                //doGenerate256(cert); //trying to generate sha256 here
                                builder.add(domainname, doGenerate256(domainname, cert));
                                keyStore.setCertificateEntry(domainname, cert);
                                domainlist.add(domainname);
                            }
                    }
                    else{
                        Log.e("SSLpinning", "getPEMdomain: not pem");
                    }
            }            


            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);
            client = client.newBuilder()
                //.certificatePinner(getPinnedHashes())
                .certificatePinner(builder.build())
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
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
    Log.e("SSLpinning", "SHAgenerated!! : " +sha + " for domain: " + domainname);
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
String urlkey = "default";
JSONObject qdata = new JSONObject();
JSONObject headers = new JSONObject();
Boolean securedomain = false;
Boolean isjson = false;
OkHttpClient useClient;

final JSONObject retObj = new JSONObject();
Headers.Builder headersBuilder = new Headers.Builder();  
final RequestBody formBody;


Request request;


if(SSL_PINNING_STATUS == false && SSL_PINNING_STOP == false)
{

         try{
                doSSLpinningTrustManager(args, callbackContext);
            }catch (NullPointerException e)
            {
                callbackContext.error(e.getMessage());
            } catch(Exception e) {
                callbackContext.error(e.getMessage());
            }
    
     SSL_PINNING_STATUS = true;            
}
    
     try{
    headersBuilder.set("User-Agent", settings.getUserAgentString()); //for user agent
}
     catch (Exception e)
                {
                    //xx.toString();
                                retObj.put("data", "");
                                retObj.put("httperrorcode", 0);
                                retObj.put("errorcode", -1);
                                retObj.put("errorinfo",e.getMessage());
                                callbackContext.error(retObj);
                }

            try{
            qdata = args.getJSONObject(1);
            headers = args.getJSONObject(2);
                try  //lets iterate the headers object sent by the requesting function
                {
                 
                    
                   Iterator<?> keys = headers.keys();
                    while (keys.hasNext())
                    {
                        String key = (String) keys.next();
                        String value = headers.getString(key);
                        if(value.toLowerCase().contains("json")) {
                           isjson = true;
                        }
                        headersBuilder.set(key, value);
                    }
                }
                catch (Exception e)
                {
                    //xx.toString();
                                retObj.put("data", "");
                                retObj.put("httperrorcode", 0);
                                retObj.put("errorcode", -1);
                                retObj.put("errorinfo",e.getMessage());
                                callbackContext.error(retObj);
                }
            }
            catch(JSONException e){
            Log.e("DataObjecterror", e.getMessage());
            retObj.put("data", "");
            retObj.put("httperrorcode", 0);
            retObj.put("errorcode", 0);
            retObj.put("errorinfo",e.getMessage());
            callbackContext.error(retObj);
            }
            
            

        if (url != null && url.length() > 0) 
        {
        try{
        
            urlkey = args.getString(3);
            
            String domainname = HttpUrl.parse(url).host();
            String wildcarddomainname = HttpUrl.parse(url).host();
            
            String [] arr = domainname.split("\\.");
            if(arr.length == 2)//just a common case of wildcard domain supporting the root as well
            {
                wildcarddomainname = "*." + arr[0] + '.' +  arr[1]; 
            }
            else if(arr.length > 2)
            {
                wildcarddomainname = "*"; 
                for(int i=1;i<arr.length;i++){
                      wildcarddomainname += "." + arr[i];
                    }
            }
            Log.e("SSLpinning", "WILDCARDDomain: " + wildcarddomainname);
            
            if (domainlist.contains(domainname)) {
                    securedomain = true;
                    Log.e("SSLpinning", "ParsedDomain: " + domainname + " Type: Secure : " + urlkey);
            }
            else if (domainlist.contains(wildcarddomainname)) {
                    securedomain = true;
                    Log.e("SSLpinning", "ParsedWildCardDomain: " + domainname + " Type: Secure : " + urlkey);
            }
            else{
                    securedomain = false;
                    Log.e("SSLpinning", "ParsedDomain: " + domainname + " Type: Not Secure : " + urlkey);
            }

if(action.equals("post"))
{
            if(isjson)
            {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                formBody = RequestBody.create(JSON, qdata.toString());
            }
            else
            {
                //## adding the post parameters
                FormBody.Builder formBuilder = new FormBody.Builder();
                Iterator<?> keys = qdata.keys();
                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    formBuilder.add(key, qdata.getString(key));
                }
                formBody = formBuilder.build();
                //## post parameters done
            }
            Log.e("SSLpinning", "POSTasJSON: " + isjson);
            
            newurl = url;
            request = new Request.Builder().url(newurl).headers(headersBuilder.build()).post(formBody).tag(urlkey).build();
            
}
else
{
                //## adding the query parameters
                HttpUrl.Builder httpBuider = HttpUrl.parse(url).newBuilder();
                Iterator<?> keys = qdata.keys();
                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    httpBuider.addQueryParameter(key, qdata.getString(key));
                }
                
                newurl = httpBuider.build().toString();
                //## query params done
            request = new Request.Builder().url(newurl).headers(headersBuilder.build()).tag(urlkey).build();
            
}

if(securedomain == true)
{
useClient = client;
}
else
{
useClient = httpclient;
}

            OkHttpUtils.cancelCallWithTag(useClient, urlkey);
            
            useClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                                //e.printStackTrace();
                                try{
                                Log.e("SSLpINErroR", e.getMessage());
                                String errStr = e.getMessage();
                                String err = errStr.toLowerCase();
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
                                
                                int myNum = 0;



                                Iterator<String> iter = erritems.keys();
                                while (iter.hasNext()) {
                                    String key = iter.next();
                                    try {
                                        String val = erritems.get(key).toString();
                                        if(err.contains(val.toLowerCase()))
                                        {
                                            try {
                                                myNum = Integer.parseInt(key.replaceAll("err", ""));
                                                retObj.put("errorcode", myNum);
                                                    } catch(NumberFormatException nfe) {
                                                System.out.println("Could not parse " + nfe);
                                                } 
                                        //retObj.put("errorcode", key.replaceAll("err", ""));
                                        break;
                                        }
                                    } catch (JSONException ee) {
                                        // Something went wrong!
                                    }
                                }
                                
                                retObj.put("errorinfo",errStr);
                                }catch(JSONException je){
                                    Log.e("Data object error", je.getMessage());
                                }catch(Exception fe){
                                    Log.e("onFailure SSL error", fe.getMessage());
                                }

                                callbackContext.error(retObj);
                                //callbackContext.error(e.getMessage());
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                        JSONObject jsonCookies = new JSONObject();
                        try{
 
                        JSONObject jsonHeaders = new JSONObject();

                        Headers responseHeaders = response.headers();
                          for (int i = 0; i < responseHeaders.size(); i++) {
                            jsonHeaders.put(responseHeaders.name(i).toString(), responseHeaders.value(i));
                          }

                        retObj.put("data", response.body().string());
                        retObj.put("headers", jsonHeaders);
                        
                        if(response.isSuccessful())
                        {
                        retObj.put("status", response.code());
                        callbackContext.success(retObj);
                        }
                        else
                        {
                        retObj.put("httperrorcode", response.code());
                        //retObj.put("errorinfo", response.message()); //response.toString()  //message is empty always!
                        retObj.put("errorcode", response.code());
                            
                            if(response.code() >= 400 && response.code() <= 600){retObj.put("errorcode", -1011);}
                        
                        retObj.put("errorinfo", response.toString()); //response.toString()
                        callbackContext.error(retObj);
                        }
                    }catch(JSONException e){
                        Log.e("DataobjectError", e.getMessage());
                                callbackContext.error("DataObjectErrN"+ e.getMessage());
                        
                    }
                        
                }
            });
            
            
            
            }
            catch (Exception e) {
                    //do something
                    Log.e("SSLGETErroR", e.getMessage());
                    retObj.put("data", "");
                                retObj.put("httperrorcode", 0);
                                retObj.put("errorcode", 1203);
                                retObj.put("errorinfo",e.getMessage());
                                callbackContext.error(retObj);
                    //callbackContext.error(e.getMessage());
            }
        } 
        else 
        {
                                retObj.put("data", "");
                                retObj.put("httperrorcode", 0);
                                retObj.put("errorcode", -1);
                                retObj.put("errorinfo","Incorrect url parameter");
                                callbackContext.error(retObj);
            //callbackContext.error("Some problems with the url parameter");
        }
}

//###############

}

// end of class method

//#############################