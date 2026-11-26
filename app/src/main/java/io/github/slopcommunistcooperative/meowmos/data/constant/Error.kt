package io.github.slopcommunistcooperative.meowmos.data.constant

import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.ext.string

class MeowmosException(string: String) : Exception(string) {
    companion object {
        val notLogin = MeowmosException("NOT_LOGIN")
        val invalidAccessToken = MeowmosException("INVALID_ACCESS_TOKEN")
        val accessTokenInvalid = MeowmosException("ACCESS_TOKEN_INVALID")
        val invalidParameter = MeowmosException("INVALID_PARAMETER")
        val invalidServer = MeowmosException("INVALID_SERVER")
    }

    override fun getLocalizedMessage(): String? {
        return when (this) {
            invalidAccessToken -> R.string.invalid_access_token.string
            accessTokenInvalid -> R.string.access_token_invalid_relogin.string
            invalidServer -> R.string.invalid_server.string
            else -> {
                super.getLocalizedMessage()
            }
        }
    }
}
