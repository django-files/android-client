package com.djangofiles.djangofiles

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlin.coroutines.resume

// Android 17 (API 37) blocks local network access by default for apps targeting API 37+.
// https://developer.android.com/privacy-and-security/local-network-permission

private const val LOCAL_NETWORK_REQUEST_CODE = 10001

// Stable key so a dialog pending across an activity recreation still delivers
// its result to the re-registered launcher.
private const val LOCAL_NETWORK_REGISTRY_KEY = "LocalNetworkPermission"

fun Context.hasLocalNetworkPermission(): Boolean {
    // The permission only exists on Android 17+; older versions are unaffected.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return true
    return ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_LOCAL_NETWORK
    ) == PackageManager.PERMISSION_GRANTED
}

fun Activity.requestLocalNetworkPermission() {
    if (hasLocalNetworkPermission()) return
    Log.i("LocalNetwork", "Requesting ACCESS_LOCAL_NETWORK")
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK),
        LOCAL_NETWORK_REQUEST_CODE
    )
}

// Requests ACCESS_LOCAL_NETWORK and suspends until the user answers the dialog.
// Returns true when access is granted (or already held / unnecessary), false on denial.
// MUST be called before the first request to a local server: on Android 17 every
// outbound LAN connection fails while the permission is not granted.
//
// Denial handling (shouldShowRequestPermissionRationale is false both before the
// first ask AND after a permanent denial, so the states are told apart by when
// it is checked):
// - Soft denial: an explanation dialog is shown before re-prompting.
// - Permanent denial ("don't ask again" or two denials): the system dialog can
//   never appear again, so the user is offered a shortcut to the app settings.
suspend fun ComponentActivity.ensureLocalNetworkPermission(): Boolean {
    if (hasLocalNetworkPermission()) return true

    // Only reachable after a previous soft denial: explain before asking again.
    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_LOCAL_NETWORK)) {
        Log.i("LocalNetwork", "Showing ACCESS_LOCAL_NETWORK rationale")
        val proceed = suspendCancellableCoroutine { cont ->
            MaterialAlertDialogBuilder(this)
                .setTitle("Local Network Access")
                .setMessage(
                    "This server is on your local network. Android requires your " +
                        "permission for apps to communicate with devices on the local " +
                        "network. Without it, connecting to this server will fail."
                )
                .setNegativeButton("Not Now") { dialog, _ ->
                    dialog.dismiss()
                    if (cont.isActive) cont.resume(false)
                }
                .setPositiveButton("Continue") { dialog, _ ->
                    dialog.dismiss()
                    if (cont.isActive) cont.resume(true)
                }
                .setOnCancelListener {
                    if (cont.isActive) cont.resume(false)
                }
                .show()
        }
        if (!proceed) return false
    }

    Log.i("LocalNetwork", "Requesting ACCESS_LOCAL_NETWORK")
    val granted = suspendCancellableCoroutine { cont ->
        val launcher = activityResultRegistry.register(
            LOCAL_NETWORK_REGISTRY_KEY,
            ActivityResultContracts.RequestPermission()
        ) { result ->
            Log.i("LocalNetwork", "ACCESS_LOCAL_NETWORK granted: $result")
            if (cont.isActive) cont.resume(result)
        }
        cont.invokeOnCancellation { launcher.unregister() }
        launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }
    if (granted) return true

    // The system prompt was just answered with a denial. If the rationale flag
    // is now false this was a permanent denial - the system will never show its
    // dialog again, so point the user at the app settings page instead.
    if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_LOCAL_NETWORK)) {
        Log.w("LocalNetwork", "ACCESS_LOCAL_NETWORK permanently denied - offering Settings")
        suspendCancellableCoroutine { cont ->
            MaterialAlertDialogBuilder(this)
                .setTitle("Local Network Access Blocked")
                .setMessage(
                    "Local network access was denied, so this server cannot be " +
                        "reached. To allow it, open App Info > Permissions > " +
                        "Nearby devices and select Allow."
                )
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    if (cont.isActive) cont.resume(false)
                }
                .setPositiveButton("Open Settings") { dialog, _ ->
                    dialog.dismiss()
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                    )
                    if (cont.isActive) cont.resume(false)
                }
                .setOnCancelListener {
                    if (cont.isActive) cont.resume(false)
                }
                .show()
        }
    }
    return false
}

// True when the URL points at a local address (.local mDNS name or a host that
// resolves to a private/link-local/CGNAT range).
// Loopback is excluded: Android 17 exempts same-profile loopback traffic from
// local network protection, so on-device servers need no permission.
suspend fun String.isLocalNetworkUrl(): Boolean {
    val url = toHttpUrlOrNull() ?: return false
    val host = url.host
    Log.d("isLocalNetworkUrl", "host: $host")
    if (host.endsWith(".local", ignoreCase = true)) return true
    return withContext(Dispatchers.IO) {
        try {
            InetAddress.getAllByName(host)
                .filterNot { it.isLoopbackAddress }
                .any { isLocalAddress(it) }
        } catch (e: Exception) {
            Log.e("isLocalNetworkUrl", "DNS resolution failed: $e")
            false
        }
    }
}

private fun isLocalAddress(address: InetAddress): Boolean {
    if (address is Inet4Address) {
        val b = address.address
        val b0 = b[0].toInt() and 0xFF
        val b1 = b[1].toInt() and 0xFF
        return address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress ||                 // 224.0.0.0/4 — was missing
            (b0 == 255 && b1 == 255 &&
                (b[2].toInt() and 0xFF) == 255 &&
                (b[3].toInt() and 0xFF) == 255) ||          // 255.255.255.255 — was missing
            (b0 == 100 && b1 in 64..127)                    // 100.64.0.0/10 CGNAT — was missing
    }
    if (address is Inet6Address) {
        val first = address.address[0].toInt() and 0xFF
        return address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isMulticastAddress ||                  // ff00::/8 — was missing
            (first and 0xFE) == 0xFC                        // fc00::/7 ULA
    }
    return address.isAnyLocalAddress
}
