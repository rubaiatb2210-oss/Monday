package dev.monday.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.monday.domain.skill.*

/**
 * Hilt module that binds all Skills into a Set<Skill>.
 * To add a new skill, simply add another @Binds @IntoSet function.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SkillModule {

    @Binds @IntoSet
    abstract fun bindAlarmSkill(skill: AlarmSkill): Skill

    @Binds @IntoSet
    abstract fun bindReminderSkill(skill: ReminderSkill): Skill

    @Binds @IntoSet
    abstract fun bindAppLaunchSkill(skill: AppLaunchSkill): Skill

    @Binds @IntoSet
    abstract fun bindScheduleQuerySkill(skill: ScheduleQuerySkill): Skill

    @Binds @IntoSet
    abstract fun bindConversationSkill(skill: ConversationSkill): Skill
}
