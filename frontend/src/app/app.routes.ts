import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
    title: 'Entrar | Recomp3B',
  },
  {
    path: 'today',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/today/today').then((m) => m.TodayPage),
    title: 'Hoy | Recomp3B',
  },
  {
    path: 'analytics',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/analytics/analytics').then((m) => m.AnalyticsPage),
    title: '14 Días | Recomp3B',
  },
  {
    path: 'plan',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/plan/plan').then((m) => m.PlanPage),
    title: 'Mi Plan | Recomp3B',
  },
  { path: '', pathMatch: 'full', redirectTo: 'today' },
  { path: 'dashboard', redirectTo: 'today', pathMatch: 'full' },
  { path: '**', redirectTo: 'today' },
];
