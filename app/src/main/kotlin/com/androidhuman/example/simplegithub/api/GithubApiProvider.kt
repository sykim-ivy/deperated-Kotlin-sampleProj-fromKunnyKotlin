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

// ▼ GithubApiProvider는 클래스 단위에서 공용으로 사용하는 객체가 없으므로 굳이 싱글톤 객체로 안만들어도 무관해 주석처리 (p.246)
//object GithubApiProvider { // 코틀린 싱글톤 객체는 object로 선언!!!
//    // object 내 선언된 값이나 함수는 자바의 static 멤버와 동일한 방법으로 사용

    /**
     * [Github OAuth API] Retrofit 객체 생성
     * [miss] 함수에서 생성된 객체반환만을 처리하므로 single expression(단일 표현식)으로 사용가능(p.248) : '= { return 생성객체; }' -> ' = '으로 변경
     */
    fun provideAuthApi(): AuthApi  = Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(provideOkHttpClient(provideLoggingInterceptor(), null))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java) // << API 통신을 위해 정의한 인터페이스를 Retrofit에 초기화 // [miss]Study : '.class' -> '::class.java' 로 사용

    /**
     * [Github Search API] Retrofit 객체 생성
     * [miss] 함수에서 생성된 객체반환만을 처리하므로 single expression(단일 표현식)으로 사용가능(p.248) : '= { return 생성객체; }' -> ' = '으로 변경
     */
    fun provideGithubApi(context: Context): GithubApi = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(provideOkHttpClient(provideLoggingInterceptor(),
                provideAuthInterceptor(provideAuthTokenProvider(context))))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApi::class.java) // << API 통신을 위해 정의한 인터페이스를 Retrofit에 초기화  // [miss]Study : '.class' -> '::class.java' 로 사용

    /**
     * Retrofit 객체 생성시 사용할 OkHttpClient 객체 생성
     *  [miss] private (코틀린은 pulic이 디폴트 접근제한자이므로 주의!!)
     *  [miss] 함수에서 생성된 내부변수 선언후 객체반환만을 처리하는 경우 -> apply(),run()같은 범위 지정함수를 사용하면 내부 변수선언 제거되므로 -> single expression(단일 표현식)으로 사용가능(p.249)
     */
    private fun provideOkHttpClient(interceptor: HttpLoggingInterceptor, authInterceptor: AuthInterceptor?) : OkHttpClient
            // [syk] run() 함수로 OkHttpClient.Builder() 변수 선언을 제거!!
            = OkHttpClient.Builder().run {
                if(null != authInterceptor) {
                    addInterceptor(authInterceptor)
                }
                addInterceptor(interceptor)
                build() // [syk] run() 함수로 여기서 생성된 OkHttpClient객체 반환
            }


    /** [syk] Interceptor는 OkHttp에 있는 강력한 메커니즘으로 호출을 모니터, 재 작성 및 재 시도를 할 수 있습니다. Interceptor는 크게 두 가지 카테고리로 분류할 수 있습니다.
    - Application Interceptors : Application Interceptor를 등록하려면 OkHttpClient.Builder에서 addInterceptor()를 호출해야 합니다.
    - Network Interceptors : Network Interceptor를 등록하려면 addInterceptor() 대신 addNetworkInterceptor()를 추가해야 합니다.
     */

    /**
     * OkHttpClient 객체 생성시 적용할 HttpLoggingInterceptor객체
     *  [miss] private (코틀린은 pulic이 디폴트 접근제한자이므로 주의!!)
     *  [miss] 함수에서 생성된 내부변수 선언후 객체반환만을 처리하는 경우 -> apply(),run()같은 범위 지정함수를 사용하면 내부 변수선언 제거되므로 -> single expression(단일 표현식)으로 사용가능(p.249)
     */
    private fun provideLoggingInterceptor() : HttpLoggingInterceptor
            // [syk] apply() 함수로 인스턴스 생성과 프로퍼티값 변경을 동시에 수행!
            = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }


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
     * [miss] private (코틀린 default 접근제한자는 'public'이므로 주의!!)
     * [miss] 함수에서 생성된 객체반환만을 처리하므로 single expression(단일 표현식)으로 사용가능(p.248) : '= { return 생성객체; }' -> ' = '으로 변경
     */
    private fun provideAuthTokenProvider(context: Context): AuthTokenProvider  = AuthTokenProvider(context.applicationContext)


    /**
     * OkHttpClient 객체 생성시 적용할 인증 Interceptor객체
     * [miss] 코틀린 1. 'internal'접근제한자 : 동일 모듈 내에서 접근가능!
     *        2. static nested class(정적 중첩 클래스) 키워드 안씀!!!
     * [miss] 함수에서 생성된 내부변수 선언후 객체반환만을 처리하는 경우 -> apply(),run()같은 범위 지정함수를 사용하면 내부 변수선언 제거되므로 -> single expression(단일 표현식)으로 사용가능(p.249)
     */
    internal class AuthInterceptor(private val token: String) : Interceptor {
        @Throws(IOException::class) // [miss]
        override fun intercept(chain: Interceptor.Chain)
                // [syk] ★ with()함수 내부 변수로 Request객체 생성하고 이를 파라미터로 Response객체 생성하여 반환받음
                : Response  = with(chain) {
                // [syk] run()함수로 Request객체 생성
                val request = request().newBuilder().run {
                    addHeader("Authorization", "token $token")
                    build()
                }
                proceed(request)
            }

//        {
//            val original = chain.request()
//
//            val b = original.newBuilder()
//                .addHeader("Authorization", "token $token")
//
//            val request = b.build()
//
//            return chain.proceed(request)
//        }
    }


//}


