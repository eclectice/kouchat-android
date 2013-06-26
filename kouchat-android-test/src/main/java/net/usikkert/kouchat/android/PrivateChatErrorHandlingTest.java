
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

package net.usikkert.kouchat.android;

import net.usikkert.kouchat.android.controller.PrivateChatController;
import net.usikkert.kouchat.android.service.ChatService;
import net.usikkert.kouchat.android.util.RobotiumTestUtils;
import net.usikkert.kouchat.testclient.TestClient;
import net.usikkert.kouchat.testclient.TestUtils;

import com.jayway.android.robotium.solo.Solo;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Tests how the private chat handles different situations where the required data might be missing.
 *
 * @author Christian Ihle
 */
public class PrivateChatErrorHandlingTest extends ActivityInstrumentationTestCase2<PrivateChatController> {

    private Solo solo;
    private TestClient client;

    public PrivateChatErrorHandlingTest() {
        super(PrivateChatController.class);
    }

    public void test01PrivateChatWithNoServiceRunningAndNoUser() {
        final PrivateChatController activity = getActivity();

        solo = new Solo(getInstrumentation(), activity);
        solo.sleep(1000);

        assertEquals("User not found - KouChat", activity.getTitle());
        RobotiumTestUtils.writeLine(solo, "Should not be able to send this");
        solo.sleep(500);
        assertFalse(RobotiumTestUtils.searchText(solo, "Should not be able to send this"));

        // Service has not been started
        assertTrue(TestUtils.fieldValueIsNull(activity, "androidUserInterface"));

        solo.sleep(1000);
    }

    public void test02PrivateChatWithServiceButNoUser() {
        final PrivateChatController activity = getActivity();
        activity.startService(new Intent(activity, ChatService.class));

        solo = new Solo(getInstrumentation(), activity);
        solo.sleep(1000);

        assertEquals("User not found - KouChat", activity.getTitle());
        RobotiumTestUtils.writeLine(solo, "Should not be able to send this");
        solo.sleep(500);
        assertFalse(RobotiumTestUtils.searchText(solo, "Should not be able to send this"));

        // Service has been started
        assertFalse(TestUtils.fieldValueIsNull(activity, "androidUserInterface"));

        solo.sleep(1000);
    }

    public void test03PrivateChatWithServiceAndUser() {
        client = new TestClient("Kou", 12345678);
        client.logon();

        final Intent intent = new Intent();
        intent.putExtra("userCode", 12345678);
        setActivityIntent(intent);

        final PrivateChatController activity = getActivity();

        solo = new Solo(getInstrumentation(), activity);
        solo.sleep(1000);

        assertEquals("Kou - KouChat", activity.getTitle());
        RobotiumTestUtils.writeLine(solo, "Should be able to send this");
        solo.sleep(500);
        assertTrue(RobotiumTestUtils.searchText(solo, "Should be able to send this"));

        // Service is still running
        assertFalse(TestUtils.fieldValueIsNull(activity, "androidUserInterface"));

        solo.sleep(1000);
    }

    public void test99Quit() {
        solo = new Solo(getInstrumentation(), getActivity());
        solo.sleep(500);

        RobotiumTestUtils.launchMainChat(this);
        solo.sleep(500);

        RobotiumTestUtils.quit(solo);
        solo.sleep(500);

        System.gc();
    }

    public void tearDown() {
        if (client != null) {
            client.logoff();
        }

        solo.finishOpenedActivities();
    }
}
