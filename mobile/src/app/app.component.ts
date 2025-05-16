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
  chevronDownOutline
} from 'ionicons/icons';
import { FriendsStore } from './friends/friends.store';
import { IonicStorageModule } from '@ionic/storage-angular';
import { AuthStore } from './login/auth.store';
import { StorageService } from './services/storage.service';
import { Capacitor } from '@capacitor/core';

@Component({
  selector: 'mr-root',
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
      checkmarkOutline,
      chevronDownOutline
    });
  }

  async ngOnInit() {
    await this.initApp();
  }
  async initApp() {
    this.storageService.storageReady$.subscribe(async () => {
      await Promise.all([
        this.authStore.setApiKey(),
        this.authStore.loadUserData(),
        this.friendsStore.loadMostlyUsedCurrencies()
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
