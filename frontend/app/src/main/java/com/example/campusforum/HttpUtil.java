package com.example.campusforum;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpUtil {
    //本地跑后端，用10.0.2.2这个ip地址
    //static final String baseUrl = "http://10.0.2.2:8080/api";
    static final  String baseUrl="http://qiuyuhan.xyz:8080/api";
    private static final String TAG = "HTTPUtil";
    static OkHttpClient client;

    /**
     * 初始化OkHttpClient
     */
    private static void init_okhttpclient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .cookieJar(new CookieJar() {
                        private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();
                        @Override
                        public void saveFromResponse(@NonNull HttpUrl httpUrl, @NonNull List<Cookie> cookies) {
                            //原来这种写法会导致存进去带/api/login的url，之后发请求就不会带上这个cookie
                            //cookieStore.put(httpUrl, cookies);

                            cookieStore.put(HttpUrl.get(baseUrl),cookies);

                            System.out.println("cookies url:" + cookies.get(0).domain());
                            System.out.println("cookies:" + cookies.toString());
                        }

                        @NonNull
                        @Override
                        public List<Cookie> loadForRequest(@NonNull HttpUrl httpUrl) {
                            List<Cookie> cookies = cookieStore.get(HttpUrl.get(baseUrl));
                            Log.d(TAG, "loadForRequest: "+cookies);
                            return cookies != null ? cookies : new ArrayList<Cookie>();
                        }
                    }).build();
        }
    }

    /**
     * @param url 请求对应的url，会被追加到baseUrl末尾
     * @param data post请求的数据，以键值对的形式存储
     * @param callback 请求完成之后的回调函数
     */
    public static void sendPostRequest(String url, HashMap<String, String> data, okhttp3.Callback callback) {
        init_okhttpclient();
        // get form data
        url = baseUrl + url;
        FormBody.Builder builder = new FormBody.Builder();
        for (String key: data.keySet()) {
            builder.add(key, data.get(key));
        }
        RequestBody requestBody = builder.build();
        Request request = new Request.Builder().url(url).post(requestBody).build();
        client.newCall(request).enqueue(callback);
    }

    /**
     * @param url 请求对应的url，会被追加到baseUrl末尾
     * @param query get请求的query值，会被追加到url末尾
     * @param callback 请求完成之后的回调函数
     */
    public static void sendGetRequest(String url, HashMap<String, String> query, okhttp3.Callback callback) {
        init_okhttpclient();
        // get query
        if (!query.isEmpty()) {
            url = baseUrl + url + "?";
            for (String key: query.keySet()) {
                url = url + key + "=" + query.get(key) + "&";
            }
            url = url.substring(0, url.length()-1);
        }
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(callback);
    }

    public static void sendRequestBody(String url,RequestBody requestBody,okhttp3.Callback callback){
        init_okhttpclient();

        url=baseUrl+url;
        Request request=new Request.Builder().url(url).post(requestBody).build();
        client.newCall(request).enqueue(callback);
    }
}
