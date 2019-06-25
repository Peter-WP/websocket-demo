package com.example.websocketdemo.socket.springsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.Data;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * 客户端应答对象
 *
 * @author peter
 * date: 2019-05-08 09:37
 **/
@Data
public class ClientReply {

    private String msgId;


    private String ack;


    /**
     * websocket 收到的消息是否可能是应答消息
     *
     * @param jsonMsg
     * @return
     */
    public static boolean maybeClientReply(String jsonMsg) {
        if (Strings.isNullOrEmpty(jsonMsg)) return false;


        Field[] declaredFields = ClientReply.class.getDeclaredFields();

        return Arrays.stream(declaredFields).allMatch(o -> {
            int i = jsonMsg.indexOf(o.getName());
            return i >= 0;
        });
    }

    /**
     * websocket 收到的消息解析成应答消息
     *
     * @param jsonMsg
     * @return 解析失败返回null
     */
    public static ClientReply parseJsonMsg(String jsonMsg) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(jsonMsg, ClientReply.class);
        } catch (IOException e) {
            return null;
        }

    }

}
