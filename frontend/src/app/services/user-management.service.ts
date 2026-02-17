import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

// A DTO for the user data from the backend
export interface UserDto {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string; // Add email
  department: string; // Add department
}

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {

  private apiUrl = '/v1/user-management'; // Base URL for the new API

  constructor(private http: HttpClient) { }

  /**
   * Fetches all users with the DATA_STEWARD role.
   */
  getStewards(): Observable<UserDto[]> {
    const hardcodedStewards: UserDto[] = [
      { id: 'a1b2c3d4-e5f6-7890-1234-567890abcdef', username: 'john.smith', firstName: 'John', lastName: 'Smith', email: 'john.smith@cbp.dhs.gov', department: 'CSPD' },
      { id: 'b2c3d4e5-f6a7-8901-2345-67890abcdef1', username: 'jane.doe', firstName: 'Jane', lastName: 'Doe', email: 'jane.doe@cbp.dhs.gov', department: 'TASPD' },
      { id: 'c3d4e5f6-a7b8-9012-3456-7890abcdef12', username: 'peter.jones', firstName: 'Peter', lastName: 'Jones', email: 'peter.jones@cbp.dhs.gov', department: 'PSPD' },
    ];
    return new Observable(observer => {
      observer.next(hardcodedStewards);
      observer.complete();
    });
  }

  /**
   * Fetches the ownership permissions for a specific user.
   * @param userId The ID of the user.
   */
  getUserPermissions(userId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/users/${userId}/permissions`);
  }

  /**
   * Updates the ownership permissions for a specific user.
   * @param userId The ID of the user.
   * @param permissions The list of desired permission role names.
   */
  updateUserPermissions(userId: string, permissions: string[]): Observable<void> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      })
    };
    return this.http.put<void>(`${this.apiUrl}/users/${userId}/permissions`, permissions, httpOptions);
  }
}
