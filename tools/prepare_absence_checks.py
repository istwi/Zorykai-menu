#!/usr/bin/env python3
"""Select only the absence probe entrypoint: the full test suite itself uses LuckPerms."""
from pathlib import Path
import json,sys,zipfile
source,target=map(Path,sys.argv[1:3])
with zipfile.ZipFile(source) as original,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as output:
    for name in original.namelist():
        data=original.read(name)
        if name=='fabric.mod.json':
            metadata=json.loads(data);metadata['entrypoints']['main']=['com.zorykai.menus.checks.AbsenceChecks'];data=json.dumps(metadata).encode()
        output.writestr(name,data)
