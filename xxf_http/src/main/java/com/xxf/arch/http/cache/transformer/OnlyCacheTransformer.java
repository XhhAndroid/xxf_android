package com.xxf.arch.http.cache.transformer;

import androidx.annotation.NonNull;

import com.xxf.arch.http.cache.RxHttpCache;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import retrofit2.Call;
import retrofit2.Response;

/**
 * @Description: 只从本地缓存中拿取, 没有缓存立即compelete Observale.empty
 * @Author: XGod  xuanyouwu@163.com  17611639080  https://github.com/NBXXF     https://blog.csdn.net/axuanqq
 * @CreateDate: 2020/11/24 10:53
 */
public class OnlyCacheTransformer<R> extends AbsCacheTransformer<R> {

    public OnlyCacheTransformer(@NonNull Call<R> call, RxHttpCache rxHttpCache) {
        super(call, rxHttpCache);
    }

    @Override
    public final ObservableSource<Response<R>> apply(Observable<Response<R>> remoteObservable) {
        return getCacheOrEmpty();
    }
}
