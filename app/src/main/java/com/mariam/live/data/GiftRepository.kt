package com.mariam.live.data

import com.mariam.live.model.Gift

class GiftRepository {

    private val gifts = mutableListOf<Gift>()

    fun getGifts(): List<Gift> = gifts

    fun addGift(gift: Gift) {
        gifts.add(gift)
    }

    fun findGift(id: String): Gift? {
        return gifts.find { it.id == id }
    }
}
