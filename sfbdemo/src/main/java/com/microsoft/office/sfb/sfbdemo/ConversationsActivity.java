/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.office.sfb.sfbdemo;

import android.app.FragmentTransaction;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.microsoft.office.sfb.appsdk.Alert;
import com.microsoft.office.sfb.appsdk.AlertObserver;
import com.microsoft.office.sfb.appsdk.Application;
import com.microsoft.office.sfb.appsdk.Conversation;
import com.microsoft.office.sfb.appsdk.ConversationActivityItem;
import com.microsoft.office.sfb.appsdk.DevicesManager;
import com.microsoft.office.sfb.appsdk.Observable;
import com.microsoft.office.sfb.appsdk.ObservableList;
import com.microsoft.office.sfb.appsdk.ParticipantActivityItem;
import com.microsoft.office.sfb.appsdk.Person;
import com.microsoft.office.sfb.appsdk.SFBException;
import com.microsoft.office.sfb.appsdk.Speaker;

/**
 * The Conversations Activity uses two fragments to provide Conversation & Chat functionality.
 */
public class ConversationsActivity extends AppCompatActivity implements ChatFragment.ChatFragmentInteractionListener {
    private static final String TAG = "ConversationsActivity";
    /**
     * Chat fragment for IM.
     */
    private ChatFragment chatFragment = null;

    /**
     * Video Fragment.
     */
    private VideoFragment videoFragment = null;

    private Conversation currentConversation = null;
    private DevicesManager devicesManager = null;

    Speaker.Endpoint endpoint = null;

    Button videoButton = null;
    LinearLayout conversationsToolbarLayout = null;

    ObservableList<ConversationActivityItem> conversationActivityItemList;

    String incomingGuest = "";

    /**
     * On creation, the activity loads the ConversationsList fragment.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        videoButton = (Button) findViewById(R.id.videoButtonId);

        if (findViewById(R.id.fragment_container) != null) {
            currentConversation = ((SFBDemoApplication) getApplication()).getAnonymousConversation();

            devicesManager = Application.getInstance(this).getDevicesManager();
            devicesManager.getSelectedSpeaker().setActiveEndpoint(Speaker.Endpoint.NONLOUDSPEAKER);

            endpoint = devicesManager.getSelectedSpeaker().getActiveEndpoint();

            // Create the chat fragment.
            this.chatFragment = ChatFragment.newInstance(this.currentConversation);

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, this.chatFragment, null);
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

            // Load the fragment.
            fragmentTransaction.commit();

            conversationActivityItemList = currentConversation.getHistoryService().getConversationActivityItems();
            conversationActivityItemList.addOnListChangedCallback(new ObservableList.OnListChangedCallback<ObservableList<ConversationActivityItem>>() {
                @Override
                public void onChanged(ObservableList<ConversationActivityItem> conversationActivityItems) {
                }

                @Override
                public void onItemRangeChanged(ObservableList<ConversationActivityItem> conversationActivityItems, int i, int i1) {

                }

                @Override
                public void onItemRangeInserted(final ObservableList<ConversationActivityItem> conversationActivityItems, final int position, int itemCount) {
                    ParticipantActivityItem participantActivityItem = (ParticipantActivityItem) conversationActivityItems.get(position);

                    // Register callback for DisplayName and URI
                    participantActivityItem.getPerson().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                        @Override
                        public void onPropertyChanged(Observable observable, int i) {
                            Person person = (Person) observable;
                            Log.v(TAG, "onPropertyChanged " + person.getDisplayName());
                            if(!incomingGuest.equals(person.getDisplayName())) {
                                incomingGuest = person.getDisplayName();
                                if (conversationActivityItems.get(position).getType().equals(ConversationActivityItem.ActivityType.PARTICIPANTJOINED)
                                        && !TextUtils.isEmpty(incomingGuest) && !incomingGuest.equals("kiosk"))
                                    openVideoFragment();
                            }
                        }
                    });
                    Log.v(TAG, "onItemRangeInserted " + conversationActivityItems.get(position).getType().name());
                }

                @Override
                public void onItemRangeMoved(ObservableList<ConversationActivityItem> conversationActivityItems, int i, int i1, int i2) {

                }

                @Override
                public void onItemRangeRemoved(ObservableList<ConversationActivityItem> conversationActivityItems, int i, int i1) {

                }
            });
        }
        this.conversationsToolbarLayout = (LinearLayout) findViewById(R.id.conversationsToolbarId);
    }

    public void onSpeakerButtonClicked(android.view.View view) {
        switch (endpoint) {
            case LOUDSPEAKER:
                devicesManager.getSelectedSpeaker().setActiveEndpoint(Speaker.Endpoint.NONLOUDSPEAKER);
                ((Button) view).setText("Speaker On");
                break;
            case NONLOUDSPEAKER:
                devicesManager.getSelectedSpeaker().setActiveEndpoint(Speaker.Endpoint.LOUDSPEAKER);
                ((Button) view).setText("Speaker Off");
                break;
            default:
        }
        endpoint = devicesManager.getSelectedSpeaker().getActiveEndpoint();
    }

    public void onVideoButtonClicked(android.view.View view) {
        videoFragment = VideoFragment.newInstance(this.currentConversation, devicesManager);

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        // Hide the current fragment.
        fragmentTransaction.hide(this.chatFragment);
        fragmentTransaction.replace(R.id.fragment_container, this.videoFragment, null);
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        // Add transaction to back stack so that "back" button restores state.

        // Load the fragment.
        fragmentTransaction.commit();

        videoButton = (Button) view;
        videoButton.setEnabled(false);

        conversationsToolbarLayout.setVisibility(View.GONE);
    }

    public void openVideoFragment() {
        videoFragment = VideoFragment.newInstance(this.currentConversation, devicesManager);

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        // Hide the current fragment.
        fragmentTransaction.hide(this.chatFragment);
        fragmentTransaction.replace(R.id.fragment_container, this.videoFragment, null);
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        // Add transaction to back stack so that "back" button restores state.

        // Load the fragment.
        fragmentTransaction.commit();

        conversationsToolbarLayout.setVisibility(View.GONE);
    }

    /**
     * onStart
     */
    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * onResume
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * onPause
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * onStop
     */
    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * onDestroy
     */
    @Override
    public void onDestroy() {
        try {
            currentConversation.leave();
        } catch (SFBException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /**
     * Process "back" button press.
     */
    @Override
    public void onBackPressed() {
        // If the chat fragment is loaded, pressing the back button pops the conversationsList fragment.
//        getFragmentManager().popBackStack();
//
//        int count = getFragmentManager().getBackStackEntryCount();
//
//        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
//        if (currentFragment instanceof VideoFragment) {
//            videoButton.setEnabled(true);
//            conversationsToolbarLayout.setVisibility(View.VISIBLE);
//        }
//
//        // If you are on the first loaded fragment let the super class handle the back button event.
//        if (count == 0) {
//            super.onBackPressed();
//        }
    }

    /**
     * The ChatFragment calls this callback method for changes to report to the activity.
     */
    @Override
    public void onChatFragmentInteraction() {
        // Dummy method provided for demonstration
    }
}
