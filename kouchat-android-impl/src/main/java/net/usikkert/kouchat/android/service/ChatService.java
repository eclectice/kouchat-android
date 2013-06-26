
/***************************************************************************
 *   Copyright 2006-2013 by Christian Ihle                                 *
 *   kontakt@usikkert.net                                                  *
 *                                                                         *
 *   This file is part of KouChat.                                         *
 *                                                                         *
 *   KouChat is free software; you can redistribute it and/or modify       *
 *   it under the terms of the GNU Lesser General Public License as        *
 *   published by the Free Software Foundation, either version 3 of        *
 *   the License, or (at your option) any later version.                   *
 *                                                                         *
 *   KouChat is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU      *
 *   Lesser General Public License for more details.                       *
 *                                                                         *
 *   You should have received a copy of the GNU Lesser General Public      *
 *   License along with KouChat.                                           *
 *   If not, see <http://www.gnu.org/licenses/>.                           *
 ***************************************************************************/

package net.usikkert.kouchat.android.service;

import net.usikkert.kouchat.Constants;
import net.usikkert.kouchat.android.AndroidUserInterface;
import net.usikkert.kouchat.android.notification.NotificationService;
import net.usikkert.kouchat.misc.Settings;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;

/**
 * Service for accessing the chat operations in lower layers.
 *
 * <p>Starts as a foreground service to avoid being killed randomly.
 * This means a notification is available at all times.</p>
 *
 * @author Christian Ihle
 */
public class ChatService extends Service {

    private AndroidUserInterface androidUserInterface;
    private NotificationService notificationService;
    private MulticastLockHandler multicastLockHandler;

    @Override
    public void onCreate() {
        System.setProperty(Constants.PROPERTY_CLIENT_UI, "Android");

        notificationService = new NotificationService(this);
        androidUserInterface = new AndroidUserInterface(this, new Settings(), notificationService);
        androidUserInterface.setNickNameFromSettings();

        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        multicastLockHandler = new MulticastLockHandler(wifiManager, androidUserInterface);

        super.onCreate();
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        if (!androidUserInterface.isLoggedOn()) {
            androidUserInterface.logOn();
        }

        startForeground(NotificationService.SERVICE_NOTIFICATION_ID, notificationService.createServiceNotification());

        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return new ChatServiceBinder(androidUserInterface);
    }

    @Override
    public void onDestroy() {
        androidUserInterface.logOff();
        multicastLockHandler.release();

        super.onDestroy();
    }
}
