package com.example.preventforgettingmedicationandroidapp.domain.model

@JvmInline
value class ScheduleId(val value: Int) {
    init {
        require(value > 0) { "ScheduleId must be positive" }
    }
}

@JvmInline
value class MedicationId(val value: Int) {
    init {
        require(value > 0) { "MedicationId must be positive" }
    }
}

@JvmInline
value class TakenAt(val value: Long) {
    companion object {
        fun now(): TakenAt = TakenAt(System.currentTimeMillis())
    }
}
