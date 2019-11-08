package io.agora.tutorials1v1vcall;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.internal.LastmileProbeConfig;
import io.agora.tutorials1v1vcall.model.ConstantApp;

public class NetworkTestActivity extends AppCompatActivity {
    private RtcEngine mRtcEngine;
    private static final String TAG = "NetworkTestActivity";
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onLastmileQuality(final int quality) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String qualityInString = ConstantApp.getNetworkQualityDescription(quality);
                    String networkResult = "onLastmileQuality quality: " + qualityInString;
                    Log.d(TAG, "networkResult = " + networkResult);
                    updateNetworkTestResult(qualityInString);
                }
            });
        }

        @Override
        public void onLastmileProbeResult(final IRtcEngineEventHandler.LastmileProbeResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String networkResult =
                            "onLastmileProbeResult state: " + result.state + " " + "rtt: " + result.rtt + "\n" +
                                    "uplinkReport { packetLossRate: " + result.uplinkReport.packetLossRate + " " +
                                    "jitter: " + result.uplinkReport.jitter + " " +
                                    "availableBandwidth: " + result.uplinkReport.availableBandwidth + "}" + "\n" +
                                    "downlinkReport { packetLossRate: " + result.downlinkReport.packetLossRate + " " +
                                    "jitter: " + result.downlinkReport.jitter + " " +
                                    "availableBandwidth: " + result.downlinkReport.availableBandwidth + "}";
                    Log.d(TAG, "networkResult = " + networkResult);
                    updateNetworkTestResult(result.rtt, result.uplinkReport.packetLossRate, result.downlinkReport.packetLossRate);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_test);

        initializeEngine();
        initUIandEvent();
    }


    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    protected void initUIandEvent() {

//        ((TextView) findViewById(R.id.ovc_page_title)).setText(R.string.label_network_testing);

        LastmileProbeConfig lastmileProbeConfig = new LastmileProbeConfig();
        lastmileProbeConfig.probeUplink = true;
        lastmileProbeConfig.probeDownlink = true;
        lastmileProbeConfig.expectedUplinkBitrate = 700;//For 360p live mode
        lastmileProbeConfig.expectedDownlinkBitrate = 700;//For 360p live mode
        mRtcEngine.startLastmileProbeTest(lastmileProbeConfig);
    }

    protected void deInitUIandEvent() {
        mRtcEngine.stopLastmileProbeTest();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        deInitUIandEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deInitUIandEvent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void updateNetworkTestResult(String qualityInString) {

        ((TextView) findViewById(R.id.network_test_quality)).setText(qualityInString);
    }

    public void updateNetworkTestResult(int rtt, int uplinkPacketLoss, int downlinkPacketLoss) {

        ((TextView) findViewById(R.id.network_test_rtt)).setText(rtt + "ms");

        ((TextView) findViewById(R.id.network_test_uplink_packet_loss)).setText(uplinkPacketLoss + "%");

        ((TextView) findViewById(R.id.network_test_downlink_packet_loss)).setText(downlinkPacketLoss + "%");
    }

    public void finishTest(View view) {
        onBackPressed();
    }
}