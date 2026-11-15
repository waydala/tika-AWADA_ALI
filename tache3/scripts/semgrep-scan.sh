#!/usr/bin/env bash
# IFT3913 - tâche 3 : scan Semgrep complet des modules du cas d'étude.
#
# Usage : tache3/scripts/semgrep-scan.sh <rapport.json>   (depuis la racine du dépôt)
#
# Les règles utilisées sont les mêmes que celles du job `semgrep` de
# .github/workflows/main-jdk17-build.yml (p/java + p/security-audit).
# Le code de test est exclu : on ne s'intéresse qu'au code livré.
set -euo pipefail

out="${1:?usage: $0 <rapport.json>}"
semgrep_bin="${SEMGREP:-semgrep}"

modules=(
  tika-core
  tika-serialization
  tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-ocr-module
  tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-pdf-module
  tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-microsoft-module
)

mkdir -p "$(dirname "$out")"
"$semgrep_bin" scan \
  --config p/java \
  --config p/security-audit \
  --metrics=off \
  --exclude '**/src/test/**' \
  --json --output "$out" \
  --quiet \
  "${modules[@]}"

# Ajoute l'extrait de code de chaque résultat (le champ extra.lines exige une
# connexion à semgrep.dev en version OSS), pour rendre le rapport lisible et
# permettre une comparaison avant/après indépendante des numéros de ligne.
python3 - "$out" <<'PY'
import json, sys
path = sys.argv[1]
with open(path, encoding="utf-8") as f:
    data = json.load(f)
for r in data["results"]:
    try:
        with open(r["path"], encoding="utf-8", errors="replace") as src:
            text = src.read()
        snippet = text[r["start"]["offset"]:r["end"]["offset"]]
    except OSError:
        snippet = ""
    r["snippet"] = " ".join(snippet.split())[:300]
with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=1, ensure_ascii=False)
print(f"{len(data['results'])} résultats -> {path}")
PY
