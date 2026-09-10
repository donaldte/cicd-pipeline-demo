# Guide de démonstration live

Ce fichier est pour toi, pas pour l'audience : c'est le script exact de ce
qu'il faut changer et cliquer pendant la démo, dans l'ordre, pour montrer
que le pipeline fonctionne vraiment — pas juste des slides.

Compte ~12-15 minutes pour la version complète. Une version courte (~6-7
min) est indiquée à la fin si le temps presse.

---

## Avant la démo (pas en direct)

Fais ceci la veille, pas devant l'audience :

1. **Répète une fois en entier** — les temps d'exécution réels (CI ~2-4
   min, release+DAST ~3-5 min) ne sont pas compressibles ; sache à quel
   moment parler pendant que ça tourne.
2. Vérifie les 3 points du README (§ "One-time setup") : permissions du
   `GITHUB_TOKEN` en Read/write, les 3 environnements créés, et
   `production` (au moins) avec toi comme reviewer requis.
3. **Fais un premier merge sur `main` avant la démo** (n'importe quel petit
   commit, ex. `fix: initial health check response`). La toute première
   release de semantic-release est *toujours* `v1.0.0`, peu importe si
   c'est un `fix` ou un `feat` — tu veux que cette bizarrerie soit déjà
   derrière toi quand l'audience regarde.
4. Garde un deuxième onglet ouvert sur l'onglet **Actions** du repo, et un
   troisième sur **Settings → Environments** — tu vas alterner entre les
   trois pendant la démo.

---

## Partie 2 du webinaire — les déclencheurs (trigger types)

Pas de changement de code ici, juste une visite guidée :

1. Ouvre `.github/workflows/ci.yml` → montre `on: pull_request: branches:
   [main]`.
2. Ouvre `.github/workflows/release-and-deploy-dev.yml` → montre `on:
   push: branches: [main]`.
3. Ouvre `.github/workflows/cd-manual.yml` → montre `on:
   workflow_dispatch:` avec les deux `inputs:` (`version`, `environment`).
4. Ligne à dire : *« Trois fichiers, trois façons différentes de démarrer
   un pipeline — automatique sur une PR, automatique après un merge, et à
   la demande. On va voir les trois tourner. »*

---

## Partie 3 du webinaire — une PR qui échoue, puis qui passe

Montre qu'une gate est réelle, pas décorative.

```bash
git checkout main && git pull
git checkout -b demo/format-gate
```

Dans `src/main/java/tech/squadplus/demo/GreetingService.java`, casse
volontairement l'indentation de la méthode `capitalize` — remplace :

```java
  private String capitalize(String value) {
    if (value.isEmpty()) {
      return value;
    }
```

par (attention, indentation volontairement fautive) :

```java
  private String capitalize(String value) {
        if (value.isEmpty()) {
      return value;
    }
```

```bash
git add -A
git commit -m "fix: adjust capitalize formatting"
git push -u origin demo/format-gate
```

Ouvre la PR sur GitHub. Pendant que les jobs tournent :

- *« Cinq jobs tournent en parallèle : format, dependency scan, SAST,
  tests, et build+scan de l'image. »*
- Le job **1 · Format check** devient rouge en premier (c'est le plus
  rapide). Clique dessus, montre le diff que `spotless` rapporte.

Corrige en direct :

```bash
mvn spotless:apply
git add -A
git commit -m "fix: apply spotless formatting"
git push
```

- Le push relance automatiquement la CI sur la même PR (même trigger
  `pull_request`, événement `synchronize`). Regarde le job format repasser
  au vert.
- Merge la PR une fois tout au vert.

---

## Partie 4 du webinaire — versions sémantiques + déploiement

### 4.1 — Le merge déclenche `release-and-deploy-dev.yml`

Bascule sur l'onglet Actions dès le merge ci-dessus. Le commit était
`fix: ...`, donc si tu es bien parti de `v1.0.0` (voir setup), tu dois
voir apparaître **v1.0.1** dans le résumé du job `release`.

Pendant que `image` / `dast` / `deploy-dev` tournent (compte 3-5 min) :

- *« `image` construit et pousse l'image taguée `1.0.1` sur GitHub
  Container Registry. »*
- *« `dast` démarre un vrai conteneur à partir de cette image et lance un
  scan OWASP ZAP dessus, en localhost, dans le runner. »* — s'il y a des
  alertes (probable sur une démo minimaliste : en-têtes manquants, etc.),
  c'est normal et c'est le point : *« Ni le format, ni les tests, ni le
  SAST n'auraient attrapé ça — c'est exactement pour ça qu'on fait aussi
  du DAST. »*
- *« `deploy-dev` republie la même image et vérifie qu'elle démarre — pas
  de simulation abstraite, c'est le vrai conteneur qui tourne. »*
- Ouvre le résumé du job `deploy-dev` (onglet Summary) : le tableau
  version/image/commit s'affiche.

### 4.2 — Un `feat:` pour montrer le bump minor (optionnel si le temps le permet)

Même recette que la Partie 3 mais sans rien casser : édite
`GreetingService.java`, ajoute un commentaire ou un petit changement
réel, commit avec `git commit -m "feat: add a demo tweak"`, PR, merge.
Résultat attendu : **v1.1.0** (minor, pas patch).

### 4.3 — Déploiement manuel avec approbation

1. Actions → **CD · Manual Deploy** → **Run workflow**.
2. `version`: la version affichée à l'étape 4.1 (ex. `1.0.1`).
   `environment`: **production**.
3. Lance. Le job `validate` confirme que l'image existe. Le job `deploy`
   passe en **Waiting** — c'est l'environnement `production` qui bloque.
4. Ouvre le run, clique **Review deployments**, approuve.
5. *« C'est la différence entre `dev` et `production` dans ce pipeline :
   même workflow de déploiement, mais `production` exige un humain. »*

---

## Bonus si le temps le permet — l'architecture du code

- `.github/workflows/reusable-build-test.yml` : appelé depuis `ci.yml`
  *et* `release-and-deploy-dev.yml` via `uses: ./.github/workflows/...` —
  la logique build+test n'existe qu'à un seul endroit.
- `.github/actions/setup-build-env/action.yml` : une composite action,
  utilisée par presque tous les jobs pour installer le JDK.
- Dans `release-and-deploy-dev.yml`, montre `env: &image_env` sur le job
  `image` et `env: *image_env` sur `dast` et `deploy-dev` : un ancrage YAML
  natif — la définition n'existe qu'une fois, les deux autres jobs la
  réutilisent telle quelle. (GitHub Actions supporte les ancres/alias
  depuis 2025, mais pas les clés de fusion `<<:` — c'est pour ça que ces
  deux jobs réutilisent le mapping tel quel plutôt que de l'étendre.)

---

## Version courte (6-7 minutes)

Si le temps manque, saute la Partie 3 (format-gate) et va direct à :

1. Montre les 3 fichiers de workflow (30 sec, trigger types).
2. Merge une PR déjà préparée avec un commit `fix:` → regarde
   `release-and-deploy-dev.yml` tourner en accéléré, arrête-toi sur le
   résumé `deploy-dev`.
3. Déclenche `cd-manual.yml` vers `production`, montre l'approbation.

---

## Dépannage rapide

- **`release` ne publie rien** : vérifie que le message de commit suit
  exactement `type: sujet` (deux-points + espace), et que
  `Settings → Actions → Workflow permissions` est bien sur *Read and
  write*.
- **`image` échoue avec une erreur de permission** : même réglage que
  ci-dessus (`packages: write` vient de là).
- **`cd-manual` ne se met jamais en Waiting** : l'environnement choisi n'a
  pas de reviewer requis configuré — vérifie
  `Settings → Environments → production`.
- **Le scan ZAP est long** : normal (1-3 min). Enchaîne avec l'explication
  du tableau `Au programme` / du sommaire de la Partie 3 du webinaire
  pendant que ça tourne, plutôt que de rester silencieux à l'écran.
