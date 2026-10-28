import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace

SCRIPT = Path(__file__).resolve().parents[1] / "check_agent_instructions.py"
SPEC = importlib.util.spec_from_file_location("agent_checks", SCRIPT)
CHECKS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CHECKS)


class AgentInstructionChecksTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.write("CLAUDE.md", "@AGENTS.md\n")
        routes = []
        for name in sorted(CHECKS.REQUIRED_TOPICS):
            path = f"docs/engineering/{name}.md"
            self.write(path, f"# {name}\n")
            routes.append(f"[{name}]({path})")
        self.write("AGENTS.md", "\n".join(routes))
        self.write(
            "docs/engineering/reference-patterns.md",
            "<!-- reference-revision: " + "a" * 40 + " -->\n"
            "<!-- reference-file: example.kt -->\n",
        )

    def write(self, path, content):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(content, encoding="utf-8")

    def test_valid_routes(self):
        self.assertEqual([], CHECKS.check(self.root))

    def test_claude_must_not_duplicate_rules(self):
        self.write("CLAUDE.md", "@AGENTS.md\nAdditional competing rules")
        self.assertIn("CLAUDE.md must contain only @AGENTS.md", CHECKS.check(self.root))

    def test_missing_topic_and_link(self):
        (self.root / "docs/engineering/ui.md").unlink()
        errors = CHECKS.check(self.root)
        self.assertTrue(any("Missing topic:" in error for error in errors))
        self.assertTrue(any("Broken/local-outside link" in error for error in errors))

    def test_root_budget(self):
        self.write("AGENTS.md", "\n" * 101)
        self.assertTrue(any("routing budget" in error for error in CHECKS.check(self.root)))

    def test_topic_budget(self):
        self.write("docs/engineering/ui.md", "a" * 12289)
        self.assertTrue(any("byte budget" in error for error in CHECKS.check(self.root)))

    def test_topic_requires_route(self):
        entry = self.root / "AGENTS.md"
        entry.write_text(entry.read_text().replace("docs/engineering/ui.md", "#ui"))
        self.assertIn("Unrouted topic: docs/engineering/ui.md", CHECKS.check(self.root))

    def test_new_topic_cannot_be_silently_unrouted(self):
        self.write("docs/engineering/new-topic.md", "# New topic")
        self.assertIn("Unrouted topic: docs/engineering/new-topic.md", CHECKS.check(self.root))

    def test_missing_entry_point(self):
        (self.root / "AGENTS.md").unlink()
        self.assertEqual(["Missing AGENTS.md"], CHECKS.check(self.root))

    def test_external_and_fragment_links_are_not_local_files(self):
        self.write("docs/engineering/ui.md", "[external](https://example.com) [section](#section)")
        self.assertEqual([], CHECKS.check(self.root))

    def test_link_cannot_escape_repository(self):
        self.write("docs/engineering/ui.md", "[outside](../../../outside.md)")
        self.assertTrue(any("local-outside" in error for error in CHECKS.check(self.root)))

    def test_literal_feature_impl_dependency_is_rejected(self):
        self.write("feature-one-impl/build.gradle", "implementation project(':feature-two-impl')")
        self.write("feature-three-impl/build.gradle.kts", 'implementation(project(path = ":feature-two-impl"))')
        errors = CHECKS.check(self.root)
        self.assertEqual(2, sum("implementation dependency" in error for error in errors))

    def test_api_and_app_dependencies_are_allowed_comments_ignored(self):
        self.write("feature-one-impl/build.gradle", "implementation project(':feature-two-api')\n"
                   "// implementation project(':feature-two-impl')\n"
                   "/* implementation project(':feature-three-impl') */")
        self.write("app/build.gradle", "implementation project(':feature-two-impl')")
        self.assertEqual([], CHECKS.check(self.root))

    def test_missing_reference_marker(self):
        self.write("docs/engineering/reference-patterns.md", "# References")
        self.assertTrue(any("pinned revision" in error for error in CHECKS.check(self.root)))

    def test_missing_pinned_object_is_reported_only_when_requested(self):
        missing = SimpleNamespace(returncode=128, stdout="")
        with patch.object(CHECKS.subprocess, "run", return_value=missing) as command:
            self.assertEqual([], CHECKS.check(self.root))
            command.assert_not_called()
            self.assertTrue(any("Pinned reference unavailable" in error
                                for error in CHECKS.check(self.root, True)))


if __name__ == "__main__":
    unittest.main()
