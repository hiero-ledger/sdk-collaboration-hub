#!/usr/bin/env python3
"""Lightweight lint checks for API meta-language markdown specs.

This script scans markdown fenced code blocks and lints only blocks that look
like the Hiero API meta-language DSL. This avoids false positives in regular
language examples (Python, Java, Rust, Go, JavaScript, etc.).
"""

from __future__ import annotations

import argparse
import fnmatch
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Sequence


@dataclass(frozen=True)
class Rule:
    rule_id: str
    pattern: re.Pattern[str]
    message: str


@dataclass(frozen=True)
class Finding:
    file_path: Path
    line_number: int
    rule_id: str
    message: str
    line_text: str


RULES: List[Rule] = [
    Rule(
        rule_id="java-byte-array",
        pattern=re.compile(r"\bbyte\[\]"),
        message="Use meta-language type `bytes` instead of Java-specific `byte[]`.",
    ),
    Rule(
        rule_id="java-boolean",
        pattern=re.compile(r"\bboolean\b"),
        message="Use canonical meta-language type `bool` instead of `boolean`.",
    ),
    Rule(
        rule_id="java-string",
        pattern=re.compile(r"\bString\b"),
        message="Use canonical meta-language type `string` instead of `String`.",
    ),
    Rule(
        rule_id="java-set-generic",
        pattern=re.compile(r"\bSet<"),
        message="Use canonical collection type `set<TYPE>` instead of `Set<TYPE>`.",
    ),
    Rule(
        rule_id="undefined-long-field",
        pattern=re.compile(r":\s*long\b"),
        message="Use an explicit meta-language integer type like `int64` instead of `long`.",
    ),
    Rule(
        rule_id="undefined-long-return",
        pattern=re.compile(r"^\s*long\b"),
        message="Use an explicit meta-language integer type like `int64` instead of `long`.",
    ),
    Rule(
        rule_id="undefined-int",
        pattern=re.compile(r"\bint\b"),
        message="Use an explicit meta-language integer type like `int32` or `int64` instead of `int`.",
    ),
    Rule(
        rule_id="undefined-float",
        pattern=re.compile(r"\bfloat\b"),
        message="Use `double` or `decimal` instead of `float`.",
    ),
    Rule(
        rule_id="void-field-or-param",
        pattern=re.compile(r":\s*void\b"),
        message="`void` is only valid as a return type, not as a field or parameter type.",
    ),
    Rule(
        rule_id="namespace-hyphen",
        pattern=re.compile(r"^\s*namespace\s+[A-Za-z0-9_.]*-[A-Za-z0-9_.-]*\s*$"),
        message="Namespace identifiers should not contain hyphens. Use lowerCamelCase and optional dot notation.",
    ),
]


NON_DSL_FENCE_LANGS = {
    "bash",
    "c",
    "cpp",
    "csharp",
    "go",
    "java",
    "javascript",
    "js",
    "json",
    "kotlin",
    "python",
    "rust",
    "shell",
    "sh",
    "swift",
    "toml",
    "ts",
    "typescript",
    "yaml",
    "yml",
}

DSL_CUE_PATTERNS: Sequence[re.Pattern[str]] = (
    re.compile(r"\bnamespace\b"),
    re.compile(r"\brequires\b"),
    re.compile(r"\babstraction\b"),
    re.compile(r"\benum\b"),
    re.compile(r"\bconstant\b"),
    re.compile(r"\bextends\b"),
    re.compile(r"@@[A-Za-z]"),
)

DEFAULT_SCOPE_GLOBS: Sequence[str] = (
    "v3-sandbox/prototype-api/*.md",
    "proposals/hips/*.md",
)


def should_lint_block(fence_lang: str, block_lines: Sequence[str], untagged_is_dsl: bool) -> bool:
    """Return True if a fenced block likely contains API meta-language DSL."""
    lang = fence_lang.lower()
    if lang in NON_DSL_FENCE_LANGS:
        return False
    if not lang and untagged_is_dsl:
        return True

    block_text = "\n".join(block_lines)
    return any(pattern.search(block_text) for pattern in DSL_CUE_PATTERNS)


def iter_lintable_code_block_lines(lines: List[str], untagged_is_dsl: bool) -> Iterable[tuple[int, str]]:
    """Yield (line_number, line_text) pairs from lintable fenced code blocks."""
    in_fence = False
    fence_lang = ""
    block_start_line = 0
    block_lines: List[str] = []

    for idx, line in enumerate(lines, start=1):
        stripped = line.strip()
        if stripped.startswith("```"):
            if not in_fence:
                # Opening fence: capture fence language and start collecting block lines.
                in_fence = True
                block_start_line = idx + 1
                fence_lang = stripped[3:].strip().split()[0].lower() if stripped[3:].strip() else ""
                block_lines = []
            else:
                # Closing fence: decide once per block whether to lint its lines.
                if should_lint_block(fence_lang, block_lines, untagged_is_dsl):
                    for offset, block_line in enumerate(block_lines):
                        yield block_start_line + offset, block_line
                in_fence = False
                fence_lang = ""
                block_lines = []
            continue

        if in_fence:
            block_lines.append(line)


def lint_file(file_path: Path, untagged_is_dsl: bool) -> List[Finding]:
    """Run all rules against one markdown file and return findings."""
    text = file_path.read_text(encoding="utf-8")
    lines = text.splitlines()
    findings: List[Finding] = []

    for line_number, line_text in iter_lintable_code_block_lines(lines, untagged_is_dsl):
        for rule in RULES:
            if rule.pattern.search(line_text):
                findings.append(
                    Finding(
                        file_path=file_path,
                        line_number=line_number,
                        rule_id=rule.rule_id,
                        message=rule.message,
                        line_text=line_text.rstrip(),
                    )
                )

    return findings


def load_ignore_patterns(root: Path) -> List[str]:
    """Load optional ignore globs from .spec-lint-ignore in repository root."""
    ignore_file = root / ".spec-lint-ignore"
    if not ignore_file.exists():
        return []

    patterns: List[str] = []
    for line in ignore_file.read_text(encoding="utf-8").splitlines():
        trimmed = line.strip()
        if not trimmed or trimmed.startswith("#"):
            continue
        patterns.append(trimmed)
    return patterns


def is_ignored(file_path: Path, root: Path, patterns: Sequence[str]) -> bool:
    """Return True when a file matches any ignore glob."""
    rel_posix = file_path.relative_to(root).as_posix()
    return any(fnmatch.fnmatch(rel_posix, pattern) for pattern in patterns)


def in_scope(file_path: Path, root: Path, scope_globs: Sequence[str]) -> bool:
    """Return True when a file matches any configured lint scope glob."""
    rel_posix = file_path.relative_to(root).as_posix()
    return any(fnmatch.fnmatch(rel_posix, scope_glob) for scope_glob in scope_globs)


def resolve_candidate_files(file_args: List[str], root: Path, scope_globs: Sequence[str]) -> List[Path]:
    """Resolve explicit file args, or discover markdown files from scope globs."""
    if file_args:
        return [Path(p).resolve() for p in file_args]

    return sorted(
        {
            file_path.resolve()
            for scope_glob in scope_globs
            for file_path in root.glob(scope_glob)
        }
    )


def is_lintable_markdown(file_path: Path, root: Path, ignore_patterns: Sequence[str]) -> bool:
    """Return True for existing, in-repo, non-ignored markdown files."""
    if not file_path.exists() or file_path.suffix != ".md":
        return False
    if not file_path.is_relative_to(root):
        return False
    return not is_ignored(file_path, root, ignore_patterns)


def collect_files(file_args: List[str], root: Path, scope_globs: Sequence[str], ignore_patterns: Sequence[str]) -> List[Path]:
    """Collect the final list of markdown files that should be linted."""
    candidates = resolve_candidate_files(file_args, root, scope_globs)
    return [p for p in candidates if is_lintable_markdown(p, root, ignore_patterns)]


def main() -> int:
    """Parse CLI arguments, lint target files, and return a process exit code."""
    parser = argparse.ArgumentParser(description="Lint markdown files for API meta-language spec consistency.")
    parser.add_argument(
        "files",
        nargs="*",
        help="Optional markdown file list. If omitted, scoped defaults are checked.",
    )
    parser.add_argument(
        "--repo-root",
        default=".",
        help="Repository root directory (default: current directory).",
    )
    parser.add_argument(
        "--scope",
        action="append",
        default=[],
        help="Optional markdown glob scope relative to repo root. Can be provided multiple times.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    scope_globs = tuple(args.scope) if args.scope else DEFAULT_SCOPE_GLOBS
    ignore_patterns = load_ignore_patterns(repo_root)
    files = collect_files(args.files, repo_root, scope_globs, ignore_patterns)

    if not files:
        print("No scoped markdown files to lint.")
        return 0

    all_findings: List[Finding] = []
    for file_path in files:
        # Untagged fences in scoped docs are treated as DSL blocks by design.
        untagged_is_dsl = in_scope(file_path, repo_root, scope_globs)
        all_findings.extend(lint_file(file_path, untagged_is_dsl))

    if all_findings:
        for finding in all_findings:
            rel_path = finding.file_path.relative_to(repo_root)
            print(
                f"{rel_path}:{finding.line_number}: [{finding.rule_id}] {finding.message}\n"
                f"  -> {finding.line_text.strip()}"
            )
        print(f"\nSpec lint failed: {len(all_findings)} finding(s).")
        return 1

    print(f"Spec lint passed for {len(files)} file(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
