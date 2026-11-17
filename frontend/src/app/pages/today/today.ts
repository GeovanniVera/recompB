import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { apiMessage } from '../../core/api-error';
import { CreateFoodItem, DailySummary, FoodItem, MealEntry, QuickPlate } from '../../core/api.models';
import { CatalogApi } from '../../core/catalog.api';
import { DailyApi } from '../../core/daily.api';
import { formatDate, todayIso } from '../../core/date.util';
import { clampPercent, inputValue, round1 } from '../../core/dom.util';
import { Sheet } from '../../shared/sheet';

type SheetKind = 'none' | 'food' | 'grams' | 'offplan';

@Component({
  selector: 'app-today',
  imports: [Sheet],
  templateUrl: './today.html',
  styleUrl: './today.css',
})
export class TodayPage implements OnInit {
  private readonly daily = inject(DailyApi);
  private readonly catalog = inject(CatalogApi);
  private readonly destroyRef = inject(DestroyRef);

  readonly date = todayIso();
  readonly dateLabel = formatDate(this.date);
  readonly quickGramOptions = [50, 100, 150, 200];
  readonly stateTags = ['Cocido', 'Crudo', 'Pieza', 'Drenado', 'Directo'];

  readonly summary = signal<DailySummary | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  readonly quickPlates = signal<QuickPlate[]>([]);

  readonly sheet = signal<SheetKind>('none');
  readonly foodQuery = signal('');
  readonly foods = signal<FoodItem[]>([]);
  readonly searching = signal(false);
  readonly selectedFood = signal<FoodItem | null>(null);
  readonly grams = signal('100');

  readonly showNewFood = signal(false);
  readonly newFoodName = signal('');
  readonly newFoodState = signal('Cocido');
  readonly newFoodKcal = signal('');
  readonly newFoodProtein = signal('');
  readonly newFoodCarbs = signal('');
  readonly newFoodFat = signal('');
  readonly creatingFood = signal(false);

  readonly offLabel = signal('');
  readonly offKcal = signal('');

  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  readonly kcalBar = computed(() => {
    const data = this.summary();
    if (!data) {
      return null;
    }
    const max = Math.max(data.targets.maxGreenKcal * 1.2, data.targets.targetKcal * 1.2, data.consumed.kcal, 1);
    const toPct = (value: number) => clampPercent((value / max) * 100);
    const bandLeft = toPct(data.targets.minGreenKcal);
    return {
      fill: toPct(data.consumed.kcal),
      bandLeft,
      bandWidth: Math.max(0, toPct(data.targets.maxGreenKcal) - bandLeft),
      tmb: toPct(data.targets.tmb),
      target: toPct(data.targets.targetKcal),
    };
  });

  readonly macroBars = computed(() => {
    const data = this.summary();
    if (!data) {
      return null;
    }
    const build = (value: number, target: number, minGreen?: number) => {
      const max = Math.max(target * 1.25, value, 1);
      return {
        value,
        target,
        percent: clampPercent((value / max) * 100),
        targetPercent: clampPercent((target / max) * 100),
        minGreenPercent:
          minGreen === undefined ? null : clampPercent((minGreen / max) * 100),
      };
    };
    return {
      protein: build(data.consumed.protein, data.targets.proteinGrams, data.targets.minGreenProtein),
      carbs: build(data.consumed.carbs, data.targets.carbsGrams),
      fat: build(data.consumed.fat, data.targets.fatGrams),
    };
  });

  readonly waterPercent = computed(() => {
    const data = this.summary();
    if (!data || data.waterTargetMl <= 0) {
      return 0;
    }
    return clampPercent((data.waterMl / data.waterTargetMl) * 100);
  });

  readonly gramsPreview = computed(() => {
    const food = this.selectedFood();
    const value = Number(this.grams().replace(',', '.'));
    if (!food || !Number.isFinite(value) || value <= 0) {
      return null;
    }
    const factor = value / 100;
    return {
      kcal: Math.round(food.kcalPer100g * factor),
      protein: round1(food.proteinPer100g * factor),
      carbs: round1(food.carbsPer100g * factor),
      fat: round1(food.fatPer100g * factor),
    };
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
      }
    });
  }

  ngOnInit(): void {
    this.loadDay();
    this.catalog.quickPlates().subscribe({
      next: (plates) => this.quickPlates.set(plates),
      error: () => this.quickPlates.set([]),
    });
  }

  // ---------- data ----------

  loadDay(): void {
    this.loading.set(true);
    this.daily.getDay(this.date).subscribe({
      next: (data) => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(apiMessage(error, 'No pudimos cargar tu día.'));
      },
    });
  }

  private apply(request: Observable<DailySummary>, fallback: string, onSuccess?: () => void): void {
    this.busy.set(true);
    this.error.set(null);
    request.subscribe({
      next: (data) => {
        this.summary.set(data);
        this.busy.set(false);
        onSuccess?.();
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.error.set(apiMessage(error, fallback));
      },
    });
  }

  // ---------- hydration & gym ----------

  toggleGym(checked: boolean): void {
    this.apply(this.daily.patchDay(this.date, { isGymDay: checked }), 'No pudimos cambiar el día de gym.');
  }

  addWater(ml: number): void {
    this.apply(this.daily.patchDay(this.date, { addWaterMl: ml }), 'No pudimos registrar el agua.');
  }

  // ---------- quick plates ----------

  addQuickPlate(plate: QuickPlate): void {
    if (this.busy()) {
      return;
    }
    this.apply(this.daily.addQuickPlate(this.date, plate.id), 'No pudimos registrar el plato.');
  }

  // ---------- entries ----------

  deleteEntry(entry: MealEntry): void {
    if (!confirm(`¿Eliminar "${entry.entryName}"?`)) {
      return;
    }
    this.apply(this.daily.removeEntry(this.date, entry.id), 'No pudimos eliminar la entrada.');
  }

  // ---------- food sheet ----------

  openFoodSheet(): void {
    this.sheet.set('food');
    this.showNewFood.set(false);
    if (this.foods().length === 0) {
      this.searchFoods(this.foodQuery());
    }
  }

  closeSheet(): void {
    this.sheet.set('none');
    this.selectedFood.set(null);
  }

  onQueryInput(event: Event): void {
    this.searchFoods(inputValue(event));
  }

  private searchFoods(query: string): void {
    this.foodQuery.set(query);
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => {
      this.searching.set(true);
      this.catalog.searchFoods(query).subscribe({
        next: (items) => {
          this.foods.set(items);
          this.searching.set(false);
        },
        error: (error: unknown) => {
          this.searching.set(false);
          this.error.set(apiMessage(error, 'No pudimos buscar alimentos.'));
        },
      });
    }, 250);
  }

  selectFood(food: FoodItem): void {
    this.selectedFood.set(food);
    this.grams.set(String(food.quickGrams?.[0] ?? 100));
    this.sheet.set('grams');
  }

  backToFoods(): void {
    this.sheet.set('food');
    this.selectedFood.set(null);
  }

  onGramsInput(event: Event): void {
    this.grams.set(inputValue(event));
  }

  setGrams(value: number): void {
    this.grams.set(String(value));
  }

  saveGrams(): void {
    const food = this.selectedFood();
    const value = Number(this.grams().replace(',', '.'));
    if (!food || !Number.isFinite(value) || value <= 0) {
      this.error.set('Escribe los gramos que marcó tu báscula.');
      return;
    }
    this.apply(this.daily.addScale(this.date, food.id, value), 'No pudimos guardar la comida.', () =>
      this.closeSheet(),
    );
  }

  // ---------- new food ----------

  toggleNewFood(): void {
    this.showNewFood.set(!this.showNewFood());
  }

  setNewFoodState(value: string): void {
    this.newFoodState.set(value);
  }

  createFood(): void {
    const name = this.newFoodName().trim();
    const kcal = Number(this.newFoodKcal().replace(',', '.'));
    const protein = Number(this.newFoodProtein().replace(',', '.') || '0');
    const carbs = Number(this.newFoodCarbs().replace(',', '.') || '0');
    const fat = Number(this.newFoodFat().replace(',', '.') || '0');

    if (!name) {
      this.error.set('Ponle nombre al alimento.');
      return;
    }
    if (!Number.isFinite(kcal) || kcal < 0) {
      this.error.set('Las kcal por 100 g no son válidas.');
      return;
    }

    const body: CreateFoodItem = {
      name,
      stateTag: this.newFoodState(),
      kcalPer100g: kcal,
      proteinPer100g: Number.isFinite(protein) ? protein : 0,
      carbsPer100g: Number.isFinite(carbs) ? carbs : 0,
      fatPer100g: Number.isFinite(fat) ? fat : 0,
    };

    this.creatingFood.set(true);
    this.catalog.createFood(body).subscribe({
      next: (created) => {
        this.creatingFood.set(false);
        this.showNewFood.set(false);
        this.newFoodName.set('');
        this.newFoodKcal.set('');
        this.newFoodProtein.set('');
        this.newFoodCarbs.set('');
        this.newFoodFat.set('');
        this.foods.set([created, ...this.foods()]);
        this.selectFood(created);
      },
      error: (error: unknown) => {
        this.creatingFood.set(false);
        this.error.set(apiMessage(error, 'No pudimos crear el alimento.'));
      },
    });
  }

  // ---------- off plan ----------

  openOffPlan(): void {
    this.offLabel.set('');
    this.offKcal.set('');
    this.sheet.set('offplan');
  }

  saveOffPlan(): void {
    const label = this.offLabel().trim();
    const kcal = Number(this.offKcal().replace(',', '.'));
    if (!label) {
      this.error.set('Escribe qué comiste.');
      return;
    }
    if (!Number.isFinite(kcal) || kcal <= 0) {
      this.error.set('Escribe las calorías del antojo.');
      return;
    }
    this.apply(this.daily.addOffPlan(this.date, label, Math.round(kcal)), 'No pudimos registrar el antojo.', () =>
      this.closeSheet(),
    );
  }

  // ---------- helpers for the template ----------

  trackEntry(_index: number, entry: MealEntry): number {
    return entry.id;
  }
}
