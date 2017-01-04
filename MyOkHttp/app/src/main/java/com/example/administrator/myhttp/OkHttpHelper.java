package com.example.administrator.myhttp;

import android.os.Handler;
import android.os.StrictMode;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by yang-jianzheng on 2016/5/4
 */
public class OkHttpHelper {
    private static OkHttpHelper mInstance = new OkHttpHelper();
    private final OkHttpClient mOkHttpClient;
    private Handler mDelivery = new Handler();

    private OkHttpHelper() {//私有构造,把工具类定义为单例,让程序中只有一个对象
        //1.创建OkHttpClient对象
        //.cache(new Cache(file,size))//设置数据缓存的,文件目录和最大大小
        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.SECONDS)//读的超时时间
                .writeTimeout(15, TimeUnit.SECONDS)
                //.cache(new Cache(file,size))//设置数据缓存的
                .build();
    }

    public static OkHttpHelper create() {
        return mInstance;
    }

    /**
     * 异步执行get请求,没有请求头的情况,将响应体的数据转为string
     * @param url
     * @param callback
     */
    public void execGet(String url, HttpCallback callback) {
        execGet(url, null, callback);
    }

    /***
     * 异步执行get请求,有请求头的情况,将响应体的数据转为string
     * @param url
     * @param callback 回调
     */
    public void execGet(String url, HashMap<String, String> headers, final HttpCallback callback) {//这个是自定义的接口,也可以把方法设置为okhttp的callback
        //2.创建请求对象Request
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();//设置请求方式是get
        //添加header(通过参数传过来的请求头),可能有多个header,所以定义map,遍历并取出设置
        if (headers != null && !headers.isEmpty()) {
            Iterator<Map.Entry<String, String>> iterator = headers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = builder.build();
        //3.执行请求
        Call call = mOkHttpClient.newCall(request);
        //执行请求，这个方式是同步请求的方式
        //Response response = call.execute();

        //执行异步请求的方式(会开子线程),所以下面的代码都运行在子线程,除了handler处理过的(如果调用下面方法的地方就是在子线程,会报错,必须在主线程调用)
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                e.printStackTrace();
                mDelivery.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onFail(e);//回调接口的方法
                        }
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //获取http响应体数据
                ResponseBody body = response.body();
                //将响应体的数据转为string(也可以转换为别的类型),千万别写成tostring
                final String string = body.string();
                //让接口的方法运行在主线程,方便外界回调接口时直接更新ui
                mDelivery.post(new Runnable() {
                    @Override
                    public void run() {
                        //将数据传递给外界
                        if (callback != null) {
                            callback.onSuccess(string);//回调接口的方法
                        }
                    }
                });
            }
        });
    }

    /**
     * 定义一个接口,把请求网络的返回数据转化为string后,通过接口传给外界
     */
    public interface HttpCallback {
        void onSuccess(String data);

        void onFail(Exception e);
    }

    /**
     * 同步执行get请求,返回结果直接转换成字符串返回
     *
     * @param url
     * @return
     */
    public String syncGet(String url) {
        //主线程中使用该工具类同步请求网络必须加下列代码,如果在子线程中调用该工具类方法,不用加
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());

        //2.创建请求对象Request
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();//设置请求方式是get
        Request request = builder.build();

        //3.执行请求
        Call call = mOkHttpClient.newCall(request);
        //执行请求，这个方式是同步请求的方式
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * POST提交Json数据
     */
    public String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }


    public static final MediaType MEDIA_TYPE_MARKDOWN
            = MediaType.parse("text/x-markdown; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Post方式提交String
     * 例子提交了一个markdown文档到web服务，以HTML方式渲染markdown。
     * 因为整个请求体都在内存中，因此避免使用此api提交大文档（大于1MB）。
     */
    public void postString( String postBody) throws Exception {
//        String postBody = ""
//                + "Releases\n"
//                + "--------\n"
//                + "\n"
//                + " * _1.0_ May 6, 2013\n"
//                + " * _1.1_ June 15, 2013\n"
//                + " * _1.2_ August 11, 2013\n";

        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(RequestBody.create(MEDIA_TYPE_MARKDOWN, postBody))
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful())
            throw new IOException("Unexpected code " + response);
        System.out.println(response.body().string());
    }


    /**
     * Post方式提交流
     * 以流的方式POST提交请求体。请求体的内容由流写入产生。
     * 例子是流直接写入Okio的BufferedSink。你的程序可能会使用OutputStream，你可以使用BufferedSink.outputStream()来获取。
     *
     * @throws Exception
     */
    public void postStream(RequestBody requestBody) throws Exception {
//        RequestBody requestBody = new RequestBody() {
//            @Override
//            public MediaType contentType() {
//                return MEDIA_TYPE_MARKDOWN;
//            }
//
//            @Override
//            public void writeTo(BufferedSink sink) throws IOException {
//                sink.writeUtf8("Numbers\n");
//                sink.writeUtf8("-------\n");
//                for (int i = 2; i <= 997; i++) {
//                    sink.writeUtf8(String.format(" * %s = %s\n", i, factor(i)));
//                }
//            }
//
//            private String factor(int n) {
//                for (int i = 2; i < n; i++) {
//                    int x = n / i;
//                    if (x * i == n)
//                        return factor(x) + " × " + i;
//                }
//                return Integer.toString(n);
//            }
//        };

        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful())
            throw new IOException("Unexpected code " + response);
        System.out.println(response.body().string());
    }

    /**
     * Post方式提交文件
     * 以文件作为请求体
     * @throws Exception
     */
    public void postFile(File file) throws Exception {
//        File file = new File("README.md");
        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(RequestBody.create(MEDIA_TYPE_MARKDOWN, file))
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful())
            throw new IOException("Unexpected code " + response);
        System.out.println(response.body().string());
    }
}
