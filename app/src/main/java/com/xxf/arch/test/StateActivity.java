package com.xxf.arch.test;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.xxf.arch.XXF;
import com.xxf.arch.activity.XXFActivity;
import com.xxf.arch.lifecycle.XXFFullLifecycleObserver;
import com.xxf.arch.lifecycle.XXFFullLifecycleObserverAdapter;
import com.xxf.arch.test.databinding.ActivityStateBinding;
import com.xxf.arch.test.databinding.ItemTestBinding;
import com.xxf.arch.utils.ToastUtils;
import com.xxf.arch.viewmodel.XXFViewModel;
import com.xxf.view.loading.ViewState;
import com.xxf.view.recyclerview.adapter.XXFRecyclerAdapter;
import com.xxf.view.recyclerview.adapter.XXFViewHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import android.app.Application;

public class StateActivity extends XXFActivity {

    ActivityStateBinding stateBinding;
    TestAdaper testAdaper;

    public static class TestViewModel extends XXFViewModel {

        public TestViewModel(@NonNull Application application) {
            super(application);
            Observable.interval(1, TimeUnit.SECONDS)
                    .compose(XXF.bindToLifecycle(this))
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            XXF.getLogger().d("=======>xxx:long:" + aLong);

                        }
                    });
        }

        @Override
        protected void onCleared() {
            super.onCleared();
            XXF.getLogger().d("=======>xxx:onclear");
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stateBinding = ActivityStateBinding.inflate(getLayoutInflater(), null, false);
        setContentView(stateBinding.getRoot());
        // TestViewModel viewModel = XXF.getViewModel(this, TestViewModel.class);

        stateBinding.recyclerView.setAdapter(testAdaper = new TestAdaper());
        stateBinding.btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XXF.getActivityStackProvider().restartApp();

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
        testAdaper.bindData(true, new ArrayList<>());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Observable.interval(50, TimeUnit.MILLISECONDS)
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        XXF.getLogger().d("============>rl cancel:");
                    }
                })
                //.compose(XXF.bindUntilEvent(this, Lifecycle.Event.ON_PAUSE))
                .as(XXF.autoDispose(this))
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        XXF.getLogger().d("============>rl:" + aLong);
                    }
                });

    }

    @Override
    protected void onPause() {
        super.onPause();
        XXF.getLogger().d("============>rl:pause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Toast.makeText(this,"abcd",Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ToastUtils.showToast("test state xxx", ToastUtils.ToastType.ERROR);
            }
        }, 2000);
        XXF.getLogger().d("=============isBack state stop:" + XXF.getActivityStackProvider().isBackground());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XXF.getLogger().d("============>rl:onDestroy");
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
                .compose(XXF.bindToLifecycle(this))
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
