package com.example.compassapp.compass

import dev.jordond.compass.Location
import dev.jordond.compass.geocoder.Geocoder
import dev.jordond.compass.geocoder.placeOrNull
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.mobile
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
    private val geolocator = Geolocator.mobile()
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

    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()
    private val _roll = MutableStateFlow(0f)
    val roll: StateFlow<Float> = _roll.asStateFlow()

    private var trackingJob: Job? = null
    private var trackingControlJob: Job? = null
    private var headingProvider: PlatformHeadingProvider? = null

    fun startLocationTracking() {
        if (trackingJob?.isActive == true) return
        trackingControlJob = geolocator.track().launchIn(scope)
        trackingJob = geolocator.locationUpdates
            .catch { throwable ->
                _locationError.value = throwable.message ?: "Couldn't get your location."
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
                val place = geocoder.placeOrNull(location.coordinates)
                _placeName.value = place?.locality ?: "Unknown Location"
            } catch (e: Exception) {
                _placeName.value = null
            }
        }
    }

    fun stopLocationTracking() {
        geolocator.stopTracking()
        trackingControlJob?.cancel()
        trackingControlJob = null
        trackingJob?.cancel()
        trackingJob = null
        _location.value = null
        _locationError.value = null
        _placeName.value = null
    }

    fun refreshLocation() {
        scope.launch {
            try {
                geolocator.current().onSuccess { location ->
                    _location.value = location
                    _locationError.value = null
                    reverseGeocode(location)
                }.onFailed { error ->
                    _locationError.value = error.message
                }
            } catch (e: Exception) {
                _locationError.value = e.message ?: "Couldn't get your location."
            }
        }
    }

    fun startHeadingUpdates() {
        if (headingProvider != null) return
        val provider = PlatformHeadingProvider { degrees, accuracy, strength, pitch, roll ->
            _heading.value = HeadingData.fromDegrees(degrees, accuracy, strength)
            _headingError.value = null
            _pitch.value = pitch
            _roll.value = roll
        }
        headingProvider = provider
        provider.start { error ->
            _headingError.value = error
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