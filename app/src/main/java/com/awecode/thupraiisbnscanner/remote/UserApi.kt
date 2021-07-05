package com.retrofitcoroutines.example.remote

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path


interface UserApi {

    @GET("/isbn/9780721416212.json")
     fun getResponse(): Call<JsonObject>

    @GET("/isbn/{isbnNumber}.json")
    fun readJson(@Path("isbnNumber") isbnNumber: String): Call<JsonObject>



}