# TODO

- [TODO](#TODO)
- [Status](#Status)

## Authentication

Options for persistent authentication are:

1. Implement secure scoped token authentication.
2. Dramatically increase session times and let users manage sessions.

## Upload

- Add Upload Options to Previews
- Add Support for Plain Text Sharing

## File List

- Add Multi-Select
- Add Swiper to Preview

## File Preview

- Add PDF
- Add File Options

## Settings

- Overhaul Server Selector
- Add Default Upload Options
- Add Fingerprint Authentication

## Retrofit

- Implement Version Check

## Room

- Move Room to Directory
- Implement Room Active Server

---

# Status

## Authentication

- Returns a Cookie
- Uses Cookie to get Token

## Uploads

### File

- File Previews

### Multiple Files

- Image Previews w/ Multi-Select

### URLs

- Preview w/ Vanity Option

### Text

- WIP

## File List/Preview

Total Cache Size: `700` MB

- Uses `/recent/`
- Uses Token
- No Caching

### Glide

**All Images**

- Uses `/raw/?view=gallery`
- Uses Cookies
- `250` MB LRU Cache
- Small Preview Enabled

### ExoPlayer

**All Video and Audio**

- Uses `/raw/?view=gallery`
- Uses Cookies
- `350` MB LRU Cache
- Small Preview Disabled

### Okhttp

**All Plain Text**

Loads the Content into a WebView with highlight.js

- Uses `/raw/?view=gallery`
- Uses Cookies
- `100` MB LRU Cache
- No Small Preview
