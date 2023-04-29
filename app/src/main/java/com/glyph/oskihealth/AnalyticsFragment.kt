package com.glyph.oskihealth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entriesOf
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.extension.ceil
import com.patrykandpatrick.vico.views.chart.ChartView
import kotlin.math.absoluteValue

/**
 * A simple [Fragment] subclass.
 * Use the [AnalyticsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AnalyticsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)
        with (view) {
            showWeekTrend(this)

            val helpButton: Button = findViewById(R.id.help_button)
            val toggleButton: MaterialButtonToggleGroup = findViewById(R.id.toggleButton)
            val timeToggleButton: MaterialButtonToggleGroup = findViewById(R.id.timeToggleButton)
            toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
                // Respond to button selection
                if (isChecked) {
                    if (checkedId == R.id.trend_button) {
                        if (findViewById<MaterialButton>(R.id.week_button).isChecked) {
                            showWeekTrend(this)
                        } else {
                            showAllTrend(this)
                        }
                    } else if (checkedId == R.id.values_button) {
                        if (findViewById<MaterialButton>(R.id.week_button).isChecked) {
                            showWeekData(this)
                        } else {
                            showAllData(this)
                        }
                    }
                }
            }

            timeToggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    if (checkedId == R.id.week_button) {
                        if (findViewById<MaterialButton>(R.id.trend_button).isChecked) {
                            showWeekTrend(this)
                        } else {
                            showWeekData(this)
                        }
                    } else if (checkedId == R.id.all_button) {
                        if (findViewById<MaterialButton>(R.id.trend_button).isChecked) {
                            showAllTrend(this)
                        } else {
                            showAllData(this)
                        }
                    }
                }
            }

            helpButton.setOnClickListener {
                findNavController().navigate(R.id.action_analyticsFragment_to_stopItGetSomeHelp)
            }
        }

        return view
    }

    private fun showWeekTrend(view: View) {
        with (view) {
            val checkInPrefs = EncryptedSharedPreferences(
                requireContext(), "checkIn", MasterKey(requireContext())
            )
            val sentimentPrefs = EncryptedSharedPreferences(
                requireContext(), "sentimentData", MasterKey(requireContext()))
            var checkInData = checkInPrefs.all.values.map { it as Float }.takeLast(7)

            checkInData = checkInData.mapIndexed { index, fl ->
                (fl + 4*sentimentPrefs.getFloat(checkInPrefs.all.keys.toTypedArray()[index], fl/4)) / 2f
            }

            if (checkInData.size > 1) {
                val average = checkInData.average()
                val maxdiff = checkInData.max() - checkInData.min()
                var fluctuations = 0f
                for (i in 0 until checkInData.size - 1) {
                    fluctuations += checkInData[i + 1] - checkInData[i]
                }
                fluctuations /= checkInData.size - 1

                val feels: TextView = findViewById(R.id.trend_feels)
                var feelsText: String

                if (fluctuations > 1.5) {
                    feelsText = "You've had a lot of fluctuation, but overall your week was "
                    if (average > 3) {
                        feelsText += "very good!"
                        findViewById<Button>(R.id.help_button).visibility = View.GONE
                    } else if (average > 2) {
                        feelsText += "quite good!"
                        findViewById<Button>(R.id.help_button).visibility = View.GONE
                    } else {
                        feelsText += "pretty bad."
                        findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
                    }
                } else if (maxdiff > 2) {
                    feelsText = "You've had your ups and downs, but overall your week was "
                    if (average > 3) {
                        feelsText += "very good!"
                        findViewById<Button>(R.id.help_button).visibility = View.GONE
                    } else if (average > 2) {
                        feelsText += "quite good!"
                        findViewById<Button>(R.id.help_button).visibility = View.GONE
                    } else {
                        feelsText += "pretty bad."
                        findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
                    }
                } else {
                    if (average > 3) {
                        feelsText = "Your week has been very good!"
                        findViewById<Button>(R.id.help_button).visibility = View.GONE
                    } else if (average > 2) {
                        feelsText = "Your week has been quite good!"
                        findViewById<Button>(R.id.help_button).visibility = View.GONE
                    } else {
                        feelsText = "Your week has been pretty bad."
                        findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
                    }
                }

                feels.text = feelsText
            } else {
                findViewById<TextView>(R.id.trend_feels).text = "Not enough data. Please use the app more often!"
                findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
            }


            val trendChart: ChartView = findViewById(R.id.chart_view_trend)
            trendChart.entryProducer = ChartEntryModelProducer(entriesOf(*(checkInData.toTypedArray())), entriesOf(4.5f))
            val trendLayout: ConstraintLayout = findViewById(R.id.trend_layout)

            val valueLayout: ConstraintLayout = findViewById(R.id.value_layout)
            trendLayout.visibility = View.VISIBLE
            valueLayout.visibility = View.GONE
        }
    }

    private fun showAllTrend(view: View) {
        with (view) {
            val checkInPrefs = EncryptedSharedPreferences(
                requireContext(), "checkIn", MasterKey(requireContext())
            )
            val sentimentPrefs = EncryptedSharedPreferences(
                requireContext(), "sentimentData", MasterKey(requireContext()))
            var checkInData = checkInPrefs.all.values.map { it as Float }

            checkInData = checkInData.mapIndexed { index, fl ->
                (fl + 4*sentimentPrefs.getFloat(checkInPrefs.all.keys.toTypedArray()[index], fl/4)) / 2f
            }


            if (checkInData.size > 5) {

                val average = checkInData.average()
                val parts = checkInData.chunked((checkInData.size / 4f).ceil.toInt()) {
                    it.average().toFloat()
                }

                var fluctuations = 0f
                for (i in 0 until parts.size - 1) {
                    fluctuations += parts[i + 1] - parts[i]
                }
                fluctuations /= parts.size - 1

                val feels: TextView = findViewById(R.id.trend_feels)
                var feelsText: String

                if (fluctuations > 1.5) {
                    feelsText = "You've had a rough time overall, "
                    if (parts.last() - parts[parts.size - 1] >= 0.5) {
                        feelsText += "but it's getting better!"
                        findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
                    } else if ((parts.last() - parts[parts.size - 1]).absoluteValue < 0.5) {
                        feelsText += "and its not changing much."
                        findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
                    } else {
                        feelsText += "and its not getting better."
                        findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
                    }
                } else {
                    if (parts.last() - parts[parts.size - 1] >= 0.5) {
                        feelsText = "Your mental health is getting better!"
                        findViewById<Button>(R.id.help_button).visibility = View.GONE
                    } else if ((parts.last() - parts[parts.size - 1]).absoluteValue < 0.5) {
                        if (average >= 3) {
                            feelsText = "Your mental health is still staying strong!"
                            findViewById<Button>(R.id.help_button).visibility = View.GONE
                        } else {
                            feelsText = "Your mental health is still quite bad."
                            findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
                        }
                    } else {
                        feelsText = "Yours mental health has been worsening."
                        findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
                    }
                }

                feels.text = feelsText

            } else {
                findViewById<TextView>(R.id.trend_feels).text = "Not enough data. Please use the app for at least 5 days!"
                findViewById<Button>(R.id.help_button).visibility = View.VISIBLE
            }

            val trendChart: ChartView = findViewById(R.id.chart_view_trend)
            trendChart.entryProducer = ChartEntryModelProducer(entriesOf(*(checkInData.toTypedArray())), entriesOf(4.5f))
            val trendLayout: ConstraintLayout = findViewById(R.id.trend_layout)

            val valueLayout: ConstraintLayout = findViewById(R.id.value_layout)
            trendLayout.visibility = View.VISIBLE
            valueLayout.visibility = View.GONE
        }
    }

    private fun showWeekData(view: View) {
        with (view) {
            val checkInData = EncryptedSharedPreferences(
                requireContext(), "checkIn", MasterKey(requireContext())
            ).all.values.map { it as Float }.takeLast(7).groupingBy { it }.eachCount()

            val valueChartEntryModel = entryModelOf(
                checkInData.getOrDefault(0f, 0),
                checkInData.getOrDefault(1f, 0),
                checkInData.getOrDefault(2f, 0),
                checkInData.getOrDefault(3f, 0),
                checkInData.getOrDefault(4f, 0)
            )
            val valueChart: ChartView = findViewById(R.id.chart_view_value)
            valueChart.setModel(valueChartEntryModel)
            val valueLayout: ConstraintLayout = findViewById(R.id.value_layout)

            val trendLayout: ConstraintLayout = findViewById(R.id.trend_layout)

            trendLayout.visibility = View.GONE
            valueLayout.visibility = View.VISIBLE
        }
    }

    private fun showAllData(view: View) {
        with (view) {
            val checkInData = EncryptedSharedPreferences(
                requireContext(), "checkIn", MasterKey(requireContext())
            ).all.values.map { it as Float }.groupingBy { it }.eachCount()

            val valueChartEntryModel = entryModelOf(
                checkInData.getOrDefault(0f, 0),
                checkInData.getOrDefault(1f, 0),
                checkInData.getOrDefault(2f, 0),
                checkInData.getOrDefault(3f, 0),
                checkInData.getOrDefault(4f, 0)
            )

            val valueChart: ChartView = findViewById(R.id.chart_view_value)
            valueChart.setModel(valueChartEntryModel)
            val valueLayout: ConstraintLayout = findViewById(R.id.value_layout)

            val trendLayout: ConstraintLayout = findViewById(R.id.trend_layout)

            trendLayout.visibility = View.GONE
            valueLayout.visibility = View.VISIBLE
        }
    }
}