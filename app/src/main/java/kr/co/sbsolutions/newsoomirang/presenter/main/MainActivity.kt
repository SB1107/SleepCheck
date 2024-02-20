package kr.co.sbsolutions.newsoomirang.presenter.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.databinding.ActivityMainBinding
import kr.co.sbsolutions.newsoomirang.databinding.RowProgressResultBinding
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
    private val resultBinding: RowProgressResultBinding by lazy {
        RowProgressResultBinding.inflate(layoutInflater)
    }

    private val resultDialog by lazy {
        BottomSheetDialog(this@MainActivity).apply {
            setContentView(resultBinding.root)
            (resultBinding.root.parent as View).setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.transparent))
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            behavior.isDraggable = false
            setCanceledOnTouchOutside(false)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Cons.NOTIFICATION_ACTION) {
                getBroadcastData()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        val value = intent?.getIntExtra("data", -1)
        if (value == 1) {
            binding.navBottomView.selectedItemId = R.id.navigation_no_sering
        }
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ContextCompat.registerReceiver(this, receiver, IntentFilter(Cons.NOTIFICATION_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        binding.navBottomView.apply {
            setupWithNavController(fragment.navController)
            itemIconTintList = null
        }
         intent?.getIntExtra("data", -1)?.let {
             if (it == 1) {
                 binding.navBottomView.selectedItemId = R.id.navigation_no_sering
             }
         }

        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.serviceCommend.collectLatest {
                        when (it) {
                            ServiceCommend.START -> startSBService(ActionMessage.StartSBService)
                            ServiceCommend.STOP -> startSBService(ActionMessage.StopSBService)
                            ServiceCommend.CANCEL -> startSBService(ActionMessage.CancelSbService)
                        }
                    }
                }
                launch(Dispatchers.Main) {
                    viewModel.isResultProgressBar.collectLatest {
                        /*binding.actionProgressResult.clProgress.visibility = if (it) View.VISIBLE else View.GONE*/
                        resultDialog.run {
                            if (it) show() else dismiss()
                        }
                        if (it.not()) {
                            binding.navBottomView.selectedItemId = R.id.navigation_history
                        }
                    }
                }
            }
        }
    }

    fun getBroadcastData() {
        viewModel.sendMeasurementResults()

    }

}