package com.vestor.animalfinder.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PetListing(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("listing_type")
    val listingType: String,     // "lost" или "found"
    @SerialName("pet_name")
    val petName: String,
    val species: String,         // собака, кошка и т.д.
    val breed: String?,
    val color: String?,
    val age: Int?,
    val gender: String?,
    @SerialName("photo_url")
    val photoUrl: String?,
    val location: String?,
    val description: String?,
    val temperament: String?,
    val contact: String?
)