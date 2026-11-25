# Feature spec: friends

> Modules `:feature:friends:domain`, `:data`, `:ui`. Template: `docs/design.md` section 6.3. `K/` = `north-ios-app/khepri/`.
> Read the Swift files listed below before porting; they are the specification.

## What to build


- **Friends:** your profile and leaderboard (four boards, XP and level); feed with kudos, workout cards and awards; invite link (share); handle editor; phone number with a texted code (only if the server says `phone.configured`); "What Followers See" toggles; follow by handle; find contacts (emails and phone numbers hashed with SHA-256 on the phone, only hashes sent: `ContactMatching.swift`); Facebook friends (only if `facebook.configured`); Following, Followers, Blocked with swipe actions.
- **Profiles:** profile, their workouts, their awards, workout detail.
- **Awards:** awards screen, detail sheet, monthly challenge card, unlock celebration (full-screen, pages through new badges, coin flip, "View on Profile"). Coins are drawn on Canvas from `medals.json`.
- **API:** social get, handle, feed, sharing, invites redeem, follows, followers, blocks, friends match, phone verification, Facebook, profiles (3), awards + unseen + seen, kudos, leaderboard.
- **Tests to port:** contact matching, phone numbers, medals.


## iOS source files

| File | Lines |
|---|---|
| `K/khepri/Features/Awards/AwardCoin.swift` | 20 |
| `K/khepri/Features/Awards/AwardDetailSheet.swift` | 51 |
| `K/khepri/Features/Awards/AwardsCard.swift` | 79 |
| `K/khepri/Features/Awards/AwardsCenter.swift` | 81 |
| `K/khepri/Features/Awards/AwardsModel.swift` | 49 |
| `K/khepri/Features/Awards/AwardsScreen.swift` | 116 |
| `K/khepri/Features/Awards/AwardsService.swift` | 30 |
| `K/khepri/Features/Awards/MonthlyChallengeCard.swift` | 37 |
| `K/khepri/Features/Awards/ProfileAwardsScreen.swift` | 25 |
| `K/khepri/Features/Awards/UnlockCelebrationView.swift` | 132 |
| `K/khepri/Features/Friends/ContactMatching.swift` | 119 |
| `K/khepri/Features/Friends/FacebookFriendsScreen.swift` | 155 |
| `K/khepri/Features/Friends/FindContactsScreen.swift` | 186 |
| `K/khepri/Features/Friends/FriendsScreen.swift` | 441 |
| `K/khepri/Features/Friends/FriendsService.swift` | 174 |
| `K/khepri/Features/Friends/LeaderboardScreen.swift` | 210 |
| `K/khepri/Features/Friends/LeaderboardService.swift` | 167 |
| `K/khepri/Features/Friends/PhoneNumbers.swift` | 116 |
| `K/khepri/Features/Friends/PhoneNumberScreen.swift` | 181 |
| `K/khepri/Features/Profile/ProfileLinks.swift` | 29 |
| `K/khepri/Features/Profile/ProfileModel.swift` | 85 |
| `K/khepri/Features/Profile/ProfileScreen.swift` | 184 |
| `K/khepri/Features/Profile/ProfileService.swift` | 37 |
| `K/khepri/Features/Profile/ProfileWorkoutsScreen.swift` | 95 |
| `K/khepri/Features/Profile/WorkoutCardFormat.swift` | 72 |
| `K/khepri/Features/Profile/WorkoutCardView.swift` | 78 |
| `K/khepri/Features/Profile/WorkoutDetailScreen.swift` | 69 |

## Done when

- Every screen above exists with loading, content, empty and failed states.
- The iOS tests named above are ported (see `docs/ios-test-inventory.md`).
- Screenshots recorded: light/dark, font scale 1.0 and 2.0, en and pt-PT.
- A Maestro flow covers the main path.
- The rows for this module in `docs/parity-checklist.md` are ticked.
- Strings exist in en, es, pt-PT, pt-BR.
