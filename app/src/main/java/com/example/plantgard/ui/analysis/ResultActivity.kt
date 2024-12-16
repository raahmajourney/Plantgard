package com.example.plantgard.ui.analysis

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.plantgard.R

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val plantType = intent.getStringExtra("PLANT_TYPE") ?: "Tipe tanaman tidak tersedia"
        val diseaseType = intent.getStringExtra("DISEASE_TYPE") ?: "Jenis penyakit tidak tersedia"
        val diseaseDescription = intent.getStringExtra("DISEASE_DESCRIPTION") ?: "Deskripsi tidak tersedia"
        val diseaseTreatment = intent.getStringExtra("DISEASE_TREATMENT") ?: "Penanganan tidak tersedia"
        intent.getStringExtra("DISEASE_PREVENTION") ?: "Pencegahan tidak tersedia"

        val titleTextView: TextView = findViewById(R.id.TitleTextView)
        val plantTypeTextView: TextView = findViewById(R.id.tipetanaman)
        val descriptionTextView: TextView = findViewById(R.id.deskripsi)
        val symptomsTextView: TextView = findViewById(R.id.gejala)
        val treatmentTextView: TextView = findViewById(R.id.penanganan)

        titleTextView.text = getString(R.string.hasil_pindai)
        plantTypeTextView.text = HtmlCompat.fromHtml(
            "<b>Tipe Tanaman:</b> $plantType", HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        descriptionTextView.text = HtmlCompat.fromHtml(
            "<b>Deskripsi:</b> $diseaseDescription", HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        symptomsTextView.text = HtmlCompat.fromHtml(
            "<b>Gejala:</b> $diseaseType", HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        treatmentTextView.text = HtmlCompat.fromHtml(
            "<b>Saran Penanganan:</b> $diseaseTreatment", HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}
