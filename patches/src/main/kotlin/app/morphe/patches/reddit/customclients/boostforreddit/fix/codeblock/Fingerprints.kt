/*
 * Copyright 2026 brealorg.
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.boostforreddit.fix.codeblock

import app.morphe.patcher.Fingerprint

internal val boostHtmlSanitizerFingerprint = Fingerprint(
    definingClass = "Lhe/h0;",
    name = "d",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("<code>", "<tt>", "<pre>", "</pre>")
)
