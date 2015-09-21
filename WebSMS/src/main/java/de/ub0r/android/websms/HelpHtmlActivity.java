package de.ub0r.android.websms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

/**
 * Activity for displaying html help pages.
 */
public class HelpHtmlActivity extends AppCompatActivity {

    private static final String INTENT_TITLE = "title";
    private static final String INTENT_CONTENT   = "content";

    public static Intent createStartIntent(Context appCtx, String title, String content) {
        Intent intent = new Intent(appCtx, HelpHtmlActivity.class);
        intent.putExtra(INTENT_TITLE, title);
        intent.putExtra(INTENT_CONTENT, content);
        return intent;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.help_html);
        this.setTitle(getIntent().getStringExtra(INTENT_TITLE));

        WebView webView = (WebView) findViewById(R.id.help_content);
        webView.loadData(getIntent().getStringExtra(INTENT_CONTENT),
                "text/html; charset=utf-8", "utf-8");
    }

}
