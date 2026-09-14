package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DashboardItem::class,
        ChatMessageEntity::class,
        ChatSessionEntity::class,
        UserEntity::class,
        CompanyProfileEntity::class,
        WorkflowTemplateEntity::class,
        WorkflowTriggerEntity::class,
        ApprovalItemEntity::class,
        AiActionLogEntity::class,
        AgentResponsibilityEntity::class,
        BusinessWorkflowConfigEntity::class,
        WorkspaceEntity::class,
        UserAccountEntity::class,
        BusinessAiConfigEntity::class,
        AuditLogEntity::class,
        AgentRegistryEntity::class,
        WorkspaceOnboardingEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun agentRegistryDao(): AgentRegistryDao
    abstract fun workspaceOnboardingDao(): WorkspaceOnboardingDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workflow_templates` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `assignedAgent` TEXT NOT NULL,
                        `triggerJson` TEXT NOT NULL,
                        `approvalPolicyJson` TEXT NOT NULL,
                        `stepsJson` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `totalExecutions` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workflow_triggers` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `type` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `configuration` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `approval_items` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `workflowTitle` TEXT NOT NULL,
                        `requestedByAgent` TEXT NOT NULL,
                        `triggerSource` TEXT NOT NULL,
                        `timestamp` TEXT NOT NULL,
                        `riskScorePct` INTEGER NOT NULL,
                        `summary` TEXT NOT NULL,
                        `proposedAction` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `reviewedBy` TEXT,
                        `reviewNotes` TEXT
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_action_logs` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `timestamp` TEXT NOT NULL,
                        `agentName` TEXT NOT NULL,
                        `workflowTitle` TEXT NOT NULL,
                        `triggerType` TEXT NOT NULL,
                        `actionSummary` TEXT NOT NULL,
                        `approvalStatus` TEXT NOT NULL,
                        `executionTimeMs` INTEGER NOT NULL,
                        `outputArtifact` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `agent_responsibilities` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `agentName` TEXT NOT NULL,
                        `roleTitle` TEXT NOT NULL,
                        `department` TEXT NOT NULL,
                        `keyResponsibilitiesJson` TEXT NOT NULL,
                        `autonomyLevel` TEXT NOT NULL,
                        `activeWorkflowsCount` INTEGER NOT NULL,
                        `actionsExecutedToday` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `business_workflow_configs` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `companyName` TEXT NOT NULL,
                        `industry` TEXT NOT NULL,
                        `operationalBottleneck` TEXT NOT NULL,
                        `workloadReductionGoalPct` INTEGER NOT NULL,
                        `autoApprovalThresholdDollars` REAL NOT NULL,
                        `maxRiskAutoApprovePct` INTEGER NOT NULL,
                        `webhookNotificationUrl` TEXT NOT NULL,
                        `emergencyHumanOverride` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workspaces` (
                        `workspaceId` TEXT NOT NULL PRIMARY KEY,
                        `companyName` TEXT NOT NULL,
                        `industry` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `primaryBottleneck` TEXT NOT NULL,
                        `targetReductionPercent` INTEGER NOT NULL,
                        `activeAgents` TEXT NOT NULL,
                        `customInstructions` TEXT NOT NULL,
                        `isConfigured` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_accounts` (
                        `userId` TEXT NOT NULL PRIMARY KEY,
                        `workspaceId` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `accessLevel` TEXT NOT NULL,
                        `department` TEXT NOT NULL,
                        `avatarColorHex` TEXT NOT NULL,
                        `registeredAt` INTEGER NOT NULL,
                        `lastActiveAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `business_ai_configs` (
                        `workspaceId` TEXT NOT NULL PRIMARY KEY,
                        `customSystemPrompt` TEXT NOT NULL,
                        `aiAgentTone` TEXT NOT NULL,
                        `allowedAutoApprovalRiskThreshold` INTEGER NOT NULL,
                        `autoApprovalDollarLimit` REAL NOT NULL,
                        `modelTier` TEXT NOT NULL,
                        `enableAutonomousActions` INTEGER NOT NULL,
                        `dataRetentionDays` INTEGER NOT NULL,
                        `dailyApiQuota` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `audit_logs` (
                        `auditId` TEXT NOT NULL PRIMARY KEY,
                        `workspaceId` TEXT NOT NULL,
                        `userId` TEXT,
                        `actorType` TEXT NOT NULL,
                        `actorName` TEXT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `resourceType` TEXT,
                        `resourceId` TEXT,
                        `agentId` TEXT,
                        `workflowId` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `previousValue` TEXT,
                        `newValue` TEXT,
                        `approvalRequired` INTEGER NOT NULL,
                        `approvalStatus` TEXT NOT NULL,
                        `result` TEXT NOT NULL,
                        `riskLevel` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_logs_workspaceId` ON `audit_logs` (`workspaceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_logs_userId` ON `audit_logs` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_logs_actionType` ON `audit_logs` (`actionType`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `agent_registry` (
                        `agentId` TEXT NOT NULL PRIMARY KEY,
                        `workspaceId` TEXT NOT NULL,
                        `agentName` TEXT NOT NULL,
                        `agentDescription` TEXT NOT NULL,
                        `agentType` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `capabilityProfile` TEXT NOT NULL,
                        `permissionLevel` TEXT NOT NULL,
                        `riskClassification` TEXT NOT NULL,
                        `modelTier` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `createdTimestamp` INTEGER NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL,
                        `assignedDepartment` TEXT,
                        `assignedUsers` TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agent_registry_workspaceId` ON `agent_registry` (`workspaceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agent_registry_agentType` ON `agent_registry` (`agentType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agent_registry_status` ON `agent_registry` (`status`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workspace_onboarding` (
                        `onboardingId` TEXT NOT NULL PRIMARY KEY,
                        `workspaceId` TEXT NOT NULL,
                        `companyName` TEXT NOT NULL,
                        `industry` TEXT NOT NULL,
                        `companySize` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `primaryBottleneck` TEXT NOT NULL,
                        `targetAutomationGoal` TEXT NOT NULL,
                        `targetReductionPercent` INTEGER NOT NULL,
                        `selectedAgentPackage` TEXT NOT NULL,
                        `governanceProfile` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `createdTimestamp` INTEGER NOT NULL,
                        `completedTimestamp` INTEGER,
                        `onboardingStatus` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_onboarding_workspaceId` ON `workspace_onboarding` (`workspaceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_onboarding_onboardingStatus` ON `workspace_onboarding` (`onboardingStatus`)")
            }
        }

        fun getDatabase(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_dashboard_db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

