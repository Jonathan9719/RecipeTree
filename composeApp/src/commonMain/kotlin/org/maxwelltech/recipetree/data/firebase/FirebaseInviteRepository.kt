package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.Serializable
import org.maxwelltech.recipetree.data.model.Invite
import org.maxwelltech.recipetree.data.repository.InviteRepository
import kotlin.random.Random

@Serializable
private data class InviteMemberDoc(
    val role: String = ""
)

@OptIn(ExperimentalTime::class)
class FirebaseInviteRepository(
    private val firestore: FirebaseFirestore
) : InviteRepository {

    private val invitesCollection = firestore.collection("invites")
    private val cookbooksCollection = firestore.collection("cookbooks")

    override suspend fun createInvite(
        cookbookId: String,
        createdBy: String,
        maxUses: Int,
        expiresAt: Long?
    ): Invite {
        // Retry on the (astronomically rare) collision. 378B combos; ~8 retries would
        // need multi-billion invites in flight.
        repeat(5) {
            val code = generateCode()
            val docRef = invitesCollection.document(code)
            val existing = docRef.get()
            if (!existing.exists) {
                val invite = Invite(
                    code = code,
                    cookbookId = cookbookId,
                    createdBy = createdBy,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    expiresAt = expiresAt,
                    maxUses = maxUses,
                    usedCount = 0,
                    revoked = false,
                    role = "member"
                )
                docRef.set(invite)
                return invite
            }
        }
        throw IllegalStateException("Failed to generate a unique invite code after 5 tries")
    }

    override suspend fun getInvite(code: String): Invite? {
        val normalized = normalizeCode(code)
        if (normalized.isEmpty()) return null
        val snapshot = invitesCollection.document(normalized).get()
        return if (snapshot.exists) snapshot.data<Invite>() else null
    }

    override suspend fun revokeInvite(code: String) {
        val normalized = normalizeCode(code)
        invitesCollection.document(normalized).delete()
    }

    override fun observeCookbookInvites(cookbookId: String): Flow<List<Invite>> {
        return invitesCollection
            .where { "cookbookId" equalTo cookbookId }
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.data<Invite>() } }
    }

    override suspend fun acceptInvite(code: String, userId: String): String {
        val normalized = normalizeCode(code)
        if (normalized.isEmpty()) throw IllegalArgumentException("Empty invite code")

        // One round-trip to validate; a second set of writes in an atomic batch.
        // Client-side race: two concurrent accepts on a single-use code could both
        // validate with usedCount=0 and both commit — at scale, Firestore security
        // rules will reject the second write (update allowed only when
        // resource.data.usedCount + 1 <= resource.data.maxUses). Until those rules
        // ship this is best-effort; the worst case is one extra member on a
        // single-use code.
        val inviteRef = invitesCollection.document(normalized)
        val snapshot = inviteRef.get()
        if (!snapshot.exists) throw IllegalStateException("Invite not found")
        val invite = snapshot.data<Invite>()

        if (invite.revoked) throw IllegalStateException("Invite was revoked")
        val expiresAt = invite.expiresAt
        if (expiresAt != null && Clock.System.now().toEpochMilliseconds() > expiresAt) {
            throw IllegalStateException("Invite expired")
        }
        if (invite.usedCount >= invite.maxUses) {
            throw IllegalStateException("Invite is fully used")
        }

        val batch = firestore.batch()
        batch.update(inviteRef, mapOf("usedCount" to FieldValue.increment(1)))

        val cookbookRef = cookbooksCollection.document(invite.cookbookId)
        batch.update(cookbookRef, mapOf("memberIds" to FieldValue.arrayUnion(userId)))

        val memberRef = cookbooksCollection
            .document(invite.cookbookId)
            .collection("members")
            .document(userId)
        batch.set(memberRef, InviteMemberDoc(role = invite.role))

        batch.commit()

        return invite.cookbookId
    }

    // ---- Code generation ----

    companion object {
        // Base32 minus confusable characters: 0, O, 1, I, L.
        private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        private const val CODE_LENGTH = 8

        /** Turns user input like "7kx9-m2pq" or " 7KX9 M2PQ " into "7KX9M2PQ". */
        fun normalizeCode(raw: String): String =
            raw.uppercase().filter { it in ALPHABET }

        /** Formats a stored code like "7KX9M2PQ" as "7KX9-M2PQ" for display. */
        fun formatForDisplay(code: String): String {
            val normalized = normalizeCode(code)
            return if (normalized.length == CODE_LENGTH) {
                "${normalized.substring(0, 4)}-${normalized.substring(4)}"
            } else {
                normalized
            }
        }

        private fun generateCode(): String = buildString(CODE_LENGTH) {
            repeat(CODE_LENGTH) { append(ALPHABET[Random.nextInt(ALPHABET.length)]) }
        }
    }
}
