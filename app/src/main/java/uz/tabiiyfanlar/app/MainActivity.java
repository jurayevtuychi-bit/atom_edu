package uz.tabiiyfanlar.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * https://www.tabiiy-fanlar.uz/ saytini ilova ichida ochuvchi WebView ilovasi.
 * Sayt menyulari yuqorida kichik tugmalar ko'rinishida ko'rsatiladi.
 */
public class MainActivity extends Activity {

    private static final String BASE_URL = "https://www.tabiiy-fanlar.uz";
    private static final String HOME_URL = BASE_URL + "/bosh-sahifa";

    // Shu domenlar (va ularning subdomenlari) ilova ichida ochiladi.
    // Qolgan barcha havolalar telefondagi brauzerda ochiladi.
    private static final String[] INTERNAL_HOSTS = {
            "tabiiy-fanlar.uz",
            "sites.google.com",
            "googleusercontent.com",
            "docs.google.com"
    };

    /** Menyu bandi: nomi, sayt ichidagi yo'li va (bo'lsa) ichki bo'limlari. */
    private static final class Item {
        final String title;
        final String path;
        final Item[] children;

        Item(String title, String path, Item... children) {
            this.title = title;
            this.path = path;
            this.children = children;
        }
    }

    // Menyu tuzilishi saytdagi menyuga mos. O'zgartirish kerak bo'lsa, shu yerni tahrirlang.
    private static final Item[] MENU = {
            new Item("Bosh sahifa", "/bosh-sahifa"),
            new Item("Tadqiqotchi", "/tadqiqotchi",
                    new Item("Darslik", "/tadqiqotchi/darslik"),
                    new Item("Video darslik", "/tadqiqotchi/video-darslik"),
                    new Item("Qiziqarli topshiriqlar", "/tadqiqotchi/qiziqarli-topshiriqlar"),
                    new Item("Krossvordlar", "/tadqiqotchi/krossvordlar"),
                    new Item("Ijodiy topshiriqlar", "/tadqiqotchi/ijodiy-topshiriqlar"),
                    new Item("Viktorina", "/tadqiqotchi/viktorina")),
            new Item("Kreativ dars", "/kreativ-dars"),
            new Item("Virtual amaliyot", "/virtual-amaliyot",
                    new Item("Simulyatsion amaliyot", "/virtual-amaliyot/simulyatsion-amaliyot")),
            new Item("Motiv", "/motiv"),
            new Item("Bilimni baholash", "/bilimni-baholash"),
            new Item("Amaliy mashg'ulot", "/amaliy-mashgulot")
    };

    private WebView webView;
    private ProgressBar progressBar;
    private View errorView;
    private FrameLayout fullscreenContainer;

    private HorizontalScrollView menuScroll;
    private HorizontalScrollView subMenuScroll;
    private LinearLayout menuContainer;
    private LinearLayout subMenuContainer;
    private final List<TextView> topChips = new ArrayList<>();
    private final List<TextView> subChips = new ArrayList<>();
    private Item currentTop = null;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        errorView = findViewById(R.id.errorView);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);
        menuScroll = findViewById(R.id.menuScroll);
        subMenuScroll = findViewById(R.id.subMenuScroll);
        menuContainer = findViewById(R.id.menuContainer);
        subMenuContainer = findViewById(R.id.subMenuContainer);
        Button retryButton = findViewById(R.id.retryButton);

        retryButton.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            if (webView.getUrl() == null) {
                webView.loadUrl(HOME_URL);
            } else {
                webView.reload();
            }
        });

        buildMenu();
        setupWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }
        if (webView.getUrl() == null) {
            webView.loadUrl(HOME_URL);
            updateSelection(HOME_URL);
        } else {
            updateSelection(webView.getUrl());
        }
    }

    // ---------------------------------------------------------------- Menyu

    private void buildMenu() {
        for (final Item item : MENU) {
            TextView chip = makeChip(item.title, v -> navigate(BASE_URL + item.path));
            topChips.add(chip);
            menuContainer.addView(chip);
        }
    }

    private TextView makeChip(String text, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextColor(getColorStateList(R.color.chip_text));
        tv.setBackgroundResource(R.drawable.chip_bg);
        tv.setPadding(dp(12), dp(6), dp(12), dp(6));
        tv.setSingleLine(true);
        tv.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(3), dp(2), dp(3), dp(2));
        tv.setLayoutParams(lp);
        tv.setOnClickListener(listener);
        return tv;
    }

    private void rebuildSubMenu(Item top) {
        subMenuContainer.removeAllViews();
        subChips.clear();
        if (top == null || top.children.length == 0) {
            subMenuScroll.setVisibility(View.GONE);
            return;
        }
        for (final Item child : top.children) {
            TextView chip = makeChip(child.title, v -> navigate(BASE_URL + child.path));
            subChips.add(chip);
            subMenuContainer.addView(chip);
        }
        subMenuScroll.scrollTo(0, 0);
        subMenuScroll.setVisibility(View.VISIBLE);
    }

    private static boolean pathMatches(String path, String base) {
        return path.equals(base) || path.startsWith(base + "/");
    }

    /** Joriy sahifa manziliga qarab menyudagi tegishli tugmalarni belgilaydi. */
    private void updateSelection(String url) {
        if (url == null) {
            return;
        }
        Uri uri = Uri.parse(url);
        if (!isInternalHost(uri.getHost()) || uri.getPath() == null) {
            return;
        }
        String path = uri.getPath().toLowerCase(Locale.ROOT);
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int topIndex = -1;
        int subIndex = -1;
        for (int i = 0; i < MENU.length && topIndex < 0; i++) {
            Item it = MENU[i];
            for (int j = 0; j < it.children.length; j++) {
                if (pathMatches(path, it.children[j].path)) {
                    topIndex = i;
                    subIndex = j;
                    break;
                }
            }
            if (topIndex < 0 && pathMatches(path, it.path)) {
                topIndex = i;
            }
        }

        for (int i = 0; i < topChips.size(); i++) {
            topChips.get(i).setSelected(i == topIndex);
        }
        Item top = topIndex >= 0 ? MENU[topIndex] : null;
        if (top != currentTop) {
            currentTop = top;
            rebuildSubMenu(top);
        }
        for (int j = 0; j < subChips.size(); j++) {
            subChips.get(j).setSelected(j == subIndex);
        }
        if (topIndex >= 0) {
            scrollIntoView(menuScroll, topChips.get(topIndex));
        }
        if (subIndex >= 0) {
            scrollIntoView(subMenuScroll, subChips.get(subIndex));
        }
    }

    private void scrollIntoView(final HorizontalScrollView scroll, final View chip) {
        scroll.post(() -> scroll.smoothScrollTo(Math.max(0, chip.getLeft() - dp(24)), 0));
    }

    private void navigate(String url) {
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
        updateSelection(url);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    // -------------------------------------------------------------- WebView

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) {
                    return false;
                }
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    if (isInternalHost(uri.getHost())) {
                        return false;
                    }
                    openExternal(uri);
                    return true;
                }
                if ("tel".equals(scheme) || "mailto".equals(scheme)) {
                    openExternal(uri);
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                updateSelection(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    webView.setVisibility(View.INVISIBLE);
                    errorView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                fullscreenContainer.setVisibility(View.VISIBLE);
                setFullscreenMode(true);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }
                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);
                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
                setFullscreenMode(false);
            }
        });

        // Fayl yuklab olish havolalari (PDF, DOCX va h.k.) tashqi ilova/brauzerda ochiladi.
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                openExternal(Uri.parse(url)));
    }

    private static boolean isInternalHost(String host) {
        if (host == null) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        for (String allowed : INTERNAL_HOSTS) {
            if (h.equals(allowed) || h.endsWith("." + allowed)) {
                return true;
            }
        }
        return false;
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.cannot_open_link, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("deprecation")
    private void setFullscreenMode(boolean on) {
        View decor = getWindow().getDecorView();
        if (on) {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (customView != null) {
            webView.getWebChromeClient().onHideCustomView();
            return;
        }
        if (errorView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            errorView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.goBack();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent != null) {
            parent.removeView(webView);
        }
        webView.destroy();
        super.onDestroy();
    }
}
