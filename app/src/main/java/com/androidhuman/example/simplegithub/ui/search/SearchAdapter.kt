package com.androidhuman.example.simplegithub.ui.search

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import kotlinx.android.synthetic.main.item_repository.view.*

class SearchAdapter : RecyclerView.Adapter<SearchAdapter.RepositoryHolder>() {

    var items: MutableList<GithubRepo> = mutableListOf()

    val placeholder: ColorDrawable? = ColorDrawable(Color.GRAY)

    var listener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryHolder {
        return RepositoryHolder(parent)
    }

    override fun onBindViewHolder(holder: RepositoryHolder, position: Int) {
        val repo: GithubRepo = items.get(position)

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

    override fun getItemCount(): Int {
        return items.count()
    }

    fun setItemClickListener(listener: ItemClickListener) {
        this.listener = listener
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

@GlideModule
class AppGlideModule : AppGlideModule()