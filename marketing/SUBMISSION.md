# Amazon listing preparation

Publisher: O. Bankole. Support: banksmi@mail.com. Draft metadata lives in `listing.en-GB.json`; no claim of name clearance, teacher approval, independent compliance, released localisation or guaranteed learning outcomes is made. The development qualifier must not simply be removed before its underlying release gates are satisfied.

The [Amazon Appstore details documentation](https://developer.amazon.com/docs/app-submission/appstore-details.html), checked 20 October 2026, lists display title, short and long descriptions, product feature bullets and optional keywords. It documents a 2,000-byte short description, 4,000-character long description and three to five feature bullets. Title limits must be checked in the actual current Developer Console; do not substitute another store's limit.

The same source specifies transparent PNG store icons at 114×114 and 512×512, and 3–10 screenshots. Listed landscape sizes are 800×480, 1024×600, 1280×720, 1280×800, 1920×1080, 1920×1200 and 2560×1600, with portrait orientation also supported. Use 1920×1200 tablet and a permitted rotated size such as 1080×1920 phone. Verify accepted sizes in the console at upload. Do not submit screenshots as promotional artwork.

`tools/export_assets.py` creates master/icon exports and original editable SVG game icons/badges. `tools/marketing_templates.py` uses actual implemented-screen captures and creates editable SVG layouts with embedded source captures, plus raster previews where a renderer is available. It never invents game UI. Caption 8 is revised for the implemented file-import model; “downloaded” remains a release claim until a download manager exists. Captures containing the development banner cannot be passed off as final production screens.

Before submission: clear brand, create the actual app, configure audience/markets, review privacy and content policies, complete payment/tax details where necessary, download the Amazon authentication public key, configure real IAP products and verify prices/restoration, sign the production artifact, supply hosted support/privacy URLs, upload real-device captures, and complete compatibility/policy review. No Amazon account changes or submission have been performed.

Do not invent ratings, testimonials, school use, endorsements, discounts or a seven-day trial. A trial may appear only if the actual Amazon configuration supports it. Search-friendly copy and structured data do not guarantee ranking, recommendation or AI citations.
