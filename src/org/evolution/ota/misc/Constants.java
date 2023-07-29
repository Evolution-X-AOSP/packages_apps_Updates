/*
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2019 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evolution.ota.misc;

public final class Constants {
    public static final String AB_PAYLOAD_BIN_PATH = "payload.bin";
    public static final String AB_PAYLOAD_PROPERTIES_PATH = "payload_properties.txt";
    public static final String PREF_AUTO_UPDATES_CHECK_INTERVAL = "auto_updates_check_interval";
    public static final String PREF_MOBILE_DATA_WARNING = "pref_mobile_data_warning";
    public static final String PREF_CUSTOM_OTA_URL = "pref_custom_ota_url";
    public static final String PREF_NEEDS_REBOOT_ID = "needs_reboot_id";
    public static final String UNCRYPT_FILE_EXT = ".uncrypt";
    public static final String PROP_BUILD_DATE = "org.evolution.build_date_utc";
    public static final String PROP_RECOVERY_UPDATE = "persist.sys.recovery_update";
    public static final String PREF_INSTALL_OLD_TIMESTAMP = "install_old_timestamp";
    public static final String PREF_INSTALL_NEW_TIMESTAMP = "install_new_timestamp";
    public static final String PREF_INSTALL_NEW_FILE_NAME = "install_new_file_name";
    public static final String PREF_INSTALL_PACKAGE_PATH = "install_package_path";
    public static final String PREF_INSTALL_AGAIN = "install_again";
    public static final String PREF_INSTALL_NOTIFIED = "install_notified";
    static final int AUTO_UPDATES_CHECK_INTERVAL_NEVER = 0;
    static final int AUTO_UPDATES_CHECK_INTERVAL_DAILY = 1;
    static final int AUTO_UPDATES_CHECK_INTERVAL_WEEKLY = 2;
    static final int AUTO_UPDATES_CHECK_INTERVAL_MONTHLY = 3;
    public static final String PROP_AB_DEVICE = "ro.build.ab_update";
    public static final String PROP_DEVICE = "org.evolution.device";
    public static final String PROP_BUILD_VERSION = "org.evolution.version.prop";
    public static final String PROP_VERSION_CODE = "org.evolution.ota.version_code";
    public static final String PROP_BUILD_VERSION_2 = "org.evolution.build_version";
    public static final String PROP_VERSION = "org.evolution.version";
    public static final String PROP_KEY_TYPE = "org.evolution.key_type";
    public static final String OTA_URL = "https://raw.githubusercontent.com/Evolution-X-Devices/official_devices/master/";
    public static final String DOWNLOAD_PATH = "/data/system_updates/";
    public static final String EXPORT_PATH = "EvolutionX-Updates/";
    public static final String HAS_SEEN_INFO_DIALOG = "has_seen_info_dialog";
    public static String fileName = "";
}
