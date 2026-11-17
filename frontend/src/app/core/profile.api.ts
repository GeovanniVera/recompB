import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Profile, UpdateProfile } from './api.models';

@Injectable({ providedIn: 'root' })
export class ProfileApi {
  private readonly http = inject(HttpClient);

  get(): Observable<Profile> {
    return this.http.get<Profile>('/api/profile');
  }

  update(body: UpdateProfile): Observable<Profile> {
    return this.http.put<Profile>('/api/profile', body);
  }
}
