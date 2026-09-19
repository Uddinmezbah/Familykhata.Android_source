# Hisabi Khata — Trial & Premium Owner Guide

## Current flow
- 30-day Full Premium Trial.
- After expiry, existing data remains readable and backup/report stay available.
- New/edit actions remain locked until Premium is active.
- Paid Premium entitlement is verified through Google Play Billing.

## Products
- Monthly: `hisabi_premium` / `monthly-prepaid`
- Yearly: `hisabi_premium` / `yearly-prepaid`
- Lifetime: `hisabi_premium_lifetime` / one-time non-consumable

## Paid customer
Open Get Premium → choose a plan → pay with Google Play → app verifies PURCHASED entitlement → Premium activates.

## Give free permanent Premium
Use a Google Play promo code for `hisabi_premium_lifetime`.

Flow:
1. Create a Lifetime promo code in Google Play Console.
2. Send the one-off code privately.
3. User opens Get Premium → Redeem Promo Code.
4. User redeems it in Google Play.
5. User returns to Hisabi Khata and taps Restore Purchases.
6. Google Play reports the Lifetime entitlement and Premium activates.

Never add a universal password or hardcoded unlock code to the APK.

## Testing
Use Google Play license testers / Play testing tracks. Do not add a permanent developer password to production builds.

## Restore Purchases
Use after reinstalling, changing phone, redeeming a promo code outside the app, or when a valid purchase is not showing. The user must use the Google Play account that owns the purchase.

## Current limitation
The 30-day trial start time is stored locally. Clearing app data or reinstalling may reset the local trial.

Strict reinstall-resistant trial enforcement requires account identity plus a secure backend that stores trial start and entitlement state.

For stronger commercial enforcement at scale, use a secure backend with Google Play Developer API and Real-time Developer Notifications.

## Owner rule
- Paid Premium: Google Play purchase.
- Free permanent Premium: Google Play Lifetime promo code.
- Testing: Google Play license tester.
- Never: hardcoded universal password, secret APK unlock code, or direct SharedPreferences editing.
