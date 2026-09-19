package com.example.medalarm.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medalarm.databinding.ActivityMainBinding
import com.example.medalarm.receiver.AlarmReceiver
import com.example.medalarm.worker.FamilyAlertWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener {
            val name = binding.etMedicineName.text.toString()
            val dosage = binding.etDosage.toString()
            val instructions = binding.etInstructions.text.toString()

            if (name.isNotEmpty()) {
                val alarmTimeMillis = System.currentTimeMillis() + 10000 // Test alarm in 10 secs
                scheduleMedicineAlarm(101L, name, dosage, alarmTimeMillis)
                scheduleFamilyMissedAlert(101L, name, "+1234567890")

                Toast.makeText(this, "Medicine Alarm Scheduled!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleMedicineAlarm(medId: Long, name: String, dosage: String, timeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("MED_ID", medId)
            putExtra("MED_NAME", name)
            putExtra("DOSAGE", dosage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, medId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }

    private fun scheduleFamilyMissedAlert(medId: Long, name: String, familyPhone: String) {
        val inputData = Data.Builder()
            .putString("MED_NAME", name)
            .putString("FAMILY_PHONE", familyPhone)
            .putBoolean("IS_TAKEN", false)
            .build()

        // Check 30 minutes after scheduled alarm time
        val missedWorkRequest = OneTimeWorkRequestBuilder<FamilyAlertWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueue(missedWorkRequest)
    }
}
