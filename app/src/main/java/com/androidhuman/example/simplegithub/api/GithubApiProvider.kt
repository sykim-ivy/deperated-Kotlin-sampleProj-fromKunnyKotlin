package com.androidhuman.example.simplegithub.api

import android.content.Context
import com.androidhuman.example.simplegithub.data.AuthTokenProvider
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.IllegalStateException


object GithubApiProvider { // [miss] 코틀린 싱글톤 객체는 object로 선언!!!
    // object 내 선언된 값이나 함수는 자바의 static 멤버와 동일한 방법으로 사용

    /**
     * [Github OAuth API] Retrofit 객체 생성
     */
    fun provideAuthApi(): AuthApi {
        return Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(provideOkHttpClient(provideLoggingInterceptor(), null))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java) // << API 통신을 위해 정의한 인터페이스를 Retrofit에 초기화 // [miss]Study : '.class' -> '::class.java' 로 사용
    }

    /**
     * [Github Search API] Retrofit 객체 생성
     */
    fun provideGithubApi(context: Context): GithubApi {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(provideOkHttpClient(provideLoggingInterceptor(),
                provideAuthInterceptor(provideAuthTokenProvider(context))))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApi::class.java) // << API 통신을 위해 정의한 인터페이스를 Retrofit에 초기화  // [miss]Study : '.class' -> '::class.java' 로 사용
    }

    /**
     * Retrofit 객체 생성시 사용할 OkHttpClient 객체 생성
     *  // [miss] private
     */
    private fun provideOkHttpClient(interceptor: HttpLoggingInterceptor, authInterceptor: AuthInterceptor?) : OkHttpClient {
        val b = OkHttpClient.Builder()
        if(null != authInterceptor) {
            b.addInterceptor(authInterceptor)
        }
        b.addInterceptor(interceptor)
        return b.build()
    }

    /** Interceptor는 OkHttp에 있는 강력한 메커니즘으로 호출을 모니터, 재 작성 및 재 시도를 할 수 있습니다. Interceptor는 크게 두 가지 카테고리로 분류할 수 있습니다.
    - Application Interceptors : Application Interceptor를 등록하려면 OkHttpClient.Builder에서 addInterceptor()를 호출해야 합니다.
    - Network Interceptors : Network Interceptor를 등록하려면 addInterceptor() 대신 addNetworkInterceptor()를 추가해야 합니다.
     */

    /**
     * OkHttpClient 객체 생성시 적용할 HttpLoggingInterceptor객체
     *  // [miss] private
     */
    private fun provideLoggingInterceptor() : HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }


    /**
     * [Github Search API] access_token값으로 AuthInterceptor 객체 생성
     * // [miss] private
     */
    private fun provideAuthInterceptor(provider: AuthTokenProvider): AuthInterceptor? {
        val token = provider.getToken() ?: throw IllegalStateException("authToken cannot be null")
        return AuthInterceptor(token)
    }


    /**
     * [Github Search API] AuthTokenProvider 객체 리턴
     * // [miss] private
     */
    private fun provideAuthTokenProvider(context: Context): AuthTokenProvider {
        return AuthTokenProvider(context.applicationContext)
    }


    /**
     * OkHttpClient 객체 생성시 적용할 인증 Interceptor객체
     * [miss] 코틀린 1. 'internal'접근제한자 : 동일 모듈 내에서 접근가능!
     *        2. static nested class(정적 중첩 클래스) 키워드 안씀!!!
     */
    internal class AuthInterceptor(private val token: String) : Interceptor {
        @Throws(IOException::class) // [miss]
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()

            val b = original.newBuilder()
                .addHeader("Authorization", "token $token")

            val request = b.build()

            return chain.proceed(request)
        }
    }


}


