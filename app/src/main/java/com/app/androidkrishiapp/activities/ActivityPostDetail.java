package com.app.androidkrishiapp.activities;

import static com.app.androidkrishiapp.utils.Constant.BANNER_POST_DETAIL;
import static com.app.androidkrishiapp.utils.Constant.INTERSTITIAL_POST_DETAIL;
import static com.app.androidkrishiapp.utils.Constant.NATIVE_AD_POST_DETAIL;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.app.androidkrishiapp.BuildConfig;
import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.adapter.AdapterImageDetail;
import com.app.androidkrishiapp.adapter.AdapterNews;
import com.app.androidkrishiapp.callbacks.CallbackPostDetail;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.AdsPref;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.database.sqlite.DbHandler;
import com.app.androidkrishiapp.models.Images;
import com.app.androidkrishiapp.models.Post;
import com.app.androidkrishiapp.models.Value;
import com.app.androidkrishiapp.rests.ApiInterface;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.AdsManager;
import com.app.androidkrishiapp.utils.AppBarLayoutBehavior;
import com.app.androidkrishiapp.utils.Constant;
import com.app.androidkrishiapp.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.Serializable;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityPostDetail extends AppCompatActivity {

    private static final String TAG = "ActivityPostDetail";
    private Call<CallbackPostDetail> callbackCall = null;
    private View lytMainContent;
    private Post post;
    private Menu menu;
    TextView txtTitle, txtCategory, txtDate, txtCommentCount, txtCommentText, txtViewCount;
    ImageView imgThumbVideo, imgDate;
    LinearLayout btnComment, btnView;
    private WebView webView;
    DbHandler dbHandler;
    CoordinatorLayout parentView;
    private ShimmerFrameLayout lytShimmer;
    RelativeLayout lytRelated;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String singleChoiceSelected;
    ViewPager2 viewPager2;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_post_detail);
        Tools.setNavigation(this);

        post = (Post) getIntent().getSerializableExtra(Constant.EXTRA_OBJC);

        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_POST_DETAIL);
        adsManager.loadInterstitialAd(INTERSTITIAL_POST_DETAIL, 1);
        adsManager.loadNativeAd(NATIVE_AD_POST_DETAIL);

        dbHandler = new DbHandler(getApplicationContext());

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setRefreshing(false);

        lytMainContent = findViewById(R.id.lyt_main_content);
        lytShimmer = findViewById(R.id.shimmer_view_container);
        parentView = findViewById(R.id.coordinatorLayout);
        webView = findViewById(R.id.news_description);
        txtTitle = findViewById(R.id.title);
        txtCategory = findViewById(R.id.category);
        txtDate = findViewById(R.id.date);
        imgDate = findViewById(R.id.ic_date);
        txtCommentCount = findViewById(R.id.txt_comment_count);
        txtCommentText = findViewById(R.id.txt_comment_text);
        txtViewCount = findViewById(R.id.txt_view_count);
        btnComment = findViewById(R.id.btn_comment);
        btnView = findViewById(R.id.btn_view);
        imgThumbVideo = findViewById(R.id.thumbnail_video);

        lytRelated = findViewById(R.id.lyt_related);

        requestAction();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            lytShimmer.setVisibility(View.VISIBLE);
            lytShimmer.startShimmer();
            lytMainContent.setVisibility(View.GONE);
            requestAction();
        });

        initToolbar();
        updateView(post.nid);

    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        new Handler().postDelayed(this::requestPostData, 200);
    }

    private void requestPostData() {
        this.callbackCall = RestAdapter.createAPI(sharedPref.getBaseUrl()).getNewsDetail(post.nid);
        this.callbackCall.enqueue(new Callback<CallbackPostDetail>() {
            public void onResponse(@NonNull Call<CallbackPostDetail> call, @NonNull Response<CallbackPostDetail> response) {
                CallbackPostDetail resp = response.body();
                if (resp == null || !resp.status.equals("ok")) {
                    onFailRequest();
                    return;
                }
                displayAllData(resp);
                swipeProgress(false);
                lytMainContent.setVisibility(View.VISIBLE);
            }

            public void onFailure(@NonNull Call<CallbackPostDetail> call, @NonNull Throwable th) {
                Log.e("onFailure", th.getMessage());
                if (!call.isCanceled()) {
                    onFailRequest();
                }
            }
        });
    }

    private void onFailRequest() {
        swipeProgress(false);
        lytMainContent.setVisibility(View.GONE);
        if (Tools.isConnect(ActivityPostDetail.this)) {
            showFailedView(true, getString(R.string.msg_no_network));
        } else {
            showFailedView(true, getString(R.string.msg_offline));
        }
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed_home);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
            lytMainContent.setVisibility(View.VISIBLE);
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            lytShimmer.setVisibility(View.VISIBLE);
            lytShimmer.startShimmer();
            lytMainContent.setVisibility(View.GONE);
        });
    }

    private void displayAllData(CallbackPostDetail resp) {
        displayImages(resp.images);
        displayPostData(resp.post);
        displayRelated(resp.related);
    }

    private void displayPostData(final Post post) {
        txtTitle.setText(Html.fromHtml(post.news_title));
        txtCommentCount.setText("" + post.comments_count);

        new Handler().postDelayed(() -> {
            if (post.comments_count == 0) {
                txtCommentText.setText(R.string.txt_no_comment);
            }
            if (post.comments_count == 1) {
                txtCommentText.setText(getResources().getString(R.string.txt_read) + " " + post.comments_count + " " + getResources().getString(R.string.txt_comment));
            } else if (post.comments_count > 1) {
                txtCommentText.setText(getResources().getString(R.string.txt_read) + " " + post.comments_count + " " + getResources().getString(R.string.txt_comments));
            }
        }, 1500);

        Tools.displayPostDescription(this, webView, post.news_description);

        txtCategory.setText(post.category_name);
        txtCategory.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCategory));

        if (AppConfig.ENABLE_DATE_DISPLAY) {
            txtDate.setVisibility(View.VISIBLE);
            imgDate.setVisibility(View.VISIBLE);
        } else {
            txtDate.setVisibility(View.GONE);
            imgDate.setVisibility(View.GONE);
        }
        txtDate.setText(Tools.getFormatedDate(post.news_date));

        if (!post.content_type.equals("Post")) {
            imgThumbVideo.setVisibility(View.VISIBLE);
        } else {
            imgThumbVideo.setVisibility(View.GONE);
        }

        new Handler().postDelayed(() -> {
            lytRelated.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.txt_related)).setText(getString(R.string.txt_suggested));
        }, 2000);

        btnComment.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ActivityComments.class);
            intent.putExtra("nid", post.nid);
            intent.putExtra("count", post.comments_count);
            intent.putExtra("post_title", post.news_title);
            startActivity(intent);
            adsManager.destroyBannerAd();
        });

        txtCommentText.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ActivityComments.class);
            intent.putExtra("nid", post.nid);
            intent.putExtra("count", post.comments_count);
            intent.putExtra("post_title", post.news_title);
            startActivity(intent);
            adsManager.destroyBannerAd();
        });

        if (!sharedPref.getLoginFeature().equals("yes")) {
            btnComment.setVisibility(View.GONE);
            txtCommentText.setVisibility(View.GONE);
        } else {
            btnComment.setVisibility(View.VISIBLE);
            txtCommentText.setVisibility(View.VISIBLE);
        }

        if (AppConfig.ENABLE_VIEW_COUNT) {
            btnView.setVisibility(View.VISIBLE);
            txtViewCount.setText("" + Tools.withSuffix(post.view_count));
        } else {
            btnView.setVisibility(View.GONE);
        }

    }

    private void displayImages(final List<Images> images) {

        TabLayout tabLayout = findViewById(R.id.tabDots);
        View bgShadow = findViewById(R.id.bgShadow);
        AdapterImageDetail adapter = new AdapterImageDetail(ActivityPostDetail.this, images);
        viewPager2 = findViewById(R.id.viewPager2);
        viewPager2.setAdapter(adapter);
        viewPager2.setOffscreenPageLimit(images.size());
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
        }).attach();

        if (images.size() > 1) {
            tabLayout.setVisibility(View.VISIBLE);
            bgShadow.setVisibility(View.VISIBLE);
        } else {
            tabLayout.setVisibility(View.GONE);
            bgShadow.setVisibility(View.GONE);
        }

        adapter.setOnItemClickListener((view, p, position) -> {
            switch (p.content_type) {
                case "youtube": {
                    Intent intent = new Intent(getApplicationContext(), ActivityYoutubePlayer.class);
                    intent.putExtra("video_id", p.video_id);
                    startActivity(intent);
                    break;
                }
                case "Url": {
                    Intent intent = new Intent(getApplicationContext(), ActivityVideoPlayer.class);
                    intent.putExtra("video_url", post.video_url);
                    startActivity(intent);
                    break;
                }
                case "Upload": {
                    Intent intent = new Intent(getApplicationContext(), ActivityVideoPlayer.class);
                    intent.putExtra("video_url", sharedPref.getBaseUrl() + "/upload/video/" + post.video_url);
                    startActivity(intent);
                    break;
                }
                default: {
                    Intent intent = new Intent(getApplicationContext(), ActivityImageSlider.class);
                    intent.putExtra("position", position);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("arrayList", (Serializable) images);
                    intent.putExtra("bundle", bundle);
                    startActivity(intent);
                    break;
                }
            }

            showInterstitialAdCounter();

        });

    }

    private void showInterstitialAdCounter() {
        if (adsPref.getCounter() >= adsPref.getInterstitialAdInterval()) {
            Log.d("COUNTER_STATUS", "reset and show interstitial");
            adsPref.saveCounter(1);
            adsManager.showInterstitialAd();
        } else {
            adsPref.saveCounter(adsPref.getCounter() + 1);
        }
    }

    private void displayRelated(List<Post> list) {
        RecyclerView recyclerView = findViewById(R.id.recycler_view_related);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        AdapterNews adapterNews = new AdapterNews(ActivityPostDetail.this, recyclerView, list);
        recyclerView.setAdapter(adapterNews);
        recyclerView.setNestedScrollingEnabled(false);
        adapterNews.setOnItemClickListener((view, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityPostDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            showInterstitialAdCounter();
            adsManager.destroyBannerAd();
        });
    }

    private void initToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(post.category_name);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_news_detail, menu);
        this.menu = menu;
        addToFavorite();

        return true;
    }

    public void addToFavorite() {
        List<Post> data = dbHandler.getFavRow(post.nid);
        if (data.size() == 0) {
            menu.getItem(1).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_outline_white));
        } else {
            if (data.get(0).getNid() == post.nid) {
                menu.getItem(1).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_white));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;

            case R.id.action_font_size:
                String[] items = getResources().getStringArray(R.array.dialog_font_size);
                singleChoiceSelected = items[sharedPref.getFontSize()];
                int itemSelected = sharedPref.getFontSize();
                AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityPostDetail.this);
                dialog.setTitle(getString(R.string.title_dialog_font_size));
                dialog.setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> singleChoiceSelected = items[i]);
                dialog.setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
                    WebSettings webSettings = webView.getSettings();
                    if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_xsmall))) {
                        sharedPref.updateFontSize(0);
                        webSettings.setDefaultFontSize(Constant.FONT_SIZE_XSMALL);
                    } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_small))) {
                        sharedPref.updateFontSize(1);
                        webSettings.setDefaultFontSize(Constant.FONT_SIZE_SMALL);
                    } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_medium))) {
                        sharedPref.updateFontSize(2);
                        webSettings.setDefaultFontSize(Constant.FONT_SIZE_MEDIUM);
                    } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_large))) {
                        sharedPref.updateFontSize(3);
                        webSettings.setDefaultFontSize(Constant.FONT_SIZE_LARGE);
                    } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_xlarge))) {
                        sharedPref.updateFontSize(4);
                        webSettings.setDefaultFontSize(Constant.FONT_SIZE_XLARGE);
                    } else {
                        sharedPref.updateFontSize(2);
                        webSettings.setDefaultFontSize(Constant.FONT_SIZE_MEDIUM);
                    }
                    dialogInterface.dismiss();
                });
                dialog.show();
                break;

            case R.id.action_later:

                List<Post> data = dbHandler.getFavRow(post.nid);
                if (data.size() == 0) {
                    dbHandler.AddtoFavorite(new Post(
                            post.nid,
                            post.news_title,
                            post.category_name,
                            post.news_date,
                            post.news_image,
                            post.news_description,
                            post.content_type,
                            post.video_url,
                            post.video_id,
                            post.comments_count
                    ));
                    Snackbar.make(parentView, R.string.favorite_added, Snackbar.LENGTH_SHORT).show();
                    menu.getItem(1).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_white));

                } else {
                    if (data.get(0).getNid() == post.nid) {
                        dbHandler.RemoveFav(new Post(post.nid));
                        Snackbar.make(parentView, R.string.favorite_removed, Snackbar.LENGTH_SHORT).show();
                        menu.getItem(1).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_outline_white));
                    }
                }

                break;

            case R.id.action_share:

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, post.news_title + "\n\n" + getResources().getString(R.string.share_content) + "\n\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);

                break;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    private void updateView(long nid) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        Call<Value> call = apiInterface.updateView(nid);
        call.enqueue(new Callback<Value>() {
            @Override
            public void onResponse(@NonNull Call<Value> call, @NonNull Response<Value> response) {
                Value data = response.body();
                if (data != null) {
                    Log.d(TAG, "View counter updated +" + data.value);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Value> call, @NonNull Throwable t) {
                Log.d(TAG, "error " + t.getMessage());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(BANNER_POST_DETAIL);
        Log.d("COUNTER", "counter : " + adsPref.getCounter());
    }

    public void onDestroy() {
        super.onDestroy();
        if (!(callbackCall == null || callbackCall.isCanceled())) {
            this.callbackCall.cancel();
        }
        lytShimmer.stopShimmer();
        adsManager.destroyBannerAd();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        adsManager.destroyBannerAd();
    }

}
