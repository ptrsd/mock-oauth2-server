package no.nav.security.mock.oauth2.grant

import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.security.mock.callback.TokenCallback
import no.nav.security.mock.oauth2.OAuth2TokenResponse
import okhttp3.HttpUrl

interface GrantHandler {
    fun tokenResponse(
        tokenRequest: TokenRequest,
        issuerUrl: HttpUrl,
        tokenCallback: TokenCallback
    ): OAuth2TokenResponse
}