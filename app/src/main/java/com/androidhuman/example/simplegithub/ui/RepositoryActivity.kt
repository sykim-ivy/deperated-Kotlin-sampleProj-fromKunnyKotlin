package com.androidhuman.example.simplegithub.ui

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.GithubApi
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.ui.search.GlideApp
import kotlinx.android.synthetic.main.activity_repository.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalArgumentException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class RepositoryActivity : AppCompatActivity() {

    companion object {
        val KEY_USER_LOGIN = "user_login"
        val KEY_REPO_NAME = "repo_name"
    }

    var api: GithubApi? = null
    var repoCall: Call<GithubRepo>? = null

    val dateFormatInResponse = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())
    val dateFormatToShow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_repository)

        api = provideGithubApi(this@RepositoryActivity)

        val login = intent.getStringExtra(KEY_USER_LOGIN)
        if(null == login)
            throw IllegalArgumentException("No login info exists in extras")

        val repo = intent.getStringExtra(KEY_REPO_NAME)
        if(null == repo)
            throw IllegalArgumentException("No repo info exists in extras")

        showRepositoryInfo(login, repo)

    }

    private fun showRepositoryInfo(login: String, repoName: String) {
        showProgress()

        repoCall = api?.getRepository(login, repoName)
        repoCall?.enqueue(object: Callback<GithubRepo> {

            override fun onResponse(call: Call<GithubRepo>, response: Response<GithubRepo>) {
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
                        if (null == repo.description) getString(R.string.no_description_provided) else repo.description

                    tvActivityRepositoryLanguage.text =
                        if (null == repo.language) getString(R.string.no_language_specified) else repo.language

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

    private fun hideProgress(isSuccessd: Boolean) {
        llActivityRepositoryContent.visibility = if(isSuccessd) View.VISIBLE else View.GONE
        pbActivityRepository.visibility = View.GONE
    }

    private fun showError(message: String?) {
        tvActivityRepositoryMessage.text = message
        tvActivityRepositoryMessage.visibility = View.VISIBLE
    }

}