#!/usr/bin/env python3
"""IFT3913 - tâche 3 : compare deux rapports Semgrep (avant / après).

Usage : python3 tache3/scripts/compare-reports.py avant.json apres.json sortie.md

Un résultat est identifié par (règle, fichier, extrait de code normalisé) :
cette clé ne dépend pas des numéros de ligne, qui se décalent quand on
ajoute du code dans un fichier déjà analysé. Les doublons sont comptés
(multi-ensemble), si bien qu'un deuxième appel vulnérable identique dans un
même fichier est bien reconnu comme nouveau.
"""
import collections
import json
import sys


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)["results"]


def key(r):
    return (r["check_id"], r["path"], r.get("snippet", ""))


def short(rule):
    return rule.rsplit(".", 1)[-1]


def main():
    before_path, after_path, out_path = sys.argv[1:4]
    before, after = load(before_path), load(after_path)

    remaining = collections.Counter(key(r) for r in before)
    new = []
    for r in after:
        k = key(r)
        if remaining[k] > 0:
            remaining[k] -= 1
        else:
            new.append(r)
    fixed = sum(remaining.values())

    by_rule_before = collections.Counter(r["check_id"] for r in before)
    by_rule_after = collections.Counter(r["check_id"] for r in after)

    lines = [
        "# Comparaison des rapports Semgrep (avant / après)",
        "",
        f"- Rapport avant : `{before_path}` — **{len(before)}** résultats",
        f"- Rapport après : `{after_path}` — **{len(after)}** résultats",
        f"- Nouveaux résultats : **{len(new)}**",
        f"- Résultats disparus : **{fixed}**",
        "",
        "## Nouveaux résultats (introduits par les modifications)",
        "",
        "| # | Règle Semgrep | Sévérité | Fichier:ligne | CWE |",
        "|---|---|---|---|---|",
    ]
    for i, r in enumerate(sorted(new, key=lambda r: (r["path"], r["start"]["line"])), 1):
        cwe = r["extra"].get("metadata", {}).get("cwe", "")
        if isinstance(cwe, list):
            cwe = cwe[0] if cwe else ""
        cwe = str(cwe).split(":")[0]
        lines.append(
            f"| {i} | `{short(r['check_id'])}` | {r['extra']['severity']} | "
            f"`{r['path']}:{r['start']['line']}` | {cwe} |"
        )

    lines += [
        "",
        "## Nombre de résultats par règle",
        "",
        "| Règle Semgrep | Avant | Après | Δ |",
        "|---|---|---|---|",
    ]
    for rule in sorted(set(by_rule_before) | set(by_rule_after)):
        b, a = by_rule_before[rule], by_rule_after[rule]
        delta = f"**+{a - b}**" if a > b else str(a - b)
        lines.append(f"| `{short(rule)}` | {b} | {a} | {delta} |")
    lines.append("")

    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"{len(new)} nouveaux résultats, {fixed} disparus -> {out_path}")


if __name__ == "__main__":
    main()
