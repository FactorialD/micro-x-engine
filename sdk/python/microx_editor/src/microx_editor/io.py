"""Project confinement and crash-safe UTF-8 writes."""
from __future__ import annotations
import os
from pathlib import Path
import tempfile

class ProjectError(ValueError): pass

class Project:
    REQUIRED = ("res", "res/levels", "res/data")
    def __init__(self, root: str | Path):
        self.root = Path(root).expanduser().resolve(strict=True)
        if not self.root.is_dir(): raise ProjectError("Project path is not a directory")
        missing = [p for p in self.REQUIRED if not (self.root / p).is_dir()]
        if missing: raise ProjectError("Missing project directories: " + ", ".join(missing))
    def path(self, path: str | Path, *, existing=False) -> Path:
        candidate = Path(path)
        if not candidate.is_absolute(): candidate = self.root / candidate
        # resolve also follows a symlink in an existing parent
        resolved = candidate.resolve(strict=existing)
        try: resolved.relative_to(self.root)
        except ValueError: raise ProjectError(f"Path escapes selected project: {path}")
        return resolved
    def read_text(self, path: str | Path) -> str:
        return self.path(path, existing=True).read_text(encoding="utf-8")
    def atomic_write(self, path: str | Path, text: str) -> None:
        target = self.path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        # Recheck the created/possibly symlinked parent before opening a temp file.
        self.path(target.parent, existing=True)
        fd, name = tempfile.mkstemp(prefix="." + target.name + ".", suffix=".tmp", dir=target.parent)
        try:
            with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as stream:
                stream.write(text); stream.flush(); os.fsync(stream.fileno())
            os.replace(name, target)
        except BaseException:
            try: os.unlink(name)
            except FileNotFoundError: pass
            raise
