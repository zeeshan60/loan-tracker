import { PreloadAllModules, provideRouter, RouteReuseStrategy, withPreloading } from '@angular/router';
import { IonicRouteStrategy } from '@ionic/angular';
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideIonicAngular } from '@ionic/angular/standalone';
import { routes } from './app/app.routes';
import { initializeApp, provideFirebaseApp } from '@angular/fire/app';
import { getAuth, provideAuth, initializeAuth, indexedDBLocalPersistence } from '@angular/fire/auth';
import { environment } from './environments/environment';
import 'ionicons';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { AuthInterceptor } from './app/auth.interceptor';
import { IonicStorageModule } from '@ionic/storage-angular';
import { importProvidersFrom } from '@angular/core';
import { provideFirestore, initializeFirestore, persistentLocalCache, persistentMultipleTabManager } from '@angular/fire/firestore';
import { Capacitor } from '@capacitor/core';

bootstrapApplication(AppComponent, {
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    {provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true},
    importProvidersFrom(IonicStorageModule.forRoot()),
    provideHttpClient(
      withInterceptorsFromDi(),
    ),
    provideIonicAngular({
      useSetInputAPI: true
    }),
    provideRouter(routes, withPreloading(PreloadAllModules)),
    provideFirebaseApp(() => initializeApp(environment.firebaseConfig)),
    provideFirestore(() => initializeFirestore(initializeApp(environment.firebaseConfig), {
      localCache: persistentLocalCache({
        tabManager: persistentMultipleTabManager(),
      }),
    })),
    provideAuth(() => {
      if (Capacitor.isNativePlatform()) {
        return initializeAuth(initializeApp(environment.firebaseConfig), {
          persistence: indexedDBLocalPersistence,
        });
      } else {
        return getAuth(initializeApp(environment.firebaseConfig));
      }
    })
  ],
});
