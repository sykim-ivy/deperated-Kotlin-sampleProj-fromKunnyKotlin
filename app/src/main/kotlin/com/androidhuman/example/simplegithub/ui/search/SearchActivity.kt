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
import com.androidhuman.example.simplegithub.extensions.plusAssign
import com.androidhuman.example.simplegithub.ui.repo.RepositoryActivity
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_search.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalStateException

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

    //[syk][RxJava] API호출 결과를 Observable로 받도록 수정했으므로 , 기존 관리를 위해 사용한 Call객체를 모두 Disposable 객체로 대체
    private val disposables = CompositeDisposable() //[syk][RxJava] 여러 객체를 관리할 수 있는 CompositeDisposable객체로 생성
//    private var searchCall: Call<RepoSearchResponse>? = null

    //[syk][RxBinding] SearchView에 RxBinding을 적용하기 위한 CompositeDisposable객체 생성
    private val viewDisposable = CompositeDisposable()

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

        //[syk][RxJava] 관리하던 disposable 객체를 모두 해체
        disposables.clear() // CompositeDisposable.clear() 함수 호출시 CompositeDisposable 내 disposable객체를 모두 해제 (disposable해제시점의 네트워크 요청이 있었으면 자동 취소)
//        // [miss][syk] 액티비티가 사리지는 시점에서 API호출객체가 생성되어 있다면 API 요청 취소
//        searchCall?.run { cancel() }

        //[syk][RxBinding] 액티비티가 완전히 종료되고 있는 경우에만 관리하고 있는 viewDisposable을 해제 +) 화면 꺼지거나 액티비티 화면에서 사라지는 경우에는 해제하지 않음
        if(isFinishing) {
            viewDisposable.clear()
        }
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

        //[syk][RxJava] Observable형태롤 반환되는 accessToken을 처리할 수 있도록 코드 변경 // REST API를 통해 access token 요청
        disposables += api.searchRepository(query)
            //[syk][RxJava] Observable형태로 바꿔주기 위해 flatMap을 사용 //TODO: flatMap, just 함수 알아보기!
            .flatMap {
                if(0 == it.totalCount) {
                    // 검색 결과가 없을 경우 에러 발생시켜 에러 메시지를 표시하도록 함 (이후 곧바로 에러블록이 실행됨)
                    Observable.error(IllegalStateException("No search result"))
                } else {
                    // 검색 결과 리스트를 다음 스트림으로 전달합니다.
                    Observable.just(it.items)
                }
            }

            // 이 이후 수행되는 모든 코드는 메인 스레드에서 실행 : RxAndroid에서 제공하는 스케줄러인 AndroidSchedulers사용
            .observeOn(AndroidSchedulers.mainThread())

            // 구독할때 수행할 작업 구현
            .doOnSubscribe {
                clearResults()
                hideError()
                showProgress()
            }

            // 스트림 종료될때 수행할 작업 구현
            .doOnTerminate {
                hideProgress()
            }

            // Observable 구독
            .subscribe({ items ->
                // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
                // [syk] with() 함수를 사용하여 객체 범위 내에서 작업수행
                with(adapter) {
                    setItems(items.toMutableList())
                    notifyDataSetChanged()
                }
            }) {
                // 작업이 정상적으로 완료되지 않았을때 호출되는 에러블록 : 네트워크 오류, 데이터 처리 오류 등
                showError(it.message)
            }


// // RxJava 미적용시 사용했던 Retrofit코드
//        clearResults()
//        hideError()
//        showProgress()
//        searchCall = api.searchRepository(query)
//        Log.d("SearchActivity", "[ksg] ${searchCall == null}")
//
//        // [miss] searchRepository()의 리턴타입이 non-null이므로 비널값 보증가능하여 '!!' 사용가능
//        searchCall!!.enqueue(object: Callback<RepoSearchResponse>{
//            override fun onResponse(call: Call<RepoSearchResponse>, response: Response<RepoSearchResponse>) {
//                Log.d("SearchActivity", "[ksg] searchCall API : onResponse()")
//                hideProgress()
//
//                val searchResult = response.body()
//                if(response.isSuccessful && null != searchResult) {
//
//                    // [miss][syk] 하나의 객체의 여러 함수를 호출하는 경우, 인스턴스를 얻기 위해 임시로 변수를 선언하는 부분은 범위 지정 함수(run,with,apply) 사용가능
//                    // [syk] with() 함수를 사용하여 객체 범위 내에서 작업수행
//                    with(adapter) {
//                        setItems(searchResult.items.toMutableList())
//                        notifyDataSetChanged()
//                    }
//
//                    if(searchResult.totalCount == 0) {
//                        showError(getString(R.string.no_search_result))
//                    }
//                } else {
//                    showError("Not Successful : " + response.message())
//                }
//            }
//
//            override fun onFailure(call: Call<RepoSearchResponse>, t: Throwable) {
//                Log.d("SearchActivity", "[ksg] onFailure()")
//                hideProgress()
//                showError(t.message)
//
//                /** [syk] nullable인 파라미터값으로 null허용하지 않는 함수호출시, null일때 호출하지 않는 경우 'nullableParameter?.let{ method(it) }' 방법 사용
//                 *  파라미터?.let {
//                 *      널허용안하는메소드(it)
//                 *  }
//                 */
////                t.message?.let {
////                    showError(it)
////                }
//            }
//
//        })
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