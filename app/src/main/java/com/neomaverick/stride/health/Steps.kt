package com.neomaverick.stride.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.neomaverick.stride.data.StepDay
import java.time.LocalDate
import java.time.Period

/** Reads daily step totals from Health Connect (Samsung Health, Google Fit and others write there). */
class HealthSteps(private val context: Context) {
    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    fun available(): Boolean = runCatching { HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE }.getOrDefault(false)

    suspend fun granted(): Boolean = available() &&
        HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(permissions)

    /** Last [days] days of steps, one row per day. */
    suspend fun read(days: Int = 28): List<StepDay> {
        val client = HealthConnectClient.getOrCreate(context)
        val end = LocalDate.now().plusDays(1).atStartOfDay()
        val start = end.minusDays(days.toLong())
        val result = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
                timeRangeSlicer = Period.ofDays(1),
            )
        )
        return result.map { StepDay(it.startTime.toLocalDate().toEpochDay(), it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
    }
}
