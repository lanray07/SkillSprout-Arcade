import json
import shutil
import tempfile
import unittest
from pathlib import Path
import verify_release


class CandidateGateTests(unittest.TestCase):
    def fixture(self, folder):
        root=Path(folder)
        for path in [verify_release.PACK, verify_release.WAIVER,
                     'app/src/main/assets/packs/manifest.json',
                     'app/src/main/java/com/skillsprout/arcade/ContentPacks.kt',
                     'release/signing-backup-verification.json']:
            dest=root/path
            dest.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(verify_release.ROOT/path,dest)
        return root

    def test_current_candidate_can_build_but_cannot_publish(self):
        self.assertEqual([],verify_release.candidate_checks())
        with tempfile.TemporaryDirectory() as folder:
            root=self.fixture(folder)
            self.assertIn('Exact-candidate acceptance has not been recorded',verify_release.publication_checks(root))

    def test_changed_pack_or_fabricated_approval_invalidates_waiver(self):
        with tempfile.TemporaryDirectory() as folder:
            root=self.fixture(folder)
            pack=root/verify_release.PACK
            pack.write_bytes(pack.read_bytes()+b' ')
            self.assertTrue(verify_release.candidate_checks(root))
        with tempfile.TemporaryDirectory() as folder:
            root=self.fixture(folder)
            waiver=root/verify_release.WAIVER
            data=json.loads(waiver.read_text('utf-8-sig'))
            data['teacherApprovalClaimed']=True
            waiver.write_text(json.dumps(data),encoding='utf-8')
            self.assertTrue(verify_release.candidate_checks(root))

    def test_publication_requires_exact_apk_and_all_hash_bound_checks(self):
        with tempfile.TemporaryDirectory() as folder:
            root=self.fixture(folder)
            apk=root/'candidate.apk';apk.write_bytes(b'test fixture only')
            record={'apkSha256':verify_release.sha(apk),'checks':{}}
            (root/verify_release.ACCEPTANCE).write_text(json.dumps(record),encoding='utf-8')
            self.assertEqual(5,len(verify_release.publication_checks(root,apk)))
            record['apkSha256']='0'*64
            (root/verify_release.ACCEPTANCE).write_text(json.dumps(record),encoding='utf-8')
            self.assertEqual(6,len(verify_release.publication_checks(root,apk)))

    def test_known_reviewer_defects_withdraw_publication_acceptance(self):
        with tempfile.TemporaryDirectory() as folder:
            root=self.fixture(folder)
            record={'knownBlockingIssues':['Read Aloud and checkout regression'],'checks':{}}
            (root/verify_release.ACCEPTANCE).write_text(json.dumps(record),encoding='utf-8')
            self.assertIn('Known release-blocking defects remain; exact-candidate acceptance is withdrawn',
                          verify_release.publication_checks(root))
