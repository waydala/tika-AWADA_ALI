# Comparaison des rapports Semgrep (avant / après)

- Rapport avant : `tache3/rapports/semgrep-avant.json` — **3** résultats
- Rapport après : `tache3/rapports/semgrep-apres.json` — **12** résultats
- Nouveaux résultats : **9**
- Résultats disparus : **0**

## Nouveaux résultats (introduits par les modifications)

| # | Règle Semgrep | Sévérité | Fichier:ligne | CWE |
|---|---|---|---|---|
| 1 | `use-of-md5` | WARNING | `tika-core/src/main/java/org/apache/tika/digest/DigestHelper.java:114` | CWE-328 |
| 2 | `insecure-trust-manager` | WARNING | `tika-core/src/main/java/org/apache/tika/parser/NetworkParser.java:104` | CWE-295 |
| 3 | `insecure-trust-manager` | WARNING | `tika-core/src/main/java/org/apache/tika/parser/NetworkParser.java:109` | CWE-295 |
| 4 | `formatted-sql-string` | ERROR | `tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-microsoft-module/src/main/java/org/apache/tika/parser/microsoft/JackcessExtractor.java:106` | CWE-89 |
| 5 | `ecb-cipher` | WARNING | `tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-microsoft-module/src/main/java/org/apache/tika/parser/microsoft/OfficeParser.java:400` | CWE-327 |
| 6 | `des-is-deprecated` | WARNING | `tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-microsoft-module/src/main/java/org/apache/tika/parser/microsoft/OfficeParser.java:400` | CWE-326 |
| 7 | `weak-random` | WARNING | `tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-ocr-module/src/main/java/org/apache/tika/parser/ocr/TesseractOCRParser.java:251` | CWE-330 |
| 8 | `documentbuilderfactory-disallow-doctype-decl-missing` | ERROR | `tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-pdf-module/src/main/java/org/apache/tika/parser/pdf/XFAExtractor.java:88` | CWE-611 |
| 9 | `object-deserialization` | WARNING | `tika-serialization/src/main/java/org/apache/tika/serialization/ParseContextUtils.java:270` | CWE-502 |

## Nombre de résultats par règle

| Règle Semgrep | Avant | Après | Δ |
|---|---|---|---|
| `des-is-deprecated` | 0 | 1 | **+1** |
| `ecb-cipher` | 0 | 1 | **+1** |
| `insecure-trust-manager` | 0 | 2 | **+2** |
| `unencrypted-socket` | 1 | 1 | 0 |
| `use-of-md5` | 0 | 1 | **+1** |
| `weak-random` | 0 | 1 | **+1** |
| `formatted-sql-string` | 0 | 1 | **+1** |
| `object-deserialization` | 0 | 1 | **+1** |
| `documentbuilderfactory-disallow-doctype-decl-missing` | 0 | 1 | **+1** |
| `saxparserfactory-disallow-doctype-decl-missing` | 1 | 1 | 0 |
| `xmlinputfactory-possible-xxe` | 1 | 1 | 0 |
