# Unconfirmed privacy policy

Effective 24 November 2026

Unconfirmed is a game about investigating cryptids on foot. This policy explains what the game does with information about you. In short: it uses your location to run the game, sends a rough area (about 1 km across) to two services so it can show the weather and work out which US state you're in, and keeps everything else on your phone. There are no accounts, ads, analytics or tracking, and the developer runs no servers. The developer only hears from you if you choose to email a cryptid request.

## What the game uses

### Your location

The game needs your location to work. It measures how far you walk and in which direction, so that clues turn up as you investigate, and it offers cases set in the US state you're in.

- **Precise location** (GPS) is used only on your phone, to track your walking. It is never sent anywhere.
- **Approximate location** is sent off your phone in two cases, described below. It is your position rounded to a grid square about 1 km across. Neither service is ever sent your exact position.

The game only uses your location while it is open on screen. It never uses your location in the background.

You can refuse or withdraw location permission at any time in your phone's settings. Without it, the game can't offer cases.

### Where your approximate location goes

1. **Weather, from MET Norway.** The game asks the Norwegian Meteorological Institute's Locationforecast service (api.met.no) for the current weather in your grid square, so the scene can match the sky outside. It asks at most once every 30 minutes for each square. The request also contains the game's name and version and a contact address for the developer, as MET Norway requires. It contains nothing that identifies you. MET Norway's handling of requests is covered by its own terms and privacy policy at [api.met.no](https://api.met.no/).
2. **Which US state you're in, from your phone's geocoder.** The game asks your phone's built-in reverse geocoder which state the centre of your grid square is in. On most Android phones, Google Play services provides the geocoder and looks the point up online. Google handles that lookup under [Google's privacy policy](https://policies.google.com/privacy).

Like any internet request, both services can see your phone's IP address. The game sends both requests over encrypted connections.

## Requesting a cryptid

The game has an optional form for suggesting a creature it doesn't have yet. When you fill it in, the game opens your own email app with a message to the developer containing what you wrote: the creature's name, where it's said to be seen, your description, and the game's version. The game itself sends nothing. You decide whether to send the email.

If you send it, the developer receives your email address and the contents of the message. They are used only to read and reply to your request, are not shared with anyone, and are deleted on request. To ask for that, reply to the same email thread or write to the address below.

The form never fills in your location. The place in a request is only what you type.

## What stays on your phone

The game saves these on your phone only:

- Your case journal, the case you're working on, paused cases, and your sound setting.
- The last state the geocoder returned and the grid square it was for, so the game works offline and doesn't repeat the lookup.

If Android backup is turned on, your phone may copy the journal, cases and sound setting to your own Google account backup, so they come back when you restore or move to a new phone. Google manages this backup, and the developer can't access it. The saved state and grid square are never included in the backup.

Uninstalling the game or clearing its data in your phone's settings deletes everything the game has saved on your phone.

## What the game doesn't do

- No accounts and no sign-in.
- No advertising, analytics, crash reporting or tracking.
- No access to your contacts, photos, camera, microphone or files.
- No sale or sharing of your information with anyone beyond the two location lookups above.
- No data kept by the developer, apart from any cryptid request emails you choose to send.

## Age

Unconfirmed is made for adults aged 18 and over. It is not directed at children or teenagers under 18, and the developer does not knowingly collect information from anyone under 18.

## Changes to this policy

If the game starts using information in a new way, this policy will be updated before that version is released, and the effective date above will change. The history of changes to this file is public on GitHub.

## Contact

Questions about this policy: [CONTACT ADDRESS]
