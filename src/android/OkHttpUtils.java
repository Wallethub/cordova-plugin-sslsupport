package com.wallethub.plugin;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class OkHttpUtils {
    public static void cancelCallWithTag(OkHttpClient client, String tag) {
        for(Call call : client.dispatcher().queuedCalls()) {
            if(call.request().tag().equals(tag))
                call.cancel();
        }
        for(Call call : client.dispatcher().runningCalls()) {
            if(call.request().tag().equals(tag))
                call.cancel();
        }
    }
}