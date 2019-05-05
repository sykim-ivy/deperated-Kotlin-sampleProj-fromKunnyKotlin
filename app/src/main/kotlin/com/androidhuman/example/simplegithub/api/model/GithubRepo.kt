package com.androidhuman.example.simplegithub.api.model

import com.google.gson.annotations.SerializedName

/**
 * Gson라이브러리는 별로 설정없으면 리플렉션을 사용해 클래스필드와 JSON 필드를 매핑하는데
 * 매핑되지 않거나 JSON값이 null인 경우, 코틀린에서 non-nullable인 변수에 null 할당가능해짐 -> 널포인터 오류 발생!!
 * 그러므로, REST API 응답 클래스 작성시 API정보 잘보고서 널값 허용 프로터피, 비허용 프로퍼티를 명확히 해야함!!
 */

class GithubRepo(
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val owner: GithubOwner,
    val description: String?,
    val language: String?,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("stargazers_count") val stars: Int
)