package io.agora.tutorials1v1vcall.base;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class AppWebViewClient extends WebViewClient {

    private ProgressBar progressBar;
    private LoadingListener loadingListener;

    public AppWebViewClient(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void setLoadingListener(LoadingListener loadingListener) {
        this.loadingListener = loadingListener;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (loadingListener != null) {
            loadingListener.onPageLoaded();
        }

        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        Log.e("Error",errorCode + description);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        //Log.e("Error",Integer.toString(error.getErrorCode()) + error.getDescription());
    }

    public interface LoadingListener {
        void onPageLoaded();
    }
}
