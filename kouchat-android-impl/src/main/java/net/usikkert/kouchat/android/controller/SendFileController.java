
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

import java.io.File;

import net.usikkert.kouchat.android.AndroidUserInterface;
import net.usikkert.kouchat.android.R;
import net.usikkert.kouchat.android.service.ChatService;
import net.usikkert.kouchat.android.service.ChatServiceBinder;
import net.usikkert.kouchat.event.UserListListener;
import net.usikkert.kouchat.misc.User;
import net.usikkert.kouchat.misc.UserList;
import net.usikkert.kouchat.util.Tools;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Controller for showing the "send file" dialog from the "share/send to" menu in other
 * Android applications like file managers and galleries.
 *
 * @author Christian Ihle
 */
public class SendFileController extends Activity implements UserListListener {

    private ServiceConnection serviceConnection;
    private UserListAdapter userListAdapter;
    private TextView line2TextView;
    private ListView userListView;
    private AndroidUserInterface androidUserInterface;

    private File fileToSend;
    private UserList userList;

    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.send_file_dialog);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_dialog);

        final Intent intent = getIntent();
        final Uri uriToFile = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        fileToSend = getFileFromUri(uriToFile);

        final TextView line1TextView = (TextView) findViewById(R.id.sendFileLine1TextView);
        line2TextView = (TextView) findViewById(R.id.sendFileLine2TextView);
        userListView = (ListView) findViewById(R.id.sendFileUserListView);

        // File not found
        if (fileToSend == null) {
            line1TextView.setText(getString(R.string.send_file_no_file));

            if (uriToFile != null) {
                // File was specified, but probably using an unsupported scheme
                line2TextView.setText(uriToFile.toString());
            } else {
                // File was never specified at all
                line2TextView.setVisibility(View.GONE);
            }
        }

        // File was found
        else {
            line1TextView.setText(getString(R.string.send_file_info, fileToSend.getName(), Tools.byteToString(fileToSend.length())));
            line2TextView.setText(getString(R.string.send_file_no_users));

            final Intent chatServiceIntent = new Intent(this, ChatService.class);
            startService(chatServiceIntent);
            serviceConnection = createServiceConnection();
            bindService(chatServiceIntent, serviceConnection, Context.BIND_NOT_FOREGROUND);
        }

        final Button cancelButton = (Button) findViewById(R.id.sendFileCancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (userList != null) {
            userList.removeUserListListener(this);
            unbindService(serviceConnection);
        }

        userList = null;
        androidUserInterface = null;

        super.onDestroy();
    }

    private void registerUserListClickListener() {
        userListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, final View view, final int position, final long id) {
                final User selectedUser = (User) adapterView.getItemAtPosition(position);
                androidUserInterface.sendFile(selectedUser, fileToSend);
                finish();
            }
        });
    }

    private ServiceConnection createServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName componentName, final IBinder iBinder) {
                final ChatServiceBinder binder = (ChatServiceBinder) iBinder;
                androidUserInterface = binder.getAndroidUserInterface();

                userListAdapter = new UserListAdapterWithoutMe(SendFileController.this, androidUserInterface.getMe());
                userListView.setAdapter(userListAdapter);

                userList = androidUserInterface.getUserList();
                userList.addUserListListener(SendFileController.this);
                userListAdapter.addUsers(userList);
                selectTextForLine2();

                registerUserListClickListener();
            }

            @Override
            public void onServiceDisconnected(final ComponentName componentName) { }
        };
    }

    private File getFileFromUri(final Uri uri) {
        if (uri == null) {
            return null;
        }

        final String scheme = uri.getScheme();

        if (scheme.equals("content")) {
            final String[] columns = new String[] {MediaStore.MediaColumns.DATA};
            final Cursor cursor = getContentResolver().query(uri, columns, null, null, null);

            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                final String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));

                return new File(path);
            }
        }

        return null;
    }

    @Override
    public void userAdded(final int pos, final User user) {
        runOnUiThread(new Runnable() {
            public void run() {
                userListAdapter.add(user);
                selectTextForLine2();
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

    @Override
    public void userRemoved(final int pos, final User user) {
        runOnUiThread(new Runnable() {
            public void run() {
                userListAdapter.remove(user);
                selectTextForLine2();
            }
        });
    }

    private void selectTextForLine2() {
        if (userListAdapter.isEmpty()) {
            line2TextView.setText(getString(R.string.send_file_no_users));
        } else {
            line2TextView.setText(getString(R.string.send_file_select_user));
        }
    }
}
