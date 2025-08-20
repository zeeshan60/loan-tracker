import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpStatusCode,
} from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import {
  catchError,
  concatMap,
  from,
  map,
  mergeMap,
  Observable,
  of,
  retry,
  switchMap,
  tap,
  throwError,
  timer,
} from 'rxjs';
import { AuthStore, IS_PUBLIC_API } from './login/auth.store';
import { DEFAULT_TOAST_DURATION } from './constants';
import { ToastController } from '@ionic/angular/standalone';
import { HelperService } from './helper.service';

@Injectable({
  providedIn: 'root'
})
export class AuthInterceptor implements HttpInterceptor {
  readonly authStore = inject(AuthStore);
  readonly toastCtrl = inject(ToastController);
  readonly helperService = inject(HelperService);
  isUnauthorizedHandlingInProgress = false;
  intercept(req: HttpRequest<any>, handler: HttpHandler): Observable<HttpEvent<any>> {
    const reqHandler = req.context.get(IS_PUBLIC_API) ?
      handler.handle(req):
      of(this.authStore.apiKey())
        .pipe(
          switchMap(authToken => handler.handle(req.clone({
            headers: req.headers.append('Authorization', `Bearer ${authToken}nomi`),
          })))
        );

    /**
     * retry the api call which failed because of some unknown server failure
     */
    return reqHandler
      .pipe(
        mergeMap((response: any) => {
          // if (response.url?.endsWith('/friends')) {
          //   return throwError(() => new HttpErrorResponse({
          //     error: 'Unauthorized',
          //     status: 401,
          //     statusText: 'Unauthorized',
          //     url: 'abc/url'
          //   }))
          // }
          return of(response);
        }),
        // retry({
        //   count: 2,
        //   delay: (error) => {
        //     if (
        //       error instanceof HttpErrorResponse &&
        //       error.status >= 500 &&
        //       error.status < 600
        //     ) {
        //       return timer(2000); // Delay before retry
        //     }
        //     // Don't retry for non-5xx errors
        //     throw error;
        //   }
        // }),
        // catchError((error: HttpErrorResponse) => {
        //   console.log(error, error.status)
        //   if (error.status === HttpStatusCode.Unauthorized) {
        //     if (!this.isUnauthorizedHandlingInProgress) {
        //       this.isUnauthorizedHandlingInProgress = true; // Set flag to prevent re-entry
        //       this.authStore.signOut();
        //
        //       return from(this.toastCtrl.create({
        //         message: 'Session expired. Please log in again.',
        //         duration: DEFAULT_TOAST_DURATION
        //       })).pipe(
        //         concatMap(toast => from(toast.present())), // Present the toast
        //         concatMap(() => {
        //           this.helperService.muteToasts(true);
        //           this.isUnauthorizedHandlingInProgress = false; // Reset flag after handling
        //           return throwError(() => error)
        //         })
        //       );
        //     } else {
        //       // If handling is already in progress, just return an empty observable
        //       // or re-throw the error if you need downstream consumers to know.
        //       // For a 401 that leads to logout, `of(null)` is generally appropriate
        //       // to prevent further error handling in the original subscriber.
        //       return of(null);
        //     }
        //   }
        //   return throwError(() => error);
        // })
    );
  }
}
