package no.nav.security.mock.oauth2.token

import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.testutils.nimbusTokenRequest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class OAuth2TokenCallbackTest {

    private val clientId = "clientId"

    @Nested
    inner class RequestMappingTokenCallbacks {
        private val issuer1 = RequestMappingTokenCallback(
            issuerId = "issuer1",
            requestMappings = setOf(
                RequestMapping(
                    requestParam = "scope",
                    match = "scope1",
                    claims = mapOf(
                        "sub" to "subByScope1",
                        "aud" to listOf("audByScope1"),
                        "custom" to "custom1"
                    )
                ),
                RequestMapping(
                    requestParam = "grant_type",
                    match = "*",
                    claims = mapOf(
                        "sub" to "defaultSub",
                        "aud" to listOf("defaultAud"),
                    )
                )
            ),
            tokenExpiry = 120
        )

        @Test
        fun `token request with request params matching requestmapping should return specific claims from callback`() {
            val scopeShouldMatch = clientCredentialsRequest("scope" to "scope1")
            assertSoftly {
                issuer1.subject(scopeShouldMatch) shouldBe "subByScope1"
                issuer1.audience(scopeShouldMatch) shouldBe listOf("audByScope1")
                issuer1.tokenExpiry() shouldBe 120
                issuer1.addClaims(scopeShouldMatch) shouldContainAll mapOf("custom" to "custom1")
            }
        }

        @Test
        fun `token request with request params matching wildcard requestmapping should return default claims from callback`() {
            val shouldMatchAllGrantTypes = clientCredentialsRequest()
            assertSoftly {
                issuer1.subject(shouldMatchAllGrantTypes) shouldBe "defaultSub"
                issuer1.audience(shouldMatchAllGrantTypes) shouldBe listOf("defaultAud")
                issuer1.tokenExpiry() shouldBe 120
            }
        }
    }

    @Nested
    inner class DefaultOAuth2TokenCallbacks {

        @Test
        fun `client credentials request should return client_id as sub from callback`() {
            val tokenRequest = clientCredentialsRequest()
            DefaultOAuth2TokenCallback().asClue {
                it.subject(tokenRequest) shouldBe clientId
            }
        }

        @Test
        fun `oidc auth code token request should return scopes not in OIDC from audience in callback`() {
            authCodeRequest("scope" to "openid").let { tokenRequest ->
                DefaultOAuth2TokenCallback().asClue {
                    it.audience(tokenRequest) shouldBe emptyList()
                }
            }
            authCodeRequest("scope" to "openid scope1").let { tokenRequest ->
                DefaultOAuth2TokenCallback().asClue {
                    it.audience(tokenRequest) shouldBe listOf("scope1")
                }
            }
        }
    }

    private fun authCodeRequest(vararg formParams: Pair<String, String>) =
        nimbusTokenRequest(
            clientId,
            "grant_type" to "authorization_code",
            "code" to "123",
            *formParams
        )

    private fun clientCredentialsRequest(vararg formParams: Pair<String, String>) =
        nimbusTokenRequest(clientId, "grant_type" to "client_credentials", *formParams)
}