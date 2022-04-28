package com.example.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button=findViewById(R.id.loginButton);
        //https://www.jianshu.com/p/b7d333f93c73
        //null是存储，可以在这里实现持久化的存储来保持登录状态
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));//开启后就会获得所有cookie

        //https://www.jianshu.com/p/d6ab78e4ed73
        //上传表单信息
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Runnable run=new Runnable() {
                    @Override
                    public void run() {
                        MultipartBodyBuilder builder=new MultipartBodyBuilder();
                        builder.part("email","yh-qiu18@163.com");
                        builder.part("password","123456");
                        WebClient webClient= WebClient.builder().build();
                        AtomicReference<MultiValueMap<String, ResponseCookie>> mycookies = new AtomicReference<>();
                        String res=webClient.post().uri("http://qiuyuhan.xyz:8080/api/login")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchangeToMono(response -> {
                                    mycookies.set(response.cookies());
                                    //mycookies.get().entrySet().stream().map(cookie -> cookie.getKey() + " : " + cookie.getValue()).forEach(System.out::println);

                                    return response.bodyToMono(String.class);
                                })
                                .block();
                        String key = "";
                        String value = "";

                        for (Map.Entry<String, List<ResponseCookie>> cookie : mycookies.get().entrySet()) {

                            key=cookie.getKey();
                            value=cookie.getValue().get(0).getValue();
                            System.out.println(key + " : " + value);
                        }
                        Log.d("net", res);
                        //https://blog.csdn.net/qq_36206114/article/details/107689814
                        res=webClient.get().uri("http://qiuyuhan.xyz:8080/api/user/info")
                                .cookie(key,value)
                                .retrieve()
                                .bodyToMono(String.class).block();

                        Log.d("net", res);
                    }

                };
                run.run();
//                HttpURLConnection conn;
//                MultipartBodyBuilder builder=new MultipartBodyBuilder();
//                //可以方便地build表单数据
//                builder.part("email","yh-qiu18@163.com");
//                builder.part("password","123456");
//
//                MultiValueMap<String, HttpEntity<?>>multipartBody=builder.build();
//
//                RestTemplate restTemplate=new RestTemplate();
//                ResponseEntity<String>responseEntity=restTemplate.postForEntity("http://qiuyuhan.xyz:8080/login",multipartBody,String.class);
//                Log.d("response", String.valueOf(responseEntity));
            }
        });
//        HttpURLConnection conn;
//        MultipartBodyBuilder builder=new MultipartBodyBuilder();
//        //可以方便地build表单数据
//        builder.part("email","yh-qiu18@163.com");
//        builder.part("password","123456");
//
//        MultiValueMap<String, HttpEntity<?>>multipartBody=builder.build();
//        //报错是因为不允许网络操作在主线程！！！
//        RestTemplate restTemplate=new RestTemplate();
//        //ResponseEntity<String>responseEntity=restTemplate.postForEntity("http://qiuyuhan.xyz:8080/login",multipartBody,String.class);
//        //Log.d("response", String.valueOf(responseEntity));
//        ResponseEntity<String>s=restTemplate.getForEntity("http://baidu.com",String.class);
//        Log.d("response", String.valueOf(s));

    }
}