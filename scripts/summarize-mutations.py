#!/usr/bin/env python3
"""Compare PIT XML reports using stable mutation identities."""
import collections
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def load(stage):
    path = ROOT / "evidence" / f"pit-{stage}" / "mutations.xml"
    mutations = list(ET.parse(path).getroot())
    indexed = {}
    for mutation in mutations:
        key = tuple(mutation.findtext(field) for field in (
            "mutatedClass", "mutatedMethod", "methodDescription", "mutator"
        )) + (tuple(node.text for node in mutation.findall("indexes/index")),)
        if key in indexed:
            raise ValueError(f"Duplicate mutant identity in {path}: {key}")
        indexed[key] = mutation
    counts = collections.Counter(m.get("status") for m in mutations)
    return indexed, {
        "total": len(mutations),
        "statuses": dict(counts),
        "mutation_score_percent": round(100 * counts["KILLED"] / len(mutations), 2),
    }


stages = {stage: load(stage) for stage in ("original", "generated", "final")}
baseline = stages["original"][0]
for stage, (mutants, _) in stages.items():
    if mutants.keys() != baseline.keys():
        raise ValueError(f"Different mutation sets for stage {stage}")

summary = {stage: result[1] for stage, result in stages.items()}
(ROOT / "evidence" / "mutation-summary.json").write_text(
    json.dumps(summary, indent=2) + "\n", encoding="utf-8"
)
rows = [
    "| ID | Méthode : ligne | Mutation | Originaux | + IA | + Manuels | Test détecteur IA | Test détecteur final |",
    "|---|---|---|---|---|---|---|---|",
]
def test_name(killer):
    match = re.search(r"\[class:([^\]]+)\]/\[method:([^\]]+)\]", killer or "")
    return match.group(1).split(".")[-1] + "." + match.group(2) if match else killer or "—"

for i, (key, mutation) in enumerate(baseline.items(), 1):
    states = [stages[stage][0][key].get("status") for stage in stages]
    killer = test_name(stages["final"][0][key].findtext("killingTest"))
    generated_killer = test_name(stages["generated"][0][key].findtext("killingTest"))
    method = mutation.findtext('mutatedMethod')
    if method == 'read':
        method += '(byte[], int, int)' if mutation.findtext('methodDescription') == '([BII)I' else '()'
    values = [
        f"M{i:02}",
        f"`{method}` : {mutation.findtext('lineNumber')}",
        mutation.findtext("description"),
        *states,
        generated_killer, killer,
    ]
    rows.append("| " + " | ".join(str(v).replace("|", "\\|") for v in values) + " |")
(ROOT / "evidence" / "mutation-comparison.md").write_text("\n".join(rows) + "\n", encoding="utf-8")
print(json.dumps(summary, indent=2))
