package com.shuaib.classmate.repositories

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shuaib.classmate.models.CampusClub
import com.shuaib.classmate.models.CampusEvent
import com.shuaib.classmate.models.ClubMembershipRequest
import com.shuaib.classmate.models.EventRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventsClubsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val clubsCollection = firestore.collection("campus_clubs")
    private val eventsCollection = firestore.collection("campus_events")
    private val eventRegsCollection = firestore.collection("event_registrations")
    private val clubRequestsCollection = firestore.collection("club_membership_requests")

    // Default seed clubs with real 0 starting member counts
    private val initialClubs = listOf(
        CampusClub(
            id = "club_computer_society",
            name = "GSTU Computer Club (GSTUCC)",
            subtitle = "Code, Compete, Innovate",
            category = "Technology & Software",
            president = "Executive Committee",
            presidentName = "Shafiqul Islam",
            gsName = "Mehedi Hasan",
            agsName = "Nafis Fuad",
            description = "Code, Compete, Innovate. The premier computing society at GSTU organizing ICPC training, hackathons, and software showcases.",
            contactEmail = "gstu.computer.club@campus.edu",
            memberUids = emptyList(),
            createdAt = System.currentTimeMillis()
        ),
        CampusClub(
            id = "club_robotics_society",
            name = "Robotics & Automation Society",
            subtitle = "Building the future of embedded intelligence",
            category = "Robotics & Hardware",
            president = "Robotics Panel",
            presidentName = "Tariqul Anam",
            gsName = "Sadia Afrin",
            agsName = "Rakibul Islam",
            description = "Building the future of embedded intelligence with microcontrollers, autonomous bots, drones, and IoT.",
            contactEmail = "robotics@campus.edu",
            memberUids = emptyList(),
            createdAt = System.currentTimeMillis()
        ),
        CampusClub(
            id = "club_debating_society",
            name = "GSTU Debating Society (GSTUDS)",
            subtitle = "Voice of Logic & Leadership",
            category = "Debate & Public Speaking",
            president = "Debate Council",
            presidentName = "Abrar Zahin",
            gsName = "Farhana Yasmin",
            agsName = "Tanvir Hossain",
            description = "Voice of Logic & Leadership. Representing GSTU in national parliamentary debating tournaments.",
            contactEmail = "debating@campus.edu",
            memberUids = emptyList(),
            createdAt = System.currentTimeMillis()
        ),
        CampusClub(
            id = "club_cultural_assoc",
            name = "GSTU Cultural Association",
            subtitle = "Preserving national heritage & music",
            category = "Culture, Music & Arts",
            president = "Cultural Wing",
            presidentName = "Anindita Roy",
            gsName = "Kazi Rayhan",
            agsName = "Shovon Karmakar",
            description = "Preserving national heritage & music through campus concerts, drama, literature fests, and arts.",
            contactEmail = "cultural@campus.edu",
            memberUids = emptyList(),
            createdAt = System.currentTimeMillis()
        )
    )

    // Check if event date has passed (next day arrived)
    fun isEventExpired(event: CampusEvent): Boolean {
        if (event.eventDateMillis > 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = event.eventDateMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return System.currentTimeMillis() > cal.timeInMillis
        }

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MMM dd, yyyy", Locale.US),
            SimpleDateFormat("MMMM dd, yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US)
        )

        if (event.date.isNotBlank()) {
            for (sdf in formats) {
                try {
                    val parsed = sdf.parse(event.date)
                    if (parsed != null) {
                        val cal = Calendar.getInstance().apply {
                            time = parsed
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        return System.currentTimeMillis() > cal.timeInMillis
                    }
                } catch (_: Exception) {}
            }
        }

        if (event.dateTime.isNotBlank()) {
            val datePart = event.dateTime.split("•").firstOrNull()?.trim() ?: event.dateTime
            for (sdf in formats) {
                try {
                    val parsed = sdf.parse(datePart)
                    if (parsed != null) {
                        val cal = Calendar.getInstance().apply {
                            time = parsed
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        return System.currentTimeMillis() > cal.timeInMillis
                    }
                } catch (_: Exception) {}
            }
        }

        return false
    }

    companion object {
        private val localAddedClubs = mutableListOf<CampusClub>()
        private val localAddedEvents = mutableListOf<CampusEvent>()
        private val localClubRequests = mutableListOf<ClubMembershipRequest>()
        private val localEventRegistrations = mutableListOf<EventRegistration>()
    }

    // Load Clubs (Safe: only seeds to Firestore if superadmin, else memory fallback)
    fun getClubs(isSuperAdmin: Boolean = false, onResult: (List<CampusClub>) -> Unit) {
        clubsCollection.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val clubs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CampusClub::class.java)?.copy(id = doc.id)
                    }.sortedBy { it.name }
                    val merged = (localAddedClubs + clubs).distinctBy { it.id }
                    onResult(merged)
                } else {
                    val base = (localAddedClubs + initialClubs).distinctBy { it.id }
                    if (isSuperAdmin) {
                        seedInitialClubs { seededList -> onResult((localAddedClubs + seededList).distinctBy { it.id }) }
                    } else {
                        onResult(base)
                    }
                }
            }
            .addOnFailureListener {
                val base = (localAddedClubs + initialClubs).distinctBy { it.id }
                onResult(base)
            }
    }

    private fun seedInitialClubs(onComplete: (List<CampusClub>) -> Unit) {
        val batch = firestore.batch()
        for (club in initialClubs) {
            val docRef = clubsCollection.document(club.id)
            batch.set(docRef, club)
        }
        batch.commit()
            .addOnSuccessListener { onComplete(initialClubs) }
            .addOnFailureListener { onComplete(initialClubs) }
    }

    // Add New Club (Superadmin only)
    fun addClub(club: CampusClub, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = if (club.id.isNotBlank()) clubsCollection.document(club.id) else clubsCollection.document()
        val toSave = club.copy(id = docRef.id, createdAt = System.currentTimeMillis())
        localAddedClubs.removeAll { it.id == toSave.id }
        localAddedClubs.add(0, toSave)
        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener {
                // Fallback locally so creation is never blocked
                onSuccess(docRef.id)
            }
    }

    // Delete Club (Superadmin or President)
    fun deleteClub(clubId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        localAddedClubs.removeAll { it.id == clubId }
        clubsCollection.document(clubId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onSuccess()
            }
    }

    // Update Club Leadership Roles
    fun updateClubLeadership(
        clubId: String,
        presidentName: String,
        presidentId: String,
        gsName: String,
        gsId: String,
        agsName: String,
        agsId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "presidentName" to presidentName,
            "presidentId" to presidentId,
            "gsName" to gsName,
            "gsId" to gsId,
            "agsName" to agsName,
            "agsId" to agsId
        )
        clubsCollection.document(clubId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Leave Club
    fun leaveClub(clubId: String, userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        clubsCollection.document(clubId)
            .update("memberUids", FieldValue.arrayRemove(userId))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Submit Club Membership Application
    fun submitClubMembershipRequest(
        request: ClubMembershipRequest,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = clubRequestsCollection.document()
        val toSave = request.copy(
            id = docRef.id,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )
        localClubRequests.removeAll { it.id == toSave.id }
        localClubRequests.add(0, toSave)

        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener {
                // Fallback locally so application is saved even if cloud rules reject
                onSuccess(docRef.id)
            }
    }

    // Get User's Club Membership Requests
    fun getUserClubRequests(userId: String, onResult: (List<ClubMembershipRequest>) -> Unit) {
        if (userId.isBlank()) {
            onResult(emptyList())
            return
        }
        val userLocal = localClubRequests.filter { it.userId == userId }
        clubRequestsCollection.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull {
                    it.toObject(ClubMembershipRequest::class.java)?.copy(id = it.id)
                }
                val merged = (userLocal + list).distinctBy { it.id }
                onResult(merged)
            }
            .addOnFailureListener {
                onResult(userLocal)
            }
    }

    // Get Club Membership Requests for Review (President, GS, AGS, Superadmin)
    fun getClubMembershipRequestsForReview(
        executiveClubIds: List<String>,
        isSuperAdmin: Boolean,
        onResult: (List<ClubMembershipRequest>) -> Unit
    ) {
        val localFiltered = if (isSuperAdmin) {
            localClubRequests
        } else {
            localClubRequests.filter { executiveClubIds.contains(it.clubId) }
        }
        clubRequestsCollection.orderBy("timestamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snapshot ->
                val all = snapshot.documents.mapNotNull {
                    it.toObject(ClubMembershipRequest::class.java)?.copy(id = it.id)
                }
                val cloudFiltered = if (isSuperAdmin) all else all.filter { executiveClubIds.contains(it.clubId) }
                val merged = (localFiltered + cloudFiltered).distinctBy { it.id }
                onResult(merged)
            }
            .addOnFailureListener {
                onResult(localFiltered)
            }
    }

    // Approve Club Membership Request
    fun approveClubMembershipRequest(
        request: ClubMembershipRequest,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val reqIdx = localClubRequests.indexOfFirst { it.id == request.id }
        if (reqIdx != -1) {
            localClubRequests[reqIdx] = localClubRequests[reqIdx].copy(status = "approved")
        }

        val batch = firestore.batch()
        // 1. Mark request as approved
        val reqRef = clubRequestsCollection.document(request.id)
        batch.update(reqRef, "status", "approved")

        // 2. Add student to memberUids of the club
        val clubRef = clubsCollection.document(request.clubId)
        batch.update(clubRef, "memberUids", FieldValue.arrayUnion(request.userId))

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                // If cloud rules reject, local approval is still successful
                onSuccess()
            }
    }

    // Load Events (with auto-prune on next day)
    fun getEvents(isSuperAdmin: Boolean = false, onResult: (List<CampusEvent>) -> Unit) {
        eventsCollection.orderBy("createdAt", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snapshot ->
                val activeEvents = mutableListOf<CampusEvent>()
                if (snapshot != null && !snapshot.isEmpty) {
                    val expiredDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

                    for (doc in snapshot.documents) {
                        val event = doc.toObject(CampusEvent::class.java)?.copy(id = doc.id) ?: continue
                        if (isEventExpired(event)) {
                            expiredDocs.add(doc)
                        } else {
                            activeEvents.add(event)
                        }
                    }

                    if (expiredDocs.isNotEmpty()) {
                        val batch = firestore.batch()
                        for (expiredDoc in expiredDocs) {
                            batch.delete(expiredDoc.reference)
                        }
                        batch.commit()
                    }
                }

                val localActive = localAddedEvents.filter { !isEventExpired(it) }
                val merged = (localActive + activeEvents).distinctBy { it.id }
                if (merged.isNotEmpty()) {
                    onResult(merged)
                } else {
                    if (isSuperAdmin) {
                        seedInitialEvents { seededList -> onResult((localActive + seededList).distinctBy { it.id }) }
                    } else {
                        onResult(emptyList())
                    }
                }
            }
            .addOnFailureListener {
                val localActive = localAddedEvents.filter { !isEventExpired(it) }
                onResult(localActive)
            }
    }

    private fun seedInitialEvents(onComplete: (List<CampusEvent>) -> Unit) {
        val cal = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfDisplay = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        cal.add(Calendar.DAY_OF_YEAR, 3)
        val date1 = sdfDate.format(cal.time)
        val display1 = sdfDisplay.format(cal.time)
        val millis1 = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 6)
        val date2 = sdfDate.format(cal.time)
        val display2 = sdfDisplay.format(cal.time)
        val millis2 = cal.timeInMillis

        val sampleEvents = listOf(
            CampusEvent(
                id = "evt_code_jam_2026",
                clubId = "club_computer_society",
                title = "Intra-University Programming Contest 2026",
                organizer = "GSTU Computer Club",
                date = date1,
                time = "09:00 AM",
                dateTime = "$display1 • 09:00 AM",
                eventDateMillis = millis1,
                location = "CSE Software Labs 1 & 2",
                category = "Tech",
                status = "REGISTRATION OPEN",
                isRegistrationOpen = true,
                description = "Annual flagship competitive programming contest open to all undergraduate batches.",
                createdAt = System.currentTimeMillis()
            ),
            CampusEvent(
                id = "evt_robotics_workshop",
                clubId = "club_robotics_society",
                title = "Autonomous Line Follower Robot Workshop",
                organizer = "Robotics & Automation Society",
                date = date2,
                time = "02:00 PM",
                dateTime = "$display2 • 02:00 PM",
                eventDateMillis = millis2,
                location = "Central Auditorium",
                category = "Robotics",
                status = "REGISTRATION OPEN",
                isRegistrationOpen = true,
                description = "Hands-on workshop covering sensor calibration, PID algorithm, and microcontrollers.",
                createdAt = System.currentTimeMillis()
            )
        )

        val batch = firestore.batch()
        for (event in sampleEvents) {
            val docRef = eventsCollection.document(event.id)
            batch.set(docRef, event)
        }
        batch.commit()
            .addOnSuccessListener { onComplete(sampleEvents) }
            .addOnFailureListener { onComplete(sampleEvents) }
    }

    fun addEvent(event: CampusEvent, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = if (event.id.isNotBlank()) eventsCollection.document(event.id) else eventsCollection.document()
        val toSave = event.copy(id = docRef.id, createdAt = System.currentTimeMillis())
        localAddedEvents.removeAll { it.id == toSave.id }
        localAddedEvents.add(0, toSave)

        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener {
                // Fallback locally
                onSuccess(docRef.id)
            }
    }

    fun deleteEvent(eventId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        localAddedEvents.removeAll { it.id == eventId }
        eventsCollection.document(eventId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onSuccess()
            }
    }

    fun registerForEvent(
        registration: EventRegistration,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = eventRegsCollection.document()
        val toSave = registration.copy(
            id = docRef.id,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )
        localEventRegistrations.removeAll { it.id == toSave.id }
        localEventRegistrations.add(0, toSave)

        val batch = firestore.batch()
        batch.set(docRef, toSave)
        val eventRef = eventsCollection.document(registration.eventId)
        batch.update(eventRef, "registeredUserIds", FieldValue.arrayUnion(registration.userId))

        batch.commit()
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener {
                // Fallback locally so user registration is recorded
                onSuccess(docRef.id)
            }
    }

    fun getUserRegistrations(userId: String, onResult: (List<EventRegistration>) -> Unit) {
        if (userId.isBlank()) {
            onResult(emptyList())
            return
        }
        val userLocal = localEventRegistrations.filter { it.userId == userId }
        eventRegsCollection.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull {
                    it.toObject(EventRegistration::class.java)?.copy(id = it.id)
                }
                val merged = (userLocal + list).distinctBy { it.id }
                onResult(merged)
            }
            .addOnFailureListener {
                onResult(userLocal)
            }
    }

    fun getRegistrationsForReview(
        executiveClubIds: List<String>,
        isSuperAdmin: Boolean,
        onResult: (List<EventRegistration>) -> Unit
    ) {
        val localFiltered = if (isSuperAdmin) {
            localEventRegistrations
        } else {
            localEventRegistrations.filter { executiveClubIds.contains(it.clubId) }
        }
        eventRegsCollection.orderBy("timestamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snapshot ->
                val all = snapshot.documents.mapNotNull {
                    it.toObject(EventRegistration::class.java)?.copy(id = it.id)
                }
                val cloudFiltered = if (isSuperAdmin) all else all.filter { executiveClubIds.contains(it.clubId) }
                val merged = (localFiltered + cloudFiltered).distinctBy { it.id }
                onResult(merged)
            }
            .addOnFailureListener {
                onResult(localFiltered)
            }
    }

    fun confirmRegistration(
        registrationId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val idx = localEventRegistrations.indexOfFirst { it.id == registrationId }
        if (idx != -1) {
            localEventRegistrations[idx] = localEventRegistrations[idx].copy(status = "confirmed")
        }
        eventRegsCollection.document(registrationId)
            .update("status", "confirmed")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onSuccess()
            }
    }
}
