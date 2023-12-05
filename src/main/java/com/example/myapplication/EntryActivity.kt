package com.example.myapplication

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView


class EntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        val label = intent.getStringExtra("label")
        val tvKey = findViewById<TextView>(R.id.tvKey)
        val etValue = findViewById<EditText>(R.id.etValue)
        val btnDone = findViewById<Button>(R.id.btnDone)
        val btnClear = findViewById<Button>(R.id.btnClear)
        tvKey.text = label

        val mappings = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val mappingsEditor = mappings.edit()
        val lastValue = mappings.getString(label,null)
        if (lastValue != null){
            etValue.setText(lastValue)
        }
        btnDone.setOnClickListener{
            if (etValue.text.toString().isNotBlank()){
                mappingsEditor.apply {
                    putString(label,etValue.text.toString())
                    apply()
                }
            }
            finish()
        }
        btnClear.setOnClickListener{
            mappingsEditor.apply {
                remove(label)
                apply()
                finish()
            }
        }
    }
}