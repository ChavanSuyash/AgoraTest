package io.agora.tutorials1v1vcall.base;

import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.tutorials1v1vcall.R;
import io.agora.uikit.logger.BuildConfig;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;

public class ChatManager {
    private static final String TAG = ChatManager.class.getSimpleName();

    private Context mContext;
    private RtmClient mRtmClient;
    private List<RtmClientListener> mListenerList = new ArrayList<>();

    public ChatManager(Context context) {
        mContext = context;
    }

    public void init() {
        String appID = mContext.getString(R.string.agora_app_id);

        try {
            mRtmClient = RtmClient.createInstance(mContext, appID, new RtmClientListener() {
                @Override
                public void onConnectionStateChanged(int state, int reason) {
                    for (RtmClientListener listener : mListenerList) {
                        listener.onConnectionStateChanged(state, reason);
                    }
                }

                @Override
                public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                }

                @Override
                public void onTokenExpired() {
                }

                @Override
                public void onPeersOnlineStatusChanged(Map<String, Integer> var1) {
                }
            });

            if (BuildConfig.DEBUG) {
//                mRtmClient.setParameters("{\"rtm.log_filter\": 65535}");
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtm sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    public void registerListener(RtmClientListener listener) {
        mListenerList.add(listener);
    }

    public void unregisterListener(RtmClientListener listener) {
        mListenerList.remove(listener);
    }

    public RtmClient getRtmClient() {
        return mRtmClient;
    }

    public void destroy() {
        mRtmClient.logout(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mRtmClient.release();
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                mRtmClient.release();
            }
        });
    }
}
