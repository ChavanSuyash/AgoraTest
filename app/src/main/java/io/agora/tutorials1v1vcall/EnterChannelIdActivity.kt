package io.agora.tutorials1v1vcall

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class EnterChannelIdActivity : AppCompatActivity() {

    lateinit var etEnterChannelId: EditText
    lateinit var btNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_channel_id)

        etEnterChannelId = findViewById<EditText>(R.id.etChannelId)
        btNext = findViewById<Button>(R.id.btNext)

        btNext.setOnClickListener {
            if(etEnterChannelId.text.toString().length > 1) {
                //TODO - Pass the channel id to VideoChatViewActivity
            }else {
                Toast.makeText(this,"Enter Valid Channel Id",Toast.LENGTH_SHORT).show()
            }
        }
    }

}