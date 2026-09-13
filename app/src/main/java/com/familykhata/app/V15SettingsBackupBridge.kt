package com.familykhata.app

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.File

object V15SettingsBackupBridge {

    private const val SETTINGS_PREFS = "hisabi_khata_v14_settings"
    private const val LANGUAGE_PREFS = "hisabi_khata_v15_language"

    fun export(
        context: Context,
        selectedWorkspace: String
    ): JSONObject {
        val prefs = context.getSharedPreferences(
            SETTINGS_PREFS,
            Context.MODE_PRIVATE
        )

        val languagePrefs = context.getSharedPreferences(
            LANGUAGE_PREFS,
            Context.MODE_PRIVATE
        )

        val logoPath = prefs.getString(
            "business_logo_path",
            ""
        ).orEmpty()

        val logoBase64 = runCatching {
            val file = File(logoPath)

            if (logoPath.isNotBlank() &&
                file.exists() &&
                file.isFile
            ) {
                Base64.encodeToString(
                    file.readBytes(),
                    Base64.NO_WRAP
                )
            } else {
                ""
            }
        }.getOrDefault("")

        return JSONObject().apply {
            put(
                "profileName",
                prefs.getString("profile_name", "").orEmpty()
            )
            put(
                "businessName",
                prefs.getString("business_name", "").orEmpty()
            )
            put(
                "profilePhone",
                prefs.getString("profile_phone", "").orEmpty()
            )
            put(
                "businessType",
                prefs.getString("business_type", "").orEmpty()
            )
            put(
                "businessAddress",
                prefs.getString("business_address", "").orEmpty()
            )

            put(
                "currencyCode",
                prefs.getString("currency_code", "BDT") ?: "BDT"
            )
            put(
                "currencySymbol",
                prefs.getString("currency_symbol", "৳") ?: "৳"
            )
            put(
                "summaryVisible",
                prefs.getBoolean("summary_visible", true)
            )

            put(
                "reminder30",
                prefs.getBoolean("reminder_30", false)
            )
            put(
                "reminder15",
                prefs.getBoolean("reminder_15", false)
            )
            put(
                "reminder7",
                prefs.getBoolean("reminder_7", true)
            )
            put(
                "reminder3",
                prefs.getBoolean("reminder_3", true)
            )
            put(
                "reminder0",
                prefs.getBoolean("reminder_0", true)
            )

            put(
                "inventoryLowStock",
                prefs.getBoolean("inventory_low_stock", true)
            )
            put(
                "expiry30",
                prefs.getBoolean("expiry_30", true)
            )
            put(
                "expiry15",
                prefs.getBoolean("expiry_15", false)
            )
            put(
                "expiry7",
                prefs.getBoolean("expiry_7", true)
            )
            put(
                "expiry3",
                prefs.getBoolean("expiry_3", true)
            )
            put(
                "expiry0",
                prefs.getBoolean("expiry_0", true)
            )

            put(
                "language",
                languagePrefs.getString("language", "bn") ?: "bn"
            )

            put(
                "selectedWorkspace",
                selectedWorkspace
            )

            put(
                "businessLogoBase64",
                logoBase64
            )
        }
    }

    fun restore(
        context: Context,
        json: JSONObject?
    ): String? {
        if (json == null) return null

        val prefs = context.getSharedPreferences(
            SETTINGS_PREFS,
            Context.MODE_PRIVATE
        )

        val languagePrefs = context.getSharedPreferences(
            LANGUAGE_PREFS,
            Context.MODE_PRIVATE
        )

        val logoFile = File(
            context.filesDir,
            "hisabi_shop_logo.img"
        )

        val logoBase64 = json.optString(
            "businessLogoBase64",
            ""
        )

        val restoredLogoPath =
            if (logoBase64.isNotBlank()) {
                runCatching {
                    logoFile.writeBytes(
                        Base64.decode(
                            logoBase64,
                            Base64.DEFAULT
                        )
                    )

                    logoFile.absolutePath
                }.getOrDefault("")
            } else {
                runCatching {
                    if (logoFile.exists()) {
                        logoFile.delete()
                    }
                }

                ""
            }

        prefs.edit()
            .putString(
                "profile_name",
                json.optString("profileName", "")
            )
            .putString(
                "business_name",
                json.optString("businessName", "")
            )
            .putString(
                "profile_phone",
                json.optString("profilePhone", "")
            )
            .putString(
                "business_type",
                json.optString("businessType", "")
            )
            .putString(
                "business_address",
                json.optString("businessAddress", "")
            )
            .putString(
                "business_logo_path",
                restoredLogoPath
            )
            .putString(
                "currency_code",
                json.optString("currencyCode", "BDT")
            )
            .putString(
                "currency_symbol",
                json.optString("currencySymbol", "৳")
            )
            .putBoolean(
                "summary_visible",
                json.optBoolean("summaryVisible", true)
            )
            .putBoolean(
                "reminder_30",
                json.optBoolean("reminder30", false)
            )
            .putBoolean(
                "reminder_15",
                json.optBoolean("reminder15", false)
            )
            .putBoolean(
                "reminder_7",
                json.optBoolean("reminder7", true)
            )
            .putBoolean(
                "reminder_3",
                json.optBoolean("reminder3", true)
            )
            .putBoolean(
                "reminder_0",
                json.optBoolean("reminder0", true)
            )
            .putBoolean(
                "inventory_low_stock",
                json.optBoolean("inventoryLowStock", true)
            )
            .putBoolean(
                "expiry_30",
                json.optBoolean("expiry30", true)
            )
            .putBoolean(
                "expiry_15",
                json.optBoolean("expiry15", false)
            )
            .putBoolean(
                "expiry_7",
                json.optBoolean("expiry7", true)
            )
            .putBoolean(
                "expiry_3",
                json.optBoolean("expiry3", true)
            )
            .putBoolean(
                "expiry_0",
                json.optBoolean("expiry0", true)
            )
            .apply()

        val language = json.optString(
            "language",
            "bn"
        )

        if (language == "bn" || language == "en") {
            languagePrefs.edit()
                .putString("language", language)
                .apply()
        }

        DueReminderScheduler.schedule(context)
        InventoryReminderScheduler.schedule(context)

        return json.optString(
            "selectedWorkspace",
            ""
        ).takeIf {
            it == "PERSONAL" ||
            it == "FAMILY" ||
            it == "SHOP"
        }
    }
}
