# Unconfirmed privacy policy

Effective 27 November 2026

Unconfirmed is a game about investigating cryptids on foot. This policy explains what the game does with information about you. In short: it uses your location to run the game, sends a rough area (about 1 km across) to two services so it can show the weather and work out which US state you're in, and shows a banner ad from Google AdMob on the newspaper's front page. Everything else stays on your phone. There are no accounts and no analytics, and the developer runs no servers. The developer only hears from you if you choose to email a cryptid request.

## What the game uses

### Your location

The game needs your location to work. It measures how far you walk and in which direction, so that clues turn up as you investigate, and it offers cases set in the US state you're in.

- **Precise location** (GPS) is used only on your phone, to track your walking. It is never sent anywhere, including to the ad service.
- **Approximate location** is sent off your phone in two cases, described below. It is your position rounded to a grid square about 1 km across. Neither service is ever sent your exact position.

The game only uses your location while it is open on screen. It never uses your location in the background.

You can refuse or withdraw location permission at any time in your phone's settings. Without it, the game can't offer cases.

### Where your approximate location goes

1. **Weather, from MET Norway.** The game asks the Norwegian Meteorological Institute's Locationforecast service (api.met.no) for the current weather in your grid square, so the scene can match the sky outside. It asks at most once every 30 minutes for each square. The request also contains the game's name and version and a contact address for the developer, as MET Norway requires. It contains nothing that identifies you. MET Norway's handling of requests is covered by its own terms and privacy policy at [api.met.no](https://api.met.no/).
2. **Which US state you're in, from your phone's geocoder.** The game asks your phone's built-in reverse geocoder which state the centre of your grid square is in. On most Android phones, Google Play services provides the geocoder and looks the point up online. Google handles that lookup under [Google's privacy policy](https://policies.google.com/privacy).

Like any internet request, both services can see your phone's IP address. The game sends both requests over encrypted connections.

## Ads

The front page of the game's newspaper has space for one banner ad. The ads come from Google AdMob, using Google's Mobile Ads SDK.

### What the ad service collects

When it shows an ad, Google's SDK collects information from your phone and sends it to Google. This includes:

- your phone's **advertising ID**, a resettable ID Android provides for ads
- your **IP address**, from which Google can estimate a rough location
- **device and app information**, such as the phone's model, operating system, language, and the game's name and version
- **ad activity**, such as which ads were shown and whether you tapped one, and diagnostic information about how the SDK is working.

Google uses this to choose and show ads, measure them, limit how often you see the same one, and prevent fraud. Where you allow it, Google may also use it to show personalized ads. Google's handling of this information is covered by [Google's privacy policy](https://policies.google.com/privacy), and [How Google uses information from sites or apps that use its services](https://policies.google.com/technologies/partner-sites) explains it in more detail.

The game itself doesn't give the ad service your location, your journal, your cases or anything you type. The developer receives only overall reports from AdMob, such as how many ads were shown. These reports don't identify you.

### Your choices

- **Consent.** Where the law requires it, including in the European Economic Area, the UK, Switzerland and US states with their own privacy laws, the game shows Google's consent form when you open it. No ads are requested until your choices allow them. Until then, and whenever no ad is available, the space shows one of the paper's made-up classifieds instead.
- **Changing your mind.** Where the law requires it, the About screen (under Options) has a "Privacy choices for ads" button that reopens the form so you can change what you allowed.
- **Your advertising ID.** You can reset or delete your advertising ID, or opt out of personalized ads, in your phone's settings, usually under Settings → Privacy → Ads or Settings → Google → Ads.

## Requesting a cryptid

The game has an optional form for suggesting a creature it doesn't have yet. When you fill it in, the game opens your own email app with a message to the developer containing what you wrote: the creature's name, where it's said to be seen, your description, and the game's version. The game itself sends nothing. You decide whether to send the email.

If you send it, the developer receives your email address and the contents of the message. They are used only to read and reply to your request, are not shared with anyone, and are deleted on request. To ask for that, reply to the same email thread or write to the address below.

The form never fills in your location. The place in a request is only what you type.

## What stays on your phone

The game saves these on your phone only:

- Your case journal and the photos in it, the case you're working on, and paused cases.
- Your settings: sound, traveler mode and the state you chose for it, and whether you've seen the safety notice.
- The last state the geocoder returned and the grid square it was for, so the game works offline and doesn't repeat the lookup.
- Your ad choices from the consent form. Google's consent software saves these on your phone so you aren't asked again on every launch.

Journal photos aren't saved as pictures. Each one records what the scene needs to draw that moment again, such as where the camera and the creature were and the weather, and never your real location.

If Android backup is turned on, your phone may copy the journal, cases and settings to your own Google account backup, so they come back when you restore or move to a new phone. Google manages this backup, and the developer can't access it. The saved state, the grid square and your ad choices are never included in the backup.

Uninstalling the game or clearing its data in your phone's settings deletes everything the game has saved on your phone.

## What the game doesn't do

- No accounts and no sign-in.
- No analytics or crash reporting of its own, and no tracking beyond what the ad service does as described under [Ads](#ads).
- No access to your contacts, photos, camera, microphone or files.
- No sale of your information. Your information is shared only with the services described above.
- No data kept by the developer, apart from any cryptid request emails you choose to send.

## Age

Unconfirmed is made for adults aged 18 and over. It is not directed at children or teenagers under 18, and the developer does not knowingly collect information from anyone under 18.

## Changes to this policy

If the game starts using information in a new way, this policy will be updated before that version is released, and the effective date above will change. The history of changes to this file is public on GitHub.

## Contact

Questions about this policy: [CONTACT ADDRESS]
