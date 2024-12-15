package com.app.taxi_dyali

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Intent(this, HomeActivity::class.java).also { startActivity(it) }
                    true
                }
                R.id.nav_leaderboard -> {
                    Intent(this, HomeActivity::class.java).also { startActivity(it) }
                    true
                }
                R.id.nav_quiz -> {
                    Intent(this, HomeActivity::class.java).also { startActivity(it) }
                    true
                }

                R.id.nav_profile -> {
                    Intent(this, HomeActivity::class.java).also { startActivity(it) }
                    true
                }

                else -> false
            }
        }
    }
}