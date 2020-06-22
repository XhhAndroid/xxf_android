package com.xxf.arch.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.xxf.arch.test.databinding.ActivityStateBinding;
import com.xxf.arch.test.databinding.ItemTestBinding;
import com.xxf.arch.utils.ToastUtils;
import com.xxf.view.loading.ViewState;
import com.xxf.view.recyclerview.adapter.XXFRecyclerAdapter;
import com.xxf.view.recyclerview.adapter.XXFViewHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class StateActivity extends Activity {

    ActivityStateBinding stateBinding;
    TestAdaper testAdaper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stateBinding = ActivityStateBinding.inflate(getLayoutInflater(), null, false);
        setContentView(stateBinding.getRoot());

        stateBinding.recyclerView.setAdapter(testAdaper = new TestAdaper());
        stateBinding.btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });
        stateBinding.stateLayout.setErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });
        stateBinding.stateLayout.setEmptyActionText("点击", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtils.showToast("点击了!", ToastUtils.ToastType.ERROR);
                stateBinding.stateLayout.setEmptyText(null);
            }
        });
        Log.d("====", "11111");
        testAdaper.bindData(true, new ArrayList<>());
        Log.d("====", "5555");
    }

    private void loadData() {
        stateBinding.stateLayout.setViewState(ViewState.VIEW_STATE_LOADING);
        Observable.fromCallable(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {

                }

                if (System.currentTimeMillis() % 3 == 0) {
                    return Arrays.asList("1", "2", "3");
                }
                return new ArrayList<>();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> strings) throws Exception {
                        Log.d("=========", "" + strings);
                        testAdaper.bindData(true, strings);
                    }
                });
    }

    class TestAdaper extends XXFRecyclerAdapter<ItemTestBinding, String> {

        @Override
        protected ItemTestBinding onCreateBinding(LayoutInflater inflater, ViewGroup viewGroup, int viewType) {
            return ItemTestBinding.inflate(inflater, viewGroup, false);
        }

        @Override
        public void onBindHolder(XXFViewHolder<ItemTestBinding, String> holder, @Nullable String s, int index) {

        }
    }
}
