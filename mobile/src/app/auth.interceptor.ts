import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of, switchMap } from 'rxjs';
import { AuthStore } from './login/auth.store';

@Injectable({
  providedIn: 'root'
})
export class AuthInterceptor implements HttpInterceptor {
  readonly authStore = inject(AuthStore);
  intercept(req: HttpRequest<any>, handler: HttpHandler): Observable<HttpEvent<any>> {
    return req.url.includes('/api/v1') ?
      of(this.authStore.apiKey())
        .pipe(
          switchMap(authToken => handler.handle(req.clone({
            headers: req.headers.append('Authorization', `Bearer ${authToken}`),
          })))
        ) :
      handler.handle(req);
  }
}
