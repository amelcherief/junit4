# Améliorations projet JUnit

# 1. Duplications:
Une classe est dupliquée dans le code. Nous avons la même classe dans deux endroits différents.
- `Assert.java` du paquetage **junit.framework**.
- `Assert.java` du paquetage **org.junit**.
Comme mentionné dans sonar, la classe junit.framework.Assert peut être remplacée par la classe org.junit.Assert dans le projet.

<img src = images/duplication.png />
<p> </p>

***Solution apportée :***
Suppression de la classe junit.framework.Assert et corriger les erreurs liées à sa suppression en utilisant org.junit.Assert à sa place.
