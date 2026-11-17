import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Attaches the bearer token to API calls and clears the session when the
 * backend rejects it. Login endpoints are exempt so a bad code does not
 * log the user out.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const authorized = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      const isLoginCall = request.url.includes('/api/auth/otp/');
      if (error instanceof HttpErrorResponse && error.status === 401 && !isLoginCall) {
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
