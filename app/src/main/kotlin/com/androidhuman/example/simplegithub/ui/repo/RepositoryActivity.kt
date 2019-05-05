package com.androidhuman.example.simplegithub.ui.repo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.ui.GlideApp
import kotlinx.android.synthetic.main.activity_repository.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalArgumentException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class RepositoryActivity : AppCompatActivity() {

    // [syk] companion object 정의부를 가장 위로 올려줌
    companion object {
        const val KEY_USER_LOGIN = "user_login"
        const val KEY_REPO_NAME = "repo_name"
    }

    /**
     * val 변수는 Lazy 프로퍼티를 써주어 사용시점 전에 초기화를 수행할 수 있다.
     * [syk][miss] Lazy 프로퍼티
     *  - 해당 프로퍼티의 첫 사용 시점에서 초기화를 수행
     *  - val 변수에서만 사용가능
     *  - 싱글톤 클래스의 구현을 프로퍼티에 적용한 형태라고 볼 수 있다고 함 (p.252)
     */
    private val api by lazy { provideGithubApi(this@RepositoryActivity) }

    private var repoCall: Call<GithubRepo>? = null

    val dateFormatInResponse = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())
    val dateFormatToShow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository)

        Log.d("RepositoryActivity", "[ksg] onCreate()")

        //TODO : elvis operator 사용 확인!
        val login = intent.getStringExtra(KEY_USER_LOGIN) ?: throw IllegalArgumentException("No login info exists in extras")

        val repo = intent.getStringExtra(KEY_REPO_NAME) ?: throw IllegalArgumentException("No repo info exists in extras")

        showRepositoryInfo(login, repo)

    }

    override fun onStop() {
        super.onStop()
        // [miss][syk] 액티비티가 사리지는 시점에서 API호출객체가 생성되어 있다면 API 요청 취소
        repoCall?.run { cancel() }
    }

    private fun showRepositoryInfo(login: String, repoName: String) {
        Log.d("RepositoryActivity", "[ksg] showRepositoryInfo() login = " + login + ", repoName = " + repoName)
        showProgress()

        repoCall = api.getRepository(login, repoName)
        // [miss] getRepository()의 리턴타입이 non-null이므로 비널값 보증가능하여 '!!' 사용가능
        repoCall!!.enqueue(object: Callback<GithubRepo> {

            override fun onResponse(call: Call<GithubRepo>, response: Response<GithubRepo>) {
                Log.d("RepositoryActivity", "[ksg] onResponse()")
                hideProgress(true)

                val repo: GithubRepo? = response.body()
                if (response.isSuccessful && null != repo) {
                    GlideApp.with(this@RepositoryActivity)
                        .load(repo.owner.avatarUrl)
                        .into(ivActivityRepositoryProfile)

                    tvActivityRepositoryName.text = repo.fullName
                    tvActivityRepositoryStars.text =
                        resources.getQuantityString(R.plurals.star, repo.stars, repo.stars)
                    /** resources.getQuantityString함수
                     * 첫 번째 매개 변수는 plurals 자원이름
                     * 두 번째 매개 변수는 올바른 quantity 문자열을 선택하는 데 사용 (int)
                     * 세 번째 매개 변수는 형식 지정자 %d 를 대체하는 데 사용할 형식 인수입니다.(Object...)
                     */

                    tvActivityRepositoryDescription.text =
                        repo.description ?: getString(R.string.no_description_provided)

                    tvActivityRepositoryLanguage.text =
                        repo.language ?: getString(R.string.no_language_specified)

                    try {
                        val lastUpdate = dateFormatInResponse.parse(repo.updatedAt)
                        tvActivityRepositoryLastUpdate.text = dateFormatToShow.format(lastUpdate)
                    } catch (e: ParseException) {
                        tvActivityRepositoryLastUpdate.text = getString(R.string.unknown)
                    }
                } else {
                    showError("Not successful: " + response.message())
                }
            }

            override fun onFailure(call: Call<GithubRepo>, t: Throwable) {
                hideProgress(false)
                showError(t.message)
            }
        })
    }

    private fun showProgress() {
        llActivityRepositoryContent.visibility = View.GONE
        pbActivityRepository.visibility = View.VISIBLE
    }

    private fun hideProgress(isSuccessed: Boolean) {
        llActivityRepositoryContent.visibility = if(isSuccessed) View.VISIBLE else View.GONE
        pbActivityRepository.visibility = View.GONE
    }

    private fun showError(message: String?) {
        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] with() 함수를 사용하여 객체 범위 내에서 작업수행
        with(tvActivityRepositoryMessage) {
            text = message
            visibility = View.VISIBLE
        }
    }

}