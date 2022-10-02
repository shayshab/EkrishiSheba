package com.app.androidkrishiapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.app.androidkrishiapp.activities.ActivityCategoryDetails;
import com.app.androidkrishiapp.activities.MainActivity;
import com.app.androidkrishiapp.adapter.AdapterCategory;
import com.app.androidkrishiapp.callbacks.CallbackCategories;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.Category;
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

public class FragmentCategory extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdapterCategory adapterCategory;
    private Call<CallbackCategories> callbackCall = null;
    private ShimmerFrameLayout lytShimmer;
    SharedPref sharedPref;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_category, container, false);

        if (getActivity() != null)
            sharedPref = new SharedPref(getActivity());

        lytShimmer = rootView.findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout_category);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        recyclerView = rootView.findViewById(R.id.recyclerViewCategory);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));

        //set data and list adapter
        adapterCategory = new AdapterCategory(getActivity(), new ArrayList<>());
        recyclerView.setAdapter(adapterCategory);

        // on item list clicked
        adapterCategory.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityCategoryDetails.class);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
            ((MainActivity) getActivity()).showInterstitialAd();
            ((MainActivity) getActivity()).destroyBannerAd();
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            adapterCategory.resetListData();
            requestAction();
        });

        requestAction();

        return rootView;
    }

    private void displayApiResult(final List<Category> categories) {
        adapterCategory.setListData(categories);
        swipeProgress(false);
        if (categories.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestCategoriesApi() {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        callbackCall = apiInterface.getAllCategories(AppConfig.REST_API_KEY);
        callbackCall.enqueue(new Callback<CallbackCategories>() {
            @Override
            public void onResponse(@NonNull Call<CallbackCategories> call, @NonNull Response<CallbackCategories> response) {
                CallbackCategories resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    displayApiResult(resp.categories);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackCategories> call, @NonNull Throwable t) {
                if (!call.isCanceled()) onFailRequest();
            }

        });
    }

    private void onFailRequest() {
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.msg_no_network));
        } else {
            showFailedView(true, getString(R.string.msg_offline));
        }
    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        showNoItemView(false);
        new Handler().postDelayed(this::requestCategoriesApi, Constant.DELAY_TIME);
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

    private void showFailedView(boolean flag, String message) {
        View lyt_failed = rootView.findViewById(R.id.lyt_failed_category);
        ((TextView) rootView.findViewById(R.id.failed_message)).setText(message);
        if (flag) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        rootView.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = rootView.findViewById(R.id.lyt_no_item_category);
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.msg_no_category);
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

}
