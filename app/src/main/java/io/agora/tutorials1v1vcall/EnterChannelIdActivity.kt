package io.agora.tutorials1v1vcall

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import io.agora.rtc.RtcEngine
import io.agora.tutorials1v1vcall.VideoChatViewActivity.*
import kotlinx.android.synthetic.main.activity_enter_channel_id.*
import android.widget.EditText
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.util.Base64
import android.view.SubMenu

class EnterChannelIdActivity : AppCompatActivity() {

    private var subMenu: SubMenu? = null
    private var mode: Int = COMMUNICATION_MODE
    private val sdkVersion: String by lazy { "SDK v ${RtcEngine.getSdkVersion()}" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_channel_id)

        version_tv.text = sdkVersion

        setSupportActionBar(toolbar)
        title = ""

        radio_group.setOnCheckedChangeListener { _, checkedId ->
            mode = when (checkedId) {
                R.id.onetoone -> COMMUNICATION_MODE.also { version_tv.text = sdkVersion }
                R.id.onetomany -> ONE_TO_MANY_BYJUS.also { version_tv.text = sdkVersion }
                else -> mode
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        subMenu = menu.addSubMenu(0, 0, 0, "Advance Mode") //If you want to add submenu
        return true
    }

    private var isAgoraUser: Boolean = false
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isAgoraUser) {
            showAgoraAuthLogin()
            return false
        }
        mode = when (item.itemId) {
            1 -> HOST_MODE.also { radio_group.clearCheck() }.also { version_tv.text = "Host Mode - $sdkVersion" }
            2 -> AUDIENCE_MODE.also { radio_group.clearCheck() }.also { version_tv.text = "Audience Mode - $sdkVersion" }
            3 -> WHITE_BOARD_ONLY.also { radio_group.clearCheck() }.also { version_tv.text = "Whiteboard Only Mode - $sdkVersion" }
            else -> mode
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAgoraAuthLogin() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Dev mode")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Enter Password"
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ -> validatePassword(input.text.toString()) }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    private fun validatePassword(text: String) {
        if (text.encode() == "YWdvcmF0b255") {
            subMenu?.add(0, 1, 0, "Host")
            subMenu?.add(0, 2, 0, "Audience")
            subMenu?.add(0, 3, 0, "Whiteboard Only")
            isAgoraUser = true
            Toast.makeText(this, "Dev mode enable. Try Advance Mode now", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Direct ref from XML
     */
    fun joinChannelOnClick(view: View) {
        if (mode == 0) {
            Toast.makeText(this, "Please select the mode", Toast.LENGTH_SHORT).show()
            return
        }
        val channelName = channel_et.text.toString()
        if (channelName.isNotBlank()) {
            startActivity(Intent(this, VideoChatViewActivity::class.java).apply {
                putExtra(CHANNEL_ID_KEY, channelName)
                putExtra(SESSION_MODE, mode)
            })
        } else {
            Toast.makeText(this, "Channel name cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun String.encode(): String {
        return Base64.encodeToString(this.toByteArray(charset("UTF-8")), Base64.NO_WRAP)
    }

    fun launchNetworkTestActivity(view: View) {
        startActivity(Intent(this, NetworkTestActivity::class.java))
    }

}