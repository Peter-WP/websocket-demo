package com.example.websocketdemo.socket;

/**
 * @author peter
 * date: 2019-05-08 11:41
 **/
public enum IdWorker {
    ;

    public static final SnowflakeIdWorker worker = new SnowflakeIdWorker(-1, -1);

    public static long getId() {
        return worker.nextId();
    }

}
