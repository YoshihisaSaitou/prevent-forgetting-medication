package com.example.preventforgettingmedicationandroidapp.receiver

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.preventforgettingmedicationandroidapp.BootReceiver
import com.example.preventforgettingmedicationandroidapp.application.usecase.SyncAlarmsUseCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BootReceiverRobolectricTest {

    @Test
    fun `onReceive calls sync usecase when boot completed`() {
        val receiver = BootReceiver()
        val useCase = mockk<SyncAlarmsUseCase>(relaxed = true)
        receiver.syncAlarmsUseCase = useCase

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        verify(exactly = 1) { useCase.invoke() }
    }
}