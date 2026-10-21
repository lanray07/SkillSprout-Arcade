# Amazon listing preparation

Publisher: O. Bankole. Support: banksmi@mail.com. Draft metadata lives in `listing.en-GB.json`; no claim of name clearance, teacher approval, independent compliance, released localisation or guaranteed learning outcomes is made. The development qualifier must not simply be removed before its underlying release gates are satisfied.

The [Amazon Appstore details documentation](https://developer.amazon.com/docs/app-submission/appstore-details.html), checked 20 October 2026, lists display title, short and long descriptions, product feature bullets and optional keywords. It documents a 2,000-byte short description, 4,000-character long description and three to five feature bullets. Title limits must be checked in the actual current Developer Console; do not substitute another store's limit.

The same source specifies transparent PNG store icons at 114×114 and 512×512, and 3–10 screenshots. Listed landscape sizes are 800×480, 1024×600, 1280×720, 1280×800, 1920×1080, 1920×1200 and 2560×1600, with portrait orientation also supported. Use 1920×1200 tablet and a permitted rotated size such as 1080×1920 phone. Verify accepted sizes in the console at upload. Do not submit screenshots as promotional artwork.

`tools/export_assets.py` creates master/icon exports and original editable SVG game icons/badges. `tools/marketing_templates.py` uses actual implemented-screen captures and creates editable SVG layouts with embedded source captures, plus raster previews where a renderer is available. It never invents game UI. Caption 8 is revised for the implemented file-import model; “downloaded” remains a release claim until a download manager exists. Captures containing the development banner cannot be passed off as final production screens.

The Amazon development draft now exists and contains the review-only APK; current account and release evidence lives in `release/`. Before production submission, complete the remaining release gates, including brand/content review, real-device captures and compatibility/policy review. Draft console completion does not establish production readiness or publish the app.

## Localized listing assets (21 October 2026)

`localized/index.html` is the review gallery for 11 language variants: US English, French, Spanish, German, Italian, Brazilian and European Portuguese, Dutch, Polish, Arabic and Hindi. Each variant contains eight tablet PNGs (1920×1200), eight phone PNGs (1080×1920), matching editable SVGs and `listing.json`. `localized/copy.json` is the copy source; `localized/manifest.json` records source/output hashes and measured caption bounds.

Only the surrounding marketing text is translated. The embedded English app captures are preserved byte for byte, with a translated English-only disclosure. These are development JVM Android renders, labelled accordingly, rather than physical Fire screenshots. Translations require native-language review. They do not satisfy app-localisation or teacher-review gates.

The live Amazon draft exposes different selectors for descriptions and images. Of these targets, nine support descriptions (all except Arabic and Hindi), while six support image sets (US English, German, Spanish, French, Italian and Brazilian Portuguese). Dutch, Polish and European Portuguese descriptions use Amazon's default US English image fallback; their translated image files remain available locally. Arabic and Hindi listing packages remain local because neither selector offers them. Existing UK English copy is preserved. Exact saved-draft coverage is recorded in `release/amazon-localization-status.json`; no publication is implied.

To regenerate, run `npm ci --prefix tools/marketing-render`, then `npm run render --prefix tools/marketing-render`. The renderer requires the installed Trebuchet MS, Arial and Nirmala UI fonts (`trebuc.ttf`, `trebucbd.ttf`, `arial.ttf`, `arialbd.ttf`, `Nirmala.ttc`); Windows uses `C:/Windows/Fonts`, or set `SKILLSPROUT_MARKETING_FONTS` to an authorised font directory. Fonts are not bundled. Resvg shapes Arabic/Devanagari and measures caption wrapping. Run `python tools/check_localized_marketing.py` to verify all 176 PNG/SVG pairs, source preservation, dimensions, hashes, text bounds and listing consistency; CI runs this check without needing the rendering fonts.

Do not invent ratings, testimonials, school use, endorsements, discounts or a seven-day trial. A trial may appear only if the actual Amazon configuration supports it. Search-friendly copy and structured data do not guarantee ranking, recommendation or AI citations.
