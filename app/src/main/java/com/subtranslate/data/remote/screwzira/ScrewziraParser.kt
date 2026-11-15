package com.subtranslate.data.remote.screwzira

object ScrewziraParser {
    fun parseSearchCandidates(body: String): List<ScrewziraSearchCandidate> {
        val payload = extractWrappedPayload(body) ?: return emptyList()
        val filmsBlock = Regex("\"Films\"\\s*:\\s*\\[(.*)]", RegexOption.DOT_MATCHES_ALL)
            .find(payload)
            ?.groupValues
            ?.getOrNull(1)
            ?: return emptyList()

        val objectRegex = Regex("\\{(.*?)\\}", RegexOption.DOT_MATCHES_ALL)
        return objectRegex.findAll(filmsBlock).mapNotNull { match ->
            val item = match.value
            val id = extractString(item, "ID") ?: return@mapNotNull null
            ScrewziraSearchCandidate(
                id = id,
                englishName = extractString(item, "EngName"),
                hebrewName = extractString(item, "HebName"),
                releaseYear = extractInt(item, "ReleaseDate"),
                isSeries = extractBoolean(item, "IsSeries") ?: false,
                imdbId = extractString(item, "ImdbID"),
            )
        }.toList()
    }

    fun pickBestCandidate(
        query: String,
        isSeries: Boolean,
        releaseYear: Int?,
        candidates: List<ScrewziraSearchCandidate>,
    ): ScrewziraSearchCandidate? {
        val normalizedQuery = normalize(query)
        return candidates
            .asSequence()
            .filter { it.isSeries == isSeries }
            .map { it to candidateScore(normalizedQuery, releaseYear, it) }
            .filter { (_, score) -> score > 0 }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    fun parseSubtitleEntries(body: String): List<ScrewziraSubtitleEntry> {
        val rowRegex = Regex("<tr>(.+?)</tr>", RegexOption.DOT_MATCHES_ALL)
        val itemRegex = Regex(
            "<div style=\"float.+?>\\s*(.+?)<br />.+?data-subtitle-id=\"(.+?)\"",
            RegexOption.DOT_MATCHES_ALL,
        )

        return rowRegex.findAll(body).mapNotNull { row ->
            val match = itemRegex.find(row.value) ?: return@mapNotNull null
            val name = match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val subtitleId = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ScrewziraSubtitleEntry(subtitleId = subtitleId, name = htmlUnescape(name))
        }.toList()
    }

    fun parseDownloadIdentifier(body: String): String? {
        val payload = extractWrappedPayload(body) ?: return null
        return extractString(payload, "DownloadIdentifier")
    }

    private fun extractWrappedPayload(body: String): String? {
        val match = Regex("\"d\"\\s*:\\s*\"(.*)\"", RegexOption.DOT_MATCHES_ALL).find(body)
            ?: return null
        return jsonUnescape(match.groupValues[1])
    }

    private fun extractString(body: String, key: String): String? {
        return Regex("\"$key\"\\s*:\\s*\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::jsonUnescape)
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractInt(body: String, key: String): Int? {
        return Regex("\"$key\"\\s*:\\s*(\\d+)")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun extractBoolean(body: String, key: String): Boolean? {
        return Regex("\"$key\"\\s*:\\s*(true|false)")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.toBooleanStrictOrNull()
    }

    private fun candidateScore(
        normalizedQuery: String,
        releaseYear: Int?,
        candidate: ScrewziraSearchCandidate,
    ): Int {
        var score = 0
        val normalizedEnglish = normalize(candidate.englishName)
        val normalizedHebrew = normalize(candidate.hebrewName)
        if (normalizedEnglish == normalizedQuery) score += 100
        if (normalizedHebrew == normalizedQuery) score += 95
        if (normalizedEnglish.isNotBlank() &&
            (normalizedEnglish.contains(normalizedQuery) || normalizedQuery.contains(normalizedEnglish))
        ) score += 25
        if (normalizedHebrew.isNotBlank() &&
            (normalizedHebrew.contains(normalizedQuery) || normalizedQuery.contains(normalizedHebrew))
        ) score += 20
        if (releaseYear != null && releaseYear == candidate.releaseYear) score += 10
        return score
    }

    private fun normalize(value: String?): String {
        return value
            ?.lowercase()
            ?.replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
            ?.trim()
            .orEmpty()
    }

    private fun jsonUnescape(value: String): String {
        return value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    private fun htmlUnescape(value: String): String {
        return value
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
