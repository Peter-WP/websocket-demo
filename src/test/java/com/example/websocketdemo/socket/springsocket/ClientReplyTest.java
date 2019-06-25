package com.example.websocketdemo.socket.springsocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class ClientReplyTest {

    @Test
    public void isClientReply() {

        boolean cccccc = ClientReply.maybeClientReply("ack:,msgId:,dasd");
        System.out.println(cccccc);


    }

    @Test
    public void parseTest() throws JsonProcessingException {

        String msg = "sssdsad";

        String msg1 = "\"ack:,msgId:,dasd\"";


        String msg2 = new ObjectMapper().writeValueAsString(new ClientReply());

        ClientReply clientReply = ClientReply.parseJsonMsg(msg);
        ClientReply clientReply1 = ClientReply.parseJsonMsg(msg1);
        ClientReply clientReply2 = ClientReply.parseJsonMsg(msg2);

        Assert.assertNull(clientReply);
        Assert.assertNull(clientReply1);
        Assert.assertNotNull(clientReply2);


        System.out.println(new ObjectMapper().writeValueAsString(msg1));
    }

}