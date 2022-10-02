package com.app.androidkrishiapp.adapter;

import static com.app.androidkrishiapp.utils.Constant.PAGER_NUMBER_DEFAULT;
import static com.app.androidkrishiapp.utils.Constant.PAGER_NUMBER_NO_VIDEO;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.app.androidkrishiapp.fragment.FragmentCategory;
import com.app.androidkrishiapp.fragment.FragmentFavorite;
import com.app.androidkrishiapp.fragment.FragmentRecent;
import com.app.androidkrishiapp.fragment.FragmentVideo;

@SuppressWarnings("ALL")
public class AdapterNavigation {

    public static class Default extends FragmentPagerAdapter {

        public Default(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return new FragmentRecent();
                case 1:
                    return new FragmentCategory();
                case 2:
                    return new FragmentVideo();
                case 3:
                    return new FragmentFavorite();
            }
            return null;
        }

        @Override
        public int getCount() {
            return PAGER_NUMBER_DEFAULT;
        }

    }

    public static class NoVideo extends FragmentPagerAdapter {

        public NoVideo(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return new FragmentRecent();
                case 1:
                    return new FragmentCategory();
                case 2:
                    return new FragmentFavorite();
            }
            return null;
        }

        @Override
        public int getCount() {
            return PAGER_NUMBER_NO_VIDEO;
        }

    }

}
