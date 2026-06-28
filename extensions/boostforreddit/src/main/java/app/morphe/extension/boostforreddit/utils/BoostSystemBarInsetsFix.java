/*
 * Modifications Copyright 2026 brealorg.
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.boostforreddit.utils;

import android.app.Activity;
import android.app.Application;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import java.util.WeakHashMap;

/**
 * @noinspection unused
 */
public final class BoostSystemBarInsetsFix {
    private static final int TARGET_SDK_EDGE_TO_EDGE = 35;
    private static final String MARKER = "MORPHE_BOOST_NAV_BAR_INSETS_FIX_V6";

    private static final WeakHashMap<Application, Boolean> INSTALLED = new WeakHashMap<>();
    private static final WeakHashMap<View, Padding> ORIGINAL_PADDING = new WeakHashMap<>();
    private static final WeakHashMap<View, Boolean> WATCHERS = new WeakHashMap<>();

    private BoostSystemBarInsetsFix() {
    }

    public static void install(final Application application) {
        try {
            if (application == null || INSTALLED.containsKey(application)) return;
            INSTALLED.put(application, Boolean.TRUE);

            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(final Activity activity, Bundle savedInstanceState) {
                    scheduleMainInsets(activity);
                }

                @Override
                public void onActivityResumed(final Activity activity) {
                    scheduleMainInsets(activity);
                }

                @Override public void onActivityStarted(Activity activity) {}
                @Override public void onActivityPaused(Activity activity) {}
                @Override public void onActivityStopped(Activity activity) {}
                @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
                @Override public void onActivityDestroyed(Activity activity) {}
            });
        } catch (Throwable ignored) {
        }
    }

    public static void applyMediaInsets(Activity activity) {
        try {
            if (!shouldApply(activity)) return;

            View bottomBar = findViewByName(activity, "bottom_bar");
            if (bottomBar != null) {
                applyBottomInsetPadding(bottomBar, false);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void applyMainInsets(Activity activity) {
        scheduleMainInsets(activity);
    }

    private static void scheduleMainInsets(final Activity activity) {
        try {
            if (!shouldApply(activity)) return;

            final View decor = activity.getWindow().getDecorView();
            if (decor == null) return;

            installLayoutWatcher(activity, decor);

            Runnable apply = new Runnable() {
                @Override
                public void run() {
                    try {
                        applyMainInsetsNow(activity);
                    } catch (Throwable ignored) {
                    }
                }
            };

            decor.post(apply);
            decor.postDelayed(apply, 100L);
            decor.postDelayed(apply, 350L);
            decor.postDelayed(apply, 1000L);
            decor.postDelayed(apply, 2000L);
        } catch (Throwable ignored) {
        }
    }

    private static void installLayoutWatcher(final Activity activity, View decor) {
        if (WATCHERS.containsKey(decor)) return;
        WATCHERS.put(decor, Boolean.TRUE);

        decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    applyMainInsetsNow(activity);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static void applyMainInsetsNow(Activity activity) {
        View drawerRecycler = findViewByName(activity, "material_drawer_recycler_view");
        if (drawerRecycler != null) {
            applyBottomInsetPadding(drawerRecycler, true);
            nudgeBottomVisibleChildAboveNavigationBar(drawerRecycler);
        }
    }

    private static boolean shouldApply(Activity activity) {
        return activity != null
                && Build.VERSION.SDK_INT >= 23
                && activity.getApplicationInfo() != null
                && activity.getApplicationInfo().targetSdkVersion >= TARGET_SDK_EDGE_TO_EDGE;
    }

    private static View findViewByName(Activity activity, String name) {
        int id = activity.getResources().getIdentifier(name, "id", activity.getPackageName());
        if (id == 0) return null;
        return activity.findViewById(id);
    }

    private static void applyBottomInsetPadding(final View view, final boolean allowScrollIntoPadding) {
        if (view == null) return;

        saveOriginalPadding(view);

        if (allowScrollIntoPadding && view instanceof ViewGroup) {
            ((ViewGroup) view).setClipToPadding(false);
        }

        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                try {
                    applyBottomInsetPaddingNow(v, insets);
                } catch (Throwable ignored) {
                }
                return insets;
            }
        });

        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    applyBottomInsetPaddingNow(view, getBestCurrentInsets(view));
                    view.requestApplyInsets();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static void saveOriginalPadding(View view) {
        if (!ORIGINAL_PADDING.containsKey(view)) {
            ORIGINAL_PADDING.put(view, new Padding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            ));
        }
    }

    private static void applyBottomInsetPaddingNow(View view, WindowInsets insets) {
        if (view == null) return;

        saveOriginalPadding(view);

        Padding original = ORIGINAL_PADDING.get(view);
        if (original == null) return;

        int bottomInset = getEffectiveNavigationBottomInset(view, insets);

        view.setPadding(
                original.left,
                original.top,
                original.right,
                original.bottom + Math.max(0, bottomInset)
        );

        view.requestLayout();
        view.invalidate();
    }

    private static void nudgeBottomVisibleChildAboveNavigationBar(final View view) {
        if (!(view instanceof ViewGroup)) return;

        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (view.canScrollVertically(1)) return;

                    ViewGroup group = (ViewGroup) view;
                    int childCount = group.getChildCount();
                    if (childCount <= 0) return;

                    View lastChild = group.getChildAt(childCount - 1);
                    if (lastChild == null) return;

                    int bottomInset = getEffectiveNavigationBottomInset(view, getBestCurrentInsets(view));
                    if (bottomInset <= 0) return;

                    int targetBottom = view.getHeight() - bottomInset;
                    int overlap = lastChild.getBottom() - targetBottom;

                    if (overlap > 0) {
                        view.scrollBy(0, overlap);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static int getEffectiveNavigationBottomInset(View view, WindowInsets insets) {
        int bottomInset = getNavigationBottomInset(insets);
        if (bottomInset <= 0) {
            bottomInset = getNavigationBottomInset(getBestCurrentInsets(view));
        }

        if (bottomInset <= 0 && isThreeButtonNavigation(view)) {
            bottomInset = getNavigationBarHeight(view);
        }

        return bottomInset;
    }

    private static WindowInsets getBestCurrentInsets(View view) {
        if (view == null || Build.VERSION.SDK_INT < 23) return null;

        WindowInsets insets = view.getRootWindowInsets();
        if (insets != null) return insets;

        View root = view.getRootView();
        if (root != null) {
            return root.getRootWindowInsets();
        }

        return null;
    }

    private static int getNavigationBottomInset(WindowInsets insets) {
        if (insets == null) return 0;

        if (Build.VERSION.SDK_INT >= 30) {
            Insets navigationInsets = insets.getInsets(WindowInsets.Type.navigationBars());
            return navigationInsets == null ? 0 : navigationInsets.bottom;
        }

        return insets.getSystemWindowInsetBottom();
    }

    private static boolean isThreeButtonNavigation(View view) {
        try {
            return android.provider.Settings.Secure.getInt(
                    view.getContext().getContentResolver(),
                    "navigation_mode",
                    -1
            ) == 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int getNavigationBarHeight(View view) {
        try {
            int id = view.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (id > 0) {
                return view.getResources().getDimensionPixelSize(id);
            }
        } catch (Throwable ignored) {
        }

        return 0;
    }

    private static final class Padding {
        final int left;
        final int top;
        final int right;
        final int bottom;

        Padding(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
