package com.example.websocketdemo.socket.springsocket;

import com.github.rholder.retry.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter
 * date: 2019-05-07 11:26
 **/
@Slf4j
public class SocketHandler implements WebSocketHandler {

    private static final AtomicInteger ONLINE = new AtomicInteger();

    private static final ConcurrentHashMap<String, WebSocketSession> SOCKET_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ReplyMessage> WAIT_REPLY_MESSAGE = new ConcurrentHashMap<>();

    // Websocket连接建立
    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        String uid = getUid(session);
        SOCKET_MAP.put(uid, session);
        log.info(uid + "成功建立Websocket连接");
        // 判断session中用户信息
        session.sendMessage(new TextMessage(uid + "已成功建立Websocket通信"));
        ONLINE.getAndIncrement();

    }

    private String getUid(WebSocketSession session) {
        return session.getAttributes().get("uid").toString();
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message)
            throws Exception {
        boolean ping = message.getPayload().equals("ping");
        if (ping) {
            session.sendMessage(new TextMessage("pong"));
        } else {
            if (ClientReply.maybeClientReply(message.getPayload().toString())) {

                ClientReply clientReply = ClientReply.parseJsonMsg(message.getPayload().toString());

                if (clientReply != null) {

                    String msgId = clientReply.getMsgId();
                    ReplyMessage record = WAIT_REPLY_MESSAGE.get(msgId);
                    if (record != null) {
                        record.reply();
                    }
                }
            }
        }
    }

    // 当连接出错时，主动关闭当前连接，并从会话列表中删除该会话
    @Override
    public void handleTransportError(WebSocketSession session, Throwable error)
            throws Exception {
        if (session.isOpen()) {
            session.close();
        }
        log.error("连接出现错误:" + error.toString());
        SOCKET_MAP.remove(getUid(session));
        ONLINE.decrementAndGet();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus arg1)
            throws Exception {
        log.info("Websocket连接已关闭");
        WebSocketSession remove = SOCKET_MAP.remove(getUid(session));
        if (remove != null) {
            ONLINE.decrementAndGet();
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 给所有在线用户发送消息
     *
     * @param message
     */
    public void sendMessageToUsers(TextMessage message) {

        SOCKET_MAP.forEachValue(2, webSocketSession -> {
            try {
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 发送需要确认客户端收到的msg，客户端需回传{@link ClientReply} 对象的json字符串
     * @param message {@link ReplyMessage}
     *
     * @see ReplyMessage
     * @see ClientReply
     */
    public void sendMessageToUserNeedReply(ReplyMessage<?> message) {

        WAIT_REPLY_MESSAGE.put(message.getMsgId(), message);

        Callable<Boolean> callable = () -> {
            ReplyMessage replyMessage = WAIT_REPLY_MESSAGE.get(message.getMsgId());
            if (replyMessage.isNeedSend()) {//是否需要发送消息
                WebSocketSession webSocketSession = SOCKET_MAP.get(replyMessage.getToUId());
                if (webSocketSession.isOpen()) {
                    try {
                        webSocketSession.sendMessage(replyMessage.toTextMessage());
                        replyMessage.sendSuccess();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        replyMessage.sendFail();
                    }
                } else {
                    replyMessage.sendFail();
                }
                return false;
            }
            return true;

        };

        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(obj -> Objects.isNull(obj) || obj.equals(false))
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withWaitStrategy(WaitStrategies.fibonacciWait(message.getMinWaitTime(), message.getMaxWaitTime(), TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(message.getMaxSendTimes()))
                .build();
        try {
            retryer.call(callable);
        } catch (RetryException | ExecutionException e) {
            log.warn(e.getMessage(), e);
        }
    }


    /**
     * 给某个用户发送消息
     * @param uid
     * @param message
     */
    public void sendMessageToUser(String uid, TextMessage message) {
        WebSocketSession webSocketSession = SOCKET_MAP.get(uid);
        try {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
