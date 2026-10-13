# IFT3913 — Tâche 2 — Ali Awada

Ali Awada — [waydala](https://github.com/waydala).
Remise individuelle, sans binôme.

Ce travail augmente les tests de `org.apache.tika.io.LookaheadInputStream`,
dans `tika-core` du [cas d'étude du cours](https://github.com/umontreal-diro/tika).
La base est le commit `a2d75c2c10563b261148e9bafe4046832b33c1a6`.
Le code de production est conservé sans modification.
Les tests ChatUniTest ont été **générés sur cet ordinateur avec Qwen dans Ollama**.
Les mesures ci-dessous ont été exécutées **localement sous Windows 11, le 13 octobre 2026**.

| Suite | Nouveaux tests cumulés | Lignes | Branches | Mutants tués | Survivants | Non couverts | Score |
|---|---:|---:|---:|---:|---:|---:|---:|
| Originale | 0 | 32/40 — 80 % | 14/16 — 87,5 % | 19/33 | 5 | 9 | 57,58 % |
| Originale + ChatUniTest corrigé | 8 | 40/40 — 100 % | 16/16 — 100 % | 27/33 | 6 | 0 | 81,82 % |
| Originale + ChatUniTest + complémentaires | 13 | 40/40 — 100 % | 16/16 — 100 % | 33/33 | 0 | 0 | **100 %** |

La suite finale du module recense **762 tests : 759 réussis, 3 ignorés,
aucun échec ni erreur**. Les **13 nouveaux tests passent tous**.
Le score porte sur les 33 mutants PIT `DEFAULTS` de cette classe;
il ne prouve pas l'absence de tous les défauts.

## 1. Classe et méthodes choisies

`LookaheadInputStream` limite la lecture à une fenêtre de `n` octets, permet
la relecture avec `mark()`/`reset()` et restaure la position du délégué.
Ses comportements sont observables avec de petits tableaux d'octets, sans réseau.

Les **six tests existants** dans
[LookaheadInputStreamTest.java](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStreamTest.java)
couvrent flux nul, flux vide, lecture limitée, fenêtre nulle, marque et saut.
La suite originale complète laisse les mêmes lacunes que ces six tests.

| JaCoCo initial | Couvert / total |
|---|---:|
| Lignes | 32/40 |
| Branches | 14/16 |
| Instructions | 129/164 |
| Méthodes | 8/10 |

Les deux méthodes **non couvertes**, `markSupported()` et
`read(byte[], int, int)`, sont les cibles principales de génération.
Elles comportent neuf mutants `NO_COVERAGE` au départ.
Le goal ChatUniTest `method` sur `read` traite les deux surcharges;
la sortie comprend donc aussi la lecture scalaire.
Une génération sur `close()` complète la comparaison d'oracles : cette méthode
est déjà couverte, mais la marque initiale non nulle du délégué et la fermeture
automatique à EOF échappent aux tests.

PIT confirme **cinq survivants** : M01–M02 (marque du constructeur),
M07 (frontière de remplissage), M08 (capacité restante) et M14 (restauration à EOF).
La classe a donc des tests existants, une couverture inférieure à 100 %
et des mutants vivants.

Preuves : [suite originale](evidence/original-suite.log),
[JaCoCo initial](evidence/coverage-original/jacoco.xml),
[PIT initial](evidence/pit-original/index.html),
[mutants initiaux](evidence/pit-original/mutations.xml).

## 2. ChatUniTest dans Maven et modèle local

Le [pom de tika-core](tika-core/pom.xml) contient le profil `tache2-generation`
avec **ChatUniTest 2.1.1** et **chatunitest-starter 1.4.0**, suivant la
[documentation du plugin](https://github.com/ZJU-ACES-ISE/chatunitest-maven-plugin).
La génération s'active explicitement; les tests retenus s'exécutent ensuite
sans Ollama.

| Élément | Configuration |
|---|---|
| Modèle ouvert | Qwen2.5-Coder 3B, famille qwen2, 3,1 milliards de paramètres |
| Quantification | Q4_K_M |
| Serveur | Ollama 0.40.3, sur 127.0.0.1:11434 |
| Matériel local | RTX 4080 Laptop, 12 Go de VRAM; 32 Go de RAM |
| Endpoint | http://127.0.0.1:11434/v1/chat/completions |
| Nom transmis au plugin | `code-llama`, **alias de compatibilité vers Qwen** |
| Clé Maven | `ollama`, valeur factice du service local |
| Génération | température 0; un essai par méthode; au plus trois rounds 0–2 |
| Réponse / contexte | 2 048 tokens de réponse; contexte de 8 192 tokens |
| Traitement | séquentiel; quatre threads CPU configurés; modèle chargé sur le GPU |

ChatUniTest 2.1.1 utilise une liste fermée de noms de modèles, d'où l'alias.
Le modèle reste Qwen : le [Modelfile](evidence/Modelfile) et les
[tags et empreintes](evidence/ollama-models.json) en conservent la famille,
la quantification et l'identité. Le
[relevé GPU](evidence/ollama-gpu.json) montre le modèle et la VRAM utilisée.
[local-environment.txt](evidence/local-environment.txt) donne les versions
Java/Maven/Windows/Ollama; un
[appel de contrôle](evidence/ollama-local-smoke.json) confirme l'inférence locale.
Aucune API de génération externe n'est utilisée pour ces tests.

Les [prompts conservés](scripts/chatunitest-prompts/) demandent au plus deux
tests courts, JUnit 5, des valeurs et tableaux attendus explicites, et les API
publiques. ChatUniTest fournit le corps de la méthode cible et les signatures
des autres méthodes. Le prompt ne fournit ni les mutants PIT ni les tests manuels.
Il adapte le format à l'extracteur et prévient les accès aux champs privés.
L'[analyse du module](evidence/chatunitest-parse.log) recense **1 617 méthodes**;
les générations réutilisent cette analyse.

## 3. Sorties, validation et corrections

Les sorties brutes sont dans
[evidence/chatunitest/generated](evidence/chatunitest/generated/tika-parent/tika-core/org/apache/tika/io/).
Les fichiers livrés sont dans `tika-core/src/test/java/org/apache/tika/io/`.
Les conversations, réponses, rounds et résultats sont dans
[history](evidence/chatunitest/history/), les diagnostics et sources rejetées
dans [errors](evidence/chatunitest/errors/).
Pour `markSupported`, aucune source valide n'a été exportée après les trois
erreurs de compilation : le code du **round 0** a été extrait de `records.json`,
conservé comme sortie brute, puis corrigé explicitement.

| Fichier livré | Tests | Sortie brute / corrections |
|---|---|---|
| [close_0_0_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_close_0_0_Test.java) | testCloseWithoutStream; testCloseWithStream | Deux tests réussis; source inchangée |
| [markSupported_6_0_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_markSupported_6_0_Test.java) | testMarkSupported; testReadWithMark | Trois rounds échouent à la compilation; 2 signatures JUnit, 1 oracle et 4 encodages corrigés |
| [read_2_0_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_read_2_0_Test.java) | testReadSingleByte; testReadMultipleBytes | Compile; second test échoué; 4 assertions corrigées |
| [read_3_0_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_read_3_0_Test.java) | testReadWithAvailableBytes; testReadWithBufferedBytes | Compile; premier test échoué; 1 assertion corrigée |

**Huit tests sont retenus dans quatre fichiers. Trois fichiers ont été modifiés :
12 corrections de sites dans le code**, réparties ainsi :

1. **Deux signatures JUnit.** `assertArrayEquals(byte[], byte[], int, int)`
   n'existe pas en JUnit 5. Deux appels deviennent `assertArrayEquals(expected, actual)`.
2. **Six corrections d'oracle.** Après `reset()` à la marque initiale,
   le tableau attendu est `"Hello"`, pas `"World!"` (une correction).
   Une fenêtre de trois octets consommée donne -1 aux deux lectures suivantes
   et laisse la destination à `[1,2,3]` : deux comptes et deux tableaux attendus
   sont corrigés (quatre corrections).
   Les deux octets encore disponibles après préchargement appartiennent au
   **wrapper**, pas au délégué (une correction de l'objet observé).
3. **Quatre encodages explicites.** Les quatre appels `String.getBytes()`
   du fichier markSupported deviennent
   `getBytes(java.nio.charset.StandardCharsets.UTF_8)`.
   Le contrôle Tika `forbiddenapis` refuse l'encodage par défaut.
   Les chaînes étant ASCII, leurs octets attendus restent identiques.

Le [journal exact des corrections](evidence/chatunitest/manual-corrections.json)
contient fragments avant/après et raisons.
Les [empreintes des sources](evidence/chatunitest/source-hashes.json) relient
les fichiers livrés aux sorties brutes. Le code de production n'est pas modifié
pour satisfaire les tests.

La validation indépendante documente chaque étape :

- [Avant correction de compilation](evidence/generated-before-compilation-correction.log) : deux signatures JUnit invalides.
- [Après correction de compilation, avant correction des oracles](evidence/generated-before-oracle-correction.log) : 14 tests ciblés, **3 échecs**, les six originaux réussissent.
- [Avant encodage explicite](evidence/generated-before-portability-correction.log) : le build install échoue sur quatre API interdites.
- [Après toutes les corrections](evidence/generated-suite.log) : **757 tests, zéro échec, zéro erreur, trois ignorés**.

Les journaux [markSupported](evidence/chatunitest-markSupported.log),
[read](evidence/chatunitest-read.log) et [close](evidence/chatunitest-close.log)
montrent la génération réellement exécutée.
Sur `read`, ChatUniTest annonce « compile and execute successfully » alors
que son résumé JUnit indique un succès et un échec pour chacune des deux classes.
Le `BUILD SUCCESS` du goal de génération ne garantit donc pas le succès des
tests produits; Surefire doit les valider indépendamment.
Les avertissements du processeur d'annotations Java 17 sous JDK 21 sont
distincts des erreurs de signature JUnit.

ChatUniTest insère des imports Mockito inutilisés. Une dépendance de
**portée test** `mockito-junit-jupiter:5.15.2` permet de les compiler avec
Maven sans supprimer ces imports. Aucun mock n'est utilisé.

## 4. Comparaison qualitative des oracles

Les tests originaux vérifient précisément les octets, -1 à la limite,
le compte de saut et la relecture du délégué après fermeture.
Leurs lacunes concernent la lecture par tableau, markSupported,
le départ du délégué à une position non nulle et les lectures fragmentées.

Les tests IA présentent plusieurs qualités d'oracles :

- `testCloseWithoutStream` utilise l'absence d'exception pour la fermeture d'un
  délégué nul. `testCloseWithStream` vérifie available()==3 après un reset
  effectué **par le test lui-même**, sans lecture préalable : cette assertion
  discrimine mal le comportement de close. Aucun de ces tests n'ajoute un mutant tué.
- `testReadSingleByte` recoupe largement les tests originaux.
  `testReadMultipleBytes` vérifie compte, contenu exact, EOF et destination
  préservée. Ses oracles corrigés sont utiles; la version brute ignorait la fenêtre.
- `testReadWithAvailableBytes` vérifie compte, tableau et disponibilité, une
  fois l'objet observé corrigé. Dans `testReadWithBufferedBytes`, la disponibilité
  nulle du délégué est correcte, mais moins discriminante.
- `assertTrue(markSupported())` est simple et pertinent : il tue M15.
  La relecture après reset est un scénario utile, une fois son tableau attendu corrigé.
  L'erreur initiale montre qu'une assertion spécifique peut néanmoins être fausse.

Les sorties IA utilisent seulement l'offset zéro et des délégués ordinaires.
Elles ignorent la marque initiale non nulle, les fragments et la restauration
à EOF avant fermeture explicite.
**100 % de couverture avec six mutants survivants** montre que l'exécution
de toutes les lignes ne suffit pas : les oracles doivent distinguer les erreurs.

## 5. Mutation : apport des tests générés

Le profil `tache2-mutation` installe **PIT 1.17.4** et
**pitest-junit5-plugin 1.2.2**, avec `DEFAULTS`, deux threads et des rapports
XML/HTML. Seule la classe sélectionnée est mutée.

| Étape | Tests autorisés dans PIT | Résultat |
|---|---|---|
| original | LookaheadInputStreamTest uniquement | 19 tués, 5 vivants, 9 non couverts |
| generated | Originaux et ChatUniTest; classe ManualTest explicitement exclue | 27 tués, 6 vivants |
| final | Originaux, ChatUniTest et complémentaires | 33 tués; seuil final 100 % |

Le score est `100 × KILLED / nombre total de mutants`.
Les NO_COVERAGE restent dans le dénominateur; aucun mutant n'est écarté comme
équivalent. Le [script de synthèse](scripts/summarize-mutations.py) compare
`classe + méthode + signature + mutateur + index de bytecode` et refuse
des ensembles différents. Les **mêmes 33 mutants** sont comparés.
L'exclusion des tests manuels a été vérifiée après leur intégration :
le pipeline conserve **27/33** pour l'étape IA.

Les tests IA corrigés tuent **huit mutants supplémentaires**, soit **24,24 points**
de score. Les détecteurs ci-dessous proviennent du rapport **IA**.

| Mutant | Test IA | Pourquoi l'oracle le détecte |
|---|---|---|
| M15, markSupported retourne false | markSupported_6_0_Test.testMarkSupported | assertTrue rejette false. |
| M23, frontière de lecture par tableau > devient >= | read_2_0_Test.testReadMultipleBytes | À la limite, le mutant retourne 0; l'oracle exige -1. |
| M25, position += len devient une soustraction | read_2_0_Test.testReadMultipleBytes | La position devient négative; la lecture suivante ne produit pas l'EOF attendu ou échoue. |
| M26, branche de lecture par tableau forcée à faux | read_2_0_Test.testReadMultipleBytes | Le premier compte devient -1 au lieu de 3. |
| M27, fill supprimé dans la lecture par tableau | read_2_0_Test.testReadMultipleBytes | Le tampon reste vide; le premier compte attendu de 3 devient -1. |
| M28, arraycopy supprimé | read_2_0_Test.testReadMultipleBytes | La destination reste [0,0,0], au lieu de [1,2,3]. |
| M29, compte de lecture remplacé par 0 | read_2_0_Test.testReadMultipleBytes | Le compte de 3 est vérifié indépendamment du tableau. |
| M30, retour EOF par tableau remplacé par 0 | read_2_0_Test.testReadMultipleBytes | Les appels après consommation de la fenêtre exigent -1. |

Les 19 mutants déjà tués le restent. Leurs raisons sont les suivantes :

| Mutants | Comportement qui les distingue |
|---|---|
| M03–M04, available | La disponibilité détermine le saut; addition ou zéro changent le compte et la lecture suivante. |
| M05–M06, close | Les originaux relisent les octets du délégué après fermeture; supprimer la restauration change cette lecture. |
| M09, fill | Soustraire le nombre lu au compteur buffered empêche l'octet attendu. |
| M10, fill, index 5 | Supprimer la condition de disponibilité empêche le remplissage initial nécessaire. |
| M11, fill, index 14 | Supprimer la condition du délégué non nul empêche la lecture attendue. |
| M12, fill | Modifier le traitement du nombre d'octets du délégué change remplissage et lecture observée. |
| M13, fill, index 11 | Supprimer la condition de capacité empêche le remplissage initial. |
| M16 et M22, read scalaire | Fenêtre nulle/flux nul exige -1; modifier frontière ou retour produit 0. |
| M17, read scalaire | Une décrémentation de position empêche la séquence et la relecture attendues. |
| M18, read scalaire | Remplacer le AND 0xff par OR modifie les octets ASCII attendus. |
| M19–M21, read scalaire | Branche ou fill supprimés, ou retour zéro, contredisent les octets attendus. |
| M31–M33, skip | Position erronée, fill supprimé ou retour zéro contredisent le saut de 1 et l'octet suivant. |

## 6. Tests complémentaires : intention, données et oracle

Les cinq tests sont dans
[LookaheadInputStreamManualTest.java](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStreamManualTest.java).
Ils répondent aux survivants **M01, M02, M07, M08, M14, M24**.
Ils sont conçus avec l'assistance de Codex et ne proviennent pas de ChatUniTest.

### closeRestoresTheCurrentDelegatePosition — M01 et M02

**Intention :** restaurer la position du délégué au moment de la création du wrapper.
**Données :** [10,20,30,40], dont 10 est consommé avant de créer une fenêtre de deux.
Ce départ non nul distingue la marque du constructeur de la marque implicite
au début de ByteArrayInputStream.
**Oracle :** wrapper : 20, 30, -1; après fermeture, délégué : 20, 30, 40.
Sans marque du constructeur, le délégué relit 10 : les deux mutants échouent.

### reachingDelegateEofRestoresItsPositionImmediately — M14

**Intention :** restaurer immédiatement le délégué à EOF, avant fermeture explicite.
**Données :** [21,42], fenêtre de cinq; la fenêtre plus grande force fill à obtenir -1.
**Oracle :** après 21, 42, -1 dans le wrapper, la lecture directe du délégué donne 21.
Supprimer close automatique le laisse à EOF et donne -1.
Une nouvelle lecture du wrapper reste -1.

### fragmentedReadsUseOnlyTheRemainingBufferCapacity — M08 et M24

**Intention :** respecter la capacité restante lors de remplissages successifs.
**Données :** délégué [11,22,33,44] fournissant au plus un octet par appel;
fenêtre de trois; destination [9,9,9,9,9]. Trois demandes aux offsets 1, 2 et 3
lisent les trois fragments. Les valeurs distinctes et sentinelles rendent
visibles compte, ordre et cellules préservées.
Le délégué instrumenté vérifie les bornes avant de fragmenter la demande,
conformément au contrat d'une lecture valide.
**Oracle :** chaque compte vaut 1, destination [9,11,22,33,9], puis lecture scalaire -1.
M08 change buffer.length-buffered en addition : au deuxième remplissage,
la demande dépasse les bornes du tampon. M24 change buffered-position en
addition : le compte/copie excède l'unique octet disponible et contredit l'attente de 1.

### bulkReadsRespectOffsetCountAndLookaheadLimit — renforce M24

**Intention :** vérifier offset, compte, position et troncature avec un délégué ordinaire.
**Données :** [0x80,0x22,0xfe,0x11], fenêtre de trois, destination de six octets
initialisés à 9. Deux octets demandés à l'offset 2, puis deux à l'offset 4
alors qu'il n'en reste qu'un.
**Oracle :** comptes 2 puis 1; destinations [9,9,0x80,0x22,9,9] puis
[9,9,0x80,0x22,0xfe,9]; disponibilités 1 puis 0; prochaine lecture -1.
Les sentinelles vérifient les cellules préservées et le quatrième octet reste
hors fenêtre. M24 calcule une mauvaise longueur au second appel et dépasse
le tampon. Ce cas renforce aussi les oracles de M23, M25 et M30 déjà tués par l'IA.

### reachingLookaheadLimitDoesNotTriggerAnotherDelegateRead — M07

**Intention :** ne plus remplir le délégué une fois la fenêtre consommée,
même avec une demande de longueur zéro.
**Données :** [11,22,33], fenêtre de deux; le délégué instrumenté autorise
un remplissage puis signale tout autre appel.
Un ByteArrayInputStream ordinaire pourrait rendre une lecture de longueur zéro invisible.
**Oracle :** après 11 et 22, le wrapper retourne -1 et skip(1) retourne 0,
sans déclencher la garde. M07 transforme < en <= et déclenche précisément
un remplissage supplémentaire à la limite.

## 7. Les 33 mutants

M01–M33 suivent l'ordre initial. KILLED = détecté; SURVIVED = exécuté sans
détection; NO_COVERAGE = non exécuté. Les signatures distinguent les surcharges.
Plusieurs tests peuvent tuer un mutant; chaque colonne donne le détecteur
retenu par PIT dans l'analyse correspondante.

| ID | Méthode : ligne | Mutation | Originaux | + IA | + Manuels | Test détecteur IA | Test détecteur final |
|---|---|---|---|---|---|---|---|
| M01 | `<init>` : 67 | removed conditional - replaced equality check with false | SURVIVED | SURVIVED | KILLED | — | LookaheadInputStreamManualTest.closeRestoresTheCurrentDelegatePosition() |
| M02 | `<init>` : 68 | removed call to java/io/InputStream::mark | SURVIVED | SURVIVED | KILLED | — | LookaheadInputStreamManualTest.closeRestoresTheCurrentDelegatePosition() |
| M03 | `available` : 124 | Replaced integer subtraction with addition | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testSkipLookahead() | LookaheadInputStreamTest.testSkipLookahead() |
| M04 | `available` : 124 | replaced int return with 0 for org/apache/tika/io/LookaheadInputStream::available | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testSkipLookahead() | LookaheadInputStreamTest.testSkipLookahead() |
| M05 | `close` : 74 | removed conditional - replaced equality check with false | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M06 | `close` : 75 | removed call to java/io/InputStream::reset | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M07 | `fill` : 81 | changed conditional boundary | SURVIVED | SURVIVED | KILLED | — | LookaheadInputStreamManualTest.reachingLookaheadLimitDoesNotTriggerAnotherDelegateRead() |
| M08 | `fill` : 82 | Replaced integer subtraction with addition | SURVIVED | SURVIVED | KILLED | — | LookaheadInputStreamManualTest.fragmentedReadsUseOnlyTheRemainingBufferCapacity() |
| M09 | `fill` : 84 | Replaced integer addition with subtraction | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M10 | `fill` : 81 | removed conditional - replaced equality check with false | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M11 | `fill` : 81 | removed conditional - replaced equality check with false | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M12 | `fill` : 83 | removed conditional - replaced equality check with false | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M13 | `fill` : 81 | removed conditional - replaced comparison check with false | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M14 | `fill` : 86 | removed call to org/apache/tika/io/LookaheadInputStream::close | SURVIVED | SURVIVED | KILLED | — | LookaheadInputStreamManualTest.reachingDelegateEofRestoresItsPositionImmediately() |
| M15 | `markSupported` : 129 | replaced boolean return with false for org/apache/tika/io/LookaheadInputStream::markSupported | NO_COVERAGE | KILLED | KILLED | LookaheadInputStream_markSupported_6_0_Test.testMarkSupported() | LookaheadInputStream_markSupported_6_0_Test.testMarkSupported() |
| M16 | `read()` : 94 | changed conditional boundary | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testNullStream() | LookaheadInputStreamTest.testZeroLookahead() |
| M17 | `read()` : 95 | Replaced integer addition with subtraction | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M18 | `read()` : 95 | Replaced bitwise AND with OR | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M19 | `read()` : 94 | removed conditional - replaced comparison check with false | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M20 | `read()` : 93 | removed call to org/apache/tika/io/LookaheadInputStream::fill | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M21 | `read()` : 95 | replaced int return with 0 for org/apache/tika/io/LookaheadInputStream::read | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testBasicLookahead() | LookaheadInputStreamTest.testMarkLookahead() |
| M22 | `read()` : 97 | replaced int return with 0 for org/apache/tika/io/LookaheadInputStream::read | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testNullStream() | LookaheadInputStreamTest.testZeroLookahead() |
| M23 | `read(byte[], int, int)` : 104 | changed conditional boundary | NO_COVERAGE | KILLED | KILLED | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() |
| M24 | `read(byte[], int, int)` : 105 | Replaced integer subtraction with addition | NO_COVERAGE | SURVIVED | KILLED | — | LookaheadInputStreamManualTest.fragmentedReadsUseOnlyTheRemainingBufferCapacity() |
| M25 | `read(byte[], int, int)` : 107 | Replaced integer addition with subtraction | NO_COVERAGE | KILLED | KILLED | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() |
| M26 | `read(byte[], int, int)` : 104 | removed conditional - replaced comparison check with false | NO_COVERAGE | KILLED | KILLED | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() |
| M27 | `read(byte[], int, int)` : 103 | removed call to org/apache/tika/io/LookaheadInputStream::fill | NO_COVERAGE | KILLED | KILLED | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() |
| M28 | `read(byte[], int, int)` : 106 | removed call to java/lang/System::arraycopy | NO_COVERAGE | KILLED | KILLED | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() |
| M29 | `read(byte[], int, int)` : 108 | replaced int return with 0 for org/apache/tika/io/LookaheadInputStream::read | NO_COVERAGE | KILLED | KILLED | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() |
| M30 | `read(byte[], int, int)` : 110 | replaced int return with 0 for org/apache/tika/io/LookaheadInputStream::read | NO_COVERAGE | KILLED | KILLED | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() | LookaheadInputStream_read_2_0_Test.testReadMultipleBytes() |
| M31 | `skip` : 118 | Replaced long addition with subtraction | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testSkipLookahead() | LookaheadInputStreamTest.testSkipLookahead() |
| M32 | `skip` : 116 | removed call to org/apache/tika/io/LookaheadInputStream::fill | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testSkipLookahead() | LookaheadInputStreamTest.testSkipLookahead() |
| M33 | `skip` : 119 | replaced long return with 0 for org/apache/tika/io/LookaheadInputStream::skip | KILLED | KILLED | KILLED | LookaheadInputStreamTest.testSkipLookahead() | LookaheadInputStreamTest.testSkipLookahead() |

Rapports : [PIT original](evidence/pit-original/index.html),
[PIT IA](evidence/pit-generated/index.html),
[PIT final](evidence/pit-final/index.html),
[synthèse JSON](evidence/mutation-summary.json),
[JaCoCo IA](evidence/coverage-generated/jacoco.xml),
[JaCoCo final](evidence/coverage-final/jacoco.xml).

## 8. Reproduction

Prérequis pour les mesures : JDK 21, Maven 3.9.9, Python 3.
Ollama n'est nécessaire que pour régénérer les tests.
La génération est séparée des mesures pour préserver les sources livrées.

### PowerShell / Windows

```powershell
# JAVA_HOME doit désigner le JDK 21; Maven et Python doivent être accessibles.
./scripts/tache2.ps1 all

# Ou utiliser des chemins personnalisés :
./scripts/tache2.ps1 all -Maven 'CHEMIN/vers/mvn.cmd' -Python 'CHEMIN/vers/python.exe'

# Serveur persistant dans un premier terminal :
ollama serve

# Dans un deuxième terminal :
ollama pull qwen2.5-coder:3b
ollama create code-llama -f evidence/Modelfile
./scripts/tache2.ps1 generate
```

### Bash / Linux

```sh
scripts/tache2.sh all
python3 scripts/summarize-mutations.py

# Après lancement du serveur Ollama :
ollama pull qwen2.5-coder:3b
ollama create code-llama -f evidence/Modelfile
scripts/tache2.sh generate
```

`all` compile/installe tika-core et ses modules requis, exécute la suite
complète avec les exclusions de chaque étape, sauvegarde Surefire/JaCoCo
et lance les trois analyses PIT. Les données JaCoCo précédentes sont supprimées
avant chaque suite pour éviter un cumul artificiel.
Les rapports de chaque étape sont séparés sous evidence.
PowerShell appelle aussi la synthèse; sous Bash elle est appelée dans la commande
ci-dessus et dans le workflow.

Une régénération écrit sous tika-core/target/chatunitest-regenerated et crée
les historiques sous tika-core/target/chatunitest-info. Après l'expérience,
ce répertoire de sortie a été séparé des preuves figées sous evidence/chatunitest/generated
pour éviter de les écraser. Elle ne remplace pas les sources retenues
dans src/test/java. Avant intégration, conserver les nouvelles sorties et historiques, compiler,
vérifier Surefire, documenter les corrections puis refaire les trois mesures.
Température zéro ne garantit pas une réponse identique entre versions ou matériels;
les sorties de cette expérience sont conservées et identifiées par empreintes.

Le profil fast évite les étapes de formatage; les mesures imposent skipTests=false.
Le contrôle des API interdites reste actif et passe après les conversions UTF-8.

## 9. Exécution et GitHub Actions

| Suite complète, Windows local | Recensés | Réussis | Ignorés | Échecs / erreurs |
|---|---:|---:|---:|---:|
| Originale | 749 | 746 | 3 | 0 / 0 |
| Avec ChatUniTest corrigé | 757 | 754 | 3 | 0 / 0 |
| Finale | 762 | 759 | 3 | 0 / 0 |

Les trois tests ignorés existaient déjà :
FileCommandDetectorTest.testBasic requiert l'utilitaire file, absent du PATH
dans cette exécution; TikaInputStreamTest.reproduceRandomizedTestFailure
est désactivé pour une reproduction ponctuelle; CustomErrorHandlerTest.testUndeclaredEntityXML
est désactivé par le projet. Aucun test ajouté n'est ignoré.

Le [workflow GitHub Actions](.github/workflows/tache2.yml), Ubuntu/Temurin 21,
exécute le script Bash et compare les identités des mutants.
Il conserve Surefire, JaCoCo, PIT, les trois journaux de suites et les synthèses.
Le seuil PIT final de 100 % fait échouer une régression. Ollama n'est pas requis en CI.

Le [pipeline PowerShell complet](evidence/local-powershell-pipeline.log) a réussi,
y compris après ajout des tests manuels. Les **commandes du workflow Bash ont
aussi été exécutées complètement sous Ubuntu WSL sur cet ordinateur**, depuis
une archive Git des sources livrées :
[journal complet](evidence/ubuntu-wsl/pipeline.log) et
[vérification structurée](evidence/ubuntu-wsl/verification.json).
Ubuntu reproduit **19/33, 27/33 et 33/33**, avec les mêmes identités de mutants
que Windows. Les 762 tests finaux y comptent **760 réussites et deux ignorés** :
l'utilitaire file y est disponible, donc le test supplémentaire ignoré sous
Windows s'y exécute avec succès. Les données XML des trois étapes Ubuntu
sont conservées sous evidence/ubuntu-wsl. La syntaxe Bash a aussi été vérifiée
avec bash -n, et le bit exécutable du script est enregistré dans Git.
**GitHub Actions a été exécuté avec succès après publication** :
[exécution 38459745558](https://github.com/waydala/tika-AWADA_ALI/actions/runs/38459745558),
sur le commit `8e3fabaedd6e6bfcba0e5cb64aa3b311aa748e43`.
Les rapports téléchargés ont été inspectés : **19/33, 27/33 et 33/33 mutants**,
avec les mêmes identités que les mesures locales. Les **13 nouveaux tests passent**,
et la suite finale recense **762 tests, 760 réussis, deux ignorés, aucun échec ni erreur**.
Les données de cette validation hébergée sont distinctes des mesures Windows locales :
[vérification](evidence/github-actions/verification.json),
[métadonnées du job](evidence/github-actions/run.json),
[synthèse PIT](evidence/github-actions/mutation-summary.json).
Les XML PIT et JaCoCo des trois étapes sont conservés sous evidence/github-actions.

## 10. Usage de l'IA et provenance

Outils utilisés le **13 octobre 2026** : Qwen2.5-Coder 3B, avec les versions
de ChatUniTest et d'Ollama précisées en section 2, et
[Codex](https://openai.com/codex/), agent basé sur GPT-6 dans cette session.
La demande initiale à Codex comprenait notamment « je veux que tu complètes la tâche numéro 2 »
et « tu peux les run sur mon ordi ». Les prompts et réponses effectivement
transmis à Qwen par ChatUniTest sont conservés dans les historiques liés en section 3.

ChatUniTest et Qwen2.5-Coder produisent les tests identifiés comme générés.
Codex assiste la configuration, les validations, les 12 corrections explicites,
l'analyse des rapports, la conception des cinq tests complémentaires et ce README.
Ces derniers sont conçus en réponse aux survivants avec cette assistance
et sont distincts des sorties ChatUniTest.

Le travail préparatoire cloud avait choisi la classe et proposé une configuration
et des tests complémentaires. Ces éléments ont été inspectés et adaptés.
Les sorties ChatUniTest livrées et les mesures rapportées ont été
**régénérées et exécutées localement**; les anciennes mesures cloud ne sont pas
présentées comme celles de cet ordinateur.

Déclaration suivant les
[consignes du cours](https://github.com/umontreal-diro/IFT3913/blob/2026/tache2/readme.md)
et la ressource de l'Université
[Citer, signaler, déclarer et documenter l'IA générative](https://boite-outils.bib.umontreal.ca/c.php?g=743753&p=5377614).
Prompts, réponses, erreurs, corrections et rapports permettent de contrôler
la provenance et les résultats.
