package com.djangofiles.djangofiles.work

import androidx.work.Constraints
import androidx.work.NetworkType

val DAILY_WORKER_CONSTRAINTS: Constraints = Constraints.Builder()
    .setRequiresBatteryNotLow(true)
    .setRequiresCharging(false)
    .setRequiresDeviceIdle(false)
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()
