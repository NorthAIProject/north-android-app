# Parity checklist

One row per iOS screen or behaviour. Built from the iOS code inventory, not from `docs/ios/parity.md` (which is out of date). Tick a row when a Maestro flow or a recorded side-by-side check against the iOS app proves it.

**Ph** = phase that delivers the row. **How** = `M` Maestro flow, `U` unit or integration test, `D` real-device check, `S` side-by-side manual check.

Add a row when you find an iOS behaviour that is missing here; the Swift code is the authority.

## Root and shell

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Launching spinner, then the right phase | 1 | U |
| ☐ | Signed out shows login | 1 | M |
| ☐ | Failed phase shows Try Again and Sign Out | 1 | U |
| ☐ | Unreadable token shows a waiting screen and never signs out | 1 | U |
| ☐ | Onboarding phase shows the wizard | 3 | M |
| ☑ | AI consent gate covers everything until answered | 2 | M |
| ☐ | Declined consent: Coach tab shows "AI is off", orb hidden | 3 | M |
| ☐ | Five tabs, each keeps its own stack | 1 | M |
| ☐ | Re-selecting a tab pops to root and reloads | 1 | M |
| ☐ | More tab badge shows crew unread count | 3 | M |
| ☐ | Tab bar hidden in a coach thread and full-screen flows | 3 | S |
| ☐ | 401 signs out; workout snapshot kept for the same account | 3 | U |
| ☐ | Explicit sign-out clears everything and unregisters push | 4 | U |
| ☐ | On foreground: sync health, refresh unread and awards, retry sets | 4 | D |

## Deep links and routing

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Every row of the link table in `docs/navigation.md` | 1 | U |
| ☐ | `khepri://` and `https://kheprios.com/app/…` open the app | 1 | D |
| ☐ | `/i/<code>` and `/i/c/<code>` verified app links | 3 | D |
| ☐ | Link while signed out is held and replayed after sign-in | 1 | U |
| ☐ | Invite and crew-join codes survive sign-up and onboarding | 3 | M |
| ☐ | Unknown link falls back to the browser | 1 | U |
| ☐ | Same crew push tapped twice still lands | 3 | U |

## Auth

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Email sign-in | 1 | M |
| ☐ | Email sign-up with strength meter and mismatch error | 1 | M |
| ☐ | Sign In / Sign Up switch animates, respects reduce motion | 1 | S |
| ☐ | Error banner announced, error haptic | 1 | S |
| ☐ | Forgot password sheet | 1 | M |
| ☐ | Google sign-in | 3 | D |
| ☐ | Passkey sign-in | 3 | D |
| ☐ | Create account with a passkey | 3 | D |
| ☐ | Sign in with Apple (web flow) for an iOS-created account | 3 | D |
| ☐ | Rate-limit (429) message | 1 | U |
| ☐ | 3D emblem on login, flat fallback | 5 | D |

## Onboarding and tour

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Six wizard steps in order | 3 | M |
| ☐ | Draft resumes after the app is quit | 3 | M |
| ☐ | Sign Out available in the wizard | 3 | M |
| ☐ | Health primer leads to the permission request | 4 | D |
| ☐ | Notification primer leads to the permission request | 3 | D |
| ☐ | 3D mascot on welcome, flat fallback | 5 | D |
| ☐ | Three-step tour with spotlight | 3 | S |
| ☐ | "Show Me Around" restarts the tour | 3 | M |

## Today and My Day

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Header with date and coach briefing | 2 | M |
| ☐ | Cards appear together, at most 2 s wait | 2 | S |
| ☐ | Stat grid: streak, check-in, water, sleep | 2 | M |
| ☐ | Stat grid stacks at large font scale | 2 | U |
| ☐ | Next-step card with the one prominent button | 2 | S |
| ☐ | First-week card: day N of 7, Done, review from day 7 | 3 | M |
| ☐ | First-week review sheet | 3 | M |
| ☐ | Recovery card only with watch data | 3 | S |
| ☐ | Outreach card with answer buttons | 3 | M |
| ☐ | Lighter-day card: lighter or keep | 3 | M |
| ☐ | Your-week card opens Recap | 3 | M |
| ☐ | Fitness-week card opens Fitness | 3 | M |
| ☐ | Goals card | 3 | S |
| ☐ | News section only when the ticker is on | 3 | S |
| ☐ | Previous / next day and Today button | 2 | M |
| ☐ | Nudges bell sheet; nudge routes through the navigator | 3 | M |
| ☐ | Reload on pull, foreground, tab re-select, and after any write | 2 | M |
| ☐ | My Day vitals strip | 2 | S |
| ☐ | Eight day cards, each with its detail sheet | 2 | M |
| ☐ | Day timeline and trends chart | 2 | S |
| ☐ | Body card: heat or soreness, tap a muscle, soreness strip | 5 | D |
| ☐ | Body sheet: weight, height, soreness, target weight, blood pressure | 3 | M |
| ☐ | Quick Add: capture, water, caffeine, supplements, screen time, trackers | 3 | M |
| ☐ | Offline: cached Today with "Offline, showing HH:mm" (Android only) | 2 | M |

## Coach

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Conversation list, reflection threads, swipe delete, empty state | 3 | M |
| ☐ | Header ring states | 3 | S |
| ☐ | Reply streams token by token; stop button | 3 | M |
| ☐ | Inline exercise cards open the exercise sheet | 3 | M |
| ☐ | Tool approval card approve / deny | 3 | M |
| ☐ | Thumbs up / down | 3 | M |
| ☐ | Attachments of each allowed type; 8 MB limit | 3 | M |
| ☐ | Reply interrupted by backgrounding is recovered | 3 | D |
| ☐ | Ended reflection thread | 3 | S |
| ☐ | Coach orb: tap opens Coach | 3 | M |
| ☐ | Coach orb: hold to dictate, spoken reply, "Open in chat" | 4 | D |
| ☐ | Memories: keep, discard, pin, exclude, edit, forget, add | 3 | M |
| ☐ | Decisions: list, new, outcome, single-decision link | 3 | M |
| ☐ | "Ask the coach about this" opens a draft, unsent | 3 | M |

## Training

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Empty state: Create a Plan, Import a Plan | 3 | M |
| ☐ | Plan overview: week, header with rationale, days, problems | 3 | M |
| ☐ | Menu: library, muscles, history, follow another, new, import | 3 | M |
| ☐ | Week editor: this/next week, weekdays, back to usual | 3 | M |
| ☐ | Plan intake and creation (long call survives backgrounding) | 3 | D |
| ☐ | Day view: start time | 3 | M |
| ☐ | Day view: swap, remove, move, sets and reps, add exercise | 3 | M |
| ☐ | Only the followed plan is editable | 3 | U |
| ☐ | 409 on edit adopts the newer version with a notice | 3 | U |
| ☐ | Exercise library search and muscle filter | 3 | M |
| ☐ | Exercise sheet from anywhere | 3 | M |
| ☐ | Muscles this week (body map) | 5 | D |
| ☐ | Activity history and log an activity | 3 | M |
| ☐ | Offline: cached plan and library (Android only) | 3 | M |

## Workout

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Starts instantly without the network | 3 | U |
| ☐ | Joins the server session when it answers; notice when refused | 3 | U |
| ☐ | Joins a session already open on the account | 3 | U |
| ☐ | Set entry: weight, reps, kind, RIR, suggestion, last time | 3 | M |
| ☐ | Warm-up logged without advancing the plan | 3 | U |
| ☐ | Rest countdown, +15 s, Skip | 3 | M |
| ☐ | Heavy haptic when rest ends | 3 | D |
| ☐ | Pause and resume, including mid-rest | 3 | U |
| ☐ | Finish and Save, Discard, Keep Going | 3 | M |
| ☐ | Recap with chart; unsaved sets counted | 3 | M |
| ☐ | Failed sets retry 2 s → 60 s, 8 tries, then on foreground and finish | 3 | U |
| ☐ | Refused sets are not auto-retried | 3 | U |
| ☐ | Survives process death mid-rest; rest that ran out is over | 3 | D |
| ☐ | Snapshot discarded after 12 hours | 3 | U |
| ☐ | Ongoing notification with set x of y and countdown | 3 | D |
| ☐ | Rest-over alert when the app is in the background | 3 | D |
| ☐ | Finished workout written to Health Connect | 4 | D |
| ☐ | Widgets refresh after finish | 4 | D |

## Plan import

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Import a workout plan from camera, photo, file | 3 | M |
| ☐ | Draft review and commit; partial import message | 3 | M |
| ☐ | Import a meal plan; preview; commit | 3 | M |
| ☐ | Build a meal plan with AI | 3 | M |

## Nutrition

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Eaten today against the goal; delete an entry | 3 | M |
| ☐ | Log food by ingredient and grams; log a plan meal | 3 | M |
| ☐ | Ingredients list and new ingredient | 3 | M |
| ☐ | Meal plans and plan detail with all sheets | 3 | M |
| ☐ | Speak a meal | 4 | D |
| ☐ | Body & Goal calculator | 3 | M |

## Check-ins, capture, inbox

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Today's check-in form | 2 | M |
| ☐ | Celebration and coach reply bubble | 2 | M |
| ☐ | Mood preset from a link | 2 | U |
| ☐ | Past days: edit, delete; week dots | 3 | M |
| ☐ | Check-in recorded with source `android` | 2 | U |
| ☐ | Capture: parse, preview, commit | 3 | M |
| ☐ | Inbox: add, file as suggested, file elsewhere, dismiss | 3 | M |

## Progress, fitness, horizon, progress check, goals

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Insights with range picker | 3 | M |
| ☐ | Areas by week; each domain view | 3 | M |
| ☐ | Metric detail | 3 | M |
| ☐ | Consistency 12-week grid | 3 | S |
| ☐ | Stats: sleep, cardio, cardio by kind, eating, patterns | 3 | M |
| ☐ | Lifting sections and exercise detail | 3 | M |
| ☐ | Awards card | 3 | M |
| ☐ | Fitness screen from Health Connect | 4 | D |
| ☐ | Horizon: slider, range bars | 3 | M |
| ☐ | Horizon: body figure follows the slider | 5 | D |
| ☐ | Horizon lift detail; share card | 3 | M |
| ☐ | Progress check: photos, measurements, estimate, compare, cadence, delete | 3 | M |
| ☐ | Goals: list, detail, milestones, notes, status, form | 3 | M |

## Reviews

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Weekly review flow, four steps | 3 | M |
| ☐ | Recap 7 / 30 days; share card; post to friends | 3 | M |
| ☐ | Reports: list, detail from markdown, helpful, archive | 3 | M |

## Wellbeing

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Care: water with undo, sleep, habits, reminders, week chart | 3 | M |
| ☐ | Journal with mood trend | 3 | M |
| ☐ | Knowledge: search, note, file, re-read, document view | 3 | M |

## Friends, profiles, awards

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Profile, leaderboard, XP | 3 | M |
| ☐ | Feed with kudos, workout cards, awards | 3 | M |
| ☐ | Invite link share; redeem an invite | 3 | M |
| ☐ | Handle editor; sharing toggles | 3 | M |
| ☐ | Follow by handle; following, followers, blocked lists | 3 | M |
| ☐ | Find contacts: only hashes leave the phone | 3 | U |
| ☐ | Phone number verification (when configured) | 3 | S |
| ☐ | Facebook friends (when configured) | 3 | S |
| ☐ | Other people's profile, workouts, awards | 3 | M |
| ☐ | Awards screen, detail, monthly challenge | 3 | M |
| ☐ | Unlock celebration with coin flip | 3 | S |

## Crews

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Crews list, new crew, join by link | 3 | M |
| ☐ | Rules must be agreed before the first post | 3 | M |
| ☐ | Channels, messages, replies, reactions, edits, pins, mentions | 3 | M |
| ☐ | Report and mute | 3 | M |
| ☐ | Live updates; reconnect after a drop | 3 | D |
| ☐ | Pending message: retry or discard; no duplicates | 3 | U |
| ☐ | Challenge board; remove or block a member; leave | 3 | M |
| ☐ | Unread badges | 3 | M |
| ☐ | Offline: cached messages (Android only) | 3 | M |

## Settings and More

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | More tab sections in iOS order | 3 | S |
| ☐ | Account: name, time zone, use this phone's | 3 | M |
| ☐ | Coaching style and tone | 3 | M |
| ☐ | AI provider and own key | 3 | M |
| ☐ | AI data sharing | 3 | M |
| ☐ | Health Connect: toggle, sync now, new types, stop, stop and delete | 4 | D |
| ☐ | Strava connect, sync, disconnect | 3 | D |
| ☐ | Agents: create with one-time token, revoke, activity | 3 | M |
| ☐ | Telegram link; calendar | 3 | M |
| ☐ | Language, applied to the whole app | 3 | M |
| ☐ | Appearance, saved to the account | 3 | M |
| ☐ | Every notification setting | 3 | M |
| ☐ | Units, diets | 3 | M |
| ☐ | Export as a zip | 3 | M |
| ☐ | Sign out with confirmation | 1 | M |
| ☐ | Delete account, type to confirm | 3 | M |
| ☐ | Review prompt rules (3 days, 24 h, 120 days) | 3 | U |
| ☐ | Daily post with kudos | 3 | M |

## Push, reminders, widgets, share, shortcuts

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | Device registered after sign-in, unregistered at sign-out | 4 | U |
| ☐ | Push tap opens the nudge and routes its link | 4 | D |
| ☐ | Check-in push: mood then energy, filed with the app killed | 4 | D |
| ☐ | Check-in push never overwrites today's existing check-in | 4 | U |
| ☐ | Coach lighter / missed answer buttons | 4 | D |
| ☐ | First-week Done button | 4 | D |
| ☐ | Crew message inline reply, stored once | 4 | D |
| ☐ | No banner for the crew channel on screen | 4 | D |
| ☐ | Workout reminders in the account time zone with lead time | 4 | D |
| ☐ | Reminders survive reboot and zone change | 4 | D |
| ☐ | Reminder Start Workout button | 4 | D |
| ☐ | Today widget; log water in place | 4 | D |
| ☐ | Check-in widget, two taps; half-made check-in expires | 4 | D |
| ☐ | Share to Khepri: Knowledge, Inbox, parse and log | 4 | D |
| ☐ | Launcher shortcuts | 4 | D |

## Health and speech

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | First sync 30 days; later 2-day overlap; 90-day backfill once | 4 | U |
| ☐ | Re-sending a day replaces it | 4 | U |
| ☐ | Background sync | 4 | D |
| ☐ | Dictation in all seven places | 4 | D |
| ☐ | Primer before the microphone prompt | 4 | D |
| ☐ | Coach replies read aloud; other audio ducks; mute | 4 | D |

## Cross-cutting

| ☐ | Behaviour | Ph | How |
|---|---|---|---|
| ☐ | All strings in en, es, pt-PT, pt-BR | 6 | U |
| ☐ | Font scale 200% on every screen | 6 | S |
| ☐ | Screen reader labels on every control | 6 | S |
| ☐ | Reduce motion honoured everywhere | 6 | S |
| ☐ | Light, dark, system | 6 | S |
| ☐ | Rotation and process death keep state | 6 | D |
| ☐ | Failed refresh keeps content; failed save keeps the form | 3 | U |

## Not ported, on purpose

Screen Time report, Form Check, lock-screen widgets, Dynamic Island, Spotlight entities, Siri confirmation dialogs. See `docs/design.md` section 3.
