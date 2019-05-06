package com.androidhuman.example.simplegithub.api

import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.model.RepoSearchResponse
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubApi {
    @GET("search/repositories")
    fun searchRepository(@Query("q") query: String) : Observable<RepoSearchResponse> // [syk][RxJava] Retrofit으로 받은 응답반환 형태를 Observable로 변환

    @GET("repos/{owner}/{name}")
    fun getRepository(@Path("owner") ownerLogin: String, @Path("name") repoName: String) : Observable<GithubRepo> // [syk][RxJava] Retrofit으로 받은 응답반환 형태를 Observable로 변환
}