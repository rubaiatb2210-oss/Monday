package dev.monday.domain.rule

import dev.monday.data.database.entity.NotificationEntity
import dev.monday.data.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule Engine — deterministic logic processor.
 *
 * All rules are evaluated in order. No AI involved.
 * Handles: spam detection, duplicate detection, priority scoring.
 */
@Singleton
class RuleEngine @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    // Known important packages (high base score)
    private val importantPackages = mapOf(
        "com.google.android.gm" to 0.8f,              // Gmail
        "com.google.android.calendar" to 0.85f,         // Calendar
        "com.google.android.apps.classroom" to 0.9f,    // Classroom
        "com.Slack" to 0.75f,                           // Slack
        "com.discord" to 0.6f,                          // Discord
        "com.google.android.apps.messaging" to 0.7f,    // Messages
        "com.google.android.dialer" to 0.8f             // Phone
    )

    // Spam keywords (penalty)
    private val spamKeywords = setOf(
        "sale", "offer", "discount", "deal", "promo",
        "unsubscribe", "limited time", "act now", "free",
        "win", "congratulations", "claim", "gift card",
        "advertisement", "sponsored"
    )

    // Important keywords (bonus)
    private val importantKeywords = setOf(
        "deadline", "urgent", "important", "due", "overdue",
        "meeting", "exam", "quiz", "assignment", "submission",
        "cancel", "changed", "updated", "emergency", "asap",
        "grade", "professor", "instructor"
    )

    /**
     * Score a notification from 0.0 (ignore) to 1.0 (critical).
     */
    fun scoreNotification(
        packageName: String,
        title: String?,
        text: String?
    ): Float {
        var score = 0f
        val combinedText = "${title ?: ""} ${text ?: ""}".lowercase()

        // Factor 1: Source package importance (weight 0.3)
        val packageScore = importantPackages[packageName] ?: 0.3f
        score += packageScore * 0.3f

        // Factor 2: Important keywords (weight 0.25)
        val importantHits = importantKeywords.count { combinedText.contains(it) }
        val keywordScore = (importantHits.coerceAtMost(3) / 3f)
        score += keywordScore * 0.25f

        // Factor 3: Spam penalty (weight 0.2)
        val spamHits = spamKeywords.count { combinedText.contains(it) }
        val spamPenalty = (spamHits.coerceAtMost(3) / 3f)
        score -= spamPenalty * 0.2f

        // Factor 4: Content length — extremely short or empty = lower value (weight 0.1)
        val contentLength = combinedText.length
        val contentScore = when {
            contentLength < 5 -> 0.2f
            contentLength < 20 -> 0.5f
            contentLength < 100 -> 0.8f
            else -> 1.0f
        }
        score += contentScore * 0.1f

        // Factor 5: Base presence (weight 0.15) — every notification starts with some value
        score += 0.15f

        return score.coerceIn(0f, 1f)
    }

    /**
     * Check if a notification is a duplicate of a recent one.
     */
    suspend fun isDuplicate(title: String?, text: String?): Boolean {
        if (title == null && text == null) return false
        val duplicates = notificationRepository.findDuplicates(title, text, withinMs = 5 * 60 * 1000)
        return duplicates.isNotEmpty()
    }

    /**
     * Check if content is spam.
     */
    fun isSpam(title: String?, text: String?): Boolean {
        val combinedText = "${title ?: ""} ${text ?: ""}".lowercase()
        val spamHits = spamKeywords.count { combinedText.contains(it) }
        return spamHits >= 2
    }
}
