package io.rudione.chatone.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiErrorParserTest {

    @Test
    fun geminiHighDemandIsOverloadedWithItsMessage() {
        val body = """[{"error":{"code":503,"message":"This model is currently experiencing high demand.","status":"UNAVAILABLE"}}]"""

        val failure = AiErrorParser.failureFor(503, body)

        assertEquals(AiFailureKind.OVERLOADED, failure.kind)
        assertEquals(503, failure.status)
        assertEquals("This model is currently experiencing high demand.", failure.detail)
    }

    @Test
    fun invalidGeminiKeyAnsweredWith400IsAnAuthProblem() {
        val body = """[{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}]"""

        assertEquals(AiFailureKind.UNAUTHORIZED, AiErrorParser.failureFor(400, body).kind)
    }

    @Test
    fun statusCodesMapToKinds() {
        assertEquals(AiFailureKind.UNAUTHORIZED, AiErrorParser.failureFor(401, "").kind)
        assertEquals(AiFailureKind.MODEL_NOT_FOUND, AiErrorParser.failureFor(404, "").kind)
        assertEquals(AiFailureKind.RATE_LIMITED, AiErrorParser.failureFor(429, "").kind)
        assertEquals(AiFailureKind.SERVER, AiErrorParser.failureFor(500, "").kind)
        assertEquals(AiFailureKind.BAD_REQUEST, AiErrorParser.failureFor(422, "").kind)
    }

    @Test
    fun openAiStyleAndPlainErrorsGiveTheMessage() {
        assertEquals("model not found", AiErrorParser.failureFor(404, """{"error":{"message":"model not found"}}""").detail)
        assertEquals("boom", AiErrorParser.failureFor(500, """{"error":"boom"}""").detail)
        assertNull(AiErrorParser.failureFor(502, "<html>Bad gateway</html>").detail)
    }

    @Test
    fun retryAfterIsReadInSeconds() {
        assertEquals(3_000L, AiErrorParser.retryAfterMs("3"))
        assertNull(AiErrorParser.retryAfterMs("Wed, 21 Oct 2026 07:28:00 GMT"))
        assertNull(AiErrorParser.retryAfterMs(null))
    }
}
