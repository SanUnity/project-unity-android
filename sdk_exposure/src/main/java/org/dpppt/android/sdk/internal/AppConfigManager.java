/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.dpppt.android.sdk.internal.backend.BackendReportRepository;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.models.DayDate;

public class AppConfigManager {

    private static AppConfigManager instance;

    public static synchronized AppConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppConfigManager(context.getApplicationContext());
        }
        return instance;
    }

    private static final String PREFS_NAME = "dp3t_sdk_preferences";
    private static final String PREF_APPLICATION = "application";
    private static final String PREF_TRACING_ENABLED = "tracingEnabled";
    private static final String PREF_LAST_SYNC_DATE = "lastSyncDate";
    private static final String PREF_LAST_SYNC_NET_SUCCESS = "lastSyncNetSuccess";
    private static final String PREF_I_AM_INFECTED = "IAmInfected";
    private static final String PREF_I_AM_INFECTED_IS_RESETTABLE = "IAmInfectedIsResettable";
    private static final String PREF_CALIBRATION_TEST_DEVICE_NAME = "calibrationTestDeviceName";
    private static final String PREF_LAST_LOADED_TIMES = "lastLoadedTimes";
    private static final String PREF_LAST_SYNC_CALL_TIMES = "lastExposureClientCalls";

    private static final String PREF_ATTENUATION_THRESHOLD_LOW = "attenuationThresholdLow";
    private static final String PREF_ATTENUATION_THRESHOLD_MEDIUM = "attenuationThresholdMedium";
    private static final String PREF_ATTENUATION_FACTOR_LOW = "attenuationFactorLow";
    private static final String PREF_ATTENUATION_FACTOR_MEDIUM = "attenuationFactorMedium";

    //USED IN OUR CONFIGURATION
    private static final int DEFAULT_ATTENUATION_THRESHOLD_LOW = 50;
    private static final int DEFAULT_ATTENUATION_THRESHOLD_MEDIUM = 60;
    private static final float DEFAULT_ATTENUATION_FACTOR_LOW = 1.0f;
    private static final float DEFAULT_ATTENUATION_FACTOR_MEDIUM = 0.5f;
    private static final int DEFAULT_MIN_DURATION_FOR_EXPOSURE = 15;
    private static final String PREF_MIN_DURATION_FOR_EXPOSURE = "minDurationForExposure";

    //EXTRA CORE
    private static final String PREF_MINIMUM_RISK_SCORE = "minimumRiskScore";
    private static final String PREF_ATTENUATION_LEVEL_VALUES = "attenuationLevelValues";
    private static final String PREF_DAYS_SINCE_LAST_EXPOSURE_LV = "daysSinceLastExposureLevelValues";
    private static final String PREF_DURATION_LEVEL_VALUES = "durationLevelValues";
    private static final String PREF_TRANSMISSION_RISK_LV = "transmissionRiskLevelValues";
    private static final int DEFAULT_MINIMUM_RISK_SCORE = 1;
    private static final String DEFAULT_ATTENUATION_LEVEL_VALUES = "0,1,3,5,6,7,8,8";
    private static final String DEFAULT_TRANSMISSION_RISK_LV = "0,1,2,3,5,6,7,8";
    private static final String DEFAULT_DAYS_SINCE_LAST_EXPOSURE_LV = "0,0,2,4,5,6,7,8";
    private static final String DEFAULT_DURATION_LEVEL_VALUES = "0,0,1,2,4,6,7,8";

    private String appId;
    private SharedPreferences sharedPrefs;
    private GoogleExposureClient googleExposureClient;

    private AppConfigManager(Context context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        googleExposureClient = GoogleExposureClient.getInstance(context);
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setManualApplicationInfo(ApplicationInfo applicationInfo) {
        setAppId(applicationInfo.getAppId());
        sharedPrefs.edit().putString(PREF_APPLICATION, Json.toJson(applicationInfo)).apply();
    }

    public ApplicationInfo getAppConfig() {
        return Json.fromJson(sharedPrefs.getString(PREF_APPLICATION, "{}"), ApplicationInfo.class);
    }

    public void setTracingEnabled(boolean enabled) {
        sharedPrefs.edit().putBoolean(PREF_TRACING_ENABLED, enabled).apply();
    }

    public boolean isTracingEnabled() {
        return sharedPrefs.getBoolean(PREF_TRACING_ENABLED, false);
    }

    public void setLastSyncDate(long lastSyncDate) {
        sharedPrefs.edit().putLong(PREF_LAST_SYNC_DATE, lastSyncDate).apply();
    }

    public long getLastSyncDate() {
        return sharedPrefs.getLong(PREF_LAST_SYNC_DATE, 0);
    }

    public void setLastSyncNetworkSuccess(boolean success) {
        sharedPrefs.edit().putBoolean(PREF_LAST_SYNC_NET_SUCCESS, success).apply();
    }

    public boolean getLastSyncNetworkSuccess() {
        return sharedPrefs.getBoolean(PREF_LAST_SYNC_NET_SUCCESS, true);
    }

    public boolean getIAmInfected() {
        return sharedPrefs.getBoolean(PREF_I_AM_INFECTED, false);
    }

    public void setIAmInfected(boolean infected) {
        sharedPrefs.edit().putBoolean(PREF_I_AM_INFECTED, infected).apply();
    }

    public void setIAmInfectedIsResettable(boolean resettable) {
        sharedPrefs.edit().putBoolean(PREF_I_AM_INFECTED_IS_RESETTABLE, resettable).apply();
    }

    public boolean getIAmInfectedIsResettable() {
        return sharedPrefs.getBoolean(PREF_I_AM_INFECTED_IS_RESETTABLE, false);
    }

    public BackendReportRepository getBackendReportRepository(Context context) throws IllegalStateException {
        ApplicationInfo appConfig = getAppConfig();
        return new BackendReportRepository(context, appConfig.getReportBaseUrl());
    }

    public void setCalibrationTestDeviceName(String name) {
        sharedPrefs.edit().putString(PREF_CALIBRATION_TEST_DEVICE_NAME, name).apply();
    }

    public String getCalibrationTestDeviceName() {
        return sharedPrefs.getString(PREF_CALIBRATION_TEST_DEVICE_NAME, null);
    }

    public void clearPreferences() {
        sharedPrefs.edit().clear().apply();
    }

    public int getMinDurationForExposure() {
        return sharedPrefs.getInt(PREF_MIN_DURATION_FOR_EXPOSURE, DEFAULT_MIN_DURATION_FOR_EXPOSURE);
    }

    public void setMinDurationForExposure(int minDuration) {
        sharedPrefs.edit().putInt(PREF_MIN_DURATION_FOR_EXPOSURE, minDuration).apply();
    }

    public int getAttenuationThresholdLow() {
        return sharedPrefs.getInt(PREF_ATTENUATION_THRESHOLD_LOW, DEFAULT_ATTENUATION_THRESHOLD_LOW);
    }

    public void setAttenuationThresholdLow(int threshold) {
        sharedPrefs.edit().putInt(PREF_ATTENUATION_THRESHOLD_LOW, threshold).apply();
    }

    public int getAttenuationThresholdMedium() {
        return sharedPrefs.getInt(PREF_ATTENUATION_THRESHOLD_MEDIUM, DEFAULT_ATTENUATION_THRESHOLD_MEDIUM);
    }

    public void setAttenuationThresholdMedium(int threshold) {
        sharedPrefs.edit().putInt(PREF_ATTENUATION_THRESHOLD_MEDIUM, threshold).apply();
    }

    public float getAttenuationFactorLow() {
        return sharedPrefs.getFloat(PREF_ATTENUATION_FACTOR_LOW, DEFAULT_ATTENUATION_FACTOR_LOW);
    }

    public void setAttenuationFactorLow(float factor) {
        sharedPrefs.edit().putFloat(PREF_ATTENUATION_FACTOR_LOW, factor).apply();
    }

    public float getAttenuationFactorMedium() {
        return sharedPrefs.getFloat(PREF_ATTENUATION_FACTOR_MEDIUM, DEFAULT_ATTENUATION_FACTOR_MEDIUM);
    }

    public void setAttenuationFactorMedium(float factor) {
        sharedPrefs.edit().putFloat(PREF_ATTENUATION_FACTOR_MEDIUM, factor).apply();
    }

    public HashMap<DayDate, Long> getLastLoadedTimes() {
        return convertToDateMap(Json.fromJson(sharedPrefs.getString(PREF_LAST_LOADED_TIMES, "{}"), StringLongMap.class));
    }

    public HashMap<DayDate, Long> getLastSyncCallTimes() {
        return convertToDateMap(Json.fromJson(sharedPrefs.getString(PREF_LAST_SYNC_CALL_TIMES, "{}"), StringLongMap.class));
    }

    public void setLastLoadedTimes(HashMap<DayDate, Long> lastLoadedTimes) {
        sharedPrefs.edit().putString(PREF_LAST_LOADED_TIMES, Json.toJson(convertFromDateMap(lastLoadedTimes))).apply();
    }

    public void setLastSyncCallTimes(HashMap<DayDate, Long> lastExposureClientCalls) {
        sharedPrefs.edit().putString(PREF_LAST_SYNC_CALL_TIMES, Json.toJson(convertFromDateMap(lastExposureClientCalls)))
                .apply();
    }

    public int getMinimumRiskScore() {
        return sharedPrefs.getInt(PREF_MINIMUM_RISK_SCORE, DEFAULT_MINIMUM_RISK_SCORE);
    }

    public void setMinimumRiskScore(int factor) {
        sharedPrefs.edit().putInt(PREF_MINIMUM_RISK_SCORE, factor).apply();
    }

    public int[] getAttenuationLevelValues() {
        return stringToIntArray(
                sharedPrefs.getString(PREF_ATTENUATION_LEVEL_VALUES, DEFAULT_ATTENUATION_LEVEL_VALUES));
    }

    public void setAttenuationLevelValues(ArrayList<Integer> factor) {
        sharedPrefs.edit().putString(PREF_ATTENUATION_LEVEL_VALUES, intArrayToString(factor)).apply();
    }

    public int[] getDaysSinceLastExposureLevelValues() {
        return stringToIntArray(
                sharedPrefs.getString(PREF_DAYS_SINCE_LAST_EXPOSURE_LV, DEFAULT_DAYS_SINCE_LAST_EXPOSURE_LV));
    }

    public void setDaysSinceLastExposureLevelValues(ArrayList<Integer> factor) {
        sharedPrefs.edit().putString(PREF_DAYS_SINCE_LAST_EXPOSURE_LV, intArrayToString(factor)).apply();
    }

    public int[] getDurationLevelValues() {
        return stringToIntArray(
                sharedPrefs.getString(PREF_DURATION_LEVEL_VALUES, DEFAULT_DURATION_LEVEL_VALUES));
    }

    public void setDurationLevelValues(ArrayList<Integer> factor) {
        sharedPrefs.edit().putString(PREF_DURATION_LEVEL_VALUES, intArrayToString(factor)).apply();
    }

    public int[] getTransmissionRiskLevelValues() {
        return stringToIntArray(
                sharedPrefs.getString(PREF_TRANSMISSION_RISK_LV, DEFAULT_TRANSMISSION_RISK_LV));
    }

    public void setTransmissionRiskLevelValues(ArrayList<Integer> factor) {
        sharedPrefs.edit().putString(PREF_TRANSMISSION_RISK_LV, intArrayToString(factor)).apply();
    }

    public void clear() {
        sharedPrefs.edit().clear().apply();
    }

    private HashMap<DayDate, Long> convertToDateMap(HashMap<String, Long> map) {
        HashMap<DayDate, Long> result = new HashMap<>();
        for (Map.Entry<String, Long> stringLongEntry : map.entrySet()) {
            try {
                result.put(new DayDate(stringLongEntry.getKey()), stringLongEntry.getValue());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private HashMap<String, Long> convertFromDateMap(HashMap<DayDate, Long> map) {
        HashMap<String, Long> result = new HashMap<>();
        for (Map.Entry<DayDate, Long> stringLongEntry : map.entrySet()) {
            result.put(stringLongEntry.getKey().formatAsString(), stringLongEntry.getValue());
        }
        return result;
    }

    private int[] stringToIntArray(String stringArray) {
        String[] st = stringArray.split(",");
        int[] savedList = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            savedList[i] = Integer.parseInt(st[i]);
        }
        return savedList;
    }

    private String intArrayToString(ArrayList<Integer> array) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < array.size(); i++) {
            str.append(array.get(i)).append(",");
        }
        return str.toString();
    }

    private static class StringLongMap extends HashMap<String, Long> {
    }

}
