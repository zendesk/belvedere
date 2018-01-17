package zendesk.belvedere;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityManager;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import zendesk.belvedere.ui.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

class ImageStreamUi extends PopupWindow implements ImageStreamMvp.View {

    static ImageStreamUi show(Activity activity, final ViewGroup parent, ImageStream popupBackend, BelvedereUi.UiConfig config) {
        final View v = LayoutInflater.from(activity).inflate(R.layout.belvedere_image_stream, parent, false);
        final ImageStreamUi attachmentPicker = new ImageStreamUi(activity, v, popupBackend, config);
        attachmentPicker.showAtLocation(parent, Gravity.TOP, 0, 0);
        return attachmentPicker;
    }

    private final ImageStreamPresenter presenter;
    private final ImageStreamAdapter adapter;
    private final List<Integer> touchableItemIds;

    private KeyboardHelper keyboardHelper;

    private View bottomSheet, dismissArea, toolbarContainer, toolbarCompatShadow;
    private FloatingActionMenu floatingActionMenu;
    private RecyclerView imageList;
    private Toolbar toolbar;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private Activity activity;

    private ImageStreamUi(Activity activity, View view, ImageStream imageStreamBackend, BelvedereUi.UiConfig uiConfig) {
        super(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, false);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        setFocusable(true);
        setTouchable(true);
        setBackgroundDrawable(new BitmapDrawable()); // need this for dismiss onBackPressed :/
        setOutsideTouchable(true);
        bindViews(view);

        this.activity = activity;
        this.adapter = new ImageStreamAdapter();
        this.keyboardHelper = imageStreamBackend.getKeyboardHelper();
        this.touchableItemIds = uiConfig.getTouchableElements();

        final ImageStreamMvp.Model model = new ImageStreamModel(view.getContext(), uiConfig);
        this.presenter = new ImageStreamPresenter(model, this, imageStreamBackend);

        presenter.init();
    }

    @Override
    public void initViews() {
        initRecycler(adapter);
        initToolbar();
        initBottomSheet();
        initGesturePassThrough(activity, touchableItemIds);
    }

    @Override
    public void showImageStream(List<MediaResult> images, List<MediaResult> selectedImages, boolean showCamera, ImageStreamAdapter.Listener listener) {
        if(!showFullScreen()) {
            KeyboardHelper.showKeyboard(keyboardHelper.getInputTrap());
        }

        final ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = MATCH_PARENT;
        bottomSheet.setLayoutParams(layoutParams);

        // Add camera item
        if(showCamera){
            adapter.addStaticItem(ImageStreamItems.forCameraSquare(listener));
        }

        // Add recent images
        adapter.initializeWithImages(ImageStreamItems.fromMediaResults(images, listener, bottomSheet.getContext()));

        // Mark selected images
        adapter.setItemsSelected(selectedImages);

        // Reload RecyclerView
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showDocumentMenuItem(View.OnClickListener onClickListener) {
        if (floatingActionMenu != null) {
            floatingActionMenu.addMenuItem(
                    R.drawable.belvedere_ic_file,
                    R.id.belvedere_fam_item_documents,
                    R.string.belvedere_fam_desc_open_gallery,
                    onClickListener);
        }
    }

    @Override
    public void showGooglePhotosMenuItem(View.OnClickListener onClickListener) {
        if (floatingActionMenu != null) {
            floatingActionMenu.addMenuItem(
                    R.drawable.belvedere_ic_collections,
                    R.id.belvedere_fam_item_google_photos,
                    R.string.belvedere_fam_desc_open_google_photos,
                    onClickListener);
        }
    }

    @Override
    public void openMediaIntent(MediaIntent mediaIntent, ImageStream imageStream) {
        mediaIntent.open(imageStream);
    }

    @Override
    public void showToast(@StringRes int textId) {
        Toast.makeText(activity, textId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateToolbarTitle(int selectedImages) {
        if(selectedImages > 0) {
            final String title = activity.getString(R.string.belvedere_image_stream_title);
            toolbar.setTitle(String.format(Locale.getDefault(), "%s (%d)", title, selectedImages));
        } else {
            toolbar.setTitle(R.string.belvedere_image_stream_title);
        }
    }

    private void bindViews(View view) {
        this.bottomSheet = view.findViewById(R.id.bottom_sheet);
        this.dismissArea = view.findViewById(R.id.dismiss_area);
        this.imageList = view.findViewById(R.id.image_list);
        this.toolbar = view.findViewById(R.id.image_stream_toolbar);
        this.toolbarContainer = view.findViewById(R.id.image_stream_toolbar_container);
        this.toolbarCompatShadow = view.findViewById(R.id.image_stream_compat_shadow);
        this.floatingActionMenu = view.findViewById(R.id.floating_action_menu);
    }

    private void initToolbar() {
        toolbar.setNavigationIcon(R.drawable.belvedere_ic_close);
        toolbar.setNavigationContentDescription(R.string.belvedere_toolbar_desc_collapse);
        toolbar.setBackgroundColor(Color.WHITE);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!showFullScreen()) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    dismiss();
                }
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            toolbarCompatShadow.setVisibility(View.VISIBLE);
        }

        CoordinatorLayout.LayoutParams layoutParams =
                (CoordinatorLayout.LayoutParams) toolbarContainer.getLayoutParams();

        if(layoutParams != null) {
            layoutParams.setBehavior(new ToolbarBehavior());
        }
    }

    private void initRecycler(ImageStreamAdapter adapter) {
        final int columns = bottomSheet.getContext().getResources().getInteger(R.integer.belvedere_image_stream_column_count);
        final StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL);

        imageList.setLayoutManager(staggeredGridLayoutManager);
        imageList.setHasFixedSize(true);
        imageList.setDrawingCacheEnabled(true);
        imageList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        final DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        defaultItemAnimator.setSupportsChangeAnimations(false);
        imageList.setItemAnimator(defaultItemAnimator);

        imageList.setAdapter(adapter);
    }

    private void initBottomSheet() {
        ViewCompat.setElevation(imageList, bottomSheet.getContext().getResources().getDimensionPixelSize(R.dimen.belvedere_bottom_sheet_elevation));

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

        if (!showFullScreen()) {
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
        } else {
            bottomSheetBehavior.setSkipCollapsed(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            KeyboardHelper.hideKeyboard(activity);
        }

        imageList.setClickable(true);
        bottomSheet.setVisibility(View.VISIBLE);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        tintStatusBar(0);
        presenter.dismiss();
    }

    private boolean showFullScreen() {
        final boolean isInExoticWindowMode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInExoticWindowMode = activity.isInMultiWindowMode() || activity.isInPictureInPictureMode();
        } else {
            isInExoticWindowMode = false;
        }

        final boolean hasHardwareKeyboard =
                activity.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

        final boolean hasAccessibilityEnabled;
        final AccessibilityManager manager = (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null) {
            int flags = AccessibilityServiceInfo.FEEDBACK_AUDIBLE | AccessibilityServiceInfo.FEEDBACK_SPOKEN
                    | AccessibilityServiceInfo.FEEDBACK_VISUAL | AccessibilityServiceInfo.FEEDBACK_BRAILLE
                    | AccessibilityServiceInfo.FEEDBACK_HAPTIC;
            final List<AccessibilityServiceInfo> enabledAccessibilityServiceList = manager.getEnabledAccessibilityServiceList(flags);
            hasAccessibilityEnabled = enabledAccessibilityServiceList != null && enabledAccessibilityServiceList.size() > 0;
        } else {
            hasAccessibilityEnabled = false;
        }

        return isInExoticWindowMode || hasHardwareKeyboard || hasAccessibilityEnabled;
    }

    private void initGesturePassThrough(final Activity activity, final List<Integer> touchableIds) {
        dismissArea.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                boolean dismiss = true;

                final int touchX = (int) event.getRawX();
                final int touchY = (int) event.getRawY();

                for(int id : touchableIds) {
                    View view = activity.findViewById(id);

                    if(view != null) {

                        final Rect viewRect = new Rect();
                        view.getGlobalVisibleRect(viewRect);

                        final boolean xMatch = touchX >= viewRect.left && touchX <= viewRect.right;
                        final boolean yMatch = touchY >= viewRect.top && touchY <= viewRect.bottom;

                        if(xMatch && yMatch) {
                            dismiss = false;
                            activity.dispatchTouchEvent(MotionEvent.obtain(event));
                            break;
                        }
                    }
                }

                if(dismiss) {
                    dismiss();
                }

                return true;
            }
        });
    }

    private void tintStatusBar(float scrollOffset) {

        int statusBarColor = toolbar.getResources().getColor(R.color.belvedere_image_stream_status_bar_color);
        int colorPrimaryDark = Utils.getThemeColor(toolbar.getContext(), R.attr.colorPrimaryDark);
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

            animateToolbarShiftIn(scrollArea, scrollPosition, ViewCompat.getMinimumHeight(toolbar), child);

            if(!showFullScreen()) {
                presenter.onImageStreamScrolled(parent.getHeight(), scrollArea, scrollPosition);
            }

            return true;
        }

        private void animateToolbarShiftIn(int scrollArea, float scrollPosition, int toolbarHeight, View toolbar) {
            float posInScrollArea = (scrollPosition * scrollArea);

            if(scrollArea - posInScrollArea <= toolbarHeight) {
                Utils.showToolbar(getContentView(), true);
                toolbar.setAlpha(1.f - ((scrollArea - posInScrollArea) / toolbarHeight));
                toolbar.setY(scrollArea - posInScrollArea);

            } else {
                Utils.showToolbar(getContentView(), false);
            }

            tintStatusBar(scrollPosition);
        }
    }
}