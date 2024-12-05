package com.example.captive_portal_analyzer_kotlin.repository

import com.example.captive_portal_analyzer_kotlin.dataclasses.CaptivePortalReport
import kotlinx.coroutines.flow.StateFlow


interface IDataRepository {
    var portalReport: CaptivePortalReport?
}

class DataRepository : IDataRepository {
    override var portalReport: CaptivePortalReport? = null
}

