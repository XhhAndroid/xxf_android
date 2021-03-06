package com.xxf.arch.widget;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;

import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Description
 * <p>
 *
 * @Author: XGod  xuanyouwu@163.com  17611639080  https://github.com/NBXXF     https://blog.csdn.net/axuanqq
 * date createTime：2017/4/8
 * version 1.0.0
 */
public class BaseFragmentAdapter extends FragmentPagerAdapter {

    public enum Behavior {
        /**
         * Indicates that {@link Fragment#setUserVisibleHint(boolean)} will be called when the current
         * fragment changes.
         *
         * @see #FragmentPagerAdapter(FragmentManager, int)
         * @deprecated This behavior relies on the deprecated
         * {@link Fragment#setUserVisibleHint(boolean)} API. Use
         * {@link #BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT} to switch to its replacement,
         * {@link FragmentTransaction#setMaxLifecycle}.
         */
        BEHAVIOR_SET_USER_VISIBLE_HINT(0),

        /**
         * Indicates that only the current fragment will be in the {@link Lifecycle.State#RESUMED}
         * state. All other Fragments are capped at {@link Lifecycle.State#STARTED}.
         *
         * @see #FragmentPagerAdapter(FragmentManager, int)
         */
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT(1);
        int value;

        Behavior(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private final List<Fragment> fragmentsList = new ArrayList<Fragment>();
    private final List<CharSequence> mFragmentTitles = new ArrayList<CharSequence>();
    private final List<CharSequence> mFragmentTitlesFormat = new ArrayList<CharSequence>();
    private FragmentManager fm;

    public List<Fragment> getFragmentsList() {
        return fragmentsList;
    }


    public BaseFragmentAdapter(FragmentManager fm) {
        super(fm);
        this.fm = fm;
    }

    /**
     * FragmentTransaction#setMaxLifecycle}.控制碎片最大生命周期
     *
     * @param fm
     * @param behavior
     */
    public BaseFragmentAdapter(FragmentManager fm, Behavior behavior) {
        super(fm, behavior.getValue());
        this.fm = fm;
    }

    public void bindData(boolean isRefresh, List<? extends Fragment> datas) {
        if (datas == null) {
            return;
        }
        if (isRefresh) {
            this.fragmentsList.clear();
        }
        this.fragmentsList.addAll(datas);
        notifyDataSetChanged();
    }

    public void bindTitle(boolean isRefresh, List<? extends CharSequence> titles) {
        bindTitle(isRefresh, titles, Collections.emptyList());
    }

    public void bindTitle(boolean isRefresh, List<? extends CharSequence> titles, List<? extends CharSequence> formatTitles) {
        if (titles == null) {
            return;
        }
        if (isRefresh) {
            mFragmentTitles.clear();
            mFragmentTitlesFormat.clear();
        }
        mFragmentTitles.addAll(titles);
        mFragmentTitlesFormat.addAll(formatTitles);
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return fragmentsList.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object obj = super.instantiateItem(container, position);
        if (obj instanceof Fragment) {
            try {
                fragmentsList.set(position, (Fragment) obj);
            } catch (Exception e) {
            }
        }
        return obj;
    }

    private Fragment primaryItem;

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        primaryItem = (Fragment) object;
    }

    public Fragment getPrimaryItem() {
        return primaryItem;
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentsList.get(position);
    }

    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (!mFragmentTitlesFormat.isEmpty()) {
            return mFragmentTitlesFormat.get(position % mFragmentTitlesFormat.size());
        }
        if (mFragmentTitles.isEmpty()) {
            return "";
        }
        return mFragmentTitles.get(position % mFragmentTitles.size());
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        try {
            super.finishUpdate(container);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}

