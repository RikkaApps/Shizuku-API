package com.thedjchi.shizukux.exampleapp.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.thedjchi.shizukux.exampleapp.databinding.MainActivityBinding
import com.thedjchi.shizukux.exampleapp.models.MainUiState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.uiState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { render(it) }
            .launchIn(lifecycleScope)

        with (binding) {
            button1.setOnClickListener {
                viewModel.onBinderWrapperClicked()
            }

            button2.setOnClickListener {
                viewModel.onUserServiceClicked()
            }
        }
    }

    private fun render(state: MainUiState) {
        with (binding) {
            serialTitle.text = state.serialTitleText
            serialNumber.text = state.serialNumber
            shizukuStatus.text = state.shizukuStatusText
            permissionStatus.text = state.permissionStatusText
            userServiceStatus.text = state.userServiceStatusText
        }
    }

}
