# Gauntlet Locker

Locks the Gauntlet portal so it cannot be entered.

When the player tries to enter the portal, the plugin consumes the interaction and replaces it with a short client-side warning sequence:

- the portal is covered by a translucent light-grey overlay;
- the overlay is masked around the player and Death so it does not render over character models;
- the Enter menu entry is greyed out;
- the normal interaction is blocked and a red click marker is rendered;
- Halloween Death appears exactly one tile north of the player and performs the halberd Swipe animation;
- Death says: `I told you this was off limits.`;
- the player simultaneously plays the stun animation, stun particle effect, and stun sound effect.

The sequence is entirely client-side and does not send the portal interaction to the game.

## Development notes

| Purpose | ID |
| --- | ---: |
| Gauntlet portal object | 36081 |
| Halloween Death NPC | 5567 |
| Halberd Swipe animation | 440 |
| Player stun animation | 881 |
| Player stun spot animation | 245 |
| Player stun sound | 2727 |
