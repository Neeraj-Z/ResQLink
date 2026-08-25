package com.resqlink.app.domain.repository

import com.resqlink.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeCurrentUser(): Flow<User?>
    suspend fun saveProfile(user: User)
}
