package uk.ac.tees.b1662096.travelhopper_travelapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.tees.b1662096.travelhopper_travelapp.databinding.FragmentCreateNewTripBinding;
import uk.ac.tees.b1662096.travelhopper_travelapp.room.TripEntity;
import uk.ac.tees.b1662096.travelhopper_travelapp.room.TripViewModel;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CreateNewTripFragment#getNewInstance} factory method to
 * create an instance of this fragment.
 */
public class CreateNewTripFragment extends Fragment {

    public FragmentCreateNewTripBinding fragmentCreateNewTripBinding;

    private View rootFragmentView;

    private TripViewModel tripViewModel;

    private static final int READ_EXTERNAL_STORAGE_CODE = 101;

    private boolean isKeyboardVisible = false;

    public CreateNewTripFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CreateNewTripFragment.
     */
    public static CreateNewTripFragment getNewInstance() {
        CreateNewTripFragment createNewTripFragment = new CreateNewTripFragment();
        Bundle fragmentBundle = new Bundle();
        createNewTripFragment.setArguments(fragmentBundle);
        return createNewTripFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater fragmentInflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentCreateNewTripBinding = FragmentCreateNewTripBinding.inflate(fragmentInflater, container, false);
        rootFragmentView = fragmentCreateNewTripBinding.getRoot();


        // Check if the keyboard appears when any text fields are clicked, then make the
        rootFragmentView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect windowFrame = new Rect();
            rootFragmentView.getWindowVisibleDisplayFrame(windowFrame);
            int screenHeight = rootFragmentView.getRootView().getHeight();

            int keyboardHeight = screenHeight - windowFrame.bottom;
            if (keyboardHeight > screenHeight * 0.15) {
                // If the user's keyboard is open, set the content view to be scrollable
                if (!isKeyboardVisible) {
                    isKeyboardVisible = true;
                }
            } else {
                // If the user's keyboard is closed, set the content view to be static
                if (isKeyboardVisible) {
                    isKeyboardVisible = false;
                }
            }
        });


        return rootFragmentView;
    }


    @Override
    public void onViewCreated(@NonNull View rootFragmentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootFragmentView, savedInstanceState);

        // Set the navigation back button in the toolbar to navigate to the previous fragment on the MainActivity
        MaterialToolbar fragmentToolbar = fragmentCreateNewTripBinding.createNewTripFragmentToolbar;
        fragmentToolbar.setNavigationIcon(R.drawable.ic_arrow_back_icon);
        fragmentToolbar.setNavigationOnClickListener(navigateFragmentView -> {
            FragmentTransaction parentFragmentTransaction = getParentFragmentManager().beginTransaction();
            // Replace the current fragment for the previous fragment (MyTripsFragment)
            parentFragmentTransaction.replace(fragmentCreateNewTripBinding.createNewTripFragmentLayout.getId(), MyTripsFragment.getNewInstance());
            parentFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_out_anim, R.anim.fragment_slide_in_anim);
            // Ensure that this fragment is not added to the backstack
            parentFragmentTransaction.addToBackStack(null);
            parentFragmentTransaction.setReorderingAllowed(true);
            parentFragmentTransaction.commit();
            // Set the visibility of current fragment's layout and toolbar to gone when navigating back to previous fragment
            fragmentToolbar.setVisibility(View.GONE);
            fragmentCreateNewTripBinding.createNewTripFragmentLayout.setVisibility(View.GONE);
        });


        // Get the TextFields from the view binding object
        TextInputEditText editTripName = fragmentCreateNewTripBinding.editTripName;
        TextInputEditText editTripLocation = fragmentCreateNewTripBinding.editTripLocation;
        TextInputEditText editTripStartDate = fragmentCreateNewTripBinding.editTripStartDate;
        TextInputEditText editTripEndDate = fragmentCreateNewTripBinding.editTripEndDate;
        TextInputEditText editTripDetails = fragmentCreateNewTripBinding.editTripDetails;

        // Initialise and show the DatePicker when either tripStartDate or tripEndDate fields are clicked
        editTripStartDate.setOnClickListener(view -> {
            MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select start date").build();
            materialDatePicker.show(getChildFragmentManager(), "tripStartDatePicker");
        });

        editTripEndDate.setOnClickListener(view -> {
            MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select start date").build();
            materialDatePicker.show(getChildFragmentManager(), "tripEndDatePicker");
        });

        // Get the imageView from the view binding object
        ImageView newTripMedia = fragmentCreateNewTripBinding.mediaFrameView;
        newTripMedia.setOnClickListener(view -> {
            getMediaFromGallery();
        });

        // Get the ViewModel based on the requirements of the parent fragment (MyTripsFragment)
        tripViewModel = new ViewModelProvider(requireParentFragment()).get(TripViewModel.class);

        // Get the FloatingActionButton from the view binding object
        FloatingActionButton createNewTripButton = fragmentCreateNewTripBinding.createNewTripButton;
        // Once button is clicked, call on the view model to insert a new trip
        createNewTripButton.setOnClickListener(insertTripToMyTrips -> {
            tripViewModel.insertTrip(new TripEntity(0, editTripName.toString(), editTripLocation.toString(), Long.parseLong(editTripStartDate.toString()), Long.parseLong(editTripEndDate.toString()), editTripDetails.toString(), false));
            FragmentTransaction parentFragmentTransaction = getParentFragmentManager().beginTransaction();
            // Replace the current fragment for the previous fragment (MyTripsFragment)
            parentFragmentTransaction.replace(fragmentCreateNewTripBinding.createNewTripFragmentLayout.getId(), MyTripsFragment.getNewInstance());
            parentFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_out_anim, R.anim.fragment_slide_in_anim);
            // Ensure that this fragment is not added to the backstack
            parentFragmentTransaction.addToBackStack(null);
            parentFragmentTransaction.setReorderingAllowed(false);
            parentFragmentTransaction.commit();
            // Set the visibility of current fragment's layout and toolbar to gone when navigating back to previous fragment
            fragmentToolbar.setVisibility(View.GONE);
            fragmentCreateNewTripBinding.createNewTripFragmentLayout.setVisibility(View.GONE);
        });

        // Get the FloatingActionButton from the view binding object
        FloatingActionButton addTripToFavouritesButton = fragmentCreateNewTripBinding.addTripToFavouritesButton;
        addTripToFavouritesButton.setOnClickListener(view -> {
            tripViewModel.insertFavouriteTrip(new TripEntity(0, editTripName.toString(), editTripLocation.toString(), Long.parseLong(editTripStartDate.toString()), Long.parseLong(editTripEndDate.toString()), editTripDetails.toString(), true), true);
            Snackbar.make(rootFragmentView, "New trip has been added to your favourites", Snackbar.LENGTH_SHORT).show();
        });


    }

    private void getMediaFromGallery() {
        if (ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_CODE);
        } else {
            Intent getMediaFromGallery = new Intent(Intent.ACTION_GET_CONTENT);
            getMediaFromGallery.setType("media/*");
            // Check if the media from the mime type is an image or video
            if (getMediaFromGallery.getType() == "image/*") {
                isMediaImage(getMediaFromGallery.getData().getPath());
            } else if (getMediaFromGallery.getType() == "video/*") {
                isMediaVideo(getMediaFromGallery.getData().getPath());
            }
            final PackageManager packageManager = requireActivity().getPackageManager();
            if (getMediaFromGallery.resolveActivity(packageManager) != null) {
                mediaChooserResultLauncher.launch(getMediaFromGallery);
            }
        }
    }

    // Check if the media picked from the user is an image
    private static boolean isMediaImage(String uriMediaPath) {
        String imageMimeType = URLConnection.guessContentTypeFromName(uriMediaPath);
        return imageMimeType != null && imageMimeType.startsWith("image");
    }

    // Check if the media picked from the user is a video
    private static boolean isMediaVideo(String uriMediaPath) {
        String videoMimeType = URLConnection.guessContentTypeFromName(uriMediaPath);
        return videoMimeType != null && videoMimeType.startsWith("video");
    }

    // Resolve the result of the intent
    @SuppressLint("NotifyDataSetChanged")
    ActivityResultLauncher<Intent> mediaChooserResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            activityResult -> {
                // Check if the result from the content picker intent is OK
                if (activityResult.getResultCode() == Activity.RESULT_OK) {
                    // Get the data as an intent to pass
                    Intent retrievedMediaData = activityResult.getData();
                    assert retrievedMediaData != null;
                    // Check if intent's data is not null
                    if (retrievedMediaData.getData() != null) {
                        // Check if the media in the intent is an image
                        if (isMediaImage(retrievedMediaData.getData().getPath())){
                            Uri imageUriData = retrievedMediaData.getData();
                            try {
                                // Convert the Uri from the intent into a Bitmap, then add the bitmap to the arrayList
                                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.requireContext().getContentResolver(), imageUriData);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (isMediaVideo(retrievedMediaData.getData().getPath())) {
                            Bitmap videoThumbnailBitmap = ThumbnailUtils.createVideoThumbnail(retrievedMediaData.getData().getPath(), MediaStore.Video.Thumbnails.MICRO_KIND);

                        }
                    }
                } else {
                    Snackbar.make(rootFragmentView, "Media has not been picked", Snackbar.LENGTH_SHORT).show();
                }
            });

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentCreateNewTripBinding = null;
    }

}