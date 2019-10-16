package io.agora.tutorials1v1vcall;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import java.util.UUID;

import io.agora.rtc.internal.LastmileProbeConfig;
import io.agora.tutorials1v1vcall.base.AppWebChromeClient;
import io.agora.tutorials1v1vcall.base.AppWebViewClient;
import io.agora.uikit.logger.LoggerRecyclerView;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

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
    private boolean mMuted;

    private FrameLayout mLocalContainer;
    private RelativeLayout mRemoteContainer;
    private SurfaceView mRemoteView;
    private ImageView mMuteBtn;
    private ProgressBar progressBar;
    private WebView wvWhiteBoard;
    private TextView remoteQualityTextView;
    private TextView localQualityTextView;


    // Customized logger view
    private LoggerRecyclerView mLogView;

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
                    mLogView.logI("Join channel success, uid: " + (uid & 0xFFFFFFFFL));
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("First remote video decoded, uid: " + (uid & 0xFFFFFFFFL));
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onNetworkQuality(final int uid, final int txQuality, final int rxQuality) {
            super.onNetworkQuality(uid, txQuality, rxQuality);
            Log.d("onNetworkQuality", "onNetworkQuality uid = " + uid);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (remoteQualityTextView != null && localQualityTextView != null) {
                        if (uid == 0) {
                            localQualityTextView.setText("tx : " + getReadableQuality(txQuality) + " & " + "rx " + getReadableQuality(rxQuality));
                            localQualityTextView.bringToFront();

                        } else {
                            remoteQualityTextView.setText("tx : " + getReadableQuality(txQuality) + " & " + "rx " + getReadableQuality(rxQuality));
                            remoteQualityTextView.bringToFront();


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
                    mLogView.logI("User offline, uid: " + (uid & 0xFFFFFFFFL));
                    onRemoteUserLeft();
                }
            });
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            mLogView.logI("onError = " + err);
        }

        @Override
        public void onLastmileQuality(int quality) {
            super.onLastmileQuality(quality);
            mLogView.logI("onLastmileQuality = " + quality + "(" + getReadableQuality(quality) + ")");
            switch (quality) {
                case 0://The network quality is unknown. Allow recheck.
                    if (retryCount >= 2) {
                        setVideoConfiguration(VideoEncoderConfiguration.VD_240x180, false);
                        retryCount = 0;
                        break;
                    }
                    retryCount++;
                    return;
                case 1://The network quality is excellent.
                    setVideoConfiguration(VideoEncoderConfiguration.VD_640x360, false);
                    break;
                case 4://Users can communicate only not very smoothly.
                case 5://The network is so bad that users can hardly communicate.
                    setVideoConfiguration(null, true);
                    break;
                case 6://Users cannot communicate at all.
                    mRtcEngine.disableLastmileTest();
                    showInternetDownAlert();
                    return;
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
    private int localUid;

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
        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
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
        setContentView(R.layout.activity_video_chat_view);
        int prefUid = getPreferences(MODE_PRIVATE).getInt("localUid", -1);
        if (prefUid == -1) {
            localUid = new Random().nextInt() & Integer.MAX_VALUE;
            getPreferences(MODE_PRIVATE).edit().putInt("localUid", localUid).apply();
        } else {
            localUid = prefUid;
        }
        readParams();
        initUI();
        // Ask for permissions at runtime.
        // This is just an example set of permissions. Other permissions
        // may be needed, and please refer to our online documents.
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndSetupVideo();
            performLastMileTestAndJoinChannel(channelId);
            loadWhiteBoard();
        }
        remoteQualityTextView = findViewById(R.id.remote_network_quality);
        localQualityTextView = findViewById(R.id.local_network_quality);
    }

    private void readParams() {
        channelId = getIntent().getStringExtra(CHANNEL_ID_KEY);
        if (channelId == null) throw new RuntimeException("Channel name cannot be null");
        ((TextView) findViewById(R.id.channel_name)).setText("Channel: " + channelId + " Uid: " + localUid);
    }

    private void initUI() {
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);
        mMuteBtn = findViewById(R.id.btn_mute);
        mLogView = findViewById(R.id.log_recycler_view);
        initWebView();
        // Sample logs are optional.
        showSampleLogs();
    }

    private void initWebView() {
        wvWhiteBoard = findViewById(R.id.wvWhiteBoard);
        progressBar = findViewById(R.id.progress_bar);

        WebSettings wvSettings = wvWhiteBoard.getSettings();
//        wvSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        wvSettings.setUseWideViewPort(true);
        wvSettings.setLoadWithOverviewMode(true);
        wvSettings.setJavaScriptEnabled(true);

        WebViewClient webViewClient = new AppWebViewClient(progressBar);
        wvWhiteBoard.setWebViewClient(webViewClient);
    }

    private void loadWhiteBoard() {
        wvWhiteBoard.setWebChromeClient(new AppWebChromeClient(progressBar));
        String url = "https://tutor-plus-staging.tllms.com/whiteboard/" + channelId + "/false/false/480p/true/" + localUid;
        wvWhiteBoard.loadUrl(url);
        mLogView.logI("Whiteboard Url = " + url);
    }

    private void showSampleLogs() {
        mLogView.logI("Welcome!");
        mLogView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mLogView != null) mLogView.setVisibility(View.GONE);
            }
        }, 1000);
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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

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
        setupLocalVideo();
    }

    private void performLastMileTestAndJoinChannel(String channelID) {
        int lastMileTestEnableStatus = mRtcEngine.enableLastmileTest();

        //If the method call fails joinChannel directly else handle joinChannel in onLastmileQuality callback.
        if (lastMileTestEnableStatus < 0) {
            setVideoConfiguration(VideoEncoderConfiguration.VD_240x180, false);
            joinChannel(channelID);
        }
    }

    private void setVideoConfiguration(VideoEncoderConfiguration.VideoDimensions videoDimensions, Boolean audioOnly) {
        if (audioOnly) {
            mLogView.logI("VC_Set_Audio_Only");
            mRtcEngine.disableVideo();
        } else {
            mLogView.logI("VC_Set_" + videoDimensions.height + "x" + videoDimensions.width);
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
        mLogView.logI("mkdir = " + result + "and sdkLogPath = " + sdkLogPath);
    }

    private void setupLocalVideo() {
        // This is used to set a local preview.
        // The steps setting local and remote view are very similar.
        // But note that if the local user do not have a uid or do
        // not care what the uid is,K he can set his uid as ZERO.
        // Our server will assign one and return the uid via the event
        // handler callback function (onJoinChannelSuccess) after
        // joining the channel successfully.
        mRtcEngine.enableVideo();
        SurfaceView mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        mLocalView.setZOrderMediaOverlay(false);
        mLocalContainer.addView(mLocalView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        mRtcEngine.startPreview();
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
        mMuted = !mMuted;
        mRtcEngine.muteLocalAudioStream(mMuted);
        int res = mMuted ? R.drawable.btn_mute : R.drawable.btn_unmute;
        mMuteBtn.setImageResource(res);
    }

    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    public void onCallClicked(View view) {
        finish();
    }

    public void onInfoButtonClicked(View view) {
        View logList = findViewById(R.id.log_recycler_view);
        logList.setVisibility(logList.isShown() ? View.GONE : View.VISIBLE);
    }
}
