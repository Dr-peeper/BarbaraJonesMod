# Complete Airline Airport Mod - Comprehensive Plan
**Minecraft 1.20.1 Fabric Mod**

## Project Overview
A complete airline/airport mod that allows players to build airports, manage flights, and travel between real-world locations using planes.

---

## Phase 1: Core Infrastructure & Setup

### 1.1 Project Structure
```
AirlineAirportMod/
├── src/main/java/com/airline/
│   ├── AirlineAirportMod.java (main class)
│   ├── entity/
│   │   ├── PlaneEntity.java
│   │   ├── PilotEntity.java
│   │   ├── FlightAttendantEntity.java
│   │   ├── GroundCrewEntity.java
│   │   ├── AirTrafficControllerEntity.java
│   │   ├── GateAgentEntity.java
│   │   └── SecurityOfficerEntity.java
│   ├── block/
│   │   ├── AirportBlockEntity.java
│   │   ├── RunwayBlock.java
│   │   ├── GateBlock.java
│   │   ├── SecurityCheckBlock.java
│   │   ├── BaggageCheckBlock.java
│   │   └── AirTrafficControlTowerBlock.java
│   ├── structure/
│   │   ├── AirportStructure.java
│   │   ├── RunwayStructure.java
│   │   ├── TerminalStructure.java
│   │   └── StructureManager.java
│   ├── flight/
│   │   ├── Flight.java
│   │   ├── FlightRoute.java
│   │   ├── FlightScheduler.java
│   │   ├── FlightData.java
│   │   └── WorldLocationData.java
│   ├── network/
│   │   ├── FlightPacket.java
│   │   ├── PlanePositionPacket.java
│   │   └── FlightUpdatePacket.java
│   ├── npc/
│   │   ├── NPCBehavior.java
│   │   ├── NPCSchedule.java
│   │   └── NPCPathfinding.java
│   ├── renderer/
│   │   ├── PlaneModelRenderer.java
│   │   ├── PlaneEntityRenderer.java
│   │   └── AirportBlockRenderer.java
│   ├── model/
│   │   ├── PlaneModel.java
│   │   ├── AirplaneGeometry.java
│   │   └── ModelData.java
│   ├── item/
│   │   ├── BoardingPassItem.java
│   │   └── AirportCardItem.java
│   └── util/
│       ├── FlightCalculator.java
│       ├── LocationMapper.java
│       ├── AirlineConfig.java
│       └── Constants.java
├── src/main/resources/
│   ├── fabric.mod.json
│   ├── assets/airline/
│   │   ├── textures/
│   │   │   ├── entity/
│   │   │   │   └── plane/
│   │   │   ├── block/
│   │   │   └── gui/
│   │   ├── models/
│   │   │   └── entity/
│   │   │       └── plane.json
│   │   ├── blockstates/
│   │   └── lang/
│   │       └── en_us.json
│   └── data/airline/
│       ├── world_locations.json
│       └── flight_routes.json
└── build.gradle
```

### 1.2 Dependencies
- Fabric API 1.20.1
- Fabric Language Kotlin (optional, for cleaner code)
- Sodium (optional, for performance)

---

## Phase 2: Core Entities

### 2.1 PlaneEntity (Primary)
- **Features:**
  - Full 3D model rendering
  - Flight path navigation
  - Passenger capacity tracking
  - Boarding/takeoff/landing states
  - Collision detection
  - Sound effects
- **Data:**
  - Current flight ID
  - Passenger list
  - Fuel level
  - Altitude
  - Speed
  - Heading
  - State (idle, boarding, taxiing, flying, landing)

### 2.2 PilotEntity
- **Behavior:**
  - Spawns at gates before flight
  - Walks to plane cockpit
  - Communicates with players (boarding announcements)
  - Sits in cockpit during flight
  - Announces landing
- **AI:** Path finding from terminal to plane, then to cockpit

### 2.3 FlightAttendantEntity
- **Behavior:**
  - Boards plane before departure
  - Walks through cabin during flight
  - Assists passengers
  - Communicates pre-flight safety
- **States:** Waiting, boarding, in-flight service, deplaning

### 2.4 GroundCrewEntity
- **Behavior:**
  - Arrives after landing
  - Services aircraft (fuel, cleaning)
  - Moves cargo
  - Animates with equipment
- **Equipment:** Luggage cart, fuel truck visuals

### 2.5 AirTrafficControllerEntity
- **Behavior:**
  - Stationed in control tower
  - Communicates with pilots
  - Manages runway traffic
  - Provides in-game lore/immersion
- **Communication:** Text bubbles with instructions

### 2.6 GateAgentEntity
- **Behavior:**
  - Stationed at gates
  - Checks boarding passes
  - Scans luggage
  - Communicates with passengers
- **Interaction:** Manages boarding queue

### 2.7 SecurityOfficerEntity
- **Behavior:**
  - Patrols security areas
  - Checks for boarding passes
  - Operates security gates
  - Can refuse entry without pass

---

## Phase 3: Structures

### 3.1 Airport Structure
- **Components:**
  - Terminal building
  - Gates (4-8 gates per airport)
  - Runway(s)
  - Control tower
  - Parking areas
  - Security checkpoints
  - Baggage claim
- **Building:**
  - Players place an "Airport Core" block
  - Expand by adding gates, runways, etc.
  - Auto-generates terminal if needed
- **Custom NBT data:**
  - Airport ID
  - Runway orientation
  - Gate positions
  - Flight schedule storage

### 3.2 Runway Structure
- **Features:**
  - 300+ block length
  - Lined blocks (white painted concrete)
  - Clearance zones
  - Taxiway system
  - Lighting (optional lamps)
- **Physics:**
  - Planes can only land/takeoff here
  - Collision detection with structures

### 3.3 Gate Structure
- **Features:**
  - Jet bridge connector
  - Signage with flight info
  - Boarding area
  - Baggage handling area
- **Data:**
  - Assigned flight
  - Boarding status
  - Passenger count

### 3.4 Terminal Building
- **Components:**
  - Check-in counters
  - Security gates
  - Boarding areas
  - Baggage claim carousels
  - Restaurants/shops
- **Customizable:** Players can add/remove sections

---

## Phase 4: Flight System

### 4.1 World Locations
**Mapping real world to Minecraft coordinates:**
- Los Angeles: X=0, Z=0
- New York: X=5000, Z=0
- London: X=10000, Z=0
- Sydney: X=15000, Z=5000
- Tokyo: X=15000, Z=-5000
- Mexico City: X=-2000, Z=-3000
- São Paulo: X=6000, Z=-8000

**Distance calculation:**
- Real-world great-circle distance → Minecraft block distance
- 1 block ≈ 50km in real world
- Adjust flight times accordingly

### 4.2 Flight Data Structure
```json
{
  "flightId": "UA123",
  "departure": {
    "airport": "LAX",
    "location": [0, 65, 0],
    "time": 1000
  },
  "arrival": {
    "airport": "JFK",
    "location": [5000, 65, 0],
    "time": 6000
  },
  "aircraft": "Boeing747",
  "capacity": 416,
  "passengers": 300,
  "duration": 5000,  // ticks (5 minutes)
  "state": "BOARDING"
}
```

### 4.3 Flight Scheduler
- **Features:**
  - Create new flights
  - Schedule departures
  - Track flight progress
  - Handle arrivals
  - Remove completed flights
- **Tick Logic:**
  - Every tick: update flight states
  - Every second: update plane position
  - Handle boarding/deplaning

### 4.4 Flight Duration Calculation
```
realWorldDistance = calculateGreatCircleDistance(dep, arr)
minecraftDistance = realWorldDistance / 50  // blocks
flightSpeedPerTick = 2.0  // blocks per tick
estimatedDuration = minecraftDistance / flightSpeedPerTick
cappedDuration = min(estimatedDuration, 5 * 60 * 20)  // Max 5 minutes
```

---

## Phase 5: Plane Model & Rendering

### 5.1 3D Plane Model
**Geometry:**
- Fuselage: elongated cuboid
- Wings: flat rectangular prisms
- Tail: vertical and horizontal stabilizers
- Cockpit: pointed nose
- Landing gear: retractable (visual)
- Engines: under wings

**Dimensions (blocks):**
- Length: 12 blocks
- Wingspan: 10 blocks
- Height: 3 blocks

### 5.2 Plane Rendering
- **Custom Entity Model:** Uses JsonUtil for geometry
- **Textures:** Blue/white livery with airline details
- **Animation:**
  - Landing gear up/down
  - Engine rotation
  - Banking during turns
  - Altitude changes

### 5.3 Passenger Rendering
- Small player models visible through windows
- Seat arrangement based on actual capacity
- Updates as passengers board

---

## Phase 6: NPC Behavior Systems

### 6.1 NPC AI
- **Waypoint System:**
  - Pre-defined paths (gate → plane → seats)
  - Dynamic pathfinding using Minecraft A*
  - Collision avoidance

### 6.2 NPC Schedules
- **Timeline:**
  - T-30 min: NPCs arrive at airport
  - T-15 min: Pilots walk to planes
  - T-10 min: Flight attendants board
  - T-5 min: Passengers board
  - T-0: Doors close, pushback
  - During flight: Attendants walk aisles
  - Landing: Pilots prepare, crew readies
  - Post-landing: Deplaning sequence

### 6.3 NPC Communication
- **Text bubbles** above NPCs
- **Boarding announcements:** "Now boarding group A"
- **Safety announcements:** "Ladies and gentlemen..."
- **Landing announcements:** "We're beginning our descent"

---

## Phase 7: Player Interaction

### 7.1 Boarding Pass System
- **Item:** Boarding Pass (paper)
  - Flight number
  - Seat assignment
  - Barcode (display format)
  - Departure time
- **Obtaining:**
  - Trade with gate agent
  - Command: `/flight book <flight_id>`
  - Find in loot (airport chests)

### 7.2 Flight Booking Interface
- **GUI Screen:**
  - Available flights list
  - Departure/arrival cities
  - Departure times
  - Price (optional, uses emeralds)
  - Book button
- **Interaction:**
  - Right-click airport core or gate agent
  - Displays available flights
  - Confirm booking

### 7.3 Boarding Process
1. Player has valid boarding pass
2. Player approaches gate agent
3. Agent scans pass
4. Door opens if valid
5. Player walks onto plane
6. Finds seat (can sit anywhere)
7. Flight departs automatically at scheduled time

### 7.4 In-Flight Experience
- Player can move around cabin
- View other passengers
- Can't leave plane until landing
- Flight attendant interactions
- Window views of terrain below

### 7.5 Landing & Arrival
- Plane descends and lands
- Doors open automatically
- Players can exit
- Spawned at arrival airport gate
- Flight data logged

---

## Phase 8: Configuration & Customization

### 8.1 Flight Management Commands
```
/flight create <id> <departure> <arrival> <time>
/flight schedule <id>
/flight cancel <id>
/flight list
/flight info <id>
/flight delete <id>
/airport create <location> <size>
/airport info
```

### 8.2 Configuration File
- World flight registry (stored in level.dat custom data)
- Airport locations and gates
- Flight schedules
- Aircraft types and models
- NPC spawn rules

### 8.3 Data Persistence
- **NBT Storage:**
  - Each airport stores its flight queue
  - World data stores global flight registry
  - Player flight history
- **Save/Load:**
  - Serialize on server tick
  - Load on world initialization

---

## Phase 9: Advanced Features

### 9.1 Aircraft Types
- Boeing 747: 416 seats, luxury
- Airbus A380: 555 seats, massive
- Boeing 787: 242 seats, efficient
- Airbus A320: 180 seats, common
- Cessna 172: 4 seats, small planes

### 9.2 Airline Companies
- Different colored liveries
- Different aircraft types
- Different flight routes
- Branded gates/terminals

### 9.3 Dynamic Events
- Flight delays (random chance)
- Cancellations
- Gate changes
- Overbooking handling
- Weather effects (particle effects)

### 9.4 Achievements/Advancement
- "Take Flight" - board first plane
- "World Traveler" - visit all cities
- "Flight Attendant" - complete 100 flights
- "Airport Manager" - build complete airport
- "Frequent Flyer" - accumulate miles

---

## Phase 10: Performance Optimizations

### 10.1 Entity Optimization
- Batch NPC updates
- Frustum culling for off-screen entities
- Reduce tick rate for distant NPCs
- Optimize pathfinding with caching

### 10.2 Network Optimization
- Delta compression for flight data
- Only sync needed data to players
- Batch packet updates

### 10.3 Rendering Optimization
- LOD (Level of Detail) for planes
- Frustum culling
- Batch rendering
- Shader optimization (if applicable)

---

## Implementation Order

1. **Week 1:** Project setup, basic entities, plane model
2. **Week 2:** Flight system, coordinate mapping
3. **Week 3:** NPC AI and behaviors
4. **Week 4:** Player interaction, boarding system
5. **Week 5:** Airport structures, gates, terminals
6. **Week 6:** Configuration, commands, persistence
7. **Week 7:** Advanced features, events, achievements
8. **Week 8:** Performance optimization, polish, testing

---

## Key Technical Decisions

### Model Format
- Use JSON models for simplicity
- Custom rendering if needed for animation
- Use TextureManager for 3D textures

### Movement System
- Linear interpolation between waypoints
- Smooth rotation toward destination
- Collision detection for runways

### State Management
- Finite state machine for flights
- Tick-based updates
- NBT persistence

### Network Architecture
- Server-authoritative flight data
- Client receives rendering data only
- Validation on server-side

---

## Notes for Development

1. **Reference Mods:** Look at Immersive Railroads, Trains in Motion, Minecraft Comes Alive
2. **3D Models:** Can use Blockbench to design (export to JSON)
3. **Textures:** Create realistic airplane textures (512x512 recommended)
4. **Performance:** Test with 20+ planes simultaneously
5. **Testing:** Create test world with multiple airports
6. **Documentation:** Maintain wiki for configuration
