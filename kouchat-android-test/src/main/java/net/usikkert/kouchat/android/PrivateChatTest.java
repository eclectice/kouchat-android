
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
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * Test of private chat.
 *
 * @author Christian Ihle
 */
public class PrivateChatTest extends ActivityInstrumentationTestCase2<MainChatController> {

    private static TestClient client;

    private Solo solo;
    private User me;

    private int defaultOrientation;

    public PrivateChatTest() {
        super(MainChatController.class);
    }

    public void setUp() {
        final MainChatController activity = getActivity();

        solo = new Solo(getInstrumentation(), activity);
        me = RobotiumTestUtils.getMe(activity);
        defaultOrientation = RobotiumTestUtils.getCurrentOrientation(solo);

        // Making sure the test client only logs on once during all the tests
        if (client == null) {
            client = new TestClient();
            client.logon();
        }
    }

    public void test01OwnMessageIsShownInChat() {
        openPrivateChat();

        RobotiumTestUtils.writeLine(solo, "This is a new message from myself");
        solo.sleep(500);

        assertTrue(RobotiumTestUtils.searchText(solo, "This is a new message from myself"));
    }

    public void test02OwnMessageShouldArriveAtOtherClient() {
        openPrivateChat();

        RobotiumTestUtils.writeLine(solo, "This is the second message");
        solo.sleep(500);

        assertTrue(client.gotPrivateMessage(me, "This is the second message"));
    }

    public void test03OtherClientMessageIsShownInChat() {
        openPrivateChat();

        client.sendPrivateChatMessage("Hello, this is a message from someone else", me);

        assertTrue(solo.searchText("Hello, this is a message from someone else"));
    }

    public void test04OrientationSwitchShouldKeepText() {
        openPrivateChat();

        RobotiumTestUtils.writeLine(solo, "This is the third message");

        solo.sleep(500);
        RobotiumTestUtils.switchOrientation(solo);

        assertTrue(solo.searchText("This is the third message"));
    }

    // Must be verified manually
    public void test05OrientationSwitchShouldKeepSmileys() {
        openPrivateChat();

        RobotiumTestUtils.writeLine(solo, ":) :( :p :D ;) :O :@ :S ;( :$ 8)");

        solo.sleep(2000);
        RobotiumTestUtils.switchOrientation(solo);
        solo.sleep(2000);
    }

    // Must be verified manually. I haven't found an automated way to verify scrolling yet.
    public void test06OrientationSwitchShouldScrollToBottom() {
        openPrivateChat();

        for (int i = 1; i <= 30; i++) {
            RobotiumTestUtils.writeLine(solo,
                    "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! " +
                            "This is message number " + i + "! ");
        }

        RobotiumTestUtils.switchOrientation(solo);
        solo.sleep(3000); // See if message number 30 is visible
    }

    public void test07InputFieldShouldAlwaysGetKeyEventsAndFocus() {
        openPrivateChat();

        // Starts with focus
        final EditText privateChatInput = (EditText) solo.getView(R.id.privateChatInput);
        assertTrue(privateChatInput.hasFocus());

        // Keeps focus after "enter"
        RobotiumTestUtils.writeLine(solo, "Keep me focused!");
        solo.sleep(500);
        assertTrue(privateChatInput.hasFocus());

        // Need to support losing focus when clicking in the private chat to support text selection. Not happening on 2.3.3
        solo.clickOnView(solo.getView(R.id.privateChatScroll));
        solo.sleep(500);

        // Let's enter a few key strokes when the input field lacks focus
        solo.sendKey(KeyEvent.KEYCODE_A);
        solo.sendKey(KeyEvent.KEYCODE_B);
        solo.sendKey(KeyEvent.KEYCODE_C);

        // The focus should now be back, and the keys entered should be in the input field
        solo.sleep(500);
        assertTrue(privateChatInput.hasFocus());
        assertEquals("abc", privateChatInput.getText().toString());
    }

    // Must be verified manually. Not working on 2.3.3.
    public void test08ShouldBeAbleToSelectText() {
        openPrivateChat();
        solo.sleep(500);

        RobotiumTestUtils.writeLine(solo, "Lets select something");
        solo.sleep(500);

        solo.clickLongOnView(solo.getView(R.id.privateChatScroll));
        solo.sleep(500);
    }

    public void test09BackButtonShouldGoBackToMainChat() {
        openPrivateChat();
        solo.sleep(500);

        final MainChatController mainChat = getActivity();
        assertFalse(mainChat.isVisible());

        RobotiumTestUtils.goBack(solo);
        solo.sleep(500);

        assertTrue(mainChat.isVisible());
    }

    public void test10UpButtonShouldGoBackToMainChat() {
        openPrivateChat();
        solo.sleep(500);

        final MainChatController mainChat = getActivity();
        assertFalse(mainChat.isVisible());

        RobotiumTestUtils.goUp(solo);
        solo.sleep(500);

        assertTrue(mainChat.isVisible());
    }

    public void test99Quit() {
        client.logoff();
        RobotiumTestUtils.quit(solo);
        System.gc();
    }

    public void tearDown() {
        RobotiumTestUtils.setOrientation(solo, defaultOrientation);
        solo.finishOpenedActivities();
    }

    private void openPrivateChat() {
        RobotiumTestUtils.openPrivateChat(solo, 2, 2, "Test");
    }
}
