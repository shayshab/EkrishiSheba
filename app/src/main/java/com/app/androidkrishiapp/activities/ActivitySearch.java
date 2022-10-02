package com.app.androidkrishiapp.activities;

import static com.app.androidkrishiapp.utils.Constant.BANNER_SEARCH;
import static com.app.androidkrishiapp.utils.Constant.INTERSTITIAL_POST_LIST;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.adapter.AdapterNews;
import com.app.androidkrishiapp.adapter.AdapterSearch;
import com.app.androidkrishiapp.callbacks.CallbackRecent;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.AdsPref;
import com.app.androidkrishiapp.database.prefs.SharedPref;
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

public class ActivitySearch extends AppCompatActivity {

    public static final String EXTRA_OBJC = "key.EXTRA_OBJC";
    private EditText edtSearch;
    private RecyclerView recyclerView;
    private AdapterNews adapterNews;
    private AdapterSearch adapterSearch;
    private LinearLayout lytSuggestion;
    private ImageButton btnClear;
    Call<CallbackRecent> callbackCall = null;
    private ShimmerFrameLayout lytShimmer;
    RecyclerView recyclerViewSuggestion;
    AdsPref adsPref;
    private int postTotal = 0;
    private int failedPage = 0;
    private AdsManager adsManager;
    SharedPref sharedPref;
    List<Post> posts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_search);
        Tools.setNavigation(this);
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        sharedPref = new SharedPref(this);

        initComponent();
        adapterNews.setOnLoadMoreListener(this::setLoadMore);
        setupToolbar();
        adsManager.loadBannerAd(BANNER_SEARCH);
        adsManager.loadInterstitialAd(INTERSTITIAL_POST_LIST, adsPref.getInterstitialAdInterval());

    }

    @SuppressLint("ClickableViewAccessibility")
    private void initComponent() {
        edtSearch = findViewById(R.id.et_search);
        btnClear = findViewById(R.id.bt_clear);
        btnClear.setVisibility(View.GONE);
        lytShimmer = findViewById(R.id.shimmer_view_container);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));

        recyclerViewSuggestion = findViewById(R.id.recyclerSuggestion);
        recyclerViewSuggestion.setLayoutManager(new LinearLayoutManager(this));

        edtSearch.addTextChangedListener(textWatcher);

        //set data and list adapter
        adapterNews = new AdapterNews(this, recyclerView, posts);
        recyclerView.setAdapter(adapterNews);

        adapterNews.setOnItemClickListener((view, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityPostDetail.class);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);
            adsManager.showInterstitialAd();
            adsManager.destroyBannerAd();
        });

        lytSuggestion = findViewById(R.id.lyt_suggestion);
        if (sharedPref.getIsDarkTheme()) {
            lytSuggestion.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
        } else {
            lytSuggestion.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
        }

        //set data and list adapter suggestion
        adapterSearch = new AdapterSearch(this);
        recyclerViewSuggestion.setAdapter(adapterSearch);
        showSuggestionSearch();
        adapterSearch.setOnItemClickListener((view, viewModel, pos) -> {
            edtSearch.setText(viewModel);
            edtSearch.setSelection(viewModel.length());
            lytSuggestion.setVisibility(View.GONE);
            hideKeyboard();
            searchAction(1);
        });

        adapterSearch.setOnItemActionClickListener((view, viewModel, pos) -> {
            edtSearch.setText(viewModel);
            edtSearch.setSelection(viewModel.length());
        });

        btnClear.setOnClickListener(v -> {
            lytSuggestion.setVisibility(View.GONE);
            edtSearch.setText("");
        });

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                searchAction(1);
                return true;
            }
            return false;
        });

        edtSearch.setOnTouchListener((view, motionEvent) -> {
            showSuggestionSearch();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            return false;
        });

    }

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPref sharedPref = new SharedPref(this);
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            findViewById(R.id.bg_view).setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            findViewById(R.id.bg_view).setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            if (c.toString().trim().length() == 0) {
                btnClear.setVisibility(View.GONE);
            } else {
                btnClear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void requestSearchApi(final String query, final int page_no) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        if (AppConfig.ENABLE_RTL_MODE) {
            callbackCall = apiInterface.getSearchPostsRTL(AppConfig.REST_API_KEY, query, page_no, AppConfig.LOAD_MORE);
        } else {
            callbackCall = apiInterface.getSearchPosts(AppConfig.REST_API_KEY, query, page_no, AppConfig.LOAD_MORE);
        }
        callbackCall.enqueue(new Callback<CallbackRecent>() {
            @Override
            public void onResponse(@NonNull Call<CallbackRecent> call, @NonNull Response<CallbackRecent> response) {
                CallbackRecent resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    postTotal = resp.count_total;
                    displayApiResult(resp.posts);
                } else {
                    onFailRequest(page_no);
                }
                swipeProgress(false);
            }

            @Override
            public void onFailure(@NonNull Call<CallbackRecent> call, @NonNull Throwable t) {
                onFailRequest(page_no);
                swipeProgress(false);
            }

        });
    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterNews.getItemCount() - current_page);
        if (postTotal > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            searchAction(next_page);
        } else {
            adapterNews.setLoaded();
        }
    }

    private void displayApiResult(final List<Post> posts) {
        adapterNews.insertDataWithNativeAd(posts);
        swipeProgress(false);
        if (posts.size() == 0) {
            showNotFoundView(true);
        }
    }

    private void onFailRequest(int page_no) {
        failedPage = page_no;
        adapterNews.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(this)) {
            showFailedView(true, getString(R.string.msg_no_network));
        } else {
            showFailedView(true, getString(R.string.msg_offline));
        }
    }

    private void searchAction(final int page_no) {
        lytSuggestion.setVisibility(View.GONE);
        showFailedView(false, "");
        showNotFoundView(false);
        final String query = edtSearch.getText().toString().trim();
        if (!query.equals("")) {
            if (page_no == 1) {
                adapterSearch.addSearchHistory(query);
                adapterNews.resetListData();
                swipeProgress(true);
            } else {
                adapterNews.setLoading();
            }
            new Handler().postDelayed(() -> requestSearchApi(query, page_no), Constant.DELAY_TIME);
        } else {
            Toast.makeText(this, R.string.msg_search_input, Toast.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    private void showSuggestionSearch() {
        adapterSearch.refreshItems();
        lytSuggestion.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
        findViewById(R.id.failed_retry).setOnClickListener(view -> searchAction(failedPage));
    }

    private void showNotFoundView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.msg_no_news_found);
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
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
            return;
        } else {
            lytShimmer.setVisibility(View.VISIBLE);
            lytShimmer.startShimmer();
        }
    }

    @Override
    public void onBackPressed() {
        if (edtSearch.length() > 0) {
            edtSearch.setText("");
            //lytSuggestion.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
            adsManager.destroyBannerAd();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(BANNER_SEARCH);
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

}
