# Team Purse Implementation - Files Reference
## 📂 Complete File Listing
### 🆕 New Files Created
#### 1. Core Entity & Repository
- **`src/main/java/com/bid/auction/entity/TeamPurse.java`**
  - JPA entity for team_purse table
  - 12 fields tracking financial metrics
  - Relationships to Team and Tournament
- **`src/main/java/com/bid/auction/repository/TeamPurseRepository.java`**
  - Database access layer
  - 6 query methods
  - Support for complex lookups
#### 2. Service Layer
- **`src/main/java/com/bid/auction/service/TeamPurseService.java`**
  - 10+ business methods
  - All financial calculations
  - Automatic update logic
  - Transaction management
#### 3. API Layer
- **`src/main/java/com/bid/auction/dto/response/TeamPurseResponse.java`**
  - Response DTO for API
  - 13 fields matching entity
  - Clean data transfer object
- **`src/main/java/com/bid/auction/controller/TeamPurseController.java`**
  - REST controller
  - 3 endpoints (GET)
  - Security annotations
  - Swagger documentation
#### 4. Documentation
- **`TEAM_PURSE_IMPLEMENTATION.md`** (565 lines)
  - Complete implementation guide
  - Architecture overview
  - All method documentation
  - Workflow explanation
- **`TEAM_PURSE_UI_INTEGRATION.md`** (400 lines)
  - UI integration guide
  - TypeScript interfaces
  - Code examples
  - Common patterns
- **`TEAM_PURSE_DATABASE_SCHEMA.md`** (396 lines)
  - Database schema details
  - Column descriptions
  - Query patterns
  - Migration guide
- **`TEAM_PURSE_COMPLETE_SUMMARY.md`** (300+ lines)
  - Implementation summary
  - Feature list
  - Integration checklist
- **`TEAM_PURSE_QUICK_START.md`** (250+ lines)
  - Quick start guide
  - 5-minute integration
  - Common scenarios
  - FAQ
- **`TEAM_PURSE_FILES_REFERENCE.md`** (this file)
  - All files created/modified
---
### 📝 Modified Files
#### 1. Service Layer Changes
**`src/main/java/com/bid/auction/service/TeamService.java`**
- Added: `final TeamPurseService teamPurseService` field
- Modified: `create()` method
  - Line 58-60: Calls `teamPurseService.initializePurse(savedTeam, tournament)`
  - Initializes purse when team created
**`src/main/java/com/bid/auction/service/AuctionPlayerService.java`**
- Added: `final TeamPurseService teamPurseService` field
- Modified: `sell()` method (Lines 248-250)
  - Calls `teamPurseService.updatePurseOnPlayerSold()`
  - Updates purse when player is sold
- Modified: `markUnsold()` method (Lines 259-264)
  - Checks if player was previously sold
  - Calls `teamPurseService.updatePurseOnPlayerUnsold()`
  - Reverts purse when player is marked unsold
**`src/main/java/com/bid/auction/service/TournamentService.java`**
- Added: `final TeamPurseService teamPurseService` field
- Modified: `update()` method (Line 90)
  - Calls `teamPurseService.recalculateAllTeamPurses(updatedTournament)`
  - Recalculates all team purses when tournament changes
---
## 📊 Statistics
### Code Files
```
New Files:        5
- Entities:       1 (TeamPurse.java)
- Repositories:   1 (TeamPurseRepository.java)
- Services:       1 (TeamPurseService.java)
- Controllers:    1 (TeamPurseController.java)
- DTOs:           1 (TeamPurseResponse.java)
Modified Files:   3
- Services:       3 (TeamService, AuctionPlayerService, TournamentService)
Total Code Files: 8
```
### Documentation Files
```
New Files:        6
- TEAM_PURSE_IMPLEMENTATION.md (565 lines)
- TEAM_PURSE_UI_INTEGRATION.md (400 lines)
- TEAM_PURSE_DATABASE_SCHEMA.md (396 lines)
- TEAM_PURSE_COMPLETE_SUMMARY.md (300+ lines)
- TEAM_PURSE_QUICK_START.md (250+ lines)
- TEAM_PURSE_FILES_REFERENCE.md (this file)
Total Doc Lines:  ~2,000+ lines
```
---
## 🔗 Integration Points
### 1. When Team is Created
```
TeamController.create()
  ↓
TeamService.create()
  ↓
NEW: teamPurseService.initializePurse()
  ↓
TeamPurse record created with:
  - initialPurse = tournament.purseAmount / tournament.totalTeams
  - maxBidPerPlayer = 25% of initialPurse
  - reservedFund = 33% of initialPurse
  - availableForBidding = initialPurse - reservedFund
```
### 2. When Player is Sold
```
AuctionPlayerController.sell()
  ↓
AuctionPlayerService.sell()
  ↓
NEW: teamPurseService.updatePurseOnPlayerSold()
  ↓
TeamPurse updated:
  - purseUsed += soldPrice
  - currentPurse = initialPurse - purseUsed
  - playersBought++
  - remainingSlots--
  - availableForBidding recalculated
```
### 3. When Player is Marked Unsold
```
AuctionPlayerController.markUnsold()
  ↓
AuctionPlayerService.markUnsold()
  ↓
NEW: teamPurseService.updatePurseOnPlayerUnsold()
  ↓
TeamPurse reverted:
  - purseUsed -= soldPrice
  - currentPurse = initialPurse - purseUsed
  - playersBought--
  - remainingSlots++
  - availableForBidding recalculated
```
### 4. When Tournament is Updated
```
TournamentController.update()
  ↓
TournamentService.update()
  ↓
NEW: teamPurseService.recalculateAllTeamPurses()
  ↓
All TeamPurse records recalculated:
  - Preserves current spending
  - Recalculates allocation metrics
  - Updates based on new tournament params
```
---
## 🎯 API Endpoints Added
### Endpoint 1: Get All Team Purses
```
GET /api/tournaments/{tournamentId}/team-purses
Returns: List<TeamPurseResponse>
```
### Endpoint 2: Get Specific Team Purse
```
GET /api/tournaments/{tournamentId}/teams/{teamId}/purse
Returns: TeamPurseResponse
```
### Endpoint 3: Get Team Purses Across Tournaments
```
GET /api/teams/{teamId}/purses
Returns: List<TeamPurseResponse>
```
---
## 💾 Database Changes
### New Table: `team_purse`
```sql
Columns: 12
Indexes: 2 (tournament_id, team_id)
Constraints: 
  - PRIMARY KEY (id)
  - UNIQUE (team_id, tournament_id)
  - FOREIGN KEY team_id → teams(id)
  - FOREIGN KEY tournament_id → tournaments(id)
Rows: 1 per team per tournament
```
---
## 📦 Dependencies
No new external dependencies added.
Uses existing:
- Spring Boot
- Spring Data JPA
- Lombok
- Jakarta Persistence API
---
## ✅ Compilation Verification
```
✓ TeamPurse.java - No errors
✓ TeamPurseRepository.java - No errors
✓ TeamPurseService.java - No errors
✓ TeamPurseResponse.java - No errors (warning: unused class - expected)
✓ TeamPurseController.java - No errors
✓ TeamService.java - No errors
✓ AuctionPlayerService.java - No errors
✓ TournamentService.java - No errors
Total: 8 files, 0 compilation errors
```
---
## 🚀 Deployment Checklist
- [ ] All 8 code files in correct locations
- [ ] Database will auto-create team_purse table on startup
- [ ] 3 API endpoints accessible via REST
- [ ] Integration points working (TeamService, AuctionPlayerService, TournamentService)
- [ ] Documentation available for UI team
- [ ] No external dependencies to install
---
## 📋 File Structure
```
auction/
├── src/main/java/com/bid/auction/
│   ├── entity/
│   │   └── TeamPurse.java ✨ NEW
│   ├── repository/
│   │   └── TeamPurseRepository.java ✨ NEW
│   ├── service/
│   │   ├── TeamService.java (modified)
│   │   ├── AuctionPlayerService.java (modified)
│   │   ├── TournamentService.java (modified)
│   │   └── TeamPurseService.java ✨ NEW
│   ├── dto/response/
│   │   └── TeamPurseResponse.java ✨ NEW
│   └── controller/
│       └── TeamPurseController.java ✨ NEW
│
└── Documentation/
    ├── TEAM_PURSE_IMPLEMENTATION.md ✨ NEW
    ├── TEAM_PURSE_UI_INTEGRATION.md ✨ NEW
    ├── TEAM_PURSE_DATABASE_SCHEMA.md ✨ NEW
    ├── TEAM_PURSE_COMPLETE_SUMMARY.md ✨ NEW
    ├── TEAM_PURSE_QUICK_START.md ✨ NEW
    └── TEAM_PURSE_FILES_REFERENCE.md ✨ NEW (this file)
```
---
## 🔍 Quick Navigation
| Need | File |
|------|------|
| Quick start | TEAM_PURSE_QUICK_START.md |
| UI integration | TEAM_PURSE_UI_INTEGRATION.md |
| All details | TEAM_PURSE_IMPLEMENTATION.md |
| Database | TEAM_PURSE_DATABASE_SCHEMA.md |
| Summary | TEAM_PURSE_COMPLETE_SUMMARY.md |
| Service code | TeamPurseService.java |
| API endpoints | TeamPurseController.java |
| Database entity | TeamPurse.java |
---
## 📞 Support
For any questions about:
- **Implementation**: See TEAM_PURSE_IMPLEMENTATION.md
- **UI Integration**: See TEAM_PURSE_UI_INTEGRATION.md
- **Database**: See TEAM_PURSE_DATABASE_SCHEMA.md
- **Quick Help**: See TEAM_PURSE_QUICK_START.md
- **Code**: Check the Java source files directly
---
**All files are in place and ready for use. Build the project and start integrating with your UI!**
