Was wurde erstellt

Neuer sub-folder: v3-sandbox/prototype-java/

- 52 Java-Dateien (49 main + 3 test) im Maven-Projekt
- Alle 7 in der META-Lang definierten Namespaces als Java-Pakete:
    - org.hiero.common (7 Typen)
    - org.hiero.config (3 Typen)
    - org.hiero.keys + org.hiero.keys.io (12 Typen — public API + ECDSA/Ed25519 impl portiert aus prototype-keys-java)
    - org.hiero.client (3 Typen)
    - org.hiero.transactions (8 Typen + impl)
    - org.hiero.transactions.accounts (4 Typen)
    - org.hiero.transactions.spi (2 Typen)
- org.hiero.sdk.annotation.ThreadSafe aus guides/java-files/
- Single module-info.java mit JPMS-Exports der Public-API-Pakete
- README.md und ausführliche REPORT.md

Compile + Test: Maven clean test läuft erfolgreich durch (Tests run: 9, Failures: 0).

Report-Highlights (Übersicht der wichtigsten Lücken)

Der vollständige Report liegt in v3-sandbox/prototype-java/REPORT.md. Die kritischsten Punkte:

Cross-cutting:
- §0.1 grpc und hiero-proto sind nur Platzhalter-Namespaces — die SPI convert(...) / updateBody(...) Methoden lassen sich nicht implementieren
- §0.2 uintX → Java hat keine unsigned-Typen, Mapping-Konvention nicht dokumentiert
- §0.5 Hyphenated Namespace-Namen (transactions-accounts) — kein Mapping definiert
- §0.6 Namespace-Funktionen ohne Owner-Type haben keine klare Zuordnung

common:
- §1.1 HbarUnit.values() kollidiert mit Javas built-in values()
- §1.6 Checksum-Algorithmus für validateChecksum(ledger) ist nirgends spezifiziert
- §1.4 Address-Abstraktion hat noch keinen sichtbaren Mehrwert

keys:
- §3.1 toBytes/toString sind doppelt deklariert mit unterschiedlichen Parameter-Namen (EncodedKeyContainer vs. KeyFormat)
- §3.2 Typo RawFormate statt RawFormat
- §3.3 Java-Syntax (byte[], boolean) statt Meta-Sprache (bytes, bool)

transactions:
- §5.1 TransactionBuilder<$$Transaction extends TransactionBuilder> — der Generic heißt "Transaction" ist aber der Builder-Typ
- §5.2 Fluent buildAndExecute braucht 4 Generics, die Spec hat nur 2
- §5.3 Transaction.unbuild() ist @@finalType aber muss polymorph den Original-Builder-Typ zurückgeben
- §5.4 BasicTransactionStatus ist mit ... nur unvollständig aufgelistet
- §6.1 AccountCreateTransactionBuilder extends TransactionBuilder<AccountCreateTransactionBuilder> — nur 1 Generic-Parameter, sollte 2 sein
- §6.2 @@default(0) für ein komplexes Hbar — die Bedeutung des Defaults ist unklar
- §6.3 key:keys.PublicKey ist required, aber im Builder-Pattern gibt es keine Annotation für "required at build time"

Bewusst nicht implementiert (siehe §9):
- Transaction.toBytes() / fromBytes() — wire format fehlt
- Transaction.execute() über gRPC — grpc namespace ist Platzhalter
- Receipt/Record-Polling
- unbuild() round-tripping (siehe §5.3)
- JPMS-Modul-Split pro Namespace (alles in einem Maven-Modul)
