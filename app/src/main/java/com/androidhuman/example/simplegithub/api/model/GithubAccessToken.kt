package com.androidhuman.example.simplegithub.api.model

import com.google.gson.annotations.SerializedName

class GithubAccessToken {

    @SerializedName("access_token")
    lateinit var accessToken: String //TODO : Q. << final 변수인데 바로 초기화 못할때 뭐로 써야함 ?

    lateinit var scope: String

    @SerializedName("token_type")
    lateinit var tokenType: String

}