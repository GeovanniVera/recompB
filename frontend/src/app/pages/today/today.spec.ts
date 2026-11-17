import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DailySummary } from '../../core/api.models';
import { TodayPage } from './today';

const SUMMARY: DailySummary = {
  date: '2026-11-16',
  isGymDay: true,
  waterMl: 250,
  waterTargetMl: 2950,
  targets: {
    tmb: 1643,
    tdee: 2431,
    targetKcal: 2139,
    minGreenKcal: 2032,
    maxGreenKcal: 2246,
    proteinGrams: 140,
    minGreenProtein: 126,
    carbsGrams: 269,
    fatGrams: 56,
    waterRestMl: 2450,
    waterGymMl: 2950,
  },
  consumed: { kcal: 248, protein: 46.5, carbs: 0, fat: 5.4 },
  remaining: { kcal: 1891, protein: 93.5, carbs: 269, fat: 50.6 },
  missingToTmb: 1395,
  inGreenMargin: false,
  cleanEatingPct: 100,
  entries: [
    {
      id: 1,
      entryName: 'Pechuga/Fajitas Pollo 3B (Cocido)',
      foodItemId: 1,
      grams: 150,
      kcal: 248,
      protein: 46.5,
      carbs: 0,
      fat: 5.4,
      isOffPlan: false,
      loggedTime: '13:05',
    },
  ],
};

describe('TodayPage', () => {
  let fixture: ComponentFixture<TodayPage>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TodayPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TodayPage);
  });

  afterEach(() => {
    http.verify();
  });

  function flushInitial(summary: DailySummary = SUMMARY): void {
    http.expectOne((request) => request.url.includes('/api/daily-logs/')).flush(summary);
    http.expectOne('/api/quick-plates').flush([]);
  }

  it('muestra las calorías, el piso TMB y el aviso preventivo', async () => {
    fixture.detectChanges();
    flushInitial();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('248');
    expect(text).toContain('2139');
    expect(text).toContain('Te faltan 1395 kcal');
    expect(text).toContain('Pechuga/Fajitas Pollo 3B');
    expect(text).toContain('250 ml');
  });

  it('no muestra el aviso cuando ya se alcanzó el piso TMB', async () => {
    fixture.detectChanges();
    flushInitial({ ...SUMMARY, consumed: { ...SUMMARY.consumed, kcal: 1800 }, missingToTmb: 0 });
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).not.toContain('Te faltan');
  });
});
