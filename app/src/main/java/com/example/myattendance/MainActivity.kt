
package com.example.myattendance

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.app.AlertDialog
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("attendance_data", MODE_PRIVATE)
    }

    private lateinit var root: LinearLayout
    private lateinit var calendarBox: LinearLayout
    private lateinit var monthTitle: TextView
    private lateinit var summary: TextView
    private lateinit var salaryInput: EditText
    private lateinit var advanceText: TextView

    private val month = Calendar.getInstance()
    private var monthlyMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen()
    }

    private fun showScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 16)
            setBackgroundColor(Color.WHITE)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "Attendance"
            textSize = 28f
            setTextColor(Color.rgb(22, 101, 52))
        })

        val name = EditText(this).apply {
            hint = "कर्मचारी का नाम"
            setText(prefs.getString("name", ""))
        }
        root.addView(name)

        root.addView(Button(this).apply {
            text = "नाम सेव करें"
            setOnClickListener {
                prefs.edit().putString("name", name.text.toString()).apply()
                Toast.makeText(this@MainActivity, "नाम सेव हुआ", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(TextView(this).apply {
            text = "महीने का कैलेंडर"
            textSize = 21f
            setTextColor(Color.BLACK)
        })

        val nav = LinearLayout(this)
        monthTitle = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
        }

        nav.addView(Button(this).apply {
            text = "◀"
            setOnClickListener {
                month.add(Calendar.MONTH, -1)
                refreshCalendar()
            }
        })

        nav.addView(monthTitle, LinearLayout.LayoutParams(0, -2, 1f))

        nav.addView(Button(this).apply {
            text = "▶"
            setOnClickListener {
                month.add(Calendar.MONTH, 1)
                refreshCalendar()
            }
        })

        root.addView(nav)

        calendarBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(calendarBox)

        root.addView(TextView(this).apply {
            text = "हरा = Present | पीला = Half Day | लाल = Absent"
            textSize = 13f
            setTextColor(Color.DKGRAY)
        })

        root.addView(Button(this).apply {
            text = "वेतन प्रकार बदलें: मासिक / दैनिक"
            setOnClickListener {
                monthlyMode = !monthlyMode
                calculate()
            }
        })

        salaryInput = EditText(this).apply {
            hint = "वेतन रुपये में"
            inputType = 8194
            setText(prefs.getString("salary", ""))
        }
        root.addView(salaryInput)

        root.addView(Button(this).apply {
            text = "वेतन सेव करें"
            setOnClickListener {
                prefs.edit().putString("salary", salaryInput.text.toString()).apply()
                calculate()
            }
        })

        root.addView(TextView(this).apply {
            text = "Advance"
            textSize = 21f
            setTextColor(Color.BLACK)
        })

        advanceText = TextView(this)
        root.addView(advanceText)

        root.addView(Button(this).apply {
            text = "+ एडवांस जोड़ें"
            setOnClickListener { addAdvance() }
        })

        summary = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.BLACK)
        }
        root.addView(summary)

        refreshCalendar()
    }

    private fun key(day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d",
            month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1, day)

    private fun status(day: Int): String =
        prefs.getString("day_" + key(day), "") ?: ""

    private fun refreshCalendar() {
        calendarBox.removeAllViews()
        monthTitle.text = java.text.SimpleDateFormat(
            "MMMM yyyy", Locale.getDefault()
        ).format(month.time)

        val week = listOf("सो", "मं", "बु", "गु", "शु", "श", "रवि")
        addRow(week, true)

        val first = month.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH, 1)
        val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val days = month.getActualMaximum(Calendar.DAY_OF_MONTH)

        var row = mutableListOf<String>()
        repeat(offset) { row.add("") }

        for (d in 1..days) {
            row.add(d.toString())
            if (row.size == 7) {
                addRow(row, false)
                row = mutableListOf()
            }
        }
        if (row.isNotEmpty()) {
            while (row.size < 7) row.add("")
            addRow(row, false)
        }
        calculate()
    }

    private fun addRow(values: List<String>, heading: Boolean) {
        val row = LinearLayout(this)
        for (v in values) {
            val cell = TextView(this).apply {
                text = v
                gravity = Gravity.CENTER
                textSize = 14f
                setPadding(2, 12, 2, 12)

                if (!heading && v.isNotEmpty()) {
                    val d = v.toInt()
                    text = v + when (status(d)) {
                        "P" -> "\n✓"
                        "H" -> "\n½"
                        "A" -> "\n✕"
                        else -> ""
                    }
                    setBackgroundColor(when (status(d)) {
                        "P" -> Color.rgb(220, 252, 231)
                        "H" -> Color.rgb(254, 243, 199)
                        "A" -> Color.rgb(254, 226, 226)
                        else -> Color.rgb(245, 245, 245)
                    })
                    setOnClickListener {
                        val next = when (status(d)) {
                            "" -> "P"
                            "P" -> "H"
                            "H" -> "A"
                            else -> ""
                        }
                        prefs.edit().putString("day_" + key(d), next).apply()
                        refreshCalendar()
                    }
                }
            }
            row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
        }
        calendarBox.addView(row)
    }

    private fun addAdvance() {
        val input = EditText(this).apply {
            hint = "रकम रुपये में"
            inputType = 8194
        }

        AlertDialog.Builder(this)
            .setTitle("एडवांस जोड़ें")
            .setView(input)
            .setNegativeButton("रद्द", null)
            .setPositiveButton("सेव") { _, _ ->
                val amount = input.text.toString().toFloatOrNull()
                if (amount != null && amount > 0) {
                    val k = "advance_" + month.get(Calendar.YEAR) + "_" +
                        (month.get(Calendar.MONTH) + 1)
                    prefs.edit().putFloat(
                        k, prefs.getFloat(k, 0f) + amount
                    ).apply()
                    calculate()
                }
            }
            .show()
    }

    private fun calculate() {
        if (!::summary.isInitialized) return

        var present = 0
        var half = 0
        var absent = 0

        for (d in 1..month.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            when (status(d)) {
                "P" -> present++
                "H" -> half++
                "A" -> absent++
            }
        }

        val wage = salaryInput.text.toString().toDoubleOrNull() ?: 0.0
        val earned = if (monthlyMode) wage else (present + half * 0.5) * wage
        val k = "advance_" + month.get(Calendar.YEAR) + "_" +
            (month.get(Calendar.MONTH) + 1)
        val advance = prefs.getFloat(k, 0f).toDouble()

        advanceText.text = "इस महीने का एडवांस: ₹$advance"
        summary.text = """
            Present: $present दिन
            Half Day: $half दिन
            Absent: $absent दिन

            कुल वेतन: ₹${"%.2f".format(earned)}
            एडवांस: ₹${"%.2f".format(advance)}
            बाकी वेतन: ₹${"%.2f".format(earned - advance)}
        """.trimIndent()
    }
}
