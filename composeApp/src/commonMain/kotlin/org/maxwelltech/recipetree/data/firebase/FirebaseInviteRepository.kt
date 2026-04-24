package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.Serializable
import org.maxwelltech.recipetree.data.model.Cookbook
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

        val inviteRef = invitesCollection.document(normalized)

        // Everything lives inside a single Firestore transaction so concurrent
        // accepts on the same code can't double-spend a seat. Firestore's
        // optimistic concurrency retries the whole block if either doc we read
        // changes between read and commit.
        return firestore.runTransaction {
            // ---- All reads first (required by Firestore transactions) ----
            val inviteSnap = get(inviteRef)
            if (!inviteSnap.exists) {
                throw IllegalStateException("Invite not found")
            }
            val invite = inviteSnap.data<Invite>()

            val cookbookRef = cookbooksCollection.document(invite.cookbookId)
            val cookbookSnap = get(cookbookRef)
            if (!cookbookSnap.exists) {
                // The cookbook was deleted between invite creation and this
                // accept — fail loudly instead of dangling a member doc under
                // a missing parent.
                throw IllegalStateException("This cookbook is no longer available")
            }
            val cookbook = cookbookSnap.data<Cookbook>()

            // ---- Validation ----
            if (invite.revoked) {
                throw IllegalStateException("Invite was revoked")
            }
            val expiresAt = invite.expiresAt
            val now = Clock.System.now().toEpochMilliseconds()
            if (expiresAt != null && now > expiresAt) {
                throw IllegalStateException("Invite expired")
            }

            // Dedup: if the user is already a member, short-circuit with the
            // cookbookId so the caller can navigate them there. We deliberately
            // do NOT increment usedCount on a re-join — leaked code abuse
            // shouldn't get easier just because someone already joined once.
            if (userId in cookbook.memberIds) {
                return@runTransaction invite.cookbookId
            }

            // Only enforce the seat cap for users who'd actually consume one.
            if (invite.usedCount >= invite.maxUses) {
                throw IllegalStateException("Invite is fully used")
            }

            // ---- Writes (reads above have pinned the versions we're branching on) ----
            // Explicit `usedCount + 1` and explicit member list instead of
            // FieldValue.increment / arrayUnion — the transaction already has the
            // current values, and explicit writes keep the Firestore security
            // rule for usedCount simple to express.
            update(inviteRef, mapOf(
                "usedCount" to invite.usedCount + 1,
                "lastUsedAt" to now,
                "lastUsedBy" to userId
            ))
            update(cookbookRef, mapOf(
                "memberIds" to cookbook.memberIds + userId
            ))

            val memberRef = cookbooksCollection
                .document(invite.cookbookId)
                .collection("members")
                .document(userId)
            set(memberRef, InviteMemberDoc(role = invite.role))

            invite.cookbookId
        }
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
