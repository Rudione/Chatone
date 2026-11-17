package io.rudione.chatone.domain.moderation

enum class ForbiddenCategory { HOMOPHOBIC, TRANSPHOBIC, RACIAL, ETHNIC, ABLEIST }

enum class ForbiddenScript { CYRILLIC, LATIN }

class ForbiddenTerm(
    val category: ForbiddenCategory,
    val script: ForbiddenScript,
    val regex: Regex,
    val collapsesRepeats: Boolean
)

data class ForbiddenHit(val start: Int, val end: Int, val category: ForbiddenCategory)
