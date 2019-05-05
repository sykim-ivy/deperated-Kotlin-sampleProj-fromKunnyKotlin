package com.androidhuman.example.simplegithub.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.GithubApi

import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.model.RepoSearchResponse
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.ui.repo.RepositoryActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_search.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity(), SearchAdapter.ItemClickListener {

    //TODO: [의문점1] lateinit 적용시 초기화 안들어갈 경우의 불안을 안고가는 것보다 var로 선언하고 null로 초기화후 코틀린이나 코드상 널체크되는게 낫지 않은지?
    var adapter: SearchAdapter? = null

    var api: GithubApi? = null
    private var searchCall: Call<RepoSearchResponse>? = null

    private var menuSearch: MenuItem? = null
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        Log.d("SearchActivity", "[ksg] onCreate()")

        adapter = SearchAdapter()
        adapter!!.setItemClickListener(this) // 위에서 객체 생성해서 !!
        rvActivitySearchList.layoutManager = LinearLayoutManager(this@SearchActivity)
        rvActivitySearchList.adapter = adapter

        api = provideGithubApi(this@SearchActivity)
        Log.d("SearchActivity", "[ksg] ${api == null}")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d("SearchActivity", "[ksg] onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu?.findItem(R.id.menu_activity_search_query)

        searchView = menuSearch?.actionView as SearchView?
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("SearchActivity", "[ksg] onQueryTextSubmit(" + query + ")")
                updateTitle(query)
                hideSoftKeyboard()
                collapseSearchView()
                query?.let { searchRepository(it) } //TODO: 이렇게 써도 돼나?
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })

        menuSearch?.expandActionView()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        Log.d("SearchActivity", "[ksg] onOptionsItemSelected()")
        if(R.id.menu_activity_search_query == item?.itemId) {
            item.expandActionView()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(repository: GithubRepo) {
        Log.d("SearchActivity", "[ksg] onItemClick()")
        intent = Intent(this@SearchActivity, RepositoryActivity::class.java)
        intent.putExtra(RepositoryActivity.KEY_USER_LOGIN, repository.owner.login)
        intent.putExtra(RepositoryActivity.KEY_REPO_NAME, repository.name)
        startActivity(intent)
    }

    private fun searchRepository(query: String) {
        Log.d("SearchActivity", "[ksg] searchRepository()")
        clearResults()
        hideError()
        showProgress()

        searchCall = api?.searchRepository(query)
        Log.d("SearchActivity", "[ksg] ${api == null}")
        Log.d("SearchActivity", "[ksg] ${searchCall == null}")
        searchCall?.enqueue(object: Callback<RepoSearchResponse>{
            override fun onResponse(call: Call<RepoSearchResponse>, response: Response<RepoSearchResponse>) {
                Log.d("SearchActivity", "[ksg] searchCall API : onResponse()")
                hideProgress()

                val searchResult = response.body()
                if(response.isSuccessful && null != searchResult) {
                    adapter?.setItems(searchResult.items.toMutableList())
                    adapter?.notifyDataSetChanged()

                    if(searchResult.totalCount == 0) {
                        showError(getString(R.string.no_search_result))
                    }
                } else {
                    showError("Not Successful : " + response.message())
                }
            }

            override fun onFailure(call: Call<RepoSearchResponse>, t: Throwable) {
                Log.d("SearchActivity", "[ksg] onFailure()")
                hideProgress()
                showError(t.message)

                /** [syk] nullable인 파라미터값으로 null허용하지 않는 함수호출시, null일때 호출하지 않는 경우 'nullableParameter?.let{ method(it) }' 방법 사용
                 *  파라미터?.let {
                 *      널허용안하는메소드(it)
                 *  }
                 */
//                t.message?.let {
//                    showError(it)
//                }
            }

        } )
    }



    private fun updateTitle(query: String?) {
        val ab = supportActionBar
        ab?.subtitle = query
    }

    private fun hideSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager //[miss] as 변환형에 굳이 '?'붙여서 널처리할 필요X
        imm.hideSoftInputFromWindow(searchView?.windowToken, 0)
    }

    private fun collapseSearchView() {
        menuSearch?.collapseActionView()
    }

    private fun clearResults() {
        adapter?.clearItems()
        adapter?.notifyDataSetChanged()
    }

    private fun showProgress() {
        pbActivitySearch.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        pbActivitySearch.visibility = View.GONE
    }

    private fun showError(message: String?) {
        /**
         * [syk] Nullable인 값을 대입시 2가지 방법 존재
         * 1) null 인 경우 아예 값을 대입하지 않는 경우 --> '?.' 사용
         * 2) null 인 경우 넣어줄 대체값이 있는 경우 ---> 엘비스 연산자 사용 --> 'null이 아닐때 값' ?: 'null일때값'
         *
         * [miss] 아래 경우, 에러메시지 이므로 null일때 대체값을 하나 만들어 넣어주는 게 좋아보임!
         */
        tvActivitySearchMessage.text = message ?: "Unexpected Error: error msg is null"

        tvActivityMainMessage.visibility = View.VISIBLE // [miss] 익스텐션으로 접근하는 레이아웃객체에는 널체크'?.'을 붙일 필요X
    }

    private fun hideError() {
        tvActivitySearchMessage?.text = ""
        tvActivityMainMessage.visibility = View.GONE
    }

}