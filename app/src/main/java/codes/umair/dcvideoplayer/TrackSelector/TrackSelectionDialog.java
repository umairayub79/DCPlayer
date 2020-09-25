package codes.umair.dcvideoplayer.TrackSelector;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import codes.umair.dcvideoplayer.R;

/** Dialog to select tracks. */
public final class TrackSelectionDialog extends DialogFragment {
    private static String TAG = "TrackSelectionDialog";
    private final SparseArray<TrackSelectionViewFragment> tabFragments;
    private final ArrayList<Integer> tabTrackTypes;

    private int titleId;
    private DialogInterface.OnClickListener onClickListener;
    private DialogInterface.OnDismissListener onDismissListener;

    /**
     * Returns whether a track selection dialog will have content to display if initialized with the
     * specified {@link DefaultTrackSelector} in its current state.
     */
    public static boolean willHaveContent(DefaultTrackSelector trackSelector) {
        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        return mappedTrackInfo != null && willHaveContent(mappedTrackInfo);
    }

    /**
     * Returns whether a track selection dialog will have content to display if initialized with the
     * specified {@link MappedTrackInfo}.
     */
    public static boolean willHaveContent(MappedTrackInfo mappedTrackInfo) {
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (showTabForRenderer(mappedTrackInfo, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a dialog for a given {@link DefaultTrackSelector}, whose parameters will be
     * automatically updated when tracks are selected.
     *
     * @param trackSelector The {@link DefaultTrackSelector}.
     * @param onDismissListener A {@link DialogInterface.OnDismissListener} to call when the dialog is
     *     dismissed.
     */
    public static TrackSelectionDialog createForTrackSelector(DefaultTrackSelector trackSelector,int trackID, int trackType, DialogInterface.OnDismissListener onDismissListener) {
        MappedTrackInfo mappedTrackInfo = Assertions.checkNotNull(trackSelector.getCurrentMappedTrackInfo());
        TrackSelectionDialog trackSelectionDialog = new TrackSelectionDialog();
        DefaultTrackSelector.Parameters parameters = trackSelector.getParameters();
        trackSelectionDialog.init(
                    /* titleId= */ trackID,
                    mappedTrackInfo,
                    /* initialParameters = */ parameters,
                    /* allowAdaptiveSelections =*/ true,
                    /* allowMultipleOverrides= */ false,
                    /* onClickListener= */ (dialog, which) -> {
                        DefaultTrackSelector.ParametersBuilder builder = parameters.buildUpon();
                        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
                            builder.clearSelectionOverrides(rendererIndex).setRendererDisabled(rendererIndex, trackSelectionDialog.getIsDisabled(rendererIndex));
                            List<SelectionOverride> overrides = trackSelectionDialog.getOverrides(rendererIndex);
                            if (!overrides.isEmpty()) {
                                Log.d(TAG, "override: " + new Gson().toJson(overrides.get(0)));
                                builder.setSelectionOverride(
                                        rendererIndex,
                                        mappedTrackInfo.getTrackGroups(rendererIndex),
                                        overrides.get(0));
                            }
                        }
                        trackSelector.setParameters(builder);
                    },
                    onDismissListener,
                    trackType);

        return trackSelectionDialog;
    }
//
//    /**
//     * Creates a dialog for given {@link MappedTrackInfo} and {@link DefaultTrackSelector.Parameters}.
//     *
//     * @param titleId The resource id of the dialog title.
//     * @param mappedTrackInfo The {@link MappedTrackInfo} to display.
//     * @param initialParameters The {@link DefaultTrackSelector.Parameters} describing the initial
//     *     track selection.
//     * @param allowAdaptiveSelections Whether adaptive selections (consisting of more than one track)
//     *     can be made.
//     * @param allowMultipleOverrides Whether tracks from multiple track groups can be selected.
//     * @param onClickListener {@link DialogInterface.OnClickListener} called when tracks are selected.
//     * @param onDismissListener {@link DialogInterface.OnDismissListener} called when the dialog is
//     *     dismissed.
//     */
//    public static TrackSelectionDialog createForMappedTrackInfoAndParameters(
//            int titleId,
//            MappedTrackInfo mappedTrackInfo,
//            DefaultTrackSelector.Parameters initialParameters,
//            boolean allowAdaptiveSelections,
//            boolean allowMultipleOverrides,
//            DialogInterface.OnClickListener onClickListener,
//            DialogInterface.OnDismissListener onDismissListener
//            int trackType) {
//        TrackSelectionDialog trackSelectionDialog = new TrackSelectionDialog();
//        trackSelectionDialog.init(
//                titleId,
//                mappedTrackInfo,
//                initialParameters,
//                allowAdaptiveSelections,
//                allowMultipleOverrides,
//                onClickListener,
//                onDismissListener,
//                trackType);
//        return trackSelectionDialog;
//    }

    public TrackSelectionDialog() {
        tabFragments = new SparseArray<>();
        tabTrackTypes = new ArrayList<>();
        // Retain instance across activity re-creation to prevent losing access to init data.
        setRetainInstance(true);
    }

    private void init(
            int titleId,
            MappedTrackInfo mappedTrackInfo,
            DefaultTrackSelector.Parameters initialParameters,
            boolean allowAdaptiveSelections,
            boolean allowMultipleOverrides,
            DialogInterface.OnClickListener onClickListener,
            DialogInterface.OnDismissListener onDismissListener,
            int track_Type) {
        this.onClickListener = onClickListener;
        this.onDismissListener = onDismissListener;
        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
            int trackType = mappedTrackInfo.getRendererType(rendererIndex);
            if (trackType == track_Type){
                TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
                Log.d(TAG, "init: " + trackGroupArray.length);
                TrackSelectionViewFragment tabFragment = new TrackSelectionViewFragment();
                tabFragment.init(
                        mappedTrackInfo,
                        rendererIndex,
                        initialParameters.getRendererDisabled(rendererIndex),
                        initialParameters.getSelectionOverride(rendererIndex, trackGroupArray),
                        allowAdaptiveSelections,
                        allowMultipleOverrides);
                tabFragments.put(rendererIndex, tabFragment);
                tabTrackTypes.add(trackType);
                this.titleId = titleId;
                break;
            }

        }
    }

    /**
     * Returns whether a renderer is disabled.
     *
     * @param rendererIndex Renderer index.
     * @return Whether the renderer is disabled.
     */
    public boolean getIsDisabled(int rendererIndex) {
        TrackSelectionViewFragment rendererView = tabFragments.get(rendererIndex);
        return rendererView != null && rendererView.isDisabled;
    }

    /**
     * Returns the list of selected track selection overrides for the specified renderer. There will
     * be at most one override for each track group.
     *
     * @param rendererIndex Renderer index.
     * @return The list of track selection overrides for this renderer.
     */
    public List<SelectionOverride> getOverrides(int rendererIndex) {
        TrackSelectionViewFragment rendererView = tabFragments.get(rendererIndex);
        return rendererView == null ? Collections.emptyList() : rendererView.overrides;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // We need to own the view to let tab layout work correctly on all API levels. We can't use
        // AlertDialog because it owns the view itself, so we use AppCompatDialog instead, themed using
        // the AlertDialog theme overlay with force-enabled title.
        AppCompatDialog dialog =
                new AppCompatDialog(getActivity(), R.style.TrackSelectionDialogThemeOverlay);
        dialog.setTitle(titleId);
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onDismissListener.onDismiss(dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false);
        TabLayout tabLayout = dialogView.findViewById(R.id.track_selection_dialog_tab_layout);
        ViewPager viewPager = dialogView.findViewById(R.id.track_selection_dialog_view_pager);
        Button cancelButton = dialogView.findViewById(R.id.track_selection_dialog_cancel_button);
        Button okButton = dialogView.findViewById(R.id.track_selection_dialog_ok_button);
        viewPager.setAdapter(new FragmentAdapter(getChildFragmentManager(), requireContext(), tabFragments, tabTrackTypes));
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setVisibility(tabFragments.size() > 1 ? View.VISIBLE : View.GONE);
        cancelButton.setOnClickListener(view -> dismiss());
        okButton.setOnClickListener(
                view -> {
                    onClickListener.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
                    dismiss();
                });
        return dialogView;
    }

    public static boolean showTabForRenderer(MappedTrackInfo mappedTrackInfo, int rendererIndex) {
        TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
        if (trackGroupArray.length == 0) {
            return false;
        }
        int trackType = mappedTrackInfo.getRendererType(rendererIndex);
        return isSupportedTrackType(trackType);
    }



    private static boolean isSupportedTrackType(int trackType) {
        switch (trackType) {
            case C.TRACK_TYPE_AUDIO:
            case C.TRACK_TYPE_TEXT:
                return true;
            default:
                return false;
        }
    }

}
