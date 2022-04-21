package com.example.demo;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicReference;

public class DemoApplication {

    public static void main(String[] args) {
        //https://www.baeldung.com/spring-5-webclient
        //https://stackoverflow.com/questions/64193940/how-to-upload-file-with-spring-5-webclient

        //CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        //还可以使用default cookie等设置cookie
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
                    for (var cookie : mycookies.get().entrySet()) {
                        System.out.println(cookie.getKey() + " : " + cookie.getValue());

                    }

                    return response.bodyToMono(String.class);
                })
                .block();
//                .retrieve()
//                .bodyToMono(String.class)

//        Consumer<MultiValueMap<String,String>>cookieConsumer=new Consumer<MultiValueMap<String, String>>();
//        webClient.options().mycookies()
        String key = "";
        String value = "";
        for (var cookie : mycookies.get().entrySet()) {

            key=cookie.getKey();
            value=cookie.getValue().get(0).getValue();
            System.out.println(key + " : " + value);
        }
        System.out.println(res);
        res=webClient.get().uri("http://qiuyuhan.xyz:8080/api/user/info")
                .cookie(key,value)
                .retrieve()
                .bodyToMono(String.class).block();
        System.out.println(res);
    }

}
