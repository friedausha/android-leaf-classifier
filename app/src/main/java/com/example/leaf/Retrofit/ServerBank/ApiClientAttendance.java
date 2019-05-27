package com.example.leaf.Retrofit.ServerBank;

import com.example.leaf.Response.ResponseApi;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiClientAttendance {
    @FormUrlEncoded
    @POST("/upload/")
    Call<ResponseApi> upload(@Field("name") String name,
                             @Field("image") String image);

    @POST("/train/")
    Call<ResponseApi> train();

    @FormUrlEncoded
    @POST("/predict/")
    Call<ResponseApi> predict(@Field("image") String image);
}
