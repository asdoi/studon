/*
    This file is part of the HHS Moodle WebApp.

    HHS Moodle WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HHS Moodle WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Diaspora Native WebApp.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.hhsmoodle;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import saschpe.android.customtabs.CustomTabsHelper;
import saschpe.android.customtabs.WebViewFallback;


public class Activity_Main extends AppCompatActivity {

    public static final String DEFAULT_WEBSITE = "https://lernplattform.mebis.bayern.de/";
    public static final String MEBIS = "mebis.bayern.de";
    public static final String LOGIN_SITE = "idp.mebis.bayern.de";
    public static final String DASHBOARD = "Schreibtisch";

    private WebViewWithTouchEvents mWebView;
    private ProgressBar progressBar;
    private SharedPreferences sharedPref;
    private ValueCallback<Uri[]> mFilePathCallback;

    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private int columns;

    private boolean loadUrl = true;

    private Bookmarks_Database db;
    private GridView bookmarkList;
    private BottomSheetDialog bottomSheetDialog;
    private TextView favoriteTitleTV;
    private Activity activity;
    private TextView titleView;

    private BottomAppBar bottomAppBar;

    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_system);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_screen_main);
        setUpBottomAppBar();

        Class_Helper.checkAuthentication(Activity_Main.this);

        activity = Activity_Main.this;

        PreferenceManager.setDefaultValues(activity, R.xml.user_settings, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        columns = Integer.parseInt(sharedPref.getString("columns", "2"));

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        mWebView = findViewById(R.id.webView);
        myWebChromeClient mWebChromeClient = new myWebChromeClient();
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setGeolocationEnabled(false);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                final Uri uri = Uri.parse(url);
                return handleUri(view, uri);
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                final Uri uri = request.getUrl();
                return handleUri(view, uri);
            }

            private boolean handleUri(final WebView webView, final Uri uri) {
                pressedBack = false;

                final String url = uri.toString();
                loadUrl = true;

                if (sharedPref.getBoolean("external", true)) {
                    if (url.contains(MEBIS)) {
                        webView.loadUrl(url);
                        return true;
                    } else {
                        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                        View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                        TextView textView = dialogView.findViewById(R.id.dialog_text);
                        textView.setText(getResources().getString(R.string.toast_external));
                        Button action_ok = dialogView.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(view -> openInCustomTabs(url));
                        bottomSheetDialog.setContentView(dialogView);
                        bottomSheetDialog.show();
                        Class_Helper.setBottomSheetBehavior(bottomSheetDialog, dialogView);
                        bottomSheetDialog.setOnCancelListener(dialog -> {
                            bottomSheetDialog.cancel();
                            if (loadUrl) {
                                webView.loadUrl(url);
                            }
                        });
                    }
                } else {
                    webView.loadUrl(url);
                }
                return true;//do nothing in other cases
            }

            @Override
            public void onPageFinished(WebView view, final String url) {
                super.onPageFinished(view, url);

                if (url.contains(LOGIN_SITE)) {
                    String username = sharedPref.getString("username", "");
                    String password = sharedPref.getString("password", "");

                    view.addJavascriptInterface(new LogInFailedInterface(), "Android");


                    final String js = "javascript:" +
                            "var logfail = document.querySelector(\".form-error\");" +
                            "if (logfail != null) {" +
                            "Android.loginFailed();" +
                            "}else{" +
                            "document.getElementById('password').value = '" + password + "';" +
                            "document.getElementById('username').value = '" + username + "';" +
                            "document.getElementById('submitbutton').click();" +
                            "}";

                    view.evaluateJavascript(js, s -> {
                    });
                } else
                    removeElements(view);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                removeElements(view);
            }
        });

        mWebView.setOnTouchListener(new SwipeTouchListener(activity) {
            public void onSwipeTop() {
                bottomAppBar.animate().translationY(+bottomAppBar.getHeight()).setInterpolator(new AccelerateInterpolator(2));
                removeElements(mWebView);
            }

            public void onSwipeBottom() {
                bottomAppBar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
                removeElements(mWebView);
            }

            public void onSwipeRight() {
                    navigationDrawerClick();
            }

            public void onSwipeLeft() {
                messagesDrawerClick();
            }
        });

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            final String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
            String text = getString(R.string.toast_download_1) + " " + filename;

            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
            View dialogView = View.inflate(activity, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(text);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(view -> {
                bottomSheetDialog.cancel();

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                CookieManager cookieManager = CookieManager.getInstance();
                String cookie = cookieManager.getCookie(url);
                request.addRequestHeader("Cookie", cookie);
                request.setTitle(filename);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

                DownloadManager manager = (DownloadManager) Objects.requireNonNull(activity).getSystemService(Context.DOWNLOAD_SERVICE);
                assert manager != null;
                activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29) {
                    int hasWRITE_EXTERNAL_STORAGE = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                        Class_Helper.grantPermissionsStorage(activity);
                    } else {
                        manager.enqueue(request);
                        Toast.makeText(activity, getString(R.string.toast_download) + " " + filename, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    manager.enqueue(request);
                    Toast.makeText(activity, getString(R.string.toast_download) + " " + filename, Toast.LENGTH_SHORT).show();
                }
            });
            bottomSheetDialog.setContentView(dialogView);

            if (sharedPref.getBoolean("confirm_download", false)) {
                bottomSheetDialog.show();
                Class_Helper.setBottomSheetBehavior(bottomSheetDialog, dialogView);
            } else
                action_ok.performClick();
        });

        registerForContextMenu(mWebView);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) && sharedPref.getBoolean("nightMode", false)) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    // Night mode is not active, we're using the light theme
                    WebSettingsCompat.setForceDark(mWebView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    // Night mode is active, we're using dark theme
                    WebSettingsCompat.setForceDark(mWebView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
                    break;
            }
        }

        bottomAppBar.setOnTouchListener(new SwipeTouchListener(activity) {
            public void onSwipeTop() {
                ObjectAnimator anim = ObjectAnimator.ofInt(mWebView, "scrollY", mWebView.getScrollY(), 0);
                anim.setDuration(500).start();
            }

            public void onSwipeBottom() {
                ObjectAnimator anim = ObjectAnimator.ofInt(mWebView, "scrollY", mWebView.getScrollY(), mWebView.getContentHeight() * ((int) getResources().getDisplayMetrics().density));
                anim.setDuration(500).start();
            }

            public void onSwipeRight() {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
                } else {
                    Toast.makeText(activity, getString(R.string.toast_notForward), Toast.LENGTH_SHORT).show();
                }
            }

            public void onSwipeLeft() {
                if (!goBack()) {
                    Toast.makeText(activity, getString(R.string.toast_notBack), Toast.LENGTH_SHORT).show();
                }
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> openMenu());
        fab.setOnLongClickListener(v -> {
            openBookmarkDialog();
            return false;
        });

        fab.setOnTouchListener(new SwipeTouchListener(activity) {
            public void onSwipeTop() {
                ObjectAnimator anim = ObjectAnimator.ofInt(mWebView, "scrollY", mWebView.getScrollY(), 0);
                anim.setDuration(500).start();
            }

            public void onSwipeBottom() {
                ObjectAnimator anim = ObjectAnimator.ofInt(mWebView, "scrollY", mWebView.getScrollY(), mWebView.getContentHeight() * ((int) getResources().getDisplayMetrics().density));
                anim.setDuration(500).start();
            }

            public void onSwipeRight() {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
                } else {
                    Toast.makeText(activity, getString(R.string.toast_notForward), Toast.LENGTH_SHORT).show();
                }
            }

            public void onSwipeLeft() {
                if (!goBack()) {
                    Toast.makeText(activity, getString(R.string.toast_notBack), Toast.LENGTH_SHORT).show();
                }
            }
        });

        try {
            if (sharedPref.getString("username", "").length() < 1 ||
                    sharedPref.getString("password", "").length() < 1 ||
                    sharedPref.getString("link", Activity_Main.DEFAULT_WEBSITE).length() < 1) {
                throw new Exception();
            } else {
                mWebView.loadUrl(sharedPref.getString("favoriteURL", Activity_Main.DEFAULT_WEBSITE));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Class_Helper.setLoginData(activity, () -> Activity_Settings.createInfoDialog(this, R.string.dialog_help_title, R.string.dialog_help_text, this::recreate), this::finishAffinity);
        }
    }

    private void navigationDrawerClick() {
        final String js = "javascript:" +
                "if (document.querySelector(\"#page-wrapper .drawer\").getAttribute(\"aria-expanded\") == \"true\") {" +
                "document.querySelector(\"#page-wrapper .drawer .message-app .closewidget .text-dark .icon\").click();" +
                "} else {" +
                "document.querySelector(\"nav.navbar div.d-inline-block button\").click(); }";

        mWebView.evaluateJavascript(js, s -> {
        });
    }

    private void messagesDrawerClick() {
        final String js = "javascript:" +
                "if(document.querySelector(\"#page-wrapper #nav-drawer\").getAttribute(\"aria-hidden\") == \"false\"){" +
                "document.querySelector(\"nav.navbar div.d-inline-block button\").click(); " +
                "} else if (document.querySelector(\"#page-wrapper .drawer\").getAttribute(\"aria-expanded\") == \"true\") {" +
                "document.querySelector(\"#page-wrapper .drawer .message-app .closewidget .text-dark .icon\").click();" +
                "} else {" +
                "document.querySelector(\"nav.navbar ul.nav li.communication div.popover-region a.nav-link .icon\").click(); }";

        mWebView.evaluateJavascript(js, s -> {
        });
    }

    private void removeElements(WebView view) {
//        final String js = "javascript:" +
//                "document.querySelector(\"#mbsmenubar\").remove();";
//
//        view.evaluateJavascript(js, s -> {
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Uri uri = Objects.requireNonNull(getIntent().getData());
            String url = uri.toString();
            mWebView.loadUrl(url);
            setIntent(null);
        } catch (Exception ignore) {
        }
    }

    private void openInCustomTabs(String url) {
        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .addDefaultShareMenuItem()
                    .setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary))
                    .setShowTitle(true)
                    .setStartAnimations(this, R.anim.slide_in, 0)
                    .build();

            // This is optional but recommended
            CustomTabsHelper.Companion.addKeepAliveExtra(activity, customTabsIntent.intent);

            // This is where the magic happens...
            CustomTabsHelper.Companion.openCustomTab(activity, customTabsIntent,
                    Uri.parse(url),
                    new WebViewFallback());
        } catch (Exception ignore) {
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView.HitTestResult result = mWebView.getHitTestResult();
        if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(result.getExtra()));
            Intent chooser = Intent.createChooser(intent, null);
            startActivity(chooser);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) && sharedPref.getBoolean("nightMode", false)) {
            int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    // Night mode is not active, we're using the light theme
                    WebSettingsCompat.setForceDark(mWebView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    // Night mode is active, we're using dark theme
                    WebSettingsCompat.setForceDark(mWebView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
                    break;
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpBottomAppBar() {
        bottomAppBar = findViewById(R.id.bar);
        bottomAppBar.setNavigationIcon(null);
        setSupportActionBar(bottomAppBar);

        bottomAppBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_search_close:
                    mWebView.findAllAsync("");
                    titleView.setText(mWebView.getTitle());
                    bottomAppBar.replaceMenu(R.menu.menu_main);
                    break;
                case R.id.action_search_next:
                    mWebView.findNext(true);
                    break;
                case R.id.action_search_prev:
                    mWebView.findNext(false);
                    break;
                case R.id.action_bookmark:
                    openBookmarkDialog();
                    break;
                case R.id.action_menu:
                    openMenu();
                    break;
            }
            return false;
        });
    }

    private void openBookmarkDialog() {
        db = new Bookmarks_Database(activity);
        db.open();
        bottomSheetDialog = new BottomSheetDialog(Objects.requireNonNull(activity));
        View dialogView = View.inflate(activity, R.layout.grid_layout, null);
        String favoriteTitle = getString(R.string.bookmark_setFav) + ": " + sharedPref.getString("favoriteTitle", DASHBOARD);
        favoriteTitleTV = dialogView.findViewById(R.id.grid_title);
        favoriteTitleTV.setText(favoriteTitle);
        bookmarkList = dialogView.findViewById(R.id.grid_item);
        setBookmarksList();
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void openMenu() {
        bottomSheetDialog = new BottomSheetDialog(Objects.requireNonNull(activity));
        View dialogView = View.inflate(activity, R.layout.grid_layout, null);
        TextView menuTitle = dialogView.findViewById(R.id.grid_title);
        menuTitle.setText(mWebView.getTitle());

        ImageButton menu_refresh = dialogView.findViewById(R.id.menu_refresh);
        menu_refresh.setVisibility(View.VISIBLE);
        menu_refresh.setOnClickListener(v -> {
            bottomSheetDialog.cancel();
            mWebView.reload();
        });

        GridView grid = dialogView.findViewById(R.id.grid_item);
        GridItem_Menu itemAlbum_downloads = new GridItem_Menu(getResources().getString(R.string.menu_more_files), R.drawable.icon_download);
        GridItem_Menu itemAlbum_settings = new GridItem_Menu(getResources().getString(R.string.menu_more_settings), R.drawable.icon_settings);
        GridItem_Menu itemAlbum_save = new GridItem_Menu(getResources().getString(R.string.menu_save_bookmark), R.drawable.icon_bookmark);
        GridItem_Menu itemAlbum_print = new GridItem_Menu(getResources().getString(R.string.menu_save_pdf), R.drawable.icon_printer);
        GridItem_Menu itemAlbum_share = new GridItem_Menu(getResources().getString(R.string.menu_share), R.drawable.icon_share);
        GridItem_Menu itemAlbum_exit = new GridItem_Menu(getResources().getString(R.string.menu_finish), R.drawable.icon_exit);
        GridItem_Menu itemAlbum_search = new GridItem_Menu(getResources().getString(R.string.menu_search), R.drawable.icon_magnify);
        GridItem_Menu itemAlbum_open_in_browser = new GridItem_Menu(getResources().getString(R.string.menu_openInBrowser), R.drawable.icon_earth);

        final String url = mWebView.getUrl();

        final int SETTINGS = 0;
        final int SAVE = 1;
        final int SHARE = 2;
        final int SEARCH = 3;
        final int DOWNLOADS = 4;
        final int EXIT = 5;
        final int PRINT = -1;
        final int OPEN_IN_BROWSER = -2;


        final List<GridItem_Menu> gridList = new LinkedList<>();

        gridList.add(SETTINGS, itemAlbum_settings);
        gridList.add(SAVE, itemAlbum_save);
        gridList.add(SHARE, itemAlbum_share);
        gridList.add(SEARCH, itemAlbum_search);
        gridList.add(DOWNLOADS, itemAlbum_downloads);
        gridList.add(EXIT, itemAlbum_exit);
//        gridList.add(PRINT, itemAlbum_print);
//        gridList.add(OPEN_IN_BROWSER, itemAlbum_open_in_browser);

        GridAdapter_Menu gridAdapter = new GridAdapter_Menu(activity, gridList);
        grid.setAdapter(gridAdapter);
        grid.setNumColumns(columns);
        gridAdapter.notifyDataSetChanged();

        grid.setOnItemClickListener((parent, view, position, id) -> {

            switch (position) {
                case SETTINGS:
                    bottomSheetDialog.cancel();
                    Class_Helper.switchToActivity(activity, Activity_Settings.class);
                    break;
                case DOWNLOADS:
                    bottomSheetDialog.cancel();
                    startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                    break;
                case SAVE:
                    if (url != null) {
                        bottomSheetDialog.cancel();
                        final Bookmarks_Database db = new Bookmarks_Database(activity);
                        db.open();
                        if (db.isExist(Class_Helper.secString(mWebView.getUrl()))) {
                            Toast.makeText(activity, getString(R.string.bookmark_saved_not), Toast.LENGTH_SHORT).show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            View dialogView1 = View.inflate(activity, R.layout.dialog_edit_title, null);
                            final EditText edit_title = dialogView1.findViewById(R.id.pass_title);
                            edit_title.setHint(R.string.bookmark_edit_hint);
                            edit_title.setText(mWebView.getTitle());
                            builder.setView(dialogView1);
                            builder.setTitle(R.string.bookmark_edit);
                            builder.setPositiveButton(R.string.toast_yes, (dialog, whichButton) -> {
                                String inputTag = edit_title.getText().toString().trim();
                                db.insert(Class_Helper.secString(inputTag), Class_Helper.secString(mWebView.getUrl()), "04", "");
                                dialog.cancel();
                                Toast.makeText(activity, getString(R.string.bookmark_saved), Toast.LENGTH_SHORT).show();
                            });
                            builder.setNegativeButton(R.string.toast_cancel, (dialog, whichButton) -> dialog.cancel());
                            final AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                    break;
                case SHARE:
                    if (url != null) {
                        bottomSheetDialog.cancel();
                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mWebView.getTitle());
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                        startActivity(Intent.createChooser(sharingIntent, (getString(R.string.app_share))));
                        break;
                    }
                case PRINT:
                    if (url != null) {
                        bottomSheetDialog.cancel();
                        try {
                            String title = mWebView.getTitle() + ".pdf";
                            String pdfTitle = mWebView.getTitle();
                            PrintManager printManager = (PrintManager) Objects.requireNonNull(activity).getSystemService(Context.PRINT_SERVICE);
                            PrintDocumentAdapter printAdapter = mWebView.createPrintDocumentAdapter(title);
                            Objects.requireNonNull(printManager).print(pdfTitle, printAdapter, new PrintAttributes.Builder().build());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(activity, getString(R.string.toast_notPrint), Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case SEARCH:
                    bottomSheetDialog.cancel();
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    View dialogView2 = View.inflate(activity, R.layout.dialog_edit_title, null);
                    final EditText edit_title = dialogView2.findViewById(R.id.pass_title);
                    edit_title.setHint(R.string.dialog_searchHint);
                    builder.setView(dialogView2);
                    builder.setTitle(R.string.menu_search);
                    builder.setPositiveButton(R.string.toast_yes, (dialog, whichButton) -> {
                        String text = getResources().getString(R.string.menu_search) + ": " + edit_title.getText();
                        titleView.setText(text);
                        dialog.cancel();
                        mWebView.findAllAsync(edit_title.getText().toString());
                        bottomAppBar.replaceMenu(R.menu.menu_search);

                    });
                    builder.setNegativeButton(R.string.toast_cancel, (dialog, whichButton) -> dialog.cancel());
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                    break;
                case EXIT:
                    bottomSheetDialog.cancel();
                    mWebView.destroy();
                    Objects.requireNonNull(activity).finish();
                    break;
                case OPEN_IN_BROWSER:
                    bottomSheetDialog.cancel();
                    openInCustomTabs(mWebView.getUrl());
                    break;
            }
        });
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
        Class_Helper.setBottomSheetBehavior(bottomSheetDialog, dialogView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private static boolean pressedBack = false;

    @Override
    public void onBackPressed() {
        if (!goBack()) {
            if (pressedBack) {
                mWebView.destroy();
                finishAffinity();
                pressedBack = false;
            } else {
                Toast.makeText(getApplicationContext(), R.string.back_button, Toast.LENGTH_LONG).show();
                pressedBack = true;
            }
        }
    }

    private boolean goBack() {
        WebBackForwardList currentList = mWebView.copyBackForwardList();

        if (mWebView.canGoBack() && (currentList.getSize() > 0 && !currentList.getItemAtIndex(currentList.getCurrentIndex() - 1).getUrl().contains(LOGIN_SITE))) {
            mWebView.goBack();
            return true;
        } else
            return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;
        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // If there is not data, then we may have taken a photo
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    private final BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);

                if (cursor.moveToFirst()) {
                    int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));

                    if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                        View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                        TextView textView = dialogView.findViewById(R.id.dialog_text);
                        textView.setText(Class_Helper.textSpannable(activity.getString(R.string.toast_download_2)));
                        Button action_ok = dialogView.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(view -> {
                            bottomSheetDialog.cancel();
                            openDownloadedAttachment(Uri.parse(downloadLocalUri), downloadMimeType);
                        });
                        bottomSheetDialog.setContentView(dialogView);
                        bottomSheetDialog.show();
                        Class_Helper.setBottomSheetBehavior(bottomSheetDialog, dialogView);
                    }
                }
                cursor.close();
            }
            Objects.requireNonNull(activity).unregisterReceiver(onComplete);
        }
    };

    private void openDownloadedAttachment(Uri attachmentUri, final String attachmentMimeType) {
        if (attachmentUri != null) {
            // Get Content Uri.
            if (ContentResolver.SCHEME_FILE.equals(attachmentUri.getScheme())) {
                // FileUri - Convert it to contentUri.
                File file = new File(Objects.requireNonNull(attachmentUri.getPath()));
                attachmentUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
            }

            Intent openAttachmentIntent = new Intent(Intent.ACTION_VIEW);
            openAttachmentIntent.setDataAndType(attachmentUri, attachmentMimeType);
            openAttachmentIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(openAttachmentIntent);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            }
        }
    }

    private void setBookmarksList() {

        //display data
        final int layoutStyle = R.layout.item;
        int[] xml_id = new int[]{
                R.id.item_title
        };
        String[] column = new String[]{
                "bookmarks_title"
        };
        final Cursor row = db.fetchAllData(activity);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(activity, layoutStyle, row, column, xml_id, 0) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                Cursor row = (Cursor) bookmarkList.getItemAtPosition(position);
                final String bookmarks_icon = row.getString(row.getColumnIndexOrThrow("bookmarks_icon"));
                View v = super.getView(position, convertView, parent);
                ImageView iv_icon = v.findViewById(R.id.item_icon);
                Class_Helper.switchIcon(activity, bookmarks_icon, iv_icon);
                return v;
            }
        };

        bookmarkList.setAdapter(adapter);
        bookmarkList.setNumColumns(columns);

        if (bookmarkList.getAdapter().getCount() == 0) {
            String url = sharedPref.getString("link", Activity_Main.DEFAULT_WEBSITE);
            db.insert(DASHBOARD, url, "14", "");
            setBookmarksList();
        }
        //onClick function
        bookmarkList.setOnItemClickListener((adapterView, view, position, id) -> {
            bottomSheetDialog.cancel();
            Cursor row12 = (Cursor) bookmarkList.getItemAtPosition(position);
            final String bookmarks_content = row12.getString(row12.getColumnIndexOrThrow("bookmarks_content"));
            mWebView.loadUrl(bookmarks_content);
        });

        bookmarkList.setOnItemLongClickListener((parent, view, position, id) -> {
            Cursor row1 = (Cursor) bookmarkList.getItemAtPosition(position);
            final String _id = row1.getString(row1.getColumnIndexOrThrow("_id"));
            final String bookmarks_title = row1.getString(row1.getColumnIndexOrThrow("bookmarks_title"));
            final String bookmarks_url = row1.getString(row1.getColumnIndexOrThrow("bookmarks_content"));
            final String bookmarks_icon = row1.getString(row1.getColumnIndexOrThrow("bookmarks_icon"));
            final String bookmarks_fav = row1.getString(row1.getColumnIndexOrThrow("bookmarks_attachment"));

            final BottomSheetDialog bottomSheetDialog_context = new BottomSheetDialog(Objects.requireNonNull(activity));
            View dialogView = View.inflate(activity, R.layout.grid_layout, null);
            TextView menuTitle = dialogView.findViewById(R.id.grid_title);
            menuTitle.setText(bookmarks_title);
            GridView grid = dialogView.findViewById(R.id.grid_item);
            GridItem_Menu itemAlbum_01 = new GridItem_Menu(getResources().getString(R.string.bookmark_edit), R.drawable.icon_edit);
            GridItem_Menu itemAlbum_02 = new GridItem_Menu(getResources().getString(R.string.bookmark_icon), R.drawable.icon_ui);
            GridItem_Menu itemAlbum_03 = new GridItem_Menu(getResources().getString(R.string.bookmark_setFav), R.drawable.icon_fav);
            GridItem_Menu itemAlbum_04 = new GridItem_Menu(getResources().getString(R.string.bookmark_remove), R.drawable.icon_delete);

            final List<GridItem_Menu> gridList = new LinkedList<>();
            gridList.add(gridList.size(), itemAlbum_01);
            gridList.add(gridList.size(), itemAlbum_02);
            gridList.add(gridList.size(), itemAlbum_03);
            gridList.add(gridList.size(), itemAlbum_04);

            GridAdapter_Menu gridAdapter = new GridAdapter_Menu(activity, gridList);
            grid.setAdapter(gridAdapter);
            gridAdapter.notifyDataSetChanged();

            grid.setOnItemClickListener((parent12, view13, position12, id12) -> {

                View dialogView1;

                switch (position12) {
                    case 0:
                        bottomSheetDialog_context.cancel();
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        dialogView1 = View.inflate(activity, R.layout.dialog_edit_title, null);

                        final EditText edit_title = dialogView1.findViewById(R.id.pass_title);
                        edit_title.setHint(R.string.bookmark_edit_hint);
                        edit_title.setText(bookmarks_title);

                        builder.setView(dialogView1);
                        builder.setTitle(R.string.bookmark_edit);
                        builder.setPositiveButton(R.string.toast_yes, (dialog, whichButton) -> {
                            String inputTag = edit_title.getText().toString().trim();
                            db.update(Integer.parseInt(_id), Class_Helper.secString(inputTag), Class_Helper.secString(bookmarks_url), bookmarks_icon, bookmarks_fav);
                            setBookmarksList();
                        });
                        builder.setNegativeButton(R.string.toast_cancel, (dialog, whichButton) -> dialog.cancel());
                        final AlertDialog dialog = builder.create();
                        dialog.show();
                        break;
                    case 1:
                        bottomSheetDialog_context.cancel();

                        final BottomSheetDialog bottomSheetDialog_icon = new BottomSheetDialog(Objects.requireNonNull(activity));
                        dialogView1 = View.inflate(activity, R.layout.grid_layout, null);
                        TextView menuTitle1 = dialogView1.findViewById(R.id.grid_title);
                        menuTitle1.setText(getString(R.string.bookmark_icon));
                        GridView grid1 = dialogView1.findViewById(R.id.grid_item);
                        GridItem_Menu itemAlbum_011 = new GridItem_Menu(getResources().getString(R.string.subjects_color_red), R.drawable.circle_red);
                        GridItem_Menu itemAlbum_021 = new GridItem_Menu(getResources().getString(R.string.subjects_color_pink), R.drawable.circle_pink);
                        GridItem_Menu itemAlbum_031 = new GridItem_Menu(getResources().getString(R.string.subjects_color_purple), R.drawable.circle_purple);
                        GridItem_Menu itemAlbum_041 = new GridItem_Menu(getResources().getString(R.string.subjects_color_blue), R.drawable.circle_blue);
                        GridItem_Menu itemAlbum_05 = new GridItem_Menu(getResources().getString(R.string.subjects_color_teal), R.drawable.circle_teal);
                        GridItem_Menu itemAlbum_06 = new GridItem_Menu(getResources().getString(R.string.subjects_color_green), R.drawable.circle_green);
                        GridItem_Menu itemAlbum_07 = new GridItem_Menu(getResources().getString(R.string.subjects_color_lime), R.drawable.circle_lime);
                        GridItem_Menu itemAlbum_08 = new GridItem_Menu(getResources().getString(R.string.subjects_color_yellow), R.drawable.circle_yellow);
                        GridItem_Menu itemAlbum_09 = new GridItem_Menu(getResources().getString(R.string.subjects_color_orange), R.drawable.circle_orange);
                        GridItem_Menu itemAlbum_10 = new GridItem_Menu(getResources().getString(R.string.subjects_color_brown), R.drawable.circle_brown);

                        final List<GridItem_Menu> gridList1 = new LinkedList<>();
                        gridList1.add(gridList1.size(), itemAlbum_011);
                        gridList1.add(gridList1.size(), itemAlbum_021);
                        gridList1.add(gridList1.size(), itemAlbum_031);
                        gridList1.add(gridList1.size(), itemAlbum_041);
                        gridList1.add(gridList1.size(), itemAlbum_05);
                        gridList1.add(gridList1.size(), itemAlbum_06);
                        gridList1.add(gridList1.size(), itemAlbum_07);
                        gridList1.add(gridList1.size(), itemAlbum_08);
                        gridList1.add(gridList1.size(), itemAlbum_09);
                        gridList1.add(gridList1.size(), itemAlbum_10);

                        GridAdapter_Menu gridAdapter1 = new GridAdapter_Menu(activity, gridList1);
                        grid1.setAdapter(gridAdapter1);
                        gridAdapter1.notifyDataSetChanged();

                        grid1.setOnItemClickListener((parent1, view12, position1, id1) -> {

                            switch (position1) {
                                case 0:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "14", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 1:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "15", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 2:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "16", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 3:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "17", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 4:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "18", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 5:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "19", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 6:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "20", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 7:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "21", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 8:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "22", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                                case 9:
                                    bottomSheetDialog_icon.cancel();
                                    db.update(Integer.parseInt(_id), Class_Helper.secString(bookmarks_title), Class_Helper.secString(bookmarks_url), "23", bookmarks_fav);
                                    setBookmarksList();
                                    break;
                            }
                        });
                        bottomSheetDialog_icon.setContentView(dialogView1);
                        bottomSheetDialog_icon.show();
                        Class_Helper.setBottomSheetBehavior(bottomSheetDialog_icon, dialogView1);
                        break;
                    case 2:
                        bottomSheetDialog_context.cancel();
                        sharedPref.edit()
                                .putString("favoriteURL", bookmarks_url)
                                .putString("favoriteTitle", bookmarks_title).apply();

                        String favoriteTitle = getString(R.string.bookmark_setFav) + ": " + sharedPref.getString("favoriteTitle", DASHBOARD);

                        favoriteTitleTV.setText(favoriteTitle);
                        break;
                    case 3:
                        bottomSheetDialog_context.cancel();
                        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                        dialogView1 = View.inflate(activity, R.layout.dialog_action, null);
                        TextView textView = dialogView1.findViewById(R.id.dialog_text);
                        textView.setText(Class_Helper.textSpannable(activity.getString(R.string.bookmark_remove_confirm)));
                        Button action_ok = dialogView1.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(view1 -> {
                            bottomSheetDialog.cancel();
                            db.delete(Integer.parseInt(_id));
                            setBookmarksList();
                        });
                        bottomSheetDialog.setContentView(dialogView1);
                        bottomSheetDialog.show();
                        Class_Helper.setBottomSheetBehavior(bottomSheetDialog, dialogView1);
                        break;
                }
            });

            bottomSheetDialog_context.setContentView(dialogView);
            bottomSheetDialog_context.show();
            Class_Helper.setBottomSheetBehavior(bottomSheetDialog_context, dialogView);
            return true;
        });
    }

    private class myWebChromeClient extends WebChromeClient {

        public void onProgressChanged(WebView view, int progress) {
            progressBar.setProgress(progress);
            titleView = findViewById(R.id.titleView);
            titleView.setText(mWebView.getTitle());
            bottomAppBar.replaceMenu(R.menu.menu_main);
            mWebView.findAllAsync("");
            progressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);
        }

        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*");
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
            return true;
        }
    }

    public class LogInFailedInterface {

        /**
         * Instantiate the interface and set the context
         */
        LogInFailedInterface() {
        }

        @JavascriptInterface
        public void loginFailed() {
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, getString(R.string.login_failed), Toast.LENGTH_LONG).show();
                Class_Helper.setLoginData(activity, () -> activity.recreate(), () -> activity.finishAffinity());
            });
        }
    }
}