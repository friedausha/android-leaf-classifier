package com.example.leaf.Response;

import com.google.gson.annotations.SerializedName;

public class ResponseApi {
    @SerializedName("msg")
    String msg;

    public String getMsg() {
        return msg;
    }
}
