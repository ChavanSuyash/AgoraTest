package io.agora.tutorials1v1vcall.base

import android.graphics.Bitmap
import android.os.Build
import android.support.annotation.RequiresApi
import android.webkit.*
import io.agora.tutorials1v1vcall.VideoChatViewActivity
import io.agora.uikit.logger.LoggerRecyclerView

class AppWebViewClient(private val loadingListener: LoadingListener) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        loadingListener.onWebViewPageLoadingStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        loadingListener.onWebViewPageLoadingFinished()
    }

    @Suppress("DEPRECATION")
    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        loadingListener.onWebViewPageLoadingFailed("Webpage loading filed. " +
                "Error code = $errorCode and " +
                "description = $description")
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        loadingListener.onWebViewPageLoadingFailed("Webpage loading filed. " +
                "Error code = ${error?.errorCode} and " +
                "description = ${error?.description}")
    }

    interface LoadingListener {
        fun onWebViewPageLoadingStarted()
        fun onWebViewPageLoadingFinished()
        fun onWebViewPageLoadingFailed(errorMsg: String)
    }
}

class AppWebChromeClient(private val webLogView: LoggerRecyclerView) : WebChromeClient() {

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
}
