import { HttpErrorResponse } from '@angular/common/http';

interface ApiErrorPayload {
  message?: string;
  code?: string;
  fieldErrors?: Record<string, string>;
}

/** Turns a backend error into something worth showing in the UI. */
export function apiMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const payload = error.error as ApiErrorPayload | null;
    if (payload?.fieldErrors) {
      const first = Object.values(payload.fieldErrors)[0];
      if (first) {
        return first;
      }
    }
    if (payload?.message) {
      return payload.message;
    }
    if (error.status === 0) {
      return 'No pudimos conectar con el servidor. Revisa tu conexión.';
    }
  }
  return fallback;
}
