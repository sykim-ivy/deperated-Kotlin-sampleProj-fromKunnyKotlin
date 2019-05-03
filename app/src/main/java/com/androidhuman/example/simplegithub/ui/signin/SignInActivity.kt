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
import com.androidhuman.example.simplegithub.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_sign_in.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalStateException

class SignInActivity : AppCompatActivity() {

    private var api: AuthApi? = null
    private var authTokenProvider: AuthTokenProvider? = null

    private var accessTokenCall: Call<GithubAccessToken>? = null // API응답형식을 GithubAccessToken로 받아오는 객체 Call

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

        api = provideAuthApi() // static method called // api는 API가 등록된 Retrofit 서비스 객체

        authTokenProvider = AuthTokenProvider(this) // SP에 auth_token값 저장 관리 객체
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

    private fun getAccessToken(code: String) {
        Log.d("SignInActivity", "[ksg] getAccessToken() : code = $code")
        showProgress()

        accessTokenCall = api?.getAccessToken(
            BuildConfig.GITHUB_CLIENT_ID,
            BuildConfig.GITHUB_CLIENT_SECRET, code)

        accessTokenCall?.enqueue(object : Callback<GithubAccessToken> { // << TODO: object로 선언하면 클래스 선언과 동시에 객체가 생성됩니다. << 이거 이렇게 쓰기도 해??

            override fun onFailure(call: Call<GithubAccessToken>, t: Throwable) {
                Log.d("SignInActivity", "[ksg] access_token API : onFailure()")
                hideProgress()
                showError(t)
            }

            override fun onResponse(call: Call<GithubAccessToken>, response: Response<GithubAccessToken>) {
                /** RES 예시)
                 * Accept: application/json
                    {"access_token":"e72e16c7e42f292c6912e7710c838347ae178b4a", "scope":"repo,gist", "token_type":"bearer"}
                 */
                Log.d("SignInActivity", "[ksg] access_token API : onResponse()")
                hideProgress()

                val token: GithubAccessToken? = response.body()
                if(response.isSuccessful && null != token) {
                    Log.d("SignInActivity", "[ksg] get auth_token success!")
                    authTokenProvider?.updateToken(token.accessToken)
                    launchMainActivity()
                } else {
                    showError(IllegalStateException("Not successful: " + response.message()))
                }
            }

        })
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
            this@SignInActivity, MainActivity::class.java) //TODO : .class 어떻게 바꿔야하는지 모르겠음
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

}