package com.mariam.live.data

import com.mariam.live.model.Report

class ReportRepository {

    private val reports = mutableListOf<Report>()

    fun submitReport(report: Report) {
        reports.add(report)
    }

    fun getReports(): List<Report> = reports

    fun getPendingReports(): List<Report> {
        return reports.filter { it.status == "pending" }
    }
}
