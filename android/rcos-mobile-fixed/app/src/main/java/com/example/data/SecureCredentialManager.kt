package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureCredentialManager(private val context: Context) {

    private val fallbackPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("rcos_secure_fallback_prefs", Context.MODE_PRIVATE)
    }

    private val insecurePrefs: SharedPreferences by lazy {
        context.getSharedPreferences("rcos_user_prefs", Context.MODE_PRIVATE)
    }

    private val encryptedPrefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "rcos_secure_keystore_prefs_v1",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            Log.w("SecureCredentialManager", "EncryptedSharedPreferences unavailable, falling back to private prefs: ${t.message}")
            null
        }
    }

    private fun getActivePrefs(): SharedPreferences {
        return encryptedPrefs ?: fallbackPrefs
    }

    init {
        try {
            migrateInsecurePreferences()
            seedInitialDefaultVaultIfNeeded()
        } catch (t: Throwable) {
            Log.w("SecureCredentialManager", "Init migration/seeding notice: ${t.message}")
        }
    }

    private fun migrateInsecurePreferences() {
        try {
            val sensitiveKeys = listOf(
                "logged_in_email",
                "profile_full_name",
                "profile_title",
                "profile_email",
                "profile_phone",
                "profile_google_email",
                "profile_ms_email",
                "profile_org_name",
                "profile_timezone"
            )

            val active = getActivePrefs()
            val editor = active.edit()
            val insecureEditor = insecurePrefs.edit()
            var migrated = false

            for (key in sensitiveKeys) {
                if (insecurePrefs.contains(key)) {
                    val value = insecurePrefs.getString(key, null)
                    if (value != null) {
                        editor.putString(key, value)
                        insecureEditor.remove(key)
                        migrated = true
                    }
                }
            }

            if (migrated) {
                editor.apply()
                insecureEditor.apply()
                Log.d("SecureCredentialManager", "Migrated sensitive preferences to secure store")
            }
        } catch (t: Throwable) {
            Log.w("SecureCredentialManager", "Error during preferences migration: ${t.message}")
        }
    }

    private fun seedInitialDefaultVaultIfNeeded() {
        try {
            if (getVaultKeys().isEmpty()) {
                // Gemini credentials are intentionally not bundled in the APK.
                // Gemini requests are routed through the authenticated Cloudflare Worker.
                saveVaultItem(
                    id = "gemini_api_key",
                    name = "Gemini API Key (server-side)",
                    secretValue = "SERVER_SIDE_ONLY",
                    category = "AI Service Token"
                )
                saveVaultItem(
                    id = "google_workspace_oauth",
                    name = "Google Workspace Service OAuth Secret",
                    secretValue = "GOWS_sec_994827103847_rcos_enterprise",
                    category = "OAuth2 Client Secret"
                )
                saveVaultItem(
                    id = "microsoft_azure_token",
                    name = "Microsoft 365 Azure AD App Key",
                    secretValue = "MS365_azure_secret_882910103726102",
                    category = "Enterprise Auth Token"
                )
            }
        } catch (t: Throwable) {
            Log.w("SecureCredentialManager", "Failed to seed default vault: ${t.message}")
        }
    }

    fun saveString(key: String, value: String) {
        try {
            getActivePrefs().edit().putString(key, value).apply()
        } catch (t: Throwable) {
            Log.w("SecureCredentialManager", "saveString failed on primary prefs, saving to fallback: ${t.message}")
            try {
                fallbackPrefs.edit().putString(key, value).apply()
            } catch (ignored: Throwable) {}
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            getActivePrefs().getString(key, defaultValue) ?: defaultValue
        } catch (t: Throwable) {
            try {
                fallbackPrefs.getString(key, defaultValue) ?: defaultValue
            } catch (ignored: Throwable) {
                defaultValue
            }
        }
    }

    fun getNullableString(key: String): String? {
        return try {
            val prefs = getActivePrefs()
            if (prefs.contains(key)) prefs.getString(key, null) else null
        } catch (t: Throwable) {
            try {
                if (fallbackPrefs.contains(key)) fallbackPrefs.getString(key, null) else null
            } catch (ignored: Throwable) {
                null
            }
        }
    }

    fun remove(key: String) {
        try {
            getActivePrefs().edit().remove(key).apply()
        } catch (t: Throwable) {
            try {
                fallbackPrefs.edit().remove(key).apply()
            } catch (ignored: Throwable) {}
        }
    }

    fun clearAll() {
        try {
            getActivePrefs().edit().clear().apply()
            insecurePrefs.edit().clear().apply()
            fallbackPrefs.edit().clear().apply()
        } catch (t: Throwable) {
            Log.w("SecureCredentialManager", "Error in clearAll: ${t.message}")
        }
    }

    // Vault Credentials Management (Android Keystore Encrypted)
    fun saveVaultItem(id: String, name: String, secretValue: String, category: String = "API Key") {
        saveString("vault_name_$id", name)
        saveString("vault_secret_$id", secretValue)
        saveString("vault_category_$id", category)

        val keys = getVaultKeys().toMutableSet()
        keys.add(id)
        saveString("vault_keys_list", keys.joinToString(","))
    }

    fun getVaultKeys(): List<String> {
        val raw = getString("vault_keys_list", "")
        if (raw.isBlank()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun getVaultItem(id: String): VaultItem? {
        val name = getString("vault_name_$id")
        if (name.isBlank()) return null
        val secret = getString("vault_secret_$id")
        val category = getString("vault_category_$id").ifBlank { "API Key" }
        return VaultItem(id, name, secret, category)
    }

    fun deleteVaultItem(id: String) {
        remove("vault_name_$id")
        remove("vault_secret_$id")
        remove("vault_category_$id")

        val keys = getVaultKeys().filter { it != id }
        saveString("vault_keys_list", keys.joinToString(","))
    }

    fun getAllVaultItems(): List<VaultItem> {
        return getVaultKeys().mapNotNull { getVaultItem(it) }
    }
}

data class VaultItem(
    val id: String,
    val name: String,
    val secretValue: String,
    val category: String
)
