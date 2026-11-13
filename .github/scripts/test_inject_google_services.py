import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from inject_google_services import inject


class FirebaseConfigInjectionTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.destination = Path(self.temporary.name) / "google-services.json"
        self.data = {"project_info": {"project_id": "test-project"}, "client": [{"client_info": {"android_client_info": {"package_name": "com.giathinh.uthconnect"}}}]}

    def test_matching_project_and_package(self):
        inject(json.dumps(self.data), "test-project", self.destination)
        self.assertEqual(json.loads(self.destination.read_text(encoding="utf-8")), self.data)

    def test_creates_destination_parent_directory(self):
        nested_destination = Path(self.temporary.name) / "nonexistent" / "dir" / "google-services.json"
        inject(json.dumps(self.data), "test-project", nested_destination)
        self.assertTrue(nested_destination.exists())
        self.assertEqual(json.loads(nested_destination.read_text(encoding="utf-8")), self.data)

    @patch("pathlib.Path.write_text", side_effect=OSError("Disk full"))
    def test_write_os_error_handling(self, _mock_write):
        with self.assertRaises(ValueError) as cm:
            inject(json.dumps(self.data), "test-project", self.destination)
        self.assertIn("Failed to write configuration", str(cm.exception))

    def test_wrong_project_or_package_does_not_write(self):
        with self.assertRaises(ValueError):
            inject(json.dumps(self.data), "wrong-project", self.destination)
        self.data["client"][0]["client_info"]["android_client_info"]["package_name"] = "wrong.package"
        with self.assertRaises(ValueError):
            inject(json.dumps(self.data), "test-project", self.destination)
        self.assertFalse(self.destination.exists())

    def test_missing_or_invalid_secret_does_not_write(self):
        for value in ("", "not-json"):
            with self.assertRaises(ValueError):
                inject(value, "test-project", self.destination)
        with self.assertRaises(ValueError):
            inject(json.dumps(self.data), "", self.destination)
        self.assertFalse(self.destination.exists())


if __name__ == "__main__":
    unittest.main()
