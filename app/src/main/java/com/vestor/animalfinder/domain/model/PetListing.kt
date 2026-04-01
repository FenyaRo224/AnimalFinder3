package com.vestor.animalfinder.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PetListing(
    val id: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("listing_type")
    val listingType: String,
    @SerialName("pet_name")
    val petName: String,
    val species: String,
    val breed: String? = null,
    val color: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    val location: String? = null,
    val description: String? = null,
    val temperament: String? = null,
    val contact: String? = null,
    @SerialName("contact_phone")
    val contactPhone: String? = null
)