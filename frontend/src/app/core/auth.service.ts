import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiError, AuthResponse, OtpRequestResponse, User } from './auth.models';

const TOKEN_KEY = 'miscalorias.access_token';
const USER_KEY = 'miscalorias.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly tokenState = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly userState = signal<User | null>(this.readStoredUser());

  readonly token = this.tokenState.asReadonly();
  readonly user = this.userState.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenState() !== null);

  requestOtp(email: string): Observable<OtpRequestResponse> {
    return this.http.post<OtpRequestResponse>('/api/auth/otp/request', { email });
  }

  verifyOtp(email: string, code: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/otp/verify', { email, code })
      .pipe(tap((response) => this.persistSession(response)));
  }

  loadCurrentUser(): Observable<User> {
    return this.http.get<User>('/api/auth/me').pipe(
      tap((user) => {
        this.userState.set(user);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      }),
    );
  }

  logout(): void {
    this.tokenState.set(null);
    this.userState.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  /** Extracts a human readable message from an API error payload. */
  static messageFrom(error: unknown, fallback: string): string {
    const apiError = error as { error?: ApiError } | null;
    return apiError?.error?.message ?? fallback;
  }

  private persistSession(response: AuthResponse): void {
    this.tokenState.set(response.accessToken);
    this.userState.set(response.user);
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
  }

  private readStoredUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as User;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  }
}
