package cn.jiiiiiin.vplus.core.update;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.alibaba.fastjson.JSONException;
import com.vector.update_app.HttpManager;
import com.zhy.http.okhttp.callback.FileCallBack;

import java.io.File;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.WeakHashMap;

import cn.jiiiiiin.vplus.core.net.RestOkHttpUtilsBuilder;
import cn.jiiiiiin.vplus.core.net.RestOkHttpUtilsClient;
import cn.jiiiiiin.vplus.core.util.log.LoggerProxy;
import okhttp3.Call;
import okhttp3.Request;

/**
 * @author jiiiiiin
 */

public class UpdateAppHttpUtil implements HttpManager , Serializable {

    public UpdateAppHttpUtil() {
    }

    /**
     * 异步get
     *
     * @param url      get请求地址
     * @param params   get参数
     * @param callBack 回调
     */
    @Override
    public void asyncGet(@NonNull String url, @NonNull Map<String, String> params, @NonNull final Callback callBack) {
        RestOkHttpUtilsBuilder restClientBuilder = RestOkHttpUtilsClient.builder()
                .ignoreCommonCheck()
                // loader 由前端控制
                //.loader()
                .url(url);
        restClientBuilder.params(new WeakHashMap<>(params));
        // 业务判断由前端完成
        restClientBuilder.ignoreCommonCheck()
                .success(response -> callBack.onResponse(response.toJSONString()))
                .error(errRes -> callBack.onError(String.format("请求失败，请稍后尝试[%s]", errRes)))
                .build()
                .get();
    }

    /**
     * 异步post
     *
     * @param url      post请求地址
     * @param params   post请求参数
     * @param callBack 回调
     */
    @Override
    public void asyncPost(@NonNull String url, @NonNull Map<String, String> params, @NonNull final Callback callBack) {
        RestOkHttpUtilsBuilder restClientBuilder = RestOkHttpUtilsClient.builder()
                .ignoreCommonCheck()
                // loader 由前端控制
                //.loader()
                .url(url);
        restClientBuilder.params(new WeakHashMap<>(params));
        // 业务判断由前端完成
        restClientBuilder.ignoreCommonCheck()
                .success(response -> callBack.onResponse(response.toJSONString()))
                .failure(()-> callBack.onError("网络异常请稍后尝试"))
                .error(errRes -> callBack.onError(String.format("请求失败，请稍后尝试[%s]", errRes)))
                .build()
                .post();
    }

    /**
     * 下载
     *
     * @param url      下载地址
     * @param path     文件保存路径
     * @param fileName 文件名称
     * @param callback 回调
     */
    @Override
    public void download(@NonNull String url, @NonNull String path, @NonNull String fileName, @NonNull final FileCallback callback) {
        RestOkHttpUtilsClient.builder()
                .ignoreCommonCheck()
                // loader 由外层控制
                //.loader()
                .url(url)
                .failure(()-> callback.onError("网络异常请稍后尝试"))
                .error(errRes -> callback.onError(String.format("请求失败，请稍后尝试[%s]", errRes)))
                .fileCallBack(new FileCallBack(path, fileName) {
                    @Override
                    public void inProgress(float progress, long total, int id) {
                        callback.onProgress(progress, total);
                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        LoggerProxy.e(e, "下载文件出错");
                        callback.onError(validateError(e));
                    }

                    @Override
                    public void onResponse(File response, int id) {
                        callback.onResponse(response);
                    }

                    @Override
                    public void onBefore(Request request, int id) {
                        super.onBefore(request, id);
                        callback.onBefore();
                    }
                })
                .build()
                .download();
    }


    private String validateError(Exception error) {
        if (error != null) {
            if (error instanceof java.net.SocketException) {
                return "网络连接超时，请不要在更新的时候切换网络，请确保网络流畅再继续操作";
            }if (error instanceof SocketTimeoutException) {
                return "网络连接超时，请稍候重试";
            } else if (error instanceof JSONException) {
                return "json转化异常";
            } else if (error instanceof ConnectException) {
                return "服务器网络异常或宕机，请稍候重试";
            } else {
                return String.format("未知异常，请稍候重试[%s]", error.getMessage());
            }
        } else {
            return "请求出错请稍后尝试";
        }
    }
}