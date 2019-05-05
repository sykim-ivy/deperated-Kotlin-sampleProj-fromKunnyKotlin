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

    /**
     * val 변수는 Lazy 프로퍼티를 써주어 사용시점 전에 초기화를 수행할 수 있다.
     * [syk][miss] Lazy 프로퍼티
     *  - 해당 프로퍼티의 첫 사용 시점에서 초기화를 수행
     *  - val 변수에서만 사용가능
     *  - 싱글톤 클래스의 구현을 프로퍼티에 적용한 형태라고 볼 수 있다고 함 (p.252)
     */
    val adapter by lazy { SearchAdapter() }

    val api by lazy { provideGithubApi(this@SearchActivity) }
    private var searchCall: Call<RepoSearchResponse>? = null

    private var menuSearch: MenuItem? = null
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        Log.d("SearchActivity", "[ksg] onCreate()")

        adapter.setItemClickListener(this)

        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] with() 함수를 사용하여 레이아웃객체 범위 내에서 작업수행
        with(rvActivitySearchList) {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter // [miss][syk] 범위 지정 함수 내부에서 클래스의 this 사용하는 방법
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d("SearchActivity", "[ksg] onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu?.findItem(R.id.menu_activity_search_query)

        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] apply() 함수를 사용하여 객체 생성과 범위 내에서 작업수행
        searchView = (menuSearch?.actionView as SearchView).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener{
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
        }

        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] with() 함수를 사용하여 객체 범위 내에서 작업수행
        with(menuSearch) {
            // [miss] 빼먹은 부분
            this?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener{
                override fun onMenuItemActionExpand(menuItem: MenuItem?): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(menuItem: MenuItem?): Boolean {
                    if("" == searchView?.query) {
                        finish()
                    }
                    return true
                }
            })

            this?.expandActionView()
        }

        return true
    }

    override fun onStop() {
        super.onStop()
        // [miss][syk] 액티비티가 사리지는 시점에서 API호출객체가 생성되어 있다면 API 요청 취소
        searchCall?.run { cancel() }
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
        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] apply() 함수를 사용하여 객체 생성과 범위 내에서 작업수행
        intent = Intent(this@SearchActivity, RepositoryActivity::class.java).apply {
            putExtra(RepositoryActivity.KEY_USER_LOGIN, repository.owner.login)
            putExtra(RepositoryActivity.KEY_REPO_NAME, repository.name)
        }
        startActivity(intent)
    }

    private fun searchRepository(query: String) {
        Log.d("SearchActivity", "[ksg] searchRepository()")
        clearResults()
        hideError()
        showProgress()

        searchCall = api.searchRepository(query)
        Log.d("SearchActivity", "[ksg] ${searchCall == null}")

        // [miss] searchRepository()의 리턴타입이 non-null이므로 비널값 보증가능하여 '!!' 사용가능
        searchCall!!.enqueue(object: Callback<RepoSearchResponse>{
            override fun onResponse(call: Call<RepoSearchResponse>, response: Response<RepoSearchResponse>) {
                Log.d("SearchActivity", "[ksg] searchCall API : onResponse()")
                hideProgress()

                val searchResult = response.body()
                if(response.isSuccessful && null != searchResult) {

                    // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
                    // [syk] with() 함수를 사용하여 객체 범위 내에서 작업수행
                    with(adapter) {
                        setItems(searchResult.items.toMutableList())
                        notifyDataSetChanged()
                    }

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
        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] run() 함수를 사용하여 별도의 변수선언없이 객체 범위 내에서 작업수행
        // [miss] getSupportActionBar()의 리턴값이 nullable이므로 '?.'을 사용해야함!!
        supportActionBar?.run {
            this.subtitle = query
        }
    }

    private fun hideSoftKeyboard() {
        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] run() 함수를 사용하여 별도의 변수선언없이 객체 범위 내에서 작업수행
        //[miss] as 변환형에 굳이 '?'붙여서 널처리할 필요X
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).run {
            hideSoftInputFromWindow(searchView?.windowToken, 0)
        }
    }

    private fun collapseSearchView() {
        menuSearch?.collapseActionView()
    }

    private fun clearResults() {
        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] with() 함수를 사용하여 객체 범위 내에서 작업수행
        with(adapter) {
            clearItems()
            notifyDataSetChanged()
        }
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
        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] with() 함수를 사용하여 객체 범위 내에서 작업수행
        with(tvActivitySearchMessage) {
            text = message ?: "Unexpected Error: error msg is null"
            visibility = View.VISIBLE // [miss] 익스텐션으로 접근하는 레이아웃객체에는 널체크'?.'을 붙일 필요X
        }
    }

    private fun hideError() {
        // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
        // [syk] with() 함수를 사용하여 객체 범위 내에서 작업수행
        with(tvActivitySearchMessage) {
            text = ""
            visibility = View.GONE
        }
    }

}