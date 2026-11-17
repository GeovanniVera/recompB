import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateFoodItem, FoodItem, QuickPlate } from './api.models';

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  searchFoods(query: string): Observable<FoodItem[]> {
    const params = query.trim() ? new HttpParams().set('q', query.trim()) : undefined;
    return this.http.get<FoodItem[]>('/api/foods', { params });
  }

  createFood(body: CreateFoodItem): Observable<FoodItem> {
    return this.http.post<FoodItem>('/api/foods', body);
  }

  quickPlates(): Observable<QuickPlate[]> {
    return this.http.get<QuickPlate[]>('/api/quick-plates');
  }
}
