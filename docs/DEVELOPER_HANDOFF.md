# Hisabi Khata — Developer Handoff

## Project identity
- App: Hisabi Khata
- Repository: https://github.com/Uddinmezbah/Familykhata.Android_source
- Authoritative repository: Familykhata.Android_source
- Do not use the old family-khata repository.
- Current development branch: v1.6-test
- Current handoff commit: 9df965553d9433011b6414569ecb77db66588966
- Stable branch: main
- Never merge to main without owner approval.

## Android stack
- Kotlin
- Jetpack Compose
- Room
- compileSdk 36
- targetSdk 36
- minSdk 24
- AGP 8.10.1
- Kotlin 1.9.24
- KSP 1.9.24-1.0.20

## Build authority
GitHub Actions is the authoritative build verification environment.

The repository may not contain a local Gradle wrapper.
Do not treat a missing local ./gradlew as a source-code failure.

Workflow:
.github/workflows/android-build.yml

## Important Compose rule
Do not import:

androidx.compose.foundation.layout.weight

Use Modifier.weight() from RowScope / ColumnScope.

## Major implemented areas
- Bengali PDF Ledger Statement
- Backup usability improvements
- Premium Billing foundation
- Generic Product Unit system
- Retail Sale -> Baki synchronization
- Dealer Delivery / Challan generic units
- Financial Accounts
- Digital Services account integration
- Business Income / Expense -> Financial Account
- Retail Sale Payment -> Financial Account

## Financial Accounts
Types:
- CASH
- MOBILE_WALLET
- BANK
- CARD
- OTHER

Internal transfers use paired entries and are excluded from P&L.

Digital Services handle account movement separately.
Only actual profit/loss belongs in P&L.

Retail Sale must NOT create a generic P&L transaction for the full sale.

## Retail payment architecture
Inventory DB and core Financial Account DB are separate Room databases.

Therefore strict cross-database atomicity is NOT guaranteed.

Authoritative data:
Retail payment event rows in Inventory DB.

Derived projection:
FinancialAccountEntry with entryType RETAIL_SALE_IN.

Deterministic source key:
RETAIL_SALE_ACCOUNT:<eventKey>

Projection/reconciliation must remain idempotent.

Legacy Retail sales must not silently infer Financial Accounts.

## Retail payment behavior
Example:
- Sale total: 500
- Cash received now: 200
- Due: 300
- Later bKash collection: 300

Expected:
- Cash +200
- bKash +300
- Due = 0

Split payments across multiple Financial Accounts are supported.

## Retail cancellation invariant
Before removing Retail credits, verify that reversal will not make
the Financial Account balance negative beyond floating tolerance.

If reversal is unsafe, cancellation must fail clearly.

Because Inventory DB and Financial Account DB are separate,
compensation/reconciliation is used rather than claiming strict atomicity.

## Backup format
Current app backup version: 12

v12 includes authoritative Retail:
- sales
- sale lines
- stock allocations
- payment events

Derived RETAIL_SALE_IN entries are not independently backed up in v12.
They are rebuilt from Retail payment events on restore.

## Git working rules
- Work on feature/test branches.
- Do not modify unrelated files.
- Do not reset/discard uncommitted work without explicit approval.
- Run git diff --check before commit.
- Push feature branch first.
- GitHub Actions must pass before a feature is considered complete.
- Do not merge to main without owner approval.

## Recovery backup
Use:

powershell -ExecutionPolicy Bypass -File .\scripts\create-recovery-backup.ps1

The generated .bundle is the most important recovery artifact because
it preserves Git history, branches and tags.

## Secrets
Never commit:
- Android signing keystore
- keystore passwords
- Play Console credentials
- API secrets
- private tokens

These must be kept separately by the owner.
