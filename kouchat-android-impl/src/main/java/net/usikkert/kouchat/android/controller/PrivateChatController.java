
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

package net.usikkert.kouchat.android.controller;

import net.usikkert.kouchat.Constants;
import net.usikkert.kouchat.android.AndroidPrivateChatWindow;
import net.usikkert.kouchat.android.AndroidUserInterface;
import net.usikkert.kouchat.android.R;
import net.usikkert.kouchat.android.service.ChatService;
import net.usikkert.kouchat.android.service.ChatServiceBinder;
import net.usikkert.kouchat.misc.User;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Controller for private chat with another user.
 *
 * @author Christian Ihle
 */
public class PrivateChatController extends SherlockActivity {

    private TextView privateChatView;
    private EditText privateChatInput;
    private ScrollView privateChatScroll;
    private ServiceConnection serviceConnection;

    private AndroidUserInterface androidUserInterface;
    private AndroidPrivateChatWindow privateChatWindow;
    private User user;

    /** If this private chat is currently visible. */
    private boolean visible;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.private_chat);

        privateChatInput = (EditText) findViewById(R.id.privateChatInput);
        privateChatView = (TextView) findViewById(R.id.privateChatView);
        privateChatScroll = (ScrollView) findViewById(R.id.privateChatScroll);

        final Intent chatServiceIntent = createChatServiceIntent();
        serviceConnection = createServiceConnection();
        bindService(chatServiceIntent, serviceConnection, Context.BIND_NOT_FOREGROUND);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        ControllerUtils.makeLinksClickable(privateChatView);
        privateChatInput.requestFocus();
    }

    @Override
    protected void onDestroy() {
        if (privateChatWindow != null) {
            privateChatWindow.unregisterPrivateChatController();
            unbindService(serviceConnection);
        }

        androidUserInterface = null;
        privateChatWindow = null;
        user = null;

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;

        if (androidUserInterface != null && user != null) {
            // Make sure that new private message notifications are hidden when the private chat is shown again
            // after being hidden. Happens when the screen is turned off and on again, or after pressing home,
            // and returning to the application, or clicking a link and returning, and so on.
            resetNewPrivateMessageIcon();
        }
    }

    @Override
    protected void onPause() {
        visible = false;
        super.onPause();
    }

    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // Clicked on KouChat icon in the action bar
                return goBackToMainChat();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean goBackToMainChat() {
        startActivity(new Intent(this, MainChatController.class));
        return true;
    }

    /**
     * Makes sure regular key events from anywhere in the activity are sent to the input field,
     * and giving it focus if it doesn't currently have focus.
     *
     * <p>Always asks the activity first, to make sure special keys are handled correctly, like the back button.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public boolean dispatchKeyEvent(final KeyEvent event) {
        if (super.dispatchKeyEvent(event)) {
            return true;
        }

        if (!privateChatInput.hasFocus()) {
            privateChatInput.requestFocus();
        }

        return privateChatInput.dispatchKeyEvent(event);
    }

    private Intent createChatServiceIntent() {
        return new Intent(this, ChatService.class);
    }

    private ServiceConnection createServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName componentName, final IBinder iBinder) {
                final ChatServiceBinder binder = (ChatServiceBinder) iBinder;
                androidUserInterface = binder.getAndroidUserInterface();

                setupPrivateChatWithUser();
            }

            @Override
            public void onServiceDisconnected(final ComponentName componentName) { }
        };
    }

    private void registerPrivateChatInputListener() {
        privateChatInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendPrivateMessage(privateChatInput.getText().toString());
                    privateChatInput.setText("");

                    return true;
                }

                return false;
            }
        });
    }

    private void sendPrivateMessage(final String privateMessage) {
        if (privateMessage != null && privateMessage.trim().length() > 0) {
            androidUserInterface.sendPrivateMessage(privateMessage, user);
        }
    }

    private void setupPrivateChatWithUser() {
        setUser();

        if (user != null) {
            setTitle();
            setPrivateChatWindow();
            resetNewPrivateMessageIcon();
            registerPrivateChatInputListener();
        }
    }

    private void setTitle() {
        final StringBuilder title = new StringBuilder();

        title.append(user.getNick());

        if (!user.isOnline()) {
            title.append(" (offline)");
        }

        else if (user.isAway()) {
            title.append(" (away: ");
            title.append(user.getAwayMsg());
            title.append(")");
        }

        title.append(" - ");
        title.append(Constants.APP_NAME);

        setTitle(title.toString());
    }

    private void setUser() {
        final Intent intent = getIntent();

        final int userCode = intent.getIntExtra("userCode", -1);
        user = androidUserInterface.getUser(userCode);
    }

    private void setPrivateChatWindow() {
        androidUserInterface.createPrivChat(user);

        privateChatWindow = (AndroidPrivateChatWindow) user.getPrivchat();
        privateChatWindow.registerPrivateChatController(this);
    }

    private void resetNewPrivateMessageIcon() {
        androidUserInterface.activatedPrivChat(user);
    }

    public void updatePrivateChat(final CharSequence savedChat) {
        privateChatView.setText(savedChat);

        // Run this after 1 second, because right after a rotate the layout is null and it's not possible to scroll yet
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ControllerUtils.scrollTextViewToBottom(privateChatView, privateChatScroll);
            }
        }, ControllerUtils.ONE_SECOND);
    }

    public void appendToPrivateChat(final CharSequence privateMessage) {
        runOnUiThread(new Runnable() {
            public void run() {
                privateChatView.append(privateMessage);
                ControllerUtils.scrollTextViewToBottom(privateChatView, privateChatScroll);
            }
        });
    }

    /**
     * Returns if this private chat view is currently visible.
     *
     * @return If the view is visible.
     */
    public boolean isVisible() {
        return visible;
    }

    public void updateTitle() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle();
            }
        });
    }
}
