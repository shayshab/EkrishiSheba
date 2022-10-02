package com.app.androidkrishiapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.app.androidkrishiapp.BuildConfig;
import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.database.sqlite.DbHandler;
import com.app.androidkrishiapp.models.Post;
import com.app.androidkrishiapp.utils.AppBarLayoutBehavior;
import com.app.androidkrishiapp.utils.Constant;
import com.app.androidkrishiapp.utils.Tools;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class ActivityPostDetailOffline extends AppCompatActivity {

    private Post post;
    View parentView, lytParent;
    private Menu menu;
    TextView txtTitle, txtCategory, txtDate, txtCommentCount, txtCommentText, txtViewCount;
    ImageView imgThumbVideo;
    LinearLayout btnComment, btnView;
    private WebView webView;
    DbHandler dbHandler;
    private String singleChoiceSelected;
    SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_post_detail_offline);
        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);
        dbHandler = new DbHandler(getApplicationContext());

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        parentView = findViewById(android.R.id.content);
        webView = findViewById(R.id.news_description);
        lytParent = findViewById(R.id.lyt_parent);

        txtTitle = findViewById(R.id.title);
        txtCategory = findViewById(R.id.category);
        txtDate = findViewById(R.id.date);
        txtCommentCount = findViewById(R.id.txt_comment_count);
        txtCommentText = findViewById(R.id.txt_comment_text);
        txtViewCount = findViewById(R.id.txt_view_count);
        btnComment = findViewById(R.id.btn_comment);
        btnView = findViewById(R.id.btn_view);
        imgThumbVideo = findViewById(R.id.thumbnail_video);

        btnComment.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ActivityComments.class);
            intent.putExtra("nid", post.nid);
            intent.putExtra("count", post.comments_count);
            startActivity(intent);
        });

        txtCommentText.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ActivityComments.class);
            intent.putExtra("nid", post.nid);
            intent.putExtra("count", post.comments_count);
            startActivity(intent);
        });

        // get extra object
        post = (Post) getIntent().getSerializableExtra(Constant.EXTRA_OBJC);

        initToolbar();

        displayData();

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

    private void displayData() {
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
        }, 1000);

        Tools.displayPostDescription(this, webView, post.news_description);

        txtCategory.setText(post.category_name);
        txtCategory.setBackgroundColor(ContextCompat.getColor(this, R.color.colorCategory));

        if (AppConfig.ENABLE_DATE_DISPLAY) {
            txtDate.setVisibility(View.VISIBLE);
            findViewById(R.id.lyt_date).setVisibility(View.VISIBLE);
        } else {
            txtDate.setVisibility(View.GONE);
            findViewById(R.id.lyt_date).setVisibility(View.GONE);
        }
        txtDate.setText(Tools.getFormatedDate(post.news_date));

        ImageView news_image = findViewById(R.id.image);

        if (post.content_type != null && post.content_type.equals("youtube")) {
            Glide.with(this)
                    .load(Constant.YOUTUBE_IMG_FRONT + post.video_id + Constant.YOUTUBE_IMG_BACK)
                    .placeholder(R.drawable.ic_thumbnail)
                    .thumbnail(Tools.RequestBuilder(this))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(news_image);

            news_image.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), ActivityYoutubePlayer.class);
                intent.putExtra("video_id", post.video_id);
                startActivity(intent);
            });

        } else if (post.content_type != null && post.content_type.equals("Url")) {
            Glide.with(this)
                    .load(sharedPref.getBaseUrl() + "/upload/" + post.news_image.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_thumbnail)
                    .thumbnail(Tools.RequestBuilder(this))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(news_image);

            news_image.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), ActivityVideoPlayer.class);
                intent.putExtra("video_url", post.video_url);
                startActivity(intent);
            });
        } else if (post.content_type != null && post.content_type.equals("Upload")) {
            Glide.with(this)
                    .load(sharedPref.getBaseUrl() + "/upload/" + post.news_image.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_thumbnail)
                    .thumbnail(Tools.RequestBuilder(this))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(news_image);

            news_image.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), ActivityVideoPlayer.class);
                intent.putExtra("video_url", sharedPref.getBaseUrl() + "/upload/video/" + post.video_url);
                startActivity(intent);
            });
        } else {
            Glide.with(this)
                    .load(sharedPref.getBaseUrl() + "/upload/" + post.news_image.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_thumbnail)
                    .thumbnail(Tools.RequestBuilder(this))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(news_image);

            news_image.setOnClickListener(view -> {
                Intent intent = new Intent(getApplicationContext(), ActivityFullScreenImage.class);
                intent.putExtra("image", post.news_image);
                startActivity(intent);
            });
        }

        if (!post.content_type.equals("Post")) {
            imgThumbVideo.setVisibility(View.VISIBLE);
        } else {
            imgThumbVideo.setVisibility(View.GONE);
        }

        if (!sharedPref.getLoginFeature().equals("yes")) {
            btnComment.setVisibility(View.GONE);
            txtCommentText.setVisibility(View.GONE);
        } else {
            btnComment.setVisibility(View.VISIBLE);
            txtCommentText.setVisibility(View.VISIBLE);
        }

        if (AppConfig.ENABLE_VIEW_COUNT) {
            if (Tools.isConnect(this)) {
                btnView.setVisibility(View.VISIBLE);
                txtViewCount.setText("" + Tools.withSuffix(post.view_count));
            } else {
                btnView.setVisibility(View.GONE);
            }
        } else {
            btnView.setVisibility(View.GONE);
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
                AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityPostDetailOffline.this);
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
                sendIntent.putExtra(Intent.EXTRA_TEXT, post.news_title + "\n\n" + getResources().getString(R.string.share_content) + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);

                break;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

}
