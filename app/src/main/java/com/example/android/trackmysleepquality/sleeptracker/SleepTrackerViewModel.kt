package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 *
 * Essentially what's happening here is the following:
 *
 *      1) Launch a coroutine that runs on the UI thread - initializeTonight()
 *      2) Calls a suspend function to do the long-running work - getTonightFromDB()
 *      3) Context shifts from UI to I/O context so work runs on thread pool optimized for it
 *      4) Actual db call is made - database.getTonight()
 *
 * See below for a template of sorts.
 *
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    // will be the job that cancels all other coroutines started by the VM when it's no longer used
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private var tonight = MutableLiveData<SleepNight?>()
    private val nights = database.getAllNights()
    val nightsString = Transformations.map(nights) {
        nights -> formatNights(nights, application.resources)
    }

    init {
        initializeTonight()
    }

    fun onStartTracking() {
        // we launch a coroutine in this scope b/c we need result to continue and update the UI
        uiScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDB()
        }
    }

    fun onStopTracking() {
        uiScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTime = System.currentTimeMillis()
            update(oldNight)
        }
    }

    fun onClear() {
        uiScope.launch {
            clear()
            tonight.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private fun initializeTonight() {
        uiScope.launch {
            tonight.value = getTonightFromDB()
        }
    }

    //DB funcs
    private suspend fun insert(sleepNight: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(night = sleepNight)
        }
    }

    private suspend fun getTonightFromDB(): SleepNight? {
        // runs in Dispatchers.IO context and not the UI thread
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            if (night?.endTime != night?.startTime) {
                night = null
            }
            night
        }
    }

    private suspend fun update(sleepNight: SleepNight) {
        withContext(Dispatchers.IO) {
            database.update(night = sleepNight)
        }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }
}

/*
 * NOT part of this class. Template.
 * fun someWorkNeedsToBeDone {
    uiScope.launch {
        suspendFunction()
      }
   }

    suspend fun suspendFunction() {
       withContext(Dispatchers.IO) {
           longrunningWork()
       }
    }
 */

