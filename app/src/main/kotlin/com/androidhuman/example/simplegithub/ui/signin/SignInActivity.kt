package com.androidhuman.example.simplegithub.ui.signin

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.androidhuman.example.simplegithub.BuildConfig
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.AuthApi
import com.androidhuman.example.simplegithub.api.model.GithubAccessToken
import com.androidhuman.example.simplegithub.api.provideAuthApi
import com.androidhuman.example.simplegithub.data.AuthTokenProvider
import com.androidhuman.example.simplegithub.extensions.plusAssign
import com.androidhuman.example.simplegithub.ui.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_sign_in.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalStateException

class SignInActivity : AppCompatActivity() {

    /**
     * val 변수는 Lazy 프로퍼티를 써주어 사용시점 전에 초기화를 수행할 수 있다.
     * [syk][miss] Lazy 프로퍼티
     *  - 해당 프로퍼티의 첫 사용 시점에서 초기화를 수행
     *  - val 변수에서만 사용가능
     *  - 싱글톤 클래스의 구현을 프로퍼티에 적용한 형태라고 볼 수 있다고 함 (p.252)
     */
    private val api by lazy { provideAuthApi() } // static method called // api는 API가 등록된 Retrofit 서비스 객체 }
    private val authTokenProvider by lazy { AuthTokenProvider(this) } // SP에 auth_token값 저장 관리 객체

    //[syk][RxJava] API호출 결과를 Observable로 받도록 수정했으므로 , 기존 관리를 위해 사용한 Call객체를 모두 Disposable 객체로 대체
    private val disposables = CompositeDisposable() //[syk][RxJava] 여러 객체를 관리할 수 있는 CompositeDisposable객체로 생성
//    private var accessTokenCall: Call<GithubAccessToken>? = null // API응답형식을 GithubAccessToken로 받아오는 객체 Call

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        Log.d("SignInActivity", "[ksg] onCreate()")

        btnActivitySignInStart.setOnClickListener{
            Log.d("SignInActivity", "[ksg] OnClick()")
            val authUri = Uri.Builder().scheme("https").authority("github.com")
                .appendPath("login")
                .appendPath("oauth")
                .appendPath("authorize")
                .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
                .build()

            Log.d("SignInActivity", "[ksg] uri = ${authUri.toString()}")

            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@SignInActivity, authUri)
        }

        if(null != authTokenProvider!!.getToken()) { // [syk] 위에서 객체 생성하니까 '!!'키워드로 non-null값 보장
            Log.d("SignInActivity", "[ksg] auth_token is exist")
            launchMainActivity()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Log.d("SignInActivity", "[ksg] onNewIntent()")

        showProgress()

        val uri = intent?.data
        if(null === uri) {
            throw IllegalArgumentException("No data exists")
        }

        val code = uri.getQueryParameter("code")
        if(null === code) {
            throw IllegalStateException("No code exists")
        }

        getAccessToken(code)
    }

    override fun onStop() {
        super.onStop()

        //[syk][RxJava] 관리하던 disposable 객체를 모두 해체
        disposables.clear() // CompositeDisposable.clear() 함수 호출시 CompositeDisposable 내 disposable객체를 모두 해제 (disposable해제시점의 네트워크 요청이 있었으면 자동 취소)
//        // [miss][syk] 액티비티가 사리지는 시점에서 API호출객체가 생성되어 있다면 API 요청 취소
//        accessTokenCall?.run { cancel() }
    }

    private fun getAccessToken(code: String) {
        Log.d("SignInActivity", "[ksg] getAccessToken() : code = $code")

        //[syk][RxJava] Observable형태롤 반환되는 accessToken을 처리할 수 있도록 코드 변경 // REST API를 통해 access token 요청
        disposables +=
            api.getAccessToken(
                BuildConfig.GITHUB_CLIENT_ID,
                BuildConfig.GITHUB_CLIENT_SECRET, code)

                //[syk][RxJava] 받은 응답에서 accessToken만 추출하여 처리하도록 map()함수로 전달된 데이터를 변경시킴
                .map { it.accessToken } //TODO: map 함수 알아보기!

                //[syk][RxJava] 이후 실행되는 로직은 모두 메인 스레드에서 실행 : RxAndroid에서 제공하는 스케줄러인 AndroidSchedulers사용
                .observeOn(AndroidSchedulers.mainThread())

                //[syk][RxJava] subsciribe할때 수행할 작업 구현
                .doOnSubscribe { showProgress() }

                //[syk][RxJava] 스트림이 종료될때 수행할 작업 구현
                .doOnTerminate { hideProgress() }

                //TODO: subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError)인데 왜 아래는 이런({onNext}){onError} 블록모양이지??
                //[syk][RxJava] Observable을 구독 (= Observable에서 이벤트가 발생하는지 Observer가 관찰하여 발생시 Observer의 작업 실행시킴), 이떄 Disposable객체 생성됨
                .subscribe({ token ->
                    //[syk][RxJava] API를 통해 정상적으로 데이터 받았을떄 처리할 작업을 구현(작업 중 오류발생시 여기 안 탐)
                    Log.d("SignInActivity", "[ksg] get auth_token success!")
                    authTokenProvider.updateToken(token) // << map()에서 accessToken만 추출하여 이렇게씀
                    launchMainActivity()
                }) {
                    //[syk][RxJava] 작업이 정상적으로 완료되지 않았을때 호출되는 에러블록 : 네트워크 오류, 데이터 처리 오류 등
                    showError(it)
                }

// // RxJava 미적용시 사용했던 Retrofit코드
//        showProgress()
//
//        accessTokenCall = api.getAccessToken(
//            BuildConfig.GITHUB_CLIENT_ID,
//            BuildConfig.GITHUB_CLIENT_SECRET, code)
//
//        // [miss] getAccessToken()의 리턴타입이 non-null이므로 비널값 보증가능하여 '!!' 사용가능
//        accessTokenCall!!.enqueue(object : Callback<GithubAccessToken> { // << TODO: object로 선언하면 클래스 선언과 동시에 객체가 생성됩니다. << 이거 이렇게 쓰기도 해??
//
//            override fun onFailure(call: Call<GithubAccessToken>, t: Throwable) {
//                Log.d("SignInActivity", "[ksg] access_token API : onFailure()")
//                hideProgress()
//                showError(t)
//            }
//
//            override fun onResponse(call: Call<GithubAccessToken>, response: Response<GithubAccessToken>) {
//                /** RES 예시)
//                 * Accept: application/json
//                    {"access_token":"e72e16c7e42f292c6912e7710c838347ae178b4a", "scope":"repo,gist", "token_type":"bearer"}
//                 */
//                Log.d("SignInActivity", "[ksg] access_token API : onResponse()")
//                hideProgress()
//
//                val token: GithubAccessToken? = response.body()
//                if(response.isSuccessful && null != token) {
//                    Log.d("SignInActivity", "[ksg] get auth_token success!")
//                    authTokenProvider.updateToken(token.accessToken)
//                    launchMainActivity()
//                } else {
//                    showError(IllegalStateException("Not successful: " + response.message()))
//                }
//            }
//
//        })
    }

    private fun showError(throwable: Throwable) {
        Toast.makeText(this@SignInActivity, throwable.message, Toast.LENGTH_LONG).show()
    }

    private fun showProgress() {
        btnActivitySignInStart.visibility = GONE
        pbActivitySignIn.visibility = VISIBLE
    }

    private fun hideProgress() {
        btnActivitySignInStart.visibility = VISIBLE
        pbActivitySignIn.visibility = GONE
    }

    fun launchMainActivity() {
        Log.d("SignInActivity", "[ksg] launchMainActivity()")
        startActivity(Intent(
            this@SignInActivity, MainActivity::class.java) // [miss]Study : '.class' -> '::class.java' 로 사용
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

}