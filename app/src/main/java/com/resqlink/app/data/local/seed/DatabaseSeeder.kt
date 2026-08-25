package com.resqlink.app.data.local.seed

import com.resqlink.app.data.local.dao.FirstAidDao
import com.resqlink.app.data.local.dao.HazardDao
import com.resqlink.app.data.local.dao.UserDao
import com.resqlink.app.data.local.entity.FirstAidEntity
import com.resqlink.app.data.local.entity.HazardEntity
import com.resqlink.app.data.local.entity.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Populates Room with realistic sample data so every screen is demoable
 * before a real backend exists. Debug-only by design (see the call site in
 * ResQLinkApp) -- this must never run in a release build, since it would
 * silently overwrite real cached data with fixtures.
 *
 * Idempotent: every entity uses a fixed, hardcoded ID and Room's upsert is
 * REPLACE-on-conflict, so calling [seedSampleData] on every app launch is safe
 * and won't duplicate rows.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val firstAidDao: FirstAidDao,
    private val hazardDao: HazardDao,
    private val userDao: UserDao
) {
    suspend fun seedSampleData() {
        seedUser()
        seedFirstAidGuides()
        seedHazards()
    }

    private suspend fun seedUser() {
        userDao.upsert(
            UserEntity(
                id = "local_user",
                name = "Aarav Sharma",
                phone = "+91 98765 43210",
                role = "CITIZEN",
                bloodGroup = "O+",
                emergencyContact = "+91 91234 56789",
                medicalNotes = "No known allergies."
            )
        )
    }

    private suspend fun seedFirstAidGuides() {
        firstAidDao.upsertAll(
            listOf(
                FirstAidEntity(
                    id = "cpr",
                    title = "CPR (Cardiopulmonary Resuscitation)",
                    category = "Cardiac",
                    summary = "For someone unresponsive and not breathing normally.",
                    steps = listOf(
                        "Check the scene is safe, then check for a response -- tap shoulders and shout.",
                        "Call for emergency help (or send someone) before starting compressions.",
                        "Place the heel of one hand on the center of the chest, other hand on top, fingers interlaced.",
                        "Push hard and fast: at least 5 cm deep, 100-120 compressions per minute.",
                        "Allow full chest recoil between compressions; minimize interruptions.",
                        "If trained, give 2 rescue breaths after every 30 compressions.",
                        "Continue until emergency responders arrive or the person shows signs of life."
                    ),
                    criticalWarning = "Do not stop compressions for more than 10 seconds at a time.",
                    imageUrl = null
                ),
                FirstAidEntity(
                    id = "severe-bleeding",
                    title = "Severe Bleeding Control",
                    category = "Trauma",
                    summary = "Stopping heavy bleeding from a wound.",
                    steps = listOf(
                        "Apply firm, direct pressure to the wound with a clean cloth or bandage.",
                        "Do not remove the cloth if it soaks through -- add more layers on top.",
                        "If on a limb and bleeding doesn't stop, apply a tourniquet 5-7cm above the wound.",
                        "Note the time the tourniquet was applied; do not loosen it once placed.",
                        "Keep the person warm and lying down if possible; treat for shock.",
                        "Get emergency medical help as soon as possible."
                    ),
                    criticalWarning = "Do not apply a tourniquet over a joint.",
                    imageUrl = null
                ),
                FirstAidEntity(
                    id = "burns",
                    title = "Burns (Thermal)",
                    category = "Trauma",
                    summary = "First response for minor to moderate burns.",
                    steps = listOf(
                        "Remove the person from the heat source; stop the burning process.",
                        "Cool the burn under cool (not ice-cold) running water for 20 minutes.",
                        "Remove tight clothing/jewelry near the burn before swelling starts.",
                        "Cover loosely with a clean, non-fluffy cloth or cling film.",
                        "Do not apply ice, butter, or ointments to the burn.",
                        "Seek medical help for burns larger than a palm, on the face/hands/genitals, or if the skin is charred or leathery."
                    ),
                    criticalWarning = "Never use ice directly on a burn -- it can worsen tissue damage.",
                    imageUrl = null
                ),
                FirstAidEntity(
                    id = "choking",
                    title = "Choking (Conscious Adult)",
                    category = "Airway",
                    summary = "When someone can't cough, speak, or breathe.",
                    steps = listOf(
                        "Ask \"Are you choking?\" -- confirm they can't speak or cough.",
                        "Give up to 5 sharp back blows between the shoulder blades with the heel of your hand.",
                        "If that doesn't clear it, give up to 5 abdominal thrusts (Heimlich maneuver).",
                        "Alternate 5 back blows and 5 abdominal thrusts until the object is expelled.",
                        "If the person becomes unresponsive, lower them to the ground and begin CPR."
                    ),
                    criticalWarning = "Do not perform abdominal thrusts on infants under 1 year -- use back blows and chest thrusts instead.",
                    imageUrl = null
                ),
                FirstAidEntity(
                    id = "fractures",
                    title = "Suspected Fracture",
                    category = "Trauma",
                    summary = "Stabilizing a suspected broken bone before help arrives.",
                    steps = listOf(
                        "Keep the injured area still -- do not try to realign or straighten it.",
                        "Support the area above and below the injury with padding or a splint.",
                        "Apply a cold pack wrapped in cloth to reduce swelling, 15-20 minutes at a time.",
                        "Check circulation beyond the injury (color, warmth, pulse) periodically.",
                        "Do not give food or water in case surgery is needed.",
                        "Get medical help, especially if the bone is visibly deformed or has broken skin."
                    ),
                    criticalWarning = "If the bone has pierced the skin, do not push it back in -- cover it loosely and seek immediate help.",
                    imageUrl = null
                ),
                FirstAidEntity(
                    id = "shock",
                    title = "Shock",
                    category = "General",
                    summary = "Recognizing and responding to shock after injury or trauma.",
                    steps = listOf(
                        "Look for pale, cold, clammy skin, rapid breathing, or confusion.",
                        "Lay the person down and raise their legs about 30cm, if no spinal/leg injury is suspected.",
                        "Keep them warm with a blanket or coat.",
                        "Do not give food or drink.",
                        "Loosen tight clothing around the neck, chest, and waist.",
                        "Monitor breathing and responsiveness until help arrives."
                    ),
                    criticalWarning = null,
                    imageUrl = null
                )
            )
        )
    }

    /**
     * Sample zones centered loosely around Delhi NCR, matching MapScreen's
     * default camera position. Coordinates are illustrative, not real hazard data.
     */
    private suspend fun seedHazards() {
        val now = System.currentTimeMillis()
        hazardDao.upsertAll(
            listOf(
                HazardEntity(
                    id = "hazard-flood-yamuna",
                    type = "FLOOD",
                    severity = "HIGH",
                    title = "Yamuna riverbank flood warning",
                    description = "Water levels rising above danger mark following heavy upstream rainfall.",
                    latitude = 28.6139,
                    longitude = 77.2410,
                    radiusMeters = 3000.0,
                    issuedAtEpochMillis = now - 3 * 60 * 60 * 1000,
                    source = "Central Water Commission"
                ),
                HazardEntity(
                    id = "hazard-heat-central",
                    type = "OTHER",
                    severity = "MODERATE",
                    title = "Extreme heat advisory",
                    description = "Daytime temperatures expected to exceed 45\u00b0C; limit outdoor activity 12pm-4pm.",
                    latitude = 28.6304,
                    longitude = 77.2177,
                    radiusMeters = 15000.0,
                    issuedAtEpochMillis = now - 6 * 60 * 60 * 1000,
                    source = "India Meteorological Department"
                ),
                HazardEntity(
                    id = "hazard-fire-industrial",
                    type = "FIRE",
                    severity = "EXTREME",
                    title = "Industrial fire, Okhla sector",
                    description = "Active fire at a warehouse facility; avoid the area, heavy smoke reported.",
                    latitude = 28.5355,
                    longitude = 77.2910,
                    radiusMeters = 1500.0,
                    issuedAtEpochMillis = now - 45 * 60 * 1000,
                    source = "Delhi Fire Services"
                ),
                HazardEntity(
                    id = "hazard-landslide-hills",
                    type = "LANDSLIDE",
                    severity = "LOW",
                    title = "Minor landslide risk, northern hills",
                    description = "Loose soil reported after recent rain; monitor local advisories before hiking.",
                    latitude = 28.9,
                    longitude = 77.5,
                    radiusMeters = 5000.0,
                    issuedAtEpochMillis = now - 20 * 60 * 60 * 1000,
                    source = "State Disaster Management Authority"
                )
            )
        )
    }
}
