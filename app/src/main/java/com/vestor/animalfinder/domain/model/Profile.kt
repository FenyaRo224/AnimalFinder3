package com.vestor.animalfinder.domain.model   // ← поменяй на свой package

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("user_id")
    val userId: String,

    val email: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val phone: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)