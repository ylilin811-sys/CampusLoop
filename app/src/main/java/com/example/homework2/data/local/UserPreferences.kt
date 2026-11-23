package com.example.homework2.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val KEY_STEP_GUIDE_COMPLETED = booleanPreferencesKey("is_step_guide_completed")
        val KEY_CONTACT_SECURITY_ALERT_READ = booleanPreferencesKey("is_contact_security_alert_read")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isStepGuideCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_STEP_GUIDE_COMPLETED] ?: false
    }

    val isContactSecurityAlertRead: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONTACT_SECURITY_ALERT_READ] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setStepGuideCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STEP_GUIDE_COMPLETED] = completed
        }
    }

    suspend fun setContactSecurityAlertRead(read: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONTACT_SECURITY_ALERT_READ] = read
        }
    }

    suspend fun resetAllGuides() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = false
            prefs[KEY_STEP_GUIDE_COMPLETED] = false
            prefs[KEY_CONTACT_SECURITY_ALERT_READ] = false
        }
    }
}
