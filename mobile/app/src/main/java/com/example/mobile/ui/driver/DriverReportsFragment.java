package com.example.mobile.ui.driver;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mobile.R;
import com.example.mobile.models.DailyReport;
import com.example.mobile.models.ReportResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.NavbarHelper;
import com.example.mobile.utils.SharedPreferencesManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverReportsFragment extends Fragment {

    private EditText etDateFrom, etDateTo;
    private Button btnGenerate;
    private View layoutResults;
    private TextView tvStats;
    private LineChart chartRides, chartEarnings, chartKilometers;

    private String selectedDateFrom = "";
    private String selectedDateTo = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_driver_reports, container, false);
        NavbarHelper.setup(this, root, "Reports");

        initViews(root);
        setupDatePickers();

        btnGenerate.setOnClickListener(v -> generateReport());

        return root;
    }

    private void initViews(View root) {
        etDateFrom = root.findViewById(R.id.et_date_from);
        etDateTo = root.findViewById(R.id.et_date_to);
        btnGenerate = root.findViewById(R.id.btn_generate);
        layoutResults = root.findViewById(R.id.layout_results);
        tvStats = root.findViewById(R.id.tv_stats);
        chartRides = root.findViewById(R.id.chart_rides);
        chartEarnings = root.findViewById(R.id.chart_earnings);
        chartKilometers = root.findViewById(R.id.chart_kilometers);
    }

    private void setupDatePickers() {
        etDateFrom.setOnClickListener(v -> showDatePicker(true));
        etDateTo.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFrom) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            if (isFrom) {
                selectedDateFrom = date;
                etDateFrom.setText(date);
            } else {
                selectedDateTo = date;
                etDateTo.setText(date);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void generateReport() {
        if (TextUtils.isEmpty(selectedDateFrom) || TextUtils.isEmpty(selectedDateTo)) {
            Toast.makeText(getContext(), "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferencesManager prefsManager = new SharedPreferencesManager(getContext());
        String token = "Bearer " + prefsManager.getToken();
        Long userId = prefsManager.getUserId();

        if (userId == -1) {
            Toast.makeText(getContext(), "Error: User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // format to ISO_DATE_TIME
        String fromIso = selectedDateFrom + "T00:00:00";
        String toIso = selectedDateTo + "T23:59:59";

        Call<ReportResponse> call = ClientUtils.reportService.getReportForUser(token, userId, fromIso, toIso);

        call.enqueue(new Callback<ReportResponse>() {
            @Override
            public void onResponse(Call<ReportResponse> call, Response<ReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleReportResponse(response.body());
                } else {
                    Toast.makeText(getContext(), "Failed to fetch report", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ReportResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleReportResponse(ReportResponse report) {
        layoutResults.setVisibility(View.VISIBLE);

        StringBuilder statsText = new StringBuilder();
        statsText.append("Cumulative Rides: ").append(report.getCumulativeRides() != null ? report.getCumulativeRides() : 0).append("\n");
        statsText.append("Cumulative Kilometers: ").append(report.getCumulativeKilometers() != null ? report.getCumulativeKilometers() : 0).append("\n");
        statsText.append("Cumulative Earnings: ").append(report.getCumulativeMoney() != null ? report.getCumulativeMoney() : 0).append("\n\n");
        statsText.append("Average Rides: ").append(report.getAverageRides() != null ? report.getAverageRides() : 0).append("\n");
        statsText.append("Average Kilometers: ").append(report.getAverageKilometers() != null ? report.getAverageKilometers() : 0).append("\n");
        statsText.append("Average Earnings: ").append(report.getAverageMoney() != null ? report.getAverageMoney() : 0).append("\n\n");
        
        statsText.append("Pending: ").append(report.getPendingRides())
                .append(" | Active: ").append(report.getActiveRides())
                .append(" | In Progress: ").append(report.getInProgressRides()).append("\n");
        statsText.append("Finished: ").append(report.getFinishedRides())
                .append(" | Rejected: ").append(report.getRejectedRides())
                .append(" | Cancelled: ").append(report.getCancelledRides());

        tvStats.setText(statsText.toString());

        setupChart(chartRides, report.getDailyData(), "RIDES");
        setupChart(chartEarnings, report.getDailyData(), "EARNINGS");
        setupChart(chartKilometers, report.getDailyData(), "KILOMETERS");
    }

    private void setupChart(LineChart chart, List<DailyReport> dailyData, String type) {
        if (dailyData == null || dailyData.isEmpty()) {
            chart.clear();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < dailyData.size(); i++) {
            DailyReport daily = dailyData.get(i);
            labels.add(daily.getDate() != null ? daily.getDate() : "");
            float val = 0f;
            if (type.equals("RIDES") && daily.getRides() != null) {
                val = daily.getRides().floatValue();
            } else if (type.equals("EARNINGS") && daily.getMoney() != null) {
                val = daily.getMoney().floatValue();
            } else if (type.equals("KILOMETERS") && daily.getKilometers() != null) {
                val = daily.getKilometers().floatValue();
            }
            entries.add(new Entry(i, val));
        }

        LineDataSet dataSet = new LineDataSet(entries, type);
        dataSet.setColor(getResources().getColor(R.color.yellow_500));
        dataSet.setValueTextColor(getResources().getColor(R.color.white));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(getResources().getColor(R.color.yellow_500));

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);
        xAxis.setTextColor(getResources().getColor(R.color.white));

        chart.getAxisLeft().setTextColor(getResources().getColor(R.color.white));
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(getResources().getColor(R.color.white));
        
        chart.getDescription().setEnabled(false);
        chart.invalidate(); // refresh
    }
}