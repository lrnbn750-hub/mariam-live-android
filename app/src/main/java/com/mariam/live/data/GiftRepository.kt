package com.mariam.live.data

import com.mariam.live.AuthApi
import com.mariam.live.model.Gift
import org.json.JSONObject
import java.util.UUID

/**
 * Network repository for the authenticated room-gift API.
 * Call these methods from a background thread (never the Android main thread).
 */
class GiftRepository {

    data class GiftEvent(
        val id: Long,
        val roomId: String,
        val senderId: String,
        val recipientId: String,
        val giftId: String,
        val quantity: Int,
        val totalCoins: Long,
        val senderName: String,
        val recipientName: String,
        val name: String,
        val animationUrl: String
    )

    fun loadCatalog(token: String): AuthApi.Result {
        val response = AuthApi.get("/api/gifts", token)
        if (response.status !in 200..299) return response

        val array = response.body.optJSONArray("gifts") ?: return response
        val parsed = ArrayList<Gift>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            parsed += Gift(
                id = item.optString("gift_id"),
                name = item.optString("name"),
                imageUrl = item.optString("image_url"),
                price = item.optLong("price_coins"),
                animationUrl = item.optString("animation_url")
            )
        }
        synchronized(this) {
            gifts.clear()
            gifts.addAll(parsed)
        }
        return response
    }

    fun sendGift(
        roomId: String,
        recipientId: String,
        giftId: String,
        quantity: Int,
        token: String,
        requestId: String = UUID.randomUUID().toString().replace("-", "")
    ): AuthApi.Result {
        require(quantity in 1..99) { "الكمية يجب أن تكون بين 1 و99" }
        val payload = JSONObject()
            .put("recipient_id", recipientId)
            .put("gift_id", giftId)
            .put("quantity", quantity)
            .put("request_id", requestId)
        return AuthApi.post("/api/voice-rooms/$roomId/gifts", payload, token)
    }

    fun loadRoomEvents(
        roomId: String,
        afterId: Long,
        token: String
    ): Pair<AuthApi.Result, List<GiftEvent>> {
        val response = AuthApi.get(
            "/api/voice-rooms/$roomId/gifts?after_id=${afterId.coerceAtLeast(0)}",
            token
        )
        if (response.status !in 200..299) return response to emptyList()

        val array = response.body.optJSONArray("events") ?: return response to emptyList()
        val events = ArrayList<GiftEvent>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            events += GiftEvent(
                id = item.optLong("id"),
                roomId = item.optString("room_id"),
                senderId = item.optString("sender_id"),
                recipientId = item.optString("recipient_id"),
                giftId = item.optString("gift_id"),
                quantity = item.optInt("quantity", 1),
                totalCoins = item.optLong("total_coins"),
                senderName = item.optString("sender_name"),
                recipientName = item.optString("recipient_name"),
                name = item.optString("name"),
                animationUrl = item.optString("animation_url")
            )
        }
        return response to events
    }

    private val gifts = mutableListOf<Gift>()

    fun getGifts(): List<Gift> = synchronized(this) { gifts.toList() }

    fun addGift(gift: Gift) {
        synchronized(this) { gifts.add(gift) }
    }

    fun findGift(id: String): Gift? = synchronized(this) {
        gifts.find { it.id == id }
    }
}
