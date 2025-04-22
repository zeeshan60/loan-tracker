import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { IonApp, IonRouterOutlet } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  ellipse,
  logoGoogle,
  square,
  triangle,
  peopleOutline,
  settingsOutline,
  addCircleOutline,
  logOutOutline,
  addOutline,
  person,
  mailOutline,
  callOutline,
  listOutline,
  receiptOutline,
  trashOutline,
  createOutline,
  closeCircle,
  checkmarkOutline,
} from 'ionicons/icons';
import { FriendsStore } from './friends/friends.store';
import { IonicStorageModule } from '@ionic/storage-angular';
import { AuthStore } from './login/auth.store';
import { StorageService } from './services/storage.service';
import { Capacitor } from '@capacitor/core';
import { Auth, getAuth, getRedirectResult, onAuthStateChanged } from '@angular/fire/auth';
import { GoogleAuthProvider } from 'firebase/auth';
import { environment } from '../environments/environment';
import { FirebaseApp } from '@angular/fire/app';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  standalone: true,
  imports: [
    IonApp,
    IonRouterOutlet,
    IonicStorageModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {
  readonly authStore = inject(AuthStore);
  readonly storageService = inject(StorageService);
  readonly friendsStore = inject(FriendsStore);
  auth = inject(Auth);

  constructor() {
    addIcons({
      triangle,
      ellipse,
      square,
      logoGoogle,
      peopleOutline,
      settingsOutline,
      addCircleOutline,
      logOutOutline,
      addOutline,
      person,
      mailOutline,
      callOutline,
      listOutline,
      receiptOutline,
      trashOutline,
      createOutline,
      closeCircle,
      checkmarkOutline
    });
  }

  async ngOnInit() {
    const auth = getAuth()
    setTimeout(() => {
      onAuthStateChanged(auth, (user) => {
        if (user) {
          console.log("User is already signed in:", user);
        } else {
          getRedirectResult(auth)
            .then((result) => {
              if (!result) return;
              // This gives you a Google Access Token. You can use it to access Google APIs.
              const credential = GoogleAuthProvider.credentialFromResult(result);
              const token = credential.accessToken;

              // The signed-in user info.
              const user = result.user;
              // IdP data available using getAdditionalUserInfo(result)
              // ...
            }).catch((error) => {
            // Handle Errors here.
            const errorCode = error.code;
            const errorMessage = error.message;
            // The email of the user's account used.
            const email = error.customData.email;
            // The AuthCredential type that was used.
            const credential = GoogleAuthProvider.credentialFromError(error);
            // ...
          });
        }
      });
    }, 2000);
    // await this.initApp();
  }
  async initApp() {
    this.storageService.storageReady$.subscribe(async () => {
      await Promise.all([
        this.authStore.setApiKey(),
        this.authStore.loadUserData(),
      ])

      this.authStore.loadUserRegion().catch((res) => {
        console.error(res);
      });

      if(this.authStore.apiKey()) {
        await this.friendsStore.loadFriends()
      }
    });
  }
}
