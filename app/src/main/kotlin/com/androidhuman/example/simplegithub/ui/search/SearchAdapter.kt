package com.androidhuman.example.simplegithub.ui.search

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.ui.GlideApp

import kotlinx.android.synthetic.main.item_repository.view.*

class SearchAdapter : RecyclerView.Adapter<SearchAdapter.RepositoryHolder>() {

    private var items: MutableList<GithubRepo> = mutableListOf()

    private val placeholder: ColorDrawable? = ColorDrawable(Color.GRAY)

    private var listener: ItemClickListener? = null

    // [miss] 함수에서 생성된 객체반환만을 처리하므로 single expression(단일 표현식)으로 사용가능(p.248) : '= { return 생성객체; }' -> ' = '으로 변경
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryHolder  = RepositoryHolder(parent)

    override fun onBindViewHolder(holder: RepositoryHolder, position: Int) {

        // items[position]값은 다른 곳에서 사용되지 않으므로, let()함수를 이용하여 값이 사용되는 범위를 명시적으로 한정할 수 있음
        items[position].let {  repo -> //[syk] let함수에서 it으로 사용하지 않고 별칭 줄 경우
            with(holder.itemView) {
                GlideApp.with(context)
                    .load(repo.owner.avatarUrl)
                    .placeholder(placeholder)
                    .into(ivItemRepositoryProfile)

                tvItemRepositoryName.text = repo.language
                try {
                    check(TextUtils.isEmpty(repo.language)) //TODO: << 원래 삼항 연산자였음
                    tvItemRepositoryLanguage.text = context.getText(R.string.no_language_specified)
                } catch (e: IllegalStateException ) {
                    tvItemRepositoryLanguage.text = repo.language
                }

                setOnClickListener {
                    if(null != listener) {
                        listener!!.onItemClick(repo) // 위에서 널체크해서 !! 사용
                    }
                }
            }
        }
    }

    // [miss] 함수에서 생성된 객체반환만을 처리하므로 single expression(단일 표현식)으로 사용가능(p.248) : '= { return 생성객체; }' -> ' = '으로 변경
    override fun getItemCount(): Int = items.count()

    fun setItemClickListener(listener: ItemClickListener) {
        this.listener = listener
    }

    fun setItems(items: List<GithubRepo>) {
        // api응답으로 받은 List를 어댑터에서 사용할 MutableList로 변환
        this.items = items.toMutableList()
    }

    fun clearItems() {
        items.clear() // (immutable)List는 clear함수 없음;
    }

    inner class RepositoryHolder(parent: ViewGroup)
        : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_repository, parent, false))

    interface ItemClickListener {
        fun onItemClick(repository: GithubRepo)
    }
}