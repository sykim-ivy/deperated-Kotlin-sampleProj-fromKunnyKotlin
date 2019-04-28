package com.androidhuman.example.simplegithub.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.GithubApi
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.model.RepoSearchResponse
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.ui.RepositoryActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_search.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity(), SearchAdapter.ItemClickListener {

    var adapter: SearchAdapter? = null

    var api: GithubApi? = null
    var searchCall: Call<RepoSearchResponse>? = null

    var menuSearch: MenuItem? = null
    var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_search)

        adapter = SearchAdapter()
        adapter!!.setItemClickListener(this) // 위에서 객체 생성해서 !!
        rvActivitySearchList.layoutManager = LinearLayoutManager(this@SearchActivity)
        rvActivitySearchList.adapter = adapter

        api = provideGithubApi(this@SearchActivity)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu?.findItem(R.id.menu_activity_search_query)

        searchView = menuSearch?.actionView as SearchView?
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
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
        if(R.id.menu_activity_search_query === item?.itemId) {
            item.expandActionView()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(repository: GithubRepo) {
        intent = Intent(this@SearchActivity, RepositoryActivity::class.java)
        intent.putExtra(RepositoryActivity.KEY_USER_LOGIN, repository.owner.login)
        intent.putExtra(RepositoryActivity.KEY_REPO_NAME, repository.name)
        startActivity(intent)
    }

    private fun searchRepository(query: String) {
        clearResults()
        hideError()
        showProgress()

        searchCall = api?.searchRepository(query)
        searchCall?.enqueue(object: Callback<RepoSearchResponse>{
            override fun onResponse(call: Call<RepoSearchResponse>, response: Response<RepoSearchResponse>) {
                hideProgress()

                val searchResult = response.body()
                if(response.isSuccessful && null != searchResult) {
                    adapter?.items = searchResult.items.toMutableList()
                    adapter?.notifyDataSetChanged()

                    if(searchResult.totalCount == 0) {
                        showError(getString(R.string.no_search_result))
                    }
                } else {
                    showError("Not Successful : " + response.message())
                }
            }

            override fun onFailure(call: Call<RepoSearchResponse>, t: Throwable) {
                hideProgress()
                t.message?.let {
                    showError(it)
                }
            }

        } )
    }



    private fun updateTitle(query: String?) {
        val ab = supportActionBar
        ab?.setSubtitle(query)
    }

    private fun hideSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(searchView?.windowToken, 0)
    }

    private fun collapseSearchView() {
        menuSearch?.collapseActionView()
    }

    private fun clearResults() {
        adapter?.items?.clear()
        adapter?.notifyDataSetChanged()
    }

    private fun showProgress() {
        pbActivitySearch.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        pbActivitySearch.visibility = View.GONE
    }

    private fun showError(message: String) {
        tvActivitySearchMessage.text = message
        tvActivityMainMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvActivitySearchMessage.text = ""
        tvActivityMainMessage.visibility = View.GONE
    }

}