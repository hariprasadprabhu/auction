# Sponsors API Documentation

## Overview
The Sponsors API allows tournament owners to add and manage sponsors for their tournaments. Only authenticated users who own a tournament can add or modify sponsors. Public endpoints are available for users to view sponsors.

## API Endpoints

### 1. Add Sponsors to Tournament
**Endpoint:** `POST /tournaments/{tournamentId}/sponsors`

**Authentication:** Required (Bearer Token)

**Authorization:** Only tournament owner can add sponsors

**Description:** Add a list of sponsors to a tournament.

**Path Parameters:**
- `tournamentId` (Long, Required) - The ID of the tournament

**Request Body:**
```json
[
  {
    "name": "Sponsor Company Name",
    "personName": "John Doe",
    "personImageUrl": "https://example.com/image.jpg"
  },
  {
    "name": "Another Sponsor",
    "personName": "Jane Smith",
    "personImageUrl": "https://example.com/image2.jpg"
  }
]
```

**Response (201 Created):**
```json
[
  {
    "id": 1,
    "name": "Sponsor Company Name",
    "personName": "John Doe",
    "personImageUrl": "https://example.com/image.jpg",
    "tournamentId": 1
  },
  {
    "id": 2,
    "name": "Another Sponsor",
    "personName": "Jane Smith",
    "personImageUrl": "https://example.com/image2.jpg",
    "tournamentId": 1
  }
]
```

**Error Responses:**
- `403 Forbidden` - User is not the tournament owner
- `404 Not Found` - Tournament not found
- `400 Bad Request` - Invalid request body (missing required fields)

---

### 2. Get Sponsors for Tournament (Authenticated)
**Endpoint:** `GET /tournaments/{tournamentId}/sponsors`

**Authentication:** Required (Bearer Token)

**Authorization:** Only tournament owner can view their sponsors

**Description:** Retrieve all sponsors for a specific tournament. Only the tournament owner can access this endpoint.

**Path Parameters:**
- `tournamentId` (Long, Required) - The ID of the tournament

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Sponsor Company Name",
    "personName": "John Doe",
    "personImageUrl": "https://example.com/image.jpg",
    "tournamentId": 1
  },
  {
    "id": 2,
    "name": "Another Sponsor",
    "personName": "Jane Smith",
    "personImageUrl": "https://example.com/image2.jpg",
    "tournamentId": 1
  }
]
```

**Error Responses:**
- `403 Forbidden` - User is not the tournament owner
- `404 Not Found` - Tournament not found

---

### 3. Get Sponsors for Tournament (Public)
**Endpoint:** `GET /tournaments/{tournamentId}/sponsors/public`

**Authentication:** Not Required

**Description:** Retrieve all sponsors for a specific tournament. This is a public endpoint used to display sponsors on tournament details page.

**Path Parameters:**
- `tournamentId` (Long, Required) - The ID of the tournament

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Sponsor Company Name",
    "personName": "John Doe",
    "personImageUrl": "https://example.com/image.jpg",
    "tournamentId": 1
  },
  {
    "id": 2,
    "name": "Another Sponsor",
    "personName": "Jane Smith",
    "personImageUrl": "https://example.com/image2.jpg",
    "tournamentId": 1
  }
]
```

**Error Responses:**
- `404 Not Found` - Tournament not found

---

## TypeScript/Angular Interface

```typescript
export interface Sponsor {
  id: number;
  name: string;
  personName?: string;
  personImageUrl?: string;
  tournamentId?: number;
}
```

---

## Angular Service Integration Example

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SponsorService {
  private baseUrl = 'http://your-api-url';

  constructor(private http: HttpClient) {}

  /**
   * Add sponsors to a tournament (owner only)
   */
  addSponsors(tournamentId: number, sponsors: Sponsor[]): Observable<Sponsor[]> {
    return this.http.post<Sponsor[]>(
      `${this.baseUrl}/tournaments/${tournamentId}/sponsors`,
      sponsors
    );
  }

  /**
   * Get sponsors for a tournament (owner only)
   */
  getTournamentSponsors(tournamentId: number): Observable<Sponsor[]> {
    return this.http.get<Sponsor[]>(
      `${this.baseUrl}/tournaments/${tournamentId}/sponsors`
    );
  }

  /**
   * Get sponsors for a tournament (public - no auth required)
   */
  getPublicSponsors(tournamentId: number): Observable<Sponsor[]> {
    return this.http.get<Sponsor[]>(
      `${this.baseUrl}/tournaments/${tournamentId}/sponsors/public`
    );
  }
}
```

---

## Usage Examples

### Adding Sponsors
```typescript
const sponsors: Sponsor[] = [
  {
    name: 'Nike',
    personName: 'John Manager',
    personImageUrl: 'https://example.com/nike.jpg'
  },
  {
    name: 'Adidas',
    personName: 'Sarah Director',
    personImageUrl: 'https://example.com/adidas.jpg'
  }
];

this.sponsorService.addSponsors(tournamentId, sponsors).subscribe(
  (response) => console.log('Sponsors added:', response),
  (error) => console.error('Error adding sponsors:', error)
);
```

### Getting Sponsors (Private)
```typescript
this.sponsorService.getTournamentSponsors(tournamentId).subscribe(
  (sponsors) => this.sponsors = sponsors,
  (error) => console.error('Error fetching sponsors:', error)
);
```

### Getting Sponsors (Public)
```typescript
// No authentication required - safe to call from public pages
this.sponsorService.getPublicSponsors(tournamentId).subscribe(
  (sponsors) => this.sponsors = sponsors,
  (error) => console.error('Error fetching sponsors:', error)
);
```

---

## HTTP Headers Required

For authenticated endpoints, include the Authorization header:

```
Authorization: Bearer {your_jwt_token}
Content-Type: application/json
```

---

## Notes

- Only the tournament owner can add or view sponsors (except for the public endpoint)
- The system automatically handles the tournament association
- Sponsors are automatically deleted when the tournament is deleted
- `personName` and `personImageUrl` are optional fields
- `name` is required for each sponsor

