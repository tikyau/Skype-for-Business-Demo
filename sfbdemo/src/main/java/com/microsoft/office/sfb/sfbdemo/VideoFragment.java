package com.microsoft.office.sfb.sfbdemo;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.microsoft.media.MMVRSurfaceView;
import com.microsoft.office.sfb.appsdk.Camera;
import com.microsoft.office.sfb.appsdk.Conversation;
import com.microsoft.office.sfb.appsdk.DevicesManager;
import com.microsoft.office.sfb.appsdk.Observable;
import com.microsoft.office.sfb.appsdk.ObservableList;
import com.microsoft.office.sfb.appsdk.Participant;
import com.microsoft.office.sfb.appsdk.ParticipantVideo;
import com.microsoft.office.sfb.appsdk.SFBException;
import com.microsoft.office.sfb.appsdk.VideoService;

import java.util.ArrayList;

/**
 * The video fragment shows the local participant video preview and
 * the incoming video from the default video channel.
 * <p>
 * Local participant Video Preview:
 * To show video preview the TextureView is used.
 * This sample demonstrates attaching to the TextureView, by passing in the SurfaceTexture
 * obtained from the TextureView to the VideoService::display API.
 * <p>
 * Incoming Video:
 * To display the incoming video, MMVRSurfaceView is provided.
 * The MMVRSurfaceView is an implementation of GLSurfaceView which provides custom rendering.
 * The sample demonstrates attaching to the MMVRSurfaceView passing it to the
 * VideoService::displayParticipantVideo API.
 * Note:
 * This is a temporary API till the implementation to display remote ParticipantVideo
 * is provided.
 */
public class VideoFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private static Conversation conversation = null;
    private static DevicesManager devicesManager = null;

    private VideoService videoService = null;

    private TextureView videoPreviewTextureView = null;

    ArrayList<Camera> cameras = null;

    Camera frontCamera = null;
    Camera backCamera = null;

    ObservableList<Participant> remoteParticipants = null;

    Participant dominantSpeaker = null;

    MMVRSurfaceView mmvrSurfaceView = null;
    MMVRSurfaceView mmvrSurface = null;
    RelativeLayout participantVideoLayout = null;

    public VideoFragment() {
    }

    /**
     * Create the Video fragment.
     *
     * @param conv     Conversation
     * @param dManager DevicesManager.
     * @return A new instance of fragment VideoFragment.
     */
    public static VideoFragment newInstance(Conversation conv, DevicesManager dManager) {
        VideoFragment fragment = new VideoFragment();
        conversation = conv;
        devicesManager = dManager;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.cameras = (ArrayList<Camera>) devicesManager.getCameras();
        for (Camera camera : this.cameras) {
            if (camera.getType() == Camera.Type.FRONTFACING)
                this.frontCamera = camera;
            if (camera.getType() == Camera.Type.BACKFACING)
                this.backCamera = camera;
        }
        this.remoteParticipants = conversation.getRemoteParticipants();
    }

    // Listener class for TextureSurface
    private class VideoPreviewSurfaceTextureListener implements TextureView.SurfaceTextureListener {

        private VideoFragment videoFragment = null;

        public VideoPreviewSurfaceTextureListener(VideoFragment videoFragment) {
            this.videoFragment = videoFragment;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            this.videoFragment.surfaceTextureCreatedCallback(surface);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    // Listener class for MMVRSurfaceView.
    private class VideoStreamSurfaceListener implements MMVRSurfaceView.MMVRCallback {

        private VideoFragment videoFragment;

        public VideoStreamSurfaceListener(VideoFragment videoFragment) {
            this.videoFragment = videoFragment;
        }

        @Override
        public void onSurfaceCreated(MMVRSurfaceView mmvrSurfaceView) {
            videoStreamSurfaceCreatedCallback(mmvrSurfaceView);
        }

        @Override
        public void onFrameRendered(MMVRSurfaceView mmvrSurfaceView) {
        }

        @Override
        public void onRenderSizeChanged(MMVRSurfaceView mmvrSurfaceView, int i, int i1) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.video_fragment_layout, container, false);

        // Get the video service and subscribe to property change notifications.
        this.videoService = conversation.getVideoService();

        // Setup the Video Preview
        // Note:
        // The only reason we have created a VideoPreviewSurfaceTextureListener is so that we can
        // immediately bind to it when the view is created. No requirement to do so.
        this.videoPreviewTextureView = (TextureView) rootView.findViewById(R.id.selfParticipantVideoView);
        this.videoPreviewTextureView.setSurfaceTextureListener(new VideoPreviewSurfaceTextureListener(this));

        try {
            this.videoService.setActiveCamera(this.backCamera);
        } catch (SFBException e) {
            e.printStackTrace();
        }

        // Setup the Incoming Video View
        // Note:
        // The only reason we have created a VideoStreamSurfaceListener is so that we can
        // immediately bind to it when the view is created. No requirement to do so.
        this.participantVideoLayout = (RelativeLayout) rootView.findViewById(R.id.participantVideoLayoutId);
        this.mmvrSurface = (MMVRSurfaceView) rootView.findViewById(R.id.mmvrSurfaceViewId);
        this.mmvrSurface.setCallback(new VideoStreamSurfaceListener(this));

        // Sample e.g. below to dynamically create the MMVRView.
        // this.mmvrSurface = new MMVRSurfaceView(this.participantVideoLayout.getContext());
        // Add view to layout.
        // this.participantVideoLayout.addView(this.mmvrSurface);

        // Inflate the layout for this fragment
        return rootView;
    }

    /**
     * Setup the Video preview.
     *
     * @param texture
     */
    public void surfaceTextureCreatedCallback(SurfaceTexture texture) {
        try {
            // Display the preview
            videoService.showPreview(texture);

            // Check state of video service.
            // If not started, start it.
            if (this.videoService.canStart()) {
                this.videoService.start();
            } else {
                // On joining the meeting the Video service is started by default.
                // Since the view is created later the video service is paused.
                // Resume the service.
                if (this.videoService.getPaused() && this.videoService.canSetPaused()) {
                    this.videoService.setPaused(false);
                }
            }
        } catch (SFBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup the default incoming video channel preview.
     *
     * @param mmvrSurfaceView
     */
    public void videoStreamSurfaceCreatedCallback(MMVRSurfaceView mmvrSurfaceView) {
        Log.v("hihi", "videoStreamSurfaceCreatedCallback");
        this.mmvrSurfaceView = mmvrSurfaceView;
        this.mmvrSurfaceView.setAutoFitMode(MMVRSurfaceView.MMVRAutoFitMode_Crop);
        this.mmvrSurfaceView.requestRender();
        try {
            for (Participant participant : this.remoteParticipants) {
                Log.v("hihi", "participant " + participant.getPerson().getDisplayName());

                if (!participant.getPerson().getDisplayName().equals("kiosk")) {
                    ParticipantVideo participantVideo = participant.getParticipantVideo();
                    participantVideo.subscribe(this.mmvrSurfaceView);
                    break;
                }
            }
        } catch (SFBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) activity;
        } else {
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
