package com.example.car002;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.TestCase;

import com.example.car002.MainActivity;
import com.example.car002.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest extends TestCase {

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    private MainActivity activity;

    @Before
    public void setUp() {
        activity = activityTestRule.getActivity();
    }

    @Test
    public void testBluetoothStatus() {
        // Проверка начального статуса Bluetooth
        Espresso.onView(ViewMatchers.withId(R.id.blueturn)).check(ViewAssertions.matches(ViewMatchers.withResourceName("ic_baseline_bluetooth_24")));

        // Нажатие на кнопку включения Bluetooth
        Espresso.onView(ViewMatchers.withId(R.id.blueturn)).perform(ViewActions.click());

        // Проверка изменения статуса Bluetooth
        Espresso.onView(ViewMatchers.withId(R.id.blueturn)).check(ViewAssertions.matches(ViewMatchers.withResourceName("ic_baseline_bluetooth_disabled_24")));
    }
}