import { HttpEvent, HttpHandler, HttpInterceptor, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { UserService } from './user.service';
import { from, Observable, of, switchMap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthInterceptor implements HttpInterceptor {
  readonly userService = new UserService();
  intercept(req: HttpRequest<any>, handler: HttpHandler): Observable<HttpEvent<any>> {
    console.log('Request URL: ' + req.url);
    if (this.userService.getUser()?.getIdToken()) {
      return from(this.userService.getUser()?.getIdToken()!)
        .pipe(
          switchMap(authToken => {
            return handler.handle(req.clone({
              headers: req.headers.append('X-Authentication-Token', authToken),
            }));
          })
        );
    }
    return handler.handle(req);
  }
}
