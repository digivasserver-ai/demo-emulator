#!/usr/bin/env python3
"""Extract the R8-minified class block that references the IGmsServiceBroker
protocol descriptor from dexdump output (dd.txt in cwd)."""
import re

want = "com.google.android.gms.common.internal.IGmsServiceBroker"
try:
    lines = open("dd.txt", encoding="utf-8", errors="replace").read().splitlines()
except FileNotFoundError:
    print("no dd.txt")
    raise SystemExit(0)

out = []
i = 0
while i < len(lines):
    ln = lines[i]
    if re.match(r"^ +Class descriptor", ln):
        cur = [ln]
        i += 1
        while i < len(lines) and not re.match(r"^ +Class descriptor", lines[i]):
            cur.append(lines[i])
            i += 1
        blob = "\n".join(cur)
        if want in blob:
            out.append(blob[-6000:])
    else:
        i += 1
print("\n---CLASS---\n".join(out[:3]) if out else "NO CLASS FOUND")