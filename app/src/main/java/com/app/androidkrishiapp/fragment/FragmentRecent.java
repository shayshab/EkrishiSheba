package com.app.androidkrishiapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.activities.ActivityPostDetail;
import com.app.androidkrishiapp.activities.MainActivity;
import com.app.androidkrishiapp.adapter.AdapterRecent;
import com.app.androidkrishiapp.callbacks.CallbackRecent;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.Post;
import com.app.androidkrishiapp.rests.ApiInterface;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.Constant;
import com.app.androidkrishiapp.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentRecent extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private AdapterRecent adapterRecent;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Call<CallbackRecent> callbackCall = null;
    private int postTotal = 0;
    private int failedPage = 0;
    List<Post> posts = new ArrayList<>();
    ShimmerFrameLayout lytShimmer;
    View lytShimmerHead;
    SharedPref sharedPref;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        if (getActivity() != null) {
            sharedPref = new SharedPref(getActivity());
        }

        initShimmerView();

        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout_home);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));

        //set data and list adapter
        adapterRecent = new AdapterRecent(getActivity(), recyclerView, posts);
        recyclerView.setAdapter(adapterRecent);

        // on item list clicked
        adapterRecent.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityPostDetail.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            ((MainActivity) getActivity()).showInterstitialAd();
            ((MainActivity) getActivity()).destroyBannerAd();
        });

        // detect when scroll reach bottom
        adapterRecent.setOnLoadMoreListener(this::setLoadMore);

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterRecent.resetListData();
            requestAction(1);
        });

        requestAction(1);

        if (AppConfig.DISPLAY_HEADER_VIEW && !AppConfig.BIG_NEWS_LIST_STYLE) {
            recyclerView.setPadding(0, 0, 0, getResources().getDimensionPixelOffset(R.dimen.padding_small));
        } else {
            recyclerView.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.padding_small), 0, getResources().getDimensionPixelOffset(R.dimen.padding_small));
        }

        return rootView;
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

    private void displayApiResult(final List<Post> posts) {
        adapterRecent.insertDataWithNativeAd(posts);
        swipeProgress(false);
        if (posts.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListPostApi(final int page_no) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        callbackCall = apiInterface.getRecentPost(AppConfig.REST_API_KEY, page_no, AppConfig.LOAD_MORE);
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
            }

            @Override
            public void onFailure(@NonNull Call<CallbackRecent> call, @NonNull Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void onFailRequest(int page_no) {
        failedPage = page_no;
        adapterRecent.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.msg_no_network));
        } else {
            showFailedView(true, getString(R.string.msg_offline));
        }
    }

    private void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            adapterRecent.setLoading();
        }
        new Handler().postDelayed(() -> requestListPostApi(page_no), Constant.DELAY_TIME);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        lytShimmer.stopShimmer();
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = rootView.findViewById(R.id.lyt_failed_home);
        ((TextView) rootView.findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        rootView.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failedPage));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = rootView.findViewById(R.id.lyt_no_item_home);
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.msg_no_news);
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

    private void initShimmerView() {
        lytShimmer = rootView.findViewById(R.id.shimmer_view_container);
        lytShimmerHead = rootView.findViewById(R.id.lyt_shimmer_head);
        if (!AppConfig.DISPLAY_HEADER_VIEW) {
            lytShimmerHead.setVisibility(View.GONE);
        }
        if (AppConfig.BIG_NEWS_LIST_STYLE) {
            rootView.findViewById(R.id.lytShimmerDefault).setVisibility(View.GONE);
            rootView.findViewById(R.id.lytShimmerBig).setVisibility(View.VISIBLE);
        } else {
            rootView.findViewById(R.id.lytShimmerDefault).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.lytShimmerBig).setVisibility(View.GONE);
        }
    }

}

