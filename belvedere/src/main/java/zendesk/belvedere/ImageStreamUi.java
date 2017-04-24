package zendesk.belvedere;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import zendesk.belvedere.ui.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class ImageStreamUi extends PopupWindow implements ImageStreamMvp.View, ImageStreamAdapter.Delegate {

    static ImageStreamUi show(Activity activity, ViewGroup parent, ImageStream popupBackend, BelvedereUi.UiConfig config) {
        final View v = LayoutInflater.from(activity).inflate(R.layout.activity_image_stream, parent, false);
        final ImageStreamUi attachmentPicker = new ImageStreamUi(activity, v, popupBackend, config);
        attachmentPicker.showAtLocation(parent, Gravity.TOP, 0, 0);
        return attachmentPicker;
    }

    private final ImageStream popupBackend;
    private final ImageStreamMvp.Presenter presenter;
    private final ImageStreamDataSource dataSource;

    private View bottomSheet, dismissArea, toolbarContainer;
    private FloatingActionMenu floatingActionMenu;
    private RecyclerView imageList;
    private Toolbar toolbar;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ImageStreamAdapter imageStreamAdapter;
    private Activity activity;

    ImageStreamUi(Activity activity, View view, ImageStream popupBackend, BelvedereUi.UiConfig uiConfig) {
        super(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, false);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        setFocusable(true);
        setTouchable(true);
        setBackgroundDrawable(new BitmapDrawable()); // need this for dismiss onBackPressed :/
        setOutsideTouchable(true);
        bindViews(view);

        this.popupBackend = popupBackend;
        this.activity = activity;
        this.dataSource = new ImageStreamDataSource();

        final PermissionStorage preferences = new PermissionStorage(view.getContext());
        final ImageStreamMvp.Model model = new ImageStreamModel(view.getContext(), uiConfig, preferences);

        this.presenter = new ImageStreamPresenter(model, this, dataSource);
        presenter.init();
    }

    @Override
    public void initUiComponents() {
        initToolbar();
        initBottomSheet();
        initGesturePassThrough(activity);
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
    public void showImageStream(List<MediaResult> images, List<MediaResult> selectedImages, boolean showCamera) {
        showKeyboard(popupBackend.getKeyboardHelper().inputTrap);

        final ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = MATCH_PARENT;
        bottomSheet.setLayoutParams(layoutParams);

        final int columns = bottomSheet.getContext().getResources().getBoolean(R.bool.bottom_sheet_portrait) ? 2 : 3;

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

        if(showCamera){
            dataSource.addStaticItem(ImageStreamItems.forCameraSquare(this));
        }

        initRecycler(adapter, staggeredGridLayoutManager);

        dataSource.setItemsSelected(selectedUris);
    }


    @Override
    public void showDocumentMenuItem(boolean visible) {
        if (floatingActionMenu != null) {
            floatingActionMenu.addMenuItem(R.drawable.ic_file, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    presenter.openGallery();
                }
            });
        }
    }

    @Override
    public void showGooglePhotosMenuItem(boolean visible) {
        if (floatingActionMenu != null) {
            floatingActionMenu.addMenuItem(R.drawable.ic_collections, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    presenter.openGooglePhotos();
                }
            });
        }
    }

    @Override
    public void openMediaIntent(MediaIntent mediaIntent) {
        mediaIntent.open(popupBackend);
    }

    @Override
    public void openCamera() {
        popupBackend.getKeyboardHelper().hideKeyboard();
        presenter.openCamera();
    }

    @Override
    public void openGallery() {
        popupBackend.getKeyboardHelper().hideKeyboard();
        presenter.openGallery();
    }

    @Override
    public void setSelected(MediaResult uri, boolean isSelected, int position) {
        imageStreamAdapter.notifyItemChanged(position);
        final List<MediaResult> selectedItems = presenter.setItemSelected(uri, isSelected);
        popupBackend.notifyImageSelected(selectedItems, true);
    }

    @Override
    public void updateToolbarTitle(int selectedImages) {
        if(selectedImages > 0) {
            final String title = activity.getString(R.string.belvedere_image_stream_title);
            toolbar.setTitle(String.format(Locale.US, "%s (%s)", title, selectedImages));
        } else {
            toolbar.setTitle(R.string.belvedere_image_stream_title);
        }
    }

    private void bindViews(View view) {
        this.bottomSheet = view.findViewById(R.id.bottom_sheet);
        this.dismissArea = view.findViewById(R.id.dismiss_area);
        this.imageList = (RecyclerView) view.findViewById(R.id.image_list);
        this.toolbar = (Toolbar) view.findViewById(R.id.image_stream_toolbar);
        this.toolbarContainer = view.findViewById(R.id.image_stream_toolbar_container);
        this.floatingActionMenu = (FloatingActionMenu) view.findViewById(R.id.floating_action_menu);
    }

    private void initToolbar() {
        toolbar.setNavigationIcon(R.drawable.belvedere_ic_close);
        toolbar.setBackgroundColor(Color.WHITE);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });


        CoordinatorLayout.LayoutParams layoutParams = null;
        if(toolbar.getLayoutParams() instanceof  CoordinatorLayout.LayoutParams) {
            layoutParams = (CoordinatorLayout.LayoutParams) toolbar.getLayoutParams();
        } else if(toolbarContainer.getLayoutParams() instanceof CoordinatorLayout.LayoutParams){
            layoutParams = (CoordinatorLayout.LayoutParams) toolbarContainer.getLayoutParams();
        }

        if(layoutParams != null) {
            layoutParams.setBehavior(new ToolbarBehavior());
        }
    }

    private void initRecycler(ImageStreamAdapter adapter, RecyclerView.LayoutManager layoutManager) {
        this.imageStreamAdapter = adapter;
        imageList.setItemAnimator(null);
        imageList.setHasFixedSize(true);
        imageList.setDrawingCacheEnabled(true);
        imageList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        imageList.setAdapter(adapter);
        imageList.setLayoutManager(layoutManager);
    }

    private void initBottomSheet() {
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
                // intentionally empty
            }
        });

        Utils.showToolbar(getContentView(), false);

        final KeyboardHelper keyboardHelper = popupBackend.getKeyboardHelper();
        bottomSheetBehavior.setPeekHeight(bottomSheet.getPaddingTop() + keyboardHelper.getKeyboardHeight());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        keyboardHelper.setKeyboardHeightListener(new KeyboardHelper.SizeListener() {
            @Override
            public void onSizeChanged(int keyboardHeight) {
                if(keyboardHeight != bottomSheetBehavior.getPeekHeight()) {
                    bottomSheetBehavior.setPeekHeight(bottomSheet.getPaddingTop() + keyboardHelper.getKeyboardHeight());
                }
            }
        });

        imageList.setClickable(true);
        bottomSheet.setVisibility(View.VISIBLE);
    }

    private void initGesturePassThrough(final Activity activity) {
        final GestureDetectorCompat gestureDetectorCompat =
                new GestureDetectorCompat(activity, new PassThroughGestureListener());
        dismissArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                activity.dispatchTouchEvent(event);
                if(gestureDetectorCompat.onTouchEvent(event)){
                    dismiss();
                }
                return true;
            }
        });
    }

    @Override
    public void dismiss() {
        super.dismiss();
        popupBackend.setImageStreamUi(null);

        tintStatusBar(0);
        popupBackend.notifyScrollListener(0,0,0);

        popupBackend.notifyDismissed();
    }

    private class PassThroughGestureListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            dismiss();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }
    }

    private void tintStatusBar(float scrollOffset) {

        int statusBarColor = toolbar.getResources().getColor(R.color.image_stream_status_bar_color);
        int colorPrimaryDark = Utils.getThemeColor(toolbar.getContext(), android.R.attr.colorPrimaryDark);
        boolean fullyExpanded = scrollOffset == 1.f;
        final Window window = activity.getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(fullyExpanded) {
                if (window.getStatusBarColor() == colorPrimaryDark) {
                    final ValueAnimator animation = ValueAnimator.ofObject(new ArgbEvaluator(), colorPrimaryDark, statusBarColor);
                    animation.setDuration(100);
                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            window.setStatusBarColor((Integer) animation.getAnimatedValue());
                        }

                    });
                    animation.start();
                }
            } else {
                window.setStatusBarColor(colorPrimaryDark);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = window.getDecorView();
            if(fullyExpanded) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decor.setSystemUiVisibility(0);
            }
        }
    }

    private class ToolbarBehavior extends CoordinatorLayout.Behavior<View> {

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
            return dependency.getId() == R.id.bottom_sheet;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
            int scrollArea = parent.getHeight() - bottomSheetBehavior.getPeekHeight();
            float scrollPosition = (parent.getHeight() - dependency.getY() - bottomSheetBehavior.getPeekHeight()) / scrollArea;

            notifyListener(parent.getHeight(), scrollArea, scrollPosition);
            animateToolbarShiftIn(scrollArea, scrollPosition, ViewCompat.getMinimumHeight(toolbar), child);
            //animateToolbarFadeIn(scrollPosition); // TODO add config option

            return true;
        }

        private void animateToolbarShiftIn(int scrollArea, float scrollPosition, int toolbarHeight, View toolbar) {
            float posInScrollArea = (scrollPosition * scrollArea);

            if(scrollArea - posInScrollArea <= toolbarHeight) {
                Utils.showToolbar(getContentView(), true);
                ViewCompat.setY(toolbar, scrollArea - posInScrollArea);

            } else {
                Utils.showToolbar(getContentView(), false);
            }

            tintStatusBar(scrollPosition);
        }

        private void animateToolbarFadeIn(float scrollPosition) {
            float offset = 0.6f;
            if (scrollPosition >= offset) {
                ViewCompat.setAlpha(toolbar, 1f - (1f - scrollPosition) / (1f - offset));
                Utils.showToolbar(getContentView(), true);
            } else {
                Utils.showToolbar(getContentView(), false);
            }

            tintStatusBar(scrollPosition);
        }

        private void notifyListener(int height, int scrollArea, float scrollPosition) {
            if(scrollPosition >= 0) {
                popupBackend.notifyScrollListener(height, scrollArea, scrollPosition);
            }
        }
    }
}