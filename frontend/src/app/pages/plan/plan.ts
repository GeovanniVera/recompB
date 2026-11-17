import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { apiMessage } from '../../core/api-error';
import { Profile, UpdateProfile } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { ProfileApi } from '../../core/profile.api';
import { THEMES, Theme, ThemeService } from '../../core/theme.service';

interface Section {
  id: string;
  title: string;
  body: string;
}

@Component({
  selector: 'app-plan',
  templateUrl: './plan.html',
  styleUrl: './plan.css',
})
export class PlanPage implements OnInit {
  private readonly profileApi = inject(ProfileApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly themeService = inject(ThemeService);

  readonly themes = THEMES;
  readonly theme = this.themeService.theme;

  readonly profile = signal<Profile | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly savedAt = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly age = signal('');
  readonly height = signal('');
  readonly weight = signal('');
  readonly gymDays = signal('');

  readonly openSection = signal<string | null>('tmb');

  readonly sections: readonly Section[] = [
    {
      id: 'tmb',
      title: 'TMB · Piso vital',
      body: 'Es lo que tu cuerpo quema en reposo absoluto, sin moverte. Funciona como piso: si comes por debajo todos los días, el cuerpo empieza a cobrar músculo.',
    },
    {
      id: 'activity',
      title: 'Factor de actividad',
      body: 'Multiplica tu TMB según cómo vives la semana. 1.48 corresponde a alguien que entrena 4–5 días y camina lo normal del día.',
    },
    {
      id: 'tdee',
      title: 'TDEE · Gasto diario',
      body: 'TMB × factor de actividad. Es lo que gastas en un día realista y el punto del que se resta el déficit.',
    },
    {
      id: 'deficit',
      title: 'Déficit %',
      body: 'Cuánto recortas sobre el TDEE. 12% es un déficit moderado y sostenible para recomposición. Sube a 14% solo cuando peso y cintura llevan dos semanas estancados con buena adherencia.',
    },
    {
      id: 'protein',
      title: 'Proteína g/kg',
      body: 'Se calcula sobre tu peso (2.0 g/kg). Es el macro que protege músculo en déficit: el mínimo verde exige al menos el 90% de esa meta.',
    },
    {
      id: 'fat',
      title: 'Grasas g/kg',
      body: '0.8 g/kg sostiene las hormonas y la saciedad. No conviene bajarlas mucho más aunque quieras recortar calorías.',
    },
    {
      id: 'carbs',
      title: 'Carbohidratos',
      body: 'No tienen un objetivo fijo: son las calorías que sobran después de proteína y grasa. Es el macro que se ajusta solo.',
    },
    {
      id: 'green',
      title: 'Margen verde',
      body: 'Banda de ±5% alrededor de la meta de calorías. Un día cuenta como verde si caes dentro de la banda y llegas al 90% de la proteína objetivo.',
    },
  ];

  private saveTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.saveTimer) {
        clearTimeout(this.saveTimer);
      }
    });
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.profileApi.get().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.syncFields(profile);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(apiMessage(error, 'No pudimos cargar tu plan.'));
      },
    });
  }

  private syncFields(profile: Profile): void {
    this.age.set(String(profile.age));
    this.height.set(String(profile.heightCm));
    this.weight.set(String(profile.currentWeightKg));
    this.gymDays.set(String(profile.gymDaysPerWeek));
  }

  onFieldChange(): void {
    this.savedAt.set(null);
    if (this.saveTimer) {
      clearTimeout(this.saveTimer);
    }
    this.saveTimer = setTimeout(() => this.save(), 600);
  }

  save(): void {
    const age = Number(this.age());
    const heightCm = Number(this.height().replace(',', '.'));
    const weightKg = Number(this.weight().replace(',', '.'));
    const gymDays = Number(this.gymDays());

    if (!Number.isFinite(age) || age < 14 || age > 100) {
      this.error.set('La edad debe estar entre 14 y 100 años.');
      return;
    }
    if (!Number.isFinite(heightCm) || heightCm < 120 || heightCm > 250) {
      this.error.set('La estatura debe estar entre 120 y 250 cm.');
      return;
    }
    if (!Number.isFinite(weightKg) || weightKg < 35 || weightKg > 300) {
      this.error.set('El peso debe estar entre 35 y 300 kg.');
      return;
    }
    if (!Number.isFinite(gymDays) || gymDays < 0 || gymDays > 7) {
      this.error.set('Los días de gym van de 0 a 7.');
      return;
    }

    const body: UpdateProfile = {
      age: Math.round(age),
      heightCm,
      currentWeightKg: weightKg,
      gymDaysPerWeek: Math.round(gymDays),
    };

    this.saving.set(true);
    this.error.set(null);
    this.profileApi.update(body).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.saving.set(false);
        this.savedAt.set(
          new Intl.DateTimeFormat('es-MX', {
            timeZone: 'America/Mexico_City',
            hour: '2-digit',
            minute: '2-digit',
          }).format(new Date()),
        );
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(apiMessage(error, 'No pudimos guardar tu plan.'));
      },
    });
  }

  toggleSection(id: string): void {
    this.openSection.set(this.openSection() === id ? null : id);
  }

  setTheme(theme: Theme): void {
    this.themeService.setTheme(theme);
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
