package com.androidhuman.example.simplegithub.api

import com.androidhuman.example.simplegithub.api.model.GithubAccessToken
import retrofit2.Call;
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

/** API 정의하기 위한 인터페이스 생성
 *  : HTTP 통신을 위한 인터페이스
 */
interface AuthApi {
    @FormUrlEncoded
    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    fun getAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String
    ) : Call<GithubAccessToken>
    /**
     * @Field - POST에서만 동작하며 form-urlencoded로 데이터를 전송합니다. 이 메소드에는 @FormUrlEncoded 어노테이션이 추가되어야 합니다.
     */
}