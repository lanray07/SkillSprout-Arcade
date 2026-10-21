import base64, hashlib, json, tempfile, unittest
from pathlib import Path
from cryptography.exceptions import InvalidTag
import backup_signing
import prepare_release_review
import review_content
import serve_review

class ReleaseReviewTests(unittest.TestCase):
    def test_review_server_and_backup_destinations_exclude_git_and_private_paths(self):
        handler=object.__new__(serve_review.ReviewHandler);handler.directory=str(serve_review.ROOT)
        self.assertEqual(str(serve_review.ROOT/'release/review-kit/index.html'),handler.translate_path('/release/review-kit/index.html'))
        for path in ['/README.md','/.git/config','/release/review-kit/../../.git/config','/local.properties']:
            self.assertEqual(str(serve_review.ROOT/'.review-not-served'),handler.translate_path(path))
        with self.assertRaises(ValueError):backup_signing.private_destination(backup_signing.ROOT/'release/never-write-backup')
        with tempfile.TemporaryDirectory() as folder:
            existing=Path(folder)/'existing';existing.write_text('fixture')
            with self.assertRaises(ValueError):backup_signing.private_destination(existing)
            self.assertEqual(Path(folder).resolve()/'new',backup_signing.private_destination(Path(folder)/'new'))

    def test_encrypted_backup_round_trip_and_wrong_password_rejection(self):
        # Ephemeral fixtures only: tests never access publisher keys.
        files={name:('fixture '+name).encode() for name in backup_signing.REQUIRED}
        encrypted=backup_signing.encrypt(files,'a private fixture passphrase')
        self.assertNotIn(b'fixture',encrypted)
        self.assertEqual(files,backup_signing.decrypt(encrypted,'a private fixture passphrase'))
        with self.assertRaises(InvalidTag):backup_signing.decrypt(encrypted,'an incorrect fixture passphrase')
        envelope=json.loads(encrypted);cipher=bytearray(base64.b64decode(envelope['ciphertext']));cipher[0]^=1;envelope['ciphertext']=base64.b64encode(cipher).decode()
        with self.assertRaises(InvalidTag):backup_signing.decrypt(json.dumps(envelope).encode(),'a private fixture passphrase')
        with self.assertRaises(ValueError):backup_signing.encrypt(files,'short')

    def test_review_import_rejects_unreviewed_stale_or_duplicate_decisions(self):
        pack={'activities':[{'id':'fixture-one'},{'id':'fixture-two'}]};raw=json.dumps(pack).encode()
        review={'reviewer':'Fixture only','qualifications':'Test fixture','reviewedAt':'2026-10-21','evidence':'Test only',
            'packSha256':hashlib.sha256(raw).hexdigest(),'status':'Approved',
            'activityDecisions':[{'id':'fixture-one','status':'Approved'},{'id':'fixture-two','status':'Pending'}]}
        with self.assertRaises(ValueError):review_content.validate_review(pack,raw,review)
        review['activityDecisions'][1]['status']='Approved'
        self.assertEqual(review,review_content.validate_review(pack,raw,review))
        with self.assertRaises(ValueError):review_content.validate_review(pack,raw+b' ',review)
        review['activityDecisions'][1]['id']='fixture-one'
        with self.assertRaises(ValueError):review_content.validate_review(pack,raw,review)

    def test_review_kit_has_every_activity_unreviewed_and_preserves_pack(self):
        root=prepare_release_review.ROOT
        source=root/'app/src/main/assets/packs/starter.json';original=source.read_bytes()
        with tempfile.TemporaryDirectory() as folder:
            temporary=Path(folder)
            for rel in ['app/src/main/assets/packs/starter.json','release/evidence.json','release/amazon-localization-status.json']:
                target=temporary/rel;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes((root/rel).read_bytes())
            manifest=prepare_release_review.prepare(temporary)
            page=(temporary/'release/review-kit/teacher-review.html').read_text('utf-8')
            self.assertEqual(444,page.count(' data-id='))
            self.assertEqual(444,page.count('<option value="Pending">Not reviewed</option>'))
            self.assertEqual(444,manifest['activities'])
            self.assertNotIn('value="Approved" selected',page)
            self.assertEqual(original,(temporary/'app/src/main/assets/packs/starter.json').read_bytes())

if __name__=='__main__':unittest.main()
