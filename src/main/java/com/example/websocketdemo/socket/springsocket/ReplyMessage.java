package com.example.websocketdemo.socket.springsocket;

import com.example.websocketdemo.socket.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.web.socket.TextMessage;

/**
 * websocket 需要确认客户端收到的message
 *
 * @author peter
 * date: 2019-05-08 09:32
 **/
@Data
public class ReplyMessage<T> {

    private String msgId;

    //发送对象的uid
    private String toUId;

    //消息对象的
    private T message;

    private int sendSuccessTimes;//发送成功的次数

    private int sendFailureTimes;//发送失败的次数

    private int maxSendTimes = 10;//最大的发送次数


    private long minWaitTime = 10 * 1000;//重发的最小时间间隔,ms

    private long maxWaitTime = 100 * 1000;//最大时间间隔,ms

    private void init() {
        msgId = String.valueOf(IdWorker.getId());
    }

    public ReplyMessage(String toUId, T message, int maxSendTimes) {
        this.toUId = toUId;
        this.message = message;
        this.maxSendTimes = maxSendTimes;
        init();
    }

    public ReplyMessage(String toUId, T message, int maxSendTimes, long minWaitTime, long maxWaitTime) {
        this(toUId,message,maxSendTimes);
        this.minWaitTime = minWaitTime;
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * 客户端是否已收到消息
     *
     * @see ClientReply
     */
    private boolean clientReply = false;


    public boolean isNeedSend() {
        if (!clientReply) {//客户端信息没收到信息
            return maxSendTimes >= sendSuccessTimes + sendFailureTimes;
        }
        return false;
    }

    public void sendSuccess() {
        this.sendSuccessTimes++;
    }

    public void sendFail() {
        this.sendFailureTimes++;
    }


    public TextMessage toTextMessage() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        String s = objectMapper.writeValueAsString(new Message(message,msgId));

        return new TextMessage(s);
    }


    public void reply() {
        this.clientReply = true;
    }

    @Data
    class Message{

        private T msg;
        private String msgId;

        Message(T msg, String msgId) {
            this.msg = msg;
            this.msgId = msgId;
        }
    }
}
