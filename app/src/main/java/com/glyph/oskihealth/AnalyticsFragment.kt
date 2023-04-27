package com.glyph.oskihealth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.views.chart.ChartView

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
            val trendChartEntryModel = entryModelOf(-5f, -4.6f, -2.3f, 0f, 1.3f, 4.2f, 5f)
            val trendChart: ChartView = findViewById(R.id.chart_view_trend)
            trendChart.setModel(trendChartEntryModel)
            val trendLayout: ConstraintLayout = findViewById(R.id.trend_layout)


            val valueChartEntryModel = entryModelOf(4f, 12f, 8f, 16f)
            val valueChart: ChartView = findViewById(R.id.chart_view_value)
            valueChart.setModel(valueChartEntryModel)
            val valueLayout: ConstraintLayout = findViewById(R.id.value_layout)

            val toggleButton: MaterialButtonToggleGroup = findViewById(R.id.toggleButton)
            toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
                // Respond to button selection
                if (isChecked) {
                    if (checkedId == R.id.trend_button) {
                        trendLayout.visibility = View.VISIBLE
                        valueLayout.visibility = View.GONE
                    } else if (checkedId == R.id.values_button) {
                        trendLayout.visibility = View.GONE
                        valueLayout.visibility = View.VISIBLE
                    }
                }
            }
        }

        return view
    }
}