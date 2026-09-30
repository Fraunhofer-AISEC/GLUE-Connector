#!/usr/bin/env python3
"""Add license and per-file contributor headers derived from Git history."""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class CommentStyle:
    name: str
    first: str
    middle: str
    last: str | None = None

    def render(self, contributors: list[str], newline: str, license_lines: list[str]) -> str:
        def comment_line(line: str) -> str:
            return f"{self.middle}{line}" if line else self.middle.rstrip()

        lines = [self.first]
        if self.last is not None:
            if license_lines:
                lines.extend(comment_line(line) for line in license_lines)
                lines.append(comment_line(""))
            lines.append(f"{self.middle}Contributors:")
        elif license_lines:
            lines = [comment_line(line) for line in license_lines]
            lines.extend([comment_line(""), self.first])
        lines.extend(f"{self.middle}- {contributor}" for contributor in contributors)
        if self.last is not None:
            lines.append(self.last)
        return newline.join(lines) + newline


C_BLOCK = CommentStyle("C-style block", "/*", " * ", " */")
HASH = CommentStyle("hash", "# Contributors:", "# ")
SLASH = CommentStyle("slash", "// Contributors:", "// ")
DASH = CommentStyle("SQL", "-- Contributors:", "-- ")
XML = CommentStyle("XML", "<!--", "  ", "-->")
BAT = CommentStyle("batch", "@rem Contributors:", "@rem ")

C_BLOCK_SUFFIXES = {
    ".c", ".cc", ".cpp", ".css", ".cxx", ".gradle", ".groovy", ".h",
    ".hh", ".hpp", ".hxx", ".java", ".js", ".jsx", ".kt", ".kts",
    ".m", ".mm", ".rs", ".scss", ".swift", ".ts", ".tsx",
}
HASH_SUFFIXES = {
    ".bash", ".conf", ".env", ".j2", ".pl", ".properties", ".py",
    ".rb", ".sh", ".toml", ".yaml", ".yml", ".zsh",
}
XML_SUFFIXES = {".htm", ".html", ".xhtml", ".xml"}


class ToolError(RuntimeError):
    pass


def git(repo: Path, *args: str, check: bool = True) -> subprocess.CompletedProcess[bytes]:
    result = subprocess.run(
        ["git", "-C", os.fspath(repo), *args],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if check and result.returncode != 0:
        detail = result.stderr.decode("utf-8", "replace").strip()
        raise ToolError(f"git {' '.join(args)} failed in {repo}: {detail}")
    return result


def repository_root(path: Path) -> Path:
    result = git(path, "rev-parse", "--show-toplevel")
    return Path(os.fsdecode(result.stdout.rstrip(b"\n"))).resolve()


def tracked_entries(repo: Path) -> list[tuple[str, str]]:
    output = git(repo, "ls-files", "--stage", "-z").stdout
    entries: list[tuple[str, str]] = []
    for record in output.split(b"\0"):
        if not record:
            continue
        metadata, raw_path = record.split(b"\t", 1)
        mode = metadata.split(b" ", 1)[0].decode("ascii")
        entries.append((mode, os.fsdecode(raw_path)))
    return entries


def discover_repositories(start: Path) -> list[Path]:
    root = repository_root(start)
    repositories: list[Path] = []
    pending = [root]
    seen: set[Path] = set()
    while pending:
        repo = pending.pop(0)
        if repo in seen:
            continue
        seen.add(repo)
        repositories.append(repo)
        for mode, relative in tracked_entries(repo):
            if mode != "160000":
                continue
            candidate = (repo / relative).resolve()
            probe = git(candidate, "rev-parse", "--show-toplevel", check=False)
            if probe.returncode == 0:
                pending.append(repository_root(candidate))
    return repositories


def comment_style(path: Path, text: str) -> CommentStyle | None:
    name = path.name.lower()
    suffix = path.suffix.lower()
    if suffix in C_BLOCK_SUFFIXES:
        return C_BLOCK
    if suffix == ".go":
        return C_BLOCK
    if suffix in {".mod", ".work"}:
        return SLASH
    #if suffix in HASH_SUFFIXES or name in {".gitignore", ".gitmodules", ".mailmap"}:
    #    return HASH
    if suffix in XML_SUFFIXES:
        return XML
    if suffix == ".sql":
        return DASH
    #if suffix in {".bat", ".cmd"}:
    #    return BAT
    #if suffix == ".dockerfile" or name == "dockerfile":
    #    return HASH
    if text.startswith("#!"):
        first_line = text.splitlines()[0].lower()
        if any(runtime in first_line for runtime in ("node", "deno", "bun")):
            return SLASH
        return HASH
    return None


def insertion_offset(text: str, path: Path, style: CommentStyle) -> int:
    """Return the position after syntax that is required to remain first."""
    offset = 0

    if style is XML and text.startswith("<?xml"):
        end = text.find("?>")
        if end >= 0:
            offset = end + 2
            if text.startswith("\r\n", offset):
                offset += 2
            elif text.startswith("\n", offset):
                offset += 1
        return offset

    lines = text.splitlines(keepends=True)
    index = 0
    if lines and lines[0].startswith("#!"):
        offset += len(lines[0])
        index = 1

    if path.suffix.lower() == ".py" and index < len(lines):
        if re.match(r"^[ \t]*#.*coding[:=][ \t]*[-\w.]+", lines[index]):
            offset += len(lines[index])
            index += 1

    if path.suffix.lower() in {".yaml", ".yml"}:
        while index < len(lines) and lines[index].startswith("%"):
            offset += len(lines[index])
            index += 1
        if index < len(lines) and lines[index].strip() == "---":
            offset += len(lines[index])
            index += 1

    if path.suffix.lower() == ".md" and index == 0 and lines and lines[0].strip() == "---":
        offset = len(lines[0])
        for line in lines[1:]:
            offset += len(line)
            if line.strip() in {"---", "..."}:
                break

    if style is HASH and (path.suffix.lower() == ".dockerfile" or path.name.lower() == "dockerfile"):
        directive = re.compile(r"^#\s*(syntax|escape|check)\s*=", re.IGNORECASE)
        offset = 0
        for line in lines:
            if directive.match(line):
                offset += len(line)
            else:
                break

    return offset


def remove_generated_header(text: str, offset: int, style: CommentStyle) -> tuple[str, bool]:
    body = text[offset:]
    patterns = {
        C_BLOCK.name: r"/\*\r?\n(?: \*(?: [^\r\n]*)?\r?\n)* \* Contributors:\r?\n(?: \* - [^\r\n]*\r?\n)+ \*/",
        HASH.name: r"(?:#(?: [^\r\n]*)?\r?\n)*# Contributors:\r?\n(?:# - [^\r\n]*\r?\n)+",
        SLASH.name: r"(?://(?: [^\r\n]*)?\r?\n)*// Contributors:\r?\n(?:// - [^\r\n]*\r?\n)+",
        DASH.name: r"(?:--(?: [^\r\n]*)?\r?\n)*-- Contributors:\r?\n(?:-- - [^\r\n]*\r?\n)+",
        XML.name: r"<!--\r?\n(?:(?:  [^\r\n]*)?\r?\n)*  Contributors:\r?\n(?:  - [^\r\n]*\r?\n)+-->",
        BAT.name: r"(?:@rem(?: [^\r\n]*)?\r?\n)*@rem Contributors:\r?\n(?:@rem - [^\r\n]*\r?\n)+",
    }
    match = re.match(patterns[style.name], body)
    if not match:
        return text, False
    end = match.end()
    if body.startswith("\r\n", end):
        end += 2
    elif body.startswith("\n", end):
        end += 1
    return text[:offset] + body[end:], True


def contributors_for(repo: Path, relative: str) -> list[str]:
    """Rank file authors by added plus deleted lines, with alphabetical ties."""
    output = git(
        repo,
        "log",
        "--follow",
        "--find-renames=20%",
        "--use-mailmap",
        "--format=%x1e%aN%x1f%aE",
        "--numstat",
        "-z",
        "--",
        relative,
    ).stdout
    by_email: dict[str, tuple[str, str, int]] = {}
    for commit in output.split(b"\x1e"):
        if not commit:
            continue
        header, _, changes = commit.partition(b"\0")
        if b"\x1f" not in header:
            continue
        name, email = (
            part.decode("utf-8", "replace").strip()
            for part in header.split(b"\x1f", 1)
        )
        name = " ".join(name.split())
        email = "".join(email.split())
        if name and email:
            total = 0
            for entry in changes.split(b"\0"):
                counts = entry.lstrip(b"\n").split(b"\t", 2)
                if len(counts) < 3:
                    continue
                added, deleted = counts[:2]
                if added.isdigit() and deleted.isdigit():
                    total += int(added) + int(deleted)
            key = email.casefold()
            previous = by_email.get(key)
            if previous is None:
                by_email[key] = (name, email, total)
            else:
                by_email[key] = (previous[0], previous[1], previous[2] + total)
    identities = list(by_email.values())
    identities.sort(key=lambda item: (-item[2], item[0].casefold(), item[1].casefold()))
    return [f"{name} <{email}>" for name, email, _ in identities]


def updated_contents(
    path: Path, raw: bytes, contributors: list[str], license_lines: list[str]
) -> bytes | None:
    has_bom = raw.startswith(b"\xef\xbb\xbf")
    try:
        text = raw.decode("utf-8-sig" if has_bom else "utf-8")
    except UnicodeDecodeError as error:
        raise ToolError(f"{path}: supported file type is not UTF-8 ({error})") from error
    style = comment_style(path, text)
    if style is None:
        return None
    newline = "\r\n" if "\r\n" in text and text.count("\r\n") >= text.count("\n") / 2 else "\n"
    offset = insertion_offset(text, path, style)
    text_without_header, _ = remove_generated_header(text, offset, style)
    remainder = text_without_header[offset:]
    if remainder.startswith("\r\n"):
        remainder = remainder[2:]
    elif remainder.startswith("\n"):
        remainder = remainder[1:]
    header = style.render(contributors, newline, license_lines)
    updated = text_without_header[:offset] + header + newline + remainder
    encoded = updated.encode("utf-8")
    return (b"\xef\xbb\xbf" + encoded) if has_bom else encoded


def process_repository(
    repo: Path, *, write: bool, verbose: bool, license_lines: list[str]
) -> tuple[int, int]:
    changed = 0
    skipped_without_history = 0
    for mode, relative in tracked_entries(repo):
        if not mode.startswith("100"):
            continue
        path = repo / relative
        raw = path.read_bytes()
        try:
            preview = raw.decode("utf-8-sig")
        except UnicodeDecodeError:
            preview = ""
        if comment_style(path, preview) is None:
            continue
        contributors = contributors_for(repo, relative)
        if not contributors:
            skipped_without_history += 1
            if verbose:
                print(f"SKIP (no history) {path}")
            continue
        replacement = updated_contents(path, raw, contributors, license_lines)
        if replacement is None or replacement == raw:
            if verbose:
                print(f"OK   {path}")
            continue
        changed += 1
        print(f"{'WRITE' if write else 'WOULD WRITE'} {path}")
        if write:
            path.write_bytes(replacement)
    return changed, skipped_without_history


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Add license and contributor comments to tracked, comment-capable files."
    )
    action = parser.add_mutually_exclusive_group()
    action.add_argument("--dry-run", action="store_true", help="show changes without writing files")
    action.add_argument("--check", action="store_true", help="show pending changes and exit 1 if any exist")
    parser.add_argument(
        "--license-file",
        type=Path,
        help="UTF-8 plain text to place above Contributors in each header",
    )
    parser.add_argument("--verbose", action="store_true", help="also list unchanged and history-free files")
    parser.add_argument(
        "repositories",
        nargs="*",
        type=Path,
        help="Git repositories to process; defaults to the current repository and initialized submodules",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        license_lines: list[str] = []
        if args.license_file is not None:
            license_text = args.license_file.read_text(encoding="utf-8-sig")
            if "\0" in license_text:
                raise ToolError(f"{args.license_file}: license text contains a NUL byte")
            license_lines = license_text.splitlines()
            while license_lines and not license_lines[0].strip():
                license_lines.pop(0)
            while license_lines and not license_lines[-1].strip():
                license_lines.pop()
            if not license_lines:
                raise ToolError(f"{args.license_file}: license text is empty")
            license_lines = [line if line.strip() else "" for line in license_lines]
            if any("*/" in line for line in license_lines):
                raise ToolError(f"{args.license_file}: license text contains the C comment terminator */")
            if any("--" in line for line in license_lines):
                raise ToolError(f"{args.license_file}: license text contains --, which is invalid in XML comments")
        if args.repositories:
            repositories = []
            seen: set[Path] = set()
            for requested in args.repositories:
                repo = repository_root(requested.resolve())
                if repo not in seen:
                    repositories.append(repo)
                    seen.add(repo)
        else:
            repositories = discover_repositories(Path.cwd())
        total_changed = 0
        total_without_history = 0
        for repo in repositories:
            print(f"Repository: {repo}")
            changed, without_history = process_repository(
                repo,
                write=not (args.dry_run or args.check),
                verbose=args.verbose,
                license_lines=license_lines,
            )
            total_changed += changed
            total_without_history += without_history
        print(
            f"{total_changed} file(s) {'would be changed' if args.dry_run or args.check else 'changed'}; "
            f"{total_without_history} supported file(s) skipped because they have no Git history"
        )
        return 1 if args.check and total_changed else 0
    except (OSError, ToolError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
