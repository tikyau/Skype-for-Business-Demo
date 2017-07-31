/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.office.sfb.sfbdemo;

import java.net.URI;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.microsoft.office.sfb.appsdk.AnonymousSession;
import com.microsoft.office.sfb.appsdk.Application;
import com.microsoft.office.sfb.appsdk.ConfigurationManager;
import com.microsoft.office.sfb.appsdk.Conversation;
import com.microsoft.office.sfb.appsdk.Observable;
import com.microsoft.office.sfb.appsdk.SFBException;
import com.microsoft.office.sfb.appsdk.DevicesManager;

/**
 * Main Activity of the app.
 * The activity provides UI to join the meeting and navigate to the conversations view.
 */
public class MainActivity extends AppCompatActivity {

    Application application = null;
    ConfigurationManager configurationManager = null;
    DevicesManager devicesManager = null;
    ConversationPropertyChangeListener conversationPropertyChangeListener = null;
    Conversation anonymousConversation = null;
    AnonymousSession anonymousSession = null;

    TextView conversationStateTextView = null;
    Button joinMeetingButton = null;
    EditText et_name, et_meeting_url;
    ProgressBar progressBar;

    /**
     * Creating the activity initializes the SDK Application instance.
     *
     * @param savedInstanceState saved instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        application = Application.getInstance(this.getApplication().getApplicationContext());
        devicesManager = application.getDevicesManager();
        configurationManager = application.getConfigurationManager();

        // This flag will enable certain features that are in preview mode.
        // E.g. Audio / Video capability OnPrem topologies.
        configurationManager.enablePreviewFeatures(true);

        // Note that the sample enable video over cellular network. This is not the default.
        configurationManager.setRequireWiFiForVideo(false);

        // Max video channel count needs to be set to view video for more than one participant.
        configurationManager.setMaxVideoChannelCount(5);

        configurationManager.setEndUserAcceptedVideoLicense();

        // Get UI elements.
        conversationStateTextView = (TextView) findViewById(R.id.statusTextViewId);
        conversationStateTextView.setVisibility(View.GONE);
        joinMeetingButton = (Button) findViewById(R.id.joinMeetingButtonId);
        joinMeetingButton.setVisibility(View.GONE);

        et_name = (EditText) findViewById(R.id.et_name);
        et_meeting_url = (EditText) findViewById(R.id.et_meeting_url);
        et_name.setVisibility(View.GONE);
        et_meeting_url.setVisibility(View.GONE);
        et_name.setText("kiosk");
        et_meeting_url.setText("https://meet.lync.com/popsquare.io/xavier.law/OW0EZMKZ");

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        joinMeeting();
    }

    @Override
    protected void onDestroy() {
        configurationManager = null;
        application = null;
        try {
            anonymousConversation.leave();
        } catch (SFBException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void joinMeeting() {
        //Join the meeting.
        String meetingUriString = et_meeting_url.getText().toString();
        URI meetingUri = URI.create(meetingUriString);

        // Join meeting and monitor conversation state to determine meeting join completion.
        try {

            // Set the default device to Speaker
            //this.devicesManager.setActiveEndpoint(DevicesManager.Endpoint.LOUDSPEAKER);

            // To join an Online meeting use the discover URL method. Please refer the documentation for
            // the overall procedure of getting the discover URL and authorization token for a meeting.
            //
            // final URL discoverUrl = new URL("https://meetings.lync.com/platformService/discover?...");
            // final String authToken = "psat=...";
            // this.anonymousSession = this.application.joinMeetingAnonymously(
            //         displayNameTextView.getText().toString(), discoverUrl, authToken);

            anonymousSession = application.joinMeetingAnonymously(et_name.getText().toString(), meetingUri);
            anonymousConversation = anonymousSession.getConversation();
            SFBDemoApplication application = (SFBDemoApplication) getApplication();
            application.setAnonymousConversation(anonymousConversation);

            // Conversation begins in Idle state. It will move from Idle->Establishing->InLobby/Established
            // depending on meeting configuration.
            // We will monitor property change notifications for State property.
            // Once the conversation is Established, we will move to the next activity.
            conversationPropertyChangeListener = new ConversationPropertyChangeListener();
            anonymousConversation.addOnPropertyChangedCallback(conversationPropertyChangeListener);
        } catch (SFBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigate to the Conversations activity.
     */
    private void navigateToConversationsActivity() {
        Intent intent = new Intent(this, ConversationsActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Determines meeting join state based on conversations state.
     */
    public void updateConversationState() {
        Conversation.State state = anonymousConversation.getState();
        conversationStateTextView.setText(state.toString());
        switch (state) {
            case ESTABLISHED:
                progressBar.setVisibility(View.GONE);
                navigateToConversationsActivity();
                break;
            case IDLE:
                conversationStateTextView.setText("");
                if (anonymousConversation != null) {
                    anonymousConversation.removeOnPropertyChangedCallback(this.conversationPropertyChangeListener);
                    anonymousConversation = null;
                }
                break;
            default:
        }
    }

    /**
     * Callback implementation for listening for conversation property changes.
     */
    private class ConversationPropertyChangeListener extends Observable.OnPropertyChangedCallback {
        /**
         * onProperty changed will be called by the Observable instance on a property change.
         *
         * @param sender     Observable instance.
         * @param propertyId property that has changed.
         */
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            if (propertyId == Conversation.STATE_PROPERTY_ID) {
                updateConversationState();
            }
        }
    }
}
