# Système d'Argent - DailyRewards

## Problèmes Résolus

### 1. Problème avec l'Enclume
- **Avant** : L'enclume ne lisait pas correctement les valeurs entrées
- **Maintenant** : Utilisation d'un panneau simple avec saisie par chat

### 2. Problème avec les Commandes d'Argent
- **Avant** : Les commandes d'argent étaient sauvegardées mais pas exécutées
- **Maintenant** : Les commandes sont lues depuis `day-X-commands` ET `day-X`

## Comment Utiliser

### Configuration
```yaml
# Activer le système d'argent
monney: "vault"  # ou "playerpoints", "coinsengine", "ultraeconomy"

# Récompenses d'objets
rewards:
  day-1:
    - "item:DIAMOND:1"
  
  # Commandes personnalisées (y compris l'argent)
  day-1-commands:
    - "eco give {player} 100"
    - "give {player} bread 5"
```

### Interface Admin
1. `/rewards admin day <jour>` - Ouvrir l'interface de configuration
2. Cliquer sur le bloc d'or au centre
3. Saisir le montant dans le chat
4. Sauvegarder avec le bouton vert

### Systèmes Supportés
- **Vault/EssentialsX** : `eco give {player} <montant>`
- **PlayerPoints** : `points give {player} <montant> -s`
- **CoinsEngine** : `coins give {player} <montant>`
- **UltraEconomy** : `addbalance {player} <devise> <montant>`
- **VotingPlugin** : `av User {player} AddPoints <montant>`

## Fonctionnalités

- ✅ Saisie d'argent par chat (plus fiable que l'enclume)
- ✅ Sauvegarde automatique des commandes d'argent
- ✅ Exécution des commandes lors de la réclamation
- ✅ Support de multiples systèmes d'économie
- ✅ Limite de sécurité (1,000,000 maximum)
- ✅ Interface intuitive avec boutons confirm/annuler

## Structure des Fichiers

```
rewards:
  day-1:                    # Objets du jour 1
    - "item:DIAMOND:1"
  day-1-commands:           # Commandes du jour 1 (y compris l'argent)
    - "eco give {player} 100"
    - "give {player} bread 5"
```

## Notes Techniques

- Les commandes d'argent sont automatiquement générées selon le système configuré
- Les anciennes commandes d'argent sont supprimées avant d'ajouter les nouvelles
- Le système vérifie que le montant est positif et dans les limites de sécurité
- Les commandes sont exécutées en tant que console pour éviter les problèmes de permissions
