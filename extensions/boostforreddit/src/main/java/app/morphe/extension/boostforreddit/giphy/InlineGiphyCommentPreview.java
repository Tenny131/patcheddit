package app.morphe.extension.boostforreddit.giphy;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InlineGiphyCommentPreview {
    private static final String PREVIEW_TAG = "morphe_boost_inline_giphy_preview";

    private static final Pattern[] GIPHY_PATTERNS = new Pattern[] {
            Pattern.compile("!\\[gif\\]\\(giphy\\|([A-Za-z0-9]+)\\)"),
            Pattern.compile("giphy\\|([A-Za-z0-9]+)"),
            Pattern.compile("media\\.giphy\\.com/media/([A-Za-z0-9]+)/"),
            Pattern.compile("giphy\\.com/gifs/(?:[^\\s\"'<>/]+-)?([A-Za-z0-9]+)(?:[\\s\"'<>/?#]|$)")
    };

    private InlineGiphyCommentPreview() {
    }

    public static void cleanCommentHtml(Object commentModel) {
        try {
            if (commentModel == null) return;

            String raw = firstNonEmpty(
                    callStringMethod(commentModel, "S0"),
                    callStringMethod(commentModel, "T0"),
                    findFirstStringValue(commentModel)
            );

            if (extractGiphyId(raw) == null) return;

            replaceGiphyStringFields(commentModel);
        } catch (Throwable ignored) {
        }
    }

    public static void bind(Object holder, Object commentModel, Object glideRequestManager) {
        try {
            if (holder == null || commentModel == null) return;

            String source = firstNonEmpty(
                    callStringMethod(commentModel, "S0"),
                    callStringMethod(commentModel, "T0"),
                    findFirstStringValue(commentModel)
            );

            final String giphyId = extractGiphyId(source);
            View itemView = getItemView(holder);

            if (!(itemView instanceof ViewGroup)) return;

            removeExistingPreview((ViewGroup) itemView);

            if (giphyId == null || giphyId.length() == 0) return;

            final Context context = itemView.getContext();
            final String gifUrl = "https://media.giphy.com/media/" + giphyId + "/giphy.gif";
            final String sourceUrl = "https://giphy.com/gifs/" + giphyId;

            LinearLayout container = new LinearLayout(context);
            container.setTag(PREVIEW_TAG);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(0, dp(context, 6), 0, dp(context, 4));

            ImageView imageView = new ImageView(context);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(context, 170)
            );

            container.addView(imageView, imageParams);

            TextView label = new TextView(context);
            label.setText("Source: " + sourceUrl);
            label.setTextSize(11f);
            label.setAlpha(0.65f);
            label.setSingleLine(true);

            container.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        view.getContext().startActivity(intent);
                    } catch (Throwable ignored) {
                    }
                }
            });

            insertBelowCommentText(holder, (ViewGroup) itemView, container);
            loadWithGlide(context, glideRequestManager, gifUrl, imageView);
            syncWithCommentState(holder);
        } catch (Throwable ignored) {
        }
    }

    public static void syncWithCommentState(Object holder) {
        try {
            View itemView = getItemView(holder);
            if (!(itemView instanceof ViewGroup)) return;

            View preview = findPreview((ViewGroup) itemView);
            if (preview == null) return;

            View commentText = findCommentTextView(holder);
            if (commentText == null) {
                preview.setVisibility(View.VISIBLE);
                return;
            }

            preview.setVisibility(commentText.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        } catch (Throwable ignored) {
        }
    }

    private static void insertBelowCommentText(Object holder, ViewGroup itemView, View preview) {
        View commentText = findCommentTextView(holder);

        if (commentText != null && commentText.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) commentText.getParent();
            int index = parent.indexOfChild(commentText);
            if (index >= 0) {
                parent.addView(preview, Math.min(index + 1, parent.getChildCount()));
                return;
            }
        }

        itemView.addView(preview);
    }

    private static View findCommentTextView(Object holder) {
        View direct = getViewField(holder, "commentTv");
        if (direct != null) return direct;

        Class<?> cls = holder.getClass();
        while (cls != null) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                try {
                    if (!View.class.isAssignableFrom(field.getType())) continue;

                    String name = field.getName().toLowerCase();
                    if (!name.contains("comment") && !name.contains("body") && !name.contains("text")) continue;

                    field.setAccessible(true);
                    Object value = field.get(holder);
                    if (value instanceof View) return (View) value;
                } catch (Throwable ignored) {
                }
            }
            cls = cls.getSuperclass();
        }

        return null;
    }

    private static View getViewField(Object holder, String name) {
        try {
            Field field = findField(holder.getClass(), name);
            if (field == null) return null;

            field.setAccessible(true);
            Object value = field.get(holder);
            return value instanceof View ? (View) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static View getItemView(Object holder) {
        return getViewField(holder, "itemView");
    }

    private static void removeExistingPreview(ViewGroup root) {
        View preview = findPreview(root);
        if (preview != null && preview.getParent() instanceof ViewGroup) {
            ((ViewGroup) preview.getParent()).removeView(preview);
        }
    }

    private static View findPreview(ViewGroup root) {
        Object tag = root.getTag();
        if (PREVIEW_TAG.equals(tag)) return root;

        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (PREVIEW_TAG.equals(child.getTag())) return child;

            if (child instanceof ViewGroup) {
                View nested = findPreview((ViewGroup) child);
                if (nested != null) return nested;
            }
        }

        return null;
    }

    private static void loadWithGlide(Context context, Object glideRequestManager, String url, ImageView imageView) {
        try {
            Object requestManager = glideRequestManager;

            if (requestManager == null) {
                Class<?> glideClass = Class.forName("com.bumptech.glide.Glide");
                Method with = glideClass.getMethod("with", Context.class);
                requestManager = with.invoke(null, context);
            }

            Object requestBuilder = invokeLoad(requestManager, url);
            if (requestBuilder == null) return;

            invokeInto(requestBuilder, imageView);
        } catch (Throwable ignored) {
        }
    }

    private static Object invokeLoad(Object requestManager, String url) {
        try {
            Method[] methods = requestManager.getClass().getMethods();
            for (Method method : methods) {
                if (!"load".equals(method.getName())) continue;
                if (method.getParameterTypes().length != 1) continue;

                Class<?> parameter = method.getParameterTypes()[0];
                if (parameter == String.class || parameter == Object.class || CharSequence.class.isAssignableFrom(parameter)) {
                    return method.invoke(requestManager, url);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static void invokeInto(Object requestBuilder, ImageView imageView) {
        try {
            Method[] methods = requestBuilder.getClass().getMethods();
            for (Method method : methods) {
                if (!"into".equals(method.getName())) continue;
                if (method.getParameterTypes().length != 1) continue;

                Class<?> parameter = method.getParameterTypes()[0];
                if (parameter.isAssignableFrom(ImageView.class) || parameter == ImageView.class) {
                    method.invoke(requestBuilder, imageView);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static String extractGiphyId(String value) {
        if (value == null) return null;

        for (Pattern pattern : GIPHY_PATTERNS) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    private static void replaceGiphyStringFields(Object target) {
        Class<?> cls = target.getClass();

        while (cls != null) {
            Field[] fields = cls.getDeclaredFields();

            for (Field field : fields) {
                try {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers)) continue;
                    if (field.getType() != String.class) continue;

                    field.setAccessible(true);

                    Object value = field.get(target);
                    if (!(value instanceof String)) continue;

                    String oldValue = (String) value;
                    if (extractGiphyId(oldValue) == null) continue;

                    String newValue = removeGiphyText(oldValue);
                    field.set(target, newValue);
                } catch (Throwable ignored) {
                }
            }

            cls = cls.getSuperclass();
        }
    }

    private static String removeGiphyText(String value) {
        if (value == null) return null;

        return value
                .replaceAll("!\\[gif\\]\\(giphy\\|[A-Za-z0-9]+\\)", "")
                .replaceAll("https?://(?:www\\.)?giphy\\.com/gifs/\\S+", "")
                .replaceAll("https?://media\\.giphy\\.com/media/[A-Za-z0-9]+/giphy\\.(?:gif|mp4)", "")
                .trim();
    }

    private static String findFirstStringValue(Object target) {
        Class<?> cls = target.getClass();

        while (cls != null) {
            Field[] fields = cls.getDeclaredFields();

            for (Field field : fields) {
                try {
                    if (field.getType() != String.class) continue;
                    field.setAccessible(true);

                    Object value = field.get(target);
                    if (value instanceof String && ((String) value).length() > 0) {
                        return (String) value;
                    }
                } catch (Throwable ignored) {
                }
            }

            cls = cls.getSuperclass();
        }

        return null;
    }

    private static String callStringMethod(Object target, String name) {
        try {
            Method method = findMethod(target.getClass(), name);
            if (method == null) return null;

            method.setAccessible(true);
            Object value = method.invoke(target);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> cls, String name) {
        while (cls != null) {
            try {
                Method method = cls.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                cls = cls.getSuperclass();
            }
        }

        return null;
    }

    private static Field findField(Class<?> cls, String name) {
        while (cls != null) {
            try {
                Field field = cls.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                cls = cls.getSuperclass();
            }
        }

        return null;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return null;

        for (String value : values) {
            if (value != null && value.length() > 0) return value;
        }

        return null;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
