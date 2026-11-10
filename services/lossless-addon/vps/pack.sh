#!/usr/bin/env bash
# Builds aura-addon-vps.tar.gz: everything the VPS needs, in one folder.
#   bash services/lossless-addon/vps/pack.sh [output directory]
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$(dirname "$HERE")"
OUT="${1:-$PWD}"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
mkdir "$STAGE/aura-addon"
cp "$SRC"/server.mjs "$SRC"/worker.mjs "$SRC"/cache.mjs "$SRC"/pool.mjs "$SRC"/usage-server.mjs "$SRC"/auth.mjs "$SRC"/security.mjs "$SRC"/attestation.mjs "$SRC"/control.mjs "$SRC"/control.html "$SRC"/control.js "$SRC"/control.css "$STAGE/aura-addon/"
mkdir -p "$OUT" "$STAGE/aura-addon/usage-api/dashboard"
USAGE="$SRC/../../usage-api"
cp "$USAGE"/worker.mjs "$USAGE"/auth.mjs "$USAGE"/addons.mjs "$USAGE"/schema.sql "$STAGE/aura-addon/usage-api/"
cp "$USAGE"/dashboard/index.html "$USAGE"/dashboard/dashboard.js "$USAGE"/dashboard/dashboard.css "$USAGE"/dashboard/model.mjs "$USAGE"/dashboard/session.mjs "$STAGE/aura-addon/usage-api/dashboard/"
cp "$HERE"/import-usage.mjs "$STAGE/aura-addon/"
cp "$HERE"/install.sh "$HERE"/aura-addon.env "$HERE"/README.md "$STAGE/aura-addon/"
tar -czf "$OUT/aura-addon-vps.tar.gz" -C "$STAGE" aura-addon
echo "$OUT/aura-addon-vps.tar.gz"

python3 - "$STAGE" "$OUT" <<'PYZIP'
import pathlib,sys,zipfile,hashlib
stage,out=map(pathlib.Path,sys.argv[1:])
archive=out/'aura-hostinger.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for f in sorted((stage/'aura-addon').rglob('*')):
  if f.is_file(): z.write(f,f.relative_to(stage))
(out/'aura-hostinger.zip.sha256').write_text(hashlib.sha256(archive.read_bytes()).hexdigest()+'  '+archive.name+'\n')
print(archive)
PYZIP
