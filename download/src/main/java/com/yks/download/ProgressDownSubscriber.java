package com.yks.download;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;

/**
 * @Description:
 * @Author: Yu ki-r
 * @CreateDate: 2021/1/8 20:27
 */
public class ProgressDownSubscriber<T> extends DisposableObserver<T> {

    public T downloadInfo;



    @Override
    public void onNext(@NonNull T t) {
        this.downloadInfo = t;
    }


    @Override
    public void onError(@NonNull Throwable e) {

    }

    @Override
    public void onComplete() {

    }
}