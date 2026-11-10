package dev.frost819.newbv.biliapi.repositories

class AuthRepository {
    @Volatile var sessionData: String? = null

    @Volatile var biliJct: String? = null

    @Volatile var accessToken: String? = null

    @Volatile var mid: Long? = null

    @Volatile var buvid3: String? = null

    @Volatile var buvid: String? = null

    @Volatile var deviceCookies: String? = null
}
