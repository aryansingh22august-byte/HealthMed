package com.example.data.user

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String = "John Doe",
    val initials: String = "JD",
    val age: Int = 30,
    val weightKg: Float = 70.0f,
    val heightCm: Float = 170.0f,
    val bloodReportStatus: String = "Not Uploaded"
)

class UserProfileManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    private val _userProfileState = MutableStateFlow(loadProfile())
    val userProfileState: StateFlow<UserProfile> = _userProfileState.asStateFlow()

    fun updateProfile(name: String, age: Int, weightKg: Float, heightCm: Float, bloodReportStatus: String) {
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").take(2)
        val profile = UserProfile(name, initials, age, weightKg, heightCm, bloodReportStatus)
        saveProfile(profile)
        _userProfileState.value = profile
    }

    private fun loadProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString("name", "John Doe") ?: "John Doe",
            initials = prefs.getString("initials", "JD") ?: "JD",
            age = prefs.getInt("age", 30),
            weightKg = prefs.getFloat("weight", 70.0f),
            heightCm = prefs.getFloat("height", 170.0f),
            bloodReportStatus = prefs.getString("blood_report", "Not Uploaded") ?: "Not Uploaded"
        )
    }

    private fun saveProfile(profile: UserProfile) {
        prefs.edit().apply {
            putString("name", profile.name)
            putString("initials", profile.initials)
            putInt("age", profile.age)
            putFloat("weight", profile.weightKg)
            putFloat("height", profile.heightCm)
            putString("blood_report", profile.bloodReportStatus)
            apply()
        }
    }
}
