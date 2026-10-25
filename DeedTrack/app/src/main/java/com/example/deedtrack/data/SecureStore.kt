package com.example.deedtrack.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Saves drafts on the phone encrypted with AES-256-GCM.
 * The key lives in the Android Keystore, so it never leaves the device and
 * the saved text can't be read by other apps or from a backup.
 */
class SecureDraftStorage(context: Context) : DraftStorage {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("secure_drafts", Context.MODE_PRIVATE)

    override fun load(ownerEmail: String): ApplicationDraft? {
        val stored = prefs.getString(keyFor(ownerEmail), null) ?: return null
        return runCatching { DraftCodec.decode(decrypt(stored)) }.getOrNull()
    }

    override fun save(ownerEmail: String, draft: ApplicationDraft) {
        runCatching { prefs.edit().putString(keyFor(ownerEmail), encrypt(DraftCodec.encode(draft))).apply() }
    }

    override fun clear(ownerEmail: String) {
        prefs.edit().remove(keyFor(ownerEmail)).apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun keyFor(email: String) = "draft_" + email.lowercase().hashCode()

    // ---------- Android Keystore encryption ----------

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(bytes.copyOfRange(IV_SIZE, bytes.size)), Charsets.UTF_8)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "al_mobile_drafts"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}
