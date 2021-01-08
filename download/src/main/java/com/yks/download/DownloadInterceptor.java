package com.yks.download;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * @Description: 下载拦截器
 * @Author: Yu ki-r
 * @CreateDate: 2021/1/7 17:03
 */
public class DownloadInterceptor implements Interceptor {

    private DownloadCallBack downloadCallBack;

    private String downloadUrl;

    public DownloadInterceptor(DownloadCallBack downloadCallBack, String url) {
        this.downloadCallBack = downloadCallBack;
        this.downloadUrl = url;
    }


    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        Response response = chain.proceed(chain.request());

        return response.newBuilder()
                .body(new DownloadResponseBody(response.body(),downloadCallBack,downloadUrl))
                .build();
    }
}