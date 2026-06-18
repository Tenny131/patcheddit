# Breal Boost Hotfixes

Unofficial Morphe patch source for **Boost for Reddit 1.12.12**.

This repository provides temporary hotfix builds for Boost while selected Boost-specific fixes are pending upstream review or are being tested separately from the main Patcheddit bundle.

This is not an official Boost for Reddit project, and it is not intended to permanently replace Patcheddit.

## Project page

GitHub repository:

`https://github.com/brealorg/breal-boost-hotfixes`

## Morphe patch source

Use this source in Morphe:

`https://raw.githubusercontent.com/brealorg/breal-boost-hotfixes/main/patches-bundle.json`

Morphe uses this JSON source to find and download the current `.mpp` patch bundle.

Do not use the normal GitHub project page as the Morphe source. The GitHub project page is for humans; the raw `patches-bundle.json` URL is for Morphe.

## Current release

Current public bundle:

`1.4.0-boost-hotfix.14`

Latest release asset:

`patches-1.4.0.mpp`

SHA256:

`98d977d363cab1afd21507b0a9fd245891bcfce2542ea486393f9209d473968c`

Hotfix 14 was tested with:

- Boost for Reddit 1.12.12
- Morphe Android app
- Clean Boost APK from APKMirror
- Public GitHub release bundle
- In-app functional testing

## Included Boost hotfixes

### v.redd.it audio fix

Fixes Boost sharing/downloading Reddit videos where audio retrieval fails with errors such as:

- `Failed to retrieve audio`
- `JsonParseException: Unexpected character '<'`

### Slow Giphy loading fix

Fixes very slow Giphy loading by bypassing Boost's old broken/slow Giphy resolver and using a direct media fallback:

`https://media.giphy.com/media/<id>/giphy.mp4`

### Inline Giphy previews in comments

Restores inline Giphy previews in Boost comment threads.

Behavior:

- Tap the preview area to collapse/expand the comment
- Tap the source/Open Giphy line to open the external source

### Direct GIF previews in comments

Adds inline previews for direct `.gif` links, including links such as:

`https://i.redd.it/example.gif`

### Download completed notification visibility

Moves completed download notifications to a separate default-importance Android notification channel.

Behavior:

- Progress/group download notifications remain on `80_downloads_channel` with LOW importance
- Completed download notifications use `81_downloads_completed_channel` with DEFAULT importance

## Tested behavior in hotfix 14

Hotfix 14 has been tested for:

- Public bundle download through Morphe
- Patching a clean Boost 1.12.12 APK
- APK build/sign/install through Morphe
- Giphy inline previews
- Direct `i.redd.it/*.gif` inline previews
- Comment collapse/expand behavior
- Source/Open Giphy link behavior
- Basic comment-thread scrolling
- Download completed notification channel split
- `80_downloads_channel` remains LOW
- `81_downloads_completed_channel` is created as DEFAULT

## Upstream status

This repository is intended as a temporary hotfix source.

Relevant work has been opened upstream where appropriate:

- Boost video audio fix: upstream PR exists
- Slow Giphy loading fix: upstream PR exists
- Inline Giphy/direct GIF preview fix: upstream draft PR exists

The goal is not to replace Patcheddit permanently, but to provide a working Boost-specific source while fixes are being tested or reviewed.

## Important usage notes

Use your own clean Boost APK as input.

Do not install random pre-patched APKs from strangers.

If installation fails because of a signature conflict, uninstall the existing Boost installation first or test on a separate device/profile. Uninstalling may remove local app data.

This repository is unofficial and provided as-is.

## Attribution

This repository is derived from Patcheddit.

Original work, upstream patch structure, and inherited patches belong to their respective authors.

Additional Boost-specific hotfix modifications in this fork are maintained separately until they are no longer needed or are superseded upstream.

## License

GPL-3.0, following the upstream project license.
