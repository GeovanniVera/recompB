/** API contracts mirrored from the Spring Boot backend. */

export interface Targets {
  tmb: number;
  tdee: number;
  targetKcal: number;
  minGreenKcal: number;
  maxGreenKcal: number;
  proteinGrams: number;
  minGreenProtein: number;
  carbsGrams: number;
  fatGrams: number;
  waterRestMl: number;
  waterGymMl: number;
}

export interface Macros {
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
}

export interface MealEntry {
  id: number;
  entryName: string;
  foodItemId: number | null;
  grams: number | null;
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
  isOffPlan: boolean;
  loggedTime: string | null;
}

export interface DailySummary {
  date: string;
  isGymDay: boolean;
  waterMl: number;
  waterTargetMl: number;
  targets: Targets;
  consumed: Macros;
  remaining: Macros;
  missingToTmb: number;
  inGreenMargin: boolean;
  cleanEatingPct: number;
  entries: MealEntry[];
}

export interface FoodItem {
  id: number;
  userId: number | null;
  name: string;
  stateTag: string;
  kcalPer100g: number;
  proteinPer100g: number;
  carbsPer100g: number;
  fatPer100g: number;
  quickGrams: number[];
  createdAt: string;
}

export interface CreateFoodItem {
  name: string;
  stateTag: string;
  kcalPer100g: number;
  proteinPer100g: number;
  carbsPer100g: number;
  fatPer100g: number;
  quickGrams?: number[];
}

export interface QuickPlate {
  id: number;
  userId: number | null;
  title: string;
  subtitle: string | null;
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
  createdAt: string;
}

export interface PeriodStats {
  startDate: string;
  endDate: string;
  totalDays: number;
  loggedDays: number;
  avgKcal: number;
  avgProtein: number;
  greenDaysCount: number;
  greenDaysPct: number;
  avgCleanEatingPct: number;
}

export interface DailyPoint {
  date: string;
  dayLabel: string;
  kcal: number;
  protein: number;
  inGreenMargin: boolean;
  cleanEatingPct: number;
}

export interface WeekDeltas {
  kcalDelta: number;
  proteinDelta: number;
  greenPctDelta: number;
  cleanPctDelta: number;
}

export interface FourteenDays {
  startDate: string;
  endDate: string;
  summary: PeriodStats;
  points: DailyPoint[];
  previousWeek: PeriodStats;
  currentWeek: PeriodStats;
  deltas: WeekDeltas;
}

export interface CheckIn {
  id: number;
  checkinDate: string;
  weightKg: number;
  waistCm: number | null;
  greenDaysPct: number | null;
  cleanEatingPct: number | null;
  note: string | null;
  createdAt: string;
}

export interface WeekBlock {
  stats: PeriodStats;
  points: DailyPoint[];
  checkIn: CheckIn | null;
}

export interface WeekComparison {
  base: WeekBlock;
  current: WeekBlock;
  deltas: WeekDeltas;
}

export interface CheckInResult {
  checkIn: CheckIn;
  adjustmentSuggested: boolean;
  suggestedDeficitPct: number | null;
  message: string | null;
}

export interface CreateCheckIn {
  checkinDate?: string;
  weightKg: number;
  waistCm?: number | null;
  note?: string | null;
}

export interface Profile {
  id: number;
  userId: number;
  sex: string;
  age: number;
  heightCm: number;
  currentWeightKg: number;
  gymDaysPerWeek: number;
  activityFactor: number;
  deficitPct: number;
  proteinPerKg: number;
  fatPerKg: number;
  updatedAt: string;
  targets: Targets;
}

export interface UpdateProfile {
  age?: number;
  heightCm?: number;
  currentWeightKg?: number;
  gymDaysPerWeek?: number;
  deficitPct?: number;
}
