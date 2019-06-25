package com.example.websocketdemo.socket.springsocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author peter
 * date: 2019-05-07 13:40
 **/
@Configuration
@EnableWebSocket
public class SpringSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/socket-server")
                .addInterceptors(myInterceptor()).setAllowedOrigins("*");


        registry.addHandler(myHandler(), "/socket-js/socket-server").setAllowedOrigins("*")
                .addInterceptors(myInterceptor()).withSockJS();
    }

    @Bean
    public SocketHandler myHandler() {
        return new SocketHandler();
    }

    @Bean
    WebSocketInterceptor myInterceptor() {
        return new WebSocketInterceptor();
    }

}
