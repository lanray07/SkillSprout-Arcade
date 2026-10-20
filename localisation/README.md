# Localisation workflow

Target locales: en-GB, en-US, fr, es, de, it, pt-BR, pt-PT, nl, pl, ar, hi.

Run `python tools/extract_ui_strings.py` to migrate direct fixed/template Compose labels, then `python tools/localise.py report`. Template extraction preserves Kotlin expressions as positional placeholders and is idempotent. Parent/PIN notifications, feedback, conditional messages and accessibility descriptions have also been migrated deliberately. Resource export escapes Android quotes, backslashes and newlines. Remaining constructed selectors, enum titles, numeric/plural formatting and underlying validation diagnostics still need language review; the audit is deliberately conservative and is not proof of complete localisation. Brand/game/district names are protected by the glossary.

English (US) has a copied source baseline in `en-US.pending.json`, awaiting regional review; it was not produced by an API and is not shipped. Reviewed languages can add zero/two/few/many plural quantities while retaining placeholder compatibility.

The explicit `translate --locale fr` command uses Google Cloud Translation v2 with `GOOGLE_TRANSLATION_ACCESS_TOKEN` in the developer environment. It never embeds credentials in the APK or contacts the provider at runtime. Translation cache keys include locale, source text and glossary version. Product names and placeholders are protected and checked. Machine outputs are stored in `<locale>.pending.json` and are not packaged.

Export requires `<locale>.review.json` containing `reviewer`, `status: Approved`, and the SHA-256 of the sorted translation JSON. Only reviewed resources can be exported with `export --locale fr`. The export supports Android plural quantities and BCP-47 resource directories; the manifest enables RTL. Native number formatting and every composed dynamic string still require locale QA. Arabic must be tested on a real RTL device. Do not mirror numbers, diagrams or the order of meaningful spelling tiles blindly.

There is no claim of complete translations. English phonics, spelling and reading activities remain en-GB. Translate and review store metadata, screenshot captions, website pages, voice command dictionaries, narration and errors separately; no pending locale gets a public hreflang URL until that page exists and is reviewed.
