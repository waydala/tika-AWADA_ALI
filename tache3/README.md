# IFT3913 — Tâche 3 : tests de sécurité automatisés avec Semgrep

| Nom complet | Identifiant GitHub |
|---|---|
| Ali Awada | waydala |

- Dépôt : https://github.com/waydala/tika-AWADA_ALI (branche `tache3-awada-ali`)
- Cas d'étude : Apache Tika, modules `tika-core`, `tika-serialization`,
  `tika-parser-ocr-module`, `tika-parser-pdf-module`, `tika-parser-microsoft-module`
- Base de travail : commit amont `a2d75c2` (`umontreal-diro/tika`, `main`)

## Sommaire

1. [Modification du workflow GitHub Actions](#1-modification-du-workflow-github-actions)
2. [Choix de conception et d'implémentation](#2-choix-de-conception-et-dimplémentation)
3. [Validation de la modification](#3-validation-de-la-modification)
4. [Les 7 vulnérabilités introduites](#4-les-7-vulnérabilités-introduites)
5. [Comparaison des rapports Semgrep avant / après](#5-comparaison-des-rapports-semgrep-avant--après)
6. [Humour : rickroll quand un test échoue](#6-humour--rickroll-quand-un-test-échoue)
7. [Reproduire](#7-reproduire)
8. [Déclaration d'utilisation de l'IA](#8-déclaration-dutilisation-de-lia)

Historique de la branche (un commit par étape, pour que chaque étape soit
vérifiable séparément par le CI) :

| Commit | Contenu | Résultat attendu du job `semgrep` |
|---|---|---|
| A — `fef5e5d` | workflow modifié, action rickroll, scripts, rapport « avant » | succès (aucune nouvelle vulnérabilité) |
| B — `56af40e` | les 7 vulnérabilités + rapport « après » + comparaison | **échec** (9 nouveaux résultats) |
| C | cette documentation et les preuves de validation | succès (rien de nouveau depuis B) |

---

## 1. Modification du workflow GitHub Actions

Fichier modifié : [`.github/workflows/main-jdk17-build.yml`](../.github/workflows/main-jdk17-build.yml),
c'est-à-dire le workflow de build principal de Tika (compilation, `apache-rat`,
tests, installation, javadoc, puis tests end-to-end).

Changements :

1. **Nouveau job `semgrep`**, exécuté en premier :
   - `actions/checkout` avec `fetch-depth: 0` (historique complet) ;
   - installation de Semgrep **1.185.0** (version figée) ;
   - calcul du *commit de référence* (voir §2.2) ;
   - `semgrep scan --config p/java --config p/security-audit --baseline-commit <ref> --error`,
     qui ne signale **que les résultats absents du commit de référence** et
     sort avec le code 1 s'il y en a au moins un ;
   - résumé Markdown des nouveaux résultats dans le *job summary* et rapports
     JSON + SARIF publiés comme artefact `semgrep-new-findings`.
2. **`build` dépend de `semgrep`** (`needs: semgrep`) : si une nouvelle
   vulnérabilité est détectée, le job `semgrep` échoue, `build` et
   `e2e-tests` sont ignorés et **le workflow entier est en échec**.
3. **Déclencheurs** : en plus de `main`, le workflow tourne sur les branches
   `tache3-**` et peut être lancé à la main (`workflow_dispatch`).
4. **Rickroll** : étape `uses: ./.github/actions/rickroll` exécutée quand
   l'étape Maven de `build` ou de `e2e-tests` échoue (voir §6).
5. `permissions: contents: read` par défaut (moindre privilège) ;
   seul l'accès `pull-requests: write` est accordé aux jobs qui peuvent
   commenter une PR pour le rickroll.

Fichiers ajoutés ou modifiés pour le CI :

| Fichier | Rôle |
|---|---|
| `.github/workflows/main-jdk17-build.yml` | workflow modifié (job `semgrep`, dépendance, rickroll) |
| `.github/actions/rickroll/action.yml` | action composite réutilisable qui rickroll |
| `pom.xml` | exclusion `tache3/**` du contrôle de licences `apache-rat` (sinon le build échoue sur les rapports JSON/Markdown de ce dossier) |
| `tache3/scripts/semgrep-scan.sh` | scan complet des 5 modules (rapports avant/après) |
| `tache3/scripts/compare-reports.py` | comparaison des deux rapports |

## 2. Choix de conception et d'implémentation

### 2.1 « Nouvelle » vulnérabilité = analyse différentielle (*diff-aware*)

L'énoncé demande que le build échoue quand un commit introduit une
**nouvelle** vulnérabilité. Le code de Tika contient déjà 3 résultats Semgrep
(§5). Un simple `semgrep --error` sur tout le dépôt échouerait donc **à chaque
commit**, même inoffensif, et ne distinguerait pas l'ancien du nouveau.

J'ai utilisé l'option native `--baseline-commit` : Semgrep analyse le commit
courant **et** le commit de référence, puis ne garde que les résultats absents
de la référence. Avantages :

- les vulnérabilités historiques ne bloquent pas le build ;
- un commit qui *déplace* du code vulnérable existant ne le fait pas réapparaître
  comme nouveau (comparaison par empreinte de résultat, pas par numéro de ligne) ;
- seuls les fichiers modifiés depuis la référence sont analysés : le job est
  rapide (environ 1 minute) même sur un dépôt de la taille de Tika.

Alternatives écartées :

| Alternative | Pourquoi écartée |
|---|---|
| `semgrep --error` sur tout le dépôt | échoue sur les 3 résultats déjà présents : ne détecte pas « nouveau » |
| comparer deux rapports JSON complets dans le CI | réimplémente ce que `--baseline-commit` fait déjà, avec plus de risques d'erreur (décalage de lignes) |
| `returntocorp/semgrep-action` | action dépréciée ; et la version Semgrep Cloud demande un jeton/compte |
| workflow séparé | l'énoncé demande que le **build** échoue : en mettant le scan dans le même workflow avec `needs`, le build ne démarre même pas |

### 2.2 Choix du commit de référence

| Événement | Référence | Justification |
|---|---|---|
| `pull_request` | `git merge-base <base de la PR> HEAD` | tout ce que la PR apporte par rapport à la branche cible, sans compter ce qui a été ajouté entre-temps sur la cible |
| `push` | `github.event.before` | tous les commits du push, même si on en pousse plusieurs d'un coup |
| nouvelle branche (`before` = `000…0`) ou `workflow_dispatch` | `HEAD~1` | le dernier commit, faute d'état précédent connu |

Le `fetch-depth: 0` est indispensable : sans historique, Semgrep ne peut pas
extraire le commit de référence.

### 2.3 Règles et périmètre

- **Règles** : `p/java` (règles de sécurité Java de Semgrep) et
  `p/security-audit` (audit de sécurité plus large). Ce sont des jeux publics du
  registre Semgrep, utilisables sans compte (`--metrics=off`).
- **Périmètre** : tout le dépôt (un commit vulnérable hors des 5 modules doit
  aussi bloquer le build), **sauf** `**/src/test/**` : le code de test n'est pas
  livré et contient volontairement des choses « dangereuses » (fichiers XML
  malicieux, etc.) qui ne sont pas des vulnérabilités du produit.
- **Toutes les sévérités bloquent** (`--error`, y compris `WARNING`) : la
  plupart des règles crypto/TLS sont classées `WARNING` ; une politique qui ne
  bloque que les `ERROR` laisserait passer un TrustManager qui accepte tout.
- **Version figée** (`SEMGREP_VERSION: 1.185.0`) : un build reproductible ne
  doit pas changer de résultat parce qu'une nouvelle version de Semgrep est
  publiée. La même version a servi aux scans locaux.

### 2.4 Visibilité des résultats

Quand le job échoue, le développeur voit sans télécharger quoi que ce soit :
le log Semgrep (règle, fichier, ligne, explication), un tableau dans le
*job summary* et les rapports JSON/SARIF en artefact (le SARIF peut être
importé dans GitHub Code Scanning ou un IDE).

## 3. Validation de la modification

### 3.1 Validation locale (Semgrep 1.185.0, Ubuntu WSL)

J'ai rejoué la commande exacte du job `semgrep` sur chaque commit, dans un
worktree Git propre (`--baseline-commit` refuse de tourner avec des
modifications non committées). Sorties complètes dans
[`rapports/validation-locale/`](rapports/validation-locale/).

| Scénario | HEAD | Référence | Nouveaux résultats | Code de sortie | Attendu |
|---|---|---|---|---|---|
| commit A (CI seul) | `fef5e5d` | `a2d75c2` | 0 | **0** | passe ✅ |
| commit B (vulnérabilités) | `56af40e` | `fef5e5d` | 9 | **1** | échoue ✅ |

Le scénario A montre aussi que les **3 vulnérabilités déjà présentes** dans
Tika ne déclenchent pas d'échec : seules les nouvelles comptent.

### 3.2 Validation sur GitHub Actions

| Scénario | Run | Résultat |
|---|---|---|
| push du commit A | _lien ajouté après exécution_ | |
| push du commit B (vulnérabilités) | _lien ajouté après exécution_ | |
| push du commit C (documentation) | _lien ajouté après exécution_ | |
| test volontairement en échec (rickroll) | _lien ajouté après exécution_ | |

## 4. Les 7 vulnérabilités introduites

Chaque vulnérabilité est ajoutée comme une méthode utilitaire plausible dans
une classe existante, en lien avec la responsabilité de cette classe. Elles
sont repérables par le commentaire `IFT3913 tache 3 - vulnerabilite volontaire Vn`.
Le code compile et passe Checkstyle et forbiddenapis (les contrôles du build de
Tika). Les 7 types sont différents (7 CWE distinctes), et les 5 modules du cas
d'étude sont couverts.

| # | Type (CWE) | Module / fichier | Règle(s) Semgrep | Sévérité |
|---|---|---|---|---|
| V1 | Validation de certificat TLS désactivée (CWE-295) | tika-core / `parser/NetworkParser.java` | `insecure-trust-manager` ×2 | WARNING |
| V2 | Fonction de hachage cassée MD5 (CWE-328) | tika-core / `digest/DigestHelper.java` | `use-of-md5` | WARNING |
| V3 | Désérialisation de données non fiables (CWE-502) | tika-serialization / `serialization/ParseContextUtils.java` | `object-deserialization` | WARNING |
| V4 | XML External Entity — XXE (CWE-611) | pdf / `parser/pdf/XFAExtractor.java` | `documentbuilderfactory-disallow-doctype-decl-missing` | ERROR |
| V5 | Injection SQL (CWE-89) | microsoft / `parser/microsoft/JackcessExtractor.java` | `formatted-sql-string` | ERROR |
| V6 | Algorithme de chiffrement faible DES/ECB (CWE-327) | microsoft / `parser/microsoft/OfficeParser.java` | `ecb-cipher`, `des-is-deprecated` | WARNING |
| V7 | Aléa prévisible (CWE-330) | ocr / `parser/ocr/TesseractOCRParser.java` | `weak-random` | WARNING |

### V1 — TrustManager qui accepte tous les certificats (CWE-295)

**Fichier** : `tika-core/src/main/java/org/apache/tika/parser/NetworkParser.java`,
méthode `openTrustingHttpsConnection(URL)`.
**Pourquoi ce fichier** : `NetworkParser` envoie le document à un service de
parsing distant (`telnet://` ou HTTP). Ouvrir une connexion HTTPS vers ce
service est l'extension naturelle de la classe.

```java
X509TrustManager acceptAll = new X509TrustManager() {
    public void checkClientTrusted(X509Certificate[] chain, String authType) { }
    public void checkServerTrusted(X509Certificate[] chain, String authType) { }
    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
};
sslContext.init(null, new TrustManager[]{acceptAll}, new SecureRandom());
```

**Pourquoi c'est vulnérable** : `checkServerTrusted` ne lève jamais
d'exception, donc n'importe quel certificat (auto-signé, expiré, pour un autre
domaine) est accepté. Un attaquant en position d'intermédiaire (Wi-Fi public,
DNS empoisonné) peut se faire passer pour le serveur, **lire les documents
envoyés** et **renvoyer un contenu falsifié** que Tika traitera comme fiable.
Le chiffrement TLS ne sert plus à rien sans authentification du serveur.

**Résultat Semgrep** : `java.lang.security.audit.crypto.ssl.insecure-trust-manager`,
2 résultats (lignes 104 et 109 : une par méthode `check*Trusted` vide).
*Correctif* : utiliser le `TrustManagerFactory` par défaut ou un `KeyStore` dédié.

J'ai volontairement utilisé `SSLContext.getInstance("TLSv1.3")` : avec `"TLS"`,
Semgrep signale en plus `weak-ssl-context`, ce qui aurait mélangé deux
problèmes dans une seule vulnérabilité.

### V2 — MD5 comme identifiant de document (CWE-328)

**Fichier** : `tika-core/src/main/java/org/apache/tika/digest/DigestHelper.java`,
méthode `fastDocumentId(byte[])`.
**Pourquoi ce fichier** : `DigestHelper` calcule déjà les empreintes des
documents (SHA-256 configurable). Ajouter un identifiant « rapide » pour la
déduplication y est réaliste.

```java
MessageDigest md5 = MessageDigest.getInstance("MD5");
return HexFormat.of().formatHex(md5.digest(content));
```

**Pourquoi c'est vulnérable** : MD5 n'est plus résistant aux collisions ; on
peut fabriquer en quelques secondes deux fichiers différents de même MD5. Si
l'identifiant sert à dédupliquer ou à vérifier l'intégrité, un attaquant peut
faire passer un document malveillant pour un document déjà connu et validé.

**Résultat Semgrep** : `java.lang.security.audit.crypto.use-of-md5` (ligne 114),
avec un autofix proposé vers `SHA-512`.

### V3 — Désérialisation Java d'un flux non fiable (CWE-502)

**Fichier** : `tika-serialization/src/main/java/org/apache/tika/serialization/ParseContextUtils.java`,
méthode `fromJavaSerialization(InputStream)`.
**Pourquoi ce fichier** : le module `tika-serialization` sert justement à
reconstruire des `ParseContext` envoyés par un client (Tika Server, tika-pipes),
aujourd'hui en JSON. Ajouter un chemin « sérialisation Java native » y est
plausible.

```java
ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
return (ParseContext) objectInputStream.readObject();
```

**Pourquoi c'est vulnérable** : `readObject()` instancie **n'importe quelle
classe sérialisable du classpath** avant même le cast vers `ParseContext`. Le
classpath de Tika est très large (POI, PDFBox, Commons, Jackson…) ; un flux
contenant une chaîne de *gadgets* peut mener à l'exécution de code à distance.
Aucun `ObjectInputFilter` ne limite les classes acceptées.

**Résultat Semgrep** : `java.lang.security.audit.object-deserialization` (ligne 270).
*Correctif* : garder le JSON, ou définir un `ObjectInputFilter` en liste blanche.

### V4 — XXE dans l'analyse des formulaires XFA (CWE-611)

**Fichier** : `tika-parsers/.../tika-parser-pdf-module/src/main/java/org/apache/tika/parser/pdf/XFAExtractor.java`,
méthode `parseXfaToDom(InputStream)`.
**Pourquoi ce fichier** : un PDF peut contenir un formulaire XFA, qui est du
**XML contrôlé par l'auteur du PDF**. `XFAExtractor` le lit aujourd'hui avec le
`XMLInputFactory` durci de `XMLReaderUtils` ; la nouvelle méthode le charge en
DOM avec une fabrique par défaut.

```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setNamespaceAware(true);
return factory.newDocumentBuilder().parse(xfaIs);
```

**Pourquoi c'est vulnérable** : par défaut, `DocumentBuilderFactory` accepte
un `DOCTYPE` et résout les entités externes. Un PDF piégé contenant
`<!ENTITY x SYSTEM "file:///etc/passwd">` fait lire des fichiers locaux du
serveur (puis les extrait dans le texte du document), ou des URL internes
(SSRF), ou épuise la mémoire (*billion laughs*). C'est grave pour Tika, dont le
rôle est justement d'ouvrir des fichiers non fiables.

**Résultat Semgrep** : `java.lang.security.audit.xxe.documentbuilderfactory-disallow-doctype-decl-missing`
(ligne 88, sévérité **ERROR**), avec un autofix qui ajoute
`disallow-doctype-decl`.

### V5 — Injection SQL sur une table Access liée (CWE-89)

**Fichier** : `tika-parsers/.../tika-parser-microsoft-module/src/main/java/org/apache/tika/parser/microsoft/JackcessExtractor.java`,
méthode `queryLinkedTable(Connection, String)`.
**Pourquoi ce fichier** : `JackcessExtractor` extrait les tables des bases
Microsoft Access (`.mdb`/`.accdb`). Une table liée peut pointer vers une base
externe ; la lire par JDBC est une extension réaliste.

```java
Statement statement = connection.createStatement();
return statement.executeQuery("SELECT * FROM " + tableName);
```

**Pourquoi c'est vulnérable** : `tableName` vient du **document analysé**
(nom de table lié). Un nom comme `t; DROP TABLE users --` ou
`t UNION SELECT password FROM users` change le sens de la requête. Le nom de
table n'est ni validé ni échappé.

**Résultat Semgrep** : `java.lang.security.audit.formatted-sql-string`
(ligne 106, sévérité **ERROR**).
*Correctif* : `PreparedStatement` pour les valeurs, et liste blanche des noms
de tables lus dans les métadonnées (un nom de table ne peut pas être un
paramètre `?`).

### V6 — Chiffrement DES en mode ECB (CWE-327)

**Fichier** : `tika-parsers/.../tika-parser-microsoft-module/src/main/java/org/apache/tika/parser/microsoft/OfficeParser.java`,
méthode `legacyDecryptionCipher(byte[])`.
**Pourquoi ce fichier** : `OfficeParser` gère déjà le cas `ENCRYPTED` des
documents OLE2 (via `Decryptor` de POI) ; un chiffrement « hérité » y a sa
place.

```java
Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DES"));
```

**Pourquoi c'est vulnérable** : deux faiblesses dans la même ligne.
(1) **DES** a une clé de 56 bits, cassable par force brute ; le NIST l'a retiré
en 2005. (2) Le mode **ECB** chiffre chaque bloc indépendamment : deux blocs
identiques en clair donnent le même bloc chiffré (les motifs restent
visibles), et aucune intégrité n'est fournie.

**Résultat Semgrep** : 2 règles sur la ligne 400 —
`java.lang.security.audit.crypto.ecb-cipher` et
`java.lang.security.audit.crypto.des-is-deprecated`.
*Correctif* : `AES/GCM/NoPadding` avec un IV aléatoire.

### V7 — `java.util.Random` pour un nom de fichier temporaire (CWE-330)

**Fichier** : `tika-parsers/.../tika-parser-ocr-module/src/main/java/org/apache/tika/parser/ocr/TesseractOCRParser.java`,
méthode `newTemporaryImageName()`.
**Pourquoi ce fichier** : le parseur OCR écrit des images temporaires qu'il
passe au programme externe `tesseract`.

```java
return OCR + Long.toHexString(new Random().nextLong()) + ".png";
```

**Pourquoi c'est vulnérable** : `java.util.Random` est un générateur
congruentiel linéaire de 48 bits. Quelques sorties suffisent pour retrouver
l'état interne et prédire les suivantes. Un nom de fichier temporaire
prévisible permet à un utilisateur local de créer le fichier avant Tika
(lien symbolique, course de type TOCTOU) et de remplacer l'image analysée ou
d'écraser un autre fichier.

**Résultat Semgrep** : `java.lang.security.audit.crypto.weak-random` (ligne 251).
*Correctif* : `Files.createTempFile` (nom aléatoire et création atomique) ou
`SecureRandom`.

### Type écarté : injection de commande

L'injection de commande (`Runtime.exec("tesseract " + image)`) aurait été très
naturelle dans le parseur OCR. Je l'ai testée : la règle correspondante
(`command-injection-process-builder`) existe dans le registre Semgrep mais ne
fait partie **ni de `p/java` ni de `p/security-audit`**, donc le CI ne la
détecterait pas. Pour que chaque vulnérabilité documentée soit vraiment
bloquée par le workflow, j'ai choisi des types couverts par les règles du CI.

## 5. Comparaison des rapports Semgrep avant / après

Les deux rapports sont des **scans complets** (pas différentiels) des 5
modules du cas d'étude, avec les mêmes règles que le CI, code de test exclu :

- avant : [`rapports/semgrep-avant.json`](rapports/semgrep-avant.json) — commit A, **3 résultats** ;
- après : [`rapports/semgrep-apres.json`](rapports/semgrep-apres.json) — commit B, **12 résultats** ;
- comparaison générée : [`rapports/comparaison.md`](rapports/comparaison.md).

Le script `compare-reports.py` identifie un résultat par
(règle, fichier, extrait de code normalisé) plutôt que par numéro de ligne,
parce que l'ajout de code dans un fichier décale les lignes des résultats
existants.

**Résultats déjà présents dans Tika (avant et après, inchangés)** :

| Règle | Emplacement |
|---|---|
| `saxparserfactory-disallow-doctype-decl-missing` | `tika-core/.../mime/MimeTypesReader.java:223` |
| `unencrypted-socket` | `tika-core/.../parser/NetworkParser.java:68` |
| `xmlinputfactory-possible-xxe` | `tika-core/.../utils/XMLReaderUtils.java:307` |

Ce sont des alertes d'audit : Tika configure ces fabriques XML ailleurs, et le
socket correspond au protocole `telnet://` voulu par `NetworkParser`. Elles ne
sont pas introduites par mon travail, et le CI ne bloque pas dessus.

**Résultats nouveaux (après − avant = 9)** :

| # | Vulnérabilité | Règle Semgrep | Fichier:ligne | Sévérité |
|---|---|---|---|---|
| 1 | V2 | `use-of-md5` | `DigestHelper.java:114` | WARNING |
| 2 | V1 | `insecure-trust-manager` | `NetworkParser.java:104` | WARNING |
| 3 | V1 | `insecure-trust-manager` | `NetworkParser.java:109` | WARNING |
| 4 | V5 | `formatted-sql-string` | `JackcessExtractor.java:106` | ERROR |
| 5 | V6 | `ecb-cipher` | `OfficeParser.java:400` | WARNING |
| 6 | V6 | `des-is-deprecated` | `OfficeParser.java:400` | WARNING |
| 7 | V7 | `weak-random` | `TesseractOCRParser.java:251` | WARNING |
| 8 | V4 | `documentbuilderfactory-disallow-doctype-decl-missing` | `XFAExtractor.java:88` | ERROR |
| 9 | V3 | `object-deserialization` | `ParseContextUtils.java:270` | WARNING |

Les 7 vulnérabilités sont toutes détectées : 9 résultats, parce que V1 en
produit deux (une par méthode vide) et V6 en produit deux (DES et ECB).
Aucun résultat n'a disparu. Ces 9 résultats sont exactement ceux du scan
différentiel du CI sur le commit B (§3.1) : les deux méthodes concordent.

## 6. Humour : rickroll quand un test échoue

Action composite réutilisable : [`.github/actions/rickroll/action.yml`](../.github/actions/rickroll/action.yml),
inspirée de [random-rickroll](https://github.com/TejasvOnly/random-rickroll).

- **Quand** : appelée par `build` et `e2e-tests` avec
  `if: failure() && steps.<maven>.outcome == 'failure'`, c'est-à-dire
  seulement quand l'étape Maven (qui exécute les tests de Tika) échoue, pas
  pour une panne d'installation.
- **Quoi** : elle choisit au hasard un « déguisement » de lien parmi 5 textes
  crédibles (« Rapport Surefire détaillé des tests en échec »,
  « Stack trace complète »…). Le lien est écrit dans le *job summary* avec un
  GIF et ajouté comme annotation d'erreur ; sur une PR, elle publie aussi un
  commentaire. Tous ces liens mènent à *Never Gonna Give You Up*.
- **Réutilisable** : paramètres `reason` et `github-token`, sortie `disguise` ;
  n'importe quel job ou workflow peut l'appeler avec `uses: ./.github/actions/rickroll`.
- **Sans risque pour le CI** : l'étape n'est exécutée qu'après un échec, donc
  elle ne peut pas faire échouer un build qui passe ; le commentaire PR est
  facultatif (`|| echo …` si le jeton n'a pas le droit d'écrire).

Validation : une branche de démonstration `tache3-demo-rickroll` ajoute un
test JUnit qui échoue volontairement dans `tika-core` ; le run GitHub Actions
correspondant est lié au §3.2. Cette branche n'est pas fusionnée.

## 7. Reproduire

```bash
python3 -m venv ~/semgrep-venv && ~/semgrep-venv/bin/pip install semgrep==1.185.0
export SEMGREP=~/semgrep-venv/bin/semgrep

# scans complets avant / après et comparaison
git checkout fef5e5d && tache3/scripts/semgrep-scan.sh /tmp/avant.json
git checkout 56af40e && tache3/scripts/semgrep-scan.sh /tmp/apres.json
python3 tache3/scripts/compare-reports.py /tmp/avant.json /tmp/apres.json /tmp/comparaison.md

# la porte du CI sur le commit B (doit sortir avec le code 1)
$SEMGREP scan --config p/java --config p/security-audit --metrics=off \
  --exclude '**/src/test/**' --baseline-commit fef5e5d --error
```

## 8. Déclaration d'utilisation de l'IA

Conformément aux consignes de l'Université de Montréal
(*Intelligence artificielle générative : citer, signaler, déclarer et documenter*) :

- **Outil** : Claude (Anthropic), via Claude Code, novembre 2026.
- **Utilisation** : aide à la conception du workflow (choix de
  `--baseline-commit` et du commit de référence), rédaction du YAML et de
  l'action rickroll, écriture des scripts de scan et de comparaison, choix des
  emplacements et rédaction du code des 7 vulnérabilités, et rédaction de ce
  rapport.
- **Vérification humaine** : les scans Semgrep, la compilation (Checkstyle,
  forbiddenapis) et la validation de la porte ont été exécutés et leurs sorties
  sont conservées dans `tache3/rapports/`. Les résultats rapportés ici viennent
  de ces exécutions, pas du modèle.
