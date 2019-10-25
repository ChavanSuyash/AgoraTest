package io.agora.tutorials1v1vcall.base

import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import io.agora.tutorials1v1vcall.VideoChatViewActivity

import io.agora.uikit.logger.LoggerRecyclerView

class AppWebChromeClient(private val progressBar: ProgressBar, private val webLogView: LoggerRecyclerView) : WebChromeClient() {
    private var isLoadingDone = false

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        if (newProgress < 100) {
            if (!isLoadingDone) {
                showProgressBar(progressBar)
            }
        } else {
            if (newProgress == 100) {
                isLoadingDone = true
            }
            hideProgressBar(progressBar)
        }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        //do not remove ?
        webLogView?.let {
            (it.context as VideoChatViewActivity).runOnUiThread {
                consoleMessage?.let { consoleMessage ->
                    val message = "${consoleMessage.message()} - line ${consoleMessage.lineNumber()}"
                    when (consoleMessage.messageLevel()) {
                        ConsoleMessage.MessageLevel.DEBUG -> webLogView.logI(message)
                        ConsoleMessage.MessageLevel.ERROR -> webLogView.logE(message)
                        ConsoleMessage.MessageLevel.WARNING -> webLogView.logW(message)
                        else -> webLogView.logI((message))
                    }
                }
            }
        }
        return super.onConsoleMessage(consoleMessage)
    }

    private fun showProgressBar(progressBar: ProgressBar?) {
        if (progressBar != null) {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar(progressBar: ProgressBar?) {
        if (progressBar != null) {
            progressBar.visibility = View.GONE
        }
    }

}
