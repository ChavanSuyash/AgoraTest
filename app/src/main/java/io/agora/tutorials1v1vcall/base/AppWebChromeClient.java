package io.agora.tutorials1v1vcall.base;

import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class AppWebChromeClient extends WebChromeClient {

    private ProgressBar progressBar;
    private boolean isLoadingDone = false;

    public AppWebChromeClient(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        if (newProgress < 100) {
            if (!isLoadingDone) {
                showProgressBar(progressBar);
            }
        } else {
            if (newProgress == 100) {
                isLoadingDone = true;
            }
            hideProgressBar(progressBar);
        }
        super.onProgressChanged(view, newProgress);
    }

    private void showProgressBar(ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar(ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

}
