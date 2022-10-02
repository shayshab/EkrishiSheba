package com.app.androidkrishiapp.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.activities.MyApplication;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.Comments;
import com.app.androidkrishiapp.utils.Tools;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("NotifyDataSetChanged")
public class AdapterComments extends RecyclerView.Adapter<AdapterComments.ViewHolder> {

    Context context;
    private List<Comments> comments;
    MyApplication myApplication;
    private OnItemClickListener mOnItemClickListener;
    SharedPref sharedPref;

    public interface OnItemClickListener {
        void onItemClick(View view, Comments obj, int position, Context context);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterComments(Context context, List<Comments> comments) {
        this.comments = comments;
        this.context = context;
        this.sharedPref = new SharedPref(context);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView user_name;
        public ImageView user_image;
        public TextView comment_date;
        public TextView comment_message;
        public LinearLayout lyt_parent;

        public ViewHolder(View v) {
            super(v);
            user_name = v.findViewById(R.id.user_name);
            user_image = v.findViewById(R.id.user_image);
            comment_date = v.findViewById(R.id.comment_date);
            comment_message = v.findViewById(R.id.edt_comment_message);
            lyt_parent = v.findViewById(R.id.lyt_parent);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.lsv_item_comments, parent, false);
        ViewHolder vh = new ViewHolder(v);
        myApplication = MyApplication.getInstance();
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Comments c = comments.get(position);

        if (myApplication.getIsLogin() && myApplication.getUserId().equals(c.user_id)) {
            holder.user_name.setText(Tools.usernameFormatter(c.name) + " ( " + context.getResources().getString(R.string.txt_you) + " )");
        } else {
            holder.user_name.setText(Tools.usernameFormatter(c.name));
        }

        Glide.with(context)
                .load(sharedPref.getBaseUrl() + "/upload/avatar/" + c.image.replace(" ", "%20"))
                .placeholder(R.drawable.ic_user_account)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .thumbnail(Tools.RequestBuilder(context))
                .apply(new RequestOptions().override(200, 200))
                .centerCrop()
                .into(holder.user_image);

        holder.comment_date.setText(Tools.getTimeAgo(c.date_time));
        holder.comment_message.setText(c.content);

        holder.lyt_parent.setOnClickListener(view -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(view, c, position, context);
            }
        });
    }

    public void setListData(List<Comments> items) {
        this.comments = items;
        notifyDataSetChanged();
    }

    public void resetListData() {
        this.comments = new ArrayList<>();
        notifyDataSetChanged();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return comments.size();
    }

}