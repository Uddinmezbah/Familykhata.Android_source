package com.familykhata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familykhata.app.ui.FamilyKhataApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DueReminderScheduler.schedule(applicationContext)
        setContent {
            val vm: FamilyKhataViewModel = viewModel()
            FamilyKhataApp(vm)
        }
    }
}
