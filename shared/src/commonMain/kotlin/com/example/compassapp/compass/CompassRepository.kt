package com.example.compassapp.compass

import dev.jordond.compass.Location
import dev.jordond.compass.geocoder.Geocoder
import dev.jordond.compass.geocoder.placeOrNull
import dev.jordond.compass.geolocation.Geolocator // <-- MUST be Geolocator, not Locator
import dev.jordond.compass.geolocation.mobile
import dev.jordond.compass.geolocation.mobile.mobile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CompassRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 1. MUST be Geolocator.mobile()
    private val geolocator = Geolocator.mobile()

    // 2. Initialize the Geocoder
    private val geocoder = Geocoder()

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _placeName = MutableStateFlow<String?>(null)
    val placeName: StateFlow<String?> = _placeName.asStateFlow()

    private val _heading = MutableStateFlow<HeadingData?>(null)
    val heading: StateFlow<HeadingData?> = _heading.asStateFlow()

    private val _headingError = MutableStateFlow<String?>(null)
    val headingError: StateFlow<String?> = _headingError.asStateFlow()

    private var trackingJob: Job? = null
    private var headingProvider: PlatformHeadingProvider? = null

    fun startLocationTracking() {
        // Guard on isActive (not just null) so a previous permission-denied /
        // provider-unavailable failure doesn't permanently block retrying --
        // a failed flow leaves trackingJob non-null but no longer active.
        if (trackingJob?.isActive == true) return

        trackingJob = geolocator.locationUpdates
            .catch { throwable ->
                // Without this, any exception from the flow (permission denied,
                // location services disabled, no provider available, etc.) was
                // silently swallowed and the UI just sat on "--" forever.
                _locationError.value = throwable.message
                    ?: "Couldn't get your location. Make sure location access is allowed and location services are turned on."
            }
            .onEach { location ->
                _location.value = location
                _locationError.value = null
                reverseGeocode(location)
            }
            .launchIn(scope)
    }

    private fun reverseGeocode(location: Location) {
        scope.launch {
            try {
                // 3. Use placeOrNull to get the Place object directly
                val place = geocoder.placeOrNull(location.coordinates)
                _placeName.value = place?.locality ?: "Unknown Location"
            } catch (e: Exception) {
                _placeName.value = null
            }
        }
    }

    fun stopLocationTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _location.value = null
        _locationError.value = null
        _placeName.value = null
    }

    fun startHeadingUpdates() {
        if (headingProvider != null) return
        val provider = PlatformHeadingProvider { degrees, accuracy ->
            _heading.value = HeadingData.fromDegrees(degrees, accuracy)
            _headingError.value = null
        }
        headingProvider = provider
        provider.start { error ->
            _headingError.value = error
            // Sensors unavailable / start failed -- clear the reference so the
            // next Start tap actually retries instead of a no-op.
            headingProvider = null
        }
    }

    fun stopHeadingUpdates() {
        headingProvider?.stop()
        headingProvider = null
        _heading.value = null
        _headingError.value = null
    }

    fun startAll() {
        startLocationTracking()
        startHeadingUpdates()
    }

    fun stopAll() {
        stopLocationTracking()
        stopHeadingUpdates()
    }
}
