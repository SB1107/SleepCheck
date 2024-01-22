package kr.co.sbsolutions.newsoomirang.presenter.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.ActivityMainBinding
import kr.co.sbsolutions.newsoomirang.presenter.ActionMessage
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel

@AndroidEntryPoint
class MainActivity : BaseServiceActivity() {
    override fun newBackPressed() {
        twiceBackPressed()
    }

    override fun injectViewModel(): BaseViewModel {
        return viewModel
    }

    private val viewModel: MainViewModel by viewModels()
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "ACTION_SEND_DATA") {
//                intent.getStringExtra(FCMPushService.DATA_KEY)?.let {
                    getBroadcastData()
//                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        registerReceiver(receiver, IntentFilter("ACTION_SEND_DATA"))
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        binding.navBottomView.apply {
            setupWithNavController(fragment.navController)
            itemIconTintList = null
        }

        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.serviceCommend.collectLatest {
                    when(it) {
                        ServiceCommend.START -> startSBService(ActionMessage.StartSBService)
                        ServiceCommend.STOP -> startSBService(ActionMessage.StopSBService)
                    }

                }
            }
        }


//        binding.navBottomView.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.navigation_breathing -> {
//                    supportFragmentManager.beginTransaction().replace(R.id.navigation_breathing, BreathingFragment.newInstance())
//                    true
//                }
//
//                R.id.navigation_no_sering -> {
//                    supportFragmentManager.beginTransaction().replace(R.id.navigation_no_sering, NoSeringFragment.newInstance())
//                    true
//                }
//
//                R.id.navigation_history -> {
//                    supportFragmentManager.beginTransaction().replace(R.id.navigation_history, HistoryFragment.newInstance())
//                    true
//                }
//
//                R.id.navigation_settings -> {
//                    supportFragmentManager.beginTransaction().replace(R.id.navigation_settings, SettingFragment.newInstance())
//                    true
//                }
//
//                else -> false
//            }
//        }
    }
    fun getBroadcastData() {
            viewModel.sendMeasurementResults()
//            viewModel.addLastDataID(data.toInt())
        // viewModel.getDataIdResult(data.toInt())
//        Log.d(TAG, "[MAIN] getBroadcastData() : $data")
    }

}