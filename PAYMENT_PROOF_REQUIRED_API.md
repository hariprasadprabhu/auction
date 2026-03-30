# Payment Proof Required Field - API Integration Guide

## Summary
Added `paymentProofRequired` field to tournament management. This field controls whether payment proof is required for tournament participants.

## Field Details
- **Name**: paymentProofRequired
- **Type**: Boolean (YES/NO)
- **Database Column**: payment_proof_required
- **Nullable**: Yes (defaults to null)

## API Endpoints

### 1. Create Tournament
```
POST /tournaments
Authorization: Bearer {token}
Content-Type: application/json

Body:
{
  "name": "Tournament Name",
  "date": "2026-05-15",
  "sport": "Cricket",
  "totalTeams": 12,
  "totalPlayers": 120,
  "purseAmount": 1000000,
  "playersPerTeam": 11,
  "basePrice": 10000,
  "initialIncrement": 5000,
  "status": "UPCOMING",
  "logo": "URL",
  "paymentProofRequired": true  // NEW FIELD
}

Response: 201 Created
{
  "id": 1,
  "name": "Tournament Name",
  "date": "2026-05-15",
  "sport": "Cricket",
  "totalTeams": 12,
  "totalPlayers": 120,
  "teamAllowed": 2,
  "status": "UPCOMING",
  "purseAmount": 1000000,
  "playersPerTeam": 11,
  "basePrice": 10000,
  "initialIncrement": 5000,
  "logoUrl": "URL",
  "paymentProofRequired": true  // NEW FIELD
}
```

### 2. Update Tournament
```
PUT /tournaments/{id}
Authorization: Bearer {token}
Content-Type: application/json

Body:
{
  "name": "Tournament Name",
  "date": "2026-05-15",
  "sport": "Cricket",
  "totalTeams": 12,
  "totalPlayers": 120,
  "purseAmount": 1000000,
  "playersPerTeam": 11,
  "basePrice": 10000,
  "initialIncrement": 5000,
  "status": "ONGOING",
  "logo": "URL",
  "paymentProofRequired": false  // NEW FIELD
}

Response: 200 OK
{
  "id": 1,
  "name": "Tournament Name",
  "date": "2026-05-15",
  "sport": "Cricket",
  "totalTeams": 12,
  "totalPlayers": 120,
  "teamAllowed": 2,
  "status": "ONGOING",
  "purseAmount": 1000000,
  "playersPerTeam": 11,
  "basePrice": 10000,
  "initialIncrement": 5000,
  "logoUrl": "URL",
  "paymentProofRequired": false  // NEW FIELD
}
```

### 3. Get Tournament (Owner Only)
```
GET /tournaments/{id}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": 1,
  "name": "Tournament Name",
  "date": "2026-05-15",
  "sport": "Cricket",
  "totalTeams": 12,
  "totalPlayers": 120,
  "teamAllowed": 2,
  "status": "UPCOMING",
  "purseAmount": 1000000,
  "playersPerTeam": 11,
  "basePrice": 10000,
  "initialIncrement": 5000,
  "logoUrl": "URL",
  "paymentProofRequired": true  // NEW FIELD
}
```

### 4. Get All Tournaments (Owner's Tournaments)
```
GET /tournaments
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "id": 1,
    "name": "Tournament Name",
    ...
    "paymentProofRequired": true  // NEW FIELD
  }
]
```

### 5. Get Tournament Public Details (No Auth)
```
GET /tournaments/{id}/public
(No authorization required)

Response: 200 OK
{
  "id": 1,
  "name": "Tournament Name",
  ...
  "paymentProofRequired": true  // NEW FIELD (visible to all)
}
```

## Security
✅ Only tournament owner can create/update (via authentication)
✅ Only tournament owner can view their tournaments
✅ Public endpoint shows paymentProofRequired to all users
✅ Full authentication guard on all edit/create operations

## Angular Interface Update
```typescript
export interface TournamentResponse {
  id: number;
  name: string;
  date: Date;
  sport: string;
  totalTeams: number;
  totalPlayers: number;
  teamAllowed: number;
  status: string; // UPCOMING | ONGOING | COMPLETED
  purseAmount: number;
  playersPerTeam: number;
  basePrice: number;
  initialIncrement: number;
  logoUrl?: string;
  paymentProofRequired?: boolean;  // NEW FIELD
}

export interface TournamentRequest {
  name: string;
  date: Date;
  sport: string;
  totalTeams: number;
  totalPlayers: number;
  purseAmount: number;
  playersPerTeam: number;
  basePrice: number;
  initialIncrement?: number;
  status?: string;
  logo?: string;
  paymentProofRequired?: boolean;  // NEW FIELD
}
```

## Files Modified
1. `/src/main/java/com/bid/auction/entity/Tournament.java` - Added field
2. `/src/main/java/com/bid/auction/dto/request/TournamentRequest.java` - Added field
3. `/src/main/java/com/bid/auction/dto/response/TournamentResponse.java` - Added field
4. `/src/main/java/com/bid/auction/service/TournamentService.java` - Updated create/update/toResponse methods

## Database Migration
A new column `payment_proof_required` has been added to the `tournaments` table.

## Usage Example in Angular
```typescript
// Create tournament with payment proof required
const request: TournamentRequest = {
  name: 'IPL 2026',
  date: new Date('2026-05-15'),
  sport: 'Cricket',
  totalTeams: 10,
  totalPlayers: 150,
  purseAmount: 5000000,
  playersPerTeam: 11,
  basePrice: 50000,
  initialIncrement: 10000,
  paymentProofRequired: true  // Enable payment proof
};

// Update tournament
const updateRequest: TournamentRequest = {
  ...tournament,
  paymentProofRequired: false  // Disable payment proof
};
```

