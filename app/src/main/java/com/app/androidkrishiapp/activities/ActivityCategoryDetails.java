package com.app.androidkrishiapp.activities;

import static com.app.androidkrishiapp.utils.Constant.BANNER_CATEGORY_DETAIL;
import static com.app.androidkrishiapp.utils.Constant.INTERSTITIAL_POST_LIST;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.adapter.AdapterRecent;
import com.app.androidkrishiapp.callbacks.CallbackCategoryDetails;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.AdsPref;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.Category;
import com.app.androidkrishiapp.models.Post;
import com.app.androidkrishiapp.rests.ApiInterface;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.AdsManager;
import com.app.androidkrishiapp.utils.Constant;
import com.app.androidkrishiapp.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityCategoryDetails extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private AdapterRecent adapterRecent;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Call<CallbackCategoryDetails> callbackCall = null;
    private Category category;
    private long postTotal = 0;
    private long failedPage = 0;
    TextView txtTitleToolbar;
    private AdsManager adsManager;
    private AdsPref adsPref;
    private ShimmerFrameLayout lytShimmer;
    View lytShimmerHead;
    private List<Post> posts = new ArrayList<>();
    SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_category_details);

        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_CATEGORY_DETAIL);
        adsManager.loadInterstitialAd(INTERSTITIAL_POST_LIST, adsPref.getInterstitialAdInterval());

        // get extra object
        category = (Category) getIntent().getSerializableExtra(Constant.EXTRA_OBJC);
        postTotal = category.post_count;

        lytShimmer = findViewById(R.id.shimmer_view_container);
        lytShimmerHead = findViewById(R.id.lyt_shimmer_head);
        if (!AppConfig.DISPLAY_HEADER_VIEW) {
            lytShimmerHead.setVisibility(View.GONE);
        }

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));

        //set data and list adapter
        adapterRecent = new AdapterRecent(this, recyclerView, posts);
        recyclerView.setAdapter(adapterRecent);

        // on item list clicked
        adapterRecent.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityPostDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            adsManager.showInterstitialAd();
            adsManager.destroyBannerAd();
        });

        // detect when scroll reach bottom
        adapterRecent.setOnLoadMoreListener(this::setLoadMore);

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) {
                callbackCall.cancel();
            }
            adapterRecent.resetListData();
            requestAction(1);
        });

        requestAction(1);

        initToolbar();
        txtTitleToolbar = findViewById(R.id.txt_title_toolbar);
        txtTitleToolbar.setText(category.category_name);

        if (AppConfig.DISPLAY_HEADER_VIEW && !AppConfig.BIG_NEWS_LIST_STYLE) {
            recyclerView.setPadding(0, 0, 0, getResources().getDimensionPixelOffset(R.dimen.padding_small));
        } else {
            recyclerView.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.padding_small), 0, getResources().getDimensionPixelOffset(R.dimen.padding_small));
        }

    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterRecent.getItemCount() - current_page);
        if (postTotal > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterRecent.setLoaded();
        }
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPref sharedPref = new SharedPref(this);
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_search:
                Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void displayApiResult(final List<Post> posts) {
        adapterRecent.insertDataWithNativeAd(posts);
        swipeProgress(false);
        if (posts.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestPostApi(final long page_no) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        callbackCall = apiInterface.getCategoryDetailsByPage(category.cid, AppConfig.REST_API_KEY, page_no, AppConfig.LOAD_MORE);
        callbackCall.enqueue(new Callback<CallbackCategoryDetails>() {
            @Override
            public void onResponse(@NonNull Call<CallbackCategoryDetails> call, @NonNull Response<CallbackCategoryDetails> response) {
                CallbackCategoryDetails resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    displayApiResult(resp.posts);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackCategoryDetails> call, @NonNull Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void onFailRequest(long page_no) {
        failedPage = page_no;
        adapterRecent.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(getApplicationContext())) {
            showFailedView(true, getString(R.string.msg_no_network));
        } else {
            showFailedView(true, getString(R.string.msg_offline));
        }
    }

    private void requestAction(final long page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            adapterRecent.setLoading();
        }
        new Handler().postDelayed(() -> requestPostApi(page_no), Constant.DELAY_TIME);
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failedPage));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.msg_no_news);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            lytShimmer.setVisibility(View.VISIBLE);
            lytShimmer.startShimmer();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        lytShimmer.stopShimmer();
        adsManager.destroyBannerAd();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(BANNER_CATEGORY_DETAIL);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        adsManager.destroyBannerAd();
    }

}
