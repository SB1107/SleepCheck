package kr.co.sbsolutions.newsoomirang.presenter.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.ActivityLoginBinding
import kr.co.sbsolutions.newsoomirang.databinding.ActivityMainBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.login.LoginViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.breathing.BreathingFragment
import kr.co.sbsolutions.newsoomirang.presenter.main.history.HistoryFragment
import kr.co.sbsolutions.newsoomirang.presenter.main.nosering.NoSeringFragment
import kr.co.sbsolutions.newsoomirang.presenter.main.setting.SettingFragment

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        binding.navBottomView.apply {
            setupWithNavController(fragment.navController)
            itemIconTintList = null
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


}