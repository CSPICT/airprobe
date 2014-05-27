/**
 * AirProbe
 * Air quality application for Android, developed as part of 
 * EveryAware project (<http://www.everyaware.eu>).
 *
 * Copyright (C) 2014 CSP Innovazione nelle ICT. All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * For any inquiry please write to <devel@csp.it>
 * 
 * CONTRIBUTORS
 * 
 * This program was made with the contribution of:
 *   Fabio Saracino <fabio.saracino@csp.it>
 *   Patrick Facco <patrick.facco@csp.it>
 * 
 * 
 * SOURCE CODE
 * 
 *  The source code of this program is available at
 *  <https://github.com/CSPICT/airprobe>
 */

package org.csp.everyaware.internet;


import org.csp.everyaware.Constants;
import org.csp.everyaware.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TwitterLogin extends Activity 
{
    public static final String TAG = TwitterLogin.class.getSimpleName();

    protected void onCreate(Bundle bundle) 
    {
        super.onCreate(bundle);
        setContentView(R.layout.twitter_login);

        WebView webView = (WebView) findViewById(R.id.twitterlogin);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() 
        {
            public boolean shouldOverrideUrlLoading(WebView view, String url) 
            {
                boolean result = true;
                if (url != null && url.startsWith(Constants.CALLBACK_URL)) 
                {
                    Uri uri = Uri.parse(url);
                    Log.v(TAG, url);
                    if (uri.getQueryParameter("denied") != null) {
                        setResult(RESULT_CANCELED);
                        finish();
                    } 
                    else 
                    {
                        String oauthToken = uri.getQueryParameter("oauth_token");
                        String oauthVerifier = uri.getQueryParameter("oauth_verifier");

                        Intent intent = getIntent();
                        intent.putExtra(Constants.IEXTRA_OAUTH_TOKEN, oauthToken);
                        intent.putExtra(Constants.IEXTRA_OAUTH_VERIFIER, oauthVerifier);

                        setResult(RESULT_OK, intent);
                        finish();
                    }
                } 
                else 
                {
                    result = super.shouldOverrideUrlLoading(view, url);
                }
                return result;
            }
        });
        webView.loadUrl(this.getIntent().getExtras().getString("auth_url"));
    }
}