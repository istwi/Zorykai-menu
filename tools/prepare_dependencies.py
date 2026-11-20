#!/usr/bin/env python3
"""Download only the pinned official Impactor compile dependency, with SHA-512 verification."""
import hashlib,json,urllib.request
from pathlib import Path
root=Path(__file__).resolve().parents[1]
version=json.loads((root/'evidence/impactor-version.json').read_text())
artifact=next(f for f in version['files'] if f['primary'])
target=root/'.cache/deps/impactor.jar';target.parent.mkdir(parents=True,exist_ok=True)
data=target.read_bytes() if target.exists() else urllib.request.urlopen(artifact['url'],timeout=120).read()
if hashlib.sha512(data).hexdigest()!=artifact['hashes']['sha512']:raise SystemExit('Impactor SHA-512 mismatch; preserved existing file for investigation')
if not target.exists():target.write_bytes(data)
print('Impactor 5.3.5+1.21.1 verified')

placeholder_url='https://github.com/Patbox/TextPlaceholderAPI/releases/download/2.4.2%2B1.21/placeholder-api-2.4.2%2B1.21.jar'
placeholder=root/'.cache/deps/placeholder-api.jar'
placeholder_data=placeholder.read_bytes() if placeholder.exists() else urllib.request.urlopen(placeholder_url,timeout=120).read()
if hashlib.sha256(placeholder_data).hexdigest()!='c0187ee299527ac7a3e0b1e83601dcdc0de631e286fe41e6be1a75958ea8e443':raise SystemExit('Text Placeholder API SHA-256 mismatch')
if not placeholder.exists():placeholder.write_bytes(placeholder_data)
print('Text Placeholder API 2.4.2+1.21 verified (optional runtime, compile dependency)')
