package com.androidhuman.example.simplegithub.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit 객체 생성
 */
fun provideAuthApi(): AuthApi {
    return Retrofit.Builder()
        .baseUrl("https://github.com/")
        .client(provideOkHttpClient(provideLoggingInterceptor(), null))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java) // << API 통신을 위해 정의한 인터페이스를 Retrofit에 초기화 //TODO : .class 어떻게 바꿔야하는지 모르겠음
}

/**
 * Retrofit 객체 생성시 사용할 OkHttpClient 객체 생성
 */
fun provideOkHttpClient(interceptor: HttpLoggingInterceptor, authInterceptor: AuthInterceptor?) : OkHttpClient {
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
 */
fun provideLoggingInterceptor() : HttpLoggingInterceptor {
    val interceptor = HttpLoggingInterceptor()
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
    return interceptor
}

/**
 * OkHttpClient 객체 생성시 적용할 인증 Interceptor객체
 */
class AuthInterceptor(val token: String) : Interceptor { //TODO: << static class
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val b = original.newBuilder()
            .addHeader("Authorization", "token " + token)

        val request = b.build()

        return chain.proceed(request)
    }
}