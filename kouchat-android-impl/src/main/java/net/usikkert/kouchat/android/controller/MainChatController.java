
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

import net.usikkert.kouchat.android.AndroidUserInterface;
import net.usikkert.kouchat.android.R;
import net.usikkert.kouchat.android.service.ChatService;
import net.usikkert.kouchat.android.service.ChatServiceBinder;
import net.usikkert.kouchat.event.UserListListener;
import net.usikkert.kouchat.misc.User;
import net.usikkert.kouchat.misc.UserList;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Controller for the main chat.
 *
 * <p>Life cycle:</p>
 * <ul>
 *   <li>Application starts:
 *       <ul>
 *           <li>ChatService is created</li>
 *           <li>ChatService is bound</li>
 *       </ul>
 *   </li>
 *   <li>Application is hidden: ChatService is unbound</li>
 *   <li>Application is shown: ChatService is bound</li>
 *   <li>Application shuts down:
 *       <ul>
 *           <li>ChatService is unbound</li>
 *           <li>ChatService is stopped</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * @author Christian Ihle
 */
public class MainChatController extends SherlockActivity implements UserListListener {

    private Intent chatServiceIntent;
    private ServiceConnection serviceConnection;
    private EditText mainChatInput;
    private ListView mainChatUserList;
    private TextView mainChatView;
    private ScrollView mainChatScroll;
    private UserListAdapter userListAdapter;

    private AndroidUserInterface androidUserInterface;
    private UserList userList;

    /** If the main chat is currently visible. */
    private boolean visible;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_chat);

        mainChatInput = (EditText) findViewById(R.id.mainChatInput);
        mainChatUserList = (ListView) findViewById(R.id.mainChatUserList);
        mainChatView = (TextView) findViewById(R.id.mainChatView);
        mainChatScroll = (ScrollView) findViewById(R.id.mainChatScroll);

        registerMainChatInputListener();
        registerMainChatTextListener();
        registerUserListClickListener();
        ControllerUtils.makeLinksClickable(mainChatView);
        setupMainChatUserList();
        openKeyboard();

        chatServiceIntent = createChatServiceIntent();
        startService(chatServiceIntent);
        serviceConnection = createServiceConnection();
        bindService(chatServiceIntent, serviceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private ServiceConnection createServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName componentName, final IBinder iBinder) {
                final ChatServiceBinder binder = (ChatServiceBinder) iBinder;

                androidUserInterface = binder.getAndroidUserInterface();
                androidUserInterface.registerMainChatController(MainChatController.this);
                androidUserInterface.showTopic();

                userList = androidUserInterface.getUserList();
                userList.addUserListListener(MainChatController.this);
                userListAdapter.addUsers(userList);
            }

            @Override
            public void onServiceDisconnected(final ComponentName componentName) { }
        };
    }

    private void registerMainChatInputListener() {
        mainChatInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendMessage(mainChatInput.getText().toString());
                    mainChatInput.setText("");

                    return true;
                }

                return false;
            }
        });
    }

    private void registerMainChatTextListener() {
        mainChatInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (androidUserInterface != null) { // Might be null on orientation changes
                    androidUserInterface.updateMeWriting(!mainChatInput.getText().toString().isEmpty());
                }
            }
        });
    }

    private void registerUserListClickListener() {
        mainChatUserList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> userAdapter, final View view, final int position, final long id) {
                final User selectedUser = (User) userAdapter.getItemAtPosition(position);

                // No point in having a private chat with one self (at least not here)
                if (selectedUser.isMe()) {
                    return;
                }

                final Intent privateChatIntent = new Intent(MainChatController.this, PrivateChatController.class);
                privateChatIntent.putExtra("userCode", selectedUser.getCode());
                startActivity(privateChatIntent);
            }
        });
    }

    private void setupMainChatUserList() {
        userListAdapter = new UserListAdapterWithChatState(this);
        mainChatUserList.setAdapter(userListAdapter);
    }

    private void openKeyboard() {
        mainChatInput.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;

        if (androidUserInterface != null) { // Is null during initial startup. Doesn't matter.
            androidUserInterface.resetAllNotifications();
        }
    }

    @Override
    protected void onPause() {
        visible = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (androidUserInterface != null) {
            userList.removeUserListListener(this);
            androidUserInterface.unregisterMainChatController();
            unbindService(serviceConnection);
        }

        androidUserInterface = null;
        userList = null;

        super.onDestroy();
    }

    /**
     *  Creates the main chat menu.
     *
     *  {@inheritDoc}
     *
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main_chat_menu, menu);

        return true;
    }

    /**
     * Selects the actions to run after a menu item in the main chat has been selected.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainChatMenuQuit:
                return shutdownApplication();
            case R.id.mainChatMenuAbout:
                return showAboutDialog();
            case R.id.mainChatMenuSettings:
                return showSettingsDialog();
            default:
                return super.onOptionsItemSelected(item);
        }
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

        if (!mainChatInput.hasFocus()) {
            mainChatInput.requestFocus();
        }

        return mainChatInput.dispatchKeyEvent(event);
    }

    private boolean showAboutDialog() {
        new AboutDialog(this);
        return true;
    }

    private boolean shutdownApplication() {
        finish();
        stopService(chatServiceIntent);

        return true;
    }

    private boolean showSettingsDialog() {
        startActivity(new Intent(this, SettingsController.class));
        return true;
    }

    private Intent createChatServiceIntent() {
        return new Intent(this, ChatService.class);
    }

    public void appendToChat(final CharSequence message) {
        runOnUiThread(new Runnable() {
            public void run() {
                mainChatView.append(message);
                ControllerUtils.scrollTextViewToBottom(mainChatView, mainChatScroll);
            }
        });
    }

    public void sendMessage(final String message) {
        if (message != null && message.trim().length() > 0) {
            androidUserInterface.sendMessage(message);
        }
    }

    public void updateChat(final CharSequence savedChat) {
        mainChatView.setText(savedChat);

        // Run this after 1 second, because right after a rotate the layout is null and it's not possible to scroll yet
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ControllerUtils.scrollTextViewToBottom(mainChatView, mainChatScroll);
            }
        }, ControllerUtils.ONE_SECOND);

    }

    public void updateTopic(final String topic) {
        runOnUiThread(new Runnable() {
            public void run() {
                setTitle(topic);
            }
        });
    }

    @Override
    public void userAdded(final int pos, final User user) {
        runOnUiThread(new Runnable() {
            public void run() {
                userListAdapter.add(user);
            }
        });
    }

    @Override
    public void userRemoved(final int pos, final User user) {
        runOnUiThread(new Runnable() {
            public void run() {
                userListAdapter.remove(user);
            }
        });
    }

    @Override
    public void userChanged(final int pos, final User user) {
        runOnUiThread(new Runnable() {
            public void run() {
                userListAdapter.sort();
            }
        });
    }

    /**
     * Returns if the main chat view is currently visible.
     *
     * @return If the view is visible.
     */
    public boolean isVisible() {
        return visible;
    }
}
