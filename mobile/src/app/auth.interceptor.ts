import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, retry, switchMap, throwError, timer } from 'rxjs';
import { AuthStore, IS_PUBLIC_API } from './login/auth.store';

@Injectable({
  providedIn: 'root'
})
export class AuthInterceptor implements HttpInterceptor {
  readonly authStore = inject(AuthStore);
  intercept(req: HttpRequest<any>, handler: HttpHandler): Observable<HttpEvent<any>> {
    const reqHandler = req.context.get(IS_PUBLIC_API) ?
      handler.handle(req):
      of(this.authStore.apiKey())
        .pipe(
          switchMap(authToken => handler.handle(req.clone({
            headers: req.headers.append('Authorization', `Bearer ${authToken}`),
          })))
        );

    /**
     * retry the api call which failed because of some unknown server failure
     */
    return reqHandler
      .pipe(
        retry({
          count: 2,
          delay: (error) => {
            if (
              error instanceof HttpErrorResponse &&
              error.status >= 500 &&
              error.status < 600
            ) {
              return timer(2000); // Delay before retry
            }
            // Don't retry for non-5xx errors
            throw error;
          }
        }),
        catchError((error: HttpErrorResponse) => {
          return throwError(() => error);
        })
    );
  }
}
