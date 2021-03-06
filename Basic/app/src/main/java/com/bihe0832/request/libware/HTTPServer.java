package com.bihe0832.request.libware;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.net.HttpURLConnection;

public class HTTPServer {
    private static volatile HTTPServer instance;
    private static final String LOG_TAG = "bihe0832 REQUEST";

    //是否为测试版本
    private static final boolean DEBUG = true;
    private Handler mCallHandler;
    private static final int MSG_REQUEST = 0;

    private HandlerThread mRequestHandlerThread = null;


    public static HTTPServer getInstance () {
        if (instance == null) {
            synchronized (HTTPServer.class) {
                if (instance == null) {
                    instance = new HTTPServer();
                    instance.init();
                }
            }
        }
        return instance;
    }

    private HTTPServer() {}

    public void init () {

        mRequestHandlerThread = new HandlerThread("HTTPServer");
        mRequestHandlerThread.start();
        mCallHandler = new Handler(mRequestHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_REQUEST:
                        if(msg.obj != null && msg.obj instanceof HttpRequest){
                            executeRequestInExecutor((HttpRequest)msg.obj);
                        }else{
                            Log.d(LOG_TAG,msg.toString());
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    
    public void doRequest(HttpRequest request) {
        Message msg = mCallHandler.obtainMessage();
        msg.what = MSG_REQUEST;
        msg.obj = request;
        mCallHandler.sendMessage(msg);
    }

    private void executeRequestInExecutor(HttpRequest request){
        request.requestTime = System.currentTimeMillis() / 1000;

        String url = request.getUrl();
        if(DEBUG){
            Log.w(LOG_TAG,"=======================================");
            Log.w(LOG_TAG,request.getClass().toString());
            Log.w(LOG_TAG,url);
            Log.w(LOG_TAG,"=======================================");
        }
        BaseConnection connection = null;
        if(url.startsWith("https:")){
            connection = new HTTPSConnection(url);
        }else{
            connection = new HTTPConnection(url);
        }

        String result = connection.doRequest(request);
        if(DEBUG){
            Log.w(LOG_TAG,"=======================================");
            Log.w(LOG_TAG,request.getClass().toString());
            Log.w(LOG_TAG,result);
            Log.w(LOG_TAG, String.valueOf(connection.getResponseCode()));
            Log.w(LOG_TAG,connection.getResponseMessage());
            Log.w(LOG_TAG,"=======================================");
        }

        if(connection.getResponseCode() == HttpURLConnection.HTTP_OK){
            request.mHttpResponseHandler.onResponse(connection.getResponseCode(), result);
        }else{
            if (TextUtils.ckIsEmpty(result)) {
                if(DEBUG) {
                    Log.e(LOG_TAG, request.getClass().getName());
                }
                Log.e(LOG_TAG,"responseBody is null");
                if(TextUtils.ckIsEmpty(connection.getResponseMessage())){
                    request.mHttpResponseHandler.onResponse(connection.getResponseCode(), "");
                }else{
                    request.mHttpResponseHandler.onResponse(connection.getResponseCode(),connection.getResponseMessage());
                }
            } else {
                request.mHttpResponseHandler.onResponse(connection.getResponseCode(), result);
            }
        }
    }
}
