v8.3.0 - The Kosmos Showdown

Cayden can finally reach the Krave Kosmos, and the boss fight actually happens.

New voice clips (recorded by the user, downmixed to mono so they attenuate
with distance instead of blaring from anywhere in the world):
  - Barbara gets three new ambient lines
  - Cayden gets real hurt and death sounds. His custom clips were never
    actually wired before - he was still using the vanilla villager's - and
    he had no ambient voice at all
  - "I KRAVE THE KRAVE" plays every time he eats cereal, and when he lands
    in the Kosmos

Travelling together:
  - Vanilla abandons pets on any dimension change that is not a nether
    portal, so the Krave Door and the Krave Tether both moved the player
    alone. Cayden and pet Barbara now travel in both directions.

The showdown:
  - He ascends on arrival instead of on touching liquid chocolate
  - He hunts the boss across 512 blocks; his 32-block follow range would
    never have found him on that island
  - Meteor barrages every ~4.5 seconds, 3-5 at a time
  - He cannot be killed while ascended - the one fight where he gets the
    full apocalypse arsenal and does not die for using it
  - He powers down when the boss dies, rather than on a 5-minute timer

Two silent breakages fixed along the way:
  - The Krave Monster cuts incoming damage to 5% unless it comes from an
    ascended Cayden, so the meteors would have tickled him. They are now
    attributed to Cayden, and hurt only hostiles - the player, Barbara and
    Cayden can stand in the crater unharmed.
  - The home re-check would have validated Cayden's Overworld house against
    Kosmos terrain every 10 seconds and evicted him mid-fight. Home now
    records its dimension.

Built on top of v8.2.2. Verified reobfuscated: 1579 SRG references.

