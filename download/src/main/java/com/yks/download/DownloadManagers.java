package com.yks.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * @Description:
 * @Author: Yu ki-r
 * @CreateDate: 2021/1/8 20:26
 */
public class DownloadManagers {
    private volatile static DownloadManagers instance;
    private static String mDownloadPath;
    HashMap<String,ProgressDownSubscriber> submap;  //存放下载任务的集合，一个下载任务对应一个观察者
    private OkHttpClient okhttpClient;
    private String mBaseUrl;


    public static DownloadManagers getInstance() {
        if (instance == null) {
            synchronized (DownloadManagers.class) {
                if (instance == null){
                    instance = new DownloadManagers();
                }
            }
        }
        return instance;
    }

    private DownloadManagers() {
        submap = new HashMap<>();
    }

    /**
     * 开始下载
     * @param url  下载的url
     * @param callBack 下载回调接口
     */
    public void download(final String url, final DownloadCallBack callBack){
        //url为空或者正在下载
        if(url == null||submap.get(url)!=null){
            return ;
        }

        DownloadInterceptor interceptor = new DownloadInterceptor(callBack,url);
        okhttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .client(okhttpClient)
                .baseUrl(mBaseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        final ApiService service = retrofit.create(ApiService.class);

        ProgressDownSubscriber subscriber =
                Observable.just(url)
                        .flatMap(new Function<String, ObservableSource<DownloadInfo>>() {
                            public ObservableSource<DownloadInfo> apply(@NonNull String s) throws Exception {
                                return Observable.just(createDownInfo(s)); //创建下载实例
                            }
                        })
                        .map(new Function<DownloadInfo,DownloadInfo>(){
                            @Override
                            public DownloadInfo apply(@NonNull DownloadInfo downloadInfo) throws Exception {
                                return getRealFileName(downloadInfo); //判断是否有下载过
                            }
                        })
                        .flatMap(new Function<DownloadInfo, Observable<ResponseBody>>() {
                            @Override
                            public Observable<ResponseBody> apply(DownloadInfo downInfo) throws Exception {
                                return service.download("bytes=" + downInfo.getProgress() + "-", downInfo.getUrl());  //访问网络，获取ResponseBody
                            }
                        })
                        .map(new Function<ResponseBody, DownloadInfo>() {
                            @Override
                            public DownloadInfo apply(ResponseBody responsebody) {
                                try {
                                    return writeCache(responsebody, url); //写进内存
                                } catch (IOException e) {
                                    //*失败抛出异常*//
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(new ProgressDownSubscriber<DownloadInfo>(){
                            @Override
                            public void onNext(@NonNull DownloadInfo downloadInfo) {
                                callBack.onSuccess(downloadInfo);  //下载成功
                                submap.remove(downloadInfo.getUrl()); //移除这个任务
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                callBack.onFailure(e.getMessage());
                                submap.remove(url);
                            }
                        });
        submap.put(url, subscriber);//集合中加入一个url

    }

    /**
     * 创建一个下载实例
     *
     * @param url 请求网址
     * @return DownloadInfo
     */
    private DownloadInfo createDownInfo(String url) {
        DownloadInfo downloadInfo = new DownloadInfo(url);
        long contentLength = getContentLength(okhttpClient, url);//获得文件大小
        downloadInfo.setTotalSize(contentLength);
        String fileName = url.substring(url.lastIndexOf("/"));
        downloadInfo.setFileName(fileName);
        return downloadInfo;
    }
    /**
     * 获取下载长度
     *
     * @param downloadUrl
     * @param mClient
     * @return
     */
    private long getContentLength(OkHttpClient mClient, String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength == 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }

    /**
     * 获取临时的下载文件的名字
     * @param url
     * @return
     */
    public String getTemporaryName(String url) {
        return getTemporaryPath()+url.substring(url.lastIndexOf("/"));
    }

    /**
     * 获取临时的下载路径
     * @return 返回一个目录路径
     */
    public String getTemporaryPath() {
        return this.mDownloadPath;
    }


    /**
     * 返回一个下载实例
     * @param downloadInfo 下载实例
     * @return
     */
    private DownloadInfo getRealFileName(DownloadInfo downloadInfo) {
        String fileName = downloadInfo.getFileName();
        long downloadLength = 0, contentLength = downloadInfo.getTotalSize();
        File file = new File(getTemporaryName(downloadInfo.getUrl()));
        downloadInfo.setSavePath(getTemporaryName(downloadInfo.getUrl()));
        if (file.exists()) {
            //找到了文件,代表已经下载过,则获取其长度
            downloadLength = file.length();
        }
        //之前下载过,需要重新来一个文件
        if (downloadLength >= contentLength) {
            file.delete(); //先删除文件，后重新下载
        }
        file = new File(getTemporaryName(downloadInfo.getUrl()));
        //设置改变过的文件名/大小
        downloadInfo.setProgress(file.length());
        downloadInfo.setFileName(file.getName());
        downloadInfo.setSavePath(getTemporaryPath() + file.getName());
        return downloadInfo;
    }

    /**
     * 写入文件
     *
     * @param url
     * @throws IOException
     */
    private DownloadInfo writeCache(ResponseBody responsebody, String url) throws IOException {
        InputStream inputStream = null;
        RandomAccessFile raf = null;
        File file = new File(getTemporaryName(url));
        try {
            raf = new RandomAccessFile(getTemporaryName(url), "rw");
            inputStream = responsebody.byteStream();
            byte[] fileReader = new byte[4096];
            raf.seek(file.length());

            while (true) {
                int read = inputStream.read(fileReader);
                if (read == -1) {
                    break;
                }
                raf.write(fileReader, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        DownloadInfo downloadInfo = new DownloadInfo(url);
        downloadInfo.setSavePath(getTemporaryName(url));
        downloadInfo.setFileName(file.getName());
        downloadInfo.setProgress(file.length());
        return downloadInfo;
    }

    /**
     * 暂停下载
     */
    public void stop(String url) {
        if (url == null) return;
        if (submap.containsKey(url)) {
            ProgressDownSubscriber subscriber = submap.get(url);
            subscriber.dispose();
            submap.remove(url);
        }
    }

    /**
     * 下载路径
     * @param mDownloadPath
     * @return
     */
    public DownloadManagers downloadPath(String mDownloadPath, String baseUrl) {
        this.mDownloadPath = mDownloadPath;
        this.mBaseUrl = baseUrl;
        if (instance != null) {
            return instance;
        } else {
            return getInstance();
        }
    }


}
