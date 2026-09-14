package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {

    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = "$password:$salt"
        val hashBytes = digest.digest(saltedPassword.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val actualHash = hashPassword(password, salt)
        return actualHash == expectedHash
    }

    data class PasswordStrength(
        val isMinLength: Boolean,
        val hasUppercase: Boolean,
        val hasLowercase: Boolean,
        val hasDigit: Boolean,
        val hasSpecialChar: Boolean
    ) {
        val isValid: Boolean get() = isMinLength && (hasUppercase || hasLowercase) && hasDigit
        val score: Int get() = listOf(isMinLength, hasUppercase, hasLowercase, hasDigit, hasSpecialChar).count { it }
        val label: String
            get() = when (score) {
                0, 1 -> "Weak"
                2, 3 -> "Fair"
                4 -> "Good"
                else -> "Strong"
            }
    }

    fun evaluatePasswordStrength(password: String): PasswordStrength {
        return PasswordStrength(
            isMinLength = password.length >= 8,
            hasUppercase = password.any { it.isUpperCase() },
            hasLowercase = password.any { it.isLowerCase() },
            hasDigit = password.any { it.isDigit() },
            hasSpecialChar = password.any { !it.isLetterOrDigit() }
        )
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.trim().matches(emailRegex.toRegex())
    }
}
