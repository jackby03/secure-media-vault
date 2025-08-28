package com.acme.vault.adapter.web.util

import com.acme.vault.application.service.TokenService
import com.acme.vault.domain.models.Role
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.server.ServerWebExchange
import org.springframework.http.server.reactive.ServerHttpRequest
import java.util.*

@DisplayName("AuthenticationHelper Tests")
class AuthenticationHelperTest {

    private val tokenService = mockk<TokenService>()
    private val authenticationHelper = AuthenticationHelper(tokenService)
    private val testUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("User Role Extraction Tests")
    inner class UserRoleExtractionTests {

        @Test
        fun `should return ADMIN role when user has ROLE_ADMIN authority`() {
            val authentication = mockk<Authentication>()
            val authorities = listOf(
                SimpleGrantedAuthority("ROLE_ADMIN"),
                SimpleGrantedAuthority("ROLE_EDITOR")
            )
            
            every { authentication.authorities } returns authorities
            
            val result = authenticationHelper.getUserRoleFromAuthentication(authentication)
            
            assertEquals(Role.ADMIN, result)
        }

        @Test
        fun `should return EDITOR role when user has ROLE_EDITOR but not ROLE_ADMIN`() {
            val authentication = mockk<Authentication>()
            val authorities = listOf(
                SimpleGrantedAuthority("ROLE_EDITOR"),
                SimpleGrantedAuthority("ROLE_VIEWER")
            )
            
            every { authentication.authorities } returns authorities
            
            val result = authenticationHelper.getUserRoleFromAuthentication(authentication)
            
            assertEquals(Role.EDITOR, result)
        }

        @Test
        fun `should return VIEWER role when user only has ROLE_VIEWER authority`() {
            val authentication = mockk<Authentication>()
            val authorities = listOf(SimpleGrantedAuthority("ROLE_VIEWER"))
            
            every { authentication.authorities } returns authorities
            
            val result = authenticationHelper.getUserRoleFromAuthentication(authentication)
            
            assertEquals(Role.VIEWER, result)
        }

        @Test
        fun `should return VIEWER role when user has no recognized authorities`() {
            val authentication = mockk<Authentication>()
            val authorities = listOf(
                SimpleGrantedAuthority("ROLE_UNKNOWN"),
                SimpleGrantedAuthority("SOME_OTHER_AUTHORITY")
            )
            
            every { authentication.authorities } returns authorities
            
            val result = authenticationHelper.getUserRoleFromAuthentication(authentication)
            
            assertEquals(Role.VIEWER, result)
        }

        @Test
        fun `should return VIEWER role when user has empty authorities`() {
            val authentication = mockk<Authentication>()
            val authorities = emptyList<GrantedAuthority>()
            
            every { authentication.authorities } returns authorities
            
            val result = authenticationHelper.getUserRoleFromAuthentication(authentication)
            
            assertEquals(Role.VIEWER, result)
        }

        @Test
        fun `should prioritize ADMIN over other roles`() {
            val authentication = mockk<Authentication>()
            val authorities = listOf(
                SimpleGrantedAuthority("ROLE_VIEWER"),
                SimpleGrantedAuthority("ROLE_ADMIN"),
                SimpleGrantedAuthority("ROLE_EDITOR")
            )
            
            every { authentication.authorities } returns authorities
            
            val result = authenticationHelper.getUserRoleFromAuthentication(authentication)
            
            assertEquals(Role.ADMIN, result)
        }

        @Test
        fun `should prioritize EDITOR over VIEWER`() {
            val authentication = mockk<Authentication>()
            val authorities = listOf(
                SimpleGrantedAuthority("ROLE_VIEWER"),
                SimpleGrantedAuthority("ROLE_EDITOR")
            )
            
            every { authentication.authorities } returns authorities
            
            val result = authenticationHelper.getUserRoleFromAuthentication(authentication)
            
            assertEquals(Role.EDITOR, result)
        }
    }

    @Nested
    @DisplayName("User ID Extraction Tests")
    inner class UserIdExtractionTests {

        @Test
        fun `should extract user ID from valid Bearer token`() {
            val token = "valid.jwt.token"
            val authHeader = "Bearer $token"
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(authHeader)
            
            every { tokenService.extractUserId(token) } returns testUserId.toString()
            
            val result = authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            
            assertEquals(testUserId, result)
            verify { tokenService.extractUserId(token) }
        }

        @Test
        fun `should throw exception when Authorization header is missing`() {
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(null)
            
            val exception = assertThrows(IllegalStateException::class.java) {
                authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            }
            
            assertEquals("Authorization header not found", exception.message)
        }

        @ParameterizedTest
        @ValueSource(strings = ["Basic token", "token", "bearer token", "BEARER token"])
        fun `should throw exception for invalid authorization header format`(invalidHeader: String) {
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(invalidHeader)
            
            val exception = assertThrows(IllegalStateException::class.java) {
                authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            }
            
            assertEquals("Invalid authorization header format", exception.message)
        }

        @Test
        fun `should throw exception when token service returns null user ID`() {
            val token = "invalid.jwt.token"
            val authHeader = "Bearer $token"
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(authHeader)
            
            every { tokenService.extractUserId(token) } returns null
            
            val exception = assertThrows(IllegalStateException::class.java) {
                authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            }
            
            assertEquals("Could not extract user ID from token", exception.message)
            verify { tokenService.extractUserId(token) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["not-a-uuid", "12345", "invalid-uuid-format", ""])
        fun `should throw exception for invalid UUID format`(invalidUuid: String) {
            val token = "valid.jwt.token"
            val authHeader = "Bearer $token"
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(authHeader)
            
            every { tokenService.extractUserId(token) } returns invalidUuid
            
            val exception = assertThrows(IllegalStateException::class.java) {
                authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            }
            
            assertTrue(exception.message!!.contains("Invalid user ID format"))
            assertTrue(exception.message!!.contains(invalidUuid))
            verify { tokenService.extractUserId(token) }
        }

        @Test
        fun `should handle Bearer token with extra spaces`() {
            val token = "valid.jwt.token"
            val authHeader = "Bearer  $token" // Extra space
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(authHeader)
            
            every { tokenService.extractUserId(" $token") } returns testUserId.toString()
            
            val result = authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            
            assertEquals(testUserId, result)
            verify { tokenService.extractUserId(" $token") }
        }

        @Test
        fun `should handle minimum length Bearer token`() {
            val token = "a"
            val authHeader = "Bearer $token"
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(authHeader)
            
            every { tokenService.extractUserId(token) } returns testUserId.toString()
            
            val result = authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            
            assertEquals(testUserId, result)
            verify { tokenService.extractUserId(token) }
        }

        @Test
        fun `should handle empty token after Bearer prefix`() {
            val authHeader = "Bearer "
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(authHeader)
            
            every { tokenService.extractUserId("") } returns null
            
            val exception = assertThrows(IllegalStateException::class.java) {
                authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            }
            
            assertEquals("Could not extract user ID from token", exception.message)
            verify { tokenService.extractUserId("") }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {

        @Test
        fun `should successfully extract both role and user ID from valid authentication context`() {
            // Test role extraction
            val authentication = mockk<Authentication>()
            val authorities = listOf(SimpleGrantedAuthority("ROLE_EDITOR"))
            every { authentication.authorities } returns authorities
            
            val role = authenticationHelper.getUserRoleFromAuthentication(authentication)
            assertEquals(Role.EDITOR, role)
            
            // Test user ID extraction
            val token = "valid.jwt.token"
            val authHeader = "Bearer $token"
            val exchange = createMockExchange(authHeader)
            every { tokenService.extractUserId(token) } returns testUserId.toString()
            
            val userId = authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            assertEquals(testUserId, userId)
            
            verify { tokenService.extractUserId(token) }
        }

        @Test
        fun `should handle multiple role authorities correctly`() {
            val authentication = mockk<Authentication>()
            val authorities = listOf(
                SimpleGrantedAuthority("ROLE_VIEWER"),
                SimpleGrantedAuthority("ROLE_EDITOR"),
                SimpleGrantedAuthority("ROLE_ADMIN"),
                SimpleGrantedAuthority("CUSTOM_ROLE")
            )
            every { authentication.authorities } returns authorities
            
            val result = authenticationHelper.getUserRoleFromAuthentication(authentication)
            
            assertEquals(Role.ADMIN, result)
        }
    }

    @Nested
    @DisplayName("Constants and Configuration Tests")
    inner class ConstantsTests {

        @Test
        fun `should use correct Bearer prefix constant`() {
            val token = "test.token"
            val authHeader = "Bearer $token"
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(authHeader)
            
            every { tokenService.extractUserId(token) } returns testUserId.toString()
            
            assertDoesNotThrow {
                authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            }
            
            verify { tokenService.extractUserId(token) }
        }

        @Test
        fun `should correctly calculate Bearer prefix length`() {
            val shortToken = "x"
            val authHeader = "Bearer $shortToken"
            val authentication = mockk<Authentication>()
            val exchange = createMockExchange(authHeader)
            
            every { tokenService.extractUserId(shortToken) } returns testUserId.toString()
            
            val result = authenticationHelper.getUserIdFromAuthentication(authentication, exchange)
            
            assertEquals(testUserId, result)
            verify { tokenService.extractUserId(shortToken) }
        }
    }

    // Helper method to create mock ServerWebExchange
    private fun createMockExchange(authHeaderValue: String?): ServerWebExchange {
        val exchange = mockk<ServerWebExchange>()
        val request = mockk<ServerHttpRequest>()
        val headers = mockk<org.springframework.http.HttpHeaders>()
        
        every { exchange.request } returns request
        every { request.headers } returns headers
        every { headers.getFirst(HttpHeaders.AUTHORIZATION) } returns authHeaderValue
        
        return exchange
    }
}
