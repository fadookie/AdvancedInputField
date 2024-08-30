/** Author: Oleksandr Kucherenko, 2018-present */
package com.jeroenvanpienbroek.nativekeyboard;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.os.Build;
//import android.support.annotation.NonNull;
//import android.support.v4.util.Pair;
import android.util.Pair;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;
import java.util.Stack;

//import javax.annotation.Nullable;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

@SuppressWarnings("WeakerAccess")
public final class DebugViews {
    /**
     * Chunk of the long log line.
     */
    public static final int LOG_MSG_LIMIT = 200;
    /**
     * Initial matrix without transformations.
     */
    public static final Matrix EMPTY_MATRIX = new Matrix();

    /**
     * Log long message by chunks
     *
     * @param message message to log.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static int longDebug(final String tag, final String message) {
        int counter = 0;

        String msg = message;

        while (msg.length() > 0) {
            final int endOfLine = msg.indexOf("\n"); // -1, 0, 1
            final int breakPoint = Math.min(endOfLine < 0 ? LOG_MSG_LIMIT : endOfLine + 1, LOG_MSG_LIMIT);
            final int last = Math.min(msg.length(), breakPoint);
            final String out = String.format(Locale.US, "%02d: %s", counter, msg.substring(0, last));
            Log.d(tag, out);

            msg = msg.substring(last);
            counter++;
        }

        return counter;
    }

    /**
     * Print into log activity views hierarchy.
     */
    public static String logViewHierarchy(final Activity activity) {
        final View view = activity.findViewById(android.R.id.content);

        if (null == view)
            return "Activity [" + activity.getClass().getSimpleName() + "] is not initialized yet. ";

        return logViewHierarchy(view);
    }

    /**
     * Print into log view hierarchy.
     */
    public static String logViewHierarchy(final View root) {
        final StringBuilder output = new StringBuilder(8192).append("\n");
        final Resources r = root.getResources();
        final Stack<Pair<String, View>> stack = new Stack<Pair<String, View>>();
        stack.push(Pair.create("", root));

        while (!stack.empty()) {
            final Pair<String, View> p = stack.pop();
            final View v = p.second;
            final String prefix = p.first;

            final boolean isLastOnLevel = stack.empty() || !prefix.equals(stack.peek().first);
            final String graphics = "" + prefix + (isLastOnLevel ? "└── " : "├── ");

            final String className = v.getClass().getSimpleName();
            final String line = graphics + className + dumpProperties(r, v);

            output.append(line).append("\n");

            if (v instanceof ViewGroup) {
                final ViewGroup vg = (ViewGroup) v;
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    stack.push(Pair.create(prefix + (isLastOnLevel ? "    " : "│   "), vg.getChildAt(i)));
                }
            }
        }

        return output.toString();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String dumpProperties(final Resources r, final View v) {
        final StringBuilder sb = new StringBuilder();

        sb.append(" ").append("id=").append(v.getId()).append(resolveIdToName(r, v));

        switch (v.getVisibility()) {
            case VISIBLE:
                sb.append(", V--");
                break;
            case INVISIBLE:
                sb.append(", -I-");
                break;
            case GONE:
                sb.append(", --G");
                break;
            default:
                sb.append(", ---");
                break;
        }

        // transformation matrix exists, rotate/scale/skew/translate/
        if (!v.getMatrix().equals(EMPTY_MATRIX)) {
            sb.append(", ").append("matrix=").append(v.getMatrix().toShortString());

            if (0.0f != v.getRotation() || 0.0f != v.getRotationX() || 0.0f != v.getRotationY()) {
                sb.append(", rotate=[")
                        .append(v.getRotation()).append(",")
                        .append(v.getRotationX()).append(",")
                        .append(v.getRotationY())
                        .append("]");

                // print pivote only if its not default
                if (v.getWidth() / 2 != v.getPivotX() || v.getHeight() / 2 != v.getPivotY()) {
                    sb.append(", pivot=[")
                            .append(v.getPivotX()).append(",")
                            .append(v.getPivotY())
                            .append("]");
                }
            }

            if (0.0f != v.getTranslationX() || 0.0f != v.getTranslationY() || 0.0f != v.getTranslationZ()) {
                sb.append(", translate=[")
                        .append(v.getTranslationX()).append(",")
                        .append(v.getTranslationY()).append(",")
                        .append(v.getTranslationZ())
                        .append("]");
            }

            if (1.0f != v.getScaleX() || 1.0f != v.getScaleY()) {
                sb.append(", scale=[")
                        .append(v.getScaleX()).append(",")
                        .append(v.getScaleY())
                        .append("]");
            }
        }

        // padding's
        if (0 != v.getPaddingStart() || 0 != v.getPaddingTop() ||
                0 != v.getPaddingEnd() || 0 != v.getPaddingBottom()) {
            sb.append(", ")
                    .append("padding=[")
                    .append(v.getPaddingStart()).append(",")
                    .append(v.getPaddingTop()).append(",")
                    .append(v.getPaddingEnd()).append(",")
                    .append(v.getPaddingBottom())
                    .append("]");
        }

        // margin's
        final ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;

            if (0 != mlp.leftMargin || 0 != mlp.topMargin ||
                    0 != mlp.rightMargin || 0 != mlp.bottomMargin) {
                sb.append(", ").append("margin=[")
                        .append(mlp.leftMargin).append(",")
                        .append(mlp.topMargin).append(",")
                        .append(mlp.rightMargin).append(",")
                        .append(mlp.bottomMargin)
                        .append("]");
            }
        }

        // width, height, size
        sb.append(", position=[").append(v.getLeft()).append(",").append(v.getTop()).append("]");
        sb.append(", size=[").append(v.getWidth()).append(",").append(v.getHeight()).append("]");

        // texts
        if (v instanceof TextView) {
            final TextView tv = (TextView) v;

            sb.append(", text=\"").append(tv.getText()).append("\"");
        }

        if (Build.VERSION.SDK_INT >= 26) {
            sb.append(", autofillId=\"").append(v.getAutofillId()).append("\"");
            sb.append(", autofillType=\"").append(v.getAutofillType()).append("\"");
            sb.append(", autofillHints=[").append(String.join(", ", v.getAutofillHints())).append("]");
            sb.append(", autofillValue=\"").append(v.getAutofillValue()).append("\"");
        }

        return sb.toString();
    }

    /**
     * @see <a href="https://stackoverflow.com/questions/10137692/how-to-get-resource-name-from-resource-id">Lookup resource name</a>
     */
    private static String resolveIdToName(final Resources r, final View v) {
        if (null == r) return "";

        try {
            return " / " + r.getResourceEntryName(v.getId());
        } catch (Throwable ignored) {
            return "";
        }
    }
}