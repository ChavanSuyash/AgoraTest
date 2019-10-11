package io.agora.tutorials1v1vcall

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import io.agora.rtc.RtcEngine
import io.agora.tutorials1v1vcall.VideoChatViewActivity.CHANNEL_ID_KEY
import kotlinx.android.synthetic.main.activity_enter_channel_id.*

class EnterChannelIdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_channel_id)

        val sdkVersion = "SDK v ${RtcEngine.getSdkVersion()}"
        version_tv.text = sdkVersion
    }

    /**
     * Direct ref from XML
     */
    fun joinChannelOnClick(view: View) {
        val channelName = channel_et.text.toString()
        if (channelName.isNotBlank()) {
            startActivity(Intent(this,
                    VideoChatViewActivity::class.java).apply { putExtra(CHANNEL_ID_KEY, channelName) })
        } else {
            Toast.makeText(this, "Channel name cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }
}