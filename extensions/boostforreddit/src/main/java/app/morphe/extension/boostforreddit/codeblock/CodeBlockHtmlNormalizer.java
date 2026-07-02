/*
 * Copyright 2026 brealorg.
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.boostforreddit.codeblock;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodeBlockHtmlNormalizer {
    public static final String MARKER = "MORPHE_CODEBLOCK_HTML_NORMALIZER_V2_SEGMENTED";

    private static final Pattern PARAGRAPH_PATTERN =
            Pattern.compile("(?is)<p>(.*?)</p>");

    private static final Pattern CODE_PATTERN =
            Pattern.compile("(?is)<code>(.*?)</code>");

    private CodeBlockHtmlNormalizer() {
    }

    public static String normalize(String html) {
        if (html == null || html.length() == 0) {
            return html;
        }

        if (html.indexOf("<code>") < 0 || html.indexOf("</code>") < 0) {
            return html;
        }

        try {
            return normalizeParagraphs(html);
        } catch (Throwable ignored) {
            return html;
        }
    }

    private static String normalizeParagraphs(String html) {
        Matcher paragraphMatcher = PARAGRAPH_PATTERN.matcher(html);
        StringBuffer out = new StringBuffer();
        boolean changed = false;

        while (paragraphMatcher.find()) {
            String paragraphBody = paragraphMatcher.group(1);
            String normalized = normalizeParagraphBody(paragraphBody);

            if (normalized != null) {
                paragraphMatcher.appendReplacement(out, Matcher.quoteReplacement(normalized));
                changed = true;
            }
        }

        paragraphMatcher.appendTail(out);
        return changed ? out.toString() : normalizeParagraphBodyFallback(html);
    }

    private static String normalizeParagraphBodyFallback(String html) {
        String normalized = normalizeParagraphBody(html);
        return normalized != null ? normalized : html;
    }

    /**
     * Reddit legacy body_html can represent fenced code blocks as multiline <code> tags inside
     * one paragraph:
     *
     * <p>Text:
     * <code>
     * code();
     * </code>
     * Or:
     * <code>
     * other();
     * </code></p>
     *
     * Boost already has a native <pre> renderer path. Split only multiline code tags into
     * standalone <pre><code> blocks and keep surrounding prose outside the code block.
     */
    private static String normalizeParagraphBody(String paragraphBody) {
        Matcher codeMatcher = CODE_PATTERN.matcher(paragraphBody);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        boolean changed = false;

        while (codeMatcher.find()) {
            String codeBody = codeMatcher.group(1);

            if (!isMultilineCode(codeBody)) {
                continue;
            }

            String before = paragraphBody.substring(cursor, codeMatcher.start());
            appendProse(out, before);

            out.append("<pre><code>");
            out.append(trimCodeBlockEdges(codeBody));
            out.append("</code></pre>");

            cursor = codeMatcher.end();
            changed = true;
        }

        if (!changed) {
            return null;
        }

        appendProse(out, paragraphBody.substring(cursor));
        return out.toString();
    }

    private static boolean isMultilineCode(String codeBody) {
        if (codeBody == null) {
            return false;
        }

        return codeBody.indexOf('\n') >= 0
                || codeBody.indexOf('\r') >= 0
                || codeBody.indexOf("<br>") >= 0
                || codeBody.indexOf("<br/>") >= 0
                || codeBody.indexOf("<br />") >= 0;
    }

    private static String trimCodeBlockEdges(String codeBody) {
        int start = 0;
        int end = codeBody.length();

        while (start < end) {
            char c = codeBody.charAt(start);
            if (c == '\n' || c == '\r') {
                start++;
            } else {
                break;
            }
        }

        while (end > start) {
            char c = codeBody.charAt(end - 1);
            if (c == '\n' || c == '\r') {
                end--;
            } else {
                break;
            }
        }

        return codeBody.substring(start, end);
    }

    private static void appendProse(StringBuilder out, String prose) {
        String trimmed = trimParagraphWhitespace(prose);

        if (trimmed.length() == 0) {
            return;
        }

        out.append("<p>");
        out.append(trimmed);
        out.append("</p>");
    }

    private static String trimParagraphWhitespace(String text) {
        int start = 0;
        int end = text.length();

        while (start < end) {
            char c = text.charAt(start);
            if (c == '\n' || c == '\r' || c == ' ' || c == '\t') {
                start++;
            } else {
                break;
            }
        }

        while (end > start) {
            char c = text.charAt(end - 1);
            if (c == '\n' || c == '\r' || c == ' ' || c == '\t') {
                end--;
            } else {
                break;
            }
        }

        return text.substring(start, end);
    }
}
