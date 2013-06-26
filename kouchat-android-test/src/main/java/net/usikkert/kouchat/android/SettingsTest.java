
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

import net.usikkert.kouchat.android.controller.MainChatController;
import net.usikkert.kouchat.android.util.RobotiumTestUtils;
import net.usikkert.kouchat.misc.User;
import net.usikkert.kouchat.testclient.TestClient;

import com.jayway.android.robotium.solo.Solo;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Test of changing the settings.
 *
 * @author Christian Ihle
 */
public class SettingsTest extends ActivityInstrumentationTestCase2<MainChatController> {

    private static String originalNickName;

    private Solo solo;
    private User me;
    private TestClient client;

    public SettingsTest() {
        super(MainChatController.class);
    }

    public void setUp() {
        final MainChatController activity = getActivity();

        solo = new Solo(getInstrumentation(), activity);
        me = RobotiumTestUtils.getMe(activity);
    }

    public void test01NickNameCanBeChanged() {
        originalNickName = me.getNick();

        RobotiumTestUtils.clickOnChangeNickNameInTheSettings(solo);
        assertTrue(solo.searchText("Set nick name"));

        RobotiumTestUtils.changeNickNameTo(solo, "Kou");
        assertTrue(solo.searchText("Kou"));

        // Go back to main chat and check result
        RobotiumTestUtils.goBack(solo);
        assertTrue(solo.searchText("You changed nick to Kou"));
    }

    public void test02RestoreNickName() {
        assertNotNull(originalNickName);

        RobotiumTestUtils.clickOnChangeNickNameInTheSettings(solo);

        // Change nick name back to the original value in the popup dialog
        RobotiumTestUtils.changeNickNameTo(solo, originalNickName);
        assertTrue(solo.searchText(originalNickName));
    }

    public void test03ChangingToNickNameInUseShouldOnlyShowToast() {
        client = new TestClient();
        client.logon();

        RobotiumTestUtils.clickOnChangeNickNameInTheSettings(solo);
        RobotiumTestUtils.changeNickNameTo(solo, "Test");

        assertTrue(solo.searchText("The nick name is in use by someone else.")); // Toast
        assertFalse(solo.searchText("Test"));
    }

    public void test99Quit() {
        RobotiumTestUtils.quit(solo);
        System.gc();
    }

    public void tearDown() {
        if (client != null) {
            client.logoff();
        }

        solo.finishOpenedActivities();
    }
}
