package com.xxf.arch.websocket.listener;

import okhttp3.Response;
import okio.ByteString;

/**
 * @Description: websocket 回调监听
 * @Author: XGod
 * @CreateDate: 2020/7/23 15:20
 */
public interface WsStatusListener {

    void onOpen(Response response);

    void onMessage(String text);

    void onMessage(ByteString bytes);

    void onReconnect();

    void onClosing(int code, String reason);


    void onClosed(int code, String reason);

    void onFailure(Throwable t, Response response);
}
