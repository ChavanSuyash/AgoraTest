package io.agora.tutorials1v1vcall;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Random;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.tutorials1v1vcall.base.AppWebChromeClient;
import io.agora.tutorials1v1vcall.base.AppWebViewClient;
import io.agora.uikit.logger.LoggerRecyclerView;

import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_DECODING;
import static io.agora.rtc.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;

public class VideoChatViewActivity extends AppCompatActivity {
    private static final String TAG = VideoChatViewActivity.class.getSimpleName();
    public static final String CHANNEL_ID_KEY = "CHANNEL_NAME";


    private static final int PERMISSION_REQ_ID = 22;

    // Permission WRITE_EXTERNAL_STORAGE is not mandatory
    // for Agora RTC SDK, just in case if you wanna save
    // logs to external sdcard.
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private RtcEngine mRtcEngine;
    private boolean mAudioMuted;
    private boolean mVideoMuted;

    private FrameLayout mLocalContainer;
    private RelativeLayout mRemoteContainer;
    private SurfaceView mRemoteView;
    private ImageView mMuteAudioBtn, mMuteVideoBtn;
    private ProgressBar progressBar;
    private WebView webView;
    private TextView remoteQualityTextView;
    private TextView localQualityTextView;
    private int localUid;
    public static final int COMMUNICATION_MODE = 100;
    public static final int AUDIENCE_MODE = 300;
    public static final int HOST_MODE = 400;
    public static final int WHITE_BOARD_ONLY = 500;
    public static final int ONE_TO_MANY_BYJUS = 600;
    public static final String SESSION_MODE = "SESSION_MODE";
    private int sessionMode;


    // Customized logger view
    private LoggerRecyclerView appLogView, webLogView;

    String channelId;
    int retryCount = 0;


    /**
     * Event handler registered into RTC engine for RTC callbacks.
     * Note that UI operations needs to be in UI thread because RTC
     * engine deals with the events in a separate thread.
     */
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appLogView.logI("Join channel success, uid: " + (uid & 0xFFFFFFFFL));
                }
            });
        }


        @Override
        public void onRemoteVideoStateChanged(final int uid, int state, int reason, int elapsed) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
            //If UID ends with 999 we assume it is teacher
            String uidString = String.valueOf(uid).trim();
            boolean isTeacherUid = uidString.substring(uidString.length() - 3).equalsIgnoreCase("999");
            if (!isTeacherUid) return;

            if (state == REMOTE_VIDEO_STATE_DECODING) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appLogView.logI("First remote video decoded, uid: " + (uid & 0xFFFFFFFFL));
                        if (isBroadcastHostMode())
                            appLogView.logE("Two or more users joined with role Broadcaster. Invalid use case");
                        setupRemoteVideo(uid);
                    }
                });
            }
        }

        @Override
        public void onUserMuteAudio(int uid, boolean muted) {
            super.onUserMuteAudio(uid, muted);
            setupRemoteVideo(uid);
        }

        @Override
        public void onNetworkQuality(final int uid, final int txQuality, final int rxQuality) {
            super.onNetworkQuality(uid, txQuality, rxQuality);
            Log.d("onNetworkQuality", "onNetworkQuality uid = " + uid);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (remoteQualityTextView != null && localQualityTextView != null) {
                        String qualityText = "tx: " + txQuality + "(" + getReadableQuality(txQuality) + ") & rx: " + txQuality + "(" + getReadableQuality(rxQuality) + ")";
                        Log.d(TAG, "quality " + qualityText);
                        if (uid == 0) {

                            if (!localQualityTextView.getText().toString().trim().equals(qualityText.trim())) {
                                localQualityTextView.setText(qualityText);
                                localQualityTextView.bringToFront();
                            }
                            if (mVideoMuted) {
                                localQualityTextView.setText(localQualityTextView.getText() + " - Local video is disabled");
                            }
                        } else {
                            if (!remoteQualityTextView.getText().toString().trim().equals(qualityText.trim())) {
                                remoteQualityTextView.setText(qualityText);
                                remoteQualityTextView.bringToFront();
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onLastmileProbeResult(LastmileProbeResult result) {
            super.onLastmileProbeResult(result);
        }

        @Override
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appLogView.logI("User offline, uid: " + (uid & 0xFFFFFFFFL));
                    onRemoteUserLeft();
                }
            });
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            appLogView.logI("onError = " + err);
        }

        @Override
        public void onLastmileQuality(int quality) {
            super.onLastmileQuality(quality);
            appLogView.logI("onLastmileQuality = " + quality + "(" + getReadableQuality(quality) + ")");
            switch (quality) {
                case 0://The network quality is unknown. Allow recheck.
                    if (retryCount >= 2) {
                        setVideoConfiguration(VideoEncoderConfiguration.VD_240x180, false);
                        retryCount = 0;
                        break;
                    }
                    retryCount++;
                    return;
                case 4://Users can communicate only not very smoothly.
                case 5://The network is so bad that users can hardly communicate.
                    setVideoConfiguration(null, true);
                    break;
                case 6://Users cannot communicate at all.
                    mRtcEngine.disableLastmileTest();
                    showInternetDownAlert();
                    return;
                case 1://The network quality is excellent.
                case 2://The network quality is quite good, but the bitrate may be slightly lower than excellent.
                case 3://Users can feel the communication slightly impaired.
                default://If it returns something else.
                    setVideoConfiguration(VideoEncoderConfiguration.VD_240x180, false);
                    break;
            }
            mRtcEngine.disableLastmileTest();
            joinChannel(channelId);
        }
    };

    public static String getReadableQuality(int value) {
        switch (value) {
            case 1:
                return "Excellent";
            case 2:
                return "Good";
            case 3:
                return "Poor";
            case 4:
                return "Bad";
            case 5:
                return "Very Bad";
            case 6:
                return "Down";
            default:
                return "Unknown";
        }
    }

    private void showInternetDownAlert() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(VideoChatViewActivity.this).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Your internet is down, please check the connection and try again.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                VideoChatViewActivity.this.finish();
                            }
                        });
                alertDialog.show();
            }
        });
    }

    private void setupRemoteVideo(int uid) {
        // Only one remote video view is available for this
        // tutorial. Here we check if there exists a surface
        // view tagged as this uid.
        int count = mRemoteContainer.getChildCount();
        View view = null;
        for (int i = 0; i < count; i++) {
            View v = mRemoteContainer.getChildAt(i);
            if (v.getTag() instanceof Integer && ((int) v.getTag()) == uid) {
                view = v;
            }
        }

        if (view != null) {
            return;
        }

        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        mRemoteContainer.addView(mRemoteView);
        mRtcEngine.setupRemoteVideo(
                new VideoCanvas(mRemoteView, isBroadcastAudienceMode() ? VideoCanvas.RENDER_MODE_FIT : VideoCanvas.RENDER_MODE_HIDDEN, uid));
        mRemoteView.setTag(uid);
    }

    private void onRemoteUserLeft() {
        removeRemoteVideo();
    }

    private void removeRemoteVideo() {
        if (mRemoteView != null) {
            mRemoteContainer.removeView(mRemoteView);
        }
        mRemoteView = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionMode = getIntent().getIntExtra(SESSION_MODE, 100);
        setContentView(R.layout.activity_video_chat_view);
        int prefUid = getPreferences(MODE_PRIVATE).getInt("localUid", -1);
        if (prefUid == -1) {
            localUid = new Random().nextInt() & Integer.MAX_VALUE;
            getPreferences(MODE_PRIVATE).edit().putInt("localUid", localUid).apply();
        } else {
            localUid = prefUid;
        }

        //Start-Just to act as teacher in host mode (Temp)
        if (isBroadcastHostMode()) {
            String localUidString = String.valueOf(localUid);
            String localUidStringSubString = localUidString.substring(0, localUidString.length() - 3);
            localUid = Integer.parseInt(localUidStringSubString + "999");
        }
        //End

        readParams();
        initUI();
        loadWhiteBoard();

        if (isOneToMany()) {
            mAudioMuted = true;
            mVideoMuted = true;

            mMuteAudioBtn.setImageResource(R.drawable.btn_mute);
            mMuteVideoBtn.setImageResource(R.drawable.btn_vid_mute);
        }

        if (isWhiteBoardOnlyMode()) {
            findViewById(R.id.parent_video_view_container).setVisibility(View.GONE);
            return;
        }

        // Ask for permissions at runtime.
        // This is just an example set of permissions. Other permissions
        // may be needed, and please refer to our online documents.
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndSetupVideo();
            performLastMileTestAndJoinChannel(channelId);
            //loadWhiteBoard();
        }
    }

    private void readParams() {
        channelId = getIntent().getStringExtra(CHANNEL_ID_KEY);
        if (channelId == null) throw new RuntimeException("Channel name cannot be null");
        ((TextView) findViewById(R.id.channel_name)).setText("Channel: " + channelId + " Uid: " + localUid + "\n" + getReadableMode());
    }

    private void initUI() {
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);
        mMuteAudioBtn = findViewById(R.id.btn_mute);
        mMuteVideoBtn = findViewById(R.id.btn_vid_mute);
        appLogView = findViewById(R.id.app_log_recycler_view);
        webLogView = findViewById(R.id.web_log_recycler_view);
        remoteQualityTextView = findViewById(R.id.remote_network_quality);
        localQualityTextView = findViewById(R.id.local_network_quality);
        initWebView();
        // Sample logs are optional.
        showSampleLogs();
    }

    private void initWebView() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progress_bar);

        WebSettings wvSettings = webView.getSettings();

        wvSettings.setJavaScriptEnabled(true);
        wvSettings.setLoadWithOverviewMode(true);
        wvSettings.setUseWideViewPort(true);

        wvSettings.setSupportZoom(true);
        wvSettings.setBuiltInZoomControls(true);
        wvSettings.setDisplayZoomControls(false);

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);

        WebViewClient webViewClient = new AppWebViewClient(progressBar);


        //turn on debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webView.setWebViewClient(webViewClient);
    }

    private void loadWhiteBoard() {
        webView.setWebChromeClient(new AppWebChromeClient(progressBar, webLogView));
        String url = "https://tutor-plus-staging.tllms.com/whiteboard/" + channelId
                + "/false/false/480p/true/" + localUid + "/" + (isBroadcastMode() ? "live" : "rtc");
        webView.loadUrl(url);
        appLogView.logI("Whiteboard Url = " + url);
    }

    private void showSampleLogs() {
        appLogView.logI("App logs will be streamed here");
        appLogView.logI("Session mode = " + getReadableMode());
        webLogView.logW("Web logs will be streamed here");
        appLogView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (appLogView != null) appLogView.setVisibility(View.GONE);
                if (webLogView != null) webLogView.setVisibility(View.GONE);
            }
        }, 500);
    }

    private String getReadableMode() {
        switch (sessionMode) {
            case COMMUNICATION_MODE:
                return "1to1 Mode";
            case HOST_MODE:
                return "Broadcast Mode - Host Role";
            case AUDIENCE_MODE:
                return "Broadcast Mode - Audience Role";
            case WHITE_BOARD_ONLY:
                return "Whiteboard Only Mode";
            case ONE_TO_MANY_BYJUS:
                return "1 to Many Mode";
            default:
                return "Unknown";
        }
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                showLongToast("Need permissions " + Manifest.permission.RECORD_AUDIO +
                        "/" + Manifest.permission.CAMERA + "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                finish();
                return;
            }

            // Here we continue only if all permissions are granted.
            // The permissions can also be granted in the system settings manually.
            initEngineAndSetupVideo();
            performLastMileTestAndJoinChannel(channelId);
        }
    }

    private void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initEngineAndSetupVideo() {
        // This is our usual steps for joining
        // a channel and starting a call.
        initializeEngine();
        mRtcEngine.enableVideo();
        setupLocalVideo();

        if (isOneToMany()) {
            mRtcEngine.muteLocalVideoStream(true);
            mRtcEngine.muteLocalAudioStream(true);
            mRtcEngine.stopPreview();
            mRtcEngine.enableLocalVideo(false);
        } else {
            mRtcEngine.startPreview();
        }
    }

    private void performLastMileTestAndJoinChannel(String channelID) {
        if (isBroadcastAudienceMode()) {
            joinChannel(channelID);
            return;
        }
        int lastMileTestEnableStatus = mRtcEngine.enableLastmileTest();

        //If the method call fails joinChannel directly else handle joinChannel in onLastmileQuality callback.
        if (lastMileTestEnableStatus < 0) {
            setVideoConfiguration(VideoEncoderConfiguration.VD_240x180, false);
            joinChannel(channelID);
        }
    }

    private void setVideoConfiguration(VideoEncoderConfiguration.VideoDimensions
                                               videoDimensions, Boolean audioOnly) {
        if (audioOnly) {
            appLogView.logI("VC_Set_Audio_Only");
            mRtcEngine.disableVideo();
        } else {
            appLogView.logI("VC_Set_" + videoDimensions.height + "x" + videoDimensions.width);
            mRtcEngine.setParameters("{\"che.extSmoothMode\": true} ");
            VideoEncoderConfiguration config =
                    new VideoEncoderConfiguration(videoDimensions,
                            VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                            VideoEncoderConfiguration.STANDARD_BITRATE,
                            ORIENTATION_MODE_ADAPTIVE);
            mRtcEngine.setVideoEncoderConfiguration(config);
        }
    }

    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
            setAgoraSDKLogPath();
            if (isBroadcastMode() || isOneToMany()) {
                mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
                mRtcEngine.enableWebSdkInteroperability(true);
                mRtcEngine.setRemoteSubscribeFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY);
                mRtcEngine.setLocalPublishFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY);
            }
            if (isBroadcastHostMode() || isOneToMany())
                mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            else if (isBroadcastAudienceMode())
                mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setAgoraSDKLogPath() {
        String sdkLogPath = Environment.getExternalStorageDirectory().toString() + "/" + getPackageName() + "/";
        File sdkLogDir = new File(sdkLogPath);
        Boolean result = sdkLogDir.mkdirs();
        mRtcEngine.setLogFile(sdkLogPath);
        appLogView.logI("mkdir = " + result + "and sdkLogPath = " + sdkLogPath);
    }

    private void setupLocalVideo() {
        if (isBroadcastAudienceMode()) {
            findViewById(R.id.local_video_parent).setVisibility(View.GONE);
            return;
        } else if (isBroadcastHostMode()) {
            findViewById(R.id.remote_video_parent).setVisibility(View.GONE);
        }
        // This is used to set a local preview.
        // The steps setting local and remote view are very similar.
        // But note that if the local user do not have a uid or do
        // not care what the uid is,K he can set his uid as ZERO.
        // Our server will assign one and return the uid via the event
        // handler callback function (onJoinChannelSuccess) after
        // joining the channel successfully.
        SurfaceView mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        mLocalView.setZOrderMediaOverlay(false);
        mLocalContainer.addView(mLocalView);
        mRtcEngine.setupLocalVideo(
                new VideoCanvas(mLocalView, isBroadcastHostMode() ? VideoCanvas.RENDER_MODE_FIT : VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void joinChannel(String channelID) {
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name that
        // you use to generate this token.
        String token = getString(R.string.agora_access_token);
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null; // default, no token
        }
        mRtcEngine.joinChannel(null, channelID, "Extra Optional Data", localUid);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveChannel();
        RtcEngine.destroy();
    }

    private void leaveChannel() {
        if (mRtcEngine != null)
            mRtcEngine.leaveChannel();
    }

    public void onLocalAudioMuteClicked(View view) {
        mAudioMuted = !mAudioMuted;
        mRtcEngine.muteLocalAudioStream(mAudioMuted);
        int res = mAudioMuted ? R.drawable.btn_mute : R.drawable.btn_unmute;
        mMuteAudioBtn.setImageResource(res);
    }

    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    public void onCallClicked(View view) {
        finish();
    }

    public void onAppInfoButtonClicked(View view) {
        appLogView.setVisibility(appLogView.isShown() ? View.GONE : View.VISIBLE);
        if (appLogView.isShown()) {
            webLogView.setVisibility(View.GONE);
        }
    }

    public void onWebInfoButtonClicked(View view) {
        webLogView.setVisibility(webLogView.isShown() ? View.GONE : View.VISIBLE);
        if (webLogView.isShown()) {
            appLogView.setVisibility(View.GONE);
        }
    }

    public Boolean isCommunicationMode() {
        return sessionMode == COMMUNICATION_MODE;
    }

    public Boolean isBroadcastMode() {
        return sessionMode == HOST_MODE || sessionMode == AUDIENCE_MODE;
    }

    public Boolean isBroadcastAudienceMode() {
        return sessionMode == AUDIENCE_MODE;
    }

    public Boolean isBroadcastHostMode() {
        return sessionMode == HOST_MODE;
    }

    public Boolean isWhiteBoardOnlyMode() {
        return sessionMode == WHITE_BOARD_ONLY;
    }

    public Boolean isOneToMany() {
        return sessionMode == ONE_TO_MANY_BYJUS;
    }

    public void onLocalVideoMuteClicked(View view) {
        mVideoMuted = !mVideoMuted;
        mRtcEngine.muteLocalVideoStream(mVideoMuted);
        int res = mVideoMuted ? R.drawable.btn_vid_mute : R.drawable.btn_vid_unmute;
        mMuteVideoBtn.setImageResource(res);
        mRtcEngine.enableLocalVideo(!mVideoMuted);
        localQualityTextView.setText("Local video is disabled");
    }
}
