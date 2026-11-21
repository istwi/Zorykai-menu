#!/usr/bin/env python3
"""Select only the market fixture; legacy full fixtures require optional providers."""
import json,sys,zipfile
from pathlib import Path
source,target=map(Path,sys.argv[1:3])
if source.resolve()==target.resolve():raise ValueError('Use a different output path')
target.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(source) as src,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as out:
 for entry in src.infolist():
  data=src.read(entry.filename)
  if entry.filename=='fabric.mod.json':
   j=json.loads(data);j['entrypoints']['main']=['com.zorykai.menus.checks.MarketChecks'];data=json.dumps(j).encode()
  out.writestr(entry,data)
