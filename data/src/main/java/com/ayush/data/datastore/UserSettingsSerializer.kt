package com.ayush.data.datastore

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object UserSettingsSerializer : Serializer<UserSettings> {
    override val defaultValue: UserSettings
        get() = UserSettings()

    override suspend fun readFrom(input: InputStream): UserSettings {
        return try {
            Json.decodeFromString(
                UserSettings.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (serializationException: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(
                    UserSettings.serializer(),
                    t
                ).encodeToByteArray()
            )
        }
    }
}