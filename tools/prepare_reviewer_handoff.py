"""Package public review inputs; never include private reviews, keys or approvals."""
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def prepare():
    files = [
        ROOT / 'app/src/main/assets/packs/starter.json',
        ROOT / 'release/batch-review-remaining-years.json',
        ROOT / 'release/amazon-localization-status.json',
        ROOT / 'release/free-reviewer-routes-2026-10-22.json',
        ROOT / 'release/production-follow-up-2026-10-22.json',
        ROOT / 'release/production-follow-up-2026-10-23.json',
        ROOT / 'release/parent-controls-persistence-2026-10-23.json',
        ROOT / 'release/narration-software-2026-10-23.json',
        ROOT / 'release/narration-recovery-2026-10-23.json',
        ROOT / 'docs/NARRATION-RECOVERY.md',
        ROOT / 'release/outstanding-approvals-2026-10-23.json',
        ROOT / 'release/fire-billing-accessibility-2026-10-23.json',
        ROOT / 'release/fire-missing-voice-2026-10-23.json',
        ROOT / 'release/voice-qa-setup-2026-10-23.json',
        ROOT / 'artifacts/screenshots/phone/12-pending-purchases-large-text.png',
        ROOT / 'artifacts/screenshots/tablet/12-pending-purchases-large-text.png',
        ROOT / 'docs/RELEASE-BLOCKERS.md',
        ROOT / 'docs/PRIVACY-ASSESSMENT.md',
        ROOT / 'release/privacy-decision-workbook.json',
        ROOT / 'release/brand-preliminary-check-2026-10-22.json',
        ROOT / 'release/privacy-asset-engineering-audit.json',
        ROOT / 'release/support-retention-policy.json',
        ROOT / 'release/fire-voiceview-2026-10-22.json',
        ROOT / 'release/fire-voiceview-publisher-2026-10-22.json',
        ROOT / 'release/voiceview-learning-checklist.json',
        ROOT / 'release/fire-voiceview-diagrams-states-2026-10-22.json',
        ROOT / 'docs/FIRE-ACCESSIBILITY-TESTING.md',
        ROOT / 'release/brand-uk-register-search-2026-10-22.json',
    ]
    for directory in ['release/review-kit', 'marketing/localized']:
        files.extend(p for p in (ROOT / directory).rglob('*')
                     if p.is_file() and p.suffix.lower() in {'.html', '.json', '.png', '.svg', '.css'})
    files = sorted(set(files))
    manifest = {str(p.relative_to(ROOT)).replace('\\', '/'): hashlib.sha256(p.read_bytes()).hexdigest()
                for p in files}
    instructions = '''SkillSprout reviewer handoff â€” no release approval included

Extract the whole archive before opening its HTML files.
Teacher/teaching-experience reviewer: open release/review-kit/teacher-review.html.
Check all 444 activities for accuracy, age/difficulty, ambiguity, choices, hints,
explanations, references and prerequisites, plus diagrams/narration in the app.
Record only your actual teaching qualifications or relevant experience. Export
your decisions privately and include app/device checks and limitations.
Conversation decisions and AI editorial changes do not establish teacher approval.
The pack digest is pinned in review-kit/manifest.json and this archive manifest.

Language reviewers: open marketing/localized/index.html and review each assigned
locale's text, screenshots, meaning, spelling, layout and English-only disclosures.
The app and activities are English only. Marketing translations are drafts, not
released app translations. Record your actual language fluency and identify each
reviewed file by the SHA-256 in handoff-manifest.json. Return corrections and
explicit decisions for every assigned file; do not infer approval for other locales.
See release/amazon-localization-status.json for Amazon profile/fallback limitations.
The image sources are development renders, not final approved Fire screenshots.

Both roles: inspect the limitations in release/review-kit/review-plan.json and
docs/RELEASE-BLOCKERS.md. Return completed reviews outside the public repository.
Do not send keys, passwords or child records. No file in this pack approves a
production release, and completing one review does not approve the other gates.
Privacy and brand drafts are included for separate publisher/professional review.
'''
    out = ROOT / 'artifacts/reviewer-handoff/SkillSprout-reviewer-handoff.zip'
    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
        for p in files:
            archive.write(p, str(p.relative_to(ROOT)).replace('\\', '/'))
        archive.writestr('START-HERE.txt', instructions)
        archive.writestr('handoff-manifest.json', json.dumps(manifest, indent=2) + '\n')
    with zipfile.ZipFile(out) as archive:
        assert archive.testzip() is None
        for name, expected in manifest.items():
            assert hashlib.sha256(archive.read(name)).hexdigest() == expected
        page = archive.read('release/review-kit/teacher-review.html').decode('utf-8')
        assert page.count('<option value="Pending">Not reviewed</option>') == 444
        assert 'value="Approved" selected' not in page
    result = {'artifact': str(out.relative_to(ROOT)).replace('\\', '/'),
              'sha256': hashlib.sha256(out.read_bytes()).hexdigest(),
              'files': len(files) + 2, 'archiveAndHashesVerified': True,
              'status': 'Handoff only; all reviewer decisions pending',
              'sourceManifestSha256': hashlib.sha256(json.dumps(manifest, sort_keys=True).encode()).hexdigest()}
    (ROOT / 'release/reviewer-handoff.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    prepare()
