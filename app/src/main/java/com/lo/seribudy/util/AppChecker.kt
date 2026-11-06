package com.lo.seribudy.util

import android.content.Context
import android.content.pm.PackageManager

/**
 * Whee ve TikTok uygulama kontrolleri
 */
object AppChecker {

    // Whee paket adı
    private const val WHEE_PACKAGE = "com.tiktok.whee"

    // TikTok paket adları (bölgeye göre değişir)
    private val TIKTOK_PACKAGES = listOf(
        "com.zhiliaoapp.musically",     // TikTok global
        "com.ss.android.ugc.trill",     // TikTok bazı bölgeler
    )

    /**
     * Whee yüklü mü kontrol et
     */
    fun isWheeInstalled(context: Context): Boolean {
        return isPackageInstalled(context, WHEE_PACKAGE)
    }

    /**
     * TikTok yüklü mü kontrol et
     */
    fun isTikTokInstalled(context: Context): Boolean {
        return TIKTOK_PACKAGES.any { isPackageInstalled(context, it) }
    }

    /**
     * Yüklü TikTok paket adını döndür
     */
    fun getTikTokPackage(context: Context): String? {
        return TIKTOK_PACKAGES.firstOrNull { isPackageInstalled(context, it) }
    }

    /**
     * Belirtilen paket yüklü mü?
     */
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * TikTok'u başlat
     */
    fun launchTikTok(context: Context): Boolean {
        val tikTokPackage = getTikTokPackage(context) ?: return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(tikTokPackage)
        return if (launchIntent != null) {
            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    /**
     * Whee'yi başlat
     */
    fun launchWhee(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(WHEE_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }
}
