package com.yks.download;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * @Description:
 * @Author: Yu ki-r
 * @CreateDate: 2021/1/8 20:27
 */
public class DownloadResponseBody extends ResponseBody {

    private ResponseBody responseBody;
    private DownloadCallBack callBack;

    private BufferedSource bufferedSource;

    private String url;


    public DownloadResponseBody(ResponseBody responseBody, DownloadCallBack callBack, String url) {
        this.responseBody = responseBody;
        this.callBack = callBack;
        this.url = url;

    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @NotNull
    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source){ //包装source，并在ForwardingSource的read()方法内实现接口回调
            long totalBytesRead = 0L;
            File file = new File(DownloadManagers.getInstance().getTemporaryName(url));

            @Override
            public long read(@NotNull Buffer sink, long byteCount) throws IOException {
                long bytesRead =super.read(sink, byteCount);
                totalBytesRead+= bytesRead!=-1?bytesRead:0;
                if (callBack!=null) {
                    if (bytesRead!=-1){
                        long localSize = file.length();  //本地已经下载好的长度
                        long trueTotalSize = localSize+responseBody.contentLength() - totalBytesRead; //文件的真实长度
                        callBack.onProgress(trueTotalSize,localSize);
                    }
                }
                return bytesRead;
            }
        };
    }
}
