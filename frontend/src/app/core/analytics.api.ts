import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CheckIn, CheckInResult, CreateCheckIn, FourteenDays, WeekComparison } from './api.models';

@Injectable({ providedIn: 'root' })
export class AnalyticsApi {
  private readonly http = inject(HttpClient);

  fourteenDays(endDate: string): Observable<FourteenDays> {
    const params = new HttpParams().set('endDate', endDate);
    return this.http.get<FourteenDays>('/api/analytics/14-days', { params });
  }

  compareWeeks(baseStart: string, currentStart: string): Observable<WeekComparison> {
    const params = new HttpParams().set('baseStart', baseStart).set('currentStart', currentStart);
    return this.http.get<WeekComparison>('/api/analytics/compare-weeks', { params });
  }

  checkIns(): Observable<CheckIn[]> {
    return this.http.get<CheckIn[]>('/api/checkins');
  }

  createCheckIn(body: CreateCheckIn): Observable<CheckInResult> {
    return this.http.post<CheckInResult>('/api/checkins', body);
  }
}
