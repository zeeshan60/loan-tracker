import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { inject } from '@angular/core';
import { HelperService } from '../helper.service';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ModalController, ToastController } from '@ionic/angular/standalone';
import { StorageService } from '../services/storage.service';
import { MethodsDictionary } from '@ngrx/signals/src/signal-store-models';
import { DEFAULT_TOAST_DURATION, PRIVATE_API, PUBLIC_API } from '../constants';
import { LoadingController } from '@ionic/angular/standalone';
import { FriendsStore } from '../friends/friends.store';
import { firstValueFrom } from 'rxjs';
import { Auth, signInWithPopup } from '@angular/fire/auth';
import { GoogleAuthProvider } from 'firebase/auth';
import { AskForPhoneComponent } from '../ask-for-phone/ask-for-phone.component';
import { Capacitor } from '@capacitor/core';
import { LoginPlugin } from 'zeenom-capacitor-social-login';

export interface User {
  uid: string,
  email: string,
  phoneNumber: string|null,
  displayName: string,
  currency: string|null,
  photoUrl: string,
  emailVerified: boolean
}

export interface Region {
  ip: string,
  city: string,
  region: string,
  region_code: string,
  country_name: string,
  country_code: string,
  timezone: string,
  utc_offset: string,
  country_calling_code: string,
  currency: string,
  currency_name: string,
  languages: string,
}

type AuthState = {
  apiKey: string,
  user: User|null,
  region: Region|null,
}

const initialState: AuthState = {
  apiKey: '',
  user: null,
  region: null
}

interface Methods extends MethodsDictionary {
  loginWithGoogle(): Promise<boolean|void>;
  setApiKey(): Promise<void>;
  signOut(): Promise<void>;
  login(idToken?: string): Promise<void>;
  loadUserData(userData?: User): Promise<void>;
  loadUserRegion(): Promise<void>;
  fetchAndSaveUserData(): Promise<void>;
  updateUserData(data: Partial<User>): Promise<void>;
  askForPhoneNumber(): Promise<void>;
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
    modalCtrl = inject(ModalController),
    storageService = inject(StorageService),
    loadingCtrl = inject(LoadingController),
    friendsStore = inject(FriendsStore),
  ): Methods => ({
    async loadUserData(userData?: User): Promise<void> {
      patchState(store, { user: userData || await storageService.get('user_data') })
    },
    async loadUserRegion(): Promise<void> {
      patchState(store, { region: await firstValueFrom(http.get<Region>('https://ipapi.co/json')) })
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
          duration: DEFAULT_TOAST_DURATION
        });
      } finally {
        await loader.dismiss();
      }
    },
    async loginWithGoogle(): Promise<boolean|void> {
      const inputValue = JSON.stringify({
        CLIENT_ID: "336545645239-ppcpb0k5hc8303p9ek0793f8lkbbqbku.apps.googleusercontent.com",
        REVERSED_CLIENT_ID: "com.googleusercontent.apps.336545645239-ppcpb0k5hc8303p9ek0793f8lkbbqbku",
        API_KEY: "AIzaSyB1qt9hOwlzyBWGIe-grrg0Vgp53tcwoLE",
        GCM_SENDER_ID: "336545645239",
        PLIST_VERSION: "1",
        BUNDLE_ID: "com.zeenomlabs.loantracker",
        PROJECT_ID: "loan-tracker-9b25d",
        STORAGE_BUCKET: "loan-tracker-9b25d.firebasestorage.app",
        IS_ADS_ENABLED: false,
        IS_ANALYTICS_ENABLED: false,
        IS_APPINVITE_ENABLED: true,
        IS_GCM_ENABLED: true,
        IS_SIGNIN_ENABLED: true,
        GOOGLE_APP_ID: "1:336545645239:ios:90e69a58265af386220332"
      });

      const loginPromise = Capacitor.getPlatform() !== 'ios' ? signInWithPopup(auth, new GoogleAuthProvider())
        .then(() => helperService.getFirebaseAccessToken()) : LoginPlugin.echo({value: inputValue})
        .then(({value: token}) => token);

      return loginPromise
        .then(async (token) => {
          const loader = await loadingCtrl.create({ duration: 2000 });
          loader.present();
          await this.login(token!)
          await Promise.all([
            this.fetchAndSaveUserData(),
            friendsStore.loadFriends({ showLoader: false })
          ]);
          await loader.dismiss();
        })
        .then(() => router.navigate(['/']))
        .catch(async (err: Error) => {
          this.signOut();
          const toast = await toastCtrl.create({
            message: 'Unable to login at the moment',
            duration: DEFAULT_TOAST_DURATION
          });
          toast.present();
        });
    },

    async askForPhoneNumber() {
      const modal = await modalCtrl.create({
        component: AskForPhoneComponent,
        componentProps: {
          user: store.user(),
          region: store.region()
        }
      })
      modal.present();
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
      try {
        await storageService.remove('api_key');
        await router.navigate(['login']);
      } catch (e) {
        const toast = await toastCtrl.create({
          message: 'Unable to logout at the moment',
          duration: DEFAULT_TOAST_DURATION
        });
        toast.present();
      }
    }
  }))
);
