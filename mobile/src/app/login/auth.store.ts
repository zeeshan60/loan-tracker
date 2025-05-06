import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { inject } from '@angular/core';
import { HelperService } from '../helper.service';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ToastController } from '@ionic/angular/standalone';
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
import { ModalService } from '../modal.service';

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
  getLoginPluginToken(): Promise<string>;
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
    modalService = inject(ModalService),
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

    async getLoginPluginToken(): Promise<string> {

      const inputValueIOS = JSON.stringify({
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

      const inputValueAndroid = JSON.stringify({
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

      switch (Capacitor.getPlatform()) {
        case 'ios':
          return LoginPlugin.echo({value: inputValueIOS})
            .then(({value: token}) => token);
        case 'android':
          console.log('zeeshan_debug: Android calling echo');
          return LoginPlugin.echo({value: inputValueAndroid})
            .then(({value: token}) => {
              console.log('zeeshan_debug: Android token:', token);
              return token;
            });
        default:
          return signInWithPopup(auth, new GoogleAuthProvider())
            .then(() => helperService.getFirebaseAccessToken());
      }
    },

    async loginWithGoogle(): Promise<boolean|void> {

      const loginPromise = this.getLoginPluginToken();

      return loginPromise
        .then(async (token) => this.login(token!))
        .catch(async (err: Error) => {
          this.signOut();
          await helperService.showToast('Unable to login at the moment', 2000, {
            color: 'danger'
          });
        });
    },

    async askForPhoneNumber() {
      await modalService.showModal({
        component: AskForPhoneComponent,
        componentProps: {
          user: store.user(),
          region: store.region()
        }
      })
    },

    async login(idToken: string) {
      const loader = await loadingCtrl.create({ duration: 2000 });
      try {
        loader.present();
        const url = `${PUBLIC_API}/login`
        console.log('zeeshan_debug: login url:', url);
        const { token: apiKey } = await firstValueFrom(
          http.post<{ token: string  }>(url, {
            idToken: `Bearer ${idToken}`
          })
        );
        await storageService.set('api_key', apiKey);
        patchState(store, { apiKey: apiKey })
        await Promise.all([
          this.fetchAndSaveUserData(),
          friendsStore.loadFriends({ showLoader: false })
        ]);
        router.navigate(['/']);
      } catch (e) {
        this.signOut();
        await helperService.showToast('Unable to login at the moment', 2000, {
          color: 'danger'
        });
      } finally {
        loader.dismiss();
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
