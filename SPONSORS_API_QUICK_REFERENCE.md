# Sponsors API - Quick Reference for Angular

## Summary
Two API endpoints for managing tournament sponsors with full authentication:

### Endpoint 1: Add Sponsors
```
POST /tournaments/{tournamentId}/sponsors
Authorization: Bearer {token}
Content-Type: application/json

Body:
[
  { "name": "Sponsor Name", "personName": "Person Name", "personImageUrl": "URL" },
  { "name": "Another Sponsor", "personName": "Another Person", "personImageUrl": "URL" }
]

Response: 201 Created - List of created sponsors with IDs
```

### Endpoint 2: Get Sponsors (Owner Only)
```
GET /tournaments/{tournamentId}/sponsors
Authorization: Bearer {token}

Response: 200 OK - List of all sponsors for that tournament
```

### Bonus: Get Sponsors (Public - No Auth Needed)
```
GET /tournaments/{tournamentId}/sponsors/public
(No authorization required)

Response: 200 OK - List of all sponsors for that tournament
```

## Angular Interface
```typescript
interface Sponsor {
  id: number;
  name: string;
  personName?: string;
  personImageUrl?: string;
  tournamentId?: number;
}
```

## Security
✅ Only tournament owner can add sponsors (endpoint 1)
✅ Only tournament owner can view sponsors (endpoint 2)
✅ Public endpoint available for displaying sponsors without auth (bonus endpoint)

## Files Created
- `/src/main/java/com/bid/auction/entity/Sponsor.java` - Entity
- `/src/main/java/com/bid/auction/dto/request/SponsorRequest.java` - Request DTO
- `/src/main/java/com/bid/auction/dto/response/SponsorResponse.java` - Response DTO
- `/src/main/java/com/bid/auction/repository/SponsorRepository.java` - Repository
- `/src/main/java/com/bid/auction/service/SponsorService.java` - Service
- `/src/main/java/com/bid/auction/controller/SponsorController.java` - Controller

## Modified Files
- `/src/main/java/com/bid/auction/entity/Tournament.java` - Added sponsors relationship

