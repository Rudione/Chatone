package io.rudione.chatone.data.remote

data class AiCloudProvider(
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val keyUrl: String,
    val needsKey: Boolean = true
)

data class AiLocalModel(
    val name: String,
    val sizeLabel: String
)

object AiProviders {
    val cloud = listOf(
        AiCloudProvider(
            "ChatGPT (OpenAI)",
            "https://api.openai.com/v1",
            "gpt-4o-mini",
            "https://platform.openai.com/api-keys"
        ),
        AiCloudProvider(
            "Claude (Anthropic)",
            "https://api.anthropic.com/v1",
            "claude-3-5-sonnet-latest",
            "https://console.anthropic.com/settings/keys"
        ),
        AiCloudProvider(
            "Grok (xAI)",
            "https://api.x.ai/v1",
            "grok-2-latest",
            "https://console.x.ai"
        ),
        AiCloudProvider(
            "DeepSeek",
            "https://api.deepseek.com/v1",
            "deepseek-chat",
            "https://platform.deepseek.com/api_keys"
        ),
        AiCloudProvider(
            "Qwen (DashScope)",
            "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
            "qwen-plus",
            "https://dashscope.console.aliyun.com/apiKey"
        )
    )

    const val LOCAL_BASE_URL = "http://localhost:11434/v1"

    val local = listOf(
        AiLocalModel("llama3.2:1b", "~1.3 GB"),
        AiLocalModel("llama3.2:3b", "~2.0 GB"),
        AiLocalModel("qwen2.5:3b", "~1.9 GB"),
        AiLocalModel("gemma2:2b", "~1.6 GB"),
        AiLocalModel("phi3:mini", "~2.2 GB"),
        AiLocalModel("qwen2.5:7b", "~4.7 GB"),
        AiLocalModel("deepseek-r1:7b", "~4.7 GB"),
        AiLocalModel("mistral:7b", "~4.1 GB")
    )
}

fun formatModelSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val gb = bytes.toDouble() / 1_000_000_000.0
    if (gb >= 1.0) {
        val rounded = (gb * 10).toLong() / 10.0
        return "$rounded GB"
    }
    val mb = bytes.toDouble() / 1_000_000.0
    return "${mb.toLong()} MB"
}

fun formatDownloadSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return ""
    val mb = bytesPerSec.toDouble() / 1_000_000.0
    if (mb >= 1.0) {
        val rounded = (mb * 10).toLong() / 10.0
        return "$rounded MB/s"
    }
    val kb = bytesPerSec.toDouble() / 1000.0
    return "${kb.toLong()} KB/s"
}

fun formatEta(seconds: Long): String {
    if (seconds < 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val sec = seconds % 60
    fun pad(v: Long) = v.toString().padStart(2, '0')
    return if (h > 0) "~$h:${pad(m)}:${pad(sec)}" else "~$m:${pad(sec)}"
}
