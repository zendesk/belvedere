package zendesk.belvedere;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;

import zendesk.belvedere.ui.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ImageStreamPopup extends PopupWindow implements ImageStreamMvp.View, ImageStreamAdapter.Delegate {

    public interface Listener {
        void onDismissed();
        void onImageSelected(List<MediaResult> mediaResults, boolean replace);
    }

    static ImageStreamPopup show(Context context, ViewGroup parent, PopupBackend popupBackend, BelvedereUi.UiConfig config) {

        final View v = LayoutInflater.from(context).inflate(R.layout.activity_image_stream, parent, false);
        final ImageStreamPopup attachmentPicker = new ImageStreamPopup(v, popupBackend, config);
        attachmentPicker.showAtLocation(parent, Gravity.TOP, 0, 0);

        return attachmentPicker;
    }

    private final PopupBackend popupBackend;
    private final ImageStreamMvp.Presenter presenter;
    private final ImageStreamDataSource dataSource;

    private View bottomSheet, dismissArea;
    private RecyclerView imageList;
    private Toolbar toolbar;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ImageStreamAdapter imageStreamAdapter;

    ImageStreamPopup(View view, PopupBackend popupBackend, BelvedereUi.UiConfig uiConfig) {
        super(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, false);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        setFocusable(true);
        setTouchable(true);
        setBackgroundDrawable(new BitmapDrawable()); // need this for dismiss onBackPressed :/
        setOutsideTouchable(true);
        bindViews(view);

        this.popupBackend = popupBackend;
        this.dataSource = new ImageStreamDataSource();
        final PermissionStorage preferences = new PermissionStorage(view.getContext());
        final ImageStreamMvp.Model model = new ImageStreamModel(view.getContext(), uiConfig, preferences);

        this.presenter = new ImageStreamPresenter(model, this, dataSource);
        presenter.init();
    }

    @Override
    public void initUiComponents() {
        initToolbar();
        initBottomSheet(false);
    }

    private void showKeyboard(final EditText editText) {
        editText.post(new Runnable() {
            @Override
            public void run() {
                if(editText.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
                }
            }
        });
    }

    @Override
    public void showImageStream(List<Uri> images, List<MediaResult> selectedImages, boolean showCamera) {
        showKeyboard(popupBackend.getKeyboardHelper().inputTrap);

        final ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = MATCH_PARENT;
        bottomSheet.setLayoutParams(layoutParams);

        int columns = bottomSheet.getContext().getResources().getBoolean(R.bool.bottom_sheet_portrait) ? 2 : 3;

        final ImageStreamAdapter adapter = new ImageStreamAdapter(dataSource);
        final StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL);


        final Activity context = (Activity) getContentView().getContext();
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int width = size.x / columns;

        dataSource.initializeWithImages(ImageStreamItems.fromUris(images, this, bottomSheet.getContext(), width));

        final List<Uri> selectedUris = new ArrayList<>();
        for(MediaResult mediaResult : selectedImages) {
            selectedUris.add(mediaResult.getOriginalUri());
        }
        dataSource.setItemsSelected(selectedUris);

        if(showCamera){
            dataSource.addStaticItem(ImageStreamItems.forCameraSquare(this));
        }

        initRecycler(adapter, staggeredGridLayoutManager);
    }

    @Override
    public void showList(MediaIntent cameraIntent, MediaIntent documentIntent) {
        final ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = WRAP_CONTENT;
        bottomSheet.setLayoutParams(layoutParams);

        final ImageStreamAdapter adapter = new ImageStreamAdapter(dataSource);

        dataSource.addStaticItem(ImageStreamItems.forCameraList(this));
        dataSource.addStaticItem(ImageStreamItems.forDocumentList(this));

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(bottomSheet.getContext());
        initRecycler(adapter, linearLayoutManager);
    }

    @Override
    public void showDocumentMenuItem(boolean visible) {

    }

    @Override
    public void openMediaIntent(MediaIntent mediaIntent) {
        mediaIntent.open(popupBackend);
    }

    @Override
    public void finishWithoutResult() {
        dismiss();
    }

    @Override
    public void finishIfNothingIsLeft() {
        dismiss();
    }

    @Override
    public void openCamera() {
        presenter.openCamera();
    }

    @Override
    public void openGallery() {
        presenter.openGallery();
    }

    @Override
    public void setSelected(Uri uri, boolean b) {
        for(int i = 0, c = dataSource.getItemCount(); i < c; i++) {
            if(dataSource.getItemForPos(i) instanceof ImageStreamItems.StreamItemImage) {
                if(((ImageStreamItems.StreamItemImage)dataSource.getItemForPos(i)).getUri().equals(uri)){
                    imageStreamAdapter.notifyItemChanged(i);
                }
            }
        }

        presenter.setItemSelected(uri, b);
        if(popupBackend.getImListener() != null){
            final List<MediaResult> selectedItems = presenter.getSelectedItems();
            popupBackend.getImListener().onImageSelected(selectedItems, true);
        }
    }

    private void bindViews(View view) {
        this.bottomSheet = view.findViewById(R.id.bottom_sheet);
        this.dismissArea = view.findViewById(R.id.dismiss_area);
        this.imageList = (RecyclerView) view.findViewById(R.id.image_list);
        this.toolbar = (Toolbar) view.findViewById(R.id.image_stream_toolbar);
    }

    private void initToolbar() {
        toolbar.setNavigationIcon(R.drawable.belvedere_ic_close);
        toolbar.setTitle("Photo library");
        toolbar.setBackgroundColor(Color.WHITE);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void initRecycler(ImageStreamAdapter adapter, RecyclerView.LayoutManager layoutManager) {
        this.imageStreamAdapter = adapter;
        imageList.setItemAnimator(null);
        imageList.setHasFixedSize(true);
        imageList.setItemViewCacheSize(25);
        imageList.setDrawingCacheEnabled(true);
        imageList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        imageList.setAdapter(adapter);
        imageList.setLayoutManager(layoutManager);
    }

    private void initBottomSheet(boolean withAnimation) {
        ViewCompat.setElevation(imageList, bottomSheet.getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_elevation));

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        dismiss();
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float offset = 0.6f;
                if (slideOffset >= offset) {
                    ViewCompat.setAlpha(toolbar, 1f - (1f - slideOffset) / (1f - offset));
                    UiUtils.showToolbar(getContentView(), true);
                } else {
                    UiUtils.showToolbar(getContentView(), false);
                }
            }
        });

        UiUtils.showToolbar(getContentView(), false);

        final KeyboardHelper keyboardHelper = popupBackend.getKeyboardHelper();
        bottomSheetBehavior.setPeekHeight(bottomSheet.getPaddingTop() + keyboardHelper.getKeyboardHeight());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        keyboardHelper.setKeyboardHeightListener(new KeyboardHelper.SizeListener() {
            @Override
            public void onSizeChanged(int keyboardHeight) {
                if(keyboardHeight != bottomSheetBehavior.getPeekHeight()) {
                    System.out.println("==== update bottomsheet " + keyboardHeight);
                    bottomSheetBehavior.setPeekHeight(bottomSheet.getPaddingTop() + keyboardHelper.getKeyboardHeight());
                }
            }
        });

        dismissArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        bottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        imageList.setClickable(true);

        bottomSheet.setVisibility(View.VISIBLE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && withAnimation) {
            bottomSheet.post(new Runnable() {
                @Override
                public void run() {
                    final int cx = bottomSheet.getWidth() / 2;
                    final int cy = bottomSheet.getHeight() / 2;
                    float finalRadius = (float) Math.hypot(cx, cy);
                    final Animator anim = ViewAnimationUtils.createCircularReveal(bottomSheet, cx, cy, 0, finalRadius);
                    bottomSheet.setVisibility(View.VISIBLE);
                    anim.start();
                }
            });
        }else {
            bottomSheet.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        popupBackend.setImageStreamPopup(null);

        if(popupBackend.getImListener() != null) {
            popupBackend.getImListener().onDismissed();
        }
    }
}