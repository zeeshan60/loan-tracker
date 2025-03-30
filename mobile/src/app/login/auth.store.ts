import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { HelperService } from '../helper.service';
import { HttpClient } from '@angular/common/http';
import { Auth, signInWithPopup, signOut } from '@angular/fire/auth';
import { GoogleAuthProvider } from 'firebase/auth';
import { Router } from '@angular/router';
import { ToastController } from '@ionic/angular/standalone';
import { StorageService } from '../services/storage.service';
import { MethodsDictionary } from '@ngrx/signals/src/signal-store-models';
import { PRIVATE_API, PUBLIC_API } from '../constants';
import { LoadingController } from '@ionic/angular/standalone';
import { FriendsStore } from '../friends/friends.store';

export interface User {
  uid: string,
  email: string,
  phoneNumber: number|null,
  displayName: string,
  currency: string|null,
  photoUrl: string,
  emailVerified: boolean
}

type AuthState = {
  apiKey: string,
  user: User|null,
}

const initialState: AuthState = {
  apiKey: '',
  user: null
}

interface Methods extends MethodsDictionary {
  loginWithGoogle(): Promise<void>;
  setApiKey(): Promise<void>;
  signOut(): Promise<void>;
  login(idToken: string): Promise<void>;
  loadUserData(userData?: User): Promise<void>;
  fetchAndSaveUserData(): Promise<void>;
  updateUserData(data: Partial<User>): Promise<void>;
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
    loadingCtrl = inject(LoadingController),
    friendsStore = inject(FriendsStore),
  ): Methods => ({
    async loadUserData(userData?: User): Promise<void> {
      patchState(store, { user: userData || await storageService.get('user_data') })
    },
    async fetchAndSaveUserData(): Promise<void> {
      const userData = await firstValueFrom(http.get<User>(`${PRIVATE_API}/users`));
      await storageService.set('user_data', userData);
      await this.loadUserData(userData);
    },
    async updateUserData(data: Partial<User>): Promise<void> {
      const loader = await loadingCtrl.create();
      try {
        await firstValueFrom(http.put<User>(`${PRIVATE_API}/users`, data));
        await this.fetchAndSaveUserData();
      } catch (e) {
        await toastCtrl.create({
          message: 'Unable to save data.',
          duration: 1500
        });
      } finally {
        await loader.dismiss();
      }
    },
    async loginWithGoogle(): Promise<void> {
      signInWithPopup(auth, new GoogleAuthProvider())
        .then(async () => {
          const loader = await loadingCtrl.create({ duration: 2000 });
          loader.present();
          await this.login((await helperService.getFirebaseAccessToken())!)
          await Promise.all([
            this.fetchAndSaveUserData(),
            friendsStore.loadFriends({ showLoader: false })
          ]);
          await loader.dismiss();
        })
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

    async login(idToken: string) {
      try {
        const url = `${PUBLIC_API}/login`
        const { token: apiKey } = await firstValueFrom(
          http.post<{ token: string  }>(url, {
            idToken: `Bearer ${idToken}`
          })
        );
        await storageService.set('api_key', apiKey);
        patchState(store, { apiKey: apiKey })
      } catch (e) {
        throw e;
      }
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
