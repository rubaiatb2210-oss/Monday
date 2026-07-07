package dev.monday.domain.skill

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.monday.core.model.ContextSnapshot
import javax.inject.Inject

/**
 * Skill: Launch apps by name.
 *
 * Handles: "open Gmail", "launch Slack", "open settings"
 */
class AppLaunchSkill @Inject constructor(
    @ApplicationContext private val context: Context
) : Skill {
    override val id = "app_launch"
    override val name = "App Launch"
    override val description = "Launch installed apps by name"

    // Common app name → package mappings
    private val knownApps = mapOf(
        "gmail" to "com.google.android.gm",
        "calendar" to "com.google.android.calendar",
        "classroom" to "com.google.android.apps.classroom",
        "slack" to "com.Slack",
        "discord" to "com.discord",
        "chrome" to "com.android.chrome",
        "youtube" to "com.google.android.youtube",
        "maps" to "com.google.android.apps.maps",
        "drive" to "com.google.android.apps.docs",
        "photos" to "com.google.android.apps.photos",
        "camera" to "com.sonyericsson.android.camera",
        "settings" to "com.android.settings",
        "clock" to "com.google.android.deskclock",
        "messages" to "com.google.android.apps.messaging",
        "phone" to "com.google.android.dialer",
        "spotify" to "com.spotify.music",
        "whatsapp" to "com.whatsapp",
        "telegram" to "org.telegram.messenger",
        "notion" to "notion.id",
        "github" to "com.github.android"
    )

    private val pattern = Regex(
        """(?:open|launch|start|go\s+to|show)\s+(.+)""",
        RegexOption.IGNORE_CASE
    )

    override fun canHandle(input: String): Float {
        val lower = input.lowercase()
        return when {
            pattern.containsMatchIn(lower) -> 0.85f
            lower.startsWith("open ") -> 0.8f
            else -> 0f
        }
    }

    override fun extractParams(input: String, context: ContextSnapshot): Map<String, Any> {
        val match = pattern.find(input)
        val appName = match?.groupValues?.get(1)?.trim()?.lowercase() ?: input.lowercase()
        return mapOf("appName" to appName)
    }

    override suspend fun execute(params: Map<String, Any>, context: ContextSnapshot): SkillResult {
        val appName = (params["appName"] as? String)?.lowercase()
            ?: return SkillResult.Error("No app name provided")

        val packageName = knownApps[appName]
            ?: knownApps.entries.find { appName.contains(it.key) }?.value
            ?: findPackageByLabel(appName)

        if (packageName == null) {
            return SkillResult.Error("I couldn't find an app called \"$appName\"")
        }

        return try {
            val launchIntent = this.context.packageManager
                .getLaunchIntentForPackage(packageName)
                ?: return SkillResult.Error("$appName is installed but can't be launched")

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.context.startActivity(launchIntent)

            SkillResult.Success(
                message = "Opening $appName",
                spokenResponse = "Opening $appName"
            )
        } catch (e: Exception) {
            SkillResult.Error("Failed to open $appName: ${e.message}")
        }
    }

    private fun findPackageByLabel(name: String): String? {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return apps.find {
            it.loadLabel(pm).toString().lowercase().contains(name)
        }?.activityInfo?.packageName
    }
}
