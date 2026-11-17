package com.civis.mobile

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** Optional remembered credentials encrypted with a non-exportable Android Keystore key. */
internal class SecureCredentials(context: Context) {
    private val preferences = context.getSharedPreferences("civis_credentials", Context.MODE_PRIVATE)
    private val alias = "civis.remembered.credentials.v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    fun save(serverUrl: String, identification: String, password: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(serverUrl.trim().trimEnd('/').toByteArray(Charsets.UTF_8))
        val payload = JSONObject().put("identification", identification).put("password", password)
        val encrypted = cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun read(serverUrl: String): Pair<String, String>? {
        val encrypted = preferences.getString("ciphertext", null) ?: return null
        return runCatching {
            val iv = Base64.decode(preferences.getString("iv", ""), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.updateAAD(serverUrl.trim().trimEnd('/').toByteArray(Charsets.UTF_8))
            val plain = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
            val payload = JSONObject(String(plain, Charsets.UTF_8))
            payload.getString("identification") to payload.getString("password")
        }.getOrNull()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
