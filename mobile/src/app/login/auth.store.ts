import { patchState, signalStore, withMethods, withState, WritableStateSource } from '@ngrx/signals';
import { inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { HelperService } from '../helper.service';
import { HttpClient } from '@angular/common/http';
import { Auth, signInWithPopup, signOut } from '@angular/fire/auth';
import { GoogleAuthProvider } from 'firebase/auth';
import { Router } from '@angular/router';
import { ToastController } from '@ionic/angular';
import { StorageService } from '../services/storage.service';
import { MethodsDictionary } from '@ngrx/signals/src/signal-store-models';

type AuthState = {
  apiKey: string,
}

const initialState: AuthState = {
  apiKey: '',
}

const login = async (
  store: WritableStateSource<AuthState>,
  idToken: string,
  http: HttpClient,
  storageService: StorageService,
) => {
  try {
    const url = 'http://52.74.229.194:8080/login'; // `${environment.apiBaseUrl.split('/api')[0]}/login`
    const { token: apiKey } = await firstValueFrom(
      http.post<{ token: string  }>(url, {
        idToken: `Bearer ${idToken}`
      })
    );
    await storageService.set('api_key', apiKey);
    patchState(store, { apiKey: apiKey })
  } catch (e) {
    throw Error;
  }
}

interface Methods extends MethodsDictionary {
  loginWithGoogle(): Promise<void>;
  setApiKey(): Promise<void>;
  signOut(): Promise<void>;
}

export const AuthStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods((
    store,
    helperService = inject(HelperService),
    http = inject(HttpClient),
    auth = inject(Auth),
    router = inject(Router),
    toastCtrl = inject(ToastController),
    storageService = inject(StorageService),
  ): Methods => ({
    async loginWithGoogle(): Promise<void> {
      signInWithPopup(auth, new GoogleAuthProvider())
        .then(async () => login(store, (await helperService.getFirebaseAccessToken())!, http, storageService))
        .then(() => router.navigate(['/']))
        .catch(async (err: Error) => {
          await signOut(auth)
          const toast = await toastCtrl.create({
            message: 'Unable to login at the moment',
            duration: 1500
          });
          toast.present();
        });
    },

    async setApiKey() {
      patchState(store, { apiKey: await storageService.get('api_key') })
    },

    async signOut(): Promise<void> {
      await signOut(auth)
      storageService.remove('api_key');
      router.navigate(['login']);
    }
  }))
);
