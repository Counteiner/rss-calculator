package com.rcalc.resourcecalculator.model

data class ScanResult(
    val rows: List<ResourceEntry>,
    val totalFromItems: Double,
    val totalResources: Double
)
