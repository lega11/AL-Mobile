package com.example.deedtrack.ui.screens

import com.example.deedtrack.ui.theme.SurfaceColor
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.cos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.deedtrack.data.PropertyListing
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.euro
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.ErrorRed
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.Navy
import com.example.deedtrack.ui.theme.NavyLight
import com.example.deedtrack.ui.theme.SoftFill
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PROPERTY_FILTERS = listOf("All", "House", "Apartment", "Bungalow", "Townhouse", "Duplex")
private const val FOCUS_ZOOM = 14f
private const val ANIMATION_MS = 800
private const val PLACEHOLDER_KEY = "YOUR_MAPS_API_KEY"

/** Ireland, used before the map has fitted the listings. */
private val IRELAND = CameraPosition.fromLatLngZoom(LatLng(53.35, -7.9), 6.2f)

private fun PropertyListing.latLng(): LatLng? = if (hasValidLocation) LatLng(latitude!!, longitude!!) else null

/**
 * Explore properties: listings on the left (or in the List tab on phones) and an interactive
 * Google Map on the right (or in the Map tab), kept in sync in both directions.
 */
@Composable
fun ExploreScreen(
    listings: List<PropertyListing>,
    onApply: (PropertyListing) -> Unit,
    onTab: (MainTab) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isWide = LocalConfiguration.current.screenWidthDp >= 600

    // ---------- Search & filter state ----------
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("All") }
    var countyFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val counties = remember(listings) { listings.map { it.county }.distinct().sorted() }

    val filtered = remember(listings, query, typeFilter, countyFilter) {
        val q = query.trim()
        listings.filter { l ->
            (q.isEmpty() || l.title.contains(q, true) || l.address.contains(q, true) ||
                l.area.contains(q, true) || l.county.contains(q, true)) &&
                (typeFilter == "All" || l.propertyType == typeFilter) &&
                (countyFilter == null || l.county == countyFilter)
        }
    }

    // ---------- Selection / map state ----------
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showMap by rememberSaveable { mutableStateOf(false) } // phones only
    var mapLoaded by remember { mutableStateOf(false) }
    val cameraState = rememberCameraPositionState { position = IRELAND }
    val markerStates = remember { mutableMapOf<String, MarkerState>() }
    val listState = rememberLazyListState()
    val selected = filtered.firstOrNull { it.id == selectedId }
    val hasMapsKey = remember { mapsKeyConfigured(context) }

    // Fit every listing that has a location into view.
    fun fitAll(animate: Boolean) {
        if (!mapLoaded) return
        val points = filtered.mapNotNull { it.latLng() }
        if (points.isEmpty()) return
        val update = if (points.size == 1) {
            CameraUpdateFactory.newLatLngZoom(points.first(), FOCUS_ZOOM - 2)
        } else {
            val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
            CameraUpdateFactory.newLatLngBounds(bounds, 120)
        }
        if (animate) scope.launch { runCatching { cameraState.animate(update, ANIMATION_MS) } }
        else runCatching { cameraState.move(update) }
    }

    fun focusOn(listing: PropertyListing) {
        val target = listing.latLng() ?: return
        if (mapLoaded) {
            scope.launch {
                runCatching { cameraState.animate(CameraUpdateFactory.newLatLngZoom(target, FOCUS_ZOOM), ANIMATION_MS) }
                markerStates[listing.id]?.showInfoWindow()
            }
        } else {
            // Map not on screen yet (phone list tab): it will open here.
            cameraState.position = CameraPosition.fromLatLngZoom(target, FOCUS_ZOOM)
        }
    }

    // Clicking a listing → highlight it and move the map to it.
    fun onListingClicked(listing: PropertyListing) {
        selectedId = listing.id
        if (!listing.hasValidLocation) {
            Toast.makeText(context, "This listing doesn't have a map location yet.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isWide) showMap = true
        focusOn(listing)
    }

    // Clicking a marker → highlight the matching listing and scroll the list to it.
    fun onMarkerClicked(listing: PropertyListing) {
        selectedId = listing.id
        focusOn(listing)
        val index = filtered.indexOfFirst { it.id == listing.id }
        if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
    }

    fun resetMap() {
        selectedId = null
        markerStates.values.forEach { it.hideInfoWindow() }
        fitAll(animate = true)
        scope.launch { listState.animateScrollToItem(0) }
    }

    // Filters changed → drop a selection that's no longer shown and refit the map.
    LaunchedEffect(filtered.map { it.id }) {
        if (selectedId != null && filtered.none { it.id == selectedId }) selectedId = null
        if (selectedId == null) fitAll(animate = true)
    }

    // ---------- Layout ----------
    Scaffold(
        topBar = { AppTopBar("Explore properties", showBrand = true) },
        bottomBar = { MainBottomBar(MainTab.EXPLORE, onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchAndFilters(
                query = query, onQuery = { query = it },
                typeFilter = typeFilter, onType = { typeFilter = it },
                counties = counties, county = countyFilter, onCounty = { countyFilter = it }
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filtered.size} ${if (filtered.size == 1) "property" else "properties"}",
                    style = MaterialTheme.typography.labelLarge, color = MutedText, modifier = Modifier.weight(1f)
                )
                if (!isWide) ListMapToggle(showMap) { showMap = it }
            }

            val listPane: @Composable (Modifier) -> Unit = { modifier ->
                ListingsList(
                    listings = filtered, selectedId = selectedId, listState = listState,
                    onClick = ::onListingClicked,
                    onDirections = { openDirections(context, it) },
                    onApply = onApply,
                    modifier = modifier
                )
            }
            val mapPane: @Composable (Modifier) -> Unit = { modifier ->
                MapPane(
                    listings = filtered, selected = selected, cameraState = cameraState,
                    markerStates = markerStates, hasMapsKey = hasMapsKey,
                    onMapLoaded = {
                        mapLoaded = true
                        val sel = selected
                        if (sel?.hasValidLocation == true) focusOn(sel) else fitAll(animate = false)
                    },
                    onMapUnloaded = { mapLoaded = false },
                    onMarkerClick = ::onMarkerClicked,
                    onReset = ::resetMap,
                    onCloseCard = { selectedId = null; markerStates.values.forEach { it.hideInfoWindow() } },
                    onShowInList = { l -> showMap = false; onMarkerClicked(l) },
                    onDirections = { openDirections(context, it) },
                    onApply = onApply,
                    showCard = !isWide,
                    modifier = modifier
                )
            }

            if (isWide) {
                // Split screen: listings on the left, map on the right.
                Row(Modifier.fillMaxSize()) {
                    listPane(Modifier.weight(0.45f).fillMaxHeight())
                    mapPane(Modifier.weight(0.55f).fillMaxHeight())
                }
            } else {
                Crossfade(targetState = showMap, label = "listOrMap") { mapVisible ->
                    if (mapVisible) mapPane(Modifier.fillMaxSize()) else listPane(Modifier.fillMaxSize())
                }
            }
        }
    }
}

// ======================= Search & filters =======================

@Composable
private fun SearchAndFilters(
    query: String, onQuery: (String) -> Unit,
    typeFilter: String, onType: (String) -> Unit,
    counties: List<String>, county: String?, onCounty: (String?) -> Unit
) {
    var countyMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(SurfaceColor).padding(top = 12.dp, bottom = 4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search by name, town or county", color = MutedText) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                FilterChip(
                    selected = county != null,
                    onClick = { countyMenu = true },
                    label = { Text(county?.let { "Co. $it" } ?: "Any county") },
                    trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenu(expanded = countyMenu, onDismissRequest = { countyMenu = false }) {
                    DropdownMenuItem(text = { Text("Any county") }, onClick = { onCounty(null); countyMenu = false })
                    counties.forEach { c ->
                        DropdownMenuItem(text = { Text("Co. $c") }, onClick = { onCounty(c); countyMenu = false })
                    }
                }
            }
            PROPERTY_FILTERS.forEach { type ->
                FilterChip(selected = typeFilter == type, onClick = { onType(type) }, label = { Text(type) })
            }
        }
    }
    HorizontalDivider(color = CardBorder)
}

@Composable
private fun ListMapToggle(showMap: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        listOf(false to "List", true to "Map").forEach { (isMap, label) ->
            val active = showMap == isMap
            Row(
                Modifier
                    .background(if (active) Navy else Color.White)
                    .clickable { onChange(isMap) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isMap) Icons.Filled.Map else Icons.AutoMirrored.Filled.List,
                    contentDescription = null, tint = if (active) Color.White else Navy, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(label, color = if (active) Color.White else Navy, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ======================= Listings =======================

@Composable
private fun ListingsList(
    listings: List<PropertyListing>,
    selectedId: String?,
    listState: LazyListState,
    onClick: (PropertyListing) -> Unit,
    onDirections: (PropertyListing) -> Unit,
    onApply: (PropertyListing) -> Unit,
    modifier: Modifier
) {
    if (listings.isEmpty()) {
        Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No properties match your search. Try clearing the filters.", color = MutedText)
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(listings, key = { _, l -> l.id }) { _, listing ->
            ListingCard(
                listing = listing,
                selected = listing.id == selectedId,
                onClick = { onClick(listing) },
                onDirections = { onDirections(listing) },
                onApply = { onApply(listing) }
            )
        }
        item {
            Text(
                "Sample listings for the prototype.",
                style = MaterialTheme.typography.labelSmall, color = MutedText,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ListingCard(
    listing: PropertyListing,
    selected: Boolean,
    onClick: () -> Unit,
    onDirections: () -> Unit,
    onApply: () -> Unit
) {
    val borderColor by animateColorAsState(if (selected) Navy else CardBorder, label = "border")
    val background by animateColorAsState(if (selected) NavyLight else SurfaceColor, label = "bg")
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
    ) {
        ListingImage(listing, Modifier.fillMaxWidth().height(130.dp))
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(listing.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listing.locationLine, style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                Text(euro(listing.price), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FactPill(listing.propertyType)
                FactPill("${listing.bedrooms} bed")
                FactPill("${listing.bathrooms} bath")
                FactPill("${listing.floorArea} m²")
                FactPill("BER ${listing.berRating}")
            }
            Spacer(Modifier.height(8.dp))
            Text(listing.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!listing.hasValidLocation) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Location unavailable – not shown on the map", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDirections,
                    enabled = listing.hasValidLocation,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (listing.hasValidLocation) Navy else CardBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Directions")
                }
                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("Apply for mortgage") }
            }
        }
    }
}

@Composable
private fun FactPill(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.background(SoftFill, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

/** Photo if the listing has one, otherwise a branded placeholder. */
@Composable
private fun ListingImage(listing: PropertyListing, modifier: Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(Navy, Color(0xFF3B5998)))), contentAlignment = Alignment.Center) {
        if (listing.imageUrl != null) {
            AsyncImage(
                model = listing.imageUrl, contentDescription = listing.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(44.dp))
        }
        Text(
            listing.propertyType,
            color = Navy, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                .background(Color.White, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ======================= Map =======================

@Composable
private fun MapPane(
    listings: List<PropertyListing>,
    selected: PropertyListing?,
    cameraState: CameraPositionState,
    markerStates: MutableMap<String, MarkerState>,
    hasMapsKey: Boolean,
    onMapLoaded: () -> Unit,
    onMapUnloaded: () -> Unit,
    onMarkerClick: (PropertyListing) -> Unit,
    onReset: () -> Unit,
    onCloseCard: () -> Unit,
    onShowInList: (PropertyListing) -> Unit,
    onDirections: (PropertyListing) -> Unit,
    onApply: (PropertyListing) -> Unit,
    showCard: Boolean,
    modifier: Modifier
) {
    DisposableEffect(Unit) { onDispose { onMapUnloaded() } }
    Box(modifier) {
        if (!hasMapsKey) {
            // No Google Maps API key yet: show the demo map so the feature can still be demonstrated.
            DemoMap(
                listings = listings,
                selected = selected,
                onMarkerClick = onMarkerClick,
                onBackgroundClick = onCloseCard,
                modifier = Modifier.fillMaxSize()
            )
        } else GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false),
            onMapLoaded = onMapLoaded,
            onMapClick = { onCloseCard() }
        ) {
            listings.forEach { listing ->
                val position = listing.latLng() ?: return@forEach // listings without a location get no marker
                val isSelected = listing.id == selected?.id
                val state = markerStates.getOrPut(listing.id) { MarkerState(position = position) }
                MarkerComposable(
                    listing.id, isSelected,
                    state = state,
                    title = listing.title,
                    snippet = "${euro(listing.price)} · ${listing.bedrooms} bed · ${listing.area}",
                    zIndex = if (isSelected) 1f else 0f,
                    onClick = { onMarkerClick(listing); true }
                ) {
                    PriceMarker(listing.price, isSelected)
                }
            }
        }

        // Reset map button
        FilledTonalButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White, contentColor = Navy),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Reset map")
        }

        if (!hasMapsKey) {
            Row(
                Modifier.align(Alignment.TopStart).padding(12.dp).widthIn(max = 230.dp)
                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(10.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DEMO", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Navy, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Demo map. Live Google Maps appears once an API key is added.",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF374151)
                )
            }
        }

        // Airbnb-style card for the selected marker (phone map view)
        AnimatedVisibility(
            visible = showCard && selected != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selected?.let { listing ->
                SelectedListingCard(
                    listing = listing,
                    onClose = onCloseCard,
                    onShowInList = { onShowInList(listing) },
                    onDirections = { onDirections(listing) },
                    onApply = { onApply(listing) }
                )
            }
        }
    }
}

/** Custom marker: price pill in the app's navy colour scheme. */
@Composable
private fun PriceMarker(price: Double, selected: Boolean) {
    val label = "€" + (price / 1000).roundToInt() + "k"
    Text(
        label,
        color = if (selected) Color.White else Navy,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(4.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(if (selected) Navy else Color.White, RoundedCornerShape(16.dp))
            .border(1.5.dp, Navy, RoundedCornerShape(16.dp))
            .padding(horizontal = if (selected) 12.dp else 10.dp, vertical = if (selected) 6.dp else 4.dp)
    )
}

@Composable
private fun SelectedListingCard(
    listing: PropertyListing,
    onClose: () -> Unit,
    onShowInList: () -> Unit,
    onDirections: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ListingImage(listing, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).clickable(onClick = onShowInList)) {
                Text(listing.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listing.locationLine, style = MaterialTheme.typography.bodySmall, color = MutedText)
                Text(
                    "${euro(listing.price)} · ${listing.bedrooms} bed · ${listing.bathrooms} bath",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Navy
                )
            }
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
        }
        Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDirections, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Directions")
            }
            Button(onClick = onApply, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) { Text("Apply") }
        }
    }
}

// ======================= Helpers =======================

/** Opens turn-by-turn directions to the listing in Google Maps (app or browser). */
private fun openDirections(context: Context, listing: PropertyListing) {
    if (!listing.hasValidLocation) {
        Toast.makeText(context, "This listing doesn't have a map location yet.", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${listing.latitude},${listing.longitude}")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to open directions.", Toast.LENGTH_SHORT).show()
    }
}

/** True when a real Maps API key (not the placeholder) is configured in the manifest. */
private fun mapsKeyConfigured(context: Context): Boolean = runCatching {
    @Suppress("DEPRECATION")
    val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    val key = info.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
    key.isNotBlank() && key != PLACEHOLDER_KEY
}.getOrDefault(false)

// ======================= Demo map (no API key) =======================

/** Rough outline of the island of Ireland (latitude, longitude), used to draw the demo map. */
private val IRELAND_OUTLINE = listOf(
    55.38 to -7.37, 55.25 to -6.45, 55.20 to -6.05, 54.85 to -5.75, 54.65 to -5.55, 54.25 to -5.60,
    53.95 to -6.10, 53.60 to -6.10, 53.35 to -6.10, 53.00 to -6.00, 52.60 to -6.20, 52.20 to -6.35,
    52.15 to -6.90, 52.05 to -7.60, 51.85 to -8.25, 51.60 to -8.90, 51.45 to -9.80, 51.75 to -10.20,
    52.10 to -10.45, 52.60 to -9.65, 52.95 to -9.45, 53.25 to -9.05, 53.40 to -10.10, 53.85 to -9.95,
    54.25 to -10.05, 54.30 to -9.20, 54.25 to -8.60, 54.65 to -8.65, 55.10 to -8.30, 55.25 to -7.60
)

private val DEMO_CITIES = listOf(
    "Dublin" to (53.35 to -6.26), "Cork" to (51.90 to -8.47), "Galway" to (53.27 to -9.05),
    "Limerick" to (52.66 to -8.63), "Waterford" to (52.26 to -7.11), "Belfast" to (54.60 to -5.93)
)

/**
 * Stand-in for Google Maps while no API key is configured. Draws Ireland and places the same
 * price markers at each listing's real coordinates, so the list ⇄ map interaction still works.
 */
@Composable
private fun DemoMap(
    listings: List<PropertyListing>,
    selected: PropertyListing?,
    onMarkerClick: (PropertyListing) -> Unit,
    onBackgroundClick: () -> Unit,
    modifier: Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier.background(Color(0xFFCFE3F3)).clickable(onClick = onBackgroundClick)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Simple equirectangular projection fitted to the box (longitude squeezed by cos(latitude)).
        val minLat = 51.3; val maxLat = 55.5; val minLon = -10.7; val maxLon = -5.3
        val lonFactor = cos(Math.toRadians(53.4))
        val mapW = (maxLon - minLon) * lonFactor
        val mapH = maxLat - minLat
        val padding = with(density) { 24.dp.toPx() }
        val scale = minOf((widthPx - 2 * padding) / mapW, (heightPx - 2 * padding) / mapH)
        val offsetX = (widthPx - mapW * scale) / 2
        val offsetY = (heightPx - mapH * scale) / 2
        fun project(lat: Double, lon: Double) = Offset(
            (offsetX + (lon - minLon) * lonFactor * scale).toFloat(),
            (offsetY + (maxLat - lat) * scale).toFloat()
        )

        Canvas(Modifier.fillMaxSize()) {
            val outline = Path().apply {
                IRELAND_OUTLINE.forEachIndexed { i, (lat, lon) ->
                    val p = project(lat, lon)
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(outline, Color(0xFFEAF1E4))
            drawPath(outline, Color(0xFFB7C9A8), style = Stroke(width = 2.dp.toPx()))
            DEMO_CITIES.forEach { (_, ll) ->
                drawCircle(Color(0xFF9AA5B1), radius = 3.dp.toPx(), center = project(ll.first, ll.second))
            }
        }

        // City labels
        DEMO_CITIES.forEach { (name, ll) ->
            val p = project(ll.first, ll.second)
            Text(
                name, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280),
                modifier = Modifier.offset { IntOffset(p.x.roundToInt() + 6, p.y.roundToInt() - 8) }
            )
        }

        // Price markers (selected one drawn last so it sits on top)
        listings.filter { it.hasValidLocation }
            .sortedBy { it.id == selected?.id }
            .forEach { listing ->
                val p = project(listing.latitude!!, listing.longitude!!)
                val isSelected = listing.id == selected?.id
                Box(
                    Modifier
                        .offset { IntOffset(p.x.roundToInt(), p.y.roundToInt()) }
                        .layout { measurable, constraints ->
                            // Centre the marker on its point
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.place(-placeable.width / 2, -placeable.height / 2)
                            }
                        }
                        .clickable { onMarkerClick(listing) }
                ) {
                    PriceMarker(listing.price, isSelected)
                }
            }
    }
}
