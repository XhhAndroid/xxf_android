
package com.xxf.arch.lifecycle;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * @author xuanyouwu@163.com
 * @version 2.3.1
 * @Description lifecycle观察
 * @date createTime：2018/9/7
 */
public class XXFSimpleLifecycleObserver implements LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    protected final void onEvent(LifecycleOwner owner, Lifecycle.Event event) {

    }
}
