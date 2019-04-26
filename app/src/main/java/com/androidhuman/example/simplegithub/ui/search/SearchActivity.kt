package com.androidhuman.example.simplegithub.ui.search

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : AppCompatActivity(), SearchAdapter.ItemClickListener {

    var adapter: SearchAdapter? = null
//    var api: GithubApi

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_search)

        adapter = SearchAdapter()
        adapter!!.setItemClickListener(this) // 위에서 객체 생성해서 !!

        rvActivitySearchList.layoutManager = LinearLayoutManager(this@SearchActivity)
        rvActivitySearchList.adapter = adapter


    }

    override fun onItemClick(repository: GithubRepo) {

    }

}