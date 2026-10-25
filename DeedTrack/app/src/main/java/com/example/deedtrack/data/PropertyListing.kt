package com.example.deedtrack.data

/** A property for sale that borrowers can browse on the Explore screen. */
data class PropertyListing(
    val id: String,
    val title: String,
    val address: String,
    val area: String,
    val county: String,
    val propertyType: String,
    val price: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val floorArea: Int,
    val berRating: String,
    val description: String,
    /** Real-world position used for the map marker. Null when the location is unknown. */
    val latitude: Double?,
    val longitude: Double?,
    /** Optional photo URL; a styled placeholder is shown when missing. */
    val imageUrl: String? = null
) {
    /** True only when the coordinates exist and are valid map coordinates. */
    val hasValidLocation: Boolean
        get() = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    val locationLine: String get() = listOf(area, "Co. $county").filter { it.isNotBlank() }.joinToString(", ")

    /** Pre-fills the mortgage application form with this property's details. */
    fun toApplicationDraft(base: ApplicationDraft = ApplicationDraft()): ApplicationDraft = base.copy(
        propertyAddress = address,
        propertyType = propertyType,
        county = county,
        area = area,
        berRating = berRating,
        bedrooms = bedrooms.toString(),
        bathrooms = bathrooms.toString(),
        floorArea = floorArea.toString(),
        askingPrice = price.toLong().toString()
    )
}

/**
 * Where listings come from. Currently the built-in sample data;
 * later this can be backed by Firestore without changing the screens.
 */
interface PropertyListingsSource {
    fun listings(): List<PropertyListing>
}

/** Sample listings for the prototype (positions are approximate town/area locations). */
object SamplePropertyListings : PropertyListingsSource {
    override fun listings(): List<PropertyListing> = listOf(
        PropertyListing("l1", "Harbour View Cottage", "4 Scilly Walk, Kinsale", "Kinsale", "Cork", "House", 465_000.0, 3, 2, 118, "B3",
            "Bright cottage a short walk from Kinsale harbour, with a south-facing garden and sea glimpses.", 51.7059, -8.5222),
        PropertyListing("l2", "Maryborough Semi-Detached", "21 Maryborough Grove, Douglas", "Douglas", "Cork", "House", 395_000.0, 3, 2, 104, "B2",
            "Family semi-detached home close to schools and Douglas village shopping centre.", 51.8770, -8.4350),
        PropertyListing("l3", "Riverside Apartment", "12 The Weir, Ballincollig", "Ballincollig", "Cork", "Apartment", 255_000.0, 2, 1, 72, "A3",
            "Modern two-bed apartment overlooking the River Lee with allocated parking.", 51.8879, -8.5900),
        PropertyListing("l4", "Rathmines Red-Brick", "8 Leinster Square, Rathmines", "Dublin 6", "Dublin", "Townhouse", 685_000.0, 3, 2, 126, "C1",
            "Period terraced home with high ceilings, minutes from Rathmines village and the Luas.", 53.3225, -6.2650),
        PropertyListing("l5", "Dundrum Family Home", "14 Oakwood Avenue, Dundrum", "Dublin 14", "Dublin", "House", 320_000.0, 3, 2, 104, "B2",
            "Well-kept three-bed home near Dundrum Town Centre, with off-street parking.", 53.2925, -6.2450),
        PropertyListing("l6", "Swords Starter Apartment", "33 Pavilions Court, Swords", "Swords", "Dublin", "Apartment", 285_000.0, 2, 2, 78, "B1",
            "Two-bed apartment close to the Pavilions and Dublin Airport bus routes.", 53.4597, -6.2181),
        PropertyListing("l7", "Salthill Seaside Bungalow", "6 Dr Mannix Road, Salthill", "Salthill", "Galway", "Bungalow", 435_000.0, 3, 1, 112, "C2",
            "Detached bungalow a few minutes from the Salthill promenade and beach.", 53.2610, -9.0730),
        PropertyListing("l8", "Castletroy Duplex", "9 Elm Park, Castletroy", "Castletroy", "Limerick", "Duplex", 275_000.0, 2, 2, 90, "A2",
            "Energy-efficient duplex near the University of Limerick and the National Technology Park.", 52.6700, -8.5450),
        PropertyListing("l9", "Tramore Coastal Home", "2 Sea View Park, Tramore", "Tramore", "Waterford", "House", 345_000.0, 4, 3, 145, "B3",
            "Spacious four-bed home with views over Tramore Bay.", 52.1624, -7.1525),
        PropertyListing("l10", "Killarney Lakeside Bungalow", "17 Lakelands, Killarney", "Killarney", "Kerry", "Bungalow", 375_000.0, 3, 2, 120, "B1",
            "Bungalow on a large site near Killarney National Park.", 52.0599, -9.5044),
        PropertyListing("l11", "Athlone Town Centre Apartment", "5 Custume Place, Athlone", "Athlone", "Westmeath", "Apartment", 210_000.0, 1, 1, 55, "B2",
            "One-bed apartment beside the Shannon, ideal first home.", 53.4239, -7.9407),
        // Location not confirmed yet – shows how listings without coordinates are handled.
        PropertyListing("l12", "New Development – Midleton", "Site 4, Castleredmond, Midleton", "Midleton", "Cork", "House", 360_000.0, 3, 3, 110, "A2",
            "New-build three-bed home. Exact location to be confirmed by the developer.", null, null)
    )
}
