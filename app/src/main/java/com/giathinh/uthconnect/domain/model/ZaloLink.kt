package com.giathinh.uthconnect.domain.model

/**
 * Quy tắc miền Zalo sẵn có của module: [HousingListing.contactZaloUrl] là link
 * `zalo.me/...`, cả form đăng tin lẫn màn chi tiết đều ghép fallback
 * `https://zalo.me/<sđt>`. Chỉ chấp nhận HTTPS trên đúng host đó để link lưu
 * trong tin không mở sang miền lạ (kể cả tin đời cũ lưu trước khi có kiểm tra).
 */
object ZaloLink {

    private val pattern = Regex("^https://zalo\\.me(/.*)?$", RegexOption.IGNORE_CASE)

    fun isValid(url: String): Boolean = pattern.matches(url.trim())

    fun forPhone(phone: String): String = "https://zalo.me/${phone.trim()}"
}
