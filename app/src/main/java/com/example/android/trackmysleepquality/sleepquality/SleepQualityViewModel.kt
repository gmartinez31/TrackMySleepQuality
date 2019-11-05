/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleepquality

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import kotlinx.coroutines.*

class SleepQualityViewModel(
        private val sleepNightKey: Long = 0L,
        val databaseDao: SleepDatabaseDao ) : ViewModel() {

    private val vmJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + vmJob)

    private val _navigateToSleepTrackerFragment = MutableLiveData<Boolean?>()
    val navigateToSleepTrackerFragment: LiveData<Boolean?>
        get() = _navigateToSleepTrackerFragment

    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
    }

    fun doneNavigating() {
        _navigateToSleepTrackerFragment.value = null
    }

    fun onSetSleepQuality( sleepQuality: Int ) {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val tonight = databaseDao.get(sleepNightKey) ?: return@withContext
                tonight.sleepQuality = sleepQuality
                databaseDao.update(tonight)
            }
            _navigateToSleepTrackerFragment.value = true
        }
    }
}