package com.resqlink.app.data.repository

import com.resqlink.app.data.local.dao.UserDao
import com.resqlink.app.data.local.entity.UserEntity
import com.resqlink.app.domain.model.User
import com.resqlink.app.domain.model.UserRole
import com.resqlink.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao
) : UserRepository {

    override fun observeCurrentUser(): Flow<User?> =
        dao.observeCurrentUser().map { it?.toDomain() }

    override suspend fun saveProfile(user: User) {
        dao.upsert(
            UserEntity(
                id = user.id, name = user.name, phone = user.phone, role = user.role.name,
                bloodGroup = user.bloodGroup, emergencyContact = user.emergencyContact,
                medicalNotes = user.medicalNotes
            )
        )
    }
}

private fun UserEntity.toDomain() = User(
    id = id, name = name, phone = phone, role = UserRole.valueOf(role),
    bloodGroup = bloodGroup, emergencyContact = emergencyContact, medicalNotes = medicalNotes
)
