package com.yks.download;


import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * @Description:
 * @Author: Yu ki-r
 * @CreateDate: 2021/1/7 18:29
 */
public interface ApiService {

    @Streaming
    @GET
    Observable<ResponseBody> download(@Header("range")String start, @Url String url);
}