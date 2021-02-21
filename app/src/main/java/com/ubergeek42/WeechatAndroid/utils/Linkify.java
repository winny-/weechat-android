package com.ubergeek42.WeechatAndroid.utils;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

import com.ubergeek42.WeechatAndroid.R;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ubergeek42.WeechatAndroid.utils.Toaster.ErrorToast;

/**
 * Our own Linkifier
 * Rationale: allow custom custom URLs as well URLs starting with www. with less fiddling;
 * also use non-colored URLSpans so that we don't have to recreate them every time
 */
public class Linkify {
    private static @Nullable Pattern messageFilter = null;

    // pattern will always find urls starting with protocol, the only exception being "www."
    // in this case, prepend "http://" to url.
    public static void linkify(@NonNull Spannable s) {
        Matcher m = URL.matcher(s);
        while (m.find()) {
            String url = m.group(0);
            if (Objects.requireNonNull(url).startsWith("www."))
                url = "http://" + url;
            s.setSpan(new URLSpan2(url), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public static void linkify(@NonNull Spannable spannable, @NonNull CharSequence message) {
        if (messageFilter != null)
            message = Utils.replaceWithSpaces(message, messageFilter);
        int offset = spannable.length() - message.length();
        Matcher m = URL.matcher(message);
        while (m.find()) {
            String url = m.group(0);
            if (Objects.requireNonNull(url).startsWith("www."))
                url = "http://" + url;
            spannable.setSpan(new URLSpan2(url), offset + m.start(), offset + m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public static void setMessageFilter(@Nullable Pattern filter) {
        messageFilter = filter;
    }

    public static @Nullable String getFirstUrlFromString(CharSequence s) {
        Matcher m = URL.matcher(s);
        return m.find() ? m.group(0) : null;
    }

    // an url span that doesn't change the color of the link
    // also it checks if the text line associated with it has clickDisable set by
    // onItemLongClick from ChatLinesAdapter, which prevents unwanted clicks on long clicks
    // also make sure we don't crash if nothing can handle our intent
    private static class URLSpan2 extends URLSpan {

        URLSpan2(@NonNull String url) {super(url);}

        @Override public void updateDrawState(@NonNull TextPaint ds) {
            ds.setUnderlineText(true);
        }

        @Override public void onClick(@NonNull View widget) {
            // don't call super because super will open urls in the same tab
            Context context = widget.getContext();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getURL()));
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                ErrorToast.show(R.string.error__etc__activity_not_found_for_url, getURL());
            }
        }
    }

    final private static String IRIC   = "[a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]";
    final private static String IRIC_H = "[a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF-]";
    final private static String GLTDC  = "[a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]";

    final private static Pattern URL = Pattern.compile(
        // url must be preceded by a word boundary
        "\\b" +
        // protocol:// or www.
        "(?:[A-Za-z]+://|www\\.)" +
        // optional user:pass at
        "(?:\\S+(?::\\S*)?@)?" +
        "(?:" +
              // ip address (+ some exceptions)
              "(?!10(?:\\.\\d{1,3}){3})" +
              "(?!127(?:\\.\\d{1,3}){3})" +
              "(?!169\\.254(?:\\.\\d{1,3}){2})" +
              "(?!192\\.168(?:\\.\\d{1,3}){2})" +
              "(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})" +
              "(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])" +
              "(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}" +
              "(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))" +
        "|" +
              // domain name (a.b.c.com)
              "(?:" + IRIC + "+(?:" + IRIC_H + "*" + IRIC + "+|"+ IRIC + ")*\\.)+" +  // (\w+((-|\w)*\w|\w)*\.)+      a. a-b. a-b.a-b. a---b. a--b.a--b.
              GLTDC + "{2,63}" +                            // (\w){2,63}           com ninja
        ")" +
        // port?
        "(?::\\d{2,5})?" +
        // & the rest
        "(?:" +
              "\\.?/" +
              "(?:" +
                    // hello(world) in hello(world))
                    "(?:" +
                         "[^\\s(]*" +
                         "\\(" +
                         "[^\\s)]+" +
                         "\\)" +
                    ")+" +
                    "[^\\s)]*?" +
              "|" +
                    // any string (non-greedy!)
                    "\\S*?" +
              ")" +
        ")?" +
        // url must be directly followed by
        "(?=" +
              // some possible punctuation
              // AND space or end of string
              "[])>,.!?:\"”]*" +
              "(?:\\s|$)" +
        ")"
        , Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
}
