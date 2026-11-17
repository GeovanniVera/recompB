import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DailySummary } from './api.models';
import { nowTime } from './date.util';

@Injectable({ providedIn: 'root' })
export class DailyApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/daily-logs';

  getDay(date: string): Observable<DailySummary> {
    return this.http.get<DailySummary>(`${this.base}/${date}`);
  }

  patchDay(
    date: string,
    body: { waterMl?: number; addWaterMl?: number; isGymDay?: boolean },
  ): Observable<DailySummary> {
    return this.http.patch<DailySummary>(`${this.base}/${date}`, body);
  }

  addScale(date: string, foodItemId: number, grams: number): Observable<DailySummary> {
    return this.http.post<DailySummary>(`${this.base}/${date}/entries/scale`, {
      foodItemId,
      grams,
      loggedTime: nowTime(),
    });
  }

  addQuickPlate(date: string, quickPlateId: number): Observable<DailySummary> {
    return this.http.post<DailySummary>(`${this.base}/${date}/entries/quick-plate`, {
      quickPlateId,
      loggedTime: nowTime(),
    });
  }

  addOffPlan(date: string, label: string, kcal: number): Observable<DailySummary> {
    return this.http.post<DailySummary>(`${this.base}/${date}/entries/off-plan`, {
      label,
      kcal,
      loggedTime: nowTime(),
    });
  }

  removeEntry(date: string, entryId: number): Observable<DailySummary> {
    return this.http.delete<DailySummary>(`${this.base}/${date}/entries/${entryId}`);
  }
}
