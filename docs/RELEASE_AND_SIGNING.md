# Hisabi Khata — Release & Signing Checklist

## Owner must keep separately
The following are NOT source-code backup items:

1. Android release signing keystore (.jks / .keystore)
2. Keystore alias
3. Keystore password
4. Key password
5. Google Play Console owner/admin access
6. Google account recovery information
7. Google Play App Signing / upload-key records
8. Any server/API credentials used by the app

## Storage rule
Keep at least two copies of signing material:
- one encrypted offline copy
- one second secure backup copy

Do NOT store passwords in this repository.

## Before handing project to another developer
Give the developer:
- repository access
- Developer Handoff document
- required non-secret environment instructions
- build workflow information

Do NOT casually give:
- Play Console owner credentials
- primary Google account password
- raw signing passwords

Prefer role-based access wherever possible.

## Before every Play release
- Confirm correct branch/commit
- Confirm GitHub Actions success
- Confirm versionCode increased
- Confirm versionName is correct
- Build signed release
- Verify applicationId
- Smoke test release build
- Upload through intended Play track
- Keep release commit/tag recorded
