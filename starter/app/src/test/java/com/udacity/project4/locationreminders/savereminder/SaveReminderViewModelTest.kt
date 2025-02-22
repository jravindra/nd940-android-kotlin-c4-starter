package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SaveReminderViewModelTest {


    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private lateinit var fakeDataSource: FakeDataSource

    private lateinit var appContext: Application

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() = runBlockingTest {
        fakeDataSource = FakeDataSource()

        appContext = getApplicationContext()
        saveReminderViewModel = SaveReminderViewModel(
            appContext,
            fakeDataSource
        )
    }

    @Test
    fun checkLoading_test() {
        mainCoroutineRule.pauseDispatcher()
        val reminder1 = ReminderDataItem("Title1", "Description1", "location1", 32.1, 32.1)
        saveReminderViewModel.saveReminder(reminder1)

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(),
            `is`(appContext.getString(R.string.reminder_saved))
        )
    }

    @Test
    fun shouldReturnErrorSnackBar_emptyTitle() {
        saveReminderViewModel =
            SaveReminderViewModel(appContext, fakeDataSource)
        val reminder1 = ReminderDataItem(null, null, null, 32.1, 32.1)
        saveReminderViewModel.validateEnteredData(reminder1)

        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )
    }

    @Test
    fun shouldReturnErrorSnackBar_emptylocation() {
        saveReminderViewModel =
            SaveReminderViewModel(appContext, fakeDataSource)
        val reminder1 = ReminderDataItem("Title", null, null, 32.1, 32.1)
        saveReminderViewModel.validateEnteredData(reminder1)

        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )

    }
}