# Public API Access for Tournament Registration

## Overview

This guide explains how the registration flow works without requiring users to be logged in, and the security approach used.

## Architecture

### Two-Tier API Pattern

Your registration system uses a **two-tier API pattern** to separate public access from authenticated access:

```
┌─────────────────────────────────────────────────────────────┐
│                    UNAUTHENTICATED USER                     │
│              (Pre-registration/Discovery Phase)             │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
        ▼                                     ▼
GET /api/tournaments/{id}/public    POST /api/players/register/{id}
(View tournament details)           (Submit registration form)
✓ No authentication needed          ✓ No authentication needed
✓ Read-only                         ✓ Multipart form data
✓ 8-second timeout                  ✓ Creates player in DB


┌─────────────────────────────────────────────────────────────┐
│                      AUTHENTICATED USER                     │
│               (Admin/Tournament Owner Operations)           │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
        ▼                                     ▼
GET /api/tournaments/{id}           /api/tournaments/{id}/...
(Manage own tournament)              (Modify tournament)
✓ JWT Token required                 ✓ JWT Token required
✓ Ownership verification             ✓ Admin only
```

## Available Public Endpoints

### 1. GET `/api/tournaments/{tournamentId}/public`

**Purpose:** Fetch tournament details for the registration page (no login required)

**Usage in Angular Register Component:**
```typescript
ngOnInit() {
  this.tournamentService.getPublicDetails(this.tournamentId).subscribe(
    (tournament) => {
      this.tournamentName = tournament.name;
      this.sport = tournament.sport;
      this.date = tournament.date;
      this.totalTeams = tournament.totalTeams;
      this.totalPlayers = tournament.totalPlayers;
      this.playersPerTeam = tournament.playersPerTeam;
    }
  );
}
```

**Response Example:**
```json
{
  "id": 1,
  "name": "IPL 2025",
  "sport": "Cricket",
  "date": "2025-04-15",
  "totalTeams": 10,
  "totalPlayers": 200,
  "playersPerTeam": 11,
  "purseAmount": 100000000,
  "basePrice": 10000,
  "status": "UPCOMING",
  "logoUrl": "/api/tournaments/1/logo"
}
```

**Security:** 
- No authentication required
- Returns only public tournament info
- Cannot be used to modify data

---

### 2. POST `/api/players/register/{tournamentId}`

**Purpose:** Register a new player for a tournament (multipart form data)

**Usage in Angular Register Component:**
```typescript
submitForm() {
  const formData = new FormData();
  formData.append('firstName', this.form.value.firstName);
  formData.append('lastName', this.form.value.lastName);
  formData.append('dob', this.form.value.dob);
  formData.append('role', this.form.value.role);
  formData.append('photo', this.form.value.photo);
  formData.append('paymentProof', this.form.value.paymentProof);

  this.playerService.register(this.tournamentId, formData).subscribe(
    (response) => {
      this.successMessage = 'Registered successfully!';
      // Player status will be PENDING for admin approval
    }
  );
}
```

**Request:**
- Method: `POST`
- Content-Type: `multipart/form-data`
- Fields:
  - `firstName` (required, string)
  - `lastName` (optional, string)
  - `dob` (required, date)
  - `role` (required, enum: BATSMAN, BOWLER, ALL_ROUNDER, WICKET_KEEPER)
  - `photo` (required, file)
  - `paymentProof` (required, file)

**Response Example:**
```json
{
  "id": 42,
  "firstName": "Rohit",
  "lastName": "Sharma",
  "dob": "1987-04-30",
  "role": "BATSMAN",
  "status": "PENDING",
  "photoUrl": "/api/players/42/photo",
  "createdAt": "2025-03-17T10:30:00Z"
}
```

**Security:**
- No authentication required
- Registration creates a PENDING player
- Requires admin approval before auction participation
- File uploads have size/type validation

---

### 3. GET `/api/tournaments/{tournamentId}/logo`

**Purpose:** Fetch tournament logo (already public in your config)

**Usage in Angular:**
```html
<img [src]="'api/tournaments/' + tournamentId + '/logo'" alt="Logo">
```

**Security:** Public access (no auth needed)

---

## Updated Security Configuration

The `SecurityConfig.java` has been updated with the following public endpoints:

```java
.authorizeHttpRequests(auth -> auth
    // Swagger UI / OpenAPI docs
    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
        .permitAll()
    // Public auth endpoints
    .requestMatchers("/api/auth/**").permitAll()
    
    // PUBLIC REGISTRATION FLOW
    .requestMatchers(HttpMethod.GET, "/api/tournaments/*/public")
        .permitAll()  // ← Tournament details (8-second timeout)
    .requestMatchers(HttpMethod.POST, "/api/players/register/**")
        .permitAll()  // ← Player registration
    
    // Public image endpoints
    .requestMatchers(HttpMethod.GET, "/api/tournaments/*/logo").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/teams/*/logo").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/auction-players/*/photo").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/players/*/photo").permitAll()
    
    // Everything else requires authentication
    .anyRequest().authenticated()
)
```

---

## Angular Service Integration

### Update Your `tournament.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TournamentService {
  private apiUrl = '/api/tournaments';

  constructor(private http: HttpClient) {}

  // For authenticated users (admin)
  getById(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}`);
  }

  // For unauthenticated users (registration page)
  getPublicDetails(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/public`)
      .pipe(timeout(8000)); // 8-second timeout
  }

  getLogo(id: number): string {
    return `${this.apiUrl}/${id}/logo`;
  }
}
```

### Update Your `player.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PlayerService {
  private apiUrl = '/api/players';

  constructor(private http: HttpClient) {}

  // Public registration endpoint
  register(tournamentId: number, formData: FormData): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/register/${tournamentId}`,
      formData
      // Note: Do NOT set Content-Type header, 
      // browser will set it automatically for multipart
    );
  }

  // For authenticated users
  getPhoto(id: number): string {
    return `${this.apiUrl}/${id}/photo`;
  }

  getPaymentProof(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/payment-proof`, {
      responseType: 'blob'
    });
  }
}
```

---

## Register Component Example

```typescript
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TournamentService } from './services/tournament.service';
import { PlayerService } from './services/player.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  tournamentId: number;
  tournament: any;
  form: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';

  roles = ['BATSMAN', 'BOWLER', 'ALL_ROUNDER', 'WICKET_KEEPER'];

  constructor(
    private route: ActivatedRoute,
    private tournamentService: TournamentService,
    private playerService: PlayerService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: [''],
      dob: ['', Validators.required],
      role: ['', Validators.required],
      photo: [null, Validators.required],
      paymentProof: [null, Validators.required]
    });
  }

  ngOnInit() {
    this.tournamentId = +this.route.snapshot.paramMap.get('tournamentId');
    this.loadTournamentDetails();
  }

  loadTournamentDetails() {
    this.loading = true;
    this.tournamentService.getPublicDetails(this.tournamentId).subscribe({
      next: (data) => {
        this.tournament = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load tournament details';
        this.loading = false;
      }
    });
  }

  onFileSelected(event: Event, fieldName: string) {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files && files.length > 0) {
      this.form.patchValue({
        [fieldName]: files[0]
      });
    }
  }

  submitForm() {
    if (!this.form.valid) {
      this.errorMessage = 'Please fill all required fields';
      return;
    }

    const formData = new FormData();
    formData.append('firstName', this.form.value.firstName);
    formData.append('lastName', this.form.value.lastName || '');
    formData.append('dob', this.form.value.dob);
    formData.append('role', this.form.value.role);
    formData.append('photo', this.form.value.photo);
    formData.append('paymentProof', this.form.value.paymentProof);

    this.loading = true;
    this.playerService.register(this.tournamentId, formData).subscribe({
      next: (response) => {
        this.successMessage = 'Registration successful! Your status is PENDING. Admin will review and approve.';
        this.form.reset();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Registration failed';
        this.loading = false;
      }
    });
  }
}
```

---

## Workflow Flow Diagram

```
1. User visits registration page
   URL: /register/:tournamentId
   ↓
2. Component initializes (ngOnInit)
   - NO login required
   - Calls: GET /api/tournaments/{id}/public
   ↓
3. Tournament details displayed
   - Name, sport, date, requirements
   - User fills registration form
   ↓
4. User submits form
   - Calls: POST /api/players/register/{id}
   - Sends: multipart form data
   - NO JWT token needed
   ↓
5. Player created in database
   - Status: PENDING
   - Awaits admin approval
   ↓
6. Success confirmation
   - User sees "Registration successful"
   - Admin sees pending approval
```

---

## Security Considerations

### ✅ What's Protected:

1. **Tournament Management**
   - `POST /api/tournaments` - Create (admin only)
   - `PUT /api/tournaments/{id}` - Update (owner only)
   - `DELETE /api/tournaments/{id}` - Delete (owner only)

2. **Player Approval**
   - `PATCH /api/players/{id}/approve` - Requires admin auth
   - `PATCH /api/players/{id}/reject` - Requires admin auth
   - `POST /api/players/{id}/add-to-auction` - Requires admin auth

3. **Payment Proof**
   - `GET /api/players/{id}/payment-proof` - Requires auth (ownership check)

### ✅ What's Public:

1. **Discovery**
   - `GET /api/tournaments/{id}/public` - Tournament details
   - `GET /api/tournaments/{id}/logo` - Tournament logo

2. **Registration**
   - `POST /api/players/register/{id}` - Self-registration
   - `GET /api/players/{id}/photo` - Public photo view

---

## Best Practices

### 1. **Timeout Handling in Angular**
```typescript
import { timeout } from 'rxjs/operators';

getPublicDetails(id: number): Observable<any> {
  return this.http.get(`/api/tournaments/${id}/public`)
    .pipe(timeout(8000)); // 8 seconds as per requirement
}
```

### 2. **Error Handling**
```typescript
submitForm() {
  this.playerService.register(this.tournamentId, formData).subscribe({
    next: (response) => {
      // Success
    },
    error: (error) => {
      if (error.name === 'TimeoutError') {
        console.error('Request timeout');
      } else if (error.status === 400) {
        console.error('Validation error', error.error);
      } else {
        console.error('Server error', error);
      }
    }
  });
}
```

### 3. **File Upload Validation**
```typescript
onPhotoSelected(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (file) {
    if (file.size > 5 * 1024 * 1024) { // 5MB limit
      this.errorMessage = 'Photo must be less than 5MB';
      return;
    }
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.errorMessage = 'Only JPEG and PNG allowed';
      return;
    }
    this.form.patchValue({ photo: file });
  }
}
```

### 4. **CORS Configuration** (Already set)
Your `SecurityConfig` is properly configured for CORS:
```java
config.setAllowedOrigins(List.of("http://localhost:4200"));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
```

---

## Testing the Endpoints

### Using cURL:

```bash
# Test public tournament details (NO auth)
curl -X GET http://localhost:8080/api/tournaments/1/public

# Test player registration (NO auth)
curl -X POST http://localhost:8080/api/players/register/1 \
  -F "firstName=Rohit" \
  -F "lastName=Sharma" \
  -F "dob=1987-04-30" \
  -F "role=BATSMAN" \
  -F "photo=@photo.jpg" \
  -F "paymentProof=@receipt.pdf"
```

### Using Postman:

1. **GET /api/tournaments/{id}/public**
   - No Authorization header
   - Should return tournament details

2. **POST /api/players/register/{id}**
   - No Authorization header
   - Content-Type: multipart/form-data
   - Select files for photo and paymentProof
   - Should return created player with ID

---

## Summary

Your registration system now works with:
- ✅ **No login required** for tournament discovery
- ✅ **No login required** for player registration
- ✅ **Public API endpoints** properly secured
- ✅ **Admin approval workflow** for registrations
- ✅ **Multipart file uploads** for documents

Users can completely register without authentication, and admins can manage approvals after login.

