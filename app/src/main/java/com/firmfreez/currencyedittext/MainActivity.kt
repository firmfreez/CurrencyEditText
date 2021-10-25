package com.firmfreez.currencyedittext

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.firmfreez.currencyedittext.databinding.ActivityMainBinding
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        configEditText()
    }

    private fun configEditText() {
        binding.currency.minValue = BigDecimal.valueOf(100.125)
        binding.currency.maxValue = BigDecimal.valueOf(200.019)
        binding.currency.digitsAfterDot = 2
        binding.currency.setOnValueChanged { bigDecimal, state ->
            binding.valueText.text = "Current value: $bigDecimal"
            binding.stateText.text = "Current state: $state"
        }
        binding.currency.setValue(BigDecimal.valueOf(150.5))
    }
}