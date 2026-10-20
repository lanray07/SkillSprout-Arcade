# Localisation workflow

Target locales: en-GB, en-US, fr, es, de, it, pt-BR, pt-PT, nl, pl, ar, hi.

Run `python tools/extract_ui_strings.py` to migrate fixed Compose UI literals, then `python tools/localise.py report`. Dynamic strings, constructed labels, error branches and stored enum titles still require deliberate resource migration; the report lists remaining obvious literal locations and must not be read as proof that extraction is complete.

The explicit `translate --locale fr` command uses Google Cloud Translation v2 with `GOOGLE_TRANSLATION_ACCESS_TOKEN` in the developer environment. It never embeds credentials in the APK or contacts the provider at runtime. Translation cache keys include locale, source text and glossary version. Product names and placeholders are protected and checked. Machine outputs are stored in `<locale>.pending.json` and are not packaged.

Export requires `<locale>.review.json` containing `reviewer`, `status: Approved`, and the SHA-256 of the sorted translation JSON. Only reviewed resources can be exported with `export --locale fr`. The export supports Android plural quantities and BCP-47 resource directories; the manifest enables RTL. Native number formatting and every composed dynamic string still require locale QA. Arabic must be tested on a real RTL device. Do not mirror numbers, diagrams or the order of meaningful spelling tiles blindly.

There is no claim of complete translations. English phonics, spelling and reading activities remain en-GB. Translate and review store metadata, screenshot captions, website pages, voice command dictionaries, narration and errors separately; no pending locale gets a public hreflang URL until that page exists and is reviewed.
