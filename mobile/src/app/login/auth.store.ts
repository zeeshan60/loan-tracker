import {patchState, signalStore, withMethods, withState} from '@ngrx/signals';
import {inject} from '@angular/core';
import {HelperService} from '../helper.service';
import {HttpClient, HttpContext, HttpContextToken} from '@angular/common/http';
import {Router} from '@angular/router';
import {ToastController} from '@ionic/angular/standalone';
import {StorageService} from '../services/storage.service';
import {DEFAULT_TOAST_DURATION, PRIVATE_API, PUBLIC_API} from '../constants';
import {LoadingController} from '@ionic/angular/standalone';
import {FriendsStore} from '../friends/friends.store';
import {firstValueFrom} from 'rxjs';
import {Auth, signInWithPopup, GoogleAuthProvider} from '@angular/fire/auth';
import {AskForPhoneComponent} from '../ask-for-phone/ask-for-phone.component';
import {Capacitor} from '@capacitor/core';
import {LoginPlugin} from 'zeenom-capacitor-social-login';
import {ModalService} from '../modal.service';
import {environment} from "../../environments/environment";

export const IS_PUBLIC_API = new HttpContextToken<boolean>(() => false);

export interface User {
  uid: string,
  email: string,
  phoneNumber: string | null,
  displayName: string,
  currency: string | null,
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
  user: User | null,
  region: Region | null,
}

const initialState: AuthState = {
  apiKey: '',
  user: null,
  region: null
}

export const AuthStore = signalStore(
  {providedIn: 'root'},
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
  ) => ({
    async loadUserData(userData?: User): Promise<void> {
      patchState(store, {user: userData || await storageService.get('user_data')})
    },
    async loadUserRegion(): Promise<void> {
      patchState(store, {region: await firstValueFrom(http.get<Region>('https://ipapi.co/json'))})
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

    async getLoginPluginToken(provider: string): Promise<string> {
      switch (Capacitor.getPlatform()) {
        case 'ios':
          return LoginPlugin.echo({
            value: JSON.stringify({
              ...environment.firebaseIOS,
              provider: provider
            })
          })
            .then(({value: token}) => token);
        case 'android':
          return LoginPlugin.echo({
            value: JSON.stringify({
              ...environment.firebaseAndroid,
              provider: provider
            })
          })
            .then(({value: token}) => token);
        default:
          return signInWithPopup(auth, new GoogleAuthProvider())
            .then(() => helperService.getFirebaseAccessToken());
      }
    },

    async loginWithGoogle(): Promise<boolean | void> {
      const loader = await loadingCtrl.create();
      loader.present();
      return this.getLoginPluginToken("google")
        .then(async (token) => {
          return this.login(token!);
        })
        .then(() => {
          loader.dismiss();
          if (!store.user()?.phoneNumber) {
            this.askForPhoneNumber();
          }
        })
        .catch(async () => {
          this.signOut();
          await helperService.showToast('Unable to login at the moment', 2000, {
            color: 'danger'
          });
        })
        .finally(() => {
          loader?.dismiss();
        });
    },

    async loginWithApple(): Promise<boolean | void> {
      const loader = await loadingCtrl.create();
      loader.present();
      return this.getLoginPluginToken("apple")
        .then(async (token) => {
          return this.login(token!);
        })
        .then(() => {
          loader.dismiss();
          if (!store.user()?.phoneNumber) {
            this.askForPhoneNumber();
          }
        })
        .catch(async () => {
          this.signOut();
          await helperService.showToast('Unable to login at the moment', 2000, {
            color: 'danger'
          });
        })
        .finally(() => {
          loader?.dismiss();
        });
    },

    async askForPhoneNumber() {
      // todo: will enable later
      // await modalService.showModal({
      //   component: AskForPhoneComponent,
      //   componentProps: {
      //     region: store.region()
      //   }
      // })
    },

    async login(idToken: string) {
      try {
        const url = `${PUBLIC_API}/login`
        const {token: apiKey} = await firstValueFrom(
          http.post<{ token: string }>(url, {
            idToken: `Bearer ${idToken}`
          }, {context: new HttpContext().set(IS_PUBLIC_API, true)})
        );
        await storageService.set('api_key', apiKey);
        patchState(store, {apiKey: apiKey})
        await Promise.all([
          this.fetchAndSaveUserData(),
          friendsStore.loadFriends({showLoader: false})
        ]);
        router.navigate(['/']);
      } catch (e) {
        this.signOut();
        throw new Error('login failed.');
      }
    },
    async setApiKey() {
      patchState(store, {apiKey: await storageService.get('api_key')})
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
