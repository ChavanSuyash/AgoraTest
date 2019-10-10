package io.agora.tutorials1v1vcall

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class EnterChannelIdActivity : AppCompatActivity() {

    private lateinit var etEnterChannelId: EditText
    private lateinit var btNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_channel_id)

        etEnterChannelId = findViewById(R.id.etChannelId)
        btNext = findViewById(R.id.btNext)

        btNext.setOnClickListener {
            val channelId = etEnterChannelId.text.toString()
            if(channelId.length > 1) {
                val intent = Intent(this, VideoChatViewActivity::class.java)
                val bundle = Bundle()
                bundle.putString("channelId", channelId)
                intent.putExtras(bundle)
                startActivity(intent)
                finish()
            }else {
                Toast.makeText(this,"Enter Valid Channel Id",Toast.LENGTH_SHORT).show()
            }
        }
    }

}