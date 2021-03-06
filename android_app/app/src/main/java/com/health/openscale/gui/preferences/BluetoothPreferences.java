/* Copyright (C) 2014  olie.xdev <olie.xdev@googlemail.com>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>
*/
package com.health.openscale.gui.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.text.Html;

import com.health.openscale.R;
import com.health.openscale.core.bluetooth.BluetoothCommunication;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.health.openscale.core.bluetooth.BluetoothCommunication.getBtDevice;

public class BluetoothPreferences extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PREFERENCE_KEY_BLUETOOTH_DEVICE_TYPE = "btDeviceTypes";
    private static final String PREFERENCE_KEY_BLUETOOTH_DEVICE_NAME = "btDeviceName";
    private static final String PREFERENCE_KEY_BLUETOOTH_SMARTUSERASSIGN = "smartUserAssign";
    private static final String PREFERENCE_KEY_BLUETOOTH_IGNOREOUTOFRANGE = "ignoreOutOfRange";

    private ListPreference deviceTypes;
    private EditTextPreference deviceName;
    private CheckBoxPreference smartAssignEnable;
    private CheckBoxPreference ignoreOutOfRangeEnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.bluetooth_preferences);

        deviceTypes = (ListPreference)findPreference(PREFERENCE_KEY_BLUETOOTH_DEVICE_TYPE);
        deviceName = (EditTextPreference)findPreference(PREFERENCE_KEY_BLUETOOTH_DEVICE_NAME);
        smartAssignEnable = (CheckBoxPreference) findPreference(PREFERENCE_KEY_BLUETOOTH_SMARTUSERASSIGN);
        ignoreOutOfRangeEnable = (CheckBoxPreference) findPreference(PREFERENCE_KEY_BLUETOOTH_IGNOREOUTOFRANGE);

        deviceTypes.setOnPreferenceChangeListener(new deviceTypeOnPreferenceChangeListener());

        updateBluetoothPreferences();
        initSummary(getPreferenceScreen());
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    public void updateBluetoothPreferences() {
        int i = 0;

        ArrayList<String> btEntries = new ArrayList();
        ArrayList<String> btEntryValues = new ArrayList();

        while (true) {
            BluetoothCommunication btCom = getBtDevice(getActivity().getApplicationContext(), i);

            if (btCom == null) {
                break;
            }

            btEntries.add(btCom.deviceName());
            btEntryValues.add(String.valueOf(i));

            i++;
        }

        deviceTypes.setEntries(btEntries.toArray(new CharSequence[btEntries.size()]));
        deviceTypes.setEntryValues(btEntryValues.toArray(new CharSequence[btEntryValues.size()]));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    private void updatePrefSummary(Preference p) {
        if (smartAssignEnable.isChecked()) {
            ignoreOutOfRangeEnable.setEnabled(true);
        } else {
            ignoreOutOfRangeEnable.setEnabled(false);
        }

        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;

            int i = Integer.parseInt(listPref.getValue());

            BluetoothCommunication btCom = BluetoothCommunication.getBtDevice(getActivity().getApplicationContext(), i);

            String summary = new String();

            summary += listPref.getEntry() + "<br>" +
                    getResources().getString(R.string.label_bt_device_support) + ":" + "<br>";

            if (btCom.initSupported()) {
                summary += getResources().getString(R.string.label_bt_device_initialization) + ": " + getResources().getString(R.string.label_yes)+ "<br>";
            } else {
                summary += getResources().getString(R.string.label_bt_device_initialization) + ": " + getResources().getString(R.string.label_no)+ "<br>";
            }

            if (btCom.transferSupported()) {
                summary += getResources().getString(R.string.label_bt_device_data_transfer) + ": " + getResources().getString(R.string.label_yes)+ "<br>";
            } else {
                summary += getResources().getString(R.string.label_bt_device_data_transfer) + ": " + getResources().getString(R.string.label_no)+ "<br>";
            }

            if (btCom.historySupported()) {
                summary += getResources().getString(R.string.label_bt_device_data_history) + ": " + getResources().getString(R.string.label_yes);
            } else {
                summary += getResources().getString(R.string.label_bt_device_data_history) + ": " + getResources().getString(R.string.label_no);
            }

            p.setSummary(Html.fromHtml(summary));
        }

        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().contains("assword"))
            {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }

        if (p instanceof MultiSelectListPreference) {
            MultiSelectListPreference editMultiListPref = (MultiSelectListPreference) p;

            CharSequence[] entries = editMultiListPref.getEntries();
            CharSequence[] entryValues = editMultiListPref.getEntryValues();
            List<String> currentEntries = new ArrayList<>();
            Set<String> currentEntryValues = editMultiListPref.getValues();

            for (int i = 0; i < entries.length; i++)
                if (currentEntryValues.contains(entryValues[i]))
                    currentEntries.add(entries[i].toString());

            p.setSummary(currentEntries.toString());
        }
    }

    private class deviceTypeOnPreferenceChangeListener implements Preference.OnPreferenceChangeListener {

        @Override
        public boolean onPreferenceChange(Preference p, Object o) {
            int i = Integer.parseInt((String)o);

            BluetoothCommunication btCom = BluetoothCommunication.getBtDevice(getActivity().getApplicationContext(), i);

            deviceName.setSummary(btCom.defaultDeviceName());
            deviceName.setText(btCom.defaultDeviceName());

            return true;
        }
    }
}
