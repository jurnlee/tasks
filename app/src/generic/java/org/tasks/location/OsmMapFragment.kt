package org.tasks.location

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.tasks.R
import org.tasks.data.Place
import org.tasks.location.MapFragment.MapFragmentCallback

class OsmMapFragment(private val context: Context) : MapFragment {
    private lateinit var callbacks: MapFragmentCallback
    private lateinit var map: MapView
    private var locationOverlay: MyLocationNewOverlay? = null

    override fun init(activity: AppCompatActivity, callbacks: MapFragmentCallback, dark: Boolean) {
        this.callbacks = callbacks
        Configuration.getInstance()
                .load(activity, PreferenceManager.getDefaultSharedPreferences(activity))
        map = MapView(activity).apply {
            isTilesScaledToDpi = true
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
            if (dark) {
                overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            }
            val copyright = CopyrightOverlay(activity)
            copyright.setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
            overlays.add(copyright)
            activity.findViewById<ViewGroup>(R.id.map).addView(this)
        }
        callbacks.onMapReady(this)
    }

    override fun getMapPosition(): MapPosition {
        val center = map.mapCenter
        return MapPosition(center.latitude, center.longitude, map.zoomLevelDouble.toFloat())
    }

    override fun movePosition(mapPosition: MapPosition, animate: Boolean) {
        val controller = map.controller
        controller.setZoom(mapPosition.zoom.toDouble())
        val geoPoint = GeoPoint(mapPosition.latitude, mapPosition.longitude)
        if (animate) {
            controller.animateTo(geoPoint)
        } else {
            controller.setCenter(geoPoint)
        }
    }

    override fun setMarkers(places: List<Place>) {
        val overlays = map.overlays
        overlays.removeAll(overlays.filterIsInstance<Marker>())
        for (place in places) {
            overlays.add(Marker(map).apply {
                position = GeoPoint(place.latitude, place.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_map_marker_select_red_48dp)!!.mutate()
                setOnMarkerClickListener { _, _ ->
                    callbacks.onPlaceSelected(place)
                    false
                }
            })
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun disableGestures() {
        map.setOnTouchListener { _, _ -> true }
    }

    @SuppressLint("MissingPermission")
    override fun showMyLocation() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map).apply {
            enableMyLocation()
            enableAutoStop = false
        }
        map.overlays.add(locationOverlay)
    }

    override fun onPause() {
        locationOverlay?.disableMyLocation()
        map.onPause()
    }

    override fun onResume() {
        locationOverlay?.enableMyLocation()
        map.onResume()
    }

    override fun onDestroy() = map.onDetach()
}