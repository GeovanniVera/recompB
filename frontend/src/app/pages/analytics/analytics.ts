import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { apiMessage } from '../../core/api-error';
import { AnalyticsApi } from '../../core/analytics.api';
import {
  CheckIn,
  CheckInResult,
  DailyPoint,
  FourteenDays,
  WeekComparison,
} from '../../core/api.models';
import { addDays, formatDate, formatDateLong, todayIso } from '../../core/date.util';
import { inputValue } from '../../core/dom.util';
import { ProfileApi } from '../../core/profile.api';
import { DeltaList } from '../../shared/delta-list';

type View = 'fortnight' | 'weeks' | 'custom';

@Component({
  selector: 'app-analytics',
  imports: [DeltaList],
  templateUrl: './analytics.html',
  styleUrl: './analytics.css',
})
export class AnalyticsPage implements OnInit {
  private readonly analytics = inject(AnalyticsApi);
  private readonly profileApi = inject(ProfileApi);

  readonly today = todayIso();
  readonly views: readonly { id: View; label: string }[] = [
    { id: 'fortnight', label: '14 Días' },
    { id: 'weeks', label: 'Ant. vs Actual' },
    { id: 'custom', label: 'Sem. X vs Actual' },
  ];

  readonly view = signal<View>('fortnight');

  readonly data = signal<FourteenDays | null>(null);
  readonly comparison = signal<WeekComparison | null>(null);
  readonly checkIns = signal<CheckIn[]>([]);

  readonly loading = signal(true);
  readonly loadingComparison = signal(false);
  readonly error = signal<string | null>(null);

  readonly baseStart = signal(addDays(this.today, -13));
  readonly currentStart = addDays(this.today, -6);

  readonly weight = signal('');
  readonly waist = signal('');
  readonly note = signal('');
  readonly saving = signal(false);
  readonly result = signal<CheckInResult | null>(null);
  readonly applyingDeficit = signal(false);
  readonly deficitApplied = signal(false);

  readonly kpis = computed(() => {
    const stats = this.data()?.summary;
    if (!stats) {
      return null;
    }
    return [
      { label: 'Margen verde', value: `${stats.greenDaysPct}%`, hint: `${stats.greenDaysCount}/${stats.totalDays} días` },
      { label: 'Limpieza', value: `${stats.avgCleanEatingPct}%`, hint: 'promedio' },
      { label: 'Kcal / día', value: `${stats.avgKcal}`, hint: `${stats.loggedDays} días registrados` },
      { label: 'Proteína / día', value: `${stats.avgProtein} g`, hint: 'promedio' },
    ];
  });

  readonly chartMax = computed(() => {
    const data = this.data();
    const target = this.targetKcal() ?? 0;
    if (!data) {
      return Math.max(target, 1) * 1.12;
    }
    const maxKcal = Math.max(...data.points.map((point) => point.kcal), 0);
    return Math.max(maxKcal, target, 1) * 1.12;
  });

  readonly targetLine = computed(() => {
    const target = this.targetKcal();
    if (!target) {
      return null;
    }
    return Math.min(100, (target / this.chartMax()) * 100);
  });

  readonly targetKcal = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
    this.profileApi.get().subscribe({
      next: (profile) => this.targetKcal.set(profile.targets.targetKcal),
      error: () => this.targetKcal.set(null),
    });
  }

  // ---------- data ----------

  load(): void {
    this.loading.set(true);
    this.analytics.fourteenDays(this.today).subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(apiMessage(error, 'No pudimos cargar tus métricas.'));
      },
    });
    this.loadCheckIns();
  }

  loadCheckIns(): void {
    this.analytics.checkIns().subscribe({
      next: (items) => this.checkIns.set([...items].reverse()),
      error: () => this.checkIns.set([]),
    });
  }

  setView(view: View): void {
    this.view.set(view);
    if (view === 'custom' && !this.comparison()) {
      this.loadComparison();
    }
  }

  onBaseStart(event: Event): void {
    const value = inputValue(event);
    if (!value) {
      return;
    }
    this.baseStart.set(value);
    this.loadComparison();
  }

  loadComparison(): void {
    this.loadingComparison.set(true);
    this.analytics.compareWeeks(this.baseStart(), this.currentStart).subscribe({
      next: (comparison) => {
        this.comparison.set(comparison);
        this.loadingComparison.set(false);
      },
      error: (error: unknown) => {
        this.loadingComparison.set(false);
        this.error.set(apiMessage(error, 'No pudimos comparar esas semanas.'));
      },
    });
  }

  // ---------- check-in ----------

  submitCheckIn(): void {
    const weight = Number(this.weight().replace(',', '.'));
    const waistRaw = this.waist().trim();
    const waist = waistRaw === '' ? null : Number(waistRaw.replace(',', '.'));

    if (!Number.isFinite(weight) || weight < 30 || weight > 300) {
      this.error.set('El peso debe estar entre 30 y 300 kg.');
      return;
    }
    if (waist !== null && (!Number.isFinite(waist) || waist < 30 || waist > 250)) {
      this.error.set('La cintura debe estar entre 30 y 250 cm.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.result.set(null);
    this.deficitApplied.set(false);

    this.analytics
      .createCheckIn({
        checkinDate: this.today,
        weightKg: weight,
        waistCm: waist,
        note: this.note().trim() || null,
      })
      .subscribe({
        next: (result) => {
          this.saving.set(false);
          this.result.set(result);
          this.weight.set('');
          this.waist.set('');
          this.note.set('');
          this.load();
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.error.set(apiMessage(error, 'No pudimos guardar tu marca.'));
        },
      });
  }

  applyDeficit(): void {
    this.applyingDeficit.set(true);
    this.profileApi.update({ deficitPct: 0.14 }).subscribe({
      next: () => {
        this.applyingDeficit.set(false);
        this.deficitApplied.set(true);
        this.load();
      },
      error: (error: unknown) => {
        this.applyingDeficit.set(false);
        this.error.set(apiMessage(error, 'No pudimos aplicar el ajuste.'));
      },
    });
  }

  // ---------- chart helpers ----------

  barPercent(point: DailyPoint): number {
    const max = this.chartMax();
    if (max <= 0) {
      return 0;
    }
    return Math.max(2, Math.min(100, (point.kcal / max) * 100));
  }

  barClass(point: DailyPoint): string {
    if (point.kcal === 0) {
      return 'bar bar--empty';
    }
    return point.inGreenMargin ? 'bar bar--green' : 'bar bar--amber';
  }

  readonly formatDate = formatDate;
  readonly formatDateLong = formatDateLong;
}
